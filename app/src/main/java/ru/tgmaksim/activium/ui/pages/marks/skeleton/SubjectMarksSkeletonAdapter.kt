package ru.tgmaksim.activium.ui.pages.marks.skeleton

import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView

import ru.tgmaksim.activium.databinding.ItemSubjectMarksSkeletonBinding

class SubjectMarksSkeletonAdapter(
    private val count: Int
) : RecyclerView.Adapter<SubjectMarksSkeletonAdapter.VH>() {
    class VH(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSubjectMarksSkeletonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding.root)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = Unit

    override fun getItemCount(): Int = count
}