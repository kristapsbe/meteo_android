package lv.kristapsbe.meteo_android

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.os.LocaleListCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import lv.kristapsbe.meteo_android.CityForecastDataDownloader.Companion.RESPONSE_FILE
import lv.kristapsbe.meteo_android.CityForecastDataDownloader.Companion.loadStringFromStorage
import lv.kristapsbe.meteo_android.ui.forecast.AllForecasts
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

class MainActivity : AppCompatActivity(), WorkerCallback {
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

        const val SINGLE_WORK_NAME = "single_forecast_work"
        const val PERIODIC_WORK_NAME = "periodic_forecast_work"
        const val WIDGET_WORK_NAME = "widget_forecast_work"
        const val IS_EXPEDITED_KEY = "is_expedited"

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

    lateinit var selectedLang: MutableState<String>
    lateinit var selectedTempType: MutableState<String>
    lateinit var showWidgetBackground: MutableState<Boolean>
    lateinit var useAltLayout: MutableState<Boolean>
    lateinit var doShowAurora: MutableState<Boolean>
    lateinit var doFixIconDayNight: MutableState<Boolean>
    lateinit var useAnimatedIcons: MutableState<Boolean>
    lateinit var enableExperimental: MutableState<Boolean>
    private lateinit var customLocationName: MutableState<String>
    private lateinit var privacyPolicyAccepted: MutableState<Boolean>
    private lateinit var locationDisclosureAccepted: MutableState<Boolean>

    var wasLastScrollNegative: Boolean = false

    var displayInfo = mutableStateOf(DisplayInfo())
    private var isLoading = mutableStateOf(false)
    var showFullHourly = mutableStateOf(false)
    var showFullDaily = mutableStateOf(listOf<LocalDateTime>())
    var showFullWarnings = mutableStateOf(setOf<Int>())
    private var locationSearchMode = mutableStateOf(false)
    var doDisplaySettings = mutableStateOf(false)
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
        super.onCreate(savedInstanceState)

        // Disable Activity transitions globally for this Activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        prefs = AppPreferences(applicationContext)
        val currentLocale: Locale = Locale.getDefault()
        val language: String = currentLocale.language
        val savedLang = prefs.getString(
            Preference.LANG,
            if (language == LANG_LV) LANG_LV else LANG_EN
        )

        // Initial locale setup
        val appLocales = LocaleListCompat.forLanguageTags(savedLang)
        AppCompatDelegate.setApplicationLocales(appLocales)

        selectedLang = mutableStateOf(savedLang)
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
                        AllForecasts(
                            this,
                            isLoading,
                            selectedLang,
                            selectedTempType,
                            doShowAurora,
                            resources,
                            doFixIconDayNight,
                            useAnimatedIcons,
                            displayInfo,
                            locationSearchMode,
                            customLocationName,
                            prefs,
                            applicationContext
                        )
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // System notified us of a config change (like locale).
        // Update the widget immediately so it reflects the new locale or layout direction.
        DisplayInfo.updateWidget(applicationContext, displayInfo.value)

        val savedLang = prefs.getString(Preference.LANG, LANG_EN)
        if (selectedLang.value != savedLang) {
            selectedLang.value = savedLang
        }
    }

    private fun setup() {
        val app = applicationContext as MyApplication
        app.workerCallback = this

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Schedule periodic refresh for weather warnings and background updates
        val periodicWorkRequest = PeriodicWorkRequestBuilder<ForecastRefreshWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )

        val lastVersionCode = prefs.getLong(Preference.LAST_LONG_VERSION_CODE)
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val currentVersionCode = packageInfo.longVersionCode
            if (lastVersionCode != currentVersionCode) {
                val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setConstraints(constraints)
                    .setInputData(Data.Builder().putBoolean(IS_EXPEDITED_KEY, true).build())
                    .build()
                WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                    SINGLE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
                prefs.setLong(Preference.LAST_LONG_VERSION_CODE, currentVersionCode)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

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
            val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(constraints)
                .setInputData(Data.Builder().putBoolean(IS_EXPEDITED_KEY, true).build())
                .build()
            WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(SINGLE_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)
        }

        val backgroundLocationRequest = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Log.d("PERM", "Background location granted")
            }
        }

        val permissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val workRequest = OneTimeWorkRequestBuilder<ForecastRefreshWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(constraints)
                .setInputData(Data.Builder().putBoolean(IS_EXPEDITED_KEY, true).build())
                .build()
            WorkManager
                .getInstance(applicationContext)
                .enqueueUniqueWork(
                    SINGLE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )

            // Check if foreground location was granted, if so, ask for background
            val locationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            if (locationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backgroundLocationRequest.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }

        val weatherWarningsChannel = NotificationChannel(
            WEATHER_WARNINGS_CHANNEL_ID,
            WEATHER_WARNINGS_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = WEATHER_WARNINGS_CHANNEL_DESCRIPTION
        }

        val auroraNotificationChannel = NotificationChannel(
            AURORA_NOTIFICATION_CHANNEL_ID,
            AURORA_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = AURORA_NOTIFICATION_CHANNEL_DESCRIPTION
        }

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(weatherWarningsChannel)
        notificationManager.createNotificationChannel(auroraNotificationChannel)

        val requiredPermissions = mutableListOf<String>()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requiredPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (requiredPermissions.isNotEmpty()) {
            permissionRequest.launch(requiredPermissions.toTypedArray())
        } else {
            // Permissions already granted, check for background location
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                backgroundLocationRequest.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    override fun onWorkerResult(cityForecast: CityForecastData?) {
        if (cityForecast != null) {
            displayInfo.value = DisplayInfo(cityForecast)
            isLoading.value = false
        }
    }
}
