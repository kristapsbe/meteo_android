package lv.kristapsbe.meteo_android

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import lv.kristapsbe.meteo_android.CityForecastDataDownloader.Companion.loadStringFromStorage
import lv.kristapsbe.meteo_android.MainActivity.Companion.SELECTED_LANG
import lv.kristapsbe.meteo_android.MainActivity.Companion.WIDGET_TRANSPARENT


class ForecastRefreshWorkerNoDL(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val app = applicationContext as MyApplication
        val callback = app.workerCallback

        runBlocking {
            val selectedLang = loadStringFromStorage(applicationContext, SELECTED_LANG)
            val isWidgetTransparent = loadStringFromStorage(applicationContext, WIDGET_TRANSPARENT)

            val cityForecast = CityForecastDataDownloader.downloadDataLatLon(applicationContext, doDL = false)

            callback?.onWorkerResult(cityForecast)

            if (cityForecast != null) {
                val displayInfo = DisplayInfo(cityForecast)
                DisplayInfo.updateWidget(
                    applicationContext,
                    displayInfo,
                    selectedLang,
                    isWidgetTransparent
                )
            }
        }
        return Result.success()
    }
}