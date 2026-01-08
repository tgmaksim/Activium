package ru.tgmaksim.gymnasium.pages.marks

import java.util.Locale
import android.view.ViewGroup
import android.view.LayoutInflater
import java.time.format.DateTimeFormatter
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.graphics.drawable.GradientDrawable

import ru.tgmaksim.gymnasium.R
import ru.tgmaksim.gymnasium.api.MarkLog
import ru.tgmaksim.gymnasium.api.MarkLast
import ru.tgmaksim.gymnasium.api.MarksOther
import ru.tgmaksim.gymnasium.databinding.LastMarkBinding

class LastMarksAdapter: ListAdapter<MarkLast, LastMarksAdapter.ViewHolder>(Diff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val ui = LastMarkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(ui)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun submitList(list: List<MarkLast?>?) {
        super.submitList(list?.sortedByDescending { it?.sentDatetime })
    }

    class ViewHolder(val ui: LastMarkBinding) : RecyclerView.ViewHolder(ui.root) {
        private var marksOthers: List<MarksOther>? = null
        private var note: String? = null

        init {
            ui.root.setOnClickListener {
                marksOthers?.let {
                    if (it.isNotEmpty())
                        RatingDialogFragment(it, note = note).show(
                            (ui.root.context as FragmentActivity).supportFragmentManager,
                            "rating"
                        )
                }
            }
        }

        fun bind(mark: MarkLast) {
            ui.value.text = mark.mark.value
            (ui.markBox.background.mutate() as GradientDrawable).setColor(getLogColor(mark.mark))
            ui.subject.text = mark.subject
            ui.note.text = if (mark.work == null) {
                "За ${mark.lessonDateFormat}"
            } else if (mark.lessonDateFormat == null) {
                mark.work.abbr
            } else {
                "${mark.work.abbr} за ${mark.lessonDateFormat}"
            }

            marksOthers = mark.othersMarks

            @Suppress("DEPRECATION")  // У Local нет России
            val format = DateTimeFormatter.ofPattern("d MMMM в HH:mm", Locale("ru"))
            val sentDatetimeFormat = mark.sentDatetime.format(format)
            note = "Оценки класса по предмету ${mark.subject} (${ui.note.text}). Вам выставили $sentDatetimeFormat"
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
                    else -> R.color.lesson_log_more
                }
            )
        }
    }

    class Diff : DiffUtil.ItemCallback<MarkLast>() {
        override fun areItemsTheSame(a: MarkLast, b: MarkLast) = a == b
        override fun areContentsTheSame(a: MarkLast, b: MarkLast) = a == b
    }
}