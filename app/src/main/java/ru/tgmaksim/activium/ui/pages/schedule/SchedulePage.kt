package ru.tgmaksim.activium.ui.pages.schedule

import kotlin.math.min

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.animation.ValueAnimator
import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator

import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.LinearLayoutManager

import ru.tgmaksim.activium.databinding.SchedulePageBinding
import ru.tgmaksim.activium.ui.pages.schedule.skeletone.DaySkeletonAdapter
import ru.tgmaksim.activium.ui.pages.schedule.skeletone.CalendarSkeletonAdapter

/**
 * Страница с расписанием, оценками на уроках
 * @author Максим Дрючин (tgmaksim)
 * @see ru.tgmaksim.activium.ui.main.MainActivity
 * */
class SchedulePage : Fragment() {
    private lateinit var ui: SchedulePageBinding

    private var shimmerAnimator: ObjectAnimator? = null

    companion object {
        private const val SKELETON_CALENDAR_COUNT = 7
        private const val SKELETON_DAYS_COUNT = 3
        private const val SKELETON_LESSONS_COUNT = 5
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui = SchedulePageBinding.inflate(inflater, container, false)

        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupSkeletonRecyclerViews()
        startShimmer()
    }

    override fun onPause() {
        stopShimmer()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        startShimmer()
    }

    override fun onDestroyView() {
        stopShimmer()
        super.onDestroyView()
    }

    private fun setupSkeletonRecyclerViews() {
        ui.calendarRecycler.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        ui.calendarRecycler.adapter = CalendarSkeletonAdapter(SKELETON_CALENDAR_COUNT)
        ui.calendarRecycler.itemAnimator = null
        ui.calendarRecycler.setHasFixedSize(true)

        ui.dayRecycler.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        PagerSnapHelper().attachToRecyclerView(ui.dayRecycler)
        ui.dayRecycler.adapter = DaySkeletonAdapter(SKELETON_DAYS_COUNT, SKELETON_LESSONS_COUNT)
        ui.dayRecycler.scrollToPosition(min(1, SKELETON_DAYS_COUNT))
        ui.dayRecycler.itemAnimator = null
        ui.dayRecycler.setHasFixedSize(true)
    }

    private fun startShimmer() {
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
                duration = 1200L
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
    }

    private fun hideSkeleton() {
        stopShimmer()
        ui.skeletonShimmer.visibility = View.GONE
    }
}