package ru.tgmaksim.gymnasium.pages

import android.os.Bundle
import android.view.View
import android.content.Intent
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.fragment.app.Fragment

import ru.tgmaksim.gymnasium.BuildConfig
import ru.tgmaksim.gymnasium.ui.LoginActivity
import ru.tgmaksim.gymnasium.ui.ParentActivity
import ru.tgmaksim.gymnasium.utilities.Utilities
import ru.tgmaksim.gymnasium.utilities.CacheManager
import ru.tgmaksim.gymnasium.pages.schedule.SchedulePage
import ru.tgmaksim.gymnasium.utilities.NotificationManager
import ru.tgmaksim.gymnasium.databinding.SettingsPageBinding

/**
 * Fragment-страница с настройками приложения
 * @author Максим Дрючин (tgmaksim)
 * @see ru.tgmaksim.gymnasium.ui.MainActivity
 * */
class SettingsPage : Fragment() {
    private lateinit var ui: SettingsPageBinding
    private var isDarkTheme: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (::ui.isInitialized && CacheManager.isDarkTheme == isDarkTheme)
            return ui.root

        ui = SettingsPageBinding.inflate(inflater, container, false)
        isDarkTheme = CacheManager.isDarkTheme

        initSettingsValues()  // Установка настроек в нужное положение
        setupSettingsListener()  // Настройка обработчиков
        setupButtons()  // Настройка кнопок после настроек

        // Показ блока с обновлением приложения
        CacheManager.versionStatus?.let {
            ui.updateApplication.visibility = View.VISIBLE
            ui.updateDescription.text = StringBuilder(it.latestVersionString).apply {
                append(' ')
                append("(${it.latestVersionNumber})")
                append('\n')
                append(it.updateLogs)
            }.toString()
        }

        return ui.root
    }

    private fun initSettingsValues() {
        // Установка Switch в нужное положение
        ui.settingsDocumentView.isChecked = CacheManager.openWebView
        ui.settingsEANotifications.isChecked = CacheManager.EANotifications
        ui.settingsTheme.isChecked = CacheManager.isDarkTheme

        // Установка нужного диапазона
        ui.settingsScheduleRange.values = listOf(-CacheManager.scheduleBefore, CacheManager.scheduleAfter).map { it.toFloat() }

        // Определение формата
        ui.settingsScheduleRange.setLabelFormatter { value: Float ->
            when (value.toInt()) {
                in -14..-1 -> "${-value.toInt()} до"
                0 -> "сегодня"
                1 -> "завтра"
                else -> "${value.toInt()} после"
            }
        }
    }

    private fun setupSettingsListener() {
        // Смена настройки для открытия документов в домашнем задании
        ui.settingsDocumentView.setOnCheckedChangeListener { _, isChecked ->
            CacheManager.openWebView = isChecked
            Utilities.log("openWebView = $isChecked", tag="settings") {
                param("name", "open_web_view")
                param("is_checked", isChecked.toString())
            }
        }

        // Смена настройки для уведомлений с напоминанием о внеурочном занятии
        ui.settingsEANotifications.setOnCheckedChangeListener { switch, isChecked ->
            if (!isChecked) {
                CacheManager.EANotifications = false
                Utilities.log("EANotifications = false", tag="settings") {
                    param("name", "ea_notifications")
                    param("is_checked", "false")
                }
                return@setOnCheckedChangeListener
            }

            val context = requireContext()
            if (NotificationManager.checkPermission(context) && NotificationManager.canScheduleExactAlarms(context)) {
                CacheManager.EANotifications = true
                Utilities.log("EANotifications = true", tag="settings") {
                    param("name", "ea_notifications")
                    param("is_checked", "true")
                }
                SchedulePage.createRemindEA(context)
            } else {
                switch.isChecked = false
                NotificationManager.setupPostNotifications(requireActivity())
            }
        }

        // Смена темы приложения
        ui.settingsTheme.setOnCheckedChangeListener { _, isChecked ->
            if (CacheManager.isDarkTheme != isChecked) {
                CacheManager.isDarkTheme = isChecked
                (requireActivity() as ParentActivity).setupActivityTheme()
            }
            Utilities.log("theme = ${if (isChecked) "dark" else "light"}", tag="settings") {
                param("name", "is_dark_theme")
                param("is_checked", isChecked.toString())
            }
        }

        // Смена периода загружаемого расписания
        ui.settingsScheduleRange.addOnChangeListener { slider, _, _ ->
            val left = slider.values.first().toInt()
            val right = slider.values.last().toInt()

            if (left in -14..0 && right in 1..21 && right - left <= 31) {
                CacheManager.scheduleBefore = -left
                CacheManager.scheduleAfter = right
            } else {
                slider.values = listOf(-CacheManager.scheduleBefore, CacheManager.scheduleAfter).map { it.toFloat() }
            }
        }
    }

    private fun setupButtons() {
        // Нажатие на кнопку обновления
        ui.buttonUpdate.setOnClickListener {
            Utilities.openUrl(requireContext(), BuildConfig.DOMAIN)
            Utilities.log("openUrl(${BuildConfig.DOMAIN})", tag="open_url") {
                param("url", BuildConfig.DOMAIN)
            }
        }

        // Нажатие на кнопку открытия сайта
        ui.buttonOpenSite.setOnClickListener {
            Utilities.openUrl(requireContext(), BuildConfig.DOMAIN)
            Utilities.log("openUrl(${BuildConfig.DOMAIN})", tag="open_url") {
                param("url", BuildConfig.DOMAIN)
            }
        }

        // Нажатие на кнопку выхода
        ui.buttonLogout.setOnClickListener {
            CacheManager.apiSession = null
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            Utilities.log("logout", tag="account") {
                param("type", "logout")
            }
            requireActivity().finish()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utilities.log("SettingsPage загружена", tag="load") {
            param("place", "SettingsPage")
        }
    }
}