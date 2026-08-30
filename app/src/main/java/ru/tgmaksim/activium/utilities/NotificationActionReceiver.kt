package ru.tgmaksim.activium.utilities

import android.os.Build
import android.Manifest

import android.content.Intent
import android.content.Context
import android.content.BroadcastReceiver
import android.content.pm.PackageManager

import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.Settings
import ru.tgmaksim.activium.api.DnevnikTools

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val notificationId = intent.getIntExtra("notificationId", -1)
        if (notificationId == -1) return

        val action = intent.getStringExtra("action")

        when (action) {
            "praise" -> {
                val ratingKey = intent.getStringExtra("ratingKey") ?: return

                runAsyncAction(context, notificationId) { sendPraise(ratingKey) }
            }
            "hide_extracurricular_activity" -> {
                val subject = intent.getStringExtra("subject") ?: return
                val place = intent.getStringExtra("place") ?: return
                val profile = intent.getStringExtra("profile")?.toLong() ?: return

                runAsyncAction(context, notificationId) { hideExtracurricularActivity(profile, subject, place) }
            }
            else -> {
                updateNotificationStatus(context, notificationId, null)
            }
        }
    }

    private fun runAsyncAction(context: Context, notificationId: Int, action: suspend () -> Boolean) {
        val pendingResult = goAsync()

        val exceptionHandler = CoroutineExceptionHandler  { _, throwable ->
            Utilities.log(throwable, "Coroutine Error at NotificationActionReceiver.runAsyncAction")
            updateNotificationStatus(context, notificationId, isSuccess = false)
            pendingResult.finish()
        }
        val coroutine = CoroutineScope(Dispatchers.IO + exceptionHandler)

        coroutine.launch {
            try {
                val isSuccess = action()

                withContext(Dispatchers.Main) {
                    updateNotificationStatus(context, notificationId, isSuccess)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun updateNotificationStatus(context: Context, notificationId: Int, isSuccess: Boolean?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (permission != PackageManager.PERMISSION_GRANTED) return
        }

        val title = context.getString(R.string.notification_broadcast_title)
        val message = context.getString(
            when (isSuccess) {
                true -> R.string.notification_broadcast_success_text
                false -> R.string.notification_broadcast_unsuccess_text
                null -> R.string.notification_broadcast_unknown_text
            })

        val statusNotification = NotificationCompat.Builder(context, NotificationManager.CHANNEL_SERVICE)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle())
            .build()

        val manager = NotificationManagerCompat.from(context)
        manager.notify(notificationId, statusNotification)
    }

    private suspend fun sendPraise(ratingKey: String): Boolean {
        val answer = DnevnikTools.sendPraise(
            ratingKey = ratingKey,
            lessonKey = null,
            text = null
        )

        return answer.status
    }

    private suspend fun hideExtracurricularActivity(childId: Long, subject: String, place: String): Boolean {
        val answer = Settings.hideExtracurricularActivity(childId, subject, place)

        return answer.status
    }
}