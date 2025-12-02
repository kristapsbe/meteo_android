package lv.kristapsbe.meteo_android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters


class ForecastRefreshWorkerNoDL(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val id = "UPDATE_CHANNEL"
        val title = "Meteo"
        val text = "Atjaunina logrīka datus"

        val name = "Datu autjaunošana"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(id, name, importance)
        val notificationManager: NotificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.baseline_warning_24)
            .setOngoing(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as MyApplication
        val callback = app.workerCallback

        val cityForecast = CityForecastDataDownloader.downloadDataLatLon(applicationContext, doDL = false)
        callback?.onWorkerResult(cityForecast)

        if (cityForecast != null) {
            val displayInfo = DisplayInfo(cityForecast)
            DisplayInfo.updateWidget(applicationContext, displayInfo)
        }

        return Result.success()
    }

    companion object {
        private const val NOTIFICATION_ID = 1002
    }
}