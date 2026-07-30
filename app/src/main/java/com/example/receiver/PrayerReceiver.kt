package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.repository.NamazRepository
import com.example.scheduler.PrayerScheduler

class PrayerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            ACTION_START_NAMAZ_SILENT -> {
                val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
                val durationMins = intent.getIntExtra(EXTRA_DURATION_MINS, 25)

                enableSilentMode(context, prayerName, durationMins)
            }
            ACTION_STOP_NAMAZ_SILENT -> {
                disableSilentMode(context)
            }
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.MY_PACKAGE_REPLACED" -> {
                PrayerScheduler.scheduleAllPrayersAndAlarms(context)
            }
        }
    }

    private fun enableSilentMode(context: Context, prayerName: String, durationMins: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        NamazRepository.setScheduledSilentActive(context, true)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            } else {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            // Fallback to ringer mode silent
            try {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            } catch (ignored: Exception) {}
        }

        showOngoingNotification(context, prayerName, durationMins)

        // Schedule automatic end alarm
        PrayerScheduler.scheduleSilentEndAlarm(context, durationMins)
    }

    private fun disableSilentMode(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        NamazRepository.setScheduledSilentActive(context, false)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cancelOngoingNotification(context)

        // Reschedule daily prayer alarms
        PrayerScheduler.scheduleAllPrayersAndAlarms(context)
    }

    private fun showOngoingNotification(context: Context, prayerName: String, durationMins: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Smart Namaz Mode Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active Namaz silent mode status"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setContentTitle("Smart Namaz Mode Active ($prayerName)")
            .setContentText("Phone silenced. Internet auto-reply active for $durationMins mins.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun cancelOngoingNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object {
        const val ACTION_START_NAMAZ_SILENT = "com.example.action.START_NAMAZ_SILENT"
        const val ACTION_STOP_NAMAZ_SILENT = "com.example.action.STOP_NAMAZ_SILENT"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_DURATION_MINS = "extra_duration_mins"

        private const val CHANNEL_ID = "namaz_mode_active_channel"
        private const val NOTIFICATION_ID = 8801
    }
}
