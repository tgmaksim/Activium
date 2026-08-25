package ru.tgmaksim.activium.ui.webview

import android.os.Bundle
import android.util.TypedValue
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher

import android.content.Intent
import android.content.Context

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface

import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient

import kotlinx.coroutines.launch
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import kotlin.math.abs
import kotlin.getValue
import kotlin.properties.Delegates

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.json
import ru.tgmaksim.activium.api.SchoolPost
import ru.tgmaksim.activium.ui.ParentActivity
import ru.tgmaksim.activium.ui.core.LoadState
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.api.MarkSchoolPostResult
import ru.tgmaksim.activium.utilities.datastore.MemoryDataManager
import ru.tgmaksim.activium.databinding.ActivityWebSchoolPostBinding

class WebSchoolPostActivity : ParentActivity() {
    private lateinit var ui: ActivityWebSchoolPostBinding
    private val activityViewModel: WebSchoolPostViewModel by viewModels()

    private var postId by Delegates.notNull<Long>()
    private var hasLike = false
    private var postResult: MarkSchoolPostResult? = null
    private var isViewedSent = false

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var originalOrientation: Int = 0

    // Обработчики кнопки назад или жестом для разных состояний
    private val fullScreenBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            ui.webView.webChromeClient?.onHideCustomView()
        }
    }
    private val activityBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (ui.webView.canScrollVertically(-1))
                ui.webView.evaluateJavascript("window.scrollTo({ top: 0, behavior: 'smooth' });", null)
            else
                finishWithResult()
        }
    }
    private val imageZoomBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            ui.webView.evaluateJavascript("closePhotoSwipe();", null)
        }
    }

    private val thresholdPx by lazy {
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            100f,
            resources.displayMetrics
        )
    }
    private val scrollThreshold by lazy {
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            15f,
            resources.displayMetrics
        )
    }

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_LIKE = "like"
        private const val EXTRA_ID = "id"

        fun start(launcher: ActivityResultLauncher<Intent>, context: Context, post: SchoolPost) {
            val intent = Intent(context, WebSchoolPostActivity::class.java).apply {
                putExtra(EXTRA_URL, post.postUrl)
                putExtra(EXTRA_LIKE, post.hasMyLike)
                putExtra(EXTRA_ID, post.postId)
            }
            launcher.launch(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Устанавливается сохраненная тема
        setupActivityTheme()
        super.onCreate(savedInstanceState)

        ui = ActivityWebSchoolPostBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // Настройка системных полей сверху и снизу
        setupSystemBars(ui.root)
        setupPaddingBottomMenu(ui.bottomBar)

        postId = intent.getLongExtra(EXTRA_ID, -1).takeIf { it != -1L } ?: return finish()
        hasLike = intent.getBooleanExtra(EXTRA_LIKE, false)

        val url = intent.getStringExtra(EXTRA_URL)?.let {
            "$it?isDarkTheme=${if (MemoryDataManager.darkTheme.value) "true" else "false"}"
        } ?: return finish()

        setupWebView()
        setupButtons()

        ui.webView.loadUrl(url)

        updateLikeUI()

        setupSwipeRefresh()

        setupScrollTracking()
        setupCollectors()
    }

    private fun finishWithResult() {
        val intent = Intent().apply {
            putExtra("postId", postId)
            putExtra("postResult", postResult?.let { json.encodeToString(it) })
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    @SuppressLint("SetJavaScriptEnabled", "MissingOnRenderProcessGone")
    private fun setupWebView() {
        ui.swipeRefresh.isRefreshing = true
        ui.webView.setBackgroundColor(getColor(R.color.main_bg))

        ui.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }

        ui.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                ui.webView.background = null
                ui.swipeRefresh.isRefreshing = false
            }
        }

        // Обработка данных от JS
        ui.webView.addJavascriptInterface(object {
            @JavascriptInterface  // Открытие или скрытие полноэкранного просмотра картинки
            fun setImageOverlayVisible(isVisible: Boolean) = runOnUiThread {
                // Смена обработчика кнопки назад или жестом
                imageZoomBackPressedCallback.isEnabled = isVisible
                activityBackPressedCallback.isEnabled = !isVisible

                // Скрытие или возвращение нижнего меню
                ui.bottomBar.visibility = if (isVisible) View.INVISIBLE else View.VISIBLE
            }
        }, "AndroidBridge")

        // Обработка полноэкранного режима видео
        ui.webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }

                customView = view
                customViewCallback = callback
                originalOrientation = requestedOrientation

                val decorView = window.decorView as ViewGroup
                decorView.addView(
                    customView, ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )

                val windowInsetsController = WindowCompat.getInsetsController(window, decorView)
                windowInsetsController.let {
                    it.hide(WindowInsetsCompat.Type.systemBars())
                    it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }

                fullScreenBackPressedCallback.isEnabled = true
                activityBackPressedCallback.isEnabled = false
            }

            override fun onHideCustomView() {
                val decorView = window.decorView as ViewGroup
                decorView.removeView(customView)
                customView = null

                customViewCallback?.onCustomViewHidden()

                val windowInsetsController = WindowCompat.getInsetsController(window, decorView)
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())

                requestedOrientation = originalOrientation

                fullScreenBackPressedCallback.isEnabled = false
                activityBackPressedCallback.isEnabled = true
            }
        }
    }

    private fun updateLikeUI() {
        ui.buttonLike.setImageResource(
            if (hasLike) R.drawable.like else R.drawable.like_outline
        )
    }

    private fun setupButtons() {
        ui.buttonClose.setOnClickListener {
            finishWithResult()
        }

        ui.buttonLike.setOnClickListener {
            toggleLike()
        }

        onBackPressedDispatcher.addCallback(this, activityBackPressedCallback)
        onBackPressedDispatcher.addCallback(this, fullScreenBackPressedCallback)
        onBackPressedDispatcher.addCallback(this, imageZoomBackPressedCallback)
    }

    private fun toggleLike() {
        activityViewModel.likePost(postId, !hasLike)
    }

    private fun setupScrollTracking() {
        // При пролистывании почти всей страницы отправляется просмотр
        ui.webView.viewTreeObserver.addOnScrollChangedListener {
            val contentHeight = ui.webView.contentHeight * ui.webView.scaleY
            val scrollY = ui.webView.scrollY + ui.webView.height

            if (!isViewedSent && contentHeight - scrollY <= thresholdPx) {
                isViewedSent = true
                sendViewed()
            }
        }

        // Скрытие и появление нижнего меню при прокрутке страницы
        ui.webView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (abs(scrollY - oldScrollY) > scrollThreshold) {
                if (scrollY > oldScrollY)
                    ui.bottomBar.visibility = View.INVISIBLE
                else if (scrollY < oldScrollY)
                    ui.bottomBar.visibility = View.VISIBLE
            }
        }
    }

    private fun setupSwipeRefresh() {
        ui.swipeRefresh.setColorSchemeColors(getColor(R.color.swipe_refresh_scheme))
        ui.swipeRefresh.setProgressBackgroundColorSchemeColor(getColor(R.color.main_bg))
    }

    private fun sendViewed() {
        activityViewModel.viewPost(postId)
    }

    private fun setupCollectors() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    activityViewModel.viewState.collect { state ->
                        if (state is LoadState.Success) {
                            postResult = state.data
                            updateCountViewings(state.data.post.countViewings)
                            activityViewModel.resetView()
                        }
                    }
                }
                launch {
                    activityViewModel.likeState.collect { state ->
                        when (state) {
                            LoadState.Empty -> {
                                ui.likeLoading.visibility = View.GONE
                                ui.buttonLike.visibility = View.VISIBLE
                                ui.likeError.visibility = View.GONE
                            }
                            LoadState.Loading -> {
                                ui.likeLoading.visibility = View.VISIBLE
                                ui.buttonLike.visibility = View.GONE
                                ui.likeError.visibility = View.GONE
                            }
                            is LoadState.Success -> {
                                postResult = state.data
                                updateCountLikes(state.data.post.countLikes)
                                hasLike = state.data.post.hasMyLike
                                updateLikeUI()
                                activityViewModel.resetLike()
                            }
                            is LoadState.Error -> {
                                Utilities.showUiMessage(this@WebSchoolPostActivity, state.message)
                                activityViewModel.resetLikeError()
                            }
                            LoadState.ShownError -> {
                                ui.likeLoading.visibility = View.GONE
                                ui.buttonLike.visibility = View.GONE
                                ui.likeError.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateCountViewings(count: Int) {
        ui.webView.evaluateJavascript("updateCountViewings($count)", null)
    }

    private fun updateCountLikes(count: Int) {
        ui.webView.evaluateJavascript("updateCountLikes($count)", null)
    }
}