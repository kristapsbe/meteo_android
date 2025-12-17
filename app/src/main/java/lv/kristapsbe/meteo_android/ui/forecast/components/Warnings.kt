package lv.kristapsbe.meteo_android.ui.forecast.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import lv.kristapsbe.meteo_android.IconMapping
import lv.kristapsbe.meteo_android.R
import kotlin.collections.minus
import kotlin.collections.plus

@Composable
fun ShowWarningInfo() {
    val self = this
    if (displayInfo.value.warnings.isNotEmpty()) {
        val coroutineScope = rememberCoroutineScope()

        ObserveLifecycle { event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    self.showFullWarnings.value = setOf<Int>()
                }
            }
        }

        for (w in displayInfo.value.warnings) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (self.showFullWarnings.value.contains(w.ids[0])) {
                            self.showFullWarnings.value -= w.ids[0]
                        } else {
                            self.showFullWarnings.value += w.ids[0]
                        }
                    },
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .padding(20.dp, 10.dp, 20.dp, 10.dp)
                            .fillMaxWidth(),
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
                                color = Color(getColor(R.color.text_color)),
                                modifier = Modifier
                                    .padding(0.dp, 10.dp, 0.dp, 10.dp),
                            )
                        }
                    }

                    if (self.showFullWarnings.value.contains(w.ids[0])) {
                        Row(
                            modifier = Modifier
                                .padding(0.dp, 0.dp, 0.dp, 10.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(0.15f)
                            ) {
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    w.getFullDescription(selectedLang.value),
                                    fontSize = 15.sp,
                                    color = Color(getColor(R.color.text_color)),
                                )
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier
                .padding(20.dp, 10.dp, 20.dp, 0.dp),
            color = Color(getColor(R.color.light_gray)),
            thickness = 1.dp
        )
    }
}