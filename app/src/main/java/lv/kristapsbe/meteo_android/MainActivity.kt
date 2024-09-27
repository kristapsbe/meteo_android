package lv.kristapsbe.meteo_android

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import lv.kristapsbe.meteo_android.CityForecastDataDownloader.Companion.RESPONSE_FILE
import lv.kristapsbe.meteo_android.CityForecastDataDownloader.Companion.loadStringFromStorage
import lv.kristapsbe.meteo_android.ui.theme.Meteo_androidTheme
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


interface WorkerCallback {
    fun onWorkerResult(cityForecast: CityForecastData?)
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
        const val LAST_COORDINATES_FILE = "last_coordinates.json"
        const val LOCKED_LOCATION_FILE = "locked_location"
        const val SELECTED_TEMP_FILE = "selected_temp"
        const val SELECTED_LANG = "selected_lang"
        const val WIDGET_TRANSPARENT = "widget_transparent"

        const val PERIODIC_FORECAST_DL_NAME = "periodic_forecast_download"
        const val SINGLE_FORECAST_DL_NAME = "single_forecast_download"

        const val LANG_EN = "en"
        const val LANG_LV = "lv"

        val selecteTempFieldMapping = hashMapOf(
            "C" to "k C f",
            "" to "k C f", // default
            "K" to "f K c",
            "F" to "c F k",
        )

        // TODO: do a linked list instead?
        val nextTemp = hashMapOf(
            "C" to "K",
            "" to "K", // default
            "K" to "F",
            "F" to "C"
        )

        fun convertFromCtoDisplayTemp(tempC: Int, toConvert: String): String {
            return when (toConvert) {
                "F" -> "${((9.0f/5.0f)*tempC.toFloat()+32.0f).roundToInt()}°" // TODO: add F
                "K" -> "${(tempC.toFloat()+273.15f).roundToInt()}°" // TODO: add K
                else -> "$tempC°"
            }
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var wasLastScrollNegative: Boolean = false

    private var displayInfo = mutableStateOf(DisplayInfo())
    private var isLoading = mutableStateOf(false)
    private var showFullHourly = mutableStateOf(false)
    private var showFullDaily = mutableStateOf(listOf<LocalDateTime>())
    private var showFullWarnings = mutableStateOf(false)
    private var locationSearchMode = mutableStateOf(false)
    private var customLocationName = mutableStateOf("")
    private var selectedTempType = mutableStateOf("")
    private var selectedLang = mutableStateOf("")
    private var isWidgetTransparent = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val lastVersionCode = prefs.getInt("lastVersionCode", -1)
        isWidgetTransparent.value = loadStringFromStorage(applicationContext, WIDGET_TRANSPARENT)

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val currentVersionCode = packageInfo.versionCode

            if (lastVersionCode != currentVersionCode) {
                val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
                WorkManager.getInstance(applicationContext).enqueueUniqueWork(SINGLE_FORECAST_DL_NAME, ExistingWorkPolicy.REPLACE, workRequest)

                // Save the current version code
                prefs.edit().putInt("lastVersionCode", currentVersionCode).apply()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        // Register a callback for back button press
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Minimize the app by moving it to the background
                moveTaskToBack(true)
            }
        }
        // Add the callback to the dispatcher
        onBackPressedDispatcher.addCallback(this, callback)

        val content = loadStringFromStorage(applicationContext, RESPONSE_FILE)
        if (content != "") {
            try {
                displayInfo.value = DisplayInfo(Json.decodeFromString<CityForecastData>(content))
            } catch (e: Exception) {
                Log.e("ERROR", "Failed to load data from storage: $e")
            }
        }
        customLocationName.value = loadStringFromStorage(applicationContext, LOCKED_LOCATION_FILE)
        selectedTempType.value = loadStringFromStorage(applicationContext, SELECTED_TEMP_FILE)
        selectedLang.value = loadStringFromStorage(applicationContext, SELECTED_LANG)
        if (selectedLang.value == "") { // default to EN if nothing's selected
            selectedLang.value = LANG_EN
            applicationContext.openFileOutput(SELECTED_LANG, MODE_PRIVATE).use { fos ->
                fos.write(selectedLang.value.toByteArray())
            }
        }

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
                    WorkManager.getInstance(applicationContext).enqueueUniqueWork(SINGLE_FORECAST_DL_NAME, ExistingWorkPolicy.REPLACE, workRequest)
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
                    WorkManager.getInstance(applicationContext).enqueueUniqueWork(SINGLE_FORECAST_DL_NAME, ExistingWorkPolicy.REPLACE, workRequest)
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
        workManager.enqueueUniquePeriodicWork(PERIODIC_FORECAST_DL_NAME, ExistingPeriodicWorkPolicy.UPDATE, workRequest)
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
                            WorkManager.getInstance(self).enqueueUniqueWork(SINGLE_FORECAST_DL_NAME, ExistingWorkPolicy.REPLACE, workRequest)
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
            ShowCurrentInfo()
            ShowHourlyInfo()
            ShowWarningInfo()
            ShowDailyInfo()
            ShowMetadataInfo()
        }
    }

    @Composable
    fun ShowCurrentInfo() {
        val focusManager = LocalFocusManager.current

        val focusRequester = remember { FocusRequester() }
        if (locationSearchMode.value) {
            LaunchedEffect(Unit) {
                focusRequester.requestFocus() // Automatically request focus when the composable launches
            }
        }

        Column(
            modifier = Modifier
                .padding(0.dp, 50.dp, 0.dp, 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp, 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .clickable {
                            if (isWidgetTransparent.value != "") {
                                isWidgetTransparent.value = ""
                            } else {
                                isWidgetTransparent.value = "true"
                            }
                            applicationContext
                                .openFileOutput(WIDGET_TRANSPARENT, MODE_PRIVATE)
                                .use { fos ->
                                    fos.write(isWidgetTransparent.value.toByteArray())
                                }
                            DisplayInfo.updateWidget(
                                applicationContext,
                                displayInfo.value,
                                selectedLang.value,
                                isWidgetTransparent.value
                            )
                        }
                ) {
                    if (isWidgetTransparent.value == "") {
                        Image(
                            painterResource(R.drawable.baseline_check_box_24),
                            contentDescription = "",
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Image(
                            painterResource(R.drawable.baseline_check_box_outline_blank_24),
                            contentDescription = "",
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = selectedLang.value,
                        modifier = Modifier
                            .clickable {
                                if (selectedLang.value == LANG_EN) {
                                    selectedLang.value = LANG_LV
                                } else {
                                    selectedLang.value = LANG_EN
                                }
                                applicationContext
                                    .openFileOutput(SELECTED_LANG, MODE_PRIVATE)
                                    .use { fos ->
                                        fos.write(selectedLang.value.toByteArray())
                                    }
                                DisplayInfo.updateWidget(
                                    applicationContext,
                                    displayInfo.value,
                                    selectedLang.value,
                                    isWidgetTransparent.value
                                )
                            }
                            .fillMaxWidth(),
                        textAlign = TextAlign.End,
                        color = Color(resources.getColor(R.color.text_color)),
                    )
                }
            }
            val hForecast: HourlyForecast = displayInfo.value.getTodayForecast()
            Row (
                modifier = Modifier
                    .height(120.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(0.45f)
                ) {
                    Image(
                        painterResource(hForecast.pictogram.getPictogram()),
                        contentDescription = "",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.8f)
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = convertFromCtoDisplayTemp(hForecast.currentTemp, selectedTempType.value),
                        fontSize = 100.sp,
                        textAlign = TextAlign.Center,
                        color = Color(resources.getColor(R.color.text_color)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Row (
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.15f)
                        .padding(20.dp, 0.dp, 0.dp, 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painterResource(R.drawable.baseline_location_pin_24),
                        contentDescription = "",
                        modifier = Modifier
                            .clickable {
                                locationSearchMode.value = !locationSearchMode.value
                            }
                            .padding(10.dp)
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth(0.47f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row {
                        Column(
                            modifier = Modifier
                                .padding(0.dp, 0.dp, 5.dp, 0.dp)
                        ) {
                            if (!locationSearchMode.value) {
                                Text(
                                    text = displayInfo.value.city,
                                    fontSize = 20.sp,
                                    textAlign = TextAlign.Left,
                                    color = Color(resources.getColor(R.color.text_color)),
                                    modifier = Modifier
                                        .clickable {
                                            locationSearchMode.value = !locationSearchMode.value
                                        }
                                )
                            } else {
                                BasicTextField(
                                    value = customLocationName.value,
                                    onValueChange = { customLocationName.value = it },
                                    keyboardOptions = KeyboardOptions.Default.copy(
                                        imeAction = ImeAction.Done
                                    ),
                                    textStyle = TextStyle(fontSize = 20.sp, color = Color(resources.getColor(R.color.text_color))),
                                    cursorBrush = SolidColor(Color(resources.getColor(R.color.text_color))),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            locationSearchMode.value = false
                                            focusManager.clearFocus()
                                            applicationContext.openFileOutput(LOCKED_LOCATION_FILE, MODE_PRIVATE).use { fos ->
                                                fos.write(customLocationName.value.toByteArray())
                                            }
                                            val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
                                            WorkManager.getInstance(applicationContext).enqueueUniqueWork(SINGLE_FORECAST_DL_NAME, ExistingWorkPolicy.REPLACE, workRequest)
                                        }
                                    ),
                                    modifier = Modifier.focusRequester(focusRequester)
                                )
                            }
                        }
                        Column {
                            if (customLocationName.value != "") {
                                Image(
                                    painterResource(R.drawable.baseline_clear_24),
                                    contentDescription = "",
                                    modifier = Modifier
                                        .clickable {
                                            customLocationName.value = ""
                                            applicationContext.openFileOutput(LOCKED_LOCATION_FILE, MODE_PRIVATE).use { fos ->
                                                fos.write(customLocationName.value.toByteArray())
                                            }
                                            val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
                                            WorkManager.getInstance(applicationContext).enqueueUniqueWork(SINGLE_FORECAST_DL_NAME, ExistingWorkPolicy.REPLACE, workRequest)
                                        }
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (selectedLang.value == LANG_EN) {
                        Text(
                            text = "${getString(R.string.feels_like_en)} ${convertFromCtoDisplayTemp(hForecast.feelsLikeTemp, selectedTempType.value)}",
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            color = Color(resources.getColor(R.color.text_color)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(0.dp, 0.dp, 20.dp, 0.dp),
                        )
                    } else {

                        Text(
                            text = "${getString(R.string.feels_like_lv)} ${convertFromCtoDisplayTemp(hForecast.feelsLikeTemp, selectedTempType.value)}",
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            color = Color(resources.getColor(R.color.text_color)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(0.dp, 0.dp, 20.dp, 0.dp),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .padding(20.dp, 0.dp)
                    .fillMaxWidth()
            ) {
                if (selectedLang.value == LANG_EN) {
                    Text(
                        text = "Aurora ${displayInfo.value.aurora.prob}% at ${displayInfo.value.aurora.time}",
                        textAlign = TextAlign.Center,
                        color = Color(resources.getColor(R.color.text_color)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(0.dp, 10.dp, 0.dp, 0.dp)
                    )
                } else {
                    Text(
                        text = "Ziemeļblāzma ${displayInfo.value.aurora.prob}% plkst. ${displayInfo.value.aurora.time}",
                        textAlign = TextAlign.Center,
                        color = Color(resources.getColor(R.color.text_color)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(0.dp, 10.dp, 0.dp, 0.dp)
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier
                .padding(20.dp, 20.dp, 20.dp, 10.dp),
            color = Color(resources.getColor(R.color.light_gray)),
            thickness = 1.dp
        )
    }

    @Composable
    fun ShowHourlyInfo() {
        val self = this
        Row (
            modifier = Modifier
                .padding(20.dp, 10.dp, 20.dp, 0.dp)
                .clickable {
                    self.showFullHourly.value = !self.showFullHourly.value
                }
        ) {
            Column(
                modifier = Modifier
                    .width(30.dp)
            ) {
                if (showFullHourly.value) {
                    Row {
                        Text(
                            "",
                            fontSize = 10.sp,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .height(40.dp)
                            .padding(0.dp, 3.dp, 0.dp, 0.dp)
                    ) {
                    }
                    Row {
                        Text("")
                        Image(
                            painterResource(R.drawable.baseline_device_thermostat_24),
                            contentDescription = "",
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Row {
                        Text("")
                        Image(
                            painterResource(R.drawable.baseline_umbrella_24),
                            contentDescription = "",
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Row {
                        Text("")
                        Image(
                            painterResource(R.drawable.baseline_bolt_24),
                            contentDescription = "",
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row {
                                Text("")
                            }
                            Row {
                                Text("")
                            }
                        }
                        Column {
                            Row {
                                Image(
                                    painterResource(R.drawable.baseline_air_24),
                                    contentDescription = "",
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                ) {
                    var prevHDay: String? = null
                    for (h in displayInfo.value.getHourlyForecasts()) {
                        Column (
                            modifier = Modifier
                                .width(90.dp)
                                .padding(10.dp, 0.dp, 10.dp, 0.dp)
                        ) {
                            Text(
                                "${h.time.take(2)}:${h.time.takeLast(2)}",
                                fontSize = 10.sp,
                                color = Color(resources.getColor(R.color.text_color)),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                            Image(
                                painterResource(h.pictogram.getPictogram()),
                                contentDescription = "",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(40.dp)
                                    .padding(3.dp, 3.dp, 3.dp, 0.dp)
                            )
                            Text(
                                convertFromCtoDisplayTemp(h.currentTemp, selectedTempType.value),
                                color = Color(resources.getColor(R.color.text_color)),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                            if (showFullHourly.value) {
                                Text(
                                    "${h.rainAmount} mm",
                                    color = Color(resources.getColor(R.color.text_color)),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    "${h.rainProb}%",
                                    color = Color(resources.getColor(R.color.text_color)),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    "${h.windSpeed} m/s",
                                    color = Color(resources.getColor(R.color.text_color)),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    h.getDirection(selectedLang.value),
                                    color = Color(resources.getColor(R.color.text_color)),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        if (prevHDay != null && prevHDay != h.getDayOfWeek()) {
                            VerticalDivider(
                                color = Color(resources.getColor(R.color.light_gray)),
                                modifier = Modifier.height(80.dp),
                                thickness = 1.dp
                            )
                        }
                        prevHDay = h.getDayOfWeek()
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier
                .padding(20.dp, 20.dp, 20.dp, 0.dp),
            color = Color(resources.getColor(R.color.light_gray)),
            thickness = 1.dp
        )
    }

    @Composable
    fun ShowWarningInfo() {
        val self = this
        if (displayInfo.value.warnings.isNotEmpty()) {
            val displayMetrics = Resources.getSystem().displayMetrics
            val screenWidthPx = displayMetrics.widthPixels  // Get width in pixels
            val screenWidthDp = screenWidthPx / displayMetrics.density  // Convert to dp
            Row (
                modifier = Modifier
                    .padding(20.dp, 20.dp, 20.dp, 0.dp)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .clickable {
                        self.showFullWarnings.value = !self.showFullWarnings.value
                    }
            ) {
                for (w in displayInfo.value.warnings) {
                    Column (
                        modifier = Modifier
                            .width((screenWidthDp - 40).dp)
                            .padding(0.dp, 0.dp, 20.dp, 0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(0.15f)
                            ) {
                                Image(
                                    painterResource(
                                        WeatherPictogram.warningIconMapping[w.intensity]
                                            ?: R.drawable.baseline_warning_yellow_24
                                    ),
                                    contentDescription = "",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .padding(10.dp)
                                )
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    w.type[selectedLang.value] ?: "",
                                    fontSize = 20.sp,
                                    color = Color(resources.getColor(R.color.text_color)),
                                    modifier = Modifier
                                        .padding(0.dp, 10.dp, 0.dp, 10.dp),
                                )
                            }
                        }

                        if (self.showFullWarnings.value) {
                            Row {
                                Column(
                                    modifier = Modifier.fillMaxWidth(0.15f)
                                ) {
                                }
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        w.description[selectedLang.value] ?: "",
                                        fontSize = 15.sp,
                                        color = Color(resources.getColor(R.color.text_color)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(
                modifier = Modifier
                    .padding(20.dp, 20.dp, 20.dp, 0.dp),
                color = Color(resources.getColor(R.color.light_gray)),
                thickness = 1.dp
            )
        }
    }

    @Composable
    fun ShowDailyInfo() {
        val self = this
        Column(
            modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 20.dp)
        ) {
            for (d in displayInfo.value.dailyForecasts) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp, 10.dp, 0.dp, 0.dp)
                        .clickable {
                            val tmp = self.showFullDaily.value.toMutableList()
                            if (tmp.contains(d.date)) {
                                tmp.remove(d.date)
                            } else {
                                tmp.add(d.date)
                            }
                            self.showFullDaily.value = tmp.toList()
                        },
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.15f)
                            .padding(0.dp, 15.dp, 0.dp, 0.dp),
                    ) {
                        Text(
                            text = d.getDayOfWeek(selectedLang.value),
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Left,
                            color = Color(resources.getColor(R.color.text_color)),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.40f)
                            ) {
                                if (showFullDaily.value.contains(d.date)) {
                                    Row {
                                        Text( // TODO: don't use substrings to format
                                            text = "${
                                                d.date.toString().take(10).takeLast(2)
                                            }.${
                                                d.date.toString().take(7).takeLast(2)
                                            }.${d.date.toString().take(4)}",
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center,
                                            color = Color(resources.getColor(R.color.text_color)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                        )
                                    }
                                }
                                Row {
                                    Text(
                                        text = "${convertFromCtoDisplayTemp(d.tempMin, selectedTempType.value)} — ${convertFromCtoDisplayTemp(d.tempMax, selectedTempType.value)}",
                                        textAlign = TextAlign.Center,
                                        color = Color(resources.getColor(R.color.text_color)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.65f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .height(50.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth(0.5f)
                                    ) {
                                        Image(
                                            painterResource(d.pictogramDay.getPictogram()),
                                            contentDescription = "",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .width(70.dp)
                                                .height(40.dp)
                                                .padding(3.dp, 3.dp, 3.dp, 0.dp)
                                        )
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    ) {
                                        Image(
                                            painterResource(d.pictogramNight.getPictogram()),
                                            contentDescription = "",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .width(70.dp)
                                                .height(40.dp)
                                                .padding(3.dp, 3.dp, 3.dp, 0.dp)
                                        )
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .height(50.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = "${d.rainAmount} mm",
                                        textAlign = TextAlign.Right,
                                        color = Color(resources.getColor(R.color.text_color)),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
                if (showFullDaily.value.contains(d.date)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.15f),
                        ) {
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.40f),
                        ) {
                            Text(
                                text = "${d.averageWind} — ${d.maxWind} m/s",
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp,
                                color = Color(resources.getColor(R.color.text_color)),
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                        ) {
                            Text(
                                text = "${d.rainProb}%",
                                textAlign = TextAlign.Right,
                                fontSize = 10.sp,
                                color = Color(resources.getColor(R.color.text_color)),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier
                .padding(20.dp, 20.dp, 20.dp, 20.dp),
            color = Color(resources.getColor(R.color.light_gray)),
            thickness = 1.dp
        )
    }

    @Composable
    fun ShowMetadataInfo() {
        var navigationBarHeight = 0
        val resources = resources
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            val displayMetrics = Resources.getSystem().displayMetrics
            navigationBarHeight = (resources.getDimensionPixelSize(resourceId)/displayMetrics.density).toInt()
        }

        Column(
            modifier = Modifier
                .padding(20.dp, 0.dp, 20.dp, (5+navigationBarHeight).dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.15f)
                        .clickable {
                            selectedTempType.value = nextTemp[selectedTempType.value] ?: ""
                            applicationContext
                                .openFileOutput(SELECTED_TEMP_FILE, MODE_PRIVATE)
                                .use { fos ->
                                    fos.write(selectedTempType.value.toByteArray())
                                }
                            DisplayInfo.updateWidget(
                                applicationContext,
                                displayInfo.value,
                                selectedLang.value,
                                isWidgetTransparent.value
                            )
                        },
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = selecteTempFieldMapping[selectedTempType.value] ?: "",
                        color = Color(resources.getColor(R.color.text_color)),
                        textAlign = TextAlign.Left
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row {
                        if (selectedLang.value == LANG_EN) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 8.sp,
                                text = "${getString(R.string.forecast_issued_en)} ${displayInfo.value.getLastUpdated()}",
                                color = Color(resources.getColor(R.color.text_color)),
                                textAlign = TextAlign.Right
                            )
                        } else {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 8.sp,
                                text = "${getString(R.string.forecast_issued_lv)} ${displayInfo.value.getLastUpdated()}",
                                color = Color(resources.getColor(R.color.text_color)),
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                    Row {
                        if (selectedLang.value == LANG_EN) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 8.sp,
                                text = "${getString(R.string.forecast_downloaded_en)} ${displayInfo.value.getLastDownloaded()}",
                                color = Color(resources.getColor(R.color.text_color)),
                                textAlign = TextAlign.Right
                            )
                        } else {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 8.sp,
                                text = "${getString(R.string.forecast_downloaded_lv)} ${displayInfo.value.getLastDownloaded()}",
                                color = Color(resources.getColor(R.color.text_color)),
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            }
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

    override fun onWorkerResult(cityForecast: CityForecastData?) {
        displayInfo.value = DisplayInfo(cityForecast)
        isLoading.value = false
    }
}