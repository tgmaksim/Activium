package ru.tgmaksim.activium.ui.pages.marks.adapters

import android.content.res.ColorStateList
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager

import ru.tgmaksim.activium.api.MarkLog
import ru.tgmaksim.activium.api.MarksSubjectPeriod
import ru.tgmaksim.activium.ui.pages.MarkLogAdapter
import ru.tgmaksim.activium.databinding.ItemMarkPeriodBinding
import ru.tgmaksim.activium.databinding.ItemSubjectMarksPeriodBinding

class SubjectMarksPeriodAdapter(
    private var showNullSubjectMarks: Boolean,
    private val onSubjectRating: (String, String) -> Unit,
    private val onMarksRating: (MarkLog, String) -> Unit
) : ListAdapter<MarksSubjectPeriod, SubjectMarksPeriodAdapter.VH>(Diff()) {
    class VH(val ui: ItemSubjectMarksPeriodBinding) : RecyclerView.ViewHolder(ui.root) {
        init {
            ui.marks.layoutManager = FlexboxLayoutManager(ui.root.context).apply {
                flexWrap = FlexWrap.WRAP
            }
        }
    }

    fun setShowNullSubjectMarks(showNullSubjectMarks: Boolean) {
        this.showNullSubjectMarks = showNullSubjectMarks
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ui = ItemSubjectMarksPeriodBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(ui)
    }

    override fun submitList(list: List<MarksSubjectPeriod?>?) {
        super.submitList(list?.filter { mark ->
            if (!showNullSubjectMarks)
                !mark?.marks.isNullOrEmpty() || mark?.averageMark != null || mark?.periodMark != null
            else true
        })
    }

    override fun submitList(list: List<MarksSubjectPeriod?>?, commitCallback: Runnable?) {
        super.submitList(list?.filter { mark ->
            if (!showNullSubjectMarks)
                !mark?.marks.isNullOrEmpty() || mark?.averageMark != null || mark?.periodMark != null
            else true
        }, commitCallback)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val subjectMarks = getItem(position)

        holder.ui.subject.text = subjectMarks.subject

        // Оценки
        val marksAdapter = (holder.ui.marks.adapter as? MarksPeriodAdapter) ?: MarksPeriodAdapter { mark ->
            onMarksRating(mark, subjectMarks.subject)
        }.also {
            holder.ui.marks.adapter = it
        }
        marksAdapter.submitList(subjectMarks.marks)

        // Средний балл
        bindOneMark(holder.ui.avgMark, subjectMarks.averageMark)

        // Итог за период
        bindOneMark(holder.ui.periodMark, subjectMarks.periodMark)

        holder.ui.subject.setOnClickListener {
            onSubjectRating(subjectMarks.ratingKey, subjectMarks.subject)
        }
        holder.ui.avgMark.root.setOnClickListener {
            onSubjectRating(subjectMarks.ratingKey, subjectMarks.subject)
        }
        holder.ui.periodMark.root.setOnClickListener {
            onSubjectRating(subjectMarks.ratingKey, subjectMarks.subject)
        }
    }

    private fun bindOneMark(view: ItemMarkPeriodBinding, log: MarkLog?) {
        if (log == null) {
            view.log.text = "—"
            view.root.background.mutate().alpha = 60
            return
        }

        view.log.text = log.value
        view.root.backgroundTintList = ColorStateList.valueOf(
            view.root.context.getColor(MarkLogAdapter.getMarkLogBgColor(log.mood))
        )
    }

    class Diff : DiffUtil.ItemCallback<MarksSubjectPeriod>() {
        override fun areItemsTheSame(a: MarksSubjectPeriod, b: MarksSubjectPeriod) = a.ratingKey == b.ratingKey
        override fun areContentsTheSame(a: MarksSubjectPeriod, b: MarksSubjectPeriod) = a == b
    }
}