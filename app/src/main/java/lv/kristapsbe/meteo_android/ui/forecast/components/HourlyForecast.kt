package lv.kristapsbe.meteo_android.ui.forecast.components

import android.content.res.Resources
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.launch
import lv.kristapsbe.meteo_android.HourlyForecast
import lv.kristapsbe.meteo_android.MainActivity
import lv.kristapsbe.meteo_android.MainActivity.Companion.convertFromCtoDisplayTemp
import lv.kristapsbe.meteo_android.R
import lv.kristapsbe.meteo_android.ui.util.ObserveLifecycle
import lv.kristapsbe.meteo_android.util.SunRiseSunSet
import lv.kristapsbe.meteo_android.util.SunriseSunsetUtils.Companion.calculate
import java.time.ZoneId
import java.time.ZonedDateTime


@Composable
fun HourlyInfo(
    mainActivity: MainActivity
) {
    Row(
        modifier = Modifier
            .padding(20.dp, 10.dp, 20.dp, 0.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                mainActivity.showFullHourly.value = !mainActivity.showFullHourly.value
            }
    ) {
        Column(
            modifier = Modifier
                .width(30.dp)
        ) {
            if (mainActivity.showFullHourly.value) {
                Row(
                    modifier = Modifier
                        .height((23f * Resources.getSystem().displayMetrics.scaledDensity / Resources.getSystem().displayMetrics.density).dp)
                ) { }
                Row(
                    modifier = Modifier
                        .height(40.dp)
                ) { }
                for (rc in listOf(
                    setOf(1, R.drawable.mono_thermometer),
                    setOf(1, R.drawable.mono_umbrella),
                    setOf(1, R.drawable.mono_thunderstorms),
                    setOf(2, R.drawable.mono_wind),
                    setOf(1, R.drawable.mono_uv_index)
                )) {
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
            val scrollState = rememberScrollState()
            val coroutineScope = rememberCoroutineScope()

            // TODO: check dependency versions
            ObserveLifecycle { event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    coroutineScope.launch {
                        scrollState.scrollTo(0)
                        mainActivity.showFullHourly.value = false
                    }
                }
            }

            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState)
            ) {
                var prevHDay: String? = null
                val zoneId = ZoneId.systemDefault()
                val tz = ZonedDateTime.now(zoneId).offset.totalSeconds / 3600

                var sunTimes: SunRiseSunSet = calculate(
                    mainActivity.displayInfo.value.getTodayForecast().date,
                    mainActivity.displayInfo.value.lat,
                    mainActivity.displayInfo.value.lon,
                    tz // TODO: lock timezone (?)
                )
                var prevH: Int? = null

                for (h in mainActivity.displayInfo.value.getHourlyForecasts()) {
                    if (prevHDay != null && prevHDay != h.getDayOfWeek()) {
                        VerticalDivider(
                            color = colorResource(id = R.color.light_gray),
                            modifier = Modifier.height(80.dp),
                            thickness = 1.dp
                        )
                    }
                    if (prevH == null) {
                        prevH = h.date.hour
                    }
                    if (prevHDay != h.getDayOfWeek()) {
                        sunTimes = calculate(
                            h.date,
                            mainActivity.displayInfo.value.lat,
                            mainActivity.displayInfo.value.lon,
                            tz // TODO: lock timezone (?)
                        )
                    }
                    prevHDay = h.getDayOfWeek()

                    if (sunTimes.riseH >= prevH && sunTimes.riseH < h.date.hour) {
                        Column(
                            modifier = Modifier
                                .width(90.dp)
                                .padding(10.dp, 0.dp, 10.dp, 0.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${sunTimes.riseH}:${sunTimes.riseMin}",
                                fontSize = 20.sp,
                                color = colorResource(id = R.color.text_color),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )

                            ShowIcon(
                                R.raw.sunrise,
                                R.drawable.sunrise,
                                Modifier
                                    .width(70.dp)
                                    .height(40.dp)
                                    .padding(3.dp, 3.dp, 3.dp, 0.dp),
                                Modifier
                                    .width(70.dp)
                                    .height(40.dp)
                                    .padding(3.dp, 3.dp, 3.dp, 0.dp),
                                mainActivity.useAnimatedIcons
                            )
                        }
                    } else if (sunTimes.setH >= prevH && sunTimes.setH < h.date.hour) {
                        Column(
                            modifier = Modifier
                                .width(90.dp)
                                .padding(10.dp, 0.dp, 10.dp, 0.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${sunTimes.setH}:${sunTimes.setMin}",
                                fontSize = 20.sp,
                                color = colorResource(id = R.color.text_color),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )

                            ShowIcon(
                                R.raw.sunset,
                                R.drawable.sunset,
                                Modifier
                                    .width(70.dp)
                                    .height(40.dp)
                                    .padding(3.dp, 3.dp, 3.dp, 0.dp),
                                Modifier
                                    .width(70.dp)
                                    .height(40.dp)
                                    .padding(3.dp, 3.dp, 3.dp, 0.dp),
                                mainActivity.useAnimatedIcons
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .width(90.dp)
                            .padding(10.dp, 0.dp, 10.dp, 0.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${h.time.take(2)}:${h.time.takeLast(2)}",
                            fontSize = 20.sp,
                            color = colorResource(id = R.color.text_color),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )

                        ShowHourlyIcon(
                            h,
                            sunTimes,
                            Modifier
                                .width(70.dp)
                                .height(40.dp)
                                .padding(3.dp, 3.dp, 3.dp, 0.dp),
                            Modifier
                                .width(70.dp)
                                .height(40.dp)
                                .padding(3.dp, 3.dp, 3.dp, 0.dp),
                            mainActivity.doFixIconDayNight,
                            mainActivity.useAnimatedIcons
                        )

                        Text(
                            convertFromCtoDisplayTemp(
                                h.currentTemp,
                                mainActivity.selectedTempType.value
                            ),
                            color = colorResource(id = R.color.text_color),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        if (mainActivity.showFullHourly.value) {
                            for (tVal in listOf(
                                "${h.rainAmount} mm",
                                (if (h.stormProb != -999) "${h.stormProb}%" else ""),
                                "${h.windSpeed} m/s",
                                h.getDirection(mainActivity),
                                (if (h.uvIndex != -999) h.uvIndex.toString() else "")
                            )) {
                                Text(
                                    tVal,
                                    color = colorResource(id = R.color.text_color),
                                    fontSize = 16.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    prevH = h.date.hour
                }
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

@Composable
fun ShowHourlyIcon(
    h: HourlyForecast,
    sunTimes: SunRiseSunSet,
    modifier: Modifier,
    imageModifier: Modifier,
    doFixIconDayNight: MutableState<Boolean>,
    useAnimatedIcons: MutableState<Boolean>
) {
    ShowIcon(
        if (doFixIconDayNight.value) h.pictogram.getAlternateAnimatedPictogram(
            h.date,
            sunTimes
        ) else h.pictogram.getAlternateAnimatedPictogram(),
        if (doFixIconDayNight.value) h.pictogram.getPictogram(
            h.date,
            sunTimes
        ) else h.pictogram.getPictogram(),
        modifier,
        imageModifier,
        useAnimatedIcons
    )
}

@Composable
fun ShowIcon(
    lottieIcon: Int,
    imageIcon: Int,
    modifier: Modifier,
    imageModifier: Modifier,
    useAnimatedIcons: MutableState<Boolean>
) {
    if (useAnimatedIcons.value) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieIcon))
        val progress by animateLottieCompositionAsState(
            composition,
            iterations = LottieConstants.IterateForever
        )
        LottieAnimation(
            composition = composition,
            progress = progress,
            modifier = modifier
        )
    } else {
        Image(
            painterResource(imageIcon),
            contentDescription = "",
            contentScale = ContentScale.Fit,
            modifier = imageModifier
        )
    }
}
