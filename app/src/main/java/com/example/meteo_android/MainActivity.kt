package com.example.meteo_android

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meteo_android.ui.theme.Meteo_androidTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL


@Serializable
data class Forecast(val id: String, val time: String, val vals: List<Double>)

@Serializable
data class Coord(val lat: Double, val lon: Double)

@Serializable
data class City(
    val id: String,
    val name: String,
    val type: String,
    val coords: Coord
)

@Serializable
data class CityForecast(
    val hourly_params: List<List<String>>,
    val daily_params: List<List<String>>,
    val cities: List<City>,
    val hourly_forecast: List<Forecast>,
    val daily_forecast: List<Forecast>,
    val last_updated: String
)

// TODO: look up how to add action for a drag from top
// (and if there's a default spinny loading thing)
// TODO: store both the time of last succesfull download
// and time of last attempt (probably show last attempt, and have
// thing that can be clicked to see last success)
class MainActivity : ComponentActivity() {
    private var cityForecast: CityForecast? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            Meteo_androidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting(cityForecast)
                }
            }
        }
    }

    private suspend fun fetchData() {
        withContext(Dispatchers.IO) {
            try {
                val response = URL("http://10.0.2.2:8000/api/v1/forecast/cities?lat=56.8750&lon=23.8658&radius=10").readText()
                cityForecast = Json.decodeFromString<CityForecast>(response)
            } catch (e: Exception) {
                // https://stackoverflow.com/questions/67771324/kotlin-networkonmainthreadexception-error-when-trying-to-run-inetaddress-isreac
                println(e)
                println(e.message)
                cityForecast = null
            } finally { }
        }

        setContent {
            Meteo_androidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting(cityForecast)
                }
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()

        setContent {
            Meteo_androidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting(cityForecast)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val ote = super.onTouchEvent(event)
        runBlocking {
            fetchData()
        }
        return ote
    }
}

@Composable
fun Greeting(data: CityForecast?, modifier: Modifier = Modifier) {
    val cTemp: Double = data?.hourly_forecast?.get(0)?.vals?.get(1) ?: -999.0
    val dData: List<Forecast> = data?.daily_forecast ?: emptyList()

    Column( // TODO: how does scrolling work? - looks like this caps me to a single screen (?)
        modifier = modifier
            .padding(8.dp)
            .background(Color.Red)
    ) {
        Row(
            modifier = modifier
                .padding(8.dp)
                .background(Color.Magenta)
        ) {
            Text(
                text = "$cTemp°",
                fontSize = 150.sp,
                lineHeight = 300.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(1.0f)
                    .background(Color.Green)
            )
        }
        Row {
            Text(
                text = "24h weather graph",
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
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Meteo_androidTheme {
        Greeting(null)
    }
}
