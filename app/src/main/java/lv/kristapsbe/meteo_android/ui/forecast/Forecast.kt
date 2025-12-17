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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import lv.kristapsbe.meteo_android.ForecastRefreshWorker
import lv.kristapsbe.meteo_android.MainActivity.Companion.SINGLE_FORECAST_DL_NAME
import lv.kristapsbe.meteo_android.R
import lv.kristapsbe.meteo_android.ui.settings.Settings
import androidx.compose.ui.res.colorResource
import lv.kristapsbe.meteo_android.ui.metadata.MetadataInfo
import kotlin.String


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
                        val workRequest =
                            OneTimeWorkRequestBuilder<ForecastRefreshWorker>().build()
                        WorkManager.getInstance(self).enqueueUniqueWork(
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
            doDisplaySettings,
            selectedLang,
            showWidgetBackground,
            selectedTempType,
            doShowAurora,
            doFixIconDayNight,
            useAltLayout,
            useAnimatedIcons,
            enableExperimental,
            currentSelectedLang,
            prefs,
            displayInfo,
            applicationContext
        )
        ShowCurrentInfo()
        ShowHourlyInfo()
        ShowWarningInfo()
        ShowDailyInfo()
        MetadataInfo(
            selectedLang,
            displayInfo,
            applicationContext,
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) navigationBarHeight else 0
        )
    }
}