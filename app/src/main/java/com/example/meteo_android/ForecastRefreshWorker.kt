import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters


class ForecastRefreshWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    init {
        Log.i("INIT", "STARTED")
        // https://developer.android.com/develop/ui/views/notifications
    }

    @SuppressLint("RestrictedApi")
    override fun doWork(): Result {
        Log.i("doWork", "doWork")
        val data = Data.Builder().put("a", "b").build()
        return Result.success(data)
    }
}