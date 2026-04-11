package ru.tgmaksim.activium.ui.pages

import kotlin.math.min
import kotlin.math.abs

import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Typeface

import android.view.View
import java.util.LinkedList
import android.content.Context
import android.view.MotionEvent
import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper

import ru.tgmaksim.activium.R

@SuppressLint("ClickableViewAccessibility")
class RatingDialogSwipeHelper(
    private val recyclerView: RecyclerView,
    private val canShowMenu: (Int) -> Boolean,
    private val getterButtonParams: (Int) -> Pair<String, Int>,
    private val onButtonClick: (Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
    private var swipedPosition = RecyclerView.NO_POSITION
    private var buttonRect: RectF? = null

    private val buttonsBuffer: MutableMap<Int, List<UnderlayButton>> = mutableMapOf()
    private val swipeProgressMap = mutableMapOf<Int, Float>()

    private val recoverQueue = object : LinkedList<Int>() {
        override fun add(element: Int): Boolean {
            if (contains(element)) return false
            return super.add(element)
        }
    }

    init {
        recyclerView.setOnTouchListener { _, event ->
            if (swipedPosition < 0) return@setOnTouchListener false
            if (event.action != MotionEvent.ACTION_UP) return@setOnTouchListener false

            val position = swipedPosition
            closeMenu(swipedPosition)

            buttonRect?.let { rect ->
                if (rect.contains(event.x, event.y)) {
                    onButtonClick(position)
                }
            }

            true
        }
    }

    private fun closeMenu(position: Int) {
        recoverQueue.add(position)
        swipedPosition = RecyclerView.NO_POSITION
        recoverSwipedItem()
    }

    private fun recoverSwipedItem() {
        while (recoverQueue.isNotEmpty()) {
            val position = recoverQueue.poll() ?: return
            recyclerView.adapter?.notifyItemChanged(position)
        }
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun getSwipeDirs(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val position = viewHolder.bindingAdapterPosition
        return if (position == RecyclerView.NO_POSITION || !canShowMenu(position)) {
            0
        } else {
            ItemTouchHelper.RIGHT
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val position = viewHolder.bindingAdapterPosition
        val itemView = viewHolder.itemView
        var maxDX = dX

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            swipeProgressMap[position] = abs(dX) / itemView.width.toFloat()

            if (dX > 0f) {
                if (!buttonsBuffer.containsKey(position)) {
                    buttonsBuffer[position] = listOf(createButton(position))
                }

                val buttons = buttonsBuffer[position] ?: return
                if (buttons.isEmpty()) return

                maxDX = min(buttons.intrinsicWidth(), dX)
                drawButtons(c, buttons, itemView, maxDX)
            }
        }

        super.onChildDraw(
            c,
            recyclerView,
            viewHolder,
            maxDX,
            dY,
            actionState,
            isCurrentlyActive
        )
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        val position = viewHolder.bindingAdapterPosition
        val progress = swipeProgressMap.getOrDefault(position, 0f)
        return if (progress > 0.4f) 1f else 0.05f
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition
        if (swipedPosition != position) recoverQueue.add(swipedPosition)
        swipedPosition = position
        recoverSwipedItem()
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        swipeProgressMap.remove(viewHolder.bindingAdapterPosition)
    }

    private fun createButton(position: Int): UnderlayButton {
        val (title, iconRes) = getterButtonParams(position)
        return UnderlayButton(
            context = recyclerView.context,
            title = title,
            textSize = 13f,
            colorRes = R.color.mark_log_more,
            iconRes = iconRes,
            iconSize = 18
        )
    }

    private fun drawButtons(
        canvas: Canvas,
        buttons: List<UnderlayButton>,
        itemView: View,
        dX: Float
    ) {
        var left = itemView.left.toFloat()
        buttons.forEach { button ->
            val width = button.intrinsicWidth / buttons.intrinsicWidth() * abs(dX)
            val right = left + width
            button.onDraw(
                canvas,
                RectF(left, itemView.top.toFloat(), right, itemView.bottom.toFloat())
            )
            left = right
        }
        buttonRect = RectF(itemView.left.toFloat(), itemView.top.toFloat(), itemView.left + dX, itemView.bottom.toFloat())
    }

    class UnderlayButton(
        private val context: Context,
        private val title: String,
        textSize: Float,
        private val colorRes: Int,
        private val iconRes: Int? = null,
        iconSize: Int? = null
    ) {
        private var clickableRegion: RectF? = null
        private val textSizeInPixel: Float = textSize * context.resources.displayMetrics.density
        private val iconSizeInPixel: Float = iconSize?.let { it * context.resources.displayMetrics.density } ?: 100f
        private val horizontalPadding = 24.0f
        private val verticalPadding = 18.0f
        val intrinsicWidth: Float

        init {
            val paint = Paint()
            paint.textSize = textSizeInPixel
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.LEFT
            val titleBounds = Rect()
            paint.getTextBounds(title, 0, title.length, titleBounds)
            intrinsicWidth = (titleBounds.width() + 2 * horizontalPadding)
        }

        fun onDraw(canvas: Canvas, rect: RectF) {
            val paint = Paint()

            paint.color = ContextCompat.getColor(context, colorRes)
            canvas.drawRect(rect, paint)

            val iconLeft = rect.left + (rect.width() - iconSizeInPixel) / 2
            val iconTop = rect.top + 8f

            iconRes?.let {
                ContextCompat.getDrawable(context, it)?.let { icon ->
                    icon.setBounds(
                        iconLeft.toInt(),
                        iconTop.toInt(),
                        (iconLeft + iconSizeInPixel).toInt(),
                        (iconTop + iconSizeInPixel).toInt()
                    )
                    icon.draw(canvas)
                }
            }

            paint.color = ContextCompat.getColor(context, android.R.color.white)
            paint.textSize = textSizeInPixel
            paint.typeface = Typeface.DEFAULT
            paint.textAlign = Paint.Align.CENTER

            val titleBounds = Rect()
            paint.getTextBounds(title, 0, title.length, titleBounds)

            val textTop = iconTop + iconSizeInPixel + verticalPadding / 2f
            val textY = textTop + titleBounds.height()
            canvas.drawText(title, rect.centerX(), textY, paint)

            clickableRegion = rect
        }
    }
}

private fun List<RatingDialogSwipeHelper.UnderlayButton>.intrinsicWidth(): Float {
    if (isEmpty()) return 0.0f
    return sumOf { it.intrinsicWidth.toDouble() }.toFloat()
}