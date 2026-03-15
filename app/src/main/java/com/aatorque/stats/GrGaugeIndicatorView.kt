package com.aatorque.stats

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.max
import kotlin.math.min
import kotlin.math.cos
import kotlin.math.sin

class GrGaugeIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val arcBounds = RectF()
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#343434")
        strokeCap = Paint.Cap.BUTT
    }
    private val foregroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#E00000")
        strokeCap = Paint.Cap.ROUND
    }
    private val peakMarkerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#E00000")
        strokeCap = Paint.Cap.BUTT
    }
    private var minValue = 0f
    private var maxValue = 100f
    private var ratio = 0f
    private var peakRatio = 0f
    private var animator: ValueAnimator? = null

    fun setRange(min: Float, max: Float) {
        minValue = min
        maxValue = if (min == max) max + 1f else max
        peakRatio = 0f
    }

    fun setPeakValue(value: Float) {
        peakRatio = normalized(value).coerceAtLeast(peakRatio)
        invalidate()
    }

    fun setValue(value: Float, animateMs: Long = 0L) {
        val target = normalized(value)
        if (animateMs <= 0L || !isLaidOut) {
            ratio = target
            invalidate()
            return
        }
        animator?.cancel()
        animator = ValueAnimator.ofFloat(ratio, target).apply {
            duration = animateMs
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener {
                ratio = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width.toFloat(), height.toFloat())
        if (size <= 0f) return

        val strokeWidth = size * (8f / 152f)
        backgroundPaint.strokeWidth = strokeWidth
        foregroundPaint.strokeWidth = strokeWidth

        val centerX = width * 0.5f
        val centerY = height * 0.5f
        val radius = (size - strokeWidth) * 0.5f
        arcBounds.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)
        canvas.drawArc(arcBounds, START_ANGLE, MAX_SWEEP * ratio, false, foregroundPaint)
        drawPeakMarker(canvas, centerX, centerY, radius, strokeWidth)
    }

    private fun normalized(value: Float): Float {
        val range = max(maxValue - minValue, 1e-6f)
        val normalized = (value - minValue) / range
        return normalized.coerceIn(0f, 1f)
    }

    private fun drawPeakMarker(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        strokeWidth: Float,
    ) {
        val markerAngle = START_ANGLE + (MAX_SWEEP * peakRatio)
        val angleRad = Math.toRadians(markerAngle.toDouble())
        val inner = radius - (strokeWidth * 0.7f)
        val outer = radius + (strokeWidth * 1.25f)
        val startX = centerX + (cos(angleRad).toFloat() * inner)
        val startY = centerY + (sin(angleRad).toFloat() * inner)
        val endX = centerX + (cos(angleRad).toFloat() * outer)
        val endY = centerY + (sin(angleRad).toFloat() * outer)
        peakMarkerPaint.strokeWidth = max(2f, strokeWidth * 0.44f)
        canvas.drawLine(startX, startY, endX, endY, peakMarkerPaint)
    }

    private companion object {
        private const val START_ANGLE = 138f
        private const val MAX_SWEEP = 259f
    }
}
