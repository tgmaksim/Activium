package ru.tgmaksim.activium

import android.os.Bundle
import android.content.Intent

import kotlinx.coroutines.runBlocking
import androidx.appcompat.app.AppCompatActivity

import ru.tgmaksim.activium.ui.LoginActivity
import ru.tgmaksim.activium.ui.main.MainActivity
import ru.tgmaksim.activium.utilities.datastore.SettingsManager
import ru.tgmaksim.activium.utilities.datastore.MemoryDataManager

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Если сессия не была загружена в фоне, то загружается в потоке
        val sessionId = MemoryDataManager.sessionId.value ?: runBlocking { SettingsManager.getSessionId() }

        // Маршрутизация в зависимости от наличия сессии
        val target = if (sessionId == null) {
            LoginActivity::class.java
        } else {
            MainActivity::class.java
        }

        startActivity(Intent(this, target).apply {
            data = intent.data
            intent.extras?.let { putExtras(it) }
        })
        finish()
    }
}