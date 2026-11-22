package ru.tgmaksim.activium.ui.pages.schedule.adapters

import kotlinx.datetime.LocalDate

import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import ru.tgmaksim.activium.databinding.ItemScheduleCalendarDayBinding

class ScheduleCalendarAdapter(
    private val onClick: (LocalDate) -> Unit
) : ListAdapter<ScheduleCalendarDayUi, ScheduleCalendarAdapter.VH>(Diff()) {
    class VH(val ui: ItemScheduleCalendarDayBinding) : RecyclerView.ViewHolder(ui.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ui = ItemScheduleCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(ui)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)

        holder.ui.weekday.text = item.weekday
        holder.ui.dayNumber.text = item.dayNumber

        // Окрашивание в разные цвета
        holder.ui.root.isSelected = item.isSelected

        holder.ui.birthdayBalloon.visibility = if (item.isBirthdayDay) View.VISIBLE else View.GONE
        holder.ui.birthdayStarOne.visibility = if (item.isBirthdayDay) View.VISIBLE else View.GONE
        holder.ui.birthdayStarTwo.visibility = if (item.isBirthdayDay) View.VISIBLE else View.GONE

        holder.ui.root.setOnClickListener { onClick(item.date) }
    }

    class Diff : DiffUtil.ItemCallback<ScheduleCalendarDayUi>() {
        override fun areItemsTheSame(a: ScheduleCalendarDayUi, b: ScheduleCalendarDayUi) = a.date == b.date
        override fun areContentsTheSame(a: ScheduleCalendarDayUi, b: ScheduleCalendarDayUi) = a == b
    }
}