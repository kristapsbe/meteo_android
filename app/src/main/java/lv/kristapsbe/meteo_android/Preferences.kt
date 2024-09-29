package lv.kristapsbe.meteo_android

import android.app.Activity.MODE_PRIVATE
import android.content.Context

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
    LAST_VERSION_CODE,
}

class AppPreferences(context: Context) {
    private val prefName = "AppPrefs"
    private val prefs = context.getSharedPreferences(prefName, MODE_PRIVATE)

    fun getInt(pref: Preferences): Int {
        return prefs.getInt(pref.toString(), -1)
    }

    fun setInt(pref: Preferences, value: Int) {
        prefs.edit().putInt(pref.toString(), value).apply()
    }

    fun getBoolean(pref: Preferences): Boolean {
        return prefs.getBoolean(pref.toString(), false)
    }

    fun setBoolean(pref: Preferences, value: Boolean) {
        prefs.edit().putBoolean(pref.toString(), value).apply()
    }

    fun getString(pref: Preferences): String {
        return prefs.getString(pref.toString(), "") ?: ""
    }

    fun setString(pref: Preferences, value: String) {
        prefs.edit().putString(pref.toString(), value).apply()
    }
}