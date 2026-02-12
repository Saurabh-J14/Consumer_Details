package com.example.feeder.custom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.animation.doOnEnd
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class PhasorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var needleAngle = 90f   // ← Sui right angle (90°) se start hogi
        set(value) {
            field = value.coerceIn(0f, 360f)
            invalidate()
        }

    private var onRotationComplete: (() -> Unit)? = null

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#4A4A4A")
    }

    private val radialLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2.2f
        color = Color.parseColor("#606060")
    }

    private val sectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        alpha = 255
    }

    private val grayFanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9EAAAA")
        style = Paint.Style.FILL
        alpha = 175
    }

    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 12f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        setShadowLayer(7f, 3f, 3f, Color.argb(160, 0, 0, 0))
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 48f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val degreeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 26f
        color = Color.parseColor("#444444")
        textAlign = Paint.Align.CENTER
    }

    private val phaseColors = mapOf(
        "A" to Color.parseColor("#D32F2F"),
        "B" to Color.parseColor("#FFEB3B"),
        "C" to Color.parseColor("#1976D2")
    )

    // To make 0° appear at the top (12 o'clock position)
    private val angleOffset = -90.0

    fun setOnRotationCompleteListener(listener: () -> Unit) {
        onRotationComplete = listener
    }

    fun startFiveSecondRotation(targetPhase: String) {
        val targetAngle = when (targetPhase.uppercase()) {
            "A" -> 0f
            "B" -> 120f
            "C" -> 240f
            else -> 90f   // default right angle pe
        }

        ValueAnimator.ofFloat(needleAngle, targetAngle).apply {
            duration = 5000L
            addUpdateListener { anim ->
                needleAngle = anim.animatedValue as Float
                invalidate()
            }
            doOnEnd { onRotationComplete?.invoke() }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) * 0.375f

        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        canvas.drawCircle(cx, cy, radius, borderPaint)

        sectorPaint.color = phaseColors["A"]!!
        canvas.drawArc(oval, 330f + angleOffset.toFloat(), 60f, true, sectorPaint)

        sectorPaint.color = phaseColors["B"]!!
        canvas.drawArc(oval, 90f + angleOffset.toFloat(), 60f, true, sectorPaint)

        sectorPaint.color = phaseColors["C"]!!
        canvas.drawArc(oval, 210f + angleOffset.toFloat(), 60f, true, sectorPaint)

        val adjustedNeedleAngle = needleAngle + angleOffset.toFloat()
        canvas.drawArc(oval, adjustedNeedleAngle - 60f, 120f, true, grayFanPaint)

        for (deg in 0 until 360 step 30) {
            val adjustedDeg = deg + angleOffset.toFloat()
            val rad = Math.toRadians(adjustedDeg.toDouble())
            val endX = cx + radius * cos(rad).toFloat()
            val endY = cy + radius * sin(rad).toFloat()

            if (deg % 90 == 0) {
                radialLinePaint.strokeWidth = 3.2f
            } else {
                radialLinePaint.strokeWidth = 2.2f
            }
            canvas.drawLine(cx, cy, endX, endY, radialLinePaint)

            val textX = cx + (radius + 34f) * cos(rad).toFloat()
            val textY = cy + (radius + 34f) * sin(rad).toFloat() + 10f
            canvas.drawText(deg.toString(), textX, textY, degreeTextPaint)
        }

        drawPhaseLabel(canvas, cx, cy, radius * 0.56f, (0f + angleOffset).toFloat(),   "A", Color.WHITE)
        drawPhaseLabel(canvas, cx, cy, radius * 0.71f, (120f + angleOffset).toFloat(), "B", Color.WHITE)
        drawPhaseLabel(canvas, cx, cy, radius * 0.56f, (240f + angleOffset).toFloat(), "C", Color.WHITE)

        val rad = Math.toRadians(needleAngle + angleOffset)
        val tipX = cx + radius * 0.85f * cos(rad).toFloat()
        val tipY = cy + radius * 0.85f * sin(rad).toFloat()
        canvas.drawLine(cx, cy, tipX, tipY, needlePaint)

        canvas.drawCircle(cx, cy, 12f, Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        })
    }

    private fun drawPhaseLabel(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        deg: Float,
        label: String,
        color: Int
    ) {
        val rad = Math.toRadians(deg.toDouble())
        val x = cx + r * cos(rad).toFloat()
        val y = cy + r * sin(rad).toFloat() + 20f
        textPaint.color = color
        canvas.drawText(label, x, y, textPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val dim = min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
        setMeasuredDimension(dim, dim)
    }
}