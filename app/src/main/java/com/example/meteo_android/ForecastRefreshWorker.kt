import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters


class ForecastRefreshWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    init {
        Log.i("INIT", "STARTED")
        // https://developer.android.com/develop/ui/views/notifications
    }

    override fun doWork(): Result {
        return Result.success()
    }
}