package lv.kristapsbe.meteo_android.ui.settings

import android.content.Context
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import lv.kristapsbe.meteo_android.AppPreferences
import lv.kristapsbe.meteo_android.DisplayInfo
import lv.kristapsbe.meteo_android.LangStrings
import lv.kristapsbe.meteo_android.MainActivity
import lv.kristapsbe.meteo_android.MainActivity.Companion.CELSIUS
import lv.kristapsbe.meteo_android.MainActivity.Companion.LANG_EN
import lv.kristapsbe.meteo_android.MainActivity.Companion.nextLang
import lv.kristapsbe.meteo_android.MainActivity.Companion.nextTemp
import lv.kristapsbe.meteo_android.Preference
import lv.kristapsbe.meteo_android.R
import lv.kristapsbe.meteo_android.Translation


@Composable
fun Settings(
    mainActivity: MainActivity,
    prefs: AppPreferences,
    applicationContext: Context
) {
    Column(
        modifier = Modifier
            .padding(20.dp, 50.dp, 20.dp, 20.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    mainActivity.doDisplaySettings.value = !mainActivity.doDisplaySettings.value
                }
                .padding(0.dp, 0.dp, 0.dp, 10.dp)
        ) {
            Image(
                painterResource(R.drawable.baseline_settings_24),
                contentDescription = "",
                contentScale = ContentScale.Fit,
            )
        }
        if (mainActivity.doDisplaySettings.value) {
            SettingsEntryString(
                Translation.SETTINGS_APP_LANGUAGE,
                Preference.LANG,
                mainActivity.selectedLang,
                nextLang,
                LANG_EN,
                prefs,
                mainActivity,
                applicationContext
            )
            SettingsEntryBoolean(
                Translation.SETTINGS_WIDGET_TRANSPARENCY,
                Preference.DO_SHOW_WIDGET_BACKGROUND,
                mainActivity.showWidgetBackground,
                prefs,
                mainActivity,
                applicationContext
            )
            SettingsEntryString(
                Translation.SETTINGS_TEMPERATURE_UNIT,
                Preference.TEMP_UNIT,
                mainActivity.selectedTempType,
                nextTemp,
                CELSIUS,
                prefs,
                mainActivity,
                applicationContext
            )
            SettingsEntryBoolean(
                Translation.SETTINGS_DISPLAY_AURORA,
                Preference.DO_SHOW_AURORA,
                mainActivity.doShowAurora,
                prefs,
                mainActivity,
                applicationContext
            )
            SettingsEntryBoolean(
                Translation.SETTINGS_FIX_ICON_DAY_NIGHT,
                Preference.DO_FIX_ICON_DAY_NIGHT,
                mainActivity.doFixIconDayNight,
                prefs,
                mainActivity,
                applicationContext
            )
            SettingsEntryBoolean(
                Translation.SETTINGS_USE_ALT_LAYOUT,
                Preference.USE_ALT_LAYOUT,
                mainActivity.useAltLayout,
                prefs,
                mainActivity,
                applicationContext
            )
            SettingsEntryBoolean(
                Translation.SETTINGS_USE_ANIMATED_ICONS,
                Preference.USE_ANIMATED_ICONS,
                mainActivity.useAnimatedIcons,
                prefs,
                mainActivity,
                applicationContext
            )
            SettingsEntryBoolean(
                Translation.SETTINGS_ENABLE_EXPERIMENTAL_FORECASTS,
                Preference.ENABLE_EXPERIMENTAL_FORECASTS,
                mainActivity.enableExperimental,
                prefs,
                mainActivity,
                applicationContext
            )

            HorizontalDivider(
                modifier = Modifier
                    .padding(0.dp, 10.dp, 0.dp, 20.dp),
                color = colorResource(id = R.color.light_gray),
                thickness = 1.dp
            )
        }
    }
}


@Composable
fun SettingsEntryBoolean(
    translation: Translation,
    preference: Preference,
    mutableState: MutableState<Boolean>,
    prefs: AppPreferences,
    mainActivity: MainActivity,
    applicationContext: Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp, 0.dp, 0.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
        ) {
            Text(
                text = LangStrings.getTranslationString(
                    mainActivity.selectedLang.value,
                    translation
                ),
                textAlign = TextAlign.Start,
                color = colorResource(id = R.color.text_color),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    mutableState.value = !mutableState.value
                    prefs.setBoolean(preference, mutableState.value)
                    DisplayInfo.updateWidget(
                        applicationContext,
                        mainActivity.displayInfo.value
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painterResource(if (mutableState.value) R.drawable.baseline_check_box_24 else R.drawable.baseline_check_box_outline_blank_24),
                contentDescription = "",
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
fun SettingsEntryString(
    translation: Translation,
    preference: Preference,
    mutableState: MutableState<String>,
    nextEntry: HashMap<String, String>,
    defaultVal: String,
    prefs: AppPreferences,
    mainActivity: MainActivity,
    applicationContext: Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp, 0.dp, 0.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
        ) {
            Text(
                text = LangStrings.getTranslationString(
                    mainActivity.selectedLang.value,
                    translation
                ),
                textAlign = TextAlign.Start,
                color = colorResource(id = R.color.text_color),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    mutableState.value = nextEntry[mutableState.value] ?: defaultVal
                    prefs.setString(preference, mutableState.value)
                    DisplayInfo.updateWidget(
                        applicationContext,
                        mainActivity.displayInfo.value
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = mutableState.value,
                color = colorResource(id = R.color.text_color),
                textAlign = TextAlign.Center
            )
        }
    }
}