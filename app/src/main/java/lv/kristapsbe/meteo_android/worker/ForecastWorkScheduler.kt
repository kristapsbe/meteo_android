package lv.kristapsbe.meteo_android.worker

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import lv.kristapsbe.meteo_android.MainActivity.Companion.PERIODIC_WORK_NAME
import lv.kristapsbe.meteo_android.data.AppPreferences
import lv.kristapsbe.meteo_android.data.Preference
import java.util.concurrent.TimeUnit

object ForecastWorkScheduler {
    private const val TAG = "WORK"

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Schedules the periodic refresh that drives weather warning notifications.
     *
     * WorkManager persists the worker's class name, and a periodic job whose class can no longer be
     * loaded stays ENQUEUED and fails on every run. KEEP would leave such a job in place forever,
     * so the first call after an app update uses UPDATE to rewrite the stored job.
     *
     * UPDATE can't be used on every call: it bumps the job's generation, and WorkManager drops start
     * requests from older generations, so it could drop the job that started this process.
     */
    fun ensurePeriodicWork(context: Context) {
        val appContext = context.applicationContext
        val prefs = AppPreferences(appContext)
        val versionCode = try {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).longVersionCode
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Failed to read the app's version: $e")
            null
        }
        val isFirstRunOfVersion =
            versionCode != null && prefs.getLong(Preference.PERIODIC_WORK_VERSION_CODE) != versionCode

        val periodicWorkRequest =
            PeriodicWorkRequestBuilder<ForecastRefreshWorker>(1, TimeUnit.HOURS)
                .setConstraints(networkConstraints)
                .build()
        val result = WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            if (isFirstRunOfVersion) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        ).result

        if (isFirstRunOfVersion) {
            result.addListener({
                try {
                    result.get()
                    prefs.setLong(Preference.PERIODIC_WORK_VERSION_CODE, versionCode)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update periodic work: $e")
                }
            }, Runnable::run)
        }
    }

    fun refreshRequest(
        expedited: Boolean = true,
        requireNetwork: Boolean = false
    ): OneTimeWorkRequest {
        val builder = OneTimeWorkRequestBuilder<ForecastRefreshWorker>()
        // Below API 31 expedited work runs in a foreground service, which needs a
        // getForegroundInfo() implementation that ForecastRefreshWorker doesn't have.
        if (expedited && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        }
        if (requireNetwork) {
            builder.setConstraints(networkConstraints)
        }
        return builder.build()
    }
}
