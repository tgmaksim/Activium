package ru.tgmaksim.gymnasium.pages.schedule

import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager

import ru.tgmaksim.gymnasium.api.ScheduleDay
import ru.tgmaksim.gymnasium.databinding.ScheduleDayBinding

/**
 * Адаптер расписания на странице
 * @author Максим Дрючин (tgmaksim)
 * @see SchedulePage
 * */
class ScheduleAdapter : ListAdapter<ScheduleDay?, ScheduleAdapter.ViewHolder>(Diff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val ui = ScheduleDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(ui)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(val ui: ScheduleDayBinding) : RecyclerView.ViewHolder(ui.root) {
        init {
            ui.lessons.layoutManager = LinearLayoutManager(
                ui.root.context,
                LinearLayoutManager.VERTICAL,
                false
            )
        }

        /**
         * Показ определенного дня расписания
         * @param day День расписания
         * @author Максим Дрючин (tgmaksim)
         * */
        fun bind(day: ScheduleDay?) {
            if (day == null) {
                // День отсутствует в расписании
                ui.lessons.visibility = View.GONE
                ui.weekendPhoto.visibility = View.GONE
            } else if (day.lessons.isEmpty()) {
                // Показывается фото выходного дня, так как уроков нет
                ui.lessons.visibility = View.GONE
                ui.weekendPhoto.visibility = View.VISIBLE
            } else {
                // Показывается обновленное расписание
                ui.weekendPhoto.visibility = View.GONE
                ui.lessons.visibility = View.VISIBLE

                // Инициализация адаптера или обновление данных
                if (ui.lessons.adapter == null)
                    ui.lessons.adapter = LessonsAdapter().apply {
                        submitList(day.lessons.sortedBy { it.number }, day.ea, day.hoursEA)
                    }
                else
                    (ui.lessons.adapter as LessonsAdapter).submitList(
                        day.lessons.sortedBy { it.number }, day.ea, day.hoursEA)
            }
        }
    }

    class Diff : DiffUtil.ItemCallback<ScheduleDay>() {
        override fun areItemsTheSame(a: ScheduleDay, b: ScheduleDay) = a.date == b.date
        override fun areContentsTheSame(a: ScheduleDay, b: ScheduleDay) = a == b
    }
}