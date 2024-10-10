package lv.kristapsbe.meteo_android

import android.app.Activity.MODE_PRIVATE
import android.content.Context


enum class Preference {
    FORCE_CURRENT_LOCATION,
    TEMP_UNIT,
    LANG,
    DO_SHOW_WIDGET_BACKGROUND,
    DO_ALWAYS_SHOW_AURORA,
    DO_ALWAYS_SHOW_UV,
    DO_FIX_ICON_DAY_NIGHT,
    USE_ALT_LAYOUT,
    USE_ANIMATED_ICONS,
    LAST_LAT,
    LAST_LON,
    PRIVACY_POLICY_ACCEPTED,
    HAS_AURORA_NOTIFIED,
    AURORA_NOTIFICATION_ID,
}

class AppPreferences(context: Context) {
    private val prefName = "AppPrefs"
    private val prefs = context.getSharedPreferences(prefName, MODE_PRIVATE)

    private val defaultString = ""
    private val defaultInt = -1
    private val defaultBoolean = false
    private val defaultFloat = -999f

    fun getInt(pref: Preference): Int {
        return prefs.getInt(pref.toString(), defaultInt)
    }

    fun setInt(pref: Preference, value: Int) {
        prefs.edit().putInt(pref.toString(), value).apply()
    }

    fun getFloat(pref: Preference, overrideDefault: Float = defaultFloat): Float {
        return prefs.getFloat(pref.toString(), overrideDefault)
    }

    fun setFloat(pref: Preference, value: Float) {
        prefs.edit().putFloat(pref.toString(), value).apply()
    }

    fun getBoolean(pref: Preference, overrideDefault: Boolean = defaultBoolean): Boolean {
        return prefs.getBoolean(pref.toString(), overrideDefault)
    }

    fun setBoolean(pref: Preference, value: Boolean) {
        prefs.edit().putBoolean(pref.toString(), value).apply()
    }

    fun getString(pref: Preference, overrideDefault: String = defaultString): String {
        return prefs.getString(pref.toString(), overrideDefault) ?: overrideDefault
    }

    fun setString(pref: Preference, value: String) {
        prefs.edit().putString(pref.toString(), value).apply()
    }
}