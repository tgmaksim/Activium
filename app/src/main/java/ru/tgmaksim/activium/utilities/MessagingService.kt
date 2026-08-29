package ru.tgmaksim.activium.utilities

import android.annotation.SuppressLint

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

import ru.tgmaksim.activium.api.json
import ru.tgmaksim.activium.api.Settings
import ru.tgmaksim.activium.utilities.datastore.SettingsManager

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MessagingService : FirebaseMessagingService() {
    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)

        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Utilities.log(throwable, "Coroutine Error at MessagingService.onRegistered")
        }
        val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)

        applicationScope.launch {
            SettingsManager.setFirebaseMessagingToken(installationId)
            Settings.updateFirebase(installationId)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        try {
            if (!NotificationManager.checkPermission(this))
                return

            val data = message.data

            val title = data["title"] ?: message.notification?.title ?: return
            val body = data["body"] ?: message.notification?.body ?: return
            val channelId = data["channelId"] ?: message.notification?.channelId ?: NotificationManager.CHANNEL_SERVICE
            val imageUrl = data["imageUrl"] ?: message.notification?.imageUrl?.toString()
            val priority = data["priority"]?.toInt() ?: message.notification?.notificationPriority ?: NotificationCompat.PRIORITY_HIGH
            val time = data["time"]?.toLong()

            val buttons = try {
                data["buttons"]?.let { json.decodeFromString<List<NotificationManager.NotificationButton>>(it) }
            } catch (e: Exception) {
                Utilities.log(e)
                null
            } ?: emptyList()

            val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
                Utilities.log(throwable, "Coroutine Error at MessagingService.onMessageReceived")
            }
            val scope = CoroutineScope(Dispatchers.IO + exceptionHandler)

            scope.launch {
                val bitmap = imageUrl?.let { downloadBitmap(imageUrl) }
                NotificationManager.showNotification(
                    this@MessagingService,
                    channelId,
                    title,
                    body,
                    data,
                    bitmap,
                    priority,
                    buttons,
                    time
                )
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