package ru.tgmaksim.gymnasium.pages.marks

import android.graphics.Color
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.graphics.drawable.GradientDrawable

import ru.tgmaksim.gymnasium.R
import ru.tgmaksim.gymnasium.api.MarkLog
import ru.tgmaksim.gymnasium.databinding.MarkPeriodBinding

/**
 * Адаптер списка оценок по предмету за текущий период
 * @author Максим Дрючин (tgmaksim)
 * @see MarksSubjectPeriodAdapter
 * */
class MarksPeriodAdapter : ListAdapter<MarkLog, MarksPeriodAdapter.ViewHolder>(Diff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val ui = MarkPeriodBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(ui)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(val ui: MarkPeriodBinding) : RecyclerView.ViewHolder(ui.root) {
        fun bind(log: MarkLog) {
            ui.scheduleLog.text = log.value
            (ui.scheduleLog.background.mutate() as GradientDrawable).setColor(getLogColor(log))
        }

        /**
         * Получение цвета оценки или отметки о посещаемости урока
         * @param log оценка или отметка о посещаемости урока
         * @return цвет фона
         * @author Максим Дрючин (tgmaksim)
         * */
        private fun getLogColor(log: MarkLog): Int {
            return ContextCompat.getColor(
                ui.root.context,
                when (log.mood) {
                    "good" -> R.color.lesson_log_good
                    "average" -> R.color.lesson_log_average
                    "bad" -> R.color.lesson_log_bad
                    "more" -> R.color.lesson_log_more
                    "null" -> return Color.TRANSPARENT
                    else -> R.color.lesson_log_more
                }
            )
        }
    }

    class Diff : DiffUtil.ItemCallback<MarkLog>() {
        override fun areItemsTheSame(a: MarkLog, b: MarkLog) = a == b
        override fun areContentsTheSame(a: MarkLog, b: MarkLog) = a == b
    }
}