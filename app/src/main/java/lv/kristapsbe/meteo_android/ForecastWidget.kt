package lv.kristapsbe.meteo_android

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import lv.kristapsbe.meteo_android.MainActivity.Companion.SINGLE_FORECAST_DL_NAME


/**
 * Implementation of App Widget functionality.
 */
class ForecastWidget : AppWidgetProvider() {
    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        // Create an Intent to launch the MainActivity when clicked
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        //val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

        val heightThresholdDp = 120

        val views = RemoteViews(context.packageName, R.layout.forecast_widget)
        views.setOnClickPendingIntent(R.id.widget, pendingIntent)

        // Check if the widget width is smaller than the threshold
        if (minHeight < heightThresholdDp) {
            views.setViewVisibility(R.id.top_widget, View.GONE)
            views.setViewVisibility(R.id.bottom_widget, View.GONE)
        } else {
            views.setViewVisibility(R.id.top_widget, View.VISIBLE)
            views.setViewVisibility(R.id.bottom_widget, View.VISIBLE)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)

        val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(SINGLE_FORECAST_DL_NAME, ExistingWorkPolicy.REPLACE, workRequest)

        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, null, null, null, false, false, false, null, null)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // Handle the broadcast from the Worker
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val text = intent.getStringExtra("widget_text")
            val locationText = intent.getStringExtra("widget_location")
            val feelsLikeText = intent.getStringExtra("widget_feelslike")
            val icon = intent.getIntExtra("icon_image", R.drawable.clear1)
            val warningRed = intent.getBooleanExtra("warning_red", false)
            val warningOrange = intent.getBooleanExtra("warning_orange", false)
            val warningYellow = intent.getBooleanExtra("warning_yellow", false)
            val rain = intent.getStringExtra("rain")

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widget = ComponentName(context, ForecastWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(widget)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, text, locationText, feelsLikeText, warningRed, warningOrange, warningYellow, icon, rain)
            }
        }
    }

    override fun onEnabled(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(SINGLE_FORECAST_DL_NAME, ExistingWorkPolicy.REPLACE, workRequest)
    }

    override fun onDisabled(context: Context) { }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    text: String?,
    locationText: String?,
    feelsLikeText: String?,
    warningRed: Boolean,
    warningOrange: Boolean,
    warningYellow: Boolean,
    icon: Int?,
    rain: String?
) {
    // Create an Intent to launch the MainActivity when clicked
    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    val views = RemoteViews(context.packageName, R.layout.forecast_widget)
    views.setOnClickPendingIntent(R.id.widget, pendingIntent)

    if (text != null) {
        views.setTextViewText(R.id.appwidget_text, text)
    }
    if (locationText != null) {
        views.setTextViewText(R.id.appwidget_location, locationText)
    }
    if (feelsLikeText != null) {
        views.setTextViewText(R.id.appwidget_feelslike, feelsLikeText)
    }
    views.setTextViewText(R.id.appwidget_rain, rain)
    if (icon != null) {
        views.setImageViewResource(R.id.icon_image, icon)
    }
    if (warningRed) {
        views.setImageViewResource(R.id.red_warning, R.drawable.baseline_warning_24_red)
    } else {
        views.setImageViewResource(R.id.red_warning, 0)
    }
    if (warningOrange) {
        views.setImageViewResource(R.id.orange_warning, R.drawable.baseline_warning_orange_24)
    } else {
        views.setImageViewResource(R.id.orange_warning, 0)
    }
    if (warningYellow) {
        views.setImageViewResource(R.id.yellow_warning, R.drawable.baseline_warning_yellow_24)
    } else {
        views.setImageViewResource(R.id.yellow_warning, 0)
    }
    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}