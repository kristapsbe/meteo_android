package lv.kristapsbe.meteo_android

import android.Manifest
import android.app.Activity.MODE_PRIVATE
import android.app.PendingIntent
import android.app.TaskStackBuilder
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
import lv.kristapsbe.meteo_android.MainActivity.Companion.AURORA_NOTIFICATION_THRESHOLD
import lv.kristapsbe.meteo_android.MainActivity.Companion.DEFAULT_LAT
import lv.kristapsbe.meteo_android.MainActivity.Companion.DEFAULT_LON
import lv.kristapsbe.meteo_android.MainActivity.Companion.LANG_EN
import lv.kristapsbe.meteo_android.MainActivity.Companion.LANG_LV
import java.util.Locale
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
            val prefs = AppPreferences(context)
            return setOf(
                prefs.getFloat(Preference.LAST_LAT, DEFAULT_LAT).toDouble(),
                prefs.getFloat(Preference.LAST_LON, DEFAULT_LON).toDouble()
            )
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
                prefs.setFloat(Preference.LAST_LAT, location.elementAt(0).toFloat())
                prefs.setFloat(Preference.LAST_LON, location.elementAt(1).toFloat())
            }

            callback?.onWorkerResult(cityForecast)

            if (cityForecast != null) {
                val currentLocale: Locale = Locale.getDefault()
                val language: String = currentLocale.language

                val selectedLang = prefs.getString(Preference.LANG, if (language == LANG_LV) LANG_LV else LANG_EN)

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

                // TODO: the file's just going to keep growing - I need to clear it out somehow
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
                applicationContext.openFileOutput(MainActivity.WEATHER_WARNINGS_NOTIFIED_FILE, MODE_PRIVATE).use { fos ->
                    fos.write(warnings.toString().toByteArray())
                }

                val hasAuroraNotificationBeenDisplayed = prefs.getBoolean(Preference.HAS_AURORA_NOTIFIED)
                if (hasAuroraNotificationBeenDisplayed) {
                    if (displayInfo.aurora.prob < AURORA_NOTIFICATION_THRESHOLD) {
                        prefs.setBoolean(Preference.HAS_AURORA_NOTIFIED, false)
                    }
                } else if (displayInfo.aurora.prob >= AURORA_NOTIFICATION_THRESHOLD) {
                    prefs.setBoolean(Preference.HAS_AURORA_NOTIFIED, true)
                    // TODO: do I actually need to set a new id?
                    val auroraNotifId = prefs.getInt(Preference.AURORA_NOTIFICATION_ID)
                    showNotification(
                        MainActivity.AURORA_NOTIFICATION_CHANNEL_ID,
                        auroraNotifId,
                        LangStrings.getTranslationString(selectedLang, Translation.NOTIFICATION_AURORA_TITLE),
                        "${LangStrings.getTranslationString(selectedLang, Translation.NOTIFICATION_AURORA_DESCRIPTION)} ${displayInfo.aurora.prob}%.",
                        R.drawable.baseline_star_border_24,
                        R.drawable.baseline_star_border_green_24
                    )
                    prefs.setInt(Preference.AURORA_NOTIFICATION_ID, (auroraNotifId+1))
                }
            }
        }
        return Result.success()
    }

    private fun showNotification(notifChannel: String, id: Int, title: String, description: String, smallIcon: Int, largeIcon: Int) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        intent.putExtra("opened_from_notification", true)

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
                return
            }
            notify(id, builder.build())
        }
    }
}