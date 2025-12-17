package lv.kristapsbe.meteo_android

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import lv.kristapsbe.meteo_android.CityForecastDataDownloader.Companion.RESPONSE_FILE
import lv.kristapsbe.meteo_android.CityForecastDataDownloader.Companion.loadStringFromStorage
import lv.kristapsbe.meteo_android.ui.privacy.PrivacyPolicy
import lv.kristapsbe.meteo_android.ui.theme.Meteo_androidTheme
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.ln
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
        const val WEATHER_WARNINGS_CHANNEL_DESCRIPTION =
            "Channel for receiving notifications about severe weather warnings"

        const val AURORA_NOTIFICATION_CHANNEL_ID = "AURORA_NOTIFICATION"
        const val AURORA_NOTIFICATION_CHANNEL_NAME = "Aurora notifications"
        const val AURORA_NOTIFICATION_CHANNEL_DESCRIPTION =
            "Channel for receiving notifications about increases in the probability of observing an aurora"
        const val AURORA_NOTIFICATION_THRESHOLD =
            5 // notify when the probability of an aurora is greater or equal than this

        const val WEATHER_WARNINGS_NOTIFIED_FILE = "warnings_notified.json"

        const val PERIODIC_FORECAST_DL_NAME = "periodic_forecast_download"
        const val SINGLE_FORECAST_DL_NAME = "single_forecast_download"
        const val SINGLE_FORECAST_NO_DL_NAME = "single_forecast_refresh_no_dl"

        const val LANG_EN = "en"
        const val LANG_LV = "lv"

        const val CELSIUS = "C"
        private const val KELVIN = "K"
        private const val FAHRENHEIT = "F"
        private const val DALTON = "D"

        val nextTemp = hashMapOf(
            CELSIUS to KELVIN,
            KELVIN to FAHRENHEIT,
            FAHRENHEIT to DALTON,
            DALTON to CELSIUS
        )

        val nextLang = hashMapOf(
            LANG_EN to LANG_LV,
            LANG_LV to LANG_EN
        )

        fun convertFromCtoDisplayTemp(tempC: Int, toConvert: String): String {
            return when (toConvert) {
                FAHRENHEIT -> "${((9.0f / 5.0f) * tempC.toFloat() + 32.0f).roundToInt()}°"
                KELVIN -> "${(tempC.toFloat() + 273.15f).roundToInt()}°"
                DALTON -> "${(320.55f * ln((tempC.toFloat() + 273.15f) / 273.15f)).roundToInt()}°" // https://www.explainxkcd.com/wiki/index.php/3001:_Temperature_Scales
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
    private lateinit var doShowAurora: MutableState<Boolean>
    private lateinit var doFixIconDayNight: MutableState<Boolean>
    private lateinit var useAnimatedIcons: MutableState<Boolean>
    private lateinit var enableExperimental: MutableState<Boolean>
    private lateinit var customLocationName: MutableState<String>
    private lateinit var privacyPolicyAccepted: MutableState<Boolean>
    private lateinit var locationDisclosureAccepted: MutableState<Boolean>

    private var wasLastScrollNegative: Boolean = false

    private var displayInfo = mutableStateOf(DisplayInfo())
    private var isLoading = mutableStateOf(false)
    private var showFullHourly = mutableStateOf(false)
    var showFullDaily = mutableStateOf(listOf<LocalDateTime>())
    private var showFullWarnings = mutableStateOf(setOf<Int>())
    private var locationSearchMode = mutableStateOf(false)
    private var doDisplaySettings = mutableStateOf(false)
    private var isPrivacyPolicyChecked = mutableStateOf(false)
    private var isLocationDisclosureAccepted = mutableStateOf(false)

    fun privacyPolicyToggle() {
        isPrivacyPolicyChecked.value = !isPrivacyPolicyChecked.value
    }

    fun locationDisclosureAcceptedToggle() {
        isLocationDisclosureAccepted.value = !isLocationDisclosureAccepted.value
    }

    fun acceptPrivacyPolicy() {
        if (isPrivacyPolicyChecked.value && isLocationDisclosureAccepted.value) {
            privacyPolicyAccepted.value = true
            prefs.setBoolean(Preference.PRIVACY_POLICY_ACCEPTED, privacyPolicyAccepted.value)
            locationDisclosureAccepted.value = true
            prefs.setBoolean(
                Preference.LOCATION_DISCLOSURE_ACCEPTED,
                locationDisclosureAccepted.value
            )
            finish()
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // TODO: there's some sort of duplicate splash screen issue - https://developer.android.com/develop/ui/views/launch/splash-screen/migrate
        // TODO: I should fix up the labeling for screen readers - https://support.google.com/accessibility/android/answer/7158690#zippy=

        super.onCreate(savedInstanceState)

        //showFullWarnings.value += intent.getIntExtra("opened_from_notification", -1)

        val currentLocale: Locale = Locale.getDefault()
        val language: String = currentLocale.language

        prefs = AppPreferences(applicationContext)
        selectedLang = mutableStateOf(
            prefs.getString(
                Preference.LANG,
                if (language == LANG_LV) LANG_LV else LANG_EN
            )
        )
        selectedTempType = mutableStateOf(prefs.getString(Preference.TEMP_UNIT, CELSIUS))
        showWidgetBackground =
            mutableStateOf(prefs.getBoolean(Preference.DO_SHOW_WIDGET_BACKGROUND, true))
        useAltLayout = mutableStateOf(prefs.getBoolean(Preference.USE_ALT_LAYOUT, false))
        doShowAurora = mutableStateOf(prefs.getBoolean(Preference.DO_SHOW_AURORA, true))
        doFixIconDayNight = mutableStateOf(prefs.getBoolean(Preference.DO_FIX_ICON_DAY_NIGHT, true))
        useAnimatedIcons = mutableStateOf(prefs.getBoolean(Preference.USE_ANIMATED_ICONS, false))
        enableExperimental =
            mutableStateOf(prefs.getBoolean(Preference.ENABLE_EXPERIMENTAL_FORECASTS, false))
        customLocationName = mutableStateOf(prefs.getString(Preference.FORCE_CURRENT_LOCATION))
        privacyPolicyAccepted =
            mutableStateOf(prefs.getBoolean(Preference.PRIVACY_POLICY_ACCEPTED, false))
        locationDisclosureAccepted =
            mutableStateOf(prefs.getBoolean(Preference.LOCATION_DISCLOSURE_ACCEPTED, false))

        if (privacyPolicyAccepted.value && locationDisclosureAccepted.value) {
            setup()
        }

        enableEdgeToEdge()
        setContent {
            Meteo_androidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (privacyPolicyAccepted.value && locationDisclosureAccepted.value) {
                        AllForecasts()
                    } else {
                        PrivacyPolicy(
                            ::privacyPolicyToggle,
                            ::locationDisclosureAcceptedToggle,
                            ::acceptPrivacyPolicy,
                            isPrivacyPolicyChecked.value,
                            isLocationDisclosureAccepted.value,
                            selectedLang.value,
                            applicationContext
                        )
                    }
                }
            }
        }
    }

    private fun setup() {
        val app = applicationContext as MyApplication
        app.workerCallback = this

        val lastVersionCode = prefs.getLong(Preference.LAST_LONG_VERSION_CODE)
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val currentVersionCode = packageInfo.longVersionCode
            if (lastVersionCode != currentVersionCode) {
                val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
                WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                    SINGLE_FORECAST_DL_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
                prefs.setLong(Preference.LAST_LONG_VERSION_CODE, currentVersionCode)
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
        } else {
            val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
            WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(SINGLE_FORECAST_DL_NAME, ExistingWorkPolicy.REPLACE, workRequest)
        }

        val permissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { _ ->
            val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
            WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(SINGLE_FORECAST_DL_NAME, ExistingWorkPolicy.REPLACE, workRequest)
        }

        val permissions = mutableListOf<String>()
        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) { // don't specifically care which granularity is granted, as long as we've got some sort of location access
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissions.isNotEmpty()) {
            permissionRequest.launch(permissions.toTypedArray())
        }

        createNotificationChannel(
            applicationContext,
            WEATHER_WARNINGS_CHANNEL_ID,
            WEATHER_WARNINGS_CHANNEL_NAME,
            WEATHER_WARNINGS_CHANNEL_DESCRIPTION
        )
        createNotificationChannel(
            applicationContext,
            AURORA_NOTIFICATION_CHANNEL_ID,
            AURORA_NOTIFICATION_CHANNEL_NAME,
            AURORA_NOTIFICATION_CHANNEL_DESCRIPTION
        )

        val workRequest =
            PeriodicWorkRequestBuilder<ForecastRefreshWorker>(20, TimeUnit.MINUTES).build()
        val workManager = WorkManager.getInstance(this)
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_FORECAST_DL_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

        // TODO: I was considering requesting an exemption from battery optimization
        // looks like trying high‑priority FCM messages, scheduled jobs instead of doing this could be better
    }

    private fun createNotificationChannel(
        context: Context,
        channelId: String,
        channelName: String,
        channelDescription: String
    ) {
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = channelDescription
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onWorkerResult(cityForecast: CityForecastData?) {
        displayInfo.value = DisplayInfo(cityForecast)
        isLoading.value = false
    }
}