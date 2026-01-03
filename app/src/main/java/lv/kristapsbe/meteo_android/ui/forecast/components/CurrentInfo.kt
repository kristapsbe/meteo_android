package lv.kristapsbe.meteo_android.ui.forecast.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import lv.kristapsbe.meteo_android.DisplayInfo
import lv.kristapsbe.meteo_android.HourlyForecast
import lv.kristapsbe.meteo_android.MainActivity.Companion.AURORA_NOTIFICATION_THRESHOLD
import lv.kristapsbe.meteo_android.MainActivity.Companion.IS_EXPEDITED_KEY
import lv.kristapsbe.meteo_android.MainActivity.Companion.LANG_EN
import lv.kristapsbe.meteo_android.MainActivity.Companion.SINGLE_WORK_NAME
import lv.kristapsbe.meteo_android.MainActivity.Companion.convertFromCtoDisplayTemp
import lv.kristapsbe.meteo_android.R
import lv.kristapsbe.meteo_android.data.AppPreferences
import lv.kristapsbe.meteo_android.data.Preference
import lv.kristapsbe.meteo_android.util.SunRiseSunSet
import lv.kristapsbe.meteo_android.util.SunriseSunsetUtils.Companion.calculate
import lv.kristapsbe.meteo_android.worker.ForecastRefreshWorker
import java.time.ZoneId
import java.time.ZonedDateTime


@Composable
fun CurrentInfo(
    locationSearchMode: MutableState<Boolean>,
    customLocationName: MutableState<String>,
    selectedLang: MutableState<String>,
    selectedTempType: MutableState<String>,
    doShowAurora: MutableState<Boolean>,
    displayInfo: MutableState<DisplayInfo>,
    prefs: AppPreferences,
    applicationContext: Context,
    doFixIconDayNight: MutableState<Boolean>,
    useAnimatedIcons: MutableState<Boolean>
) {
    val focusManager = LocalFocusManager.current

    val focusRequester = remember { FocusRequester() }
    if (locationSearchMode.value) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus() // Automatically request focus when the composable launches
        }
    }

    val zoneId = ZoneId.systemDefault()
    val sunTimes: SunRiseSunSet = calculate(
        displayInfo.value.getTodayForecast().date,
        displayInfo.value.lat,
        displayInfo.value.lon,
        ZonedDateTime.now(zoneId).offset.totalSeconds / 3600 // TODO: lock timezone (?)
    )

    Column {
        val hForecast: HourlyForecast = displayInfo.value.getTodayForecast()
        Row(
            modifier = Modifier
                .height(120.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(0.45f)
            ) {
                ShowHourlyIcon(
                    hForecast,
                    sunTimes,
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.8f),
                    doFixIconDayNight,
                    useAnimatedIcons
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = convertFromCtoDisplayTemp(
                        hForecast.currentTemp,
                        selectedTempType.value
                    ),
                    fontSize = 100.sp,
                    textAlign = TextAlign.Center,
                    color = colorResource(id = R.color.text_color),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Row(
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
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
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
                                    color = colorResource(id = R.color.text_color),
                                    modifier = Modifier
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
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
                                    textStyle = TextStyle(
                                        fontSize = 20.sp,
                                        color = colorResource(id = R.color.text_color)
                                    ),
                                    cursorBrush = SolidColor(colorResource(id = R.color.text_color)),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            locationSearchMode.value = false
                                            focusManager.clearFocus()
                                            prefs.setString(
                                                Preference.FORCE_CURRENT_LOCATION,
                                                customLocationName.value
                                            )
                                            val workRequest =
                                                OneTimeWorkRequestBuilder<ForecastRefreshWorker>()
                                                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                                                    .setInputData(
                                                        Data.Builder()
                                                            .putBoolean(IS_EXPEDITED_KEY, true)
                                                            .build()
                                                    )
                                                    .build()
                                            WorkManager.getInstance(applicationContext)
                                                .enqueueUniqueWork(
                                                    SINGLE_WORK_NAME,
                                                    ExistingWorkPolicy.REPLACE,
                                                    workRequest
                                                )
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
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            customLocationName.value = ""
                                            prefs.setString(
                                                Preference.FORCE_CURRENT_LOCATION,
                                                customLocationName.value
                                            )
                                            val workRequest =
                                                OneTimeWorkRequestBuilder<ForecastRefreshWorker>()
                                                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                                                    .setInputData(
                                                        Data.Builder()
                                                            .putBoolean(IS_EXPEDITED_KEY, true)
                                                            .build()
                                                    )
                                                    .build()
                                            WorkManager.getInstance(applicationContext)
                                                .enqueueUniqueWork(
                                                    SINGLE_WORK_NAME,
                                                    ExistingWorkPolicy.REPLACE,
                                                    workRequest
                                                )
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
                    text = "${stringResource(R.string.feels_like)} ${
                        convertFromCtoDisplayTemp(
                            hForecast.feelsLikeTemp,
                            selectedTempType.value
                        )
                    }",
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    color = colorResource(id = R.color.text_color),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp, 0.dp, 20.dp, 0.dp),
                )
            }
        }
        if (doShowAurora.value && displayInfo.value.aurora.prob >= AURORA_NOTIFICATION_THRESHOLD) {
            Row(
                modifier = Modifier
                    .padding(20.dp, 0.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = if (selectedLang.value == LANG_EN) "Aurora ${displayInfo.value.aurora.prob}% at ${displayInfo.value.aurora.time}" else "Ziemeļblāzma ${displayInfo.value.aurora.prob}% plkst. ${displayInfo.value.aurora.time}",
                    textAlign = TextAlign.Center,
                    color = colorResource(id = R.color.text_color),
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
        color = colorResource(id = R.color.light_gray),
        thickness = 1.dp
    )
}
