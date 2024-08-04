package com.example.meteo_android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meteo_android.ui.theme.Meteo_androidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import kotlin.random.Random


@Serializable
data class Coordinate(val lat: Double, val lon: Double)

@Serializable
data class City(
    val id: String,
    val name: String,
    val type: String,
    val coords: Coordinate
)

@Serializable
data class Forecast(val id: String, val time: Long, val vals: List<Double>)

@Serializable
data class Warning(val intensity: List<String>, val regions: List<String>, val type: List<String>)

@Serializable
data class CityForecast(
    val hourly_params: List<List<String>>,
    val daily_params: List<List<String>>,
    val cities: List<City>,
    val hourly_forecast: List<Forecast>,
    val daily_forecast: List<Forecast>,
    val warnings: List<Warning>,
    val last_updated: String
)

// TODO: look up how to add action for a drag from top
// (and if there's a default spinny loading thing)
// TODO: store both the time of last succesfull download
// and time of last attempt (probably show last attempt, and have
// thing that can be clicked to see last success)
class MainActivity : ComponentActivity() {
    private var cityForecast: CityForecast? = null
    private var isLoading: Boolean = false
    private var wasLastNegative: Int = 0

    private val cTemp = mutableStateOf(-999.0)

    private suspend fun fetchData() {
        if (!isLoading) {
            isLoading = true
            withContext(Dispatchers.IO) {
                try {
                    val randTemp = String.format("%.1f", Random.nextInt(60)-30+Random.nextDouble())
                    val response =
                    //    URL("http://10.0.2.2:8000/api/v1/forecast/cities?lat=56.8750&lon=23.8658&radius=10").readText()
                        URL("http://10.0.2.2:8000/api/v1/forecast/test_ctemp?temp=$randTemp").readText()
                    cityForecast = Json.decodeFromString<CityForecast>(response)

                    var tVal: Double = cTemp.value
                    if ((cityForecast?.hourly_forecast?.size ?: 0) > 0) {
                        tVal = cityForecast?.hourly_forecast?.get(0)?.vals?.get(1) ?: tVal
                    }
                    cTemp.value = tVal
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


    @Composable
    fun AllForecasts(data: CityForecast?, modifier: Modifier = Modifier) {
        var offset by remember {
            mutableStateOf(0f)
        }

        val scrollState = rememberScrollState()
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val delay = 10
                    if ((offset - available.y) < 0 && wasLastNegative < delay) {
                        if (wasLastNegative >= (delay-1)) {
                            runBlocking {
                                fetchData()
                            }
                        }
                        wasLastNegative++
                    }
                    return super.onPreScroll(available, source)
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity
                ): Velocity {
                    wasLastNegative = 0
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
            Row(
                modifier = modifier
                    .padding(8.dp)
                    .background(Color.Magenta)
            ) {
                CurrentTemp()
            }
            Row {
                Text(
                    text = "24h weather",
                    fontSize = 60.sp,
                    lineHeight = 120.sp,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(1.0f)
                        .background(Color.Yellow)
                )
            }
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
    fun CurrentTemp() {
        val showTemp by cTemp
        Text(
            text = "$showTemp°",
            fontSize = 100.sp,
            lineHeight = 300.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth(1.0f)
                .background(Color.Green)
        )
    }

    @Composable
    fun DailyForecasts(data: CityForecast?) {
        val dData: List<Forecast> = data?.daily_forecast ?: emptyList()

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