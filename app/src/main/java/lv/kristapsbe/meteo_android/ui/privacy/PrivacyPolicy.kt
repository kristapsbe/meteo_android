package lv.kristapsbe.meteo_android.ui.privacy

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import lv.kristapsbe.meteo_android.R


@Composable
fun PrivacyPolicy(
    privacyPolicyToggleFun: () -> Unit,
    locationDisclosureAcceptedToggleFun: () -> Unit,
    acceptPrivacyPolicy: () -> Unit,
    isPrivacyPolicyChecked: Boolean,
    isLocationDisclosureAccepted: Boolean,
    selectedLang: String,
    context: Context
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color.LightGray)
            .padding(40.dp, 80.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(colorResource(id = R.color.sky_blue))
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            privacyPolicyToggleFun()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painterResource(if (isPrivacyPolicyChecked) R.drawable.baseline_check_box_24 else R.drawable.baseline_check_box_outline_blank_24),
                        contentDescription = "",
                        contentScale = ContentScale.Fit,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    val annotatedText = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Normal,
                                color = LocalContentColor.current
                            )
                        ) {
                            append(stringResource(R.string.privacy_i_have_read))
                        }

                        withStyle(
                            style = SpanStyle(
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Normal,
                                color = Color.Blue
                            )
                        ) {
                            pushStringAnnotation(
                                tag = "URL",
                                annotation = "https://meteo.kristapsbe.lv/privacy-policy?lang=${selectedLang}"
                            )
                            append(stringResource(R.string.privacy_policy))
                            pop()
                        }
                    }

                    ClickableText(
                        text = annotatedText,
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        onClick = { offset ->
                            annotatedText.getStringAnnotations(
                                tag = "URL",
                                start = offset,
                                end = offset
                            )
                                .firstOrNull()?.let { annotation ->
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        annotation.item.toUri()
                                    )
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                }
                        }
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            locationDisclosureAcceptedToggleFun()
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painterResource(if (isLocationDisclosureAccepted) R.drawable.baseline_check_box_24 else R.drawable.baseline_check_box_outline_blank_24),
                        contentDescription = "",
                        contentScale = ContentScale.Fit,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.disclosure),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Normal,
                        color = LocalContentColor.current,
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                    )
                }
            }
            Row {
                Button(
                    onClick = acceptPrivacyPolicy,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)  // Set button background color
                ) {
                    Text(
                        stringResource(R.string.privacy_continue),
                        color = Color.White
                    )  // Set text and its color
                }
            }
        }
    }
}
