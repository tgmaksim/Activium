package ru.tgmaksim.activium

import android.os.Bundle
import android.content.Intent

import androidx.appcompat.app.AppCompatActivity

import ru.tgmaksim.activium.ui.LoginActivity
import ru.tgmaksim.activium.ui.main.MainActivity
import ru.tgmaksim.activium.utilities.datastore.MemoryDataManager

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Маршрутизация в зависимости от наличия сессии
        val sessionId = MemoryDataManager.sessionId.value

        val target = if (sessionId == null) {
            LoginActivity::class.java
        } else {
            MainActivity::class.java
        }

        startActivity(Intent(this, target).apply {
            data = intent.data
        })
        finish()
    }
}