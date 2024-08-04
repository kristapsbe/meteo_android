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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
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


data class CurrentTemp(val temp: Double, val city: String)

// TODO: look up how to add action for a drag from top
// (and if there's a default spinny loading thing)
// TODO: store both the time of last succesfull download
// and time of last attempt (probably show last attempt, and have
// thing that can be clicked to see last success)
class MainActivity : ComponentActivity() {
    private var cityForecast: CityForecastData? = null
    private var isLoading: Boolean = false
    private var wasLastNegative: Boolean = false

    private val currentTemp = mutableStateOf(CurrentTemp(-999.0, "Temp"))

    private suspend fun fetchData() {
        if (!isLoading) {
            isLoading = true
            withContext(Dispatchers.IO) {
                try {
                    val randTemp = String.format("%.1f", Random.nextInt(60)-30+Random.nextDouble())
                    val response =
                    //    URL("http://10.0.2.2:8000/api/v1/forecast/cities?lat=56.8750&lon=23.8658&radius=10").readText()
                        URL("http://10.0.2.2:8000/api/v1/forecast/test_ctemp?temp=$randTemp").readText()
                    cityForecast = Json.decodeFromString<CityForecastData>(response)

                    var tVal: Double = currentTemp.value.temp
                    if ((cityForecast?.hourly_forecast?.size ?: 0) > 0) {
                        tVal = cityForecast?.hourly_forecast?.get(0)?.vals?.get(1) ?: tVal
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
                    currentTemp.value = com.example.meteo_android.CurrentTemp(tVal, cName)
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
    fun AllForecasts(data: CityForecastData?, modifier: Modifier = Modifier) {
        val scrollState = rememberScrollState()
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    Log.d("DEBUG", "$available.y ($wasLastNegative)")
                    if (available.y > 0 && !wasLastNegative) {
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
    fun CurrentTemp(modifier: Modifier) {
        val cTemp by currentTemp

        Column(
            modifier = modifier
                .background(Color.Magenta)
        ) {
            Row {
                Text(
                    text = "${cTemp.temp}°",
                    fontSize = 100.sp,
                    lineHeight = 300.sp,
                    textAlign = TextAlign.Center,
                    modifier = modifier
                        .fillMaxWidth(1.0f)
                        .background(Color.Green)
                )
            }
            Row {
                Text(
                    text = "${LocalDate(2024, Month.APRIL, 16).dayOfWeek} (${cTemp.city})",
                    fontSize = 20.sp,
                    color = Color.White,
                    textAlign = TextAlign.Right,
                    modifier = modifier
                        .fillMaxWidth(1.0f)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .background(Color.Blue)
                )
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