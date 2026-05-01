package ru.tgmaksim.activium.ui.webview

import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.School
import ru.tgmaksim.activium.ui.core.LoadState
import ru.tgmaksim.activium.ui.core.UiViewModel
import ru.tgmaksim.activium.api.MarkSchoolPostResult
import ru.tgmaksim.activium.ui.core.setShownError

class WebSchoolPostViewModel : UiViewModel() {
    private val _viewState = MutableStateFlow<LoadState<MarkSchoolPostResult>>(LoadState.Empty)
    val viewState = _viewState.asStateFlow()
    private val _likeState = MutableStateFlow<LoadState<MarkSchoolPostResult>>(LoadState.Empty)
    val likeState = _likeState.asStateFlow()

    fun resetView() {
        _viewState.value = LoadState.Empty
    }

    fun resetLike() {
        _likeState.value = LoadState.Empty
    }

    fun resetLikeError() {
        _likeState.setShownError()
    }

    fun viewPost(postId: Long) {
        viewModelScope.launch {
            executeRequest(
                _viewState,
                "viewPost",
                R.string.error_mark_school_post,
                { School.viewPost(postId) },
                { it.answer }
            )
        }
    }

    fun likePost(postId: Long, like: Boolean) {
        viewModelScope.launch {
            executeRequest(
                _likeState,
                "${if (like) "" else "un"}likePost",
                R.string.error_mark_school_post,
                { if (like) School.likePost(postId) else School.unlikePost(postId) },
                { it.answer as MarkSchoolPostResult? }
            )
        }
    }
}