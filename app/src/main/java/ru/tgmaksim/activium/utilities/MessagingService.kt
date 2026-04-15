package ru.tgmaksim.activium.utilities

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import androidx.core.app.NotificationCompat

import coil3.toBitmap
import coil3.ImageLoader
import android.graphics.Bitmap
import coil3.request.ImageRequest
import coil3.request.allowHardware

import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineExceptionHandler
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
        try {
            if (NotificationManager.checkPermission(this))
                message.notification?.let { notification ->
                    val title = notification.title
                    val body = notification.body
                    val imageUrl = notification.imageUrl?.toString()
                    val data = message.data

                    if (title != null && body != null) {
                        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
                            Utilities.log("Coroutine Error: ${throwable.message}")
                        }
                        val scope = CoroutineScope(Dispatchers.IO + exceptionHandler)

                        scope.launch {
                            val bitmap = imageUrl?.let { downloadBitmap(imageUrl) }
                            NotificationManager.showNotification(
                                this@MessagingService,
                                notification.channelId ?: NotificationManager.CHANNEL_SERVICE,
                                title,
                                body,
                                data,
                                bitmap,
                                notification.notificationPriority ?: NotificationCompat.PRIORITY_HIGH
                            )
                        }
                    }
                }
        } catch (e: Exception) {
            Utilities.log(e)
        }
    }

    private suspend fun downloadBitmap(url: String): Bitmap? {
        val loader = ImageLoader(this)
        val request = ImageRequest.Builder(this)
            .data(url)
            .allowHardware(false)
            .build()

        val result = loader.execute(request)
        return result.image?.toBitmap()
    }
}