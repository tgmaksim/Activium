package ru.tgmaksim.gymnasium.pages.marks

import android.graphics.Color
import android.view.ViewGroup
import android.widget.TextView
import android.view.LayoutInflater
import com.google.android.flexbox.FlexWrap
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.graphics.drawable.GradientDrawable
import androidx.fragment.app.FragmentActivity
import com.google.android.flexbox.FlexboxLayoutManager

import ru.tgmaksim.gymnasium.R
import ru.tgmaksim.gymnasium.api.MarkLog
import ru.tgmaksim.gymnasium.api.MarksOther
import ru.tgmaksim.gymnasium.api.MarksSubjectPeriod
import ru.tgmaksim.gymnasium.databinding.MarksSubjectPeriodItemBinding

class MarksSubjectPeriodAdapter : ListAdapter<MarksSubjectPeriod, MarksSubjectPeriodAdapter.ViewHolder>(Diff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val ui = MarksSubjectPeriodItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(ui)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val ui: MarksSubjectPeriodItemBinding): RecyclerView.ViewHolder(ui.root) {
        private var marksOthers: List<MarksOther>? = null

        init {
            ui.marks.layoutManager = FlexboxLayoutManager(ui.root.context).apply {
                flexWrap = FlexWrap.WRAP
            }

            ui.subject.setOnClickListener { openRating() }
            ui.averageMark.setOnClickListener { openRating() }
            ui.periodMark.setOnClickListener { openRating() }
        }

        fun openRating() {
            marksOthers?.let {
                val note = "Рейтинг в классе по среднему баллу (${ui.subject.text})"
                if (it.isNotEmpty())
                    RatingDialogFragment(it, showNumber = true, note = note).show(
                        (ui.root.context as FragmentActivity).supportFragmentManager,
                        "rating"
                    )
            }
        }

        fun bind(item: MarksSubjectPeriod) {
            ui.subject.text = item.subject

            // Оценки
            if (ui.marks.adapter == null) {
                ui.marks.adapter = MarksPeriodAdapter().apply {
                    submitList(item.marks)
                }
            } else {
                (ui.marks.adapter as MarksPeriodAdapter).submitList(item.marks)
            }
            marksOthers = item.othersAverageMark

            // Средний балл
            bindLog(ui.averageMark, item.averageMark)

            // Итог за период
            bindLog(ui.periodMark, item.periodMark)
        }

        private fun bindLog(view: TextView, log: MarkLog?) {
            if (log == null) {
                view.text = "—"
                view.background.mutate().alpha = 60
                return
            }
            view.text = log.value
            (view.background.mutate() as GradientDrawable).setColor(getLogColor(log))
        }

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

    class Diff : DiffUtil.ItemCallback<MarksSubjectPeriod>() {
        override fun areItemsTheSame(a: MarksSubjectPeriod, b: MarksSubjectPeriod) = a.subject == b.subject
        override fun areContentsTheSame(a: MarksSubjectPeriod, b: MarksSubjectPeriod) = a == b
    }
}