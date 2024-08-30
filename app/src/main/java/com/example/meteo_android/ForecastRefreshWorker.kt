package com.example.meteo_android

import android.Manifest
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters


class ForecastRefreshWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val cityForecast = CityForecastDataDownloader.downloadData("doWork", applicationContext)

        // Get the callback from Application class and invoke it
        val result = "Result from Worker"
        val app = applicationContext as MyApplication
        val callback = app.workerCallback
        callback?.onWorkerResult(cityForecast, result)

        if (cityForecast != null) {
            val displayInfo = DisplayInfo(cityForecast)
            updateWidget("${displayInfo.getTodayForecast().currentTemp}")
        }

        // TODO: push notifications if weather warnings appear, use a file to keep track of what we've already warned about?
        Log.i("doWork", "showNotification")
        showNotification("Your Title", "Your Message")

        return Result.success()
    }

    private fun updateWidget(text: String) {
        val context = applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Retrieve the widget IDs
        val widget: ComponentName = ComponentName(context, ForecastWidget::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(widget)

        // Create an intent to update the widget
        val intent = Intent(context, ForecastWidget::class.java)
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        intent.putExtra("widget_text", text) // Pass the updated text

        context.sendBroadcast(intent)
    }

    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "YOUR_CHANNEL_ID"

    fun showNotification(title: String, message: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = TaskStackBuilder.create(applicationContext).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.example_battery)
            .setContentTitle(title)
            .setContentText(message)
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
            notify(NOTIFICATION_ID, builder.build())
        }
    }
}