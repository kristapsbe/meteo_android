package com.example.meteo_android

import android.Manifest
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


// TODO: look up how to add action for a drag from top
// (and if there's a default spinny loading thing)
// TODO: store both the time of last succesfull download
// and time of last attempt (probably show last attempt, and have
// thing that can be clicked to see last success)
class MainActivity : ComponentActivity() {
    private val responseFname = "response.json"

    private var cityForecast: CityForecastData? = null
    private var isLoading: Boolean = false
    private var wasLastScrollPosNegative: Boolean = false

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val currentInfo = mutableStateOf(CurrentInfo(null, null))
    private val dailyInfo = mutableStateOf(DailyInfo(emptyList()))
    private val metadataInfo = mutableStateOf(MetadataInfo(null))

    private fun loadData() {
        try {
            val content = openFileInput(responseFname).bufferedReader().use { it.readText() }
            cityForecast = Json.decodeFromString<CityForecastData>(content)

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
            Toast.makeText(this, "Successfully read data", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to read data", Toast.LENGTH_SHORT).show()
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
                    val randTemp = String.format("%.1f", Random.nextInt(60)-30+Random.nextDouble())

                    var urlString = "http://10.0.2.2:8000/api/v1/forecast/test_ctemp?temp=$randTemp"
                    urlString = "http://10.0.2.2:8000/api/v1/forecast/cities?lat=$lat&lon=$lon&radius=10"

                    val response = URL(urlString).readText()
                    openFileOutput(responseFname, MODE_PRIVATE).use { fos ->
                        fos.write(response.toByteArray())
                    }
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
            Toast.makeText(this, "Refreshed weather data", Toast.LENGTH_SHORT).show()
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
                        text = "${dForecast.stormProb.toInt()}%",
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
                        text = "${stringToDateTime(d.day).dayOfWeek}".substring(0, 1),
                        modifier = modifier.fillMaxWidth(0.03f)
                    )
                    Text(
                        text = "${d.rainAmount} mm",
                        textAlign = TextAlign.Right,
                        modifier = modifier.fillMaxWidth(0.2f)
                    )
                    Text(
                        text = "${d.stormProb.toInt()}%",
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