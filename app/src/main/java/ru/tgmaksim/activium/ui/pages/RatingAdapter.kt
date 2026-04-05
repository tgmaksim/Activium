package ru.tgmaksim.activium.ui.pages

import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.MarksOther
import ru.tgmaksim.activium.databinding.ItemMarksRatingBinding
import ru.tgmaksim.activium.ui.pages.schedule.adapters.MarkLogAdapter

class RatingAdapter(
    private val showNumber: Boolean
) : ListAdapter<MarksOther, RatingAdapter.ViewHolder>(Diff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val ui = ItemMarksRatingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(ui)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position, showNumber)
    }

    class ViewHolder(val ui: ItemMarksRatingBinding) : RecyclerView.ViewHolder(ui.root) {
        init {
            ui.logs.layoutManager = LinearLayoutManager(
                ui.root.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }

        fun bind(item: MarksOther, position: Int, showNumber: Boolean) {
            ui.studentName.text = item.name
            ui.number.text = ui.root.context.getString(R.string.person_number, position + 1)
            ui.number.visibility = if (showNumber) View.VISIBLE else View.GONE

            val adapter = (ui.logs.adapter as? MarkLogAdapter) ?: MarkLogAdapter().also {
                ui.logs.adapter = it
            }
            adapter.submitList(item.marks)
        }
    }

    class Diff : DiffUtil.ItemCallback<MarksOther>() {
        override fun areItemsTheSame(a: MarksOther, b: MarksOther) = a == b
        override fun areContentsTheSame(a: MarksOther, b: MarksOther) = a == b
    }
}