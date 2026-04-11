package ru.tgmaksim.activium.ui.pages.marks

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
import ru.tgmaksim.activium.api.MarkLast
import ru.tgmaksim.activium.ui.core.toUi
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.ui.pages.RatingAdapter
import ru.tgmaksim.activium.ui.pages.MarkLogAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import ru.tgmaksim.activium.api.MarksRatingStatsResult
import ru.tgmaksim.activium.ui.core.CacheDataLoadState
import ru.tgmaksim.activium.databinding.RatingSheetBinding
import ru.tgmaksim.activium.ui.pages.RatingDialogSwipeHelper

class LastMarkRatingDialog(
    private val ratingKey: String,
    private val myMark: MarkLast,
    private val showNumber: Boolean
) : BottomSheetDialogFragment() {
    private lateinit var ui: RatingSheetBinding
    private lateinit var oldAvgMarkAdapter: MarkLogAdapter
    private lateinit var newAvgMarkAdapter: MarkLogAdapter

    private val ratingViewModel: LastMarkRatingViewModel by viewModels()

    private var swipeHelper: ItemTouchHelper? = null

    companion object {
        const val TAG = "LastMarkRatingDialog"
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
        ui.myMark.number.visibility = View.GONE
        ui.myMark.studentName.text = getString(R.string.my_mark)

        ui.myMark.logs.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        ui.myMark.logs.adapter = MarkLogAdapter().apply {
            submitList(listOf(myMark.mark))
        }
    }

    private fun setupInfo() {
        @Suppress("DEPRECATION")  // У Local нет России
        val formatter = DateTimeFormatter.ofPattern("d MMMM в HH:mm")
            .withLocale(Locale("ru")).withZone(ZoneId.systemDefault())
        val datetime = myMark.mark.created?.toJavaInstant()
        val datetimeFormat = datetime?.let { formatter.format(it) }

        ui.info.text = getString(
            R.string.rating_info,
            myMark.subject,
            myMark.mark.work?.title ?: "",
            datetimeFormat?.let { getString(R.string.rating_info_time, it) }.orEmpty()
        )
    }

    private fun setupAvgGroupMark() {
        ui.avgGroupMark.root.visibility = View.GONE

        ui.avgGroupMark.number.visibility = View.GONE
        ui.avgGroupMark.studentName.text = getString(R.string.avg_group_lesson_mark)

        ui.avgGroupMark.logs.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        ui.avgGroupMark.logs.adapter = MarkLogAdapter()
    }

    private fun setupList() {
        ui.ratingList.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        ui.ratingList.adapter = RatingAdapter(showNumber)
    }

    private fun setupCollectors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    ratingViewModel.marksState.collect { state ->
                        when (state) {
                            CacheDataLoadState.Empty -> {
                                ratingViewModel.loadCacheMarksRatingStats(ratingKey)
                            }

                            CacheDataLoadState.CacheLoading -> {
                                updateLoading(true)
                            }

                            CacheDataLoadState.CacheSuccess -> {
                                ratingViewModel.loadCloudMarksRatingStats(ratingKey)
                            }

                            is CacheDataLoadState.CacheError -> {
                                Utilities.showUiMessage(requireContext(), state.message)
                                ratingViewModel.loadCloudMarksRatingStats(ratingKey)
                            }

                            CacheDataLoadState.CloudLoading -> {
                                updateLoading(true)
                            }

                            CacheDataLoadState.CloudSuccess -> {
                                updateLoading(false)
                            }

                            is CacheDataLoadState.CloudError -> {
                                updateLoading(false)
                                Utilities.showUiMessage(requireContext(), state.message)
                                ratingViewModel.resetError()
                            }

                            CacheDataLoadState.ShownError -> {
                                // Ошибка уже показана
                            }
                        }
                    }
                }
                launch {
                    ratingViewModel.marksData.collect { data ->
                        if (data != null)
                            renderMarksRatingStats(data)
                    }
                }
            }
        }
    }

    private fun renderMarksRatingStats(data: MarksRatingStatsResult) {
        ui.oldAvgMarkLoading.visibility = View.GONE
        ui.newAvgMarkLoading.visibility = View.GONE

        data.oldAvgMark?.let {
            oldAvgMarkAdapter.submitList(listOf(it))
            val holder = MarkLogAdapter.VH(ui.oldAvgMark)
            oldAvgMarkAdapter.onBindViewHolder(holder, 0)
            ui.oldAvgMark.root.visibility = View.VISIBLE
        } ?: { ui.oldAvgMark.root.visibility = View.GONE }
        data.newAvgMark?.let {
            newAvgMarkAdapter.submitList(listOf(it))
            val holder = MarkLogAdapter.VH(ui.newAvgMark)
            newAvgMarkAdapter.onBindViewHolder(holder, 0)
            ui.newAvgMark.root.visibility = View.VISIBLE
        } ?: { ui.newAvgMark.root.visibility = View.GONE }

        if (data.oldAvgMark == null && data.newAvgMark == null) {
            ui.stats.visibility = View.GONE
        }

        val avgAdapter = (ui.avgGroupMark.logs.adapter as? MarkLogAdapter) ?: MarkLogAdapter().also {
            ui.avgGroupMark.logs.adapter = it
        }
        avgAdapter.submitList(listOfNotNull(data.avgGroupMark))
        ui.avgGroupMark.root.visibility = if (data.avgGroupMark != null) View.VISIBLE else View.GONE

        val ratingAdapter = (ui.ratingList.adapter as? RatingAdapter) ?: RatingAdapter(showNumber).also {
            ui.ratingList.adapter = it
        }
        ratingAdapter.submitList(data.othersMarks.toUi())

        if (swipeHelper == null) {
            swipeHelper = ItemTouchHelper(RatingDialogSwipeHelper(
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
                { position ->
                    val list = ratingAdapter.currentList
                    val item = list[position]

                    val newList = if (!item.isHighlighting!!) {
                        mutableListOf(item).apply {
                            addAll(list.filterIndexed { index, _ -> index != position })
                        }
                    } else {
                        list
                    }

                    ratingAdapter.submitList(newList)

                    ratingViewModel.highlightPerson(item.personKey!!, ratingKey, !item.isHighlighting)
                }
            )).apply {
                attachToRecyclerView(ui.ratingList)
            }
        }
    }

    private fun updateLoading(loading: Boolean) {
        if (ratingViewModel.marksData.value?.newAvgMark != null) {
            ui.loading.visibility = if (loading) View.VISIBLE else View.GONE
            ui.oldAvgMarkLoading.visibility = View.GONE
            ui.newAvgMarkLoading.visibility = View.GONE
        } else {
            ui.loading.visibility = View.GONE
            ui.oldAvgMarkLoading.visibility = if (loading) View.VISIBLE else View.GONE
            ui.newAvgMarkLoading.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }
}