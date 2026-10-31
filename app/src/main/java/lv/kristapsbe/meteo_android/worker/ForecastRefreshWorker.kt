package lv.kristapsbe.meteo_android.worker

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.util.AtomicFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import lv.kristapsbe.meteo_android.DisplayInfo
import lv.kristapsbe.meteo_android.MainActivity
import lv.kristapsbe.meteo_android.MyApplication
import lv.kristapsbe.meteo_android.R
import lv.kristapsbe.meteo_android.data.AppPreferences
import lv.kristapsbe.meteo_android.data.CityForecastDataDownloader
import lv.kristapsbe.meteo_android.data.DownloadResult
import lv.kristapsbe.meteo_android.data.Preference
import lv.kristapsbe.meteo_android.util.IconMapping
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

class ForecastRefreshWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    companion object {
        private const val MAX_DOWNLOAD_RETRIES = 3
    }

    private suspend fun getLastLocation(context: Context): Set<Double> {
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        val lastLocation = suspendCancellableCoroutine { continuation ->
            if (
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
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
                prefs.getFloat(Preference.LAST_LAT, MainActivity.Companion.DEFAULT_LAT).toDouble(),
                prefs.getFloat(Preference.LAST_LON, MainActivity.Companion.DEFAULT_LON).toDouble()
            )
        }
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as MyApplication
        val callback = app.workerCallback

        val prefs = AppPreferences(app)

        val customLocationName = prefs.getString(Preference.FORCE_CURRENT_LOCATION)
        val isAnimated = prefs.getBoolean(Preference.USE_ANIMATED_ICONS)
        val enableExperimental = prefs.getBoolean(Preference.ENABLE_EXPERIMENTAL_FORECASTS)
        val download: DownloadResult
        if (customLocationName != "") {
            download = CityForecastDataDownloader.Companion.downloadDataCityName(
                app,
                locationName = customLocationName,
                isAnimated = isAnimated,
                enableExperimental = enableExperimental
            )
        } else {
            val location = getLastLocation(app)
            download = CityForecastDataDownloader.Companion.downloadDataLatLon(
                app,
                lat = location.elementAt(0),
                lon = location.elementAt(1),
                isAnimated = isAnimated,
                enableExperimental = enableExperimental
            )
            prefs.setFloat(Preference.LAST_LAT, location.elementAt(0).toFloat())
            prefs.setFloat(Preference.LAST_LON, location.elementAt(1).toFloat())
        }

        val cityForecast = download.forecast
        callback?.onWorkerResult(cityForecast)

        val displayInfo = cityForecast?.let { DisplayInfo(it) }
        if (displayInfo != null) {
            DisplayInfo.Companion.updateWidget(
                applicationContext,
                displayInfo
            )
        }

        // Cached warnings were already handled when they were downloaded, and may have expired
        if (displayInfo == null || !download.isFresh) {
            return if (runAttemptCount < MAX_DOWNLOAD_RETRIES) Result.retry() else Result.success()
        }

        // Update last successful update time
        prefs.setLong(Preference.LAST_SUCCESSFUL_UPDATE_TIME, System.currentTimeMillis())

        val currentLocale: Locale = Locale.getDefault()
        val language: String = currentLocale.language

        val selectedLang =
            prefs.getString(
                Preference.LANG,
                if (language == MainActivity.Companion.LANG_LV) MainActivity.Companion.LANG_LV else MainActivity.Companion.LANG_EN
            )

        val warnings = loadNotifiedWarnings()

        // TODO: the file's just going to keep growing - I need to clear it out somehow
        for (w in displayInfo.warnings) {
            if (!w.ids.all { it in warnings }) {
                val wasPosted = showNotification(
                    MainActivity.Companion.WEATHER_WARNINGS_CHANNEL_ID,
                    w.ids[0],
                    w.type[selectedLang] ?: "",
                    w.getFullDescription(selectedLang),
                    R.drawable.baseline_warning_24,
                    IconMapping.Companion.warningIconMapping[w.intensity]
                        ?: R.drawable.baseline_warning_yellow_24
                )
                if (wasPosted) {
                    warnings.addAll(w.ids)
                }
            }
        }
        saveNotifiedWarnings(warnings)

        val doShowAurora = prefs.getBoolean(Preference.DO_SHOW_AURORA, true)
        if (doShowAurora) {
            val hasAuroraNotificationBeenDisplayed =
                prefs.getBoolean(Preference.HAS_AURORA_NOTIFIED)
            if (hasAuroraNotificationBeenDisplayed) {
                if (displayInfo.aurora.prob < MainActivity.Companion.AURORA_NOTIFICATION_THRESHOLD) {
                    prefs.setBoolean(Preference.HAS_AURORA_NOTIFIED, false)
                }
            } else if (displayInfo.aurora.prob >= MainActivity.Companion.AURORA_NOTIFICATION_THRESHOLD) {
                // TODO: do I actually need to set a new id?
                val auroraNotifId = prefs.getInt(Preference.AURORA_NOTIFICATION_ID)
                val wasPosted = showNotification(
                    MainActivity.Companion.AURORA_NOTIFICATION_CHANNEL_ID,
                    auroraNotifId,
                    applicationContext.getString(R.string.notification_aurora_title),
                    "${applicationContext.getString(R.string.notification_aurora_description)} ${displayInfo.aurora.prob}%.",
                    R.drawable.baseline_star_border_24,
                    R.drawable.baseline_star_border_green_24
                )
                if (wasPosted) {
                    prefs.setBoolean(Preference.HAS_AURORA_NOTIFIED, true)
                    prefs.setInt(Preference.AURORA_NOTIFICATION_ID, (auroraNotifId + 1))
                }
            }
        }

        return Result.success()
    }

    private fun loadNotifiedWarnings(): HashSet<Int> {
        val content = CityForecastDataDownloader.Companion.loadStringFromStorage(
            applicationContext,
            MainActivity.Companion.WEATHER_WARNINGS_NOTIFIED_FILE
        )
        if (content == "") {
            return hashSetOf()
        }
        return try {
            Json.Default.decodeFromString<HashSet<Int>>(content)
        } catch (e: SerializationException) {
            // Starting over repeats active warnings, a corrupt file must not block them for good
            Log.e("ERROR", "Failed to read notified warnings: $e")
            hashSetOf()
        }
    }

    private fun saveNotifiedWarnings(warnings: HashSet<Int>) {
        val file = AtomicFile(
            File(applicationContext.filesDir, MainActivity.Companion.WEATHER_WARNINGS_NOTIFIED_FILE)
        )
        val fos = file.startWrite()
        try {
            fos.write(Json.Default.encodeToString(warnings).toByteArray())
            file.finishWrite(fos)
        } catch (e: IOException) {
            file.failWrite(fos)
            throw e
        }
    }

    /** Returns false if the system won't display the notification. */
    private fun showNotification(
        notifChannel: String,
        id: Int,
        title: String,
        description: String,
        smallIcon: Int,
        largeIcon: Int
    ): Boolean {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(applicationContext, notifChannel)
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
                return false
            }
            val channel = getNotificationChannel(notifChannel)
            if (
                !areNotificationsEnabled() ||
                channel == null ||
                channel.importance == NotificationManager.IMPORTANCE_NONE
            ) {
                return false
            }
            notify(id, builder.build())
        }
        return true
    }
}