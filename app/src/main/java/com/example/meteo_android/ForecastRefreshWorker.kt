package com.example.meteo_android

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters


class ForecastRefreshWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        CityForecastDataDownloader.downloadData("doWork", applicationContext)
        return Result.success()
    }
}