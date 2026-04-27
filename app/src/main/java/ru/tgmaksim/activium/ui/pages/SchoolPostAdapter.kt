package ru.tgmaksim.activium.ui.pages

import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import coil3.load
import coil3.request.crossfade
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation

import ru.tgmaksim.activium.R
import ru.tgmaksim.activium.api.SchoolPost
import ru.tgmaksim.activium.databinding.ItemSchoolPostBinding

class SchoolPostAdapter : ListAdapter<SchoolPost, SchoolPostAdapter.VH>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ui = ItemSchoolPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(ui)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(val ui: ItemSchoolPostBinding) : RecyclerView.ViewHolder(ui.root) {
        fun bind(post: SchoolPost) {
            ui.title.text = post.title

            if (post.description.isNullOrBlank()) {
                ui.description.visibility = View.GONE
            } else {
                ui.description.visibility = View.VISIBLE
                ui.description.text = post.description
            }

            // image
            if (post.imageUrl == null) {
                ui.imageContainer.visibility = View.GONE
            } else {
                ui.imageContainer.visibility = View.VISIBLE
                ui.imagePlaceholder.visibility = View.VISIBLE

                ui.image.load(post.imageUrl) {
                    transformations(RoundedCornersTransformation(16f))
                    crossfade(true)

                    listener(
                        onSuccess = { _, _ ->
                            ui.image.background = null
                            ui.imagePlaceholder.visibility = View.GONE
                        },
                        onError = { _, _ ->
                            ui.imagePlaceholder.visibility = View.VISIBLE
                            ui.image.setBackgroundColor(ui.root.context.getColor(R.color.skeleton_base))
                        }
                    )
                }
            }

            ui.author.text = post.author
            ui.authorVerified.visibility = if (post.authorVerified) View.VISIBLE else View.GONE
            ui.date.text = post.humanCreatedAt
            ui.updated.visibility = if (post.isUpdated) View.VISIBLE else View.GONE

            // событие
            if (post.scheduleDate != null) {
                ui.scheduleDate.visibility = View.VISIBLE
                ui.scheduleDate.text = ui.root.context.getString(
                    R.string.school_post_schedule,
                    post.humanScheduleDate
                )
            } else {
                ui.scheduleDate.visibility = View.GONE
            }

            // просмотры
            ui.viewings.text = ui.root.context.getString(R.string.school_post_viewings, post.countViewings)

            // лайки
            ui.likes.text = post.countLikes.toString()

            ui.likeIcon.setImageResource(
                if (post.hasMyLike) R.drawable.like else R.drawable.like_outline
            )
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SchoolPost>() {
        override fun areItemsTheSame(oldItem: SchoolPost, newItem: SchoolPost) =
            oldItem.postId == newItem.postId

        override fun areContentsTheSame(oldItem: SchoolPost, newItem: SchoolPost) =
            oldItem == newItem
    }
}