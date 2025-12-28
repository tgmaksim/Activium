package ru.tgmaksim.gymnasium.pages.marks

import android.graphics.Color
import android.view.ViewGroup
import android.content.Context
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.graphics.drawable.GradientDrawable

import ru.tgmaksim.gymnasium.R
import ru.tgmaksim.gymnasium.api.MarkLog
import ru.tgmaksim.gymnasium.api.MarksOther
import ru.tgmaksim.gymnasium.databinding.MarkLogBinding
import ru.tgmaksim.gymnasium.pages.schedule.LessonsAdapter

/**
 * Адаптер списка оценок и отметок о посещаемости уроков
 * @author Максим Дрючин (tgmaksim)
 * @see LessonsAdapter
 * */
class LogsAdapter(private val context: Context): ListAdapter<MarkLog, LogsAdapter.ViewHolder>(Diff()) {
    private var marksOthers: List<MarksOther>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val ui = MarkLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        // Показ рейтинга при нажатии на список с оценками и отметками о посещаемости
        ui.root.setOnClickListener {
            marksOthers?.let {
                if (it.isNotEmpty())
                    RatingDialogFragment(it).show(
                        (ui.root.context as FragmentActivity).supportFragmentManager,
                        "rating"
                    )
            }
        }

        return ViewHolder(ui)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    fun submitList(list: List<MarkLog>, newMarksOthers: List<MarksOther>?) {
        marksOthers = newMarksOthers

        // При отсутствии своих оценок и наличии других, показывается эмодзи статистики
        val statistics = MarkLog(value = ContextCompat.getString(context, R.string.marks_rating_emoji), mood = "null")
        super.submitList(if (list.isEmpty() && marksOthers?.isEmpty() == false) listOf(statistics) else list)
    }

    override fun submitList(list: List<MarkLog?>?) {
        super.submitList(list)
        marksOthers = null
    }

    class ViewHolder(val ui: MarkLogBinding) : RecyclerView.ViewHolder(ui.root) {
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