package ru.tgmaksim.activium.utilities

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import androidx.core.app.NotificationCompat

import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.FirebaseMessagingService

import ru.tgmaksim.activium.api.Settings
import ru.tgmaksim.activium.utilities.datastore.SettingsManager

class MessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        applicationScope.launch {
            SettingsManager.setFirebaseMessagingToken(token)
            if (SettingsManager.getSessionId() != null)
                Settings.updateFirebase(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (NotificationManager.checkPermission(this))
            message.notification?.let { notification ->
                val title = notification.title
                val body = notification.body
                if (title != null && body != null)
                    NotificationManager.showNotification(
                        this,
                        notification.channelId ?: NotificationManager.CHANNEL_SERVICE,
                        title,
                        body,
                        notification.notificationPriority ?: NotificationCompat.PRIORITY_DEFAULT
                    )
            }
    }
}