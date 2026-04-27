package ru.tgmaksim.activium.utilities

import android.Manifest
import android.os.Build
import android.app.Activity
import android.content.Intent
import android.content.Context
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.content.Context.NOTIFICATION_SERVICE
import android.graphics.Bitmap

import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import kotlinx.serialization.Serializable

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.ui.main.MainActivity

/**
 * Менеджер уведомлений для создания, планирования и запроса разрешений
 * @author Максим Дрючин (tgmaksim)
 * */
object NotificationManager {
    /** Название канала уведомлений о внеурочных занятиях */
    const val CHANNEL_EA = "extracurricular_activities"
    const val CHANNEL_MARKS = "marks"
    const val CHANNEL_SERVICE = "service"
    const val CHANNEL_PRAISE = "praise"
    const val CHANNEL_NOTES = "notes"

    /**
     * Проверка разрешения на отправку уведомлений и запрос в случае необходимости
     * @param activity Android-activity
     * @author Максим Дрючин (tgmaksim)
     * */
    fun setupPostNotifications(activity: Activity) {
        val notificationManager = activity.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val existsEA = notificationManager.getNotificationChannel(CHANNEL_EA)
        val existsDnevnik = notificationManager.getNotificationChannel(CHANNEL_MARKS)
        val existsService = notificationManager.getNotificationChannel(CHANNEL_SERVICE)
        val existsPraise = notificationManager.getNotificationChannel(CHANNEL_PRAISE)
        val existsNotes = notificationManager.getNotificationChannel(CHANNEL_NOTES)

        // Создание канала уведомлений
        if (existsEA == null) {
            val channelName = "Внеурочные занятия"
            val channelDescription = "Уведомления о том, что через несколько минут начнется внеурочное занятие"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHANNEL_EA, channelName, importance)
            channel.description = channelDescription

            notificationManager.createNotificationChannel(channel)
        }

        if (existsDnevnik == null) {
            val channelName = "Новые оценки"
            val channelDescription = "Уведомления о выставлении новых оценок"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHANNEL_MARKS, channelName, importance)
            channel.description = channelDescription

            notificationManager.createNotificationChannel(channel)
        }

        if (existsService == null) {
            val channelName = "Сервисные события"
            val channelDescription = "Сервисные уведомления о прохождении модерации Ваших отзывов и важные оповещения от Активиум"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHANNEL_SERVICE, channelName, importance)
            channel.description = channelDescription

            notificationManager.createNotificationChannel(channel)
        }

        if (existsPraise == null) {
            val channelName = "Похвала от родителей"
            val channelDescription = "Уведомления о получении похвалы от родителей за получение оценок"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHANNEL_PRAISE, channelName, importance)
            channel.description = channelDescription

            notificationManager.createNotificationChannel(channel)
        }

        if (existsNotes == null) {
            val channelName = "Напоминания о заметках"
            val channelDescription = "Уведомления с напоминанием о заметках, если вы установили таймер"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHANNEL_NOTES, channelName, importance)
            channel.description = channelDescription

            notificationManager.createNotificationChannel(channel)
        }

        // Явный запрос на уведомления для SDK >= 33
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!checkPermission(activity)) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0
                )
            }
        }
    }

    /**
     * Проверка разрешения на отправку уведомлений
     * @param context Android-контекст
     * @return true, если разрешение есть, иначе - false
     * @author Максим Дрючин (tgmaksim)
     * */
    fun checkPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Показ уведомления
     * @param context Android-контекст
     * @param channel название канала уведомлений
     * @param title заголовок уведомления
     * @param message текст уведомления
     * @param priority приоритет уведомления
     * @author Максим Дрючин (tgmaksim)
     * */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showNotification(
        context: Context,
        channel: String,
        title: String,
        message: String,
        data: Map<String, String> = emptyMap(),
        bitmap: Bitmap? = null,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        buttons: List<NotificationButton> = emptyList(),
        time: Long? = null
    ) {
        val id = (System.currentTimeMillis() % (7 * 24 * 60 * 60 * 1000)).toInt()

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            for (entry in data) {
                putExtra(entry.key, entry.value)
            }
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context, id, mainIntent, PendingIntent.FLAG_IMMUTABLE)

        val buttonActions = buttons.mapIndexed { index, button ->
            val intent = if (button.action == "open") Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("notificationId", id)
                for (entry in button.data) {
                    putExtra(entry.key, entry.value)
                }
            } else null

            val pendingIntent = intent?.let { PendingIntent.getActivity(
                context, id + index, intent, PendingIntent.FLAG_IMMUTABLE) }

            NotificationCompat.Action(R.drawable.ic_launcher_foreground, button.text, pendingIntent)
        }

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setContentIntent(mainPendingIntent)
            .apply { buttonActions.forEach { addAction(it) } }
            .setAutoCancel(true)
            .setShowWhen(true)
            .apply { time?.let { setWhen(it) } }
            .apply { bitmap?.let { setLargeIcon(it) } }
            .setStyle(NotificationCompat.BigTextStyle())

        val manager = NotificationManagerCompat.from(context)
        manager.notify(id, builder.build())
    }

    @Serializable
    data class NotificationButton(
        val text: String,
        val action: String,
        val data: Map<String, String>
    )
}