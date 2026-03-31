package ru.tgmaksim.activium.ui.pages.schedule.adapters

import android.graphics.Color
import android.content.Context

import android.text.Spanned
import android.text.TextUtils
import android.text.TextPaint
import android.text.SpannableString
import android.text.style.ClickableSpan
import android.text.method.LinkMovementMethod

import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil

import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.ScheduleLesson
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.api.ScheduleHomeworkDocument
import ru.tgmaksim.activium.databinding.ItemScheduleLessonBinding
import ru.tgmaksim.activium.databinding.ItemScheduleWorkTypeBinding
import ru.tgmaksim.activium.databinding.ItemScheduleHomeworkFileBinding

class ScheduleLessonAdapter(
    private val onPraiseClick: (String) -> Unit
) : ListAdapter<ScheduleLesson, ScheduleLessonAdapter.VH>(Diff()) {
    private var hasAbilityPraise = false

    fun setHasAbilityPraise(value: Boolean) {
        hasAbilityPraise = value
    }

    class VH(val ui: ItemScheduleLessonBinding) : RecyclerView.ViewHolder(ui.root) {
        init {
            ui.logsRecycler.layoutManager = LinearLayoutManager(
                ui.root.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            ui.logsRecycler.itemAnimator = null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ui = ItemScheduleLessonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(ui)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val lesson = getItem(position)
        val isExtra = lesson.lessonKey.startsWith("ea:")

        holder.ui.number.text = holder.ui.root.context.getString(R.string.lesson_number, position + 1)
        holder.ui.subject.text = lesson.subject
        holder.ui.place.text = lesson.place
        holder.ui.time.text = lesson.hours.string

        holder.ui.root.background = ContextCompat.getDrawable(
            holder.ui.root.context,
            if (isExtra) R.drawable.lesson_bg_extra else R.drawable.lesson_bg
        )

        if (isExtra) {
            holder.ui.homeworkGroup.visibility = View.GONE
            holder.ui.noteGroup.visibility = View.GONE
            holder.ui.filesContainer.visibility = View.GONE
            holder.ui.worksContainer.visibility = View.GONE
            holder.ui.logsRecycler.visibility = View.GONE
            holder.ui.praiseButton.visibility = View.GONE
            return
        }

        holder.ui.homeworkGroup.visibility =
            if (lesson.homework.isNullOrBlank()) View.GONE else View.VISIBLE
        holder.ui.noteGroup.visibility =
            if (lesson.note.isNullOrBlank()) View.GONE else View.VISIBLE
        holder.ui.filesContainer.visibility =
            if (lesson.files.isEmpty()) View.GONE else View.VISIBLE
        holder.ui.worksContainer.visibility =
            if (lesson.works.isEmpty()) View.GONE else View.VISIBLE
        holder.ui.logsRecycler.visibility =
            if (lesson.logs.isEmpty()) View.GONE else View.VISIBLE

        holder.ui.homework.text = lesson.homework.orEmpty()
        holder.ui.note.text = lesson.note.orEmpty()

        holder.ui.homework.movementMethod = LinkMovementMethod.getInstance()
        holder.ui.homework.highlightColor = Color.TRANSPARENT

        holder.ui.filesContainer.removeAllViews()
        lesson.files.forEach { file ->
            holder.ui.filesContainer.addView(createFileView(holder.ui.root.context, file))
        }

        holder.ui.worksContainer.removeAllViews()
        lesson.works.forEach { workType ->
            val item = ItemScheduleWorkTypeBinding.inflate(
                LayoutInflater.from(holder.ui.root.context),
                holder.ui.worksContainer,
                false
            )
            item.workTypeText.text = workType.title
            holder.ui.worksContainer.addView(item.root)
        }

        val logsAdapter = (holder.ui.logsRecycler.adapter as? MarkLogAdapter) ?: MarkLogAdapter().also {
            holder.ui.logsRecycler.adapter = it
        }
        logsAdapter.submitList(lesson.logs)

        holder.ui.praiseButton.visibility =
            if (hasAbilityPraise && lesson.logs.isNotEmpty()) View.VISIBLE else View.GONE
        holder.ui.praiseButton.setOnClickListener {
            onPraiseClick(lesson.lessonKey)
        }
    }

    private fun createFileView(context: Context, file: ScheduleHomeworkDocument): View {
        val ui = ItemScheduleHomeworkFileBinding.inflate(LayoutInflater.from(context))
        val spannable = SpannableString(file.fileName)

        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                Utilities.openUrl(context, file.downloadUrl)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
            }
        }, 0, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        ui.homeworkDocumentName.text = spannable
        ui.homeworkDocumentName.movementMethod = LinkMovementMethod.getInstance()
        ui.homeworkDocumentName.highlightColor = Color.TRANSPARENT
        ui.homeworkDocumentName.ellipsize = TextUtils.TruncateAt.MIDDLE
        ui.homeworkDocumentName.setSingleLine()
        return ui.root
    }

    class Diff : DiffUtil.ItemCallback<ScheduleLesson>() {
        override fun areItemsTheSame(a: ScheduleLesson, b: ScheduleLesson) = a.lessonKey == b.lessonKey
        override fun areContentsTheSame(a: ScheduleLesson, b: ScheduleLesson) = a == b
    }
}