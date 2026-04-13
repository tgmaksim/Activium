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

import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.ui.core.toUi
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.ui.pages.RatingAdapter
import ru.tgmaksim.activium.ui.core.CacheDataLoadState
import ru.tgmaksim.activium.api.MarksSubjectRatingResult
import ru.tgmaksim.activium.databinding.RatingSheetBinding

class SubjectRatingDialog(
    private val ratingKey: String,
    private val subject: String?,
    private val showNumber: Boolean,
    private val classRating: Boolean = false
) : BottomSheetDialogFragment() {
    private lateinit var ui: RatingSheetBinding

    private val ratingViewModel: SubjectRatingViewModel by viewModels()

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

        setupOtherElements()
        setupList()

        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCollectors()
    }

    private fun setupOtherElements() {
        ui.stats.visibility = View.GONE
        ui.myMark.root.visibility = View.GONE
        ui.avgGroupMark.root.visibility = View.GONE
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
                        Utilities.log(state.toString(), tag = "debug")
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

    private fun renderMarksRatingStats(data: MarksSubjectRatingResult) {
        if (subject != null || classRating) {
            ui.info.text = if (classRating) getString(R.string.class_rating_info) else getString(R.string.subject_rating_info, subject)
            ui.info.visibility = View.VISIBLE
        } else {
            ui.info.visibility = View.GONE
        }

        val ratingAdapter = (ui.ratingList.adapter as? RatingAdapter) ?: RatingAdapter(showNumber).also {
            ui.ratingList.adapter = it
        }

        val list = data.rating.toUi().toMutableList()
        data.oldMark?.let { oldMark ->
            val myMark = list.find { it.number == data.oldMark.number }
            if (myMark != null)
                list.add(list.indexOf(myMark) + 1, oldMark.toUi(true))
        }
        ratingAdapter.submitList(list.toList())

        // Временно закрыто
        /*if (swipeHelper == null) {
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
        }*/
    }

    private fun updateLoading(loading: Boolean) {
        ui.loading.visibility = if (loading) View.VISIBLE else View.GONE
    }
}