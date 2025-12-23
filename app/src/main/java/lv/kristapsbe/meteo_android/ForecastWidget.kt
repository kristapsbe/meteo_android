package lv.kristapsbe.meteo_android

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.serialization.json.Json
import lv.kristapsbe.meteo_android.CityForecastDataDownloader.Companion.RESPONSE_FILE
import lv.kristapsbe.meteo_android.MainActivity.Companion.WIDGET_WORK_NAME
import java.util.concurrent.TimeUnit

data class WidgetForecastState(
    val tempText: String?,
    val locationText: String?,
    val feelsLikeText: String?,
    val weatherIconRes: Int?,
    val rainText: String,
    val rainIconRes: Int,
    val auroraText: String?,
    val uvIndexText: String?,
    val hasRedWarning: Boolean,
    val hasOrangeWarning: Boolean,
    val hasYellowWarning: Boolean,
    val showAurora: Boolean,
    val showUV: Boolean,
    val showBackground: Boolean,
    val useAltLayout: Boolean
)

class ForecastWidget : AppWidgetProvider() {
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val content = CityForecastDataDownloader.loadStringFromStorage(context, RESPONSE_FILE)
        if (content.isNotEmpty()) {
            val data = Json.decodeFromString<CityForecastData>(content)
            DisplayInfo.updateWidget(context, DisplayInfo(data))
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = AppPreferences(context)
        val lastSuccess = prefs.getLong(Preference.LAST_SUCCESSFUL_UPDATE_TIME, 0L)
        val currentTime = System.currentTimeMillis()

        val content = CityForecastDataDownloader.loadStringFromStorage(context, RESPONSE_FILE)
        if (content.isNotEmpty()) {
            val data = Json.decodeFromString<CityForecastData>(content)
            DisplayInfo.updateWidget(context, DisplayInfo(data))
        }

        val isStale = (currentTime - lastSuccess) > TimeUnit.MINUTES.toMillis(20)
        val isRecentlyAttempted = (currentTime - lastSuccess) < TimeUnit.MINUTES.toMillis(1)

        if (isStale && !isRecentlyAttempted) {
            val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WIDGET_WORK_NAME, ExistingWorkPolicy.KEEP, workRequest)
        } else {
            Log.i("WIDGET", "Widget update skipped")
        }
    }

    override fun onEnabled(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WIDGET_WORK_NAME, ExistingWorkPolicy.KEEP, workRequest)
    }

    override fun onDisabled(context: Context) {}
}

fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    state: WidgetForecastState
) {
    if (state.tempText == null) return

    val views = RemoteViews(context.packageName, R.layout.forecast_widget).apply {
        // Click behavior
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        setOnClickPendingIntent(R.id.widget, pendingIntent)

        // Background
        val bgColor = ContextCompat.getColor(
            context,
            if (state.showBackground) R.color.sky_blue else R.color.transparent
        )
        setInt(R.id.widget, "setBackgroundColor", bgColor)

        // Main info
        setTextViewText(R.id.appwidget_text, state.tempText)
        state.locationText?.let {
            setTextViewText(R.id.appwidget_location, it)
            setTextViewText(R.id.appwidget_location_small, it)
        }
        state.feelsLikeText?.let { setTextViewText(R.id.appwidget_feelslike, it) }

        // Weather Icon
        state.weatherIconRes?.let { setImageViewResource(R.id.icon_image, it) }

        // Rain section
        val hasRain = state.rainText.isNotEmpty()
        setVisibility(R.id.appwidget_rain_wrap, hasRain)
        if (hasRain) {
            setTextViewText(R.id.appwidget_rain, state.rainText)
            setImageViewResource(R.id.appwidget_rain_icon, state.rainIconRes)
        }

        // Aurora section
        setVisibility(R.id.appwidget_aurora_wrap, state.showAurora)
        if (state.showAurora) {
            setTextViewText(R.id.appwidget_aurora, state.auroraText)
        }

        // UV section
        setVisibility(R.id.appwidget_uv_wrap, state.showUV && !state.useAltLayout)
        setImageViewResource(
            R.id.uv_alt,
            if (state.showUV && state.useAltLayout) R.drawable.uv else 0
        )
        if (state.showUV) {
            setTextViewText(R.id.appwidget_uv, state.uvIndexText)
        }

        // Warnings
        setWarningIcons(
            state.hasRedWarning,
            R.drawable.baseline_warning_24_red,
            R.id.red_warning,
            R.id.red_warning_small,
            R.id.red_warning_small_alt
        )
        setWarningIcons(
            state.hasOrangeWarning,
            R.drawable.baseline_warning_orange_24,
            R.id.orange_warning,
            R.id.orange_warning_small,
            R.id.orange_warning_small_alt
        )
        setWarningIcons(
            state.hasYellowWarning,
            R.drawable.baseline_warning_yellow_24,
            R.id.yellow_warning,
            R.id.yellow_warning_small,
            R.id.yellow_warning_small_alt
        )

        // Responsive Layout sizes
        val minHeightDp = appWidgetManager.getAppWidgetOptions(appWidgetId)
            .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) / context.resources.displayMetrics.density

        val isLarge = minHeightDp > 45
        setVisibility(R.id.top_widget, isLarge)
        setVisibility(R.id.bottom_widget, isLarge)
        setVisibility(R.id.appwidget_location_small, !isLarge)

        val showAltWarn = state.useAltLayout
        val showMainWarn = !state.useAltLayout

        setVisibility(R.id.main_warnings, isLarge && showMainWarn)
        setVisibility(R.id.main_warnings_small, !isLarge && showMainWarn)
        setVisibility(R.id.main_warnings_small_alt, showAltWarn)
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

/** Extension helpers for cleaner RemoteViews logic **/
private fun RemoteViews.setVisibility(id: Int, isVisible: Boolean) {
    setViewVisibility(id, if (isVisible) View.VISIBLE else View.GONE)
}

private fun RemoteViews.setWarningIcons(active: Boolean, resId: Int, vararg ids: Int) {
    ids.forEach { setImageViewResource(it, if (active) resId else 0) }
}
