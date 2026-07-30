package com.example.service

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.repository.NamazRepository

class NotificationReplyService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return

        // 1. Check if social/VoIP application
        if (!isSocialMessagingApp(packageName)) {
            return
        }

        // 2. Check if Namaz Mode is currently active
        val isNamazActive = NamazRepository.isNamazModeActive(applicationContext)
        if (!isNamazActive) {
            return
        }

        // 3. Check active internet connection (STRICT REQUIREMENT: Internet-Only Auto-Reply)
        if (!isInternetAvailable(applicationContext)) {
            Log.d(TAG, "Namaz Mode Active, but internet connection unavailable. Skipping auto-reply.")
            return
        }

        // 4. Locate Direct Reply RemoteInput in Notification Actions
        val notification = sbn.notification ?: return
        val actions = notification.actions ?: return

        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            for (remoteInput in remoteInputs) {
                if (remoteInput.allowFreeFormInput) {
                    val replyMessage = NamazRepository.getAutoReplyMessage(applicationContext)
                    val success = sendRemoteInputReply(applicationContext, action, remoteInput, replyMessage)
                    if (success) {
                        Log.i(TAG, "Automated internet auto-reply sent to $packageName: '$replyMessage'")
                        return
                    }
                }
            }
        }
    }

    private fun isSocialMessagingApp(packageName: String): Boolean {
        return ALLOWED_SOCIAL_PACKAGES.contains(packageName)
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } else {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = cm.activeNetworkInfo
            @Suppress("DEPRECATION")
            activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }

    private fun sendRemoteInputReply(
        context: Context,
        action: Notification.Action,
        remoteInput: RemoteInput,
        replyMessage: String
    ): Boolean {
        return try {
            val intent = Intent()
            val bundle = Bundle()
            bundle.putCharSequence(remoteInput.resultKey, replyMessage)
            RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)

            action.actionIntent.send(context, 0, intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification remote reply: ${e.message}", e)
            false
        }
    }

    companion object {
        private const val TAG = "NamazAutoReplyService"

        val ALLOWED_SOCIAL_PACKAGES = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "com.facebook.orca",
            "com.instagram.android",
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "com.viber.voip",
            "com.discord",
            "org.thoughtcrime.securesms" // Signal
        )
    }
}
