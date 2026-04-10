package ru.tgmaksim.activium.ui.pages.marks.adapters

import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import android.graphics.drawable.GradientDrawable

import ru.tgmaksim.activium.api.MarkLast
import ru.tgmaksim.activium.ui.pages.MarkLogAdapter
import ru.tgmaksim.activium.databinding.ItemLastMarkBinding

class LastMarksAdapter(
    private val onLastMarkRating: (String) -> Unit
): ListAdapter<MarkLast, LastMarksAdapter.VH>(Diff()) {
    class VH(val ui: ItemLastMarkBinding) : RecyclerView.ViewHolder(ui.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ui = ItemLastMarkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(ui)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val mark = getItem(position)

        holder.ui.value.text = mark.mark.value

        (holder.ui.markBox.background.mutate() as GradientDrawable)
            .setColor(holder.ui.root.context.getColor(MarkLogAdapter.getMarkLogBgColor(mark.mark.mood)))

        holder.ui.subject.text = mark.subject
        holder.ui.detail.text = if (mark.mark.work == null) {  // За период или год
            "За ${mark.humanLessonDate}"
        } else if (mark.humanLessonDate == null) {
            mark.mark.work.abbr
        } else {
            "${mark.mark.work.abbr} за ${mark.humanLessonDate}"
        }

        holder.ui.root.setOnClickListener {
            onLastMarkRating(mark.ratingKey)
        }
    }

    class Diff : DiffUtil.ItemCallback<MarkLast>() {
        override fun areItemsTheSame(a: MarkLast, b: MarkLast) = a.ratingKey == b.ratingKey
        override fun areContentsTheSame(a: MarkLast, b: MarkLast) = a == b
    }
}