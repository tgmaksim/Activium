package ru.tgmaksim.activium.utilities

import android.util.Log
import android.widget.Toast
import android.content.Intent
import android.content.Context
import android.content.DialogInterface
import android.content.ActivityNotFoundException

import androidx.core.net.toUri
import androidx.annotation.StringRes

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.ui.core.UiText

/**
 * Утилиты приложения
 * @author Максим Дрючин (tgmaksim)
 * */
object Utilities {
    /**
     * Открытие ссылки в браузере
     * @param context Android-контекст
     * @param url Ссылка для открытия
     * @return Результат операции
     * @author Максим Дрючин (tgmaksim)
     * */
    fun openUrl(context: Context, url: String): Boolean {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
            context.startActivity(browserIntent)
            return true
        } catch (e: ActivityNotFoundException) {
            log(e)
            showText(context, "Не найдено приложение для открытия ссылки")
            return false
        }
    }

    /**
     * Показ системного текстового сообщения
     * @param context Android-контекст
     * @param text текст сообщения
     * @param long показывать ли долгое сообщение
     * @author Максим Дрючин (tgmaksim)
     * */
    fun showText(context: Context, text: String, long: Boolean = false) {
        Toast.makeText(
            context,
            text,
            if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Показ системного текстового сообщения
     * @param context Android-контекст
     * @param resId текст сообщения в виде ресурса
     * @param long показывать ли долгое сообщение
     * @author Максим Дрючин (tgmaksim)
     * */
    fun showText(context: Context, @StringRes resId: Int, vararg formatArgs: Any, long: Boolean = false) {
        Toast.makeText(
            context,
            context.getString(resId, formatArgs),
            if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        ).show()
    }

    fun showUiMessage(context: Context, message: UiText) {
        when (message) {
            is UiText.DynamicString -> {
                showText(context, message.value)
            }
            is UiText.StringResource -> {
                showText(context, message.resId, *message.args.toTypedArray())
            }
        }
    }

    /**
     * Логирование данных локально
     * @param string текстовые данные для логирования
     * @param tag дополнительный тег для логов
     * @author Максим Дрючин (tgmaksim)
     * */
    fun log(string: String?, tag: String) {
        Log.d("Activium.$tag", string.toString())
    }

    /**
     * Логирование данных о некорректных данных локально и отправка в Firebase Crashlytics
     * @param e описание ошибки
     * @author Максим Дрючин (tgmaksim)
     * */
    fun log(e: String) {
        Log.e("Activium.error", e)

        runCatching {
            FirebaseCrashlytics.getInstance().log(e)
        }
    }

    /**
     * Логирование данных об ошибке локально и отправка в Firebase Crashlytics
     * @param e возникшая ошибка
     * @author Максим Дрючин (tgmaksim)
     * */
    fun log(e: Throwable, context: String? = null) {
        Log.e("Activium.error", "Ошибка", e)

        runCatching {
            val crashlytics = FirebaseCrashlytics.getInstance()

            context?.let { crashlytics.setCustomKey("error_context", it) }

            crashlytics.recordException(e)
        }
    }

    /**
     * Показ диалогового окна с уведомлением
     * @param context Android-контекст
     * @param title заголовок диалогового окна
     * @param message текст сообщения
     * @param buttonText текст кнопки
     * @param back показывать ли кнопку назад
     * @param buttonListener действие при нажатии кнопки
     * @author Максим Дрючин (tgmaksim)
     * */
    fun showAlertDialog(
        context: Context,
        title: String,
        message: String,
        buttonText: String,
        back: Boolean = true,
        buttonListener: DialogInterface.OnClickListener? = null
    ) {
        MaterialAlertDialogBuilder(context, R.style.AppDialogTheme).apply {
            setTitle(title)
            setMessage(message)
            setPositiveButton(buttonText, buttonListener)
            if (back) setNegativeButton("Отмена", null)
            setCancelable(back)
        }.show()
    }
}