package ru.tgmaksim.activium.ui.pages.schedule.adapters

import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager

import ru.tgmaksim.activium.ui.pages.schedule.UiScheduleDay
import ru.tgmaksim.activium.databinding.ItemScheduleDayBinding
import ru.tgmaksim.activium.ui.pages.schedule.UiScheduleLesson
import ru.tgmaksim.activium.api.ScheduleExtracurricularActivity
import ru.tgmaksim.activium.ui.pages.schedule.skeletone.LessonSkeletonAdapter

class ScheduleDayAdapter(
    private val skeletonLessonsCount: Int,
    private val onPraiseClick: (String, FloatArray) -> Unit,
    private val onMenuLesson: (String, FloatArray) -> Unit,
    private val onRating: (String) -> Unit
) : ListAdapter<UiScheduleDay?, ScheduleDayAdapter.VH>(Diff()) {
    class VH(
        val ui: ItemScheduleDayBinding,
        skeletonLessonsCount: Int,
        private val onPraiseClick: (String, FloatArray) -> Unit,
        private val onMenuLesson: (String, FloatArray) -> Unit,
        private val onRating: (String) -> Unit
    ) : RecyclerView.ViewHolder(ui.root) {
        private val skeletonAdapter = LessonSkeletonAdapter(skeletonLessonsCount)

        init {
            ui.lessonsRecycler.layoutManager = LinearLayoutManager(
                ui.root.context,
                LinearLayoutManager.VERTICAL,
                false
            )
        }

        fun bind(day: UiScheduleDay?) {
            if (day == null) {
                ui.weekendPhoto.visibility = View.GONE
                ui.lessonsRecycler.visibility = View.VISIBLE
                ui.lessonsRecycler.adapter = skeletonAdapter
            } else if (day.lessons.isEmpty() && day.ea.isEmpty()) {
                ui.lessonsRecycler.visibility = View.GONE
                ui.weekendPhoto.visibility = View.VISIBLE
            } else {
                ui.weekendPhoto.visibility = View.GONE
                ui.lessonsRecycler.visibility = View.VISIBLE

                val lessonAdapter = (ui.lessonsRecycler.adapter as? ScheduleLessonAdapter)
                    ?: ScheduleLessonAdapter(onPraiseClick, onMenuLesson, onRating).also {
                        ui.lessonsRecycler.adapter = it
                    }

                val items = buildList {
                    addAll(day.lessons.sortedBy { it.number })
                    addAll(day.ea.mapIndexed { index, ea ->
                        ea.toUiScheduleLesson(day.lessons.size + index)
                    })
                }

                lessonAdapter.submitList(items)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ui = ItemScheduleDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(ui, skeletonLessonsCount, onPraiseClick, onMenuLesson, onRating)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class Diff : DiffUtil.ItemCallback<UiScheduleDay>() {
        override fun areItemsTheSame(a: UiScheduleDay, b: UiScheduleDay) = a.date == b.date
        override fun areContentsTheSame(a: UiScheduleDay, b: UiScheduleDay) = a == b
    }
}

private fun ScheduleExtracurricularActivity.toUiScheduleLesson(number: Int): UiScheduleLesson {
    return UiScheduleLesson(
        lessonKey = null,
        number = number,
        subject = subject,
        place = place,
        hours = hours,
        works = emptyList(),
        logs = emptyList(),
        othersMarks = emptyList(),
        avgGroupLessonMark = null,
        homework = null,
        note = null,
        files = emptyList(),
        ratingKey = null,
        praiseState = null,
        dnevnikruUrl = null,
        noteState = null,
        isExtra = true
    )
}