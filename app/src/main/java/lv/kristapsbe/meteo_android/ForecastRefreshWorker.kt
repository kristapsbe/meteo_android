package lv.kristapsbe.meteo_android

import android.Manifest
import android.app.Activity.MODE_PRIVATE
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.util.Log
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
import lv.kristapsbe.meteo_android.MainActivity.Companion.AURORA_NOTIFICATION_THRESHOLD
import lv.kristapsbe.meteo_android.MainActivity.Companion.AURORA_NOTIF_ID
import lv.kristapsbe.meteo_android.MainActivity.Companion.HAS_AURORA_NOTIFIED
import lv.kristapsbe.meteo_android.MainActivity.Companion.LAST_COORDINATES_FILE
import lv.kristapsbe.meteo_android.MainActivity.Companion.defaultCoords
import kotlin.coroutines.resume


class ForecastRefreshWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private suspend fun getLastLocation(context: Context): Set<Double> {
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        val lastLocation = suspendCancellableCoroutine { continuation ->
            if (
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        continuation.resume(location)
                    }
                    .addOnFailureListener { _ ->
                        continuation.resume(null)
                    }
                    .addOnCanceledListener {
                        continuation.resume(null)
                    }
            } else {
                continuation.resume(null)
            }
        }
        if (lastLocation != null) {
            return setOf(lastLocation.latitude, lastLocation.longitude)
        } else {
            val coordContent = loadStringFromStorage(applicationContext, LAST_COORDINATES_FILE)
            return if (coordContent != "") {
                Json.decodeFromString<Set<Double>>(coordContent)
            } else {
                defaultCoords
            }
        }
    }

    override fun doWork(): Result {
        val app = applicationContext as MyApplication
        val callback = app.workerCallback

        runBlocking {
            val prefs = AppPreferences(app)

            val customLocationName = prefs.getString(Preference.FORCE_CURRENT_LOCATION)
            val cityForecast: CityForecastData?
            if (customLocationName != "") {
                cityForecast = CityForecastDataDownloader.downloadDataCityName(app, customLocationName)
            } else {
                val location = getLastLocation(app)
                cityForecast = CityForecastDataDownloader.downloadDataLatLon(app, location.elementAt(0), location.elementAt(1))
                app.openFileOutput(LAST_COORDINATES_FILE, MODE_PRIVATE).use { fos ->
                    fos.write(location.toString().toByteArray())
                }
            }

            callback?.onWorkerResult(cityForecast)

            if (cityForecast != null) {
                val selectedLang = prefs.getString(Preference.LANG)

                val displayInfo = DisplayInfo(cityForecast)
                DisplayInfo.updateWidget(
                    applicationContext,
                    displayInfo
                )
                var warnings: HashSet<Int> = hashSetOf()
                val content = loadStringFromStorage(applicationContext, MainActivity.WEATHER_WARNINGS_NOTIFIED_FILE)
                if (content != "") {
                    warnings = Json.decodeFromString<HashSet<Int>>(content)
                }

                for (w in displayInfo.warnings) {
                    if (!warnings.contains(w.id)) {
                        warnings.add(w.id) // TODO: only add if allowed to push notifs
                        showNotification(
                            MainActivity.WEATHER_WARNINGS_CHANNEL_ID,
                            w.id,
                            w.type[selectedLang] ?: "",
                            w.description[selectedLang] ?: "",
                            R.drawable.baseline_warning_24,
                            IconMapping.warningIconMapping[w.intensity] ?: R.drawable.baseline_warning_yellow_24
                        )
                    }
                }
                val hasAuroraNotificationBeenDisplayed = (loadStringFromStorage(applicationContext, HAS_AURORA_NOTIFIED) != "")
                if (hasAuroraNotificationBeenDisplayed) {
                    if (displayInfo.aurora.prob < AURORA_NOTIFICATION_THRESHOLD) {
                        applicationContext.openFileOutput(HAS_AURORA_NOTIFIED, MODE_PRIVATE).use { fos ->
                            fos.write("".toByteArray())
                        }
                    }
                } else if (displayInfo.aurora.prob >= AURORA_NOTIFICATION_THRESHOLD) {
                    applicationContext.openFileOutput(HAS_AURORA_NOTIFIED, MODE_PRIVATE).use { fos ->
                        fos.write("true".toByteArray())
                    }
                    var auroraNotifId = 1
                    try {
                        auroraNotifId = loadStringFromStorage(applicationContext, AURORA_NOTIF_ID).toInt()
                    } catch (e: Exception) {
                        Log.d("DEBUG", "Failed to parse aurora notification id: $e")
                    }
                    showNotification(
                        MainActivity.AURORA_NOTIFICATION_CHANNEL_ID,
                        auroraNotifId,
                        LangStrings.getTranslationString(selectedLang, Translation.NOTIFICATION_AURORA_TITLE),
                        "${LangStrings.getTranslationString(selectedLang, Translation.NOTIFICATION_AURORA_DESCRIPTION)} ${displayInfo.aurora.prob}%.",
                        R.drawable.aurora_borealis,
                        R.drawable.aurora_borealis
                    )
                    applicationContext.openFileOutput(AURORA_NOTIF_ID, MODE_PRIVATE).use { fos ->
                        fos.write((auroraNotifId+1).toString().toByteArray())
                    }
                }
                applicationContext.openFileOutput(MainActivity.WEATHER_WARNINGS_NOTIFIED_FILE, MODE_PRIVATE).use { fos ->
                    fos.write(warnings.toString().toByteArray())
                }
            }
        }
        return Result.success()
    }

    private fun showNotification(notifChannel: String, id: Int, title: String, description: String, smallIcon: Int, largeIcon: Int) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = TaskStackBuilder.create(applicationContext).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val builder = NotificationCompat.Builder(applicationContext, notifChannel)
            .setSmallIcon(smallIcon)
            .setLargeIcon(Icon.createWithResource(applicationContext, largeIcon))
            .setContentTitle(title)
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