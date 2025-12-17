package lv.kristapsbe.meteo_android

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import lv.kristapsbe.meteo_android.MainActivity.Companion.SINGLE_FORECAST_NO_DL_NAME


class ForecastWidget : AppWidgetProvider() {
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorkerNoDL>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(SINGLE_FORECAST_NO_DL_NAME, ExistingWorkPolicy.REPLACE, workRequest)

        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(
                context,
                appWidgetManager,
                appWidgetId,
                null,
                null,
                null,
                false,
                false,
                false,
                null,
                "",
                false,
                null,
                false,
                false,
                false,
                -1,
                ""
            )
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
            val uvIndex = intent.getStringExtra("uv_index")
            val rainImage = intent.getIntExtra("rain_image", R.drawable.clear1)
            val aurora = intent.getStringExtra("aurora")
            val doShowAurora = intent.getBooleanExtra("do_show_aurora", false)
            val doShowUV = intent.getBooleanExtra("do_show_uv", false)

            val doShowWidgetBackground = intent.getBooleanExtra("do_show_widget_background", false)
            val useAltLayout = intent.getBooleanExtra("use_alt_layout", false)

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widget = ComponentName(context, ForecastWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(widget)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(
                    context,
                    appWidgetManager,
                    appWidgetId,
                    text,
                    locationText,
                    feelsLikeText,
                    warningRed,
                    warningOrange,
                    warningYellow,
                    icon,
                    rain ?: "",
                    doShowWidgetBackground,
                    aurora,
                    useAltLayout,
                    doShowAurora,
                    doShowUV,
                    rainImage,
                    uvIndex
                )
            }
        }
    }

    override fun onEnabled(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorkerNoDL>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(SINGLE_FORECAST_NO_DL_NAME, ExistingWorkPolicy.REPLACE, workRequest)
    }

    override fun onDisabled(context: Context) {}
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
    if (text != null) {
        views.setTextViewText(R.id.appwidget_text, text)
    }
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
