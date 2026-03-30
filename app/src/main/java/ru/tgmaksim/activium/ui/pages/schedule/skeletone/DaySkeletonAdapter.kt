package ru.tgmaksim.activium.ui.pages.schedule.skeletone

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.tgmaksim.activium.databinding.ItemScheduleDayBinding

class DaySkeletonAdapter(
    private val daysCount: Int,
    private val lessonsCount: Int
) : RecyclerView.Adapter<DaySkeletonAdapter.VH>() {
    class VH(val ui: ItemScheduleDayBinding) : RecyclerView.ViewHolder(ui.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemScheduleDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.ui.lessonsRecycler.layoutManager = LinearLayoutManager(
            holder.ui.root.context,
            LinearLayoutManager.VERTICAL,
            false
        )
        holder.ui.lessonsRecycler.adapter = LessonSkeletonAdapter(lessonsCount)
        holder.ui.lessonsRecycler.itemAnimator = null
        holder.ui.lessonsRecycler.setHasFixedSize(true)
    }

    override fun getItemCount(): Int = daysCount
}