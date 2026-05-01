package ru.tgmaksim.activium.ui.pages.school

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager

import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.fragment.app.activityViewModels

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.ui.LoginActivity
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.ui.pages.MainFragment
import ru.tgmaksim.activium.api.SchoolPostsResult
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
    private val schoolViewModel: SchoolViewModel by activityViewModels()

    private var currentData: SchoolPostsResult? = null

    private val postsAdapter = SchoolPostAdapter(
        onClickPost = ::onClickPost
    )

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

        postsAdapter.submitList(data.posts)
    }

    private fun onClickPost(postId: Long) {
        val posts = currentData?.posts ?: return
        val post = posts.find { it.postId == postId } ?: return

        WebSchoolPostActivity.start(requireContext(), post)

        schoolViewModel.clickPost(postId)
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