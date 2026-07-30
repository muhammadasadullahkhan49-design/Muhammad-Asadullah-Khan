package com.example.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.db.AppDatabase
import com.example.db.CustomAlarm
import com.example.prayer.City
import com.example.prayer.PrayerCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NamazRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("namaz_mode_prefs", Context.MODE_PRIVATE)

    private val db = AppDatabase.getDatabase(context)
    val alarmDao = db.customAlarmDao()

    private val _isNamazModeManualActive = MutableStateFlow(getManualNamazMode())
    val isNamazModeManualActive: StateFlow<Boolean> = _isNamazModeManualActive.asStateFlow()

    private val _selectedCity = MutableStateFlow(getSelectedCityInternal())
    val selectedCity: StateFlow<City> = _selectedCity.asStateFlow()

    private val _autoReplyText = MutableStateFlow(getAutoReplyTextInternal())
    val autoReplyText: StateFlow<String> = _autoReplyText.asStateFlow()

    private val _isJummahModeEnabled = MutableStateFlow(prefs.getBoolean("jummah_mode_enabled", true))
    val isJummahModeEnabled: StateFlow<Boolean> = _isJummahModeEnabled.asStateFlow()

    private val _jummahDurationMinutes = MutableStateFlow(prefs.getInt("jummah_duration_mins", 90))
    val jummahDurationMinutes: StateFlow<Int> = _jummahDurationMinutes.asStateFlow()

    private val _isEidModeEnabled = MutableStateFlow(prefs.getBoolean("eid_mode_enabled", true))
    val isEidModeEnabled: StateFlow<Boolean> = _isEidModeEnabled.asStateFlow()

    private val _prayerSilenceDurationMins = MutableStateFlow(prefs.getInt("prayer_silence_duration_mins", 25))
    val prayerSilenceDurationMins: StateFlow<Int> = _prayerSilenceDurationMins.asStateFlow()

    fun getManualNamazMode(): Boolean = prefs.getBoolean("manual_namaz_mode", false)

    fun setManualNamazMode(active: Boolean) {
        prefs.edit().putBoolean("manual_namaz_mode", active).apply()
        _isNamazModeManualActive.value = active
    }

    private fun getSelectedCityInternal(): City {
        val cityId = prefs.getString("selected_city_id", "multan") ?: "multan"
        return PrayerCalculator.PUNJAB_CITIES.find { it.id == cityId }
            ?: PrayerCalculator.getDefaultCity()
    }

    fun setSelectedCity(city: City) {
        prefs.edit().putString("selected_city_id", city.id).apply()
        _selectedCity.value = city
    }

    private fun getAutoReplyTextInternal(): String {
        return prefs.getString(
            "auto_reply_text",
            "Assalamu Alaikum, I am currently offering Namaz / in Ibadat. I will reply to your message shortly. InshaAllah."
        ) ?: "Assalamu Alaikum, I am currently offering Namaz / in Ibadat. I will reply to your message shortly. InshaAllah."
    }

    fun setAutoReplyText(text: String) {
        prefs.edit().putString("auto_reply_text", text).apply()
        _autoReplyText.value = text
    }

    fun setJummahModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("jummah_mode_enabled", enabled).apply()
        _isJummahModeEnabled.value = enabled
    }

    fun setJummahDurationMinutes(duration: Int) {
        prefs.edit().putInt("jummah_duration_mins", duration).apply()
        _jummahDurationMinutes.value = duration
    }

    fun setEidModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("eid_mode_enabled", enabled).apply()
        _isEidModeEnabled.value = enabled
    }

    fun setPrayerSilenceDurationMins(duration: Int) {
        prefs.edit().putInt("prayer_silence_duration_mins", duration).apply()
        _prayerSilenceDurationMins.value = duration
    }

    // Custom Alarms Flow & DAO wrappers
    val allAlarms: Flow<List<CustomAlarm>> = alarmDao.getAllAlarms()

    suspend fun addAlarm(alarm: CustomAlarm) {
        alarmDao.insertAlarm(alarm)
    }

    suspend fun updateAlarm(alarm: CustomAlarm) {
        alarmDao.updateAlarm(alarm)
    }

    suspend fun deleteAlarm(alarm: CustomAlarm) {
        alarmDao.deleteAlarm(alarm)
    }

    // Static helper to check active status from Services without coroutine flow overhead
    companion object {
        fun isNamazModeActive(context: Context): Boolean {
            val prefs = context.getSharedPreferences("namaz_mode_prefs", Context.MODE_PRIVATE)
            val manual = prefs.getBoolean("manual_namaz_mode", false)
            if (manual) return true

            val scheduledActive = prefs.getBoolean("scheduled_silent_active", false)
            return scheduledActive
        }

        fun getAutoReplyMessage(context: Context): String {
            val prefs = context.getSharedPreferences("namaz_mode_prefs", Context.MODE_PRIVATE)
            return prefs.getString(
                "auto_reply_text",
                "Assalamu Alaikum, I am currently offering Namaz / in Ibadat. I will reply to your message shortly. InshaAllah."
            ) ?: "Assalamu Alaikum, I am currently offering Namaz / in Ibadat. I will reply to your message shortly. InshaAllah."
        }

        fun setScheduledSilentActive(context: Context, active: Boolean) {
            val prefs = context.getSharedPreferences("namaz_mode_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("scheduled_silent_active", active).apply()
        }
    }
}
