package ru.tgmaksim.gymnasium.pages.schedule

import android.view.View
import android.text.Spanned
import android.graphics.Color
import android.text.TextPaint
import android.view.ViewGroup
import android.text.TextUtils
import android.widget.LinearLayout
import android.view.LayoutInflater
import android.text.SpannableString
import android.text.style.ClickableSpan
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.fragment.app.FragmentActivity
import android.text.method.LinkMovementMethod
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager

import ru.tgmaksim.gymnasium.R
import ru.tgmaksim.gymnasium.api.ScheduleHours
import ru.tgmaksim.gymnasium.api.ScheduleLesson
import ru.tgmaksim.gymnasium.utilities.Utilities
import ru.tgmaksim.gymnasium.utilities.CacheManager
import ru.tgmaksim.gymnasium.pages.marks.LogsAdapter
import ru.tgmaksim.gymnasium.databinding.ScheduleLessonBinding
import ru.tgmaksim.gymnasium.api.ScheduleExtracurricularActivity
import ru.tgmaksim.gymnasium.databinding.ScheduleHomeworkFileBinding

/**
 * Адаптер списка уроков в расписании на странице
 * @author Максим Дрючин (tgmaksim)
 * @see ScheduleAdapter
 * */
class LessonsAdapter : ListAdapter<ScheduleLesson, LessonsAdapter.ViewHolder>(Diff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val ui = ScheduleLessonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(ui)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun submitList(list: List<ScheduleLesson>, ea: List<ScheduleExtracurricularActivity>, hoursEA: ScheduleHours?) {
        if (ea.isEmpty() || hoursEA == null)
            super.submitList(list)
        else  // Добавление одной карточки всех внеурочных занятий в список
            super.submitList(list + ScheduleLesson(
                number = list.size,
                subject = ea.joinToString("\n") { it.subject },
                place = ea.joinToString("; ") { it.place },
                hours = hoursEA,
                logs = emptyList(),
                othersMarks = emptyList(),
                homework = null,
                files = emptyList(),
                isEA = true
            ))
    }

    class ViewHolder(val ui: ScheduleLessonBinding) : RecyclerView.ViewHolder(ui.root) {
        init {
            ui.logs.layoutManager = LinearLayoutManager(
                ui.root.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }

        fun bind(lesson: ScheduleLesson) {
            // Заполняется информация в элементе
            ui.time.text = lesson.hours.stringFormat
            ui.subject.text = lesson.subject
            ui.place.text = lesson.place

            // Выделение цветом внеурочного занятия
            if (lesson.isEA)
                ui.root.background = ContextCompat.getDrawable(ui.root.context, R.drawable.bg_lesson_extra)
            else
                ui.root.background = ContextCompat.getDrawable(ui.root.context, R.drawable.bg_lesson)

            // Показывается или скрывается домашнее задание
            if (lesson.homework?.isEmpty() == false) {
                ui.homework.text = lesson.homework.trimIndent()
                ui.homeworkGroup.visibility = View.VISIBLE
            } else {
                ui.homework.text = R.string.homework_not_found.toString()
                ui.homeworkGroup.visibility = View.GONE
            }

            // Показываются ссылки на файлы
            ui.filesContainer.removeAllViews()
            for (file in lesson.files) {
                ui.filesContainer.addView(createFileSpannable(file.fileName, file.downloadUrl))
            }

            // Инициализация адаптера или обновление данных
            if (ui.logs.adapter == null)
                ui.logs.adapter = LogsAdapter(ui.root.context)
                    .apply { submitList(lesson.logs, lesson.othersMarks) }
            else
                (ui.logs.adapter as LogsAdapter).submitList(lesson.logs, lesson.othersMarks)
        }

        private fun createFileSpannable(fileName: String, downloadUrl: String) : LinearLayout {
            val homeworkFile = ScheduleHomeworkFileBinding.inflate(
                LayoutInflater.from(ui.root.context),
                ui.root,
                false
            )
            val spannable = SpannableString(fileName)

            // Кликабельная ссылка
            spannable.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // Открытие либо WebView, либо браузера
                    if (CacheManager.openWebView) {
                        (ui.root.context as FragmentActivity)
                            .supportFragmentManager
                            .beginTransaction()
                            .replace(
                                R.id.content_container,  // Основной контейнер
                                DocumentView.newInstance(downloadUrl)
                            ).addToBackStack(null).commit()
                    } else {
                        Utilities.openUrl(ui.root.context, downloadUrl)
                    }
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = true
                }
            }, 0, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            homeworkFile.homeworkDocumentName.apply {
                text = spannable
                movementMethod = LinkMovementMethod.getInstance()
                highlightColor = Color.TRANSPARENT
                setSingleLine()
                ellipsize = TextUtils.TruncateAt.MIDDLE
            }

            return homeworkFile.root
        }
    }

    class Diff : DiffUtil.ItemCallback<ScheduleLesson>() {
        override fun areItemsTheSame(a: ScheduleLesson, b: ScheduleLesson) = a == b
        override fun areContentsTheSame(a: ScheduleLesson, b: ScheduleLesson) = a == b
    }
}