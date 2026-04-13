package ru.tgmaksim.activium.ui.widgets

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import android.animation.AnimatorListenerAdapter

import android.content.Context

import android.graphics.Path
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Canvas
import android.graphics.PathMeasure

import kotlin.math.min
import android.view.View
import android.util.AttributeSet
import ru.tgmaksim.activium.R

class LongPressBorderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
        color = context.getColor(R.color.lesson_menu_stroke)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val rect = RectF()
    private val arcTL = RectF()
    private val arcTR = RectF()
    private val arcBR = RectF()
    private val arcBL = RectF()

    private val path = Path()
    private val segment = Path()
    private val pathMeasure = PathMeasure()

    private var progress = 0f
    private var animator: ValueAnimator? = null
    private var onComplete: (() -> Unit)? = null

    var cornerRadiusPx: Float = 15f * resources.displayMetrics.density

    fun start(durationMs: Long = 300L, onComplete: () -> Unit) {
        cancel()

        visibility = VISIBLE
        this.onComplete = onComplete

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (progress >= 1f) {
                        this@LongPressBorderView.onComplete?.invoke()
                    }
                }
            })
            start()
        }
    }

    fun cancel() {
        animator?.cancel()
        animator = null
        onComplete = null
        progress = 0f
        visibility = GONE
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (visibility != VISIBLE || progress <= 0f) return

        val halfStroke = paint.strokeWidth / 2f

        rect.set(
            halfStroke,
            halfStroke,
            width - halfStroke,
            height - halfStroke
        )

        val radius = min(cornerRadiusPx, min(rect.width(), rect.height()) / 2f)

        val left = rect.left
        val top = rect.top
        val right = rect.right
        val bottom = rect.bottom
        val cy = rect.centerY()

        arcTL.set(left, top, left + 2 * radius, top + 2 * radius)
        arcTR.set(right - 2 * radius, top, right, top + 2 * radius)
        arcBR.set(right - 2 * radius, bottom - 2 * radius, right, bottom)
        arcBL.set(left, bottom - 2 * radius, left + 2 * radius, bottom)

        path.reset()

        path.reset()

        path.moveTo(left, cy)
        path.lineTo(left, top + radius)

        path.arcTo(arcTL, 180f, 90f, false)
        path.lineTo(right - radius, top)

        path.arcTo(arcTR, 270f, 90f, false)
        path.lineTo(right, bottom - radius)

        path.arcTo(arcBR, 0f, 90f, false)
        path.lineTo(left + radius, bottom)

        path.arcTo(arcBL, 90f, 90f, false)
        path.lineTo(left, cy)

        pathMeasure.setPath(path, false)

        segment.reset()
        pathMeasure.getSegment(0f, pathMeasure.length * progress, segment, true)

        canvas.drawPath(segment, paint)
    }
}