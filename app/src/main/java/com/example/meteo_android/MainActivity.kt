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


// classes for visualization data
data class WeatherPictogram(
    val code: Int
) {
    fun getPictogram(): Int {
        return code
    }
}

data class DailyForecast(
    val day: String,
    val rainAmount: Int,
    val stormProb: Double,
    val tempMin: Double,
    val tempMax: Double,
    val pictogramDay: WeatherPictogram,
    val pictogramNight: WeatherPictogram
)

data class HourlyForecast(
    val currTemp: Double,
    val feelsLike: Double,
    val pictogram: WeatherPictogram
)

data class CurrentInfo(
    var hourlyForecast: HourlyForecast?,
    var dailyForecast: DailyForecast?
)

data class DailyInfo(
    var dailyForecasts: List<DailyForecast>
)

data class MetadataInfo(
    var lastUpdated: LocalDateTime?
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

    private val currentInfo = mutableStateOf(CurrentInfo(null, null))
    private val dailyInfo = mutableStateOf(DailyInfo(emptyList()))
    private val metadataInfo = mutableStateOf(MetadataInfo(null))

    private fun getCoordsAndReload() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val lastLocation = fusedLocationClient.getLastLocation()
            lastLocation.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("DEBUG", "LAST LOCATION COMPLETED ${task.result.latitude}  ${task.result.longitude}")

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

    private suspend fun fetchData(lat: Double = 56.8750, lon: Double = 23.8658) {
        if (!isLoading) {
            isLoading = true
            withContext(Dispatchers.IO) {
                try {
                    val randTemp = String.format("%.1f", Random.nextInt(60)-30+Random.nextDouble())

                    var urlString = "http://10.0.2.2:8000/api/v1/forecast/test_ctemp?temp=$randTemp"
                    urlString = "http://10.0.2.2:8000/api/v1/forecast/cities?lat=$lat&lon=$lon&radius=10"

                    val response = URL(urlString).readText()
                    cityForecast = Json.decodeFromString<CityForecastData>(response)

                    var currTempTmp: Double = currentInfo.value.hourlyForecast?.currTemp ?: -999.0
                    var feelsLikeTmp: Double = currentInfo.value.hourlyForecast?.feelsLike ?: -999.0
                    var pictogramTmp: Int = currentInfo.value.hourlyForecast?.pictogram?.code ?: 0
                    if ((cityForecast?.hourly_forecast?.size ?: 0) > 0) {
                        currTempTmp = cityForecast?.hourly_forecast?.get(0)?.vals?.get(1) ?: currTempTmp
                        feelsLikeTmp = cityForecast?.hourly_forecast?.get(0)?.vals?.get(2) ?: feelsLikeTmp
                        pictogramTmp = cityForecast?.hourly_forecast?.get(0)?.vals?.get(0)?.toInt() ?: pictogramTmp
                    }
                    Log.d("DEBUG", "DLOADED --- ${currTempTmp} | ${feelsLikeTmp} | ${pictogramTmp}")

                    currentInfo.value = CurrentInfo(
                        HourlyForecast(
                            currTempTmp, feelsLikeTmp, WeatherPictogram(pictogramTmp)
                        ),
                        null
                    )

                    var tmpDayList = dailyInfo.value.dailyForecasts
                    if ((cityForecast?.daily_forecast?.size ?: 0) > 0) {
                        val tmpNewList = cityForecast?.daily_forecast?.map {
                            DailyForecast(
                                it.time.toString(),
                                it.vals.get(4).toInt(),
                                it.vals.get(5),
                                it.vals.get(3),
                                it.vals.get(2),
                                WeatherPictogram(it.vals.get(7).toInt()),
                                WeatherPictogram(it.vals.get(6).toInt())
                            )
                        } ?: emptyList()
                        tmpDayList = tmpNewList
                    }
                    dailyInfo.value = DailyInfo(tmpDayList)

                    var lastUpdatedTmp: LocalDateTime? = metadataInfo.value.lastUpdated
                    if (cityForecast?.last_updated != null) {
                        lastUpdatedTmp = LocalDateTime(
                            cityForecast?.last_updated!!.substring(0, 4).toInt(),
                            cityForecast?.last_updated!!.substring(4, 6).toInt(),
                            cityForecast?.last_updated!!.substring(6, 8).toInt(),
                            cityForecast?.last_updated!!.substring(8, 10).toInt(),
                            cityForecast?.last_updated!!.substring(10, 12).toInt(),
                            0, 0
                        )
                    }
                    metadataInfo.value = MetadataInfo(lastUpdatedTmp)
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
    }

    private fun formatDateTime(time: LocalDateTime): String {
        val format = LocalDateTime.Format { byUnicodePattern("yyyy.MM.dd HH:mm") }
        return format.format(time)
    }

    @Composable
    fun AllForecasts(data: CityForecastData?, modifier: Modifier = Modifier) {
        val scrollState = rememberScrollState()
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    Log.d("DEBUG", "$available.y ($wasLastNegative)")
                    if (available.y > 0 && !wasLastNegative) {
                        getCoordsAndReload()
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
            ShowCurrentInfo(modifier)
            Column (
                modifier = modifier
                    .padding(8.dp)
                    .background(Color.Cyan)
            ) {
                ShowDailyInfo()
            }
            ShowMetadataInfo()
        }
    }

    @Composable
    fun ShowCurrentInfo(modifier: Modifier) {
        val cInfo by currentInfo
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
                    Row {
                        Text(
                            text = "${cInfo.hourlyForecast?.pictogram?.getPictogram()}",
                            fontSize = 40.sp,
                            lineHeight = 150.sp,
                            textAlign = TextAlign.Center,
                            modifier = modifier
                                .fillMaxWidth(0.33f)
                                .alpha(0.5f)
                                .background(Color.Cyan)
                        )
                        Text(
                            text = "${cInfo.hourlyForecast?.currTemp}",
                            fontSize = 40.sp,
                            lineHeight = 150.sp,
                            textAlign = TextAlign.Center,
                            modifier = modifier
                                .fillMaxWidth(.5f)
                                .alpha(0.5f)
                                .background(Color.Green)
                        )
                        Text(
                            text = "${cInfo.hourlyForecast?.feelsLike}",
                            fontSize = 40.sp,
                            lineHeight = 150.sp,
                            textAlign = TextAlign.Center,
                            modifier = modifier
                                .fillMaxWidth(1.0f)
                                .alpha(0.5f)
                                .background(Color.Magenta)
                        )
                    }
                    Row {
                        Text(
                            text = "${cInfo.dailyForecast?.tempMin}",
                            fontSize = 20.sp,
                            lineHeight = 40.sp,
                            textAlign = TextAlign.Center,
                            modifier = modifier
                                .fillMaxWidth(.25f)
                                .alpha(0.5f)
                                .background(Color.Magenta)
                        )
                        Text(text = "${cInfo.dailyForecast?.tempMax}",
                            fontSize = 20.sp,
                            lineHeight = 40.sp,
                            textAlign = TextAlign.Center,
                            modifier = modifier
                                .fillMaxWidth(.3f)
                                .alpha(0.5f)
                                .background(Color.Gray))
                        Text(text = "${cInfo.dailyForecast?.rainAmount}",
                            fontSize = 20.sp,
                            lineHeight = 40.sp,
                            textAlign = TextAlign.Center,
                            modifier = modifier
                                .fillMaxWidth(.5f)
                                .alpha(0.5f)
                                .background(Color.Yellow))
                        Text(
                            text = "${cInfo.dailyForecast?.stormProb}",
                            fontSize = 20.sp,
                            lineHeight = 40.sp,
                            textAlign = TextAlign.Center,
                            modifier = modifier
                                .fillMaxWidth(1.0f)
                                .alpha(0.5f)
                                .background(Color.Blue)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ShowDailyInfo() {
        val dInfo by dailyInfo
        for (d in dInfo.dailyForecasts) {
            Row {
                Text(text = d.day)
                Text(text = "${d.rainAmount}")
                Text(text = "${d.stormProb}")
                Text(text = "${d.stormProb}")
                Text(text = "${d.pictogramDay.getPictogram()}")
                Text(text = "${d.pictogramNight.getPictogram()}")
                Text(text = "${d.tempMax}")
                Text(text = "${d.tempMin}")
            }
        }
    }

    @Composable
    fun ShowMetadataInfo() {
        val mInfo by metadataInfo
        Row {
            Text(text = "${mInfo.lastUpdated}")
        }
    }
}