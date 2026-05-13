package ru.tgmaksim.activium.ui.pages.marks

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.LinearLayoutManager

import android.animation.ValueAnimator
import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator

import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.fragment.app.activityViewModels
import kotlinx.coroutines.flow.distinctUntilChanged

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.MarkLast
import ru.tgmaksim.activium.api.MarkLog
import ru.tgmaksim.activium.ui.LoginActivity
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.ui.pages.MainFragment
import ru.tgmaksim.activium.ui.core.CacheDataLoadState
import ru.tgmaksim.activium.databinding.MarksPageBinding
import ru.tgmaksim.activium.utilities.datastore.SettingsManager
import ru.tgmaksim.activium.ui.pages.marks.adapters.LastMarksAdapter
import ru.tgmaksim.activium.ui.pages.marks.adapters.SubjectMarksPeriodAdapter
import ru.tgmaksim.activium.ui.pages.marks.adapters.SubjectMarksYearAdapter
import ru.tgmaksim.activium.ui.pages.marks.skeleton.LastMarksSkeletonAdapter
import ru.tgmaksim.activium.ui.pages.marks.skeleton.SubjectMarksSkeletonAdapter

/**
 * Страница с мероприятиями и другими событиями и объявлениями школы
 * @author Максим Дрючин (tgmaksim)
 * @see ru.tgmaksim.activium.ui.main.MainActivity
 * */
class MarksPage(param: String? = null) : MainFragment(param) {
    private lateinit var ui: MarksPageBinding
    private val marksViewModel: MarksViewModel by activityViewModels()

    private var shimmerAnimator: ObjectAnimator? = null
    private var shouldAnimateShimmer = false

    private val lastMarksSkeletonAdapter = LastMarksSkeletonAdapter(SKELETON_LAST_MARKS_COUNT)
    private val lastMarksAdapter = LastMarksAdapter(::onLastMarkRating)
    private val subjectMarksPeriodSkeletonAdapter = SubjectMarksSkeletonAdapter(SKELETON_SUBJECT_MARKS_COUNTS)
    private val subjectMarksPeriodAdapter = SubjectMarksPeriodAdapter(
        false,
        onSubjectRating = ::onSubjectRating,
        onMarksRating = ::onMarksRating
    )
    private val subjectMarksYearSkeletonAdapter = SubjectMarksSkeletonAdapter(SKELETON_SUBJECT_MARKS_COUNTS)
    private val subjectMarksYearAdapter = SubjectMarksYearAdapter(
        false,
        onPeriodMarkRating = ::onMarksRating,
        onMarksRating = ::onMarksRating
    )

    private var currentData: UiMarksResult? = null
    private var currentShowNullSubjectMarks: Boolean? = null
    private var currentPeriod: Int? = null
    private var currentChildId: Long? = null
    private var currentTable = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui = MarksPageBinding.inflate(inflater, container, false)

        return ui.root
    }

    companion object {
        private const val SKELETON_LAST_MARKS_COUNT = 7
        private const val SKELETON_SUBJECT_MARKS_COUNTS = 10
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerViews()

        setupCollectors()

        setupSwipeRefresh()

        handleIntent()
    }

    override fun onResume() {
        super.onResume()
        if (shouldAnimateShimmer)
            startShimmer()
    }

    override fun onPause() {
        stopShimmer()
        super.onPause()
    }

    override fun onDestroyView() {
        stopShimmer()
        super.onDestroyView()
    }

    fun onBackPressed(): Boolean {
        if (ui.lastMarks.canScrollHorizontally(-1)) {
            ui.lastMarks.smoothScrollToPosition(0)

            return true
        }

        return false
    }

    override fun newIntent(param: String) {
        super.newIntent(param)
        handleIntent()
    }

    private fun handleIntent() {
        if (param == "update") {
            when (marksViewModel.marksState.value) {
                CacheDataLoadState.Empty, CacheDataLoadState.CacheLoading,
                CacheDataLoadState.CacheSuccess, CacheDataLoadState.CloudLoading -> Unit
                else -> {
                    param = null
                    marksViewModel.loadCloudMarks()
                }
            }
        }
    }

    private fun startShimmer() {
        ui.skeletonShimmer.visibility = View.VISIBLE
        ui.skeletonShimmer.doOnLayout {
            val startX = -ui.skeletonShimmer.width.toFloat()
            val endX = ui.root.width.toFloat()

            ui.skeletonShimmer.translationX = startX

            shimmerAnimator?.cancel()
            shimmerAnimator = ObjectAnimator.ofFloat(
                ui.skeletonShimmer,
                View.TRANSLATION_X,
                startX,
                endX
            ).apply {
                duration = 600L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                interpolator = LinearInterpolator()
                start()
            }
        }
    }

    private fun stopShimmer() {
        shimmerAnimator?.cancel()
        shimmerAnimator = null
        ui.skeletonShimmer.visibility = View.GONE
    }

    private fun setupRecyclerViews() {
        ui.lastMarks.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        ui.lastMarks.setHasFixedSize(true)
        ui.lastMarks.adapter = lastMarksSkeletonAdapter

        ui.buttonRatingText.text = null

        ui.periodTable.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        ui.periodTable.setHasFixedSize(true)
        ui.periodTable.adapter = subjectMarksPeriodSkeletonAdapter

        ui.yearTable.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        ui.yearTable.setHasFixedSize(true)
        ui.yearTable.adapter = subjectMarksYearSkeletonAdapter

        ui.buttonRating.setOnClickListener {
            currentData?.ratingKey?.let {
                onSubjectRating(it, classRating = true)
            }
        }
    }

    private fun onLastMarkRating(ratingKey: String) {
        LastMarkRatingDialog(
            ratingKey,
            currentData?.recentMarks?.find { it.ratingKey == ratingKey } ?: return,
            false
        ).show(parentFragmentManager, LastMarkRatingDialog.TAG)
    }

    private fun onSubjectRating(ratingKey: String, subject: String? = null, classRating: Boolean = false) {
        SubjectRatingDialog(
            ratingKey,
            subject,
            true,
            classRating = classRating
        ).show(parentFragmentManager, SubjectRatingDialog.TAG)
    }

    private fun onMarksRating(myMark: MarkLog, subject: String) {
        if (myMark.ratingKey == null) return

        LastMarkRatingDialog(
            myMark.ratingKey,
            MarkLast(
                mark = myMark,
                subject = subject,
                lessonDate = null,
                humanLessonDate = null,
                ratingKey = myMark.ratingKey
            ),
            false
        ).show(parentFragmentManager, LastMarkRatingDialog.TAG)
    }

    private fun setupCollectors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    SettingsManager.showNullSubjectMarksFlow().distinctUntilChanged().collect { showNullSubjectMarks ->
                        val showNullSubjectMarksChanged = currentShowNullSubjectMarks != showNullSubjectMarks && currentShowNullSubjectMarks != null

                        currentShowNullSubjectMarks = showNullSubjectMarks

                        if (showNullSubjectMarksChanged) {
                            currentData?.let { data ->
                                renderMarks(data)
                            }
                        }
                    }
                }
                launch {
                    SettingsManager.lastMarksPeriodFlow().distinctUntilChanged().collect { period ->
                        val periodChanged = currentPeriod != period

                        currentPeriod = period

                        if (periodChanged) {
                            marksViewModel.loadCloudMarks()
                        }
                    }
                }
                launch {
                    SettingsManager.activeChildIdFlow().distinctUntilChanged().collect { childId ->
                        val childChanged = currentChildId != childId

                        currentChildId = childId

                        if (childChanged) {
                            restartMarksFromScratch()
                        }
                    }
                }
                launch {
                    marksViewModel.marksState.collect { state ->
                        when (state) {
                            CacheDataLoadState.Empty -> {
                                marksViewModel.loadCacheMarks()

                                if (!shouldAnimateShimmer) {
                                    showSkeletonMode()
                                }
                            }
                            CacheDataLoadState.CacheLoading -> {
                                updateCloudLoading(false)
                            }
                            CacheDataLoadState.CacheSuccess -> {
                                shouldAnimateShimmer = false
                                stopShimmer()
                                marksViewModel.loadCloudMarks()
                            }
                            is CacheDataLoadState.CacheError -> {
                                Utilities.showUiMessage(requireContext(), state.message)
                                marksViewModel.loadCloudMarks()
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
                                marksViewModel.resetError(MarksViewModel.StateType.Marks)
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
                    marksViewModel.finalMarksState.collect { state ->
                        when (state) {
                            CacheDataLoadState.Empty -> {
                                // Загрузка при открытии
                            }
                            CacheDataLoadState.CacheLoading -> {
                                updateCloudLoading(false)
                            }
                            CacheDataLoadState.CacheSuccess -> {
                                marksViewModel.loadCloudFinalMarks()
                            }
                            is CacheDataLoadState.CacheError -> {
                                Utilities.showUiMessage(requireContext(), state.message)
                                marksViewModel.loadCloudFinalMarks()
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
                                marksViewModel.resetError(MarksViewModel.StateType.FinalMarks)
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
                    marksViewModel.marksData.collect { data ->
                        if (currentData != data) {
                            if (currentShowNullSubjectMarks == null)
                                currentShowNullSubjectMarks = SettingsManager.getShowNullSubjectMarks()
                            if (currentPeriod == null)
                                currentPeriod = SettingsManager.getLastMarksPeriod()
                            if (currentChildId == null)
                                currentChildId = SettingsManager.getActiveChildId()

                            currentData = data

                            if (data != null)
                                renderMarks(data)
                        }
                    }
                }
            }
        }
    }

    private fun restartMarksFromScratch() {
        updateCurrentTable(0)
        ui.yearTable.visibility = View.GONE
        ui.periodTable.visibility = View.VISIBLE

        showSkeletonMode()
        marksViewModel.resetMarks()
    }

    private fun renderMarks(data: UiMarksResult) {
        if (data.recentMarks.isEmpty() && data.periodMarks.isEmpty()) return

        if (ui.lastMarks.adapter !== lastMarksAdapter) {
            ui.lastMarks.adapter = lastMarksAdapter
        }
        if (ui.periodTable.adapter !== subjectMarksPeriodAdapter) {
            ui.periodTable.adapter = subjectMarksPeriodAdapter
        }
        if (ui.yearTable.adapter !== subjectMarksYearAdapter) {
            ui.yearTable.adapter = subjectMarksYearAdapter
        }

        lastMarksAdapter.submitList(data.recentMarks) {
            ui.lastMarks.scrollToPosition(0)
        }

        subjectMarksPeriodAdapter.setShowNullSubjectMarks(currentShowNullSubjectMarks ?: false)
        subjectMarksPeriodAdapter.submitList(data.periodMarks) {
            ui.periodTable.requestLayout()
        }

        subjectMarksYearAdapter.setShowNullSubjectMarks(currentShowNullSubjectMarks ?: false)
        subjectMarksYearAdapter.submitList(data.finalMarks) {
            ui.yearTable.requestLayout()
        }

        ui.buttonRatingText.text = getString(R.string.class_rating)
        updateCurrentTable(currentTable)

        ui.buttonYearTable.setOnClickListener {
            if (currentTable == 1) return@setOnClickListener

            updateCurrentTable(1)
            ui.yearTable.visibility = View.VISIBLE
            ui.periodTable.visibility = View.GONE

            if (currentData?.finalMarks.isNullOrEmpty())
                marksViewModel.loadCacheFinalMarks()
        }

        ui.buttonCurrentTable.setOnClickListener {
            if (currentTable == 0) return@setOnClickListener

            updateCurrentTable(0)
            ui.yearTable.visibility = View.GONE
            ui.periodTable.visibility = View.VISIBLE
        }
    }

    private fun showSkeletonMode() {
        currentData = null
        shouldAnimateShimmer = true

        ui.lastMarks.adapter = lastMarksSkeletonAdapter
        ui.buttonRatingText.text = null
        ui.periodTable.adapter = subjectMarksPeriodSkeletonAdapter
        ui.yearTable.adapter = subjectMarksYearSkeletonAdapter

        startShimmer()
    }

    private fun updateCloudLoading(show: Boolean) {
        ui.swipeRefresh.isRefreshing = show
    }

    private fun logout() {
        marksViewModel.logout()

        LoginActivity.openLoginActivity(requireActivity())
    }

    private fun updateCurrentTable(table: Int) {
        currentTable = table
        ui.buttonCurrentTable.isSelected = currentTable == 0
        ui.buttonYearTable.isSelected = currentTable == 1
    }

    private fun setupSwipeRefresh() {
        ui.swipeRefresh.setColorSchemeColors(requireContext().getColor(R.color.swipe_refresh_scheme))
        ui.swipeRefresh.setProgressBackgroundColorSchemeColor(requireContext().getColor(R.color.main_bg))

        ui.swipeRefresh.setDistanceToTriggerSync((150 * resources.displayMetrics.density).toInt())
        ui.swipeRefresh.setOnChildScrollUpCallback { _, _ ->
            ui.nestedScrollView.scrollY > 0
        }

        ui.swipeRefresh.setOnRefreshListener {
            when (marksViewModel.marksState.value) {
                is CacheDataLoadState.CloudSuccess, is CacheDataLoadState.CloudError, is CacheDataLoadState.ShownError -> {
                    marksViewModel.loadCloudMarks()
                }
                else -> {
                    updateCloudLoading(false)
                }
            }
        }
    }
}