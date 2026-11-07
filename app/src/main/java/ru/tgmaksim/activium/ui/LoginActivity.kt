package ru.tgmaksim.activium.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import android.app.Activity
import android.content.Intent

import android.text.Spanned
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.text.method.LinkMovementMethod

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers

import androidx.core.net.toUri

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

        suspend fun logout(deleteSession: Boolean = false) {
            if (deleteSession) {
                MemoryDataManager.sessionId.value = null
                SettingsManager.setSessionId(null)
            }

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

        setupAgreement()

        // Настройка кнопки входа
        ui.buttonLogin.setOnClickListener {
            lifecycleScope.launch {
                showLoading()
                login()
                hideLoading()
            }
        }
    }

    private fun setupAgreement() {
        val privacyUrl = "${BuildConfig.DOMAIN}/privacy"
        val termsUrl = "${BuildConfig.DOMAIN}/terms"

        val agreementText = getString(R.string.login_agreement)
        val spannable = SpannableString(agreementText)

        addLink(spannable, getString(R.string.privacy), privacyUrl)
        addLink(spannable, getString(R.string.terms), termsUrl)

        ui.textAgreement.text = spannable
        ui.textAgreement.movementMethod = LinkMovementMethod.getInstance()

        ui.checkAgreement.setOnCheckedChangeListener { _, _ ->
            updateLoginButton()
        }

        updateLoginButton()
    }

    fun addLink(spannable: SpannableString, text: String, url: String) {
        val agreementText = getString(R.string.login_agreement)

        val start = agreementText.indexOf(text)
        if (start == -1) return

        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    startActivity(intent)
                }

                override fun updateDrawState(ds: android.text.TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = getColor(R.color.lesson_link)
                    ds.isUnderlineText = true
                }
            },
            start,
            start + text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun updateLoginButton() {
        ui.buttonLogin.isEnabled = ui.checkAgreement.isChecked
        ui.buttonLogin.isClickable = ui.checkAgreement.isChecked
        ui.buttonLogin.alpha = if (ui.checkAgreement.isChecked) 1f else 0.5f
    }

    private suspend fun login() {
        if (!ui.checkAgreement.isChecked) return

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