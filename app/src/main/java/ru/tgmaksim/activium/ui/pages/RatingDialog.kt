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

import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.ui.core.LoadState
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.databinding.RatingSheetBinding
import ru.tgmaksim.activium.ui.pages.schedule.UiScheduleLesson
import ru.tgmaksim.activium.databinding.ItemMarksRatingBinding
import ru.tgmaksim.activium.ui.pages.schedule.adapters.MarkLogAdapter

class RatingDialog(
    private val lesson: UiScheduleLesson,
    private val showNumber: Boolean
) : BottomSheetDialogFragment() {
    private lateinit var ui: RatingSheetBinding

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
        setupMyMark(inflater)
        setupList()

        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCollectors()
    }

    private fun setupAvgMarks() {
        ui.oldAvgMark.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        ui.oldAvgMark.itemAnimator = null
        ui.oldAvgMark.adapter = MarkLogAdapter()

        ui.newAvgMark.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        ui.newAvgMark.itemAnimator = null
        ui.newAvgMark.adapter = MarkLogAdapter()
    }

    private fun setupMyMark(layoutInflater: LayoutInflater) {
        if (lesson.logs.isEmpty()) {
            ui.myMark.visibility = View.GONE
            return
        }

        val view = ItemMarksRatingBinding.inflate(layoutInflater, ui.root, false)

        view.number.visibility = View.GONE
        view.studentName.text = getString(if (lesson.logs.size == 1) R.string.my_mark else R.string.my_marks)

        view.logs.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        view.logs.itemAnimator = null
        view.logs.adapter = MarkLogAdapter().apply {
            submitList(lesson.logs)
        }

        ui.myMark.addView(view.root)
    }

    private fun setupList() {
        ui.ratingList.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        ui.ratingList.adapter = RatingAdapter(showNumber).apply {
            submitList(lesson.othersMarks)
        }
    }

    private fun setupCollectors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ratingViewModel.lessonRatingStates.collect { state ->
                    when (state) {
                        is LoadState.Empty -> {
                            ratingViewModel.loadLessonRatingStats(lesson.ratingKey!!)
                        }
                        is LoadState.Loading -> {
                            ui.oldAvgMarkLoading.visibility = View.VISIBLE
                            ui.newAvgMarkLoading.visibility = View.VISIBLE
                            ui.oldAvgMark.visibility = View.GONE
                            ui.newAvgMark.visibility = View.GONE
                        }
                        is LoadState.Success -> {
                            ui.oldAvgMarkLoading.visibility = View.GONE
                            ui.newAvgMarkLoading.visibility = View.GONE

                            (ui.oldAvgMark.adapter as MarkLogAdapter)
                                .submitList(listOfNotNull(state.data.oldAvgMark))
                            (ui.newAvgMark.adapter as MarkLogAdapter)
                                .submitList(listOfNotNull(state.data.newAvgMark))

                            ui.oldAvgMark.visibility = View.VISIBLE
                            ui.newAvgMark.visibility = View.VISIBLE
                        }
                        is LoadState.Error -> {
                            ui.stats.visibility = View.GONE

                            Utilities.showUiMessage(requireContext(), state.message)
                            ratingViewModel.reset()
                        }
                        is LoadState.ShownError -> {

                        }
                    }
                }
            }
        }
    }
}