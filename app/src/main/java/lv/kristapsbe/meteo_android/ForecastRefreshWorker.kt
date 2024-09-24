package lv.kristapsbe.meteo_android

import android.Manifest
import android.app.Activity.MODE_PRIVATE
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import lv.kristapsbe.meteo_android.CityForecastDataDownloader.Companion.loadStringFromStorage
import lv.kristapsbe.meteo_android.MainActivity.Companion.LANG_EN
import lv.kristapsbe.meteo_android.MainActivity.Companion.LOCKED_LOCATION_FILE
import lv.kristapsbe.meteo_android.MainActivity.Companion.SELECTED_LANG
import lv.kristapsbe.meteo_android.MainActivity.Companion.SELECTED_TEMP_FILE
import lv.kristapsbe.meteo_android.MainActivity.Companion.convertFromCtoDisplayTemp
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class ForecastRefreshWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private suspend fun getLastLocation(context: Context): Set<Double> {
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        val lastLocation = suspendCancellableCoroutine { continuation ->
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        continuation.resume(location)
                    }
                    .addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
                    .addOnCanceledListener {
                        continuation.cancel()
                    }
            } else {
                continuation.resume(null)
            }
        }
        if (lastLocation != null) {
            return setOf(lastLocation.latitude, lastLocation.longitude)
        } else {
            for (f in applicationContext.fileList()) {
                if (f.equals(MainActivity.LAST_COORDINATES_FILE)) {
                    val content = applicationContext.openFileInput(MainActivity.LAST_COORDINATES_FILE).bufferedReader().use { it.readText() }
                    return Json.decodeFromString<Set<Double>>(content)
                }
            }
            return setOf(56.9730, 24.1327) // Don't have anything to go off of - default to Riga
        }
    }

    override fun doWork(): Result {
        val app = applicationContext as MyApplication
        val callback = app.workerCallback

        runBlocking {
            val customLocationName = loadStringFromStorage(applicationContext, LOCKED_LOCATION_FILE)
            val selectedLang = loadStringFromStorage(applicationContext, SELECTED_LANG)
            val cityForecast: CityForecastData?
            if (customLocationName != "") {
                cityForecast = CityForecastDataDownloader.downloadDataCityName(applicationContext, customLocationName)
            } else {
                val location = getLastLocation(applicationContext)
                cityForecast = CityForecastDataDownloader.downloadDataLatLon(applicationContext, location.elementAt(0), location.elementAt(1))
                applicationContext.openFileOutput(MainActivity.LAST_COORDINATES_FILE, MODE_PRIVATE).use { fos ->
                    fos.write(location.toString().toByteArray())
                }
            }

            callback?.onWorkerResult(cityForecast)

            if (cityForecast != null) {
                val displayInfo = DisplayInfo(cityForecast)
                updateWidget(
                    displayInfo.getTodayForecast().currentTemp,
                    displayInfo.city,
                    displayInfo.getTodayForecast().feelsLikeTemp,
                    displayInfo.getTodayForecast().pictogram.getPictogram(),
                    cityForecast.warnings.any { it.intensity[1] == "Red" },
                    cityForecast.warnings.any { it.intensity[1] == "Orange" },
                    cityForecast.warnings.any { it.intensity[1] == "Yellow" },
                    displayInfo.getWhenRainExpected(applicationContext, selectedLang),
                    selectedLang
                )
                var warnings: HashSet<Int> = hashSetOf()
                val content = loadStringFromStorage(applicationContext, MainActivity.WEATHER_WARNINGS_NOTIFIED_FILE)
                if (content != "") {
                    warnings = Json.decodeFromString<HashSet<Int>>(content)
                }

                for (w in displayInfo.warnings) {
                    if (!warnings.contains(w.id)) {
                        warnings.add(w.id) // TODO: only add if allowed to push notifs
                        showNotification(w.id, w.intensity, w.type[selectedLang] ?: "", w.description[selectedLang] ?: "")
                    }
                }
                applicationContext.openFileOutput(MainActivity.WEATHER_WARNINGS_NOTIFIED_FILE, MODE_PRIVATE).use { fos ->
                    fos.write(warnings.toString().toByteArray())
                }
            }
        }
        return Result.success()
    }

    private fun updateWidget(tempC: Int, textLocation: String, feelsLikeC: Int, icon: Int, warningRed: Boolean, warningOrange: Boolean, warningYellow: Boolean, rainTime: String, lang: String) {
        val context = applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(context)

        val selectedTemp = loadStringFromStorage(applicationContext, SELECTED_TEMP_FILE)

        // Retrieve the widget IDs
        val widget = ComponentName(context, ForecastWidget::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(widget)

        // Create an intent to update the widget
        val intent = Intent(context, ForecastWidget::class.java)
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        intent.putExtra("widget_text", convertFromCtoDisplayTemp(tempC, selectedTemp))
        intent.putExtra("widget_location", textLocation)
        if (lang == LANG_EN) {
            intent.putExtra("widget_feelslike", "${applicationContext.getString(R.string.feels_like_en)} ${convertFromCtoDisplayTemp(feelsLikeC, selectedTemp)}")
        } else {
            intent.putExtra("widget_feelslike", "${applicationContext.getString(R.string.feels_like_lv)} ${convertFromCtoDisplayTemp(feelsLikeC, selectedTemp)}")
        }
        intent.putExtra("icon_image", icon)
        intent.putExtra("warning_red", warningRed)
        intent.putExtra("warning_orange", warningOrange)
        intent.putExtra("warning_yellow", warningYellow)
        intent.putExtra("rain", rainTime)

        context.sendBroadcast(intent)
    }

    private fun showNotification(id: Int, intensity: String, type: String, description: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = TaskStackBuilder.create(applicationContext).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val builder = NotificationCompat.Builder(applicationContext,
            MainActivity.WEATHER_WARNINGS_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.baseline_warning_24)
            .setLargeIcon(Icon.createWithResource(applicationContext, WeatherPictogram.warningIconMapping[intensity] ?: R.drawable.baseline_warning_yellow_24))
            .setContentTitle(type)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(applicationContext)) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            notify(id, builder.build())
        }
    }
}