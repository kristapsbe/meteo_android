package lv.kristapsbe.meteo_android

import android.app.Activity.MODE_PRIVATE
import android.content.Context
import androidx.core.content.edit


enum class Preference {
    FORCE_CURRENT_LOCATION,
    TEMP_UNIT,
    LANG,
    DO_SHOW_WIDGET_BACKGROUND,
    DO_SHOW_AURORA,
    DO_FIX_ICON_DAY_NIGHT,
    USE_ALT_LAYOUT,
    USE_ANIMATED_ICONS,
    ENABLE_EXPERIMENTAL_FORECASTS,
    LAST_LAT,
    LAST_LON,
    PRIVACY_POLICY_ACCEPTED,
    LOCATION_DISCLOSURE_ACCEPTED,
    HAS_AURORA_NOTIFIED,
    AURORA_NOTIFICATION_ID,
    LAST_LONG_VERSION_CODE,
}

class AppPreferences(context: Context) {
    private val prefName = "AppPrefs"
    private val prefs = context.getSharedPreferences(prefName, MODE_PRIVATE)

    private val defaultString = ""
    private val defaultInt = -1
    private val defaultLong = -1L
    private val defaultBoolean = false
    private val defaultFloat = -999f

    fun getInt(pref: Preference, overrideDefault: Int = defaultInt): Int {
        return prefs.getInt(pref.toString(), overrideDefault)
    }

    fun setInt(pref: Preference, value: Int) {
        prefs.edit { putInt(pref.toString(), value)}
    }

    fun getLong(pref: Preference, overrideDefault: Long = defaultLong): Long {
        return prefs.getLong(pref.toString(), overrideDefault)
    }

    fun setLong(pref: Preference, value: Long) {
        prefs.edit { putLong(pref.toString(), value) }
    }

    fun getFloat(pref: Preference, overrideDefault: Float = defaultFloat): Float {
        return prefs.getFloat(pref.toString(), overrideDefault)
    }

    fun setFloat(pref: Preference, value: Float) {
        prefs.edit { putFloat(pref.toString(), value) }
    }

    fun getBoolean(pref: Preference, overrideDefault: Boolean = defaultBoolean): Boolean {
        return prefs.getBoolean(pref.toString(), overrideDefault)
    }

    fun setBoolean(pref: Preference, value: Boolean) {
        prefs.edit { putBoolean(pref.toString(), value) }
    }

    fun getString(pref: Preference, overrideDefault: String = defaultString): String {
        return prefs.getString(pref.toString(), overrideDefault) ?: overrideDefault
    }

    fun setString(pref: Preference, value: String) {
        prefs.edit { putString(pref.toString(), value) }
    }
}