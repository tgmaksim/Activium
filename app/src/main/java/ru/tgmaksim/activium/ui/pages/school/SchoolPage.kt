package ru.tgmaksim.activium.ui.pages.school

import android.os.Bundle
import android.view.View
import android.app.Activity
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.activity.result.contract.ActivityResultContracts

import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.json
import ru.tgmaksim.activium.ui.LoginActivity
import ru.tgmaksim.activium.ui.core.LoadState
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.ui.main.MainActivity
import ru.tgmaksim.activium.ui.pages.MainFragment
import ru.tgmaksim.activium.api.SchoolPostsResult
import ru.tgmaksim.activium.api.MarkSchoolPostResult
import ru.tgmaksim.activium.ui.pages.SchoolPostAdapter
import ru.tgmaksim.activium.ui.core.CacheDataLoadState
import ru.tgmaksim.activium.databinding.SchoolPageBinding
import ru.tgmaksim.activium.ui.webview.WebSchoolPostActivity

/**
 * Страница с мероприятиями и другими событиями и объявлениями школы
 * @author Максим Дрючин (tgmaksim)
 * @see ru.tgmaksim.activium.ui.main.MainActivity
 * */
class SchoolPage(param: String? = null) : MainFragment(param) {
    private lateinit var ui: SchoolPageBinding
    private val schoolViewModel
        get() = (requireActivity() as MainActivity).schoolViewModel

    private var currentData: SchoolPostsResult? = null

    private val postsAdapter = SchoolPostAdapter(
        onClickPost = ::onClickPost
    )

    private val postLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val postId = result.data?.getLongExtra("postId", -1L)?.takeIf {
                    it != -1L
                } ?: return@registerForActivityResult
                val stringPost = result.data?.getStringExtra("postResult")
                val newPostResult = try {
                    stringPost?.let { json.decodeFromString<MarkSchoolPostResult>(it) }
                } catch (_: Exception) {
                    null
                } ?: return@registerForActivityResult

                (activity as MainActivity).updateNewSchoolPosts(newPostResult.countPostsWithoutVision)
                schoolViewModel.updatePost(postId, newPostResult)
            }

            requireActivity().recreate()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui = SchoolPageBinding.inflate(inflater, container, false)

        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()

        setupCollectors()

        setupSwipeRefresh()
    }

    private fun setupRecyclerView() {
        ui.postsRecycler.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        ui.postsRecycler.adapter = postsAdapter

        ui.postsRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy <= 0) return // только вниз

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager

                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                val threshold = 3
                val nextOffset = currentData?.nextOffset

                if (nextOffset != null && lastVisibleItem >= totalItemCount - threshold) {
                    when (schoolViewModel.postsState.value) {
                        is CacheDataLoadState.CloudSuccess, is CacheDataLoadState.CloudError,
                        is CacheDataLoadState.ShownError -> {
                            schoolViewModel.loadCloudPosts(nextOffset)
                        }
                        else -> {}
                    }
                }
            }
        })
    }

    private fun setupCollectors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    schoolViewModel.postsState.collect { state ->
                        when (state) {
                            CacheDataLoadState.Empty -> {
                                schoolViewModel.loadCachePosts()
                            }
                            CacheDataLoadState.CacheLoading -> {
                                updateCloudLoading(false)
                            }
                            CacheDataLoadState.CacheSuccess -> {
                                schoolViewModel.loadCloudPosts()
                            }
                            is CacheDataLoadState.CacheError -> {
                                Utilities.showUiMessage(requireContext(), state.message)
                                schoolViewModel.loadCloudPosts()
                            }
                            CacheDataLoadState.CloudLoading -> {
                                updateCloudLoading(true)
                            }
                            CacheDataLoadState.CloudSuccess -> {
                                updateCloudLoading(false)
                            }
                            is CacheDataLoadState.CloudError -> {
                                updateCloudLoading(false)
                                Utilities.showUiMessage(requireContext(), state.message)
                                schoolViewModel.resetError()
                                if (state.unauthorized)
                                    logout()
                            }
                            CacheDataLoadState.ShownError -> {
                                // Ошибка уже показана
                            }
                        }
                    }
                }
                launch {
                    schoolViewModel.postsData.collect { data ->
                        if (currentData != data) {
                            currentData = data

                            if (data != null)
                                renderPosts(data)
                        }
                    }
                }
                launch {
                    schoolViewModel.clickPostStates.collect { states ->
                        for ((postId, state) in states) {
                            if (state is LoadState.Success) {
                                (activity as MainActivity).updateNewSchoolPosts(state.data.countPostsWithoutVision)
                            }

                            schoolViewModel.resetClickPost(postId)
                        }
                    }
                }
                launch {
                    schoolViewModel.seePostStates.collect { states ->
                        for ((postId, state) in states) {
                            if (state is LoadState.Success) {
                                (activity as MainActivity).updateNewSchoolPosts(state.data.countPostsWithoutVision)
                            }

                            schoolViewModel.resetSeePost(postId)
                        }
                    }
                }
                launch {
                    val mainActivity = (requireActivity() as MainActivity)
                    mainActivity.activityViewModel.adState.collect { state ->
                        if (state is LoadState.Success && state.data.ad != null) {
                            mainActivity.renderAdBanner(ui.adBanner, state.data.ad)
                            ui.adBanner.root.visibility = View.VISIBLE
                        } else {
                            ui.adBanner.root.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun updateCloudLoading(show: Boolean) {
        ui.swipeRefresh.isRefreshing = show
    }

    private fun logout() {
        schoolViewModel.logout()

        LoginActivity.openLoginActivity(requireActivity())
    }

    private fun renderPosts(data: SchoolPostsResult) {
        if (data.posts.isEmpty()) {
            ui.noPosts.visibility = View.VISIBLE
            ui.postsRecycler.visibility = View.GONE
            return
        }

        ui.noPosts.visibility = View.GONE
        ui.postsRecycler.visibility = View.VISIBLE

        if (ui.postsRecycler.adapter !== postsAdapter)
            ui.postsRecycler.adapter = postsAdapter

        val checker = postsAdapter.settingsScroll(
            ui.postsRecycler,
            ui.postsRecycler,
            ::onSeePost
        )

        postsAdapter.submitList(data.posts) {
            checker()
        }
    }

    private fun onClickPost(postId: Long) {
        val posts = currentData?.posts ?: return
        val post = posts.find { it.postId == postId } ?: return

        WebSchoolPostActivity.start(postLauncher, requireContext(), post)

        schoolViewModel.clickPost(postId)
    }

    private fun onSeePost(postId: Long) {
        val posts = currentData?.posts ?: return
        val post = posts.find { it.postId == postId } ?: return

        if (post.isSaw) return

        schoolViewModel.seePost(postId)
    }

    private fun setupSwipeRefresh() {
        ui.swipeRefresh.setColorSchemeColors(requireContext().getColor(R.color.swipe_refresh_scheme))
        ui.swipeRefresh.setProgressBackgroundColorSchemeColor(requireContext().getColor(R.color.main_bg))

        ui.swipeRefresh.setDistanceToTriggerSync((150 * resources.displayMetrics.density).toInt())
        ui.swipeRefresh.setOnChildScrollUpCallback { _, _ ->
            ui.postsRecycler.scrollY > 0
        }

        ui.swipeRefresh.setOnRefreshListener {
            when (schoolViewModel.postsState.value) {
                is CacheDataLoadState.CloudSuccess, is CacheDataLoadState.CloudError, is CacheDataLoadState.ShownError -> {
                    schoolViewModel.loadCloudPosts()
                }
                else -> {
                    updateCloudLoading(false)
                }
            }
        }
    }
}