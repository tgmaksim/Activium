package ru.tgmaksim.activium.ui

import android.view.View
import android.os.Bundle
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import java.util.concurrent.CancellationException

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.Login
import ru.tgmaksim.activium.api.Request
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.databinding.ActivityLoginBinding

/**
 * Activity для авторизации пользователя
 * @author Максим Дрючин (tgmaksim)
 * */
class LoginActivity : ParentActivity() {
    private lateinit var ui: ActivityLoginBinding
    companion object {
        var loginUrl: String? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Устанавливается сохраненная тема
        setupActivityTheme()
        super.onCreate(savedInstanceState)

        ui = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // Настройка системных полей сверху и снизу
        setupSystemBars(ui.contentContainer)

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
                val response = Login.login()

                response.error?.let { error ->
                    Utilities.log("API error(${error.type}) at login: ${error.errorMessage}")

                    if (error.errorMessage != null)
                        Utilities.showText(this, error.errorMessage)
                    else
                        Utilities.showText(this, R.string.error_api)
                }

                if (!response.status || response.answer == null)
                    return

                loginUrl = response.answer.loginUrl
            }

            if (Utilities.openUrl(this, loginUrl))
                finish()
        } catch (_: CancellationException) {
            hideLoading()
        } catch (e: Exception) {
            Utilities.log(e)
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