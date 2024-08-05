package com.example.meteo_android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.paint
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
import com.example.meteo_android.ui.theme.Meteo_androidTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import kotlin.random.Random


@Serializable
data class CoordinateData(val lat: Double, val lon: Double)

@Serializable
data class CityData(
    val id: String,
    val name: String,
    val type: String,
    val coords: CoordinateData
)

@Serializable
data class ForecastData(val id: String, val time: Long, val vals: List<Double>)

@Serializable
data class WarningData(val intensity: List<String>, val regions: List<String>, val type: List<String>)

@Serializable
data class CityForecastData(
    val hourly_params: List<List<String>>,
    val daily_params: List<List<String>>,
    val cities: List<CityData>,
    val hourly_forecast: List<ForecastData>,
    val daily_forecast: List<ForecastData>,
    val warnings: List<WarningData>,
    val last_updated: String
)


data class CurrentTemp(
    val temp: Double,
    val city: String,
    val time: LocalDateTime
)

// TODO: look up how to add action for a drag from top
// (and if there's a default spinny loading thing)
// TODO: store both the time of last succesfull download
// and time of last attempt (probably show last attempt, and have
// thing that can be clicked to see last success)
class MainActivity : ComponentActivity() {
    private var cityForecast: CityForecastData? = null
    private var isLoading: Boolean = false
    private var wasLastNegative: Boolean = false

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val currentTemp = mutableStateOf(CurrentTemp(
        -999.0,
        "Temp",
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    ))

    private suspend fun fetchData() {
        if (!isLoading) {
            isLoading = true
            withContext(Dispatchers.IO) {
                try {
                    val randTemp = String.format("%.1f", Random.nextInt(60)-30+Random.nextDouble())

                    var urlString = "http://10.0.2.2:8000/api/v1/forecast/test_ctemp?temp=$randTemp"
                    //urlString = "http://10.0.2.2:8000/api/v1/forecast/cities?lat=56.8750&lon=23.8658&radius=10"

                    val response = URL(urlString).readText()
                    cityForecast = Json.decodeFromString<CityForecastData>(response)

                    var tVal: Double = currentTemp.value.temp
                    var time: LocalDateTime = currentTemp.value.time
                    if ((cityForecast?.hourly_forecast?.size ?: 0) > 0) {
                        tVal = cityForecast?.hourly_forecast?.get(0)?.vals?.get(1) ?: tVal
                        val tmp: String = cityForecast?.hourly_forecast?.get(0)?.time.toString()
                        time = LocalDateTime(
                            tmp.substring(0, 4).toInt(),
                            tmp.substring(4, 6).toInt(),
                            tmp.substring(6, 8).toInt(),
                            tmp.substring(8, 10).toInt(),
                            0, 0, 0
                        )
                    }
                    var cName: String = currentTemp.value.city
                    if ((cityForecast?.hourly_forecast?.size ?: 0) > 0) {
                        val cId = cityForecast?.hourly_forecast?.get(0)?.id ?: ""
                        for (c in cityForecast?.cities ?: emptyList()) {
                            if (c.id == cId) {
                                cName = c.name
                                break
                            }
                        }
                    }
                    currentTemp.value = com.example.meteo_android.CurrentTemp(tVal, cName, time)
                } catch (e: Exception) {
                    // https://stackoverflow.com/questions/67771324/kotlin-networkonmainthreadexception-error-when-trying-to-run-inetaddress-isreac
                    println(e)
                    println(e.message)
                    cityForecast = null
                } finally {
                    isLoading = false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        runBlocking {
            fetchData()
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
    }

    private fun formatDateTime(time: LocalDateTime): String {
        val format = LocalDateTime.Format { byUnicodePattern("yyyy.MM.dd HH:mm") }
        return format.format(time)
    }

    @Composable
    fun AllForecasts(data: CityForecastData?, modifier: Modifier = Modifier) {
        val self = this
        val scrollState = rememberScrollState()
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    Log.d("DEBUG", "$available.y ($wasLastNegative)")
                    if (available.y > 0 && !wasLastNegative) {
                        if (ActivityCompat.checkSelfPermission(
                                self,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                                self,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            val lastLocation = fusedLocationClient.getLastLocation()
                            lastLocation.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("DEBUG", "LAST LOCATION COMPLETED ${task.result}")
                                } else {
                                    Log.d("DEBUG", "LAST LOCATION FAILED")
                                }
                            }
                            Log.d("DEBUG", "LAST LOCATION CALLED")
                        }
                        runBlocking {
                            fetchData()
                        }
                        wasLastNegative = true
                    }
                    return super.onPreScroll(available, source)
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity
                ): Velocity {
                    wasLastNegative = false
                    return super.onPostFling(consumed, available)
                }
            }
        }

        Column(
            modifier = Modifier
                .nestedScroll(nestedScrollConnection)
                .fillMaxSize()
                .padding(8.dp)
                .background(Color.Red)
                .verticalScroll(state = scrollState)
        ) {
            CurrentTemp(modifier)
            Column (
                modifier = modifier
                    .padding(8.dp)
                    .background(Color.Cyan)
            ) {
                DailyForecasts(data)
            }
        }
    }

    @Composable
    fun CurrentTemp(modifier: Modifier) {
        val cTemp by currentTemp

        Row(
            modifier = modifier.height(335.dp)
        ) {
            Box(
                modifier = with (modifier) {
                    fillMaxSize().paint(
                        // Replace with your image id
                        painterResource(id = R.drawable.blue_skies_cumulus_clouds),
                        contentScale = ContentScale.FillBounds
                    )
                }
            ) {
                Column {
                    Text(
                        text = "${cTemp.temp}°",
                        fontSize = 100.sp,
                        lineHeight = 300.sp,
                        textAlign = TextAlign.Center,
                        modifier = modifier
                            .fillMaxWidth(1.0f)
                            .alpha(0.5f)
                            .background(Color.Green)
                    )
                    Text(
                        text = "${cTemp.time.dayOfWeek}, ${formatDateTime(cTemp.time)} (${cTemp.city})",
                        fontSize = 20.sp,
                        color = Color.White,
                        textAlign = TextAlign.Right,
                        modifier = modifier
                            .fillMaxWidth(1.0f)
                            .alpha(0.5f)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .background(Color.Blue)
                    )
                }
            }
        }
    }

    @Composable
    fun DailyForecasts(data: CityForecastData?) {
        val dData: List<ForecastData> = data?.daily_forecast ?: emptyList()

        for (d in dData) {
            val minTemp: Double = d.vals[3]
            val maxTemp: Double = d.vals[2]

            Row {
                Text(
                    text = "$minTemp° - $maxTemp°",
                    fontSize = 40.sp,
                    lineHeight = 80.sp,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(1.0f)
                        .background(Color.Yellow)
                )
            }
        }
    }
}