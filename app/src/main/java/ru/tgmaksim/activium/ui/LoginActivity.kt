package ru.tgmaksim.activium.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import android.app.Activity
import android.content.Intent

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.util.concurrent.CancellationException

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.Login
import ru.tgmaksim.activium.BuildConfig
import ru.tgmaksim.activium.api.Request
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.databinding.ActivityLoginBinding
import ru.tgmaksim.activium.utilities.datastore.CacheManager
import ru.tgmaksim.activium.utilities.datastore.SettingsManager
import ru.tgmaksim.activium.utilities.datastore.MemoryDataManager

/**
 * Activity для авторизации пользователя
 * @author Максим Дрючин (tgmaksim)
 * */
class LoginActivity : ParentActivity() {
    private lateinit var ui: ActivityLoginBinding
    companion object {
        var loginUrl: String? = null

        suspend fun logout() {
            MemoryDataManager.sessionId.value = null
            SettingsManager.setSessionId(null)
            withContext(Dispatchers.IO) {
                CacheManager.clear()
            }
        }

        fun openLoginActivity(nowActivity: Activity) {
            val intent = Intent(nowActivity, LoginActivity::class.java)
            nowActivity.startActivity(intent)
            nowActivity.finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Устанавливается сохраненная тема
        setupActivityTheme()
        super.onCreate(savedInstanceState)

        ui = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // Настройка системных полей сверху и снизу
        setupSystemBars(ui.contentContainer)

        ui.version.text = getString(R.string.version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        ui.android.text = getString(R.string.android, Build.VERSION.RELEASE,Build.VERSION.SDK_INT)

        // Настройка кнопки входа
        ui.buttonLogin.setOnClickListener {
            lifecycleScope.launch {
                showLoading()
                login()
                hideLoading()
            }
        }
    }

    private suspend fun login() {
        var loginUrl = loginUrl

        try {
            if (loginUrl == null) {
                val sessionId = SettingsManager.getSessionId()
                val firebaseToken = SettingsManager.getFirebaseMessagingToken().toString()
                val response = Login.login(sessionId, firebaseToken)

                if (!response.status || response.answer == null) {
                    if (response.error != null)
                        Utilities.log("API error(${response.error.type}) at login: ${response.error.errorMessage}")

                    if (response.error?.errorMessage != null)
                        Utilities.showText(this, response.error.errorMessage)
                    else
                        Utilities.showText(this, R.string.error_api)

                    return
                }

                SettingsManager.setSessionId(response.answer.sessionId)
                MemoryDataManager.sessionId.value = response.answer.sessionId

                loginUrl = response.answer.loginUrl
            }

            if (Utilities.openUrl(this, loginUrl))
                finish()
        } catch (_: CancellationException) {
            hideLoading()
        } catch (e: Exception) {
            Utilities.log(e, "Error at login")
            if (!Request.checkInternet())
                Utilities.showText(this, R.string.error_internet)
            else
                Utilities.showText(this, R.string.error_login)
        }
    }

    /**
     * Показ анимации загрузки у кнопки входа
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun showLoading() {
        ui.loadingOverlay.visibility = View.VISIBLE
    }

    /**
     * Скрытие анимации загрузки у кнопки входа
     * @author Максим Дрючин (tgmaksim)
     * */
    private fun hideLoading() {
        ui.loadingOverlay.visibility = View.GONE
    }
}