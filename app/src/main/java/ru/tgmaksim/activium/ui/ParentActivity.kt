package ru.tgmaksim.activium.ui

import android.os.Build
import android.view.ViewGroup

import kotlin.math.max
import androidx.core.view.ViewCompat
import kotlinx.coroutines.runBlocking
import androidx.core.view.updatePadding
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.utilities.datastore.SettingsManager
import ru.tgmaksim.activium.utilities.datastore.MemoryDataManager

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

        if (!MemoryDataManager.themeInitialized) {
            MemoryDataManager.darkTheme.value = runBlocking { SettingsManager.getDarkTheme() }
            MemoryDataManager.themeInitialized = true
        }

        val darkTheme = MemoryDataManager.darkTheme.value
        if (darkTheme)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        else
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    /**
     * Настройка системных краев (верхнего и нижнего)
     * @author Максим Дрючин (tgmaksim)
     * */
    fun setupSystemBars(contentContainer: ViewGroup) {
        ViewCompat.setOnApplyWindowInsetsListener(contentContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = 0)

            insets
        }

        // Взятие системных полей под контроль приложения для 30 <= SDK < 35
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.setDecorFitsSystemWindows(false)
        }
    }

    fun setupPaddingBottomMenu(bottomMenu: ViewGroup) {
        ViewCompat.setOnApplyWindowInsetsListener(bottomMenu) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = 0, bottom = 0)
            (v.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                max(systemBars.bottom, (10 * resources.displayMetrics.density).toInt())

            insets
        }
    }
}