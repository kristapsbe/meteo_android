package lv.kristapsbe.meteo_android

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking


class ForecastRefreshWorkerNoDL(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        val app = applicationContext as MyApplication
        val callback = app.workerCallback

        runBlocking {
            val cityForecast = CityForecastDataDownloader.downloadDataLatLon(applicationContext, doDL = false)

            callback?.onWorkerResult(cityForecast)

            if (cityForecast != null) {
                val displayInfo = DisplayInfo(cityForecast)
                DisplayInfo.updateWidget(
                    applicationContext,
                    displayInfo
                )
            }
        }
        return Result.success()
    }
}