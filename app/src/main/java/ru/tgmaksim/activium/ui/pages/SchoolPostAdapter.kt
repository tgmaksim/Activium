package ru.tgmaksim.activium.ui.pages

import android.view.View
import android.graphics.Rect
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.core.widget.NestedScrollView

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager

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

    fun settingsScroll(scrollView: NestedScrollView, recyclerView: RecyclerView, onSeePost: (Long) -> Unit) {
        scrollView.setOnScrollChangeListener(object : NestedScrollView.OnScrollChangeListener {
            private val sawPosts = mutableSetOf<Long>()

            override fun onScrollChange(v: NestedScrollView, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return

                val first = layoutManager.findFirstVisibleItemPosition()
                val last = layoutManager.findLastVisibleItemPosition()

                if (first == RecyclerView.NO_POSITION) return

                for (i in first..last) {
                    val view = layoutManager.findViewByPosition(i) ?: continue
                    val postId = view.tag as? Long ?: continue

                    if (postId in sawPosts) continue

                    if (isViewVisible(view)) {
                        sawPosts.add(postId)
                        onSeePost(postId)
                    }
                }
            }

            private fun isViewVisible(view: View): Boolean {
                val rect = Rect()

                val isVisible = view.getGlobalVisibleRect(rect)
                if (!isVisible) return false

                val viewHeight = view.height.toFloat()
                val visibleHeight = rect.height().toFloat()

                return visibleHeight >= viewHeight
            }
        })
    }

    class VH(val ui: ItemSchoolPostBinding) : RecyclerView.ViewHolder(ui.root) {
        fun bind(post: SchoolPost) {
            ui.root.tag = post.postId
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