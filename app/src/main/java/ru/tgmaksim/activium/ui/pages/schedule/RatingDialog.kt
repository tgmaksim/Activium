package ru.tgmaksim.activium.ui.pages.schedule

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.launch
import android.view.LayoutInflater
import androidx.lifecycle.Lifecycle
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager

import java.util.Locale
import java.time.ZoneId
import kotlin.time.toJavaInstant
import java.time.format.DateTimeFormatter

import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.ui.core.toUi
import ru.tgmaksim.activium.ui.core.LoadState
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.ui.pages.RatingAdapter
import ru.tgmaksim.activium.ui.pages.MarkLogAdapter
import ru.tgmaksim.activium.api.LessonRatingStatsResult
import ru.tgmaksim.activium.databinding.RatingSheetBinding
import ru.tgmaksim.activium.ui.pages.RatingDialogSwipeHelper

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
        val ratingAdapter = RatingAdapter(showNumber).apply {
            submitList(lesson.othersMarks.toUi())
        }
        ui.ratingList.adapter = ratingAdapter

        ItemTouchHelper(RatingDialogSwipeHelper(
            ui.ratingList,
            { position ->
                ratingAdapter.currentList[position].isHighlighting != null &&
                        ratingAdapter.currentList[position].personKey != null
            },
            { position ->
                val person = ratingAdapter.currentList[position]
                if (person.isHighlighting == true)
                    Pair(getString(R.string.unhighlight_person), R.drawable.ic_arrow_down_rating)
                else
                    Pair(getString(R.string.highlight_person), R.drawable.ic_arrow_up_rating)
            },
            {
                Utilities.showAlertDialog(
                    requireContext(),
                    getString(R.string.title_dialog_highlight_at_marks),
                    getString(R.string.message_dialog_highlight_at_marks),
                    getString(R.string.ok)
                )
            }
        )).attachToRecyclerView(ui.ratingList)
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
                            updateLoading(true)
                        }
                        is LoadState.Success -> {
                            updateLoading(false)

                            renderLessonRatingStats(state.data)
                        }
                        is LoadState.Error -> {
                            ui.stats.visibility = View.GONE

                            Utilities.showUiMessage(requireContext(), state.message)
                            ratingViewModel.resetLessonRating()
                        }
                        is LoadState.ShownError -> {
                            // Ошибка уже показан
                        }
                    }
                }
            }
        }
    }

    private fun updateLoading(loading: Boolean) {
        if (ratingViewModel.lessonRatingState.value is LoadState.Success) {
            ui.loading.visibility = if (loading) View.VISIBLE else View.GONE
            ui.oldAvgMarkLoading.visibility = View.GONE
            ui.newAvgMarkLoading.visibility = View.GONE
        } else {
            ui.loading.visibility = View.GONE
            ui.oldAvgMarkLoading.visibility = if (loading) View.VISIBLE else View.GONE
            ui.newAvgMarkLoading.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun renderLessonRatingStats(data: LessonRatingStatsResult) {
        data.oldAvgMark?.let {
            oldAvgMarkAdapter.submitList(listOf(it))
            val holder = MarkLogAdapter.VH(ui.oldAvgMark)
            oldAvgMarkAdapter.onBindViewHolder(holder, 0)
            ui.oldAvgMark.root.visibility = View.VISIBLE
        }
        data.newAvgMark?.let {
            newAvgMarkAdapter.submitList(listOf(it))
            val holder = MarkLogAdapter.VH(ui.newAvgMark)
            newAvgMarkAdapter.onBindViewHolder(holder, 0)
            ui.newAvgMark.root.visibility = View.VISIBLE
        }

        if (data.oldAvgMark == null && data.newAvgMark == null) {
            ui.stats.visibility = View.GONE
        }
    }
}