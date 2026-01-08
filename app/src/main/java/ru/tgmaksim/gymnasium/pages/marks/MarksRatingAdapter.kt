package ru.tgmaksim.gymnasium.pages.marks

import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager

import ru.tgmaksim.gymnasium.api.MarksOther
import ru.tgmaksim.gymnasium.databinding.MarksRatingItemBinding

class MarksRatingAdapter(
    private val showNumber: Boolean = false
) : ListAdapter<MarksOther, MarksRatingAdapter.ViewHolder>(Diff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val ui = MarksRatingItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(ui)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position, showNumber)
    }

    class ViewHolder(val ui: MarksRatingItemBinding) : RecyclerView.ViewHolder(ui.root) {
        init {
            ui.logs.layoutManager = LinearLayoutManager(
                ui.root.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }

        fun bind(marks: MarksOther, position: Int, showNumber: Boolean) {
            ui.studentName.text = marks.name
            ui.number.text = "${position + 1}."
            ui.number.visibility = if (showNumber) View.VISIBLE else View.GONE

            if (ui.logs.adapter == null)
                ui.logs.adapter = LogsAdapter(ui.root.context).apply { submitList(marks.marks) }
            else
                (ui.logs.adapter as LogsAdapter).submitList(marks.marks)
        }
    }

    class Diff : DiffUtil.ItemCallback<MarksOther>() {
        override fun areItemsTheSame(a: MarksOther, b: MarksOther) = a == b
        override fun areContentsTheSame(a: MarksOther, b: MarksOther) = a == b
    }
}