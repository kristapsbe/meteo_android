package lv.kristapsbe.meteo_android

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import lv.kristapsbe.meteo_android.CityForecastDataDownloader.Companion.RESPONSE_FILE
import lv.kristapsbe.meteo_android.CityForecastDataDownloader.Companion.loadStringFromStorage
import lv.kristapsbe.meteo_android.SunriseSunsetUtils.Companion.calculate
import lv.kristapsbe.meteo_android.ui.theme.Meteo_androidTheme
import java.time.ZonedDateTime
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

        const val AURORA_NOTIFICATION_CHANNEL_ID = "AURORA_NOTIFICATION"
        const val AURORA_NOTIFICATION_CHANNEL_NAME = "Aurora notifications"
        const val AURORA_NOTIFICATION_CHANNEL_DESCRIPTION = "Channel for receiving notifications about increases in the probability of observing an aurora"
        const val AURORA_NOTIFICATION_THRESHOLD = 1 // notify when the probability of an aurora is greater or equal than this

        const val WEATHER_WARNINGS_NOTIFIED_FILE = "warnings_notified.json"
        const val HAS_AURORA_NOTIFIED = "aurora_notified"
        const val AURORA_NOTIF_ID = "aurora_notif_id"

        const val PERIODIC_FORECAST_DL_NAME = "periodic_forecast_download"
        const val SINGLE_FORECAST_DL_NAME = "single_forecast_download"
        const val SINGLE_FORECAST_NO_DL_NAME = "single_forecast_refresh_no_dl"

        const val LANG_EN = "en"
        const val LANG_LV = "lv"

        const val CELSIUS = "C"
        const val KELVIN = "K"
        const val FAHRENHEIT = "F"

        val nextTemp = hashMapOf(
            CELSIUS to KELVIN,
            KELVIN to FAHRENHEIT,
            FAHRENHEIT to CELSIUS
        )

        val nextLang = hashMapOf(
            LANG_EN to LANG_LV,
            LANG_LV to LANG_EN
        )

        fun convertFromCtoDisplayTemp(tempC: Int, toConvert: String): String {
            return when (toConvert) {
                FAHRENHEIT -> "${((9.0f/5.0f)*tempC.toFloat()+32.0f).roundToInt()}°"
                KELVIN -> "${(tempC.toFloat()+273.15f).roundToInt()}°"
                else -> "$tempC°"
            }
        }

        const val DEFAULT_LAT = 56.9730f
        const val DEFAULT_LON = 24.1327f
    }

    private lateinit var prefs: AppPreferences

    private lateinit var selectedLang: MutableState<String>
    private lateinit var selectedTempType: MutableState<String>
    private lateinit var showWidgetBackground: MutableState<Boolean>
    private lateinit var useAltLayout: MutableState<Boolean>
    private lateinit var doAlwaysShowAurora: MutableState<Boolean>
    private lateinit var doAlwaysShowUV: MutableState<Boolean>
    private lateinit var doFixIconDayNight: MutableState<Boolean>
    private lateinit var useAnimatedIcons: MutableState<Boolean>
    private lateinit var customLocationName: MutableState<String>
    private lateinit var privacyPolicyAccepted: MutableState<Boolean>

    private var wasLastScrollNegative: Boolean = false

    private var displayInfo = mutableStateOf(DisplayInfo())
    private var isLoading = mutableStateOf(false)
    private var showFullHourly = mutableStateOf(false)
    private var showFullDaily = mutableStateOf(listOf<LocalDateTime>())
    private var showFullWarnings = mutableStateOf(false)
    private var locationSearchMode = mutableStateOf(false)
    private var doDisplaySettings = mutableStateOf(false)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = AppPreferences(applicationContext)
        selectedLang = mutableStateOf(prefs.getString(Preference.LANG, LANG_EN))
        selectedTempType = mutableStateOf(prefs.getString(Preference.TEMP_UNIT, CELSIUS))
        showWidgetBackground = mutableStateOf(prefs.getBoolean(Preference.DO_SHOW_WIDGET_BACKGROUND, true))
        useAltLayout = mutableStateOf(prefs.getBoolean(Preference.USE_ALT_LAYOUT, false))
        doAlwaysShowAurora = mutableStateOf(prefs.getBoolean(Preference.DO_ALWAYS_SHOW_AURORA, false))
        doAlwaysShowUV = mutableStateOf(prefs.getBoolean(Preference.DO_ALWAYS_SHOW_UV, false))
        doFixIconDayNight = mutableStateOf(prefs.getBoolean(Preference.DO_FIX_ICON_DAY_NIGHT, true))
        useAnimatedIcons = mutableStateOf(prefs.getBoolean(Preference.USE_ANIMATED_ICONS, false))
        customLocationName = mutableStateOf(prefs.getString(Preference.FORCE_CURRENT_LOCATION))
        privacyPolicyAccepted = mutableStateOf(prefs.getBoolean(Preference.PRIVACY_POLICY_ACCEPTED, false))

        val lastVersionCode = prefs.getInt(Preference.LAST_VERSION_CODE)

        try {
            val currentVersionCode = packageManager.getPackageInfo(packageName, 0).versionCode

            if (lastVersionCode != currentVersionCode) {
                val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
                WorkManager.getInstance(applicationContext).enqueueUniqueWork(SINGLE_FORECAST_DL_NAME, ExistingWorkPolicy.REPLACE, workRequest)
                prefs.setInt(Preference.LAST_VERSION_CODE, currentVersionCode)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        // Fixing the back button / gesture
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        val content = loadStringFromStorage(applicationContext, RESPONSE_FILE)
        if (content != "") {
            try {
                displayInfo.value = DisplayInfo(Json.decodeFromString<CityForecastData>(content))
                DisplayInfo.updateWidget(
                    applicationContext,
                    displayInfo.value
                )
            } catch (e: Exception) {
                Log.e("ERROR", "Failed to load data from storage: $e")
            }
        }

        val app = applicationContext as MyApplication
        app.workerCallback = this

        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { _ ->
            val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
            WorkManager.getInstance(applicationContext).enqueueUniqueWork(SINGLE_FORECAST_DL_NAME, ExistingWorkPolicy.REPLACE, workRequest)
        }

        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        createNotificationChannel(applicationContext, WEATHER_WARNINGS_CHANNEL_ID, WEATHER_WARNINGS_CHANNEL_NAME, WEATHER_WARNINGS_CHANNEL_DESCRIPTION)
        createNotificationChannel(applicationContext, AURORA_NOTIFICATION_CHANNEL_ID, AURORA_NOTIFICATION_CHANNEL_NAME, AURORA_NOTIFICATION_CHANNEL_DESCRIPTION)

        // TODO: revisit interval
        val workRequest = PeriodicWorkRequestBuilder<ForecastRefreshWorker>(20, TimeUnit.MINUTES).build()
        val workManager = WorkManager.getInstance(this)
        workManager.enqueueUniquePeriodicWork(PERIODIC_FORECAST_DL_NAME, ExistingPeriodicWorkPolicy.UPDATE, workRequest)

        enableEdgeToEdge()
        setContent {
            Meteo_androidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (privacyPolicyAccepted.value) {
                        AllForecasts()
                    } else {
                        PrivacyPolicy()
                    }
                }
            }
        }
    }

    @Composable
    fun PrivacyPolicy() {
        Column(
            modifier = Modifier
                .background(Color.Gray)
                .padding(40.dp, 80.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(resources.getColor(R.color.sky_blue)))
                    .fillMaxWidth()
            ) {
                val annotatedText = buildAnnotatedString {
                    append("Privacy policy available ")
                    // Add clickable part
                    withStyle(style = SpanStyle(color = Color.Blue)) {
                        pushStringAnnotation(tag = "URL", annotation = "https://meteo.kristapsbe.lv/privacy-policy")
                        append("here")
                        pop()
                    }
                }

                ClickableText(
                    text = annotatedText,
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    onClick = { offset ->
                        annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                applicationContext.startActivity(intent)
                            }
                    }
                )
            }
            Row {
                Button(
                    onClick = {
                        privacyPolicyAccepted.value = true
                        prefs.setBoolean(Preference.PRIVACY_POLICY_ACCEPTED, privacyPolicyAccepted.value)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)  // Set button background color
                ) {
                    Text("Click Me", color = Color.White)  // Set text and its color
                }
            }
        }
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


        // Get the current configuration (including orientation)
        val configuration = LocalConfiguration.current
        var navigationBarHeight = 0
        val resources = resources
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            val displayMetrics = Resources.getSystem().displayMetrics
            navigationBarHeight = (resources.getDimensionPixelSize(resourceId)/displayMetrics.density).toInt()
        }
        Column(
            modifier = Modifier
                .nestedScroll(nestedScrollConnection)
                .fillMaxSize()
                .background(Color(resources.getColor(R.color.sky_blue)))
                .verticalScroll(state = scrollState)
                .padding(
                    (if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) navigationBarHeight else 0).dp,
                    0.dp
                )
        ) {
            ShowSettings()
            ShowCurrentInfo()
            ShowHourlyInfo()
            ShowWarningInfo()
            ShowDailyInfo()
            ShowMetadataInfo(if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) navigationBarHeight else 0)
        }
    }

    @Composable
    fun SettingsEntryBoolean(translation: Translation, preference: Preference, mutableState: MutableState<Boolean>) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 0.dp, 0.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
            ) {
                Text(
                    text = LangStrings.getTranslationString(selectedLang.value, translation),
                    textAlign = TextAlign.Start,
                    color = Color(resources.getColor(R.color.text_color)),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        mutableState.value = !mutableState.value
                        prefs.setBoolean(preference, mutableState.value)
                        DisplayInfo.updateWidget(
                            applicationContext,
                            displayInfo.value
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painterResource(if (mutableState.value) R.drawable.baseline_check_box_24 else R.drawable.baseline_check_box_outline_blank_24),
                    contentDescription = "",
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }

    @Composable
    fun SettingsEntryString(translation: Translation, preference: Preference, mutableState: MutableState<String>, nextEntry: HashMap<String, String>, defaultVal: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 0.dp, 0.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
            ) {
                Text(
                    text = LangStrings.getTranslationString(selectedLang.value, translation),
                    textAlign = TextAlign.Start,
                    color = Color(resources.getColor(R.color.text_color)),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        mutableState.value = nextEntry[mutableState.value] ?: defaultVal
                        prefs.setString(preference, mutableState.value)
                        DisplayInfo.updateWidget(
                            applicationContext,
                            displayInfo.value
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = mutableState.value,
                    color = Color(resources.getColor(R.color.text_color)),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @Composable
    fun ShowSettings() {
        Column(
            modifier = Modifier
                .padding(20.dp, 50.dp, 20.dp, 20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier
                    .clickable {
                        doDisplaySettings.value = !doDisplaySettings.value
                    }
                    .padding(0.dp, 0.dp, 0.dp, 10.dp)
            ) {
                Image(
                    painterResource(R.drawable.baseline_settings_24),
                    contentDescription = "",
                    contentScale = ContentScale.Fit,
                )
            }
            if (doDisplaySettings.value) {
                SettingsEntryString(Translation.SETTINGS_APP_LANGUAGE, Preference.LANG, selectedLang, nextLang, LANG_EN)
                SettingsEntryBoolean(Translation.SETTINGS_WIDGET_TRANSPARENCY, Preference.DO_SHOW_WIDGET_BACKGROUND, showWidgetBackground)
                SettingsEntryString(Translation.SETTINGS_TEMPERATURE_UNIT, Preference.TEMP_UNIT, selectedTempType, nextTemp, CELSIUS)
                SettingsEntryBoolean(Translation.SETTINGS_ALWAYS_DISPLAY_AURORA, Preference.DO_ALWAYS_SHOW_AURORA, doAlwaysShowAurora)
                SettingsEntryBoolean(Translation.SETTINGS_ALWAYS_DISPLAY_UV, Preference.DO_ALWAYS_SHOW_UV, doAlwaysShowUV)
                SettingsEntryBoolean(Translation.SETTINGS_FIX_ICON_DAY_NIGHT, Preference.DO_FIX_ICON_DAY_NIGHT, doFixIconDayNight)
                SettingsEntryBoolean(Translation.SETTINGS_USE_ALT_LAYOUT, Preference.USE_ALT_LAYOUT, useAltLayout)
                SettingsEntryBoolean(Translation.SETTINGS_USE_ANIMATED_ICONS, Preference.USE_ANIMATED_ICONS, useAnimatedIcons)

                HorizontalDivider(
                    modifier = Modifier
                        .padding(0.dp, 10.dp, 0.dp, 20.dp),
                    color = Color(resources.getColor(R.color.light_gray)),
                    thickness = 1.dp
                )
            }
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

        val sunTimes: SunRiseSunSet = calculate(
            displayInfo.value.getTodayForecast().date,
            prefs.getFloat(Preference.LAST_LAT, DEFAULT_LAT).toDouble(),
            prefs.getFloat(Preference.LAST_LON, DEFAULT_LON).toDouble(),
            ZonedDateTime.now().offset.totalSeconds / 3600
        )

        Column {
            val hForecast: HourlyForecast = displayInfo.value.getTodayForecast()
            Row (
                modifier = Modifier
                    .height(120.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(0.45f)
                ) {
                    if (useAnimatedIcons.value) {
                        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(
                            if (doFixIconDayNight.value) hForecast.pictogram.getAlternateAnimatedPictogram(hForecast.date, sunTimes) else hForecast.pictogram.getAlternateAnimatedPictogram())
                        )
                        val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)
                        LottieAnimation(
                            composition = composition,
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                        )
                    } else {
                        Image(
                            painterResource(if (doFixIconDayNight.value) hForecast.pictogram.getPictogram(hForecast.date, sunTimes) else hForecast.pictogram.getPictogram()),
                            contentDescription = "",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.8f)
                        )
                    }
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
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        val maxWidth = maxWidth

                        Row {
                            Column(
                                modifier = Modifier
                                    .padding(0.dp, 0.dp, 5.dp, 0.dp)
                                    .widthIn(max = maxWidth * 0.85f)
                                    .wrapContentWidth()
                            ) {
                                if (!locationSearchMode.value) {
                                    Text(
                                        text = displayInfo.value.city,
                                        fontSize = 20.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
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
                                        maxLines = 1,
                                        textStyle = TextStyle(fontSize = 20.sp, color = Color(resources.getColor(R.color.text_color))),
                                        cursorBrush = SolidColor(Color(resources.getColor(R.color.text_color))),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                locationSearchMode.value = false
                                                focusManager.clearFocus()
                                                prefs.setString(Preference.FORCE_CURRENT_LOCATION, customLocationName.value)
                                                val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
                                                WorkManager.getInstance(applicationContext).enqueueUniqueWork(SINGLE_FORECAST_DL_NAME, ExistingWorkPolicy.REPLACE, workRequest)
                                            }
                                        ),
                                        modifier = Modifier.focusRequester(focusRequester)
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                if (customLocationName.value != "") {
                                    Image(
                                        painterResource(R.drawable.baseline_clear_24),
                                        contentDescription = "",
                                        modifier = Modifier
                                            .clickable {
                                                customLocationName.value = ""
                                                prefs.setString(Preference.FORCE_CURRENT_LOCATION, customLocationName.value)
                                                val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
                                                WorkManager.getInstance(applicationContext).enqueueUniqueWork(SINGLE_FORECAST_DL_NAME, ExistingWorkPolicy.REPLACE, workRequest)
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${LangStrings.getTranslationString(selectedLang.value, Translation.FEELS_LIKE)} ${convertFromCtoDisplayTemp(hForecast.feelsLikeTemp, selectedTempType.value)}",
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        color = Color(resources.getColor(R.color.text_color)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(0.dp, 0.dp, 20.dp, 0.dp),
                    )
                }
            }
            if (doAlwaysShowAurora.value || displayInfo.value.aurora.prob > AURORA_NOTIFICATION_THRESHOLD) {
                Row(
                    modifier = Modifier
                        .padding(20.dp, 0.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = if (selectedLang.value == LANG_EN) "Aurora ${displayInfo.value.aurora.prob}% at ${displayInfo.value.aurora.time}" else "Ziemeļblāzma ${displayInfo.value.aurora.prob}% plkst. ${displayInfo.value.aurora.time}",
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
                    Row(
                        modifier = Modifier
                            .height((24f * Resources.getSystem().displayMetrics.scaledDensity / Resources.getSystem().displayMetrics.density).dp)
                    ) { }
                    Row(
                        modifier = Modifier
                            .height(40.dp)
                    ) { }
                    for (rc in listOf(setOf(1, R.drawable.mono_thermometer), setOf(1, R.drawable.mono_umbrella), setOf(1, R.drawable.mono_thunderstorms), setOf(2, R.drawable.mono_wind), setOf(1, R.drawable.mono_uv_index))) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                // TODO: I've eyeballed the values here atm, should find a better solution
                                .height((4f + 21f * rc.elementAt(0) * Resources.getSystem().displayMetrics.scaledDensity / Resources.getSystem().displayMetrics.density).toInt().dp)
                                .width((4f + 21f * Resources.getSystem().displayMetrics.scaledDensity / Resources.getSystem().displayMetrics.density).toInt().dp)
                        ) {
                            Image(
                                painterResource(rc.elementAt(1)),
                                contentDescription = "",
                                contentScale = ContentScale.Fit,
                            )
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
                    val lat = prefs.getFloat(Preference.LAST_LAT, DEFAULT_LAT).toDouble()
                    val lon = prefs.getFloat(Preference.LAST_LON, DEFAULT_LON).toDouble()
                    val tz = ZonedDateTime.now().offset.totalSeconds / 3600

                    var sunTimes: SunRiseSunSet = calculate(displayInfo.value.getTodayForecast().date, lat, lon, tz)

                    for (h in displayInfo.value.getHourlyForecasts()) {
                        if (prevHDay != null && prevHDay != h.getDayOfWeek()) {
                            VerticalDivider(
                                color = Color(resources.getColor(R.color.light_gray)),
                                modifier = Modifier.height(80.dp),
                                thickness = 1.dp
                            )
                        }
                        if (prevHDay != h.getDayOfWeek()) {
                            sunTimes = calculate(h.date, lat, lon, tz)
                        }
                        prevHDay = h.getDayOfWeek()
                        Column (
                            modifier = Modifier
                                .width(90.dp)
                                .padding(10.dp, 0.dp, 10.dp, 0.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${h.time.take(2)}:${h.time.takeLast(2)}",
                                fontSize = if (useAltLayout.value) 20.sp else 10.sp,
                                color = Color(resources.getColor(R.color.text_color)),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )

                            if (useAnimatedIcons.value) {
                                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(
                                    if (doFixIconDayNight.value) h.pictogram.getAlternateAnimatedPictogram(h.date, sunTimes) else h.pictogram.getAlternateAnimatedPictogram())
                                )
                                val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)
                                LottieAnimation(
                                    composition = composition,
                                    progress = progress,
                                    modifier = Modifier
                                        .width(70.dp)
                                        .height(40.dp)
                                        .padding(3.dp, 3.dp, 3.dp, 0.dp)
                                )
                            } else {
                                Image(
                                    painterResource(if (doFixIconDayNight.value) h.pictogram.getPictogram(h.date, sunTimes) else h.pictogram.getPictogram()),
                                    contentDescription = "",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .width(70.dp)
                                        .height(40.dp)
                                        .padding(3.dp, 3.dp, 3.dp, 0.dp)
                                )
                            }

                            Text(
                                convertFromCtoDisplayTemp(h.currentTemp, selectedTempType.value),
                                color = Color(resources.getColor(R.color.text_color)),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                            if (showFullHourly.value) {
                                for (tVal in listOf("${h.rainAmount} mm", "${h.stormProb}%", "${h.windSpeed} m/s", h.getDirection(selectedLang.value), h.uvIndex.toString())) {
                                    Text(
                                        tVal,
                                        color = Color(resources.getColor(R.color.text_color)),
                                        fontSize = 16.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                        if (h.date.hour == sunTimes.riseH) {
                            Column (
                                modifier = Modifier
                                    .width(90.dp)
                                    .padding(10.dp, 0.dp, 10.dp, 0.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "${h.time.take(2)}:${sunTimes.riseMin}",
                                    fontSize = if (useAltLayout.value) 20.sp else 10.sp,
                                    color = Color(resources.getColor(R.color.text_color)),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                                if (useAnimatedIcons.value) {
                                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(
                                        R.raw.sunrise
                                    ))
                                    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)
                                    LottieAnimation(
                                        composition = composition,
                                        progress = progress,
                                        modifier = Modifier
                                            .width(70.dp)
                                            .height(40.dp)
                                            .padding(3.dp, 3.dp, 3.dp, 0.dp)
                                    )
                                } else {
                                    Image(
                                        painterResource(R.drawable.sunrise),
                                        contentDescription = "",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .width(70.dp)
                                            .height(40.dp)
                                            .padding(3.dp, 3.dp, 3.dp, 0.dp)
                                    )
                                }
                            }
                        } else if (h.date.hour == sunTimes.setH) {
                            Column (
                                modifier = Modifier
                                    .width(90.dp)
                                    .padding(10.dp, 0.dp, 10.dp, 0.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "${h.time.take(2)}:${sunTimes.setMin}",
                                    fontSize = if (useAltLayout.value) 20.sp else 10.sp,
                                    color = Color(resources.getColor(R.color.text_color)),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )

                                if (useAnimatedIcons.value) {
                                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(
                                        R.raw.sunset
                                    ))
                                    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)
                                    LottieAnimation(
                                        composition = composition,
                                        progress = progress,
                                        modifier = Modifier
                                            .width(70.dp)
                                            .height(40.dp)
                                            .padding(3.dp, 3.dp, 3.dp, 0.dp)
                                    )
                                } else {
                                    Image(
                                        painterResource(R.drawable.sunset),
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
                                        IconMapping.warningIconMapping[w.intensity]
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
                                        if (useAnimatedIcons.value) {
                                            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(
                                                d.pictogramDay.getAlternateAnimatedPictogram()
                                            ))
                                            val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)
                                            LottieAnimation(
                                                composition = composition,
                                                progress = progress,
                                                modifier = Modifier
                                                    .width(70.dp)
                                                    .height(40.dp)
                                                    .padding(3.dp, 3.dp, 3.dp, 0.dp)
                                            )
                                        } else {
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
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    ) {
                                        if (useAnimatedIcons.value) {
                                            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(
                                                d.pictogramNight.getAlternateAnimatedPictogram()
                                            ))
                                            val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)
                                            LottieAnimation(
                                                composition = composition,
                                                progress = progress,
                                                modifier = Modifier
                                                    .width(70.dp)
                                                    .height(40.dp)
                                                    .padding(3.dp, 3.dp, 3.dp, 0.dp)
                                            )
                                        } else {
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
                            .fillMaxWidth()
                            .clickable {
                                val tmp = self.showFullDaily.value.toMutableList()
                                if (tmp.contains(d.date)) {
                                    tmp.remove(d.date)
                                } else {
                                    tmp.add(d.date)
                                }
                                self.showFullDaily.value = tmp.toList()
                            },
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
    fun ShowMetadataInfo(navigationBarHeight: Int) {
        Column(
            modifier = Modifier
                .padding(20.dp, 0.dp, 20.dp, (5+navigationBarHeight).dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                fontSize = 8.sp,
                lineHeight = 10.sp,
                text = "${LangStrings.getTranslationString(selectedLang.value, Translation.FORECAST_ISSUED)} ${displayInfo.value.getLastUpdated()}",
                color = Color(resources.getColor(R.color.text_color)),
                textAlign = TextAlign.Right
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                fontSize = 8.sp,
                lineHeight = 10.sp,
                text = "${LangStrings.getTranslationString(selectedLang.value, Translation.FORECAST_DOWNLOADED)} ${displayInfo.value.getLastDownloaded()}",
                color = Color(resources.getColor(R.color.text_color)),
                textAlign = TextAlign.Right
            )
        }
    }

    private fun createNotificationChannel(context: Context, channelId: String, channelName: String, channelDescription: String) {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = channelDescription
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