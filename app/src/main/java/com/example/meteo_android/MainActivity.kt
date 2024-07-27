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
import java.net.URL


// TODO: look up how to add action for a drag from top 
// (and if there's a default spinny loading thing)
// TODO: store both the time of last succesfull download
// and time of last attempt (probably show last attempt, and have
// thing that can be clicked to see last success)
class MainActivity : ComponentActivity() {
    private var apiResponse: String = "NODATA";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            Meteo_androidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Cheeseday")
                }
            }
        }
    }

    private suspend fun fetchData() {
        withContext(Dispatchers.IO) {
            try {
                apiResponse = URL("http://10.0.2.2:8000/api/v1/forecast/cities?lat=56.8750&lon=23.8658&radius=10").readText()
            } catch (e: Exception) {
                // https://stackoverflow.com/questions/67771324/kotlin-networkonmainthreadexception-error-when-trying-to-run-inetaddress-isreac
                println(e)
                println(e.message)
                apiResponse = "API ERROR"
            } finally { }
        }

        setContent {
            Meteo_androidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting(apiResponse)
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
                    Greeting("OUE Bday")
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
fun Greeting(text: String, modifier: Modifier = Modifier) {
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
                text = "19°",
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
            Row {
                Text(
                    text = "1r from $text",
                    fontSize = 40.sp,
                    lineHeight = 80.sp,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(1.0f)
                        .background(Color.Yellow)
                )
            }
            Row {
                Text(
                    text = "2r from $text",
                    fontSize = 40.sp,
                    lineHeight = 80.sp,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(1.0f)
                        .background(Color.Yellow)
                )
            }
            Row {
                Text(
                    text = "3r from $text",
                    fontSize = 40.sp,
                    lineHeight = 80.sp,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(1.0f)
                        .background(Color.Yellow)
                )
            }
            Row {
                Text(
                    text = "4r from $text",
                    fontSize = 40.sp,
                    lineHeight = 80.sp,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(1.0f)
                        .background(Color.Yellow)
                )
            }
            Row {
                Text(
                    text = "5r from $text",
                    fontSize = 40.sp,
                    lineHeight = 80.sp,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(1.0f)
                        .background(Color.Yellow)
                )
            }
            Row {
                Text(
                    text = "6r from $text",
                    fontSize = 40.sp,
                    lineHeight = 80.sp,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(1.0f)
                        .background(Color.Yellow)
                )
            }
            Row {
                Text(
                    text = "7r from $text",
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Meteo_androidTheme {
        Greeting("Birthday")
    }
}
