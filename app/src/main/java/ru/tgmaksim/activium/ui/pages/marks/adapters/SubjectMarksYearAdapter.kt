package ru.tgmaksim.activium.ui.pages.marks.adapters

import android.view.ViewGroup
import android.view.LayoutInflater
import android.content.res.ColorStateList
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager

import ru.tgmaksim.activium.api.MarkLog
import ru.tgmaksim.activium.api.MarksSubjectFinal
import ru.tgmaksim.activium.ui.pages.MarkLogAdapter
import ru.tgmaksim.activium.databinding.ItemMarkPeriodBinding
import ru.tgmaksim.activium.databinding.ItemSubjectMarksYearBinding

class SubjectMarksYearAdapter(
    private var showNullSubjectMarks: Boolean,
    private val onPeriodMarkRating: (String, String) -> Unit,
    private val onMarksRating: (MarkLog, String) -> Unit
) : ListAdapter<MarksSubjectFinal, SubjectMarksYearAdapter.VH>(Diff()) {
    class VH(val ui: ItemSubjectMarksYearBinding) : RecyclerView.ViewHolder(ui.root)

    fun setShowNullSubjectMarks(showNullSubjectMarks: Boolean) {
        this.showNullSubjectMarks = showNullSubjectMarks
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ui = ItemSubjectMarksYearBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(ui)
    }

    override fun submitList(list: List<MarksSubjectFinal?>?) {
        super.submitList(list?.filter { mark ->
            if (!showNullSubjectMarks)
                !mark?.marks.isNullOrEmpty() || mark?.finalMark != null
            else true
        })
    }

    override fun submitList(list: List<MarksSubjectFinal?>?, commitCallback: Runnable?) {
        super.submitList(list?.filter { mark ->
            if (!showNullSubjectMarks)
                !mark?.marks.isNullOrEmpty() || mark?.finalMark != null
            else true
        }, commitCallback)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val subjectMarks = getItem(position)

        holder.ui.marks.layoutManager = GridLayoutManager(
            holder.ui.root.context,
            subjectMarks.marks.size
        )

        holder.ui.subject.text = subjectMarks.subject

        // Оценки
        val marksAdapter = (holder.ui.marks.adapter as? MarksPeriodAdapter) ?: MarksPeriodAdapter { mark ->
            onMarksRating(mark, subjectMarks.subject)
        }.also {
            holder.ui.marks.adapter = it
        }
        marksAdapter.submitList(subjectMarks.marks)

        // Итог за год
        bindOneMark(holder.ui.periodMark, subjectMarks.finalMark)

        holder.ui.periodMark.root.setOnClickListener {
            subjectMarks.finalMark?.ratingKey?.let { onPeriodMarkRating(it, subjectMarks.subject) }
        }
    }

    private fun bindOneMark(view: ItemMarkPeriodBinding, log: MarkLog?) {
        if (log == null) {
            view.log.text = "—"
            view.root.background.mutate().alpha = 60
            view.root.backgroundTintList = null
            return
        }

        view.log.text = log.value
        view.root.background.mutate().alpha = 255
        view.root.backgroundTintList = ColorStateList.valueOf(
            view.root.context.getColor(MarkLogAdapter.getMarkLogBgColor(log.mood))
        )
    }

    class Diff : DiffUtil.ItemCallback<MarksSubjectFinal>() {
        override fun areItemsTheSame(a: MarksSubjectFinal, b: MarksSubjectFinal) = a.subject == b.subject
        override fun areContentsTheSame(a: MarksSubjectFinal, b: MarksSubjectFinal) = a == b
    }
}