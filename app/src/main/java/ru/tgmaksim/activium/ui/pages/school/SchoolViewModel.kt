package ru.tgmaksim.activium.ui.pages.school

import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException

import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.json
import ru.tgmaksim.activium.api.School
import ru.tgmaksim.activium.api.SchoolPost
import ru.tgmaksim.activium.ui.core.UiText
import ru.tgmaksim.activium.ui.LoginActivity
import ru.tgmaksim.activium.ui.core.LoadState
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.ui.core.UiViewModel
import ru.tgmaksim.activium.ui.core.setShownError
import ru.tgmaksim.activium.ui.core.setCacheError
import ru.tgmaksim.activium.api.SchoolPostsResult
import ru.tgmaksim.activium.ui.core.setCacheLoading
import ru.tgmaksim.activium.ui.core.setCacheSuccess
import ru.tgmaksim.activium.api.MarkSchoolPostResult
import ru.tgmaksim.activium.ui.core.CacheDataLoadState
import ru.tgmaksim.activium.utilities.datastore.CacheManager
import ru.tgmaksim.activium.utilities.datastore.SettingsManager

class SchoolViewModel : UiViewModel() {
    private val _postsData = MutableStateFlow<SchoolPostsResult?>(null)
    val postsData = _postsData.asStateFlow()

    private val _postsState = MutableStateFlow<CacheDataLoadState>(CacheDataLoadState.Empty)
    val postsState = _postsState.asStateFlow()

    private val _clickPostStates = MutableStateFlow<Map<Long, LoadState<MarkSchoolPostResult>>>(emptyMap())
    val clickPostStates = _clickPostStates.asStateFlow()

    private val _seePostStates = MutableStateFlow<Map<Long, LoadState<MarkSchoolPostResult>>>(emptyMap())
    val seePostStates = _seePostStates.asStateFlow()

    private var loadCachePostsJob: Job? = null
    private var loadCloudCachePostsJob: Job? = null

    companion object {
        const val CACHE_POSTS_NAME = "school_posts"
    }

    fun resetError() {
        _postsState.setShownError()
    }

    fun resetClickPost(postId: Long) {
        _clickPostStates.value = _clickPostStates.value.toMutableMap().apply { remove(postId) }
    }

    fun resetSeePost(postId: Long) {
        _seePostStates.value = _seePostStates.value.toMutableMap().apply { remove(postId) }
    }

    fun logout() {
        viewModelScope.launch {
            LoginActivity.logout()
        }
    }

    fun loadCachePosts() {
        val job = loadCachePostsJob
        if (job?.isActive == true)
            return

        loadCachePostsJob = viewModelScope.launch {
            _postsState.setCacheLoading()

            try {
                val childId = SettingsManager.getActiveChildId()

                try {
                    val entity = CacheManager.read(childId, CACHE_POSTS_NAME)
                        ?: throw CacheNullException()
                    val posts = json.decodeFromString<List<SchoolPost>>(entity.value)

                    _postsData.value = SchoolPostsResult(
                        posts = posts,
                        nextOffset = null
                    )
                    _postsState.setCacheSuccess()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (e !is CacheNullException) {
                        Utilities.log(e)
                        CacheManager.writeDnevnikCache(childId, CACHE_POSTS_NAME, value = "")
                    }
                    _postsData.value = SchoolPostsResult(
                        posts = emptyList(),
                        nextOffset = null
                    )
                    _postsState.setCacheSuccess()
                }
            } catch (_: CancellationException) {
                _postsState.setCacheError(UiText.StringResource(R.string.error_school_posts))
            }
        }
    }

    fun loadCloudPosts(offset: Int = 0) {
        val job = loadCloudCachePostsJob
        if (job?.isActive == true)
            return

        loadCloudCachePostsJob = viewModelScope.launch {
            executeRequest(
                _postsState,
                _postsData,
                "schoolPosts",
                R.string.error_school_posts,
                { School.getPosts(offset) },
                {
                    if (offset == 0) {
                        it.answer
                    } else if (it.answer == null) {
                        null
                    } else {
                        SchoolPostsResult(
                            posts = buildList {
                                addAll(_postsData.value?.posts ?: emptyList())
                                addAll(it.answer.posts)
                            },
                            nextOffset = it.answer.nextOffset
                        )
                    }
                }
            ) {
                it.answer ?: return@executeRequest

                val childId = SettingsManager.getActiveChildId()
                CacheManager.writeDnevnikCache(
                    childId,
                    CACHE_POSTS_NAME,
                    value = json.encodeToString(it.answer.posts)
                )
            }
        }
    }

    fun clickPost(postId: Long) {
        viewModelScope.launch {
            executeRequest(
                _clickPostStates,
                postId,
                "clickPost",
                R.string.error_mark_school_post,
                { School.clickPost(postId) },
                { it.answer }
            ) { postResult -> onSuccessUpdatePost(postId, postResult) }
        }
    }

    fun seePost(postId: Long) {
        viewModelScope.launch {
            executeRequest(
                _seePostStates,
                postId,
                "seePost",
                R.string.error_mark_school_post,
                { School.seePost(postId) },
                { it.answer }
            )
        }
    }

    fun updatePost(postId: Long, postResult: MarkSchoolPostResult) {
        val data = _postsData.value
        _postsData.value = data?.copy(
            posts = data.posts.map { post ->
                if (post.postId == postId) {
                    postResult.post
                } else {
                    post
                }
            }
        )
    }

    private suspend fun onSuccessUpdatePost(postId: Long, postResult: MarkSchoolPostResult) {
        updatePost(postId, postResult)

        try {
            val childId = SettingsManager.getActiveChildId()
            val entity = CacheManager.read(childId, CACHE_POSTS_NAME)
                ?: throw CacheNullException()
            val posts = json.decodeFromString<List<SchoolPost>>(entity.value)
            val newPosts = json.encodeToString(
                posts.map { post ->
                    if (post.postId == postId) {
                        postResult.post
                    } else {
                        post
                    }
                }
            )

            CacheManager.writeDnevnikCache(childId, CACHE_POSTS_NAME, value = newPosts)
        } catch (_: CacheNullException) {
        } catch (_: CancellationException) {
        } catch (e: Exception) {
            Utilities.log(e)
        }
    }
}