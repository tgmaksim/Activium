package ru.tgmaksim.activium.ui.pages

import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager

import androidx.appcompat.content.res.AppCompatResources

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.ui.core.UiMarksOther
import ru.tgmaksim.activium.databinding.ItemMarksRatingBinding

class RatingAdapter(
    private val showNumber: Boolean
) : ListAdapter<UiMarksOther, RatingAdapter.ViewHolder>(Diff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val ui = ItemMarksRatingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(ui)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), showNumber)
    }

    class ViewHolder(val ui: ItemMarksRatingBinding) : RecyclerView.ViewHolder(ui.root) {
        init {
            ui.logs.layoutManager = LinearLayoutManager(
                ui.root.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }

        fun bind(item: UiMarksOther, showNumber: Boolean) {
            ui.studentName.text = item.name
            ui.number.text = item.number?.let { ui.root.context.getString(R.string.person_number, it + 1) }
            ui.number.visibility = if (showNumber) View.VISIBLE else View.GONE

            val adapter = (ui.logs.adapter as? MarkLogAdapter) ?: MarkLogAdapter().also {
                ui.logs.adapter = it
            }
            adapter.submitList(item.marks)

            if (item.isOldMark) {
                ui.root.background = AppCompatResources.getDrawable(ui.root.context, R.drawable.old_rating_mark_bg)
            } else {
                ui.root.background = null
            }
        }
    }

    class Diff : DiffUtil.ItemCallback<UiMarksOther>() {
        override fun areItemsTheSame(a: UiMarksOther, b: UiMarksOther) = a == b
        override fun areContentsTheSame(a: UiMarksOther, b: UiMarksOther) = a == b
    }
}