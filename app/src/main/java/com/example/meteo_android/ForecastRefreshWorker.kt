import android.Manifest
import android.R
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.meteo_android.MainActivity


class ForecastRefreshWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private var notificationManagerCompat: NotificationManagerCompat? = null
    private var notificationBuilder: NotificationCompat.Builder? = null

    init {
        Log.i("INIT", "STARTED")
        val pendingIntent = PendingIntent.getActivity(
            context, 0, Intent(
                context,
                MainActivity::class.java
            ),
            // https://developer.android.com/about/versions/12/behavior-changes-12#pending-intent-mutability
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        notificationBuilder =
            NotificationCompat.Builder(applicationContext, "warnotifier_notification_channel")
                .setSmallIcon(R.drawable.star_on)
                .setContentTitle("notificationTitle")
                .setContentText("notificationText")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)

        notificationManagerCompat = NotificationManagerCompat.from(applicationContext)
        Log.i("INIT", "DONE")
    }

    override fun doWork(): Result {
        val notificationText = "Updating"
        Log.i("MYTAG",notificationText)
        notificationBuilder!!.setContentText(notificationText)
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return Result.failure()
        }
        notificationManagerCompat!!.notify(1, notificationBuilder!!.build())
        return Result.success()
    }
}