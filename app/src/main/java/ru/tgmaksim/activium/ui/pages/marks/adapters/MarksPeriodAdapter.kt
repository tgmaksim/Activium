package ru.tgmaksim.activium.ui.pages.marks.adapters

import android.view.ViewGroup
import android.view.LayoutInflater
import android.content.res.ColorStateList
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import ru.tgmaksim.activium.api.MarkLog
import ru.tgmaksim.activium.ui.pages.MarkLogAdapter
import ru.tgmaksim.activium.databinding.ItemMarkPeriodBinding

class MarksPeriodAdapter(
    private val onMarksRating: (MarkLog) -> Unit
) : ListAdapter<MarkLog, MarksPeriodAdapter.VH>(Diff()) {
    class VH(val ui: ItemMarkPeriodBinding) : RecyclerView.ViewHolder(ui.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ui = ItemMarkPeriodBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(ui)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val mark = getItem(position)

        if (mark == null) {
            holder.ui.log.text = "—"
            holder.ui.root.background.mutate().alpha = 60
            return
        }

        holder.ui.log.text = mark.value

        holder.ui.root.backgroundTintList = ColorStateList.valueOf(
            holder.ui.root.context.getColor(MarkLogAdapter.getMarkLogBgColor(mark.mood))
        )

        holder.ui.log.setOnClickListener {
            mark.ratingKey?.let { onMarksRating(mark) }
        }
    }

    class Diff : DiffUtil.ItemCallback<MarkLog>() {
        override fun areItemsTheSame(a: MarkLog, b: MarkLog) = a == b
        override fun areContentsTheSame(a: MarkLog, b: MarkLog) = a == b
    }
}