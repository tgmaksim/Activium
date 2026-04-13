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
import android.view.MotionEvent
import android.view.LayoutInflater
import android.view.ViewConfiguration
import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil

import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.ui.core.LoadState
import ru.tgmaksim.activium.utilities.Utilities
import ru.tgmaksim.activium.ui.pages.MarkLogAdapter
import ru.tgmaksim.activium.api.ScheduleHomeworkDocument
import ru.tgmaksim.activium.ui.pages.schedule.UiScheduleLesson
import ru.tgmaksim.activium.databinding.ItemScheduleLessonBinding
import ru.tgmaksim.activium.databinding.ItemScheduleWorkTypeBinding
import ru.tgmaksim.activium.databinding.ItemScheduleHomeworkFileBinding

class ScheduleLessonAdapter(
    private val onPraiseClick: (String) -> Unit,
    private val onMenuLesson: (String) -> Unit,
    private val onRating: (String) -> Unit
) : ListAdapter<UiScheduleLesson, ScheduleLessonAdapter.VH>(Diff()) {
    class VH(val ui: ItemScheduleLessonBinding) : RecyclerView.ViewHolder(ui.root) {
        init {
            ui.logsRecycler.layoutManager = LinearLayoutManager(
                ui.root.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
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

        holder.ui.number.text = if (!lesson.isExtra) holder.ui.root.context.getString(R.string.lesson_number, position + 1) else ""
        holder.ui.subject.text = lesson.subject
        holder.ui.place.text = lesson.place
        holder.ui.time.text = lesson.hours.string

        holder.ui.root.background = ContextCompat.getDrawable(
            holder.ui.root.context,
            if (lesson.isExtra) R.drawable.lesson_bg_extra else R.drawable.lesson_bg
        )

        if (lesson.isExtra) {
            holder.ui.number.visibility = View.GONE
            holder.ui.homeworkGroup.visibility = View.GONE
            holder.ui.noteGroup.visibility = View.GONE
            holder.ui.filesContainer.visibility = View.GONE
            holder.ui.worksContainer.visibility = View.GONE
            holder.ui.logsRecycler.visibility = View.GONE
            holder.ui.praise.visibility = View.GONE
            @SuppressLint("ClickableViewAccessibility")
            holder.ui.lessonContent.setOnTouchListener(null)
            return
        }

        holder.ui.number.visibility = View.VISIBLE
        holder.ui.homeworkGroup.visibility =
            if (lesson.homework.isNullOrBlank()) View.GONE else View.VISIBLE
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

        val logsAdapter = (holder.ui.logsRecycler.adapter as? MarkLogAdapter) ?: MarkLogAdapter { onRating(lesson.lessonKey!!) }.also {
            holder.ui.logsRecycler.adapter = it
        }
        logsAdapter.submitList(lesson.logs)

        if (lesson.praiseState != null && lesson.logs.isNotEmpty() && lesson.praiseState !is LoadState.Success) {
            holder.ui.praise.visibility = View.VISIBLE
            holder.ui.praiseLoading.visibility =
                if (lesson.praiseState is LoadState.Loading) View.VISIBLE else View.GONE
            holder.ui.praiseError.visibility =
                if (lesson.praiseState.isError()) View.VISIBLE else View.GONE
            holder.ui.praiseButton.visibility =
                if (lesson.praiseState is LoadState.Empty) View.VISIBLE else View.GONE
            holder.ui.praiseButton.setOnClickListener {
                onPraiseClick(lesson.lessonKey!!)  // Проверка при создании объекта
            }
        } else {
            holder.ui.praise.visibility = View.GONE
        }

        if (!lesson.note.isNullOrBlank() || lesson.noteState != null && lesson.noteState !is LoadState.Success) {
            holder.ui.noteGroup.visibility = View.VISIBLE
            holder.ui.noteLoading.visibility = if (lesson.noteState is LoadState.Loading) View.VISIBLE else View.GONE
            holder.ui.noteError.visibility = if (lesson.noteState?.isError() == true) View.VISIBLE else View.GONE
            holder.ui.note.visibility = if (!lesson.note.isNullOrBlank()) View.VISIBLE else View.GONE

            holder.ui.note.text = lesson.note
        } else {
            holder.ui.noteGroup.visibility = View.GONE
        }

        setupLessonMenu(holder, lesson.lessonKey!!)
    }

    private fun setupLessonMenu(holder: VH, lessonKey: String) {
        val touchSlop = ViewConfiguration.get(holder.ui.root.context).scaledTouchSlop
        var downX = 0f
        var downY = 0f

        val longPressRunnable = Runnable {
            holder.ui.longPressBorder.start {
                val position = holder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onMenuLesson(lessonKey)
                }
            }
        }

        holder.ui.longPressBorder.cancel()

        @SuppressLint("ClickableViewAccessibility")
        holder.ui.lessonContent.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y

                    view.postDelayed(longPressRunnable, 100)
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = kotlin.math.abs(event.x - downX)
                    val dy = kotlin.math.abs(event.y - downY)

                    if (dx > touchSlop || dy > touchSlop) {
                        holder.ui.longPressBorder.cancel()
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.removeCallbacks(longPressRunnable)
                    holder.ui.longPressBorder.cancel()
                }
            }
            false
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
        ui.homeworkDocumentName.ellipsize = TextUtils.TruncateAt.MIDDLE
        return ui.root
    }

    class Diff : DiffUtil.ItemCallback<UiScheduleLesson>() {
        override fun areItemsTheSame(a: UiScheduleLesson, b: UiScheduleLesson) =
            a.lessonKey == b.lessonKey && a.lessonKey != null
        override fun areContentsTheSame(a: UiScheduleLesson, b: UiScheduleLesson) = a == b
    }
}