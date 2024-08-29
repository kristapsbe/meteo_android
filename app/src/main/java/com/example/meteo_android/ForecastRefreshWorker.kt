package com.example.meteo_android

import android.content.Context
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

        // TODO: push notifications if weather warnings appear, use a file to keep track of what we've already warned about?
        return Result.success()
    }
}