package ru.tgmaksim.activium.ui.pages.schedule

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.animation.AccelerateDecelerateInterpolator

import android.os.Bundle
import android.view.View
import android.graphics.Color
import android.view.ViewGroup
import android.view.WindowManager
import android.view.LayoutInflater

import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.LinearLayoutManager

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.ui.core.toUi
import ru.tgmaksim.activium.ui.core.LoadState
import ru.tgmaksim.activium.ui.pages.MarkLogAdapter
import ru.tgmaksim.activium.databinding.DialogLessonMenuBinding
import ru.tgmaksim.activium.databinding.ItemScheduleLessonBinding
import ru.tgmaksim.activium.databinding.ItemScheduleWorkTypeBinding

class LessonMenuDialog(
    private val lesson: UiScheduleLesson,
    private val onCreateNote: () -> Unit,
    private val onDeleteNote: () -> Unit,
    private val onPraise: () -> Unit,
    private val onOpenDnevnikru: () -> Unit,
    private val onRating: () -> Unit
) : DialogFragment() {
    private lateinit var ui: DialogLessonMenuBinding

    companion object {
        const val TAG = "LessonMenuDialog"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui = DialogLessonMenuBinding.inflate(inflater, container, false)

        return ui.root
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }

        showEnterAnimation()
    }

    private fun showEnterAnimation() {
        ui.content.alpha = 0f
        ui.content.translationY = 60f
        ui.background.alpha = 0f

        ui.background.animate()
            .alpha(1f)
            .setDuration(180L)
            .start()

        ui.content.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ui.background.setOnClickListener {
            dismissWithAnimation()
        }

        ui.content.setOnClickListener(null)

        bindLesson()
        setupButtons()
    }

    private fun dismissWithAnimation(afterDismiss: (() -> Unit)? = null) {
        ui.background.animate()
            .alpha(0f)
            .setDuration(160L)
            .start()

        ui.content.animate()
            .alpha(0f)
            .translationY(60f)
            .setDuration(160L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (isAdded) dismissAllowingStateLoss()
                    afterDismiss?.invoke()
                }
            })
            .start()
    }

    private fun bindLesson() {
        ui.lesson.logsRecycler.layoutManager = LinearLayoutManager(
            ui.root.context,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        bindLessonPreview(layoutInflater, ui.lesson, lesson)
    }

    private fun bindLessonPreview(layoutInflater: LayoutInflater, binding: ItemScheduleLessonBinding, lesson: UiScheduleLesson) {
        binding.praise.visibility = View.GONE
        binding.filesContainer.visibility = View.GONE
        binding.root.isClickable = false
        binding.root.isFocusable = false
        binding.longPressBorder.visibility = View.GONE

        binding.number.text = binding.root.context.getString(R.string.lesson_number, lesson.number + 1)
        binding.subject.text = lesson.subject
        binding.place.text = lesson.place
        binding.time.text = lesson.hours.string

        binding.root.background = ContextCompat.getDrawable(
            binding.root.context,
            if (lesson.isExtra) R.drawable.lesson_bg_extra else R.drawable.lesson_bg
        )

        if (lesson.isExtra) {
            binding.number.visibility = View.GONE
            binding.homeworkGroup.visibility = View.GONE
            binding.noteGroup.visibility = View.GONE
            binding.filesContainer.visibility = View.GONE
            binding.logsRecycler.visibility = View.GONE
            return
        }

        binding.number.visibility = View.VISIBLE
        binding.homeworkGroup.visibility =
            if (lesson.homework.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.noteGroup.visibility =
            if (lesson.note.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.worksContainer.visibility =
            if (lesson.works.isEmpty()) View.GONE else View.VISIBLE
        binding.logsRecycler.visibility =
            if (lesson.logs.isEmpty()) View.GONE else View.VISIBLE

        binding.homework.text = lesson.homework.orEmpty()
        binding.note.text = lesson.note.orEmpty()

        binding.worksContainer.removeAllViews()
        lesson.works.forEach { workType ->
            val item = ItemScheduleWorkTypeBinding.inflate(
                layoutInflater,
                binding.worksContainer,
                false
            )
            item.workTypeText.text = workType.title
            binding.worksContainer.addView(item.root)
        }

        val logsAdapter = (binding.logsRecycler.adapter as? MarkLogAdapter) ?: MarkLogAdapter().also {
            binding.logsRecycler.adapter = it
        }
        logsAdapter.submitList(lesson.logs.toUi())
    }

    private fun setupButtons() {
        ui.buttonNote.visibility = if (lesson.note.isNullOrBlank()) View.VISIBLE else View.GONE
        ui.buttonEditNote.visibility = if (!lesson.note.isNullOrBlank()) View.VISIBLE else View.GONE
        ui.buttonDeleteNote.visibility = if (!lesson.note.isNullOrBlank()) View.VISIBLE else View.GONE
        ui.buttonPraise.visibility =
            if (lesson.praiseState is LoadState.Empty && lesson.logs.isNotEmpty()) View.VISIBLE else View.GONE
        ui.buttonRating.visibility =
            if (lesson.logs.isNotEmpty() || lesson.othersMarks.isNotEmpty()) View.VISIBLE else View.GONE

        ui.buttonNote.setOnClickListener {
            dismissWithAnimation(onCreateNote)
        }
        ui.buttonEditNote.setOnClickListener {
            dismissWithAnimation(onCreateNote)
        }
        ui.buttonDeleteNote.setOnClickListener {
            dismissWithAnimation(onDeleteNote)
        }
        ui.buttonPraise.setOnClickListener {
            dismissWithAnimation(onPraise)
        }
        ui.buttonRating.setOnClickListener {
            dismissWithAnimation(onRating)
        }
        ui.buttonDnevnikru.setOnClickListener {
            dismissWithAnimation(onOpenDnevnikru)
        }
    }
}