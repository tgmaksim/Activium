package ru.tgmaksim.activium.ui

import android.os.Bundle
import android.webkit.WebView
import android.graphics.Bitmap
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.annotation.SuppressLint

import ru.tgmaksim.activium.databinding.WebViewBinding

/**
 * Fragment для отображения веб-страницы в окне приложения
 * @author Максим Дрючин (tgmaksim)
 * */
class DocumentView : ParentActivity() {
    private lateinit var ui: WebViewBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Устанавливается сохраненная тема
        setupActivityTheme()
        super.onCreate(savedInstanceState)

        ui = WebViewBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // Настройка системных полей сверху и снизу
        setupSystemBars(ui.root)

        ui.webView.settings.apply {
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            javaScriptEnabled = true
            domStorageEnabled = true
        }

        ui.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                ui.swipeRefresh.isRefreshing = true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                if (view?.contentHeight == 0)
                    view.reload()
                else
                    ui.swipeRefresh.isRefreshing = false
            }
        }
        ui.webView.webChromeClient = WebChromeClient()

        ui.swipeRefresh.setOnRefreshListener {
            ui.webView.reload()
        }

        val url = intent.getStringExtra("url")
        url?.let {
            ui.webView.loadUrl(it)
            ui.swipeRefresh.isRefreshing = true
        }
    }
}