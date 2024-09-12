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
import android.location.Location
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class ForecastRefreshWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private suspend fun getLastLocation(context: Context): Location? {
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        return suspendCancellableCoroutine { continuation ->
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
            }
        }
    }

    override fun doWork(): Result {
        val app = applicationContext as MyApplication
        val callback = app.workerCallback

        runBlocking {
            val location = getLastLocation(applicationContext)
            // TODO: save location whenever one gets returned so that this can run if only foreground location permissions are granted
            if (location != null) {
                //val cityForecast = CityForecastDataDownloader.downloadData(applicationContext)
                val cityForecast = CityForecastDataDownloader.downloadData(applicationContext, location.latitude, location.longitude)

                // Get the callback from Application class and invoke it
                val result = "Result from Worker"
                callback?.onWorkerResult(cityForecast, result)

                if (cityForecast != null) {
                    val displayInfo = DisplayInfo(cityForecast)
                    updateWidget(
                        "${displayInfo.getTodayForecast().currentTemp}",
                        displayInfo.location.name,
                        "feels like ${displayInfo.getTodayForecast().feelsLikeTemp}°",
                        displayInfo.getTodayForecast().pictogram.getPictogram()
                    )

                    var warnings: HashSet<Int> = hashSetOf()
                    for (f in applicationContext.fileList()) {
                        if (f.equals(MainActivity.WEATHER_WARNINGS_NOTIFIED_FILE)) {
                            val content = applicationContext.openFileInput(MainActivity.WEATHER_WARNINGS_NOTIFIED_FILE).bufferedReader().use { it.readText() }
                            warnings = Json.decodeFromString<HashSet<Int>>(content)
                            break
                        }
                    }

                    for (w in displayInfo.warnings) {
                        if (!warnings.contains(w.id)) {
                            warnings.add(w.id)
                            showNotification(w.id, w.intensity, w.type, w.description)
                        }
                    }
                    applicationContext.openFileOutput(MainActivity.WEATHER_WARNINGS_NOTIFIED_FILE, MODE_PRIVATE).use { fos ->
                        fos.write(warnings.toString().toByteArray())
                    }
                }
            }
        }
        return Result.success()
    }

    private fun updateWidget(text: String, textLocation: String, textFeelsLike: String, icon: Int) {
        val context = applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Retrieve the widget IDs
        val widget = ComponentName(context, ForecastWidget::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(widget)

        // Create an intent to update the widget
        val intent = Intent(context, ForecastWidget::class.java)
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        intent.putExtra("widget_text", "$text°") // Pass the updated text
        intent.putExtra("widget_location", textLocation) // Pass the updated text
        intent.putExtra("widget_feelslike", textFeelsLike) // Pass the updated text
        intent.putExtra("icon_image", icon) // Pass the updated text

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
            .setSmallIcon(WeatherPictogram.warningIconMapping[intensity] ?: R.drawable.example_battery)
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