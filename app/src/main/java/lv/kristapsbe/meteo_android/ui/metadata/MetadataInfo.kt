package lv.kristapsbe.meteo_android.ui.metadata

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import lv.kristapsbe.meteo_android.DisplayInfo
import lv.kristapsbe.meteo_android.R


@Composable
fun MetadataInfo(
    selectedLang: MutableState<String>,
    displayInfo: MutableState<DisplayInfo>,
    applicationContext: Context,
    navigationBarHeight: Int
) {
    Column(
        modifier = Modifier
            .padding(20.dp, 0.dp, 20.dp, 0.dp)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            fontSize = 8.sp,
            lineHeight = 10.sp,
            text = "${stringResource(R.string.forecast_issued)} ${displayInfo.value.getLastUpdated()}",
            color = colorResource(id = R.color.text_color),
            textAlign = TextAlign.Right
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            fontSize = 8.sp,
            lineHeight = 10.sp,
            text = "${stringResource(R.string.forecast_downloaded)} ${displayInfo.value.getLastDownloaded()}",
            color = colorResource(id = R.color.text_color),
            textAlign = TextAlign.Right
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            fontSize = 8.sp,
            lineHeight = 10.sp,
            text = "${stringResource(R.string.forecast_downloaded_no_skip)} ${displayInfo.value.getLastDownloadedNoSkip()}",
            color = colorResource(id = R.color.text_color),
            textAlign = TextAlign.Right
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End // This aligns the content to the right
        ) {
            val annotatedText = buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color.LightGray, fontSize = 8.sp)) {
                    pushStringAnnotation(
                        tag = "URL",
                        annotation = "https://meteo.kristapsbe.lv/attribution?lang=${selectedLang.value}"
                    )
                    append(stringResource(R.string.data_sources))
                    pop()
                }
            }

            ClickableText(
                text = annotatedText,
                onClick = { offset ->
                    annotatedText.getStringAnnotations(
                        tag = "URL",
                        start = offset,
                        end = offset
                    )
                        .firstOrNull()?.let { annotation ->
                            val intent = Intent(Intent.ACTION_VIEW, annotation.item.toUri())
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            applicationContext.startActivity(intent)
                        }
                }
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier
            .padding(20.dp, 20.dp, 20.dp, 20.dp),
        color = colorResource(id = R.color.light_gray),
        thickness = 1.dp
    )
    Column(
        modifier = Modifier
            .padding(20.dp, 0.dp, 20.dp, (5 + navigationBarHeight).dp)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            fontSize = 8.sp,
            lineHeight = 10.sp,
            text = stringResource(R.string.disclosure),
            color = colorResource(id = R.color.text_color),
            textAlign = TextAlign.Right
        )
    }
}
