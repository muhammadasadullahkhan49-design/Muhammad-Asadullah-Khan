package com.example.service

import android.content.Context
import android.media.AudioManager
import android.app.NotificationManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.repository.NamazRepository

class NamazTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val repository = NamazRepository(applicationContext)
        val currentState = repository.getManualNamazMode()
        val newState = !currentState

        repository.setManualNamazMode(newState)

        if (newState) {
            enableSilence()
        } else {
            disableSilence()
        }

        updateTileState()
    }

    private fun enableSilence() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            } else {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            } catch (ignored: Exception) {}
        }
    }

    private fun disableSilence() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isManual = NamazRepository(applicationContext).getManualNamazMode()
        val isActive = isManual || NamazRepository.isNamazModeActive(applicationContext)

        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isActive) "Namaz (Active)" else "Namaz Mode"
        tile.subtitle = if (isActive) "Phone Silenced" else "Tap to Silence"
        tile.updateTile()
    }
}
