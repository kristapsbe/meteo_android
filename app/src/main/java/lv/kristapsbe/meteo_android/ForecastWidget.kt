package lv.kristapsbe.meteo_android

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import kotlinx.serialization.json.Json
import lv.kristapsbe.meteo_android.CityForecastDataDownloader.Companion.RESPONSE_FILE
import lv.kristapsbe.meteo_android.MainActivity.Companion.WIDGET_WORK_NAME


class ForecastWidget : AppWidgetProvider() {
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorkerNoDL>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(Data.Builder().putBoolean(MainActivity.IS_EXPEDITED_KEY, true).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WIDGET_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)

        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Instead of resetting to nulls, try to load the last known data
        try {
            val content = CityForecastDataDownloader.loadStringFromStorage(context, RESPONSE_FILE)
            if (content.isNotEmpty()) {
                val data = Json.decodeFromString<CityForecastData>(content)
                val displayInfo = DisplayInfo(data)
                DisplayInfo.updateWidget(context, displayInfo)
            } else {
                // If no data, trigger a refresh
                val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorkerNoDL>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(Data.Builder().putBoolean(MainActivity.IS_EXPEDITED_KEY, true).build())
                    .build()
                WorkManager.getInstance(context)
                    .enqueueUniqueWork(WIDGET_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)
            }
        } catch (e: Exception) {
            Log.e("WIDGET", "Failed to update widget onUpdate: $e")
        }
    }

    override fun onEnabled(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorkerNoDL>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(Data.Builder().putBoolean(MainActivity.IS_EXPEDITED_KEY, true).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WIDGET_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)
    }

    override fun onDisabled(context: Context) {}
}

fun updateAppWidget(
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
    rain: String,
    doShowWidgetBackground: Boolean,
    aurora: String?,
    useAltLayout: Boolean,
    doShowAurora: Boolean,
    doShowUV: Boolean,
    rainImage: Int,
    uvIndex: String?
) {
    // Create an Intent to launch the MainActivity when clicked
    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    val views = RemoteViews(context.packageName, R.layout.forecast_widget)
    views.setOnClickPendingIntent(R.id.widget, pendingIntent)

    views.setInt(
        R.id.widget,
        "setBackgroundColor",
        ContextCompat.getColor(
            context,
            if (doShowWidgetBackground) R.color.sky_blue else R.color.transparent
        )
    )
    
    // Safety check: if text is null, the widget is in an uninitialized state
    if (text == null) return

    views.setTextViewText(R.id.appwidget_text, text)
    
    if (locationText != null) {
        views.setTextViewText(R.id.appwidget_location, locationText)
        views.setTextViewText(R.id.appwidget_location_small, locationText)
    }
    if (feelsLikeText != null) {
        views.setTextViewText(R.id.appwidget_feelslike, feelsLikeText)
    }

    if (rain == "") {
        views.setViewVisibility(R.id.appwidget_rain_wrap, View.GONE)
    } else {
        views.setTextViewText(R.id.appwidget_rain, rain)
        views.setImageViewResource(R.id.appwidget_rain_icon, rainImage)
        views.setViewVisibility(R.id.appwidget_rain_wrap, View.VISIBLE)
    }

    if (doShowAurora) {
        views.setTextViewText(R.id.appwidget_aurora, aurora)
        views.setViewVisibility(R.id.appwidget_aurora_wrap, View.VISIBLE)
    } else {
        views.setViewVisibility(R.id.appwidget_aurora_wrap, View.GONE)
    }

    if (doShowUV) {
        views.setTextViewText(R.id.appwidget_uv, uvIndex)
        if (useAltLayout) {
            views.setViewVisibility(R.id.appwidget_uv_wrap, View.GONE)
            views.setImageViewResource(R.id.uv_alt, R.drawable.uv)
        } else {
            views.setViewVisibility(R.id.appwidget_uv_wrap, View.VISIBLE)
            views.setImageViewResource(R.id.uv_alt, 0)
        }
    } else {
        views.setViewVisibility(R.id.appwidget_uv_wrap, View.GONE)
        views.setImageViewResource(R.id.uv_alt, 0)
    }

    if (icon != null) {
        views.setImageViewResource(R.id.icon_image, icon)
    }
    if (warningRed) {
        views.setImageViewResource(R.id.red_warning, R.drawable.baseline_warning_24_red)
        views.setImageViewResource(R.id.red_warning_small, R.drawable.baseline_warning_24_red)
        views.setImageViewResource(R.id.red_warning_small_alt, R.drawable.baseline_warning_24_red)
    } else {
        views.setImageViewResource(R.id.red_warning, 0)
        views.setImageViewResource(R.id.red_warning_small, 0)
        views.setImageViewResource(R.id.red_warning_small_alt, 0)
    }
    if (warningOrange) {
        views.setImageViewResource(R.id.orange_warning, R.drawable.baseline_warning_orange_24)
        views.setImageViewResource(R.id.orange_warning_small, R.drawable.baseline_warning_orange_24)
        views.setImageViewResource(
            R.id.orange_warning_small_alt,
            R.drawable.baseline_warning_orange_24
        )
    } else {
        views.setImageViewResource(R.id.orange_warning, 0)
        views.setImageViewResource(R.id.orange_warning_small, 0)
        views.setImageViewResource(R.id.orange_warning_small_alt, 0)
    }
    if (warningYellow) {
        views.setImageViewResource(R.id.yellow_warning, R.drawable.baseline_warning_yellow_24)
        views.setImageViewResource(R.id.yellow_warning_small, R.drawable.baseline_warning_yellow_24)
        views.setImageViewResource(
            R.id.yellow_warning_small_alt,
            R.drawable.baseline_warning_yellow_24
        )
    } else {
        views.setImageViewResource(R.id.yellow_warning, 0)
        views.setImageViewResource(R.id.yellow_warning_small, 0)
        views.setImageViewResource(R.id.yellow_warning_small_alt, 0)
    }

    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
    val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
    val displayMetrics = Resources.getSystem().displayMetrics
    val density = displayMetrics.density
    val minHeightDp = (minHeight / density).toInt()

    if (minHeightDp > 45) {
        views.setViewVisibility(R.id.top_widget, View.VISIBLE)
        views.setViewVisibility(R.id.bottom_widget, View.VISIBLE)

        if (useAltLayout) {
            views.setViewVisibility(R.id.main_warnings_small_alt, View.VISIBLE)
            views.setViewVisibility(R.id.main_warnings, View.GONE)
        } else {
            views.setViewVisibility(R.id.main_warnings_small_alt, View.GONE)
            views.setViewVisibility(R.id.main_warnings, View.VISIBLE)
        }

        views.setViewVisibility(R.id.appwidget_location_small, View.GONE)
        views.setViewVisibility(R.id.main_warnings_small, View.GONE)
    } else {
        views.setViewVisibility(R.id.top_widget, View.GONE)
        views.setViewVisibility(R.id.bottom_widget, View.GONE)

        if (useAltLayout) {
            views.setViewVisibility(R.id.main_warnings_small_alt, View.VISIBLE)
            views.setViewVisibility(R.id.main_warnings_small, View.GONE)
        } else {
            views.setViewVisibility(R.id.main_warnings_small_alt, View.GONE)
            views.setViewVisibility(R.id.main_warnings_small, View.VISIBLE)
        }

        views.setViewVisibility(R.id.appwidget_location_small, View.VISIBLE)
    }
    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}
