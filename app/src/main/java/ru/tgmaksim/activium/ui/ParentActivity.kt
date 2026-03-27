package ru.tgmaksim.activium.ui

import android.os.Build
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.updatePadding
import kotlinx.coroutines.runBlocking

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.utilities.datastore.SettingsManager

/**
 * Базовый класс для всех Activity
 * @author Максим Дрючин (tgmaksim)
 * */
open class ParentActivity : AppCompatActivity() {
    /**
     * Смена темы приложения на сохраненную в кеше
     * @author Максим Дрючин (tgmaksim)
     * */
    fun setupActivityTheme() {
        setTheme(R.style.Theme_Activium)

        runBlocking {
            val darkTheme = SettingsManager.getDarkTheme()
            if (darkTheme)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    /**
     * Настройка системных краев (верхнего и нижнего)
     * @author Максим Дрючин (tgmaksim)
     * */
    fun setupSystemBars(contentContainer: ViewGroup) {
        ViewCompat.setOnApplyWindowInsetsListener(contentContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = systemBars.bottom)

            insets
        }

        // Взятие системных полей под контроль приложения для 30 <= SDK < 35
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.setDecorFitsSystemWindows(false)
        }
    }
}