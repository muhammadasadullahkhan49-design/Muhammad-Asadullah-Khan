package com.example.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.db.AppDatabase
import com.example.prayer.PrayerCalculator
import com.example.receiver.PrayerReceiver
import com.example.repository.NamazRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

object PrayerScheduler {

    fun scheduleAllPrayersAndAlarms(context: Context) {
        val repo = NamazRepository(context)
        val city = repo.selectedCity.value
        val prayerSilenceMins = repo.prayerSilenceDurationMins.value
        val jummahEnabled = repo.isJummahModeEnabled.value
        val jummahDuration = repo.jummahDurationMinutes.value

        val now = System.currentTimeMillis()
        val timesToday = PrayerCalculator.calculateTimes(city, Date())

        // Calculate tomorrow's times for fallback if today's passed
        val calTomorrow = Calendar.getInstance(TimeZone.getTimeZone("Asia/Karachi")).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val timesTomorrow = PrayerCalculator.calculateTimes(city, calTomorrow.time)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val isFriday = Calendar.getInstance(TimeZone.getTimeZone("Asia/Karachi")).get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

        val prayersList = listOf(
            Triple("Fajr", timesToday.fajrMillis, prayerSilenceMins),
            Triple(
                if (isFriday && jummahEnabled) "Jummah" else "Dhuhr",
                timesToday.dhuhrMillis,
                if (isFriday && jummahEnabled) jummahDuration else prayerSilenceMins
            ),
            Triple("Asr", timesToday.asrMillis, prayerSilenceMins),
            Triple("Maghrib", timesToday.maghribMillis, prayerSilenceMins),
            Triple("Isha", timesToday.ishaMillis, prayerSilenceMins)
        )

        val tomorrowPrayersList = listOf(
            Triple("Fajr", timesTomorrow.fajrMillis, prayerSilenceMins),
            Triple("Dhuhr", timesTomorrow.dhuhrMillis, prayerSilenceMins),
            Triple("Asr", timesTomorrow.asrMillis, prayerSilenceMins),
            Triple("Maghrib", timesTomorrow.maghribMillis, prayerSilenceMins),
            Triple("Isha", timesTomorrow.ishaMillis, prayerSilenceMins)
        )

        // Schedule next upcoming prayer
        val nextPrayer = prayersList.firstOrNull { it.second > now }
            ?: tomorrowPrayersList.first { it.second > now }

        scheduleSinglePrayerAlarm(context, alarmManager, nextPrayer.first, nextPrayer.second, nextPrayer.third)

        // Schedule custom alarms from Room database
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val activeAlarms = db.customAlarmDao().getActiveAlarms()

                activeAlarms.forEach { alarm ->
                    val alarmCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Karachi")).apply {
                        set(Calendar.HOUR_OF_DAY, alarm.hour)
                        set(Calendar.MINUTE, alarm.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    if (alarmCal.timeInMillis <= now) {
                        alarmCal.add(Calendar.DAY_OF_YEAR, 1)
                    }

                    scheduleSinglePrayerAlarm(
                        context,
                        alarmManager,
                        alarm.title,
                        alarmCal.timeInMillis,
                        alarm.durationMinutes,
                        requestCodeOffset = 1000 + alarm.id
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun scheduleSinglePrayerAlarm(
        context: Context,
        alarmManager: AlarmManager,
        prayerName: String,
        triggerTimeMillis: Long,
        durationMins: Int,
        requestCodeOffset: Int = 0
    ) {
        val intent = Intent(context, PrayerReceiver::class.java).apply {
            action = PrayerReceiver.ACTION_START_NAMAZ_SILENT
            putExtra(PrayerReceiver.EXTRA_PRAYER_NAME, prayerName)
            putExtra(PrayerReceiver.EXTRA_DURATION_MINS, durationMins)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            100 + requestCodeOffset,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun scheduleSilentEndAlarm(context: Context, durationMins: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PrayerReceiver::class.java).apply {
            action = PrayerReceiver.ACTION_STOP_NAMAZ_SILENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (durationMins * 60 * 1000L)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
