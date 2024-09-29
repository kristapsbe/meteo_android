package lv.kristapsbe.meteo_android

enum class Preferences {
    FORCE_CURRENT_LOCATION,
    FORCE_DEFAULT_LOCATION,
    TEMP_UNIT,
    LANG,
    USE_TRANSPARENT_WIDGET,
    DO_ALWAYS_SHOW_AURORA,
    USE_ALT_LAYOUT,
    USE_ANIMATED_ICONS,
    LAST_LAT_LON,
}

class PrefUtils {
    companion object {
        const val APP_PREFS = "AppPrefs"
    }
}