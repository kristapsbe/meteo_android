package com.example.meteo_android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.getValue
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
import com.example.meteo_android.ui.theme.Meteo_androidTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.serialization.json.Json
import java.net.URL
import kotlin.random.Random


// classes for visualization data
data class WeatherPictogram(
    val code: Int
) {
    fun getPictogram(): Int {
        // TODO: build dynamic icon based on the code
        return R.drawable.example_battery
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
    private var wasLastScrollPosNegative: Boolean = false

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val currentInfo = mutableStateOf(CurrentInfo(null, null))
    private val dailyInfo = mutableStateOf(DailyInfo(emptyList()))
    private val metadataInfo = mutableStateOf(MetadataInfo(null))

    private fun getCoordsAndReload() {
        if ( // TODO: do I have to recheck permissions every time? and what do I do if I'm not allowed access - default to Riga and let the user change cities?
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            val lastLocation = fusedLocationClient.getLastLocation()
            lastLocation.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
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

                    val currHForecast = currentInfo.value.hourlyForecast
                    var currTempTmp: Double = currHForecast?.currTemp ?: -999.0
                    var feelsLikeTmp: Double = currHForecast?.feelsLike ?: -999.0
                    var pictogramTmp: Int = currHForecast?.pictogram?.code ?: 0
                    if ((cityForecast?.hourly_forecast?.size ?: 0) > 0) {
                        val currEntry = cityForecast?.hourly_forecast?.get(0)?.vals
                        currTempTmp = currEntry?.get(1) ?: currTempTmp
                        feelsLikeTmp = currEntry?.get(2) ?: feelsLikeTmp
                        pictogramTmp = currEntry?.get(0)?.toInt() ?: pictogramTmp
                    }
                    var currDaily = currentInfo.value.dailyForecast
                    if ((cityForecast?.daily_forecast?.size ?: 0) > 0) {
                        val currEntry = cityForecast?.daily_forecast?.get(0)
                        val tmpNewDaily = DailyForecast(
                            currEntry?.time.toString(),
                            currEntry?.vals?.get(4)?.toInt() ?: -999,
                            currEntry?.vals?.get(5) ?: -999.0,
                            currEntry?.vals?.get(3) ?: -999.0,
                            currEntry?.vals?.get(2) ?: -999.0,
                            WeatherPictogram((currEntry?.vals?.get(7) ?: -999).toInt()),
                            WeatherPictogram((currEntry?.vals?.get(6) ?: -999).toInt())
                        )
                        currDaily = tmpNewDaily
                    }
                    Log.d("DEBUG", "DLOADED --- ${currTempTmp} | ${feelsLikeTmp} | ${pictogramTmp}")

                    currentInfo.value = CurrentInfo(
                        HourlyForecast(
                            currTempTmp, feelsLikeTmp, WeatherPictogram(pictogramTmp)
                        ),
                        currDaily
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
                        lastUpdatedTmp = stringToDateTime(cityForecast?.last_updated!!)
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

    private fun formatDateTime(time: LocalDateTime?): String {
        val format = LocalDateTime.Format { byUnicodePattern("yyyy.MM.dd HH:mm") }
        return format.format(time ?: LocalDateTime(1972,1,1,0,0,0))
    }

    private fun stringToDateTime(dateString: String): LocalDateTime {
        return LocalDateTime(
            dateString.substring(0, 4).toInt(),
            dateString.substring(4, 6).toInt(),
            dateString.substring(6, 8).toInt(),
            dateString.substring(8, 10).toInt(),
            dateString.substring(10, 12).toInt(),
            0, 0
        )
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
            if (currentInfo.value.hourlyForecast != null) {
                val hForecast: HourlyForecast = currentInfo.value.hourlyForecast!!
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
                            painterResource(hForecast.pictogram.getPictogram() ?: R.drawable.example_battery),
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
                            text = "${hForecast.currTemp.toInt()}°",
                            fontSize = 100.sp,
                            textAlign = TextAlign.Right,
                            modifier = modifier
                                .fillMaxWidth(1.0f)
                        )
                        Text(
                            text = "feels like ${hForecast.feelsLike.toInt()}°",
                            fontSize = 20.sp,
                            lineHeight = 40.sp,
                            textAlign = TextAlign.Center,
                            modifier = modifier
                                .fillMaxWidth(1.0f)
                        )
                    }
                }
            }
            if (currentInfo.value.dailyForecast != null) {
                val dForecast: DailyForecast = currentInfo.value.dailyForecast!!
                Row( // TODO: this is wrong - the daily forecast doesn't contain info for today
                    modifier = modifier
                        .fillMaxHeight(1.0f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${dForecast.tempMin}°-${dForecast.tempMax}°",
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        modifier = modifier
                            .fillMaxWidth(.333f)
                            .alpha(0.5f)
                            .background(Color.Magenta)
                    )
                    Text(text = "${dForecast.rainAmount}",
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = modifier
                            .fillMaxWidth(.5f)
                            .alpha(0.5f)
                            .background(Color.Yellow))
                    Text(
                        text = "${dForecast.stormProb}",
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
            for (d in dailyInfo.value.dailyForecasts) {
                Row(
                    modifier = modifier
                        .fillMaxWidth(1.0f)
                        .height(60.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stringToDateTime(d.day).dayOfWeek}".substring(0, 3),
                        modifier = modifier.fillMaxWidth(0.125f)
                    )
                    Text(
                        text = "${d.rainAmount}",
                        modifier = modifier.fillMaxWidth(0.14f)
                    )
                    Text(
                        text = "${d.stormProb}",
                        modifier = modifier.fillMaxWidth(0.166f)
                    )
                    Text(
                        text = "${d.stormProb}",
                        modifier = modifier.fillMaxWidth(0.2f)
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
                    Text(
                        text = "${d.tempMax}°",
                        textAlign = TextAlign.Right,
                        modifier = modifier.fillMaxWidth(0.5f)
                    )
                    Text(
                        text = "${d.tempMin}°",
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
            modifier = modifier.fillMaxWidth().padding(10.dp, 0.dp)
        ) {
            Text( // TODO: this is currently the time at which LVGMC last updated their forecast - I should probably show when the server last pulled data as well (?)
                modifier = modifier.fillMaxWidth(),
                text = formatDateTime(metadataInfo.value.lastUpdated),
                textAlign = TextAlign.Right
            )
        }
    }
}