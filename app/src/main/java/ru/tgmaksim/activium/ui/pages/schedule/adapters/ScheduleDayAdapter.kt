package ru.tgmaksim.activium.ui.pages.schedule.adapters

import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import ru.tgmaksim.activium.api.ScheduleDay
import ru.tgmaksim.activium.api.ScheduleLesson
import ru.tgmaksim.activium.databinding.ItemScheduleDayBinding
import ru.tgmaksim.activium.api.ScheduleExtracurricularActivity
import ru.tgmaksim.activium.ui.pages.schedule.skeletone.LessonSkeletonAdapter

class ScheduleDayAdapter(
    private val skeletonLessonsCount: Int,
    private val onPraiseClick: (String) -> Unit
) : ListAdapter<ScheduleDay?, ScheduleDayAdapter.VH>(Diff()) {
    private var hasAbilityPraise = false

    fun setHasAbilityPraise(value: Boolean) {
        hasAbilityPraise = value
    }

    class VH(
        val ui: ItemScheduleDayBinding,
        skeletonLessonsCount: Int,
        onPraiseClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(ui.root) {
        private val lessonAdapter = ScheduleLessonAdapter(onPraiseClick)
        private val skeletonAdapter = LessonSkeletonAdapter(skeletonLessonsCount)

        init {
            ui.lessonsRecycler.layoutManager = LinearLayoutManager(
                ui.root.context,
                LinearLayoutManager.VERTICAL,
                false
            )
            ui.lessonsRecycler.adapter = lessonAdapter
            ui.lessonsRecycler.itemAnimator = null
        }

        fun bind(day: ScheduleDay?, hasAbilityPraise: Boolean) {
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
                ui.lessonsRecycler.adapter = lessonAdapter

                lessonAdapter.setHasAbilityPraise(hasAbilityPraise)

                val items = buildList {
                    addAll(day.lessons.sortedBy { it.number })
                    addAll(day.ea.mapIndexed { index, ea ->
                        ea.toScheduleLesson(day.lessons.size + index)
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
        return VH(ui, skeletonLessonsCount, onPraiseClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), hasAbilityPraise)
    }

    class Diff : DiffUtil.ItemCallback<ScheduleDay>() {
        override fun areItemsTheSame(a: ScheduleDay, b: ScheduleDay) = a.date == b.date
        override fun areContentsTheSame(a: ScheduleDay, b: ScheduleDay) = a == b
    }
}

private fun ScheduleExtracurricularActivity.toScheduleLesson(number: Int): ScheduleLesson {
    return ScheduleLesson(
        lessonKey = "ea:",
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
        ratingKey = null
    )
}