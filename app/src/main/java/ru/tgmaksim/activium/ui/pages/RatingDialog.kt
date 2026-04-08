package ru.tgmaksim.activium.ui.pages

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.launch
import android.view.LayoutInflater
import androidx.lifecycle.Lifecycle
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager

import java.util.Locale
import java.time.ZoneId
import kotlin.time.toJavaInstant
import java.time.format.DateTimeFormatter

import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.ui.core.LoadState
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.databinding.RatingSheetBinding
import ru.tgmaksim.activium.ui.pages.schedule.UiScheduleLesson

class RatingDialog(
    private val lesson: UiScheduleLesson,
    private val showNumber: Boolean
) : BottomSheetDialogFragment() {
    private lateinit var ui: RatingSheetBinding
    private lateinit var oldAvgMarkAdapter: MarkLogAdapter
    private lateinit var newAvgMarkAdapter: MarkLogAdapter

    private val ratingViewModel: RatingViewModel by viewModels()

    companion object {
        const val TAG = "RatingDialog"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui = RatingSheetBinding.inflate(inflater, container, false)

        setupAvgMarks()
        setupMyMark()
        setupInfo()
        setupAvgGroupMark()
        setupList()

        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCollectors()
    }

    private fun setupAvgMarks() {
        oldAvgMarkAdapter = MarkLogAdapter()
        newAvgMarkAdapter = MarkLogAdapter()
    }

    private fun setupMyMark() {
        if (lesson.logs.isEmpty()) {
            ui.myMark.root.visibility = View.GONE
            return
        }

        ui.myMark.number.visibility = View.GONE
        ui.myMark.studentName.text = getString(if (lesson.logs.size == 1) R.string.my_mark else R.string.my_marks)

        ui.myMark.logs.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        ui.myMark.logs.itemAnimator = null
        ui.myMark.logs.adapter = MarkLogAdapter().apply {
            submitList(lesson.logs)
        }
    }

    private fun setupInfo() {
        @Suppress("DEPRECATION")  // У Local нет России
        val formatter = DateTimeFormatter.ofPattern("d MMMM в HH:mm")
            .withLocale(Locale("ru")).withZone(ZoneId.systemDefault())
        val datetime = lesson.logs
            .filter { it.created != null }
            .minByOrNull { it.created!!.epochSeconds }
            ?.created
            ?.toJavaInstant()
        val datetimeFormat = datetime?.let { formatter.format(it) }

        ui.info.text = getString(
            R.string.rating_info,
            lesson.subject,
            lesson.works.joinToString(", ") { it.title },
            datetimeFormat?.let { getString(R.string.rating_info_time, it) }.orEmpty()
        )
    }

    private fun setupAvgGroupMark() {
        if (lesson.othersMarks.isEmpty() || lesson.avgGroupLessonMark == null) {
            ui.avgGroupMark.root.visibility = View.GONE
            return
        }

        ui.avgGroupMark.number.visibility = View.GONE
        ui.avgGroupMark.studentName.text = getString(R.string.avg_group_lesson_mark)

        ui.avgGroupMark.logs.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        ui.avgGroupMark.logs.itemAnimator = null
        ui.avgGroupMark.logs.adapter = MarkLogAdapter().apply {
            submitList(listOf(lesson.avgGroupLessonMark))
        }
    }

    private fun setupList() {
        ui.ratingList.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        ui.ratingList.itemAnimator = null
        ui.ratingList.adapter = RatingAdapter(showNumber).apply {
            submitList(lesson.othersMarks)
        }
    }

    private fun setupCollectors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ratingViewModel.lessonRatingState.collect { state ->
                    when (state) {
                        is LoadState.Empty -> {
                            ratingViewModel.loadLessonRatingStats(lesson.ratingKey!!)
                        }
                        is LoadState.Loading -> {
                            ui.oldAvgMarkLoading.visibility = View.VISIBLE
                            ui.newAvgMarkLoading.visibility = View.VISIBLE
                            ui.oldAvgMark.root.visibility = View.GONE
                            ui.newAvgMark.root.visibility = View.GONE
                        }
                        is LoadState.Success -> {
                            ui.oldAvgMarkLoading.visibility = View.GONE
                            ui.newAvgMarkLoading.visibility = View.GONE

                            state.data.oldAvgMark?.let {
                                oldAvgMarkAdapter.submitList(listOf(it))
                                val holder = MarkLogAdapter.VH(ui.oldAvgMark)
                                oldAvgMarkAdapter.onBindViewHolder(holder, 0)
                                ui.oldAvgMark.root.visibility = View.VISIBLE
                            }
                            state.data.newAvgMark?.let {
                                newAvgMarkAdapter.submitList(listOf(it))
                                val holder = MarkLogAdapter.VH(ui.newAvgMark)
                                newAvgMarkAdapter.onBindViewHolder(holder, 0)
                                ui.newAvgMark.root.visibility = View.VISIBLE
                            }
                        }
                        is LoadState.Error -> {
                            ui.stats.visibility = View.GONE

                            Utilities.showUiMessage(requireContext(), state.message)
                            ratingViewModel.resetLessonRating()
                        }
                        is LoadState.ShownError -> {
                            // ОШибка уже показан
                        }
                    }
                }
            }
        }
    }
}