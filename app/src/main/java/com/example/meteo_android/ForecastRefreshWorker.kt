package com.example.meteo_android

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
}