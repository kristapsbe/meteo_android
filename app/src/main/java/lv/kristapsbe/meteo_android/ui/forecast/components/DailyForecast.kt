package lv.kristapsbe.meteo_android.ui.forecast.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import lv.kristapsbe.meteo_android.MainActivity.Companion.convertFromCtoDisplayTemp
import lv.kristapsbe.meteo_android.R


@Composable
fun ShowDailyInfo() {
    val self = this
    Column(
        modifier = Modifier
            .padding(
                20.dp,
                if (displayInfo.value.warnings.isNotEmpty()) 20.dp else 10.dp,
                20.dp,
                20.dp
            )
    ) {
        val coroutineScope = rememberCoroutineScope()

        ObserveLifecycle { event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    self.showFullDaily.value = listOf<LocalDateTime>()
                }
            }
        }

        for (d in displayInfo.value.dailyForecasts) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val tmp = self.showFullDaily.value.toMutableList()
                        if (tmp.contains(d.date)) {
                            tmp.remove(d.date)
                        } else {
                            tmp.add(d.date)
                        }
                        self.showFullDaily.value = tmp.toList()
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp, 0.dp, 0.dp, 10.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
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
                                color = Color(getColor(R.color.text_color)),
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
                                            val dateStr = d.date.toString()
                                            Text( // TODO: don't use substrings to format
                                                text = "${
                                                    dateStr.take(10).takeLast(2)
                                                }.${
                                                    dateStr.take(7).takeLast(2)
                                                }.${dateStr.take(4)}",
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Center,
                                                color = Color(getColor(R.color.text_color)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                            )
                                        }
                                    }
                                    Row {
                                        Text(
                                            text = "${
                                                convertFromCtoDisplayTemp(
                                                    d.tempMin,
                                                    selectedTempType.value
                                                )
                                            } — ${
                                                convertFromCtoDisplayTemp(
                                                    d.tempMax,
                                                    selectedTempType.value
                                                )
                                            }",
                                            textAlign = TextAlign.Center,
                                            color = Color(getColor(R.color.text_color)),
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
                                            ShowIcon(
                                                d.pictogramDay.getAlternateAnimatedPictogram(),
                                                d.pictogramDay.getPictogram(),
                                                Modifier
                                                    .width(70.dp)
                                                    .height(40.dp)
                                                    .padding(3.dp, 3.dp, 3.dp, 0.dp),
                                                Modifier
                                                    .width(70.dp)
                                                    .height(40.dp)
                                                    .padding(3.dp, 3.dp, 3.dp, 0.dp)
                                            )
                                        }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                        ) {
                                            ShowIcon(
                                                d.pictogramNight.getAlternateAnimatedPictogram(),
                                                d.pictogramNight.getPictogram(),
                                                Modifier
                                                    .width(70.dp)
                                                    .height(40.dp)
                                                    .padding(3.dp, 3.dp, 3.dp, 0.dp),
                                                Modifier
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
                                            color = Color(getColor(R.color.text_color)),
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
                                    color = Color(getColor(R.color.text_color)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(),
                            ) {
                                Text(
                                    text = (if (d.rainProb != -999) "${d.rainProb}%" else ""),
                                    textAlign = TextAlign.Right,
                                    fontSize = 10.sp,
                                    color = Color(getColor(R.color.text_color)),
                                    modifier = Modifier.fillMaxWidth()
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
            .padding(20.dp, 20.dp, 20.dp, 20.dp),
        color = Color(getColor(R.color.light_gray)),
        thickness = 1.dp
    )
}