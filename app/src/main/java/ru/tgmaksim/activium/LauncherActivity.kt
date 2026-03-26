package ru.tgmaksim.activium

import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

import ru.tgmaksim.activium.ui.MainActivity
import ru.tgmaksim.activium.ui.LoginActivity
import ru.tgmaksim.activium.utilities.datastore.SettingsManager

class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Маршрутизация в зависимости от наличия сессии
        lifecycleScope.launch {
            val sessionId = SettingsManager.getSessionId()

            val target = if (sessionId == null) {
                LoginActivity::class.java
            } else {
                MainActivity::class.java
            }

            startActivity(Intent(this@LauncherActivity, target))
            finish()
        }
    }
}