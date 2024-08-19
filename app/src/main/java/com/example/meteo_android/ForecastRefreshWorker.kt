import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class ForecastRefreshWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    override fun doWork(): Result {
        for(i : Int in 0..60) {
            Log.i("MYTAG","Uploading $i")
        }
        return Result.success()
    }
}