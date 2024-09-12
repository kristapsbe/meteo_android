package lv.kristapsbe.meteo_android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import lv.kristapsbe.meteo_android.ui.theme.Meteo_androidTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.concurrent.TimeUnit
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat


interface WorkerCallback {
    fun onWorkerResult(cityForecast: CityForecastData?, result: String?)
}

class MyApplication : Application() {
    var workerCallback: WorkerCallback? = null
}

class MainActivity : ComponentActivity(), WorkerCallback {
    companion object {
        const val WEATHER_WARNINGS_CHANNEL_ID = "WEATHER_WARNINGS"
        const val WEATHER_WARNINGS_CHANNEL_NAME = "Severe weather warnings"
        const val WEATHER_WARNINGS_CHANNEL_DESCRIPTION = "Channel for receiving notifications about severe weather warnings"

        const val WEATHER_WARNINGS_NOTIFIED_FILE = "warnings_notified.json"
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var wasLastScrollNegative: Boolean = false

    private var displayInfo = mutableStateOf(DisplayInfo())
    private var isLoading = mutableStateOf(false)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        enableEdgeToEdge()
        setContent {
            Meteo_androidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AllForecasts()
                }
            }
        }

        val app = applicationContext as MyApplication
        app.workerCallback = this

        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when { // TODO: do I need to enqueue in both?
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
                    WorkManager.getInstance(applicationContext).enqueue(workRequest)
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
                    WorkManager.getInstance(applicationContext).enqueue(workRequest)
                } else -> {
                    // No location access granted.
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(
                arrayOf(
                    //Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        createNotificationChannel(applicationContext)

        val workRequest = PeriodicWorkRequestBuilder<ForecastRefreshWorker>(15, TimeUnit.MINUTES).build()
        val workManager = WorkManager.getInstance(this)
        workManager.enqueue(workRequest)
    }

    @Composable
    fun AllForecasts() {
        val self = this
        val scrollState = rememberScrollState()
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y > 0 && !wasLastScrollNegative) {
                        wasLastScrollNegative = true
                        if (!isLoading.value) {
                            isLoading.value = true
                            val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
                            WorkManager.getInstance(self).enqueue(workRequest)
                        }

                    }
                    return super.onPreScroll(available, source)
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    wasLastScrollNegative = false
                    return super.onPostFling(consumed, available)
                }
            }
        }

        Column(
            modifier = Modifier
                .nestedScroll(nestedScrollConnection)
                .fillMaxSize()
                .background(Color(resources.getColor(R.color.sky_blue)))
                .verticalScroll(state = scrollState)
        ) {
            if (isLoading.value) {
                CircularProgressIndicator(progress = { 1.0f }, modifier = Modifier.fillMaxWidth())
            }
            ShowCurrentInfo()
            ShowDailyInfo()
            ShowMetadataInfo()
        }
    }

    @Composable
    fun ShowCurrentInfo() {
        Column(
            modifier = Modifier
                .height(300.dp)
                .padding(0.dp, 50.dp, 0.dp, 0.dp)
        ) {
            val hForecast: HourlyForecast = displayInfo.value.getTodayForecast()
            Row (
                modifier = Modifier.fillMaxHeight(0.8f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painterResource(hForecast.pictogram.getPictogram()),
                    contentDescription = "",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .fillMaxHeight(1.0f)
                )
                Text(
                    text = "${hForecast.currentTemp}°",
                    fontSize = 100.sp,
                    textAlign = TextAlign.Center,
                    color = Color(resources.getColor(R.color.text_color)),
                    modifier = Modifier.fillMaxWidth(1.0f)
                )
            }
            Row (
                modifier = Modifier.fillMaxHeight(1.0f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayInfo.value.location.name,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    color = Color(resources.getColor(R.color.text_color)),
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                )
                Text(
                    text = "feels like ${hForecast.feelsLikeTemp}°",
                    fontSize = 20.sp,
                    lineHeight = 40.sp,
                    textAlign = TextAlign.Center,
                    color = Color(resources.getColor(R.color.text_color)),
                    modifier = Modifier
                        .fillMaxWidth(1.0f)
                )
            }
        }
    }

    @Composable
    fun ShowDailyInfo() {
        Column(
            modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 20.dp)
        ) {
            for (d in displayInfo.value.getCurrentDailyForecasts()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(1.0f)
                        .height(60.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = d.getDay().substring(0, 1),
                        color = Color(resources.getColor(R.color.text_color)),
                        modifier = Modifier.fillMaxWidth(0.03f)
                    )
                    Text(
                        text = "${d.rainAmount} mm",
                        textAlign = TextAlign.Right,
                        color = Color(resources.getColor(R.color.text_color)),
                        modifier = Modifier.fillMaxWidth(0.2f)
                    )
                    Text(
                        text = "${d.rainProb}%",
                        textAlign = TextAlign.Right,
                        color = Color(resources.getColor(R.color.text_color)),
                        modifier = Modifier.fillMaxWidth(0.2f)
                    )
                    Text(
                        text = "",
                        color = Color(resources.getColor(R.color.text_color)),
                        modifier = Modifier.fillMaxWidth(0.05f)
                    )
                    Image(
                        painterResource(d.pictogramDay.getPictogram()),
                        contentDescription = "",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxWidth(0.25f).fillMaxHeight(0.8f)
                    )
                    Image(
                        painterResource(d.pictogramNight.getPictogram()),
                        contentDescription = "",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxWidth(0.33f).fillMaxHeight(0.8f)
                    )
                    Text( // TODO: make width consistent
                        text = "${d.tempMin}° to ${d.tempMax}°",
                        textAlign = TextAlign.Right,
                        color = Color(resources.getColor(R.color.text_color)),
                        modifier = Modifier.fillMaxWidth(1.0f)
                    )
                }
            }
        }
    }

    @Composable
    fun ShowMetadataInfo() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp, 0.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = displayInfo.value.getLastUpdated(),
                color = Color(resources.getColor(R.color.text_color)),
                textAlign = TextAlign.Right
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp, 0.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = displayInfo.value.getLastDownloaded(),
                color = Color(resources.getColor(R.color.text_color)),
                textAlign = TextAlign.Right
            )
        }
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(WEATHER_WARNINGS_CHANNEL_ID, WEATHER_WARNINGS_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = WEATHER_WARNINGS_CHANNEL_DESCRIPTION
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onWorkerResult(cityForecast: CityForecastData?, result: String?) {
        Log.i("onWorkerResult", "Worker Result: $result")
        displayInfo.value = DisplayInfo(cityForecast)
        isLoading.value = false
    }
}