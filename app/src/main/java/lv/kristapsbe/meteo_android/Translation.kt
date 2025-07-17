package lv.kristapsbe.meteo_android

import lv.kristapsbe.meteo_android.MainActivity.Companion.LANG_EN
import lv.kristapsbe.meteo_android.MainActivity.Companion.LANG_LV


enum class Translation {
    RAIN_EXPECTED_TODAY,
    RAIN_EXPECTED_TOMORROW,
    FEELS_LIKE,
    FORECAST_ISSUED,
    FORECAST_DOWNLOADED,
    FORECAST_DOWNLOADED_NO_SKIP,

    SETTINGS_APP_LANGUAGE,
    SETTINGS_WIDGET_TRANSPARENCY,
    SETTINGS_FORCE_ALWAYS_SHOW_DETAILS,
    SETTINGS_TEMPERATURE_UNIT,
    SETTINGS_DISPLAY_AURORA,
    SETTINGS_FIX_ICON_DAY_NIGHT,
    SETTINGS_USE_ALT_LAYOUT,
    SETTINGS_USE_ANIMATED_ICONS,
    SETTINGS_ENABLE_EXPERIMENTAL_FORECASTS,

    NOTIFICATION_AURORA_TITLE,
    NOTIFICATION_AURORA_DESCRIPTION,

    PRIVACY_I_HAVE_READ,
    PRIVACY_POLICY,
    PRIVACY_CONTINUE,
}

class LangStrings {
    companion object {
        private val dayMapping = hashMapOf(
            LANG_EN to hashMapOf(
                "MONDAY" to "Mo",
                "TUESDAY" to "Tu",
                "WEDNESDAY" to "We",
                "THURSDAY" to "Th",
                "FRIDAY" to "Fr",
                "SATURDAY" to "Sa",
                "SUNDAY" to "Su"
            ),
            LANG_LV to hashMapOf(
                "MONDAY" to "P.",
                "TUESDAY" to "O.",
                "WEDNESDAY" to "T.",
                "THURSDAY" to "C.",
                "FRIDAY" to "Pk.",
                "SATURDAY" to "S.",
                "SUNDAY" to "Sv."
            )
        )

        fun getShortenedDayString(lang: String, dow: String): String {
            return dayMapping[lang]?.get(dow) ?: ""
        }

        //https://uni.edu/storm/Wind%20Direction%20slide.pdf
        private val directions = hashMapOf(
            LANG_EN to hashMapOf(
                35 to "N",
                36 to "N",
                0 to "N",
                1 to "N",
                2 to "N/NE",
                3 to "N/NE",
                4 to "NE",
                5 to "NE",
                6 to "E/NE",
                7 to "E/NE",
                8 to "E",
                9 to "E",
                10 to "E",
                11 to "E/SE",
                12 to "E/SE",
                13 to "SE",
                14 to "SE",
                15 to "S/SE",
                16 to "S/SE",
                17 to "S",
                18 to "S",
                19 to "S",
                20 to "S/SW",
                21 to "S/SW",
                22 to "SW",
                23 to "SW",
                24 to "W/SW",
                25 to "W/SW",
                26 to "W",
                27 to "W",
                28 to "W",
                29 to "W/NW",
                30 to "W/NW",
                31 to "NW",
                32 to "NW",
                33 to "N/NW",
                34 to "N/NW",
            ),
            LANG_LV to hashMapOf(
                35 to "Z",
                36 to "Z",
                0 to "Z",
                1 to "Z",
                2 to "Z/ZA",
                3 to "Z/ZA",
                4 to "ZA",
                5 to "ZA",
                6 to "A/ZA",
                7 to "A/ZA",
                8 to "A",
                9 to "A",
                10 to "A",
                11 to "A/DA",
                12 to "A/DA",
                13 to "DA",
                14 to "DA",
                15 to "D/DA",
                16 to "D/DA",
                17 to "D",
                18 to "D",
                19 to "D",
                20 to "D/DR",
                21 to "D/DR",
                22 to "DR",
                23 to "DR",
                24 to "R/DR",
                25 to "R/DR",
                26 to "R",
                27 to "R",
                28 to "R",
                29 to "R/ZR",
                30 to "R/ZR",
                31 to "ZR",
                32 to "ZR",
                33 to "Z/ZR",
                34 to "Z/ZR",
            )
        )

        fun getDirectionString(lang: String, deg: Int): String {
            return directions[if (lang == LANG_LV) LANG_LV else LANG_EN]?.get(deg) ?: ""
        }

        private val translationStrings = hashMapOf(
            LANG_EN to hashMapOf(
                Translation.RAIN_EXPECTED_TODAY to "today at",
                Translation.RAIN_EXPECTED_TOMORROW to "tomorrow at",
                Translation.FEELS_LIKE to "Feels like",
                Translation.FORECAST_ISSUED to "forecast issued at",
                Translation.FORECAST_DOWNLOADED to "forecast downloaded at",
                Translation.FORECAST_DOWNLOADED_NO_SKIP to "full forecast downloaded at",
                Translation.SETTINGS_APP_LANGUAGE to "App language",
                Translation.SETTINGS_WIDGET_TRANSPARENCY to "Show widget background color",
                Translation.SETTINGS_FORCE_ALWAYS_SHOW_DETAILS to "Always force widget into detailed mode",
                Translation.SETTINGS_TEMPERATURE_UNIT to "Temperature unit",
                Translation.SETTINGS_DISPLAY_AURORA to "Display aurora forecast",
                Translation.SETTINGS_USE_ALT_LAYOUT to "Use alternative widget layout",
                Translation.NOTIFICATION_AURORA_TITLE to "Aurora",
                Translation.NOTIFICATION_AURORA_DESCRIPTION to "Aurora probability has increased to",
                Translation.SETTINGS_FIX_ICON_DAY_NIGHT to "Match icons to sunrise and sunset",
                Translation.SETTINGS_USE_ANIMATED_ICONS to "Use animated icons",
                Translation.SETTINGS_ENABLE_EXPERIMENTAL_FORECASTS to "Enable experimental forecasts",
                Translation.PRIVACY_I_HAVE_READ to "I have read and agree to the ",
                Translation.PRIVACY_POLICY to "Privacy Policy",
                Translation.PRIVACY_CONTINUE to "Continue"
            ),
            LANG_LV to hashMapOf(
                Translation.RAIN_EXPECTED_TODAY to "šodien plkst.",
                Translation.RAIN_EXPECTED_TOMORROW to "rīt plkst.",
                Translation.FEELS_LIKE to "Sajūta",
                Translation.FORECAST_ISSUED to "prognoze atjaunināta",
                Translation.FORECAST_DOWNLOADED to "prognoze lejupielādēta",
                Translation.FORECAST_DOWNLOADED_NO_SKIP to "pilna prognoze lejupielādēta",
                Translation.SETTINGS_APP_LANGUAGE to "Lietotnes valoda",
                Translation.SETTINGS_WIDGET_TRANSPARENCY to "Rādīt logrīka fona krāsu",
                Translation.SETTINGS_FORCE_ALWAYS_SHOW_DETAILS to "Piespiest logrīku vienmēr rādīt detalizēto izkārtojumu",
                Translation.SETTINGS_TEMPERATURE_UNIT to "Temperatūras mērvienība",
                Translation.SETTINGS_DISPLAY_AURORA to "Rādīt ziemeļblāzmas prognozi",
                Translation.SETTINGS_USE_ALT_LAYOUT to "Lietot alternatīvo logrīka izkārtojumu",
                Translation.NOTIFICATION_AURORA_TITLE to "Ziemeļblāzma",
                Translation.NOTIFICATION_AURORA_DESCRIPTION to "Ziemeļblāzmas varbūtība ir palielinājusies līdz",
                Translation.SETTINGS_FIX_ICON_DAY_NIGHT to "Likt ikonām sakrist ar saullēktu un saulrietu",
                Translation.SETTINGS_USE_ANIMATED_ICONS to "Lietot animētas ikonas",
                Translation.SETTINGS_ENABLE_EXPERIMENTAL_FORECASTS to "Iespējot eksperimentālās prognozes",
                Translation.PRIVACY_I_HAVE_READ to "Es esmu izlasījis un piekrītu ",
                Translation.PRIVACY_POLICY to "Privātuma politikai",
                Translation.PRIVACY_CONTINUE to "Turpināt"
            )
        )

        fun getTranslationString(lang: String, key: Translation): String {
            return translationStrings[if (lang == LANG_LV) LANG_LV else LANG_EN]?.get(key) ?: ""
        }
    }
}