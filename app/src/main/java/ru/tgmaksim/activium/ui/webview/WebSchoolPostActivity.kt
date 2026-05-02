package ru.tgmaksim.activium.ui.webview

import android.os.Bundle
import android.view.View
import android.util.TypedValue
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher

import android.content.Intent
import android.content.Context
import android.webkit.WebViewClient
import android.annotation.SuppressLint

import kotlinx.coroutines.launch
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

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

    private val thresholdPx by lazy {
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            100f,
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

        ui.webView.loadUrl(url)

        setupWebView()
        updateLikeUI()

        setupButtons()

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

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        ui.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }

        ui.webView.webViewClient = WebViewClient()
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

        onBackPressedDispatcher.addCallback(this) {
            if (ui.webView.canScrollVertically(-1))
                ui.webView.scrollTo(0, 0)
            else
                finishWithResult()
        }
    }

    private fun toggleLike() {
        activityViewModel.likePost(postId, !hasLike)
    }

    private fun setupScrollTracking() {
        ui.webView.viewTreeObserver.addOnScrollChangedListener {
            val contentHeight = ui.webView.contentHeight * ui.webView.scaleY
            val scrollY = ui.webView.scrollY + ui.webView.height

            if (!isViewedSent && contentHeight - scrollY <= thresholdPx) {
                isViewedSent = true
                sendViewed()
            }
        }
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