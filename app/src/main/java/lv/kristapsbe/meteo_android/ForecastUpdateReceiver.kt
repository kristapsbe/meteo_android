package lv.kristapsbe.meteo_android

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ForecastUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            scheduleNextUpdate(context)
        } else {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val prefs = AppPreferences(context)
            val lastUpdateTime = prefs.getLong(Preference.LAST_SUCCESSFUL_UPDATE_TIME, 0L)
            
            // Check if it's been more than 1 hour since the last successful update
            // If it's been less than 1 hour, we run as non-expedited to save quota
            val shouldBeExpedited = (System.currentTimeMillis() - lastUpdateTime) > TimeUnit.HOURS.toMillis(1)

            val workRequestBuilder = OneTimeWorkRequestBuilder<ForecastRefreshWorker>()
                .setConstraints(constraints)

            if (shouldBeExpedited) {
                workRequestBuilder
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(Data.Builder().putBoolean(MainActivity.IS_EXPEDITED_KEY, true).build())
            } else {
                workRequestBuilder
                    .setInputData(Data.Builder().putBoolean(MainActivity.IS_EXPEDITED_KEY, false).build())
            }
                
            WorkManager.getInstance(context).enqueueUniqueWork(
                MainActivity.SINGLE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequestBuilder.build()
            )

            // Schedule the next alarm
            scheduleNextUpdate(context)
        }
    }

    companion object {
        const val REQUEST_CODE = 101
        // Interval changed to 20 minutes
        val ALARM_INTERVAL = TimeUnit.MINUTES.toMillis(20)

        fun scheduleNextUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ForecastUpdateReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + ALARM_INTERVAL,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact if permission is missing
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + ALARM_INTERVAL,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + ALARM_INTERVAL,
                    pendingIntent
                )
            }
        }
    }
}
