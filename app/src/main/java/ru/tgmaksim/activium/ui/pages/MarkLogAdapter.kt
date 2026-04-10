package ru.tgmaksim.activium.ui.pages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.MarkLog
import ru.tgmaksim.activium.databinding.ItemMarkLogBinding

class MarkLogAdapter(
    private val onRating: (() -> Unit)? = null
) : ListAdapter<MarkLog, MarkLogAdapter.VH>(Diff()) {
    class VH(val ui: ItemMarkLogBinding) : RecyclerView.ViewHolder(ui.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ui = ItemMarkLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(ui)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.ui.log.text = item.value

        val bgColor = getMarkLogBgColor(item.mood)

        val drawable = DrawableCompat.wrap(
            ContextCompat.getDrawable(
                holder.ui.root.context,
                R.drawable.mark_log_bg
            )!!.mutate()
        )
        DrawableCompat.setTint(drawable, ContextCompat.getColor(holder.ui.root.context, bgColor))
        holder.ui.root.background = drawable

        holder.ui.root.setOnClickListener {
            onRating?.invoke()
        }
    }

    companion object {
        fun getMarkLogBgColor(mood: String) =
            when (mood) {
                "good" -> R.color.mark_log_good
                "average" -> R.color.mark_log_average
                "bad" -> R.color.mark_log_bad
                "more" -> R.color.mark_log_more
                else -> R.color.mark_log_more
            }
    }

    class Diff : DiffUtil.ItemCallback<MarkLog>() {
        override fun areItemsTheSame(a: MarkLog, b: MarkLog) = a == b
        override fun areContentsTheSame(a: MarkLog, b: MarkLog) = a == b
    }
}