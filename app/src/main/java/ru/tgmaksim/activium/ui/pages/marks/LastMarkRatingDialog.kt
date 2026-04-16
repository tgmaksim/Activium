package ru.tgmaksim.activium.ui.pages.marks

import android.os.Bundle
import android.view.View
import android.graphics.Color
import android.view.ViewGroup
import kotlinx.coroutines.launch
import android.view.LayoutInflater
import androidx.lifecycle.Lifecycle
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager

import java.util.Locale
import java.time.ZoneId
import kotlin.time.toJavaInstant
import java.time.format.DateTimeFormatter

import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.MarkLast
import ru.tgmaksim.activium.ui.core.toUi
import ru.tgmaksim.activium.ui.core.LoadState
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.ui.main.MainActivity
import ru.tgmaksim.activium.ui.pages.RatingAdapter
import ru.tgmaksim.activium.ui.pages.MarkLogAdapter
import ru.tgmaksim.activium.api.MarksRatingStatsResult
import ru.tgmaksim.activium.ui.core.CacheDataLoadState
import ru.tgmaksim.activium.databinding.DialogPraiseBinding
import ru.tgmaksim.activium.databinding.RatingSheetBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LastMarkRatingDialog(
    private val ratingKey: String,
    private val myMark: MarkLast,
    private val showNumber: Boolean
) : BottomSheetDialogFragment() {
    private lateinit var ui: RatingSheetBinding
    private lateinit var oldAvgMarkAdapter: MarkLogAdapter
    private lateinit var newAvgMarkAdapter: MarkLogAdapter

    private val ratingViewModel: LastMarkRatingViewModel by viewModels()

    private var praiseAnimation: FloatArray? = null

    companion object {
        const val TAG = "LastMarkRatingDialog"
        private const val PRAISE_TEXT_LIMIT = 64
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
        setupPraise()
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

    private fun setupPraise() {
        ui.praiseButton.setOnClickListener {
            val viewLocation = IntArray(2)
            ui.praiseButton.getLocationInWindow(viewLocation)
            val location = FloatArray(2)
            location[0] = viewLocation[0] + ui.praiseButton.width / 2f
            location[1] = viewLocation[1] + ui.praiseButton.height / 2f
            onPraise(location)
        }
        ui.praiseError.setOnClickListener {
            val viewLocation = IntArray(2)
            ui.praiseError.getLocationInWindow(viewLocation)
            val location = FloatArray(2)
            location[0] = viewLocation[0] + ui.praiseError.width / 2f
            location[1] = viewLocation[1] + ui.praiseError.height / 2f
            onPraise(location)
        }
    }

    private fun onPraise(location: FloatArray) {
        val view = DialogPraiseBinding.inflate(layoutInflater, ui.root, false)

        view.textCounter.text = getString(R.string.praise_text_counter, view.text.text?.length ?: 0, PRAISE_TEXT_LIMIT)

        view.text.addTextChangedListener {
            view.textCounter.text = getString(R.string.praise_text_counter, view.text.text?.length ?: 0, PRAISE_TEXT_LIMIT)
            if ((it?.length ?: 0) > PRAISE_TEXT_LIMIT)
                view.textCounter.setTextColor(Color.RED)
            else if ((it?.length ?: 0) == PRAISE_TEXT_LIMIT)
                view.textCounter.setTextColor(requireContext().getColor(R.color.text_secondary))
        }

        val dialog = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.AppDialogTheme
        ).setView(view.root).create()

        view.buttonSendPraise.setOnClickListener {
            val text = view.text.text?.toString()?.trim()?.ifEmpty { null }

            dialog.dismiss()

            praiseAnimation = location
            ratingViewModel.sendPraise(ratingKey, text)
        }

        dialog.show()
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
        ui.avgGroupMark.studentName.text = getString(R.string.avg_group_work_mark)

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
                                ratingViewModel.resetError(LastMarkRatingViewModel.StateType.Marks)
                            }

                            CacheDataLoadState.ShownError -> {
                                // Ошибка уже показана
                            }
                        }
                    }
                }
                launch {
                    ratingViewModel.praiseState.collect { state ->
                        renderPraise(state)

                        if (state is LoadState.Error) {
                            Utilities.showUiMessage(requireContext(), state.message)
                            ratingViewModel.resetError(LastMarkRatingViewModel.StateType.Praise)
                        } else if (state is LoadState.Success) {
                            ratingViewModel.resetPraise()

                            val activity = requireActivity() as MainActivity
                            dismiss()
                            activity.startKonfettiAnimation(location = praiseAnimation)
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

        ui.praise.visibility = if (data.hasAbilityPraise) View.VISIBLE else View.GONE

        val avgAdapter = (ui.avgGroupMark.logs.adapter as? MarkLogAdapter) ?: MarkLogAdapter().also {
            ui.avgGroupMark.logs.adapter = it
        }
        avgAdapter.submitList(listOfNotNull(data.avgGroupMark))
        ui.avgGroupMark.root.visibility = if (data.avgGroupMark != null) View.VISIBLE else View.GONE

        val ratingAdapter = (ui.ratingList.adapter as? RatingAdapter) ?: RatingAdapter(showNumber).also {
            ui.ratingList.adapter = it
        }
        ratingAdapter.submitList(data.othersMarks.toUi())

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

    private fun renderPraise(state: LoadState<Unit>) {
        ui.praiseButton.visibility = if (state is LoadState.Empty) View.VISIBLE else View.GONE
        ui.praiseError.visibility = if (state.isError()) View.VISIBLE else View.GONE
        ui.praiseLoading.visibility = if (state is LoadState.Loading) View.VISIBLE else View.GONE
    }
}