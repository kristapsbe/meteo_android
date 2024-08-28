package com.example.meteo_android

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.core.app.ActivityCompat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.meteo_android.ui.theme.Meteo_androidTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit


interface WorkerCallback {
    fun onWorkerResult(result: String?)
}

class MyApplication : Application() {
    var workerCallback: WorkerCallback? = null
}

// TODO: look up how to add action for a drag from top
// (and if there's a default spinny loading thing)
// TODO: store both the time of last succesfull download
// and time of last attempt (probably show last attempt, and have
// thing that can be clicked to see last success)
class MainActivity : ComponentActivity(), WorkerCallback {
    private var cityForecast: CityForecastData? = null
    private var isLoading: Boolean = false
    private var wasLastScrollPosNegative: Boolean = false

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var displayInfo = mutableStateOf(DisplayInfo())

    private fun loadData() {
        try {
            val content = openFileInput(CityForecastDataDownloader.responseFname).bufferedReader().use { it.readText() }
            cityForecast = Json.decodeFromString<CityForecastData>(content)
            displayInfo.value = DisplayInfo(cityForecast)
        } catch (e: Exception) {
            Log.d("DEBUG", "LOADDATA FAILED")
        }
    }

    // TODO: it's possible to get location in background process
    // https://developer.android.com/develop/sensors-and-location/location/background
    // I weather warnings are a sensible reason to ask for this
    private suspend fun fetchData(lat: Double = 56.8750, lon: Double = 23.8658) {
        if (!isLoading) {
            isLoading = true
            withContext(Dispatchers.IO) {
                try {
                    CityForecastDataDownloader.downloadData("fetchData", applicationContext)
                    loadData()
                } catch (e: Exception) {
                    // https://stackoverflow.com/questions/67771324/kotlin-networkonmainthreadexception-error-when-trying-to-run-inetaddress-isreac
                    println(e)
                    println(e.message)
                    cityForecast = null
                } finally {
                    Log.d("DEGUG", "FINALLY FINALLY FINALLY")
                    isLoading = false
                }
            }
        }
    }

    private fun getCoordsAndReload() {
        if ( // TODO: do I have to recheck permissions every time? and what do I do if I'm not allowed access - default to Riga and let the user change cities?
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("DEBUG", "LAST LOCATION COMPLETED")
                    // TODO: not getting lat lon for some reason anymore
                    runBlocking {
                        fetchData()
                        //fetchData(task.result.latitude, task.result.longitude)
                    }
                } else {
                    Log.d("DEBUG", "LAST LOCATION FAILED")
                }
            }
            Log.d("DEBUG", "LAST LOCATION CALLED")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadData()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        runBlocking {
            getCoordsAndReload()
        }
        enableEdgeToEdge()
        setContent {
            Meteo_androidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AllForecasts(cityForecast)
                }
            }
        }

        val app = applicationContext as MyApplication
        app.workerCallback = this

        // TODO
        // https://medium.com/@arjunnarikkuni00/workmanager-using-kotlin-android-c72660afef31
        // looks like I should be able to update stuff from the worker
        // https://stackoverflow.com/questions/59762077/how-can-i-access-objects-from-my-activity-in-a-worker-to-periodically-change-a
        // Assuming WorkManager is set up in your project
        //val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        //val workRequest = PeriodicWorkRequestBuilder<com.example.meteo_android.ForecastRefreshWorker>(15, TimeUnit.SECONDS).setConstraints(constraints).build()
        val workRequest = PeriodicWorkRequestBuilder<ForecastRefreshWorker>(15, TimeUnit.SECONDS).build()
        val workManager = WorkManager.getInstance(this)
        workManager.enqueue(workRequest)
    }

    @Composable
    fun AllForecasts(data: CityForecastData?, modifier: Modifier = Modifier) {
        val scrollState = rememberScrollState()
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    Log.d("DEBUG", "$available.y ($wasLastScrollPosNegative)")
                    if (available.y > 0 && !wasLastScrollPosNegative) {
                        wasLastScrollPosNegative = true
                        getCoordsAndReload()
                    }
                    return super.onPreScroll(available, source)
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    wasLastScrollPosNegative = false
                    return super.onPostFling(consumed, available)
                }
            }
        }

        Column(
            modifier = Modifier
                .nestedScroll(nestedScrollConnection)
                .fillMaxSize()
                .background(Color(0xFF82CAFF)) // Sky Blue
                .verticalScroll(state = scrollState)
        ) {
            ShowCurrentInfo(modifier)
            ShowDailyInfo(modifier)
            ShowMetadataInfo(modifier)
        }
    }

    @Composable
    fun ShowCurrentInfo(modifier: Modifier) {
        Column(
            modifier = modifier
                .height(400.dp)
                .padding(0.dp, 50.dp)
        ) {
            val hForecast: HourlyForecast = displayInfo.value.getTodayForecast()
            Row(
                modifier = modifier
                    .height(260.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = modifier
                        .fillMaxWidth(0.4f)
                        .height(160.dp)
                ) {
                    Image(
                        painterResource(hForecast.pictogram.getPictogram()),
                        contentDescription = "",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    )
                }
                Column(
                    modifier = modifier
                        .fillMaxWidth(1.0f)
                        .padding(20.dp, 0.dp)
                ) {
                    Text(
                        text = "${hForecast.currentTemp}°",
                        fontSize = 100.sp,
                        textAlign = TextAlign.Right,
                        modifier = modifier
                            .fillMaxWidth(1.0f)
                    )
                    Text(
                        text = "feels like ${hForecast.feelsLikeTemp}°",
                        fontSize = 20.sp,
                        lineHeight = 40.sp,
                        textAlign = TextAlign.Center,
                        modifier = modifier
                            .fillMaxWidth(1.0f)
                    )
                }
            }
            if (displayInfo.value.dailyForecasts.isNotEmpty()) {
                val dForecast: DailyForecast = displayInfo.value.dailyForecasts[0]
                Row( // TODO: this is wrong - the daily forecast doesn't contain info for today
                    modifier = modifier
                        .fillMaxHeight(1.0f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${dForecast.tempMin}° to ${dForecast.tempMax}°",
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        modifier = modifier
                            .fillMaxWidth(.333f)
                            .alpha(0.5f)
                            .background(Color.Magenta)
                    )
                    Text(text = "${dForecast.rainAmount} mm",
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = modifier
                            .fillMaxWidth(.5f)
                            .alpha(0.5f)
                            .background(Color.Yellow))
                    Text(
                        text = "${dForecast.rainProb}%",
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        modifier = modifier
                            .fillMaxWidth(1.0f)
                            .alpha(0.5f)
                            .background(Color.Blue)
                    )
                }
            }
        }
    }

    @Composable
    fun ShowDailyInfo(modifier: Modifier) {
        Column(
            modifier = modifier.padding(10.dp, 0.dp)
        ) {
            for (d in displayInfo.value.dailyForecasts) {
                Row(
                    modifier = modifier
                        .fillMaxWidth(1.0f)
                        .height(60.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = d.getDay().substring(0, 1),
                        modifier = modifier.fillMaxWidth(0.03f)
                    )
                    Text(
                        text = "${d.rainAmount} mm",
                        textAlign = TextAlign.Right,
                        modifier = modifier.fillMaxWidth(0.2f)
                    )
                    Text(
                        text = "${d.rainProb}%",
                        textAlign = TextAlign.Right,
                        modifier = modifier.fillMaxWidth(0.2f)
                    )
                    Text(
                        text = "",
                        modifier = modifier.fillMaxWidth(0.05f)
                    )
                    Image(
                        painterResource(d.pictogramDay.getPictogram()),
                        contentDescription = "",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxWidth(0.25f)
                    )
                    Image(
                        painterResource(d.pictogramNight.getPictogram()),
                        contentDescription = "",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxWidth(0.33f)
                    )
                    Text( // TODO: make width consistent
                        text = "${d.tempMin}° to ${d.tempMax}°",
                        textAlign = TextAlign.Right,
                        modifier = modifier.fillMaxWidth(1.0f)
                    )
                }
            }
        }
    }

    @Composable
    fun ShowMetadataInfo(modifier: Modifier) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(10.dp, 0.dp)
        ) {
            Text( // TODO: this is currently the time at which LVGMC last updated their forecast - I should probably show when the server last pulled data as well (?)
                modifier = modifier.fillMaxWidth(),
                text = displayInfo.value.getLastUpdated(),
                textAlign = TextAlign.Right
            )
        }
    }

    override fun onWorkerResult(result: String?) {
        // TODO: loadData() here
        Log.i("WR", "Worker Result: $result")
    }
}