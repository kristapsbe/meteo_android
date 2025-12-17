package lv.kristapsbe.meteo_android.ui.forecast

import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import lv.kristapsbe.meteo_android.AppPreferences
import lv.kristapsbe.meteo_android.DisplayInfo
import lv.kristapsbe.meteo_android.ForecastRefreshWorker
import lv.kristapsbe.meteo_android.MainActivity
import lv.kristapsbe.meteo_android.MainActivity.Companion.SINGLE_FORECAST_DL_NAME
import lv.kristapsbe.meteo_android.R
import lv.kristapsbe.meteo_android.ui.forecast.components.CurrentInfo
import lv.kristapsbe.meteo_android.ui.forecast.components.DailyInfo
import lv.kristapsbe.meteo_android.ui.forecast.components.HourlyInfo
import lv.kristapsbe.meteo_android.ui.forecast.components.WarningInfo
import lv.kristapsbe.meteo_android.ui.metadata.MetadataInfo
import lv.kristapsbe.meteo_android.ui.settings.Settings


@Composable
fun AllForecasts(
    mainActivity: MainActivity,
    isLoading: MutableState<Boolean>,
    doDisplaySettings: MutableState<Boolean>,
    selectedLang: MutableState<String>,
    showWidgetBackground: MutableState<Boolean>,
    selectedTempType: MutableState<String>,
    doShowAurora: MutableState<Boolean>,
    resources: Resources,
    doFixIconDayNight: MutableState<Boolean>,
    useAltLayout: MutableState<Boolean>,
    useAnimatedIcons: MutableState<Boolean>,
    enableExperimental: MutableState<Boolean>,
    displayInfo: MutableState<DisplayInfo>,
    locationSearchMode: MutableState<Boolean>,
    customLocationName: MutableState<String>,
    prefs: AppPreferences,
    applicationContext: android.content.Context,
    showFullHourly: MutableState<Boolean>,
    showFullDaily: MutableState<List<kotlinx.datetime.LocalDateTime>>
) {
    val scrollState = rememberScrollState()
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && !mainActivity.wasLastScrollNegative) {
                    mainActivity.wasLastScrollNegative = true
                    if (!isLoading.value) {
                        isLoading.value = true
                        val workRequest =
                            OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
                        WorkManager.getInstance(mainActivity).enqueueUniqueWork(
                            SINGLE_FORECAST_DL_NAME,
                            ExistingWorkPolicy.REPLACE,
                            workRequest
                        )
                    }
                }
                return super.onPreScroll(available, source)
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                mainActivity.wasLastScrollNegative = false
                return super.onPostFling(consumed, available)
            }
        }
    }


    // Get the current configuration (including orientation)
    val configuration = LocalConfiguration.current
    var navigationBarHeight = 0
    val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
    if (resourceId > 0) {
        val displayMetrics = Resources.getSystem().displayMetrics
        navigationBarHeight =
            (resources.getDimensionPixelSize(resourceId) / displayMetrics.density).toInt()
    }
    Column(
        modifier = Modifier
            .nestedScroll(nestedScrollConnection)
            .fillMaxSize()
            .background(colorResource(id = R.color.sky_blue))
            .verticalScroll(state = scrollState)
            .padding(
                (if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) navigationBarHeight else 0).dp,
                0.dp
            )
    ) {
        Settings(
            mainActivity,
            prefs,
            applicationContext
        )
        CurrentInfo(
            locationSearchMode,
            customLocationName,
            selectedLang,
            selectedTempType,
            doShowAurora,
            displayInfo,
            prefs,
            applicationContext,
            doFixIconDayNight,
            useAnimatedIcons
        )
        HourlyInfo(
            mainActivity
        )
        WarningInfo(
            mainActivity
        )
        DailyInfo(
            mainActivity
        )
        MetadataInfo(
            selectedLang,
            displayInfo,
            applicationContext,
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) navigationBarHeight else 0
        )
    }
}