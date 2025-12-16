package lv.kristapsbe.meteo_android

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ForecastUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            scheduleNextUpdate(context)
        } else {
            // Enqueue the work
            val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                MainActivity.PERIODIC_FORECAST_DL_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            // Schedule the next alarm
            scheduleNextUpdate(context)
        }
    }

    companion object {
        const val REQUEST_CODE = 101
        val ALARM_INTERVAL = TimeUnit.HOURS.toMillis(6)

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
