package lv.kristapsbe.meteo_android

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.serialization.json.Json
import lv.kristapsbe.meteo_android.CityForecastDataDownloader.Companion.RESPONSE_FILE
import lv.kristapsbe.meteo_android.CityForecastDataDownloader.Companion.loadStringFromStorage
import lv.kristapsbe.meteo_android.MainActivity.Companion.SELECTED_TEMP_FILE
import lv.kristapsbe.meteo_android.MainActivity.Companion.SINGLE_FORECAST_DL_NAME
import lv.kristapsbe.meteo_android.MainActivity.Companion.convertFromCtoDisplayTemp


/**
 * Implementation of App Widget functionality.
 */
class ForecastWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, null, null, null, false, false, false, null)
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

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widget = ComponentName(context, ForecastWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(widget)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, text, locationText, feelsLikeText, warningRed, warningOrange, warningYellow, icon)
            }
        }
    }

    override fun onEnabled(context: Context) { }

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
    icon: Int?
) {
    // Create an Intent to launch the MainActivity when clicked
    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    // Construct the RemoteViews object
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