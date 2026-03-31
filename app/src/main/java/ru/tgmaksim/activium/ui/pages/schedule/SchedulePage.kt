package ru.tgmaksim.activium.ui.pages.schedule

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.animation.ValueAnimator
import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator

import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.LinearLayoutManager

import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.combine
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged

import java.util.Locale
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.datetime.plus
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.toKotlinMonth
import java.time.format.DateTimeFormatter

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.ui.LoginActivity
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.ui.core.CacheDataLoadState
import ru.tgmaksim.activium.databinding.SchedulePageBinding
import ru.tgmaksim.activium.utilities.datastore.SettingsManager
import ru.tgmaksim.activium.ui.pages.schedule.adapters.ScheduleDayAdapter
import ru.tgmaksim.activium.ui.pages.schedule.adapters.ScheduleCalendarDayUi
import ru.tgmaksim.activium.ui.pages.schedule.adapters.ScheduleCalendarAdapter
import ru.tgmaksim.activium.ui.pages.schedule.skeletone.CalendarSkeletonAdapter

/**
 * Страница с расписанием, оценками на уроках
 * @author Максим Дрючин (tgmaksim)
 * @see ru.tgmaksim.activium.ui.main.MainActivity
 * */
class SchedulePage : Fragment() {
    private lateinit var ui: SchedulePageBinding
    private val scheduleViewModel: ScheduleViewModel by activityViewModels()

    private var shimmerAnimator: ObjectAnimator? = null

    private val calendarSkeletonAdapter = CalendarSkeletonAdapter(SKELETON_CALENDAR_COUNT)
    private val calendarAdapter = ScheduleCalendarAdapter { date -> onCalendarDayClick(date) }
    private val dayAdapter = ScheduleDayAdapter(
        skeletonLessonsCount = SKELETON_LESSONS_COUNT,
        onPraiseClick = ::onPraiseLesson
    )

    private val pagerSnapHelper = PagerSnapHelper()

    private var currentData: UiScheduleResult? = null
    private var currentBefore = 0
    private var currentAfter = 0
    private var currentSelectedDate: LocalDate? = null
    private var currentDates: List<LocalDate> = emptyList()
    private var shouldAnimateShimmer = true

    companion object {
        private const val SKELETON_CALENDAR_COUNT = 7
        private const val SKELETON_DAYS_COUNT = 1
        private const val SKELETON_LESSONS_COUNT = 5
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui = SchedulePageBinding.inflate(inflater, container, false)

        startShimmer()

        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val settings = runBlocking { SettingsManager.snapshot() }
        currentBefore = settings.beforeSchedule
        currentAfter = settings.afterSchedule

        setupRecyclerViews()

        setupCollectors()
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

    private fun setupRecyclerViews() {
        ui.calendarRecycler.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        ui.calendarRecycler.itemAnimator = null
        ui.calendarRecycler.setHasFixedSize(true)
        ui.calendarRecycler.adapter = calendarSkeletonAdapter

        ui.dayRecycler.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        ui.dayRecycler.itemAnimator = null
        ui.dayRecycler.setHasFixedSize(true)
        ui.dayRecycler.adapter = dayAdapter
        pagerSnapHelper.attachToRecyclerView(ui.dayRecycler)

        dayAdapter.submitList(List(SKELETON_DAYS_COUNT) { null })

        ui.dayRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var position: Int? = null

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                val layoutManager = ui.dayRecycler.layoutManager as? LinearLayoutManager ?: return
                val snappedView = pagerSnapHelper.findSnapView(layoutManager) ?: return
                val newPosition = layoutManager.getPosition(snappedView)

                if (position == newPosition) return
                position = newPosition

                currentDates.getOrNull(newPosition)?.let { date ->
                    if (currentSelectedDate != date) {
                        currentSelectedDate = date
                        submitCalendar(currentData)
                    }
                }
            }
        })
    }

    private fun submitCalendar(data: UiScheduleResult?, selectedIndex: Int? = null) {
        if (data == null || currentDates.isEmpty()) return

        val today = currentDateInTimezone(data.timezone)
        val activeSelectedIndex = selectedIndex ?: currentSelectedDate?.let { currentDates.indexOf(it) } ?: 0

        val items = currentDates.mapIndexed { index, date ->
            val day = data.schedule.getOrNull(index)

            ScheduleCalendarDayUi(
                date = date,
                weekday = formatWeekday(date),
                dayNumber = date.day.toString(),
                isToday = date == today,
                isSelected = index == activeSelectedIndex,
                isWeekend = day?.let { it.lessons.isEmpty() && it.ea.isEmpty() } == true
            )
        }

        calendarAdapter.submitList(items)

        centerCalendarItem(activeSelectedIndex)
    }

    private fun currentDateInTimezone(offsetSeconds: Int): LocalDate {
        val utcNow = Instant.now()
        val zone = ZoneOffset.ofTotalSeconds(offsetSeconds)
        val zoned = utcNow.atZone(zone)
        return LocalDate(
            zoned.year,
            zoned.month.toKotlinMonth(),
            zoned.dayOfMonth
        )
    }

    private fun centerCalendarItem(position: Int) {
        ui.calendarRecycler.post {
            val layoutManager = ui.calendarRecycler.layoutManager as? LinearLayoutManager ?: return@post

            val itemWidth = resources.getDimensionPixelSize(R.dimen.schedule_calendar_day_width)
            val recyclerWidth = ui.calendarRecycler.width

            val offset = recyclerWidth / 2 - itemWidth / 2

            layoutManager.scrollToPositionWithOffset(position, offset)
        }
    }

    private fun setupCollectors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(
                        SettingsManager.activeChildIdFlow(),
                        SettingsManager.beforeScheduleFlow(),
                        SettingsManager.afterScheduleFlow()
                    ) { childId, before, after ->
                        Triple(childId, before, after)
                    }
                        .distinctUntilChanged()
                        .collect {
                            // TODO: обработать изменения

                            scheduleViewModel.loadCacheSchedule()
                        }
                }
                launch {
                    scheduleViewModel.scheduleState.collect { state ->
                        when (state) {
                            CacheDataLoadState.Empty -> {
                                // Загрузка кэша начинается выше
                            }
                            CacheDataLoadState.CacheLoading -> {
                                if (!shouldAnimateShimmer) {
                                    shouldAnimateShimmer = true
                                    showSkeletonMode()
                                }
                            }
                            CacheDataLoadState.CacheSuccess -> {
                                shouldAnimateShimmer = false
                                stopShimmer()
                                scheduleViewModel.loadCloudSchedule()
                            }
                            is CacheDataLoadState.CacheError -> {
                                Utilities.showUiMessage(requireContext(), state.message)
                                scheduleViewModel.resetError(ScheduleViewModel.StateType.Schedule)
                            }
                            CacheDataLoadState.CloudLoading -> {
                                updateCloudLoading(true)
                            }
                            CacheDataLoadState.CloudSuccess -> {
                                updateCloudLoading(false)
                            }
                            is CacheDataLoadState.CloudError -> {
                                Utilities.showUiMessage(requireContext(), state.message)
                                scheduleViewModel.resetError(ScheduleViewModel.StateType.Schedule)
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
                    scheduleViewModel.scheduleData.collect { data ->
                        currentData = data
                        if (data != null)
                            renderSchedule(data)
                    }
                }
            }
        }
    }

    private fun showSkeletonMode() {
        currentData = null
        shouldAnimateShimmer = true

        ui.calendarRecycler.adapter = calendarSkeletonAdapter
        dayAdapter.submitList(List(SKELETON_DAYS_COUNT) { null })

        startShimmer()
    }

    private fun updateCloudLoading(show: Boolean) {
        ui.swipeRefresh.isRefreshing = show
    }

    private fun logout() {
        scheduleViewModel.logout()

        LoginActivity.openLoginActivity(requireActivity())
    }

    private fun renderSchedule(data: UiScheduleResult) {
        if (data.schedule.isEmpty()) return

        if (ui.calendarRecycler.adapter !== calendarAdapter) {
            ui.calendarRecycler.adapter = calendarAdapter
        }
        if (ui.dayRecycler.adapter !== dayAdapter) {
            ui.dayRecycler.adapter = dayAdapter
        }

        currentDates = buildDates(data.timezone)
        val selected = currentSelectedDate?.takeIf { it in currentDates }
            ?: getDefaultDate(data.timezone).takeIf { it in currentDates }
            ?: currentDates.getOrNull(currentBefore.coerceIn(0, currentDates.lastIndex))
            ?: currentDates.first()

        currentSelectedDate = selected
        val selectedIndex = currentDates.indexOf(selected).coerceAtLeast(0)

        dayAdapter.setHasAbilityPraise(data.hasAbilityPraise)
        dayAdapter.submitList(data.schedule)

        submitCalendar(data, selectedIndex)
        ui.dayRecycler.post {
            ui.dayRecycler.scrollToPosition(selectedIndex)
        }
    }

    private fun onCalendarDayClick(date: LocalDate) {
        val position = currentDates.indexOf(date)
        if (position < 0) return

        currentSelectedDate = date
        submitCalendar(currentData, position)
        ui.dayRecycler.post {
            ui.dayRecycler.scrollToPosition(position)
        }
    }

    private fun onPraiseLesson(lessonKey: String) {
        // позже сюда будет вызов API похвалы
        Utilities.log("praise lesson: $lessonKey", tag = "schedule")
    }

    private fun buildDates(timezone: Int): List<LocalDate> {
        val today = currentDateInTimezone(timezone)
        val first = today.minus(DatePeriod(days = currentBefore))
        return List(currentBefore + currentAfter + 1) { index ->
            first.plus(DatePeriod(days = index))
        }
    }

    private fun getDefaultDate(timezone: Int): LocalDate {
        val zoned = Instant.now().atZone(ZoneOffset.ofTotalSeconds(timezone))
        val today = LocalDate(
            zoned.year,
            zoned.month.toKotlinMonth(),
            zoned.dayOfMonth
        )

        return if (zoned.hour >= 15) today.plus(DatePeriod(days = 1)) else today
    }

    @Suppress("DEPRECATION")
    private fun formatWeekday(date: LocalDate): String {
        val javaDate = java.time.LocalDate.of(date.year, date.month.number, date.day)
        return javaDate.format(DateTimeFormatter.ofPattern("EE", Locale("ru"))).uppercase()
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
}