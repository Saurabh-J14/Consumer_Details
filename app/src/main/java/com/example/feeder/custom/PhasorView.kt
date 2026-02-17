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

    var needleAngle = 30f
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

    // 🔥 Arrow Paint
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        setShadowLayer(8f, 3f, 3f, Color.argb(150, 0, 0, 0))
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

    private val angleOffset = -90.0

    fun setOnRotationCompleteListener(listener: () -> Unit) {
        onRotationComplete = listener
    }

    fun startFiveSecondRotation(targetPhase: String) {
        val targetAngle = when (targetPhase.uppercase()) {
            "A" -> 0f
            "B" -> 120f
            "C" -> 240f
            else -> 30f
        }

        ValueAnimator.ofFloat(needleAngle, targetAngle).apply {
            duration = 5000L
            addUpdateListener { anim ->
                needleAngle = anim.animatedValue as Float
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

        // Outer Circle
        canvas.drawCircle(cx, cy, radius, borderPaint)

        // Phase Sectors
        sectorPaint.color = phaseColors["A"]!!
        canvas.drawArc(oval, 330f + angleOffset.toFloat(), 60f, true, sectorPaint)

        sectorPaint.color = phaseColors["B"]!!
        canvas.drawArc(oval, 90f + angleOffset.toFloat(), 60f, true, sectorPaint)

        sectorPaint.color = phaseColors["C"]!!
        canvas.drawArc(oval, 210f + angleOffset.toFloat(), 60f, true, sectorPaint)

        // Gray fan
        val adjustedNeedleAngle = needleAngle + angleOffset.toFloat()
        canvas.drawArc(oval, adjustedNeedleAngle - 60f, 120f, true, grayFanPaint)

        // Degree lines
        for (deg in 0 until 360 step 30) {
            val adjustedDeg = deg + angleOffset.toFloat()
            val rad = Math.toRadians(adjustedDeg.toDouble())

            val endX = cx + radius * cos(rad).toFloat()
            val endY = cy + radius * sin(rad).toFloat()

            radialLinePaint.strokeWidth =
                if (deg % 90 == 0) 3.5f else 2f

            canvas.drawLine(cx, cy, endX, endY, radialLinePaint)

            val textX = cx + (radius + 34f) * cos(rad).toFloat()
            val textY = cy + (radius + 34f) * sin(rad).toFloat() + 10f
            canvas.drawText(deg.toString(), textX, textY, degreeTextPaint)
        }

        // Phase Labels
        drawPhaseLabel(canvas, cx, cy, radius * 0.56f, (0f + angleOffset).toFloat(), "A")
        drawPhaseLabel(canvas, cx, cy, radius * 0.71f, (120f + angleOffset).toFloat(), "B")
        drawPhaseLabel(canvas, cx, cy, radius * 0.56f, (240f + angleOffset).toFloat(), "C")

        // 🔥 Draw Arrow (Image jaisa)
        drawArrow(canvas, cx, cy, radius * 0.85f)

        // Center circle
        canvas.drawCircle(cx, cy, 14f, Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        })
    }

    // 🔥 Flat Body + Triangle Head Arrow
    private fun drawArrow(canvas: Canvas, cx: Float, cy: Float, length: Float) {

        val rad = Math.toRadians(needleAngle + angleOffset)

        // 🔽 Size Reduced Here
        val bodyLength = length * 0.55f     // pehle 0.65f tha
        val headLength = length * 0.25f     // pehle 0.35f tha
        val bodyWidth = 18f                 // pehle 28f tha
        val headWidth = 45f                 // pehle 70f tha

        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()

        val bodyEndX = cx + bodyLength * cosA
        val bodyEndY = cy + bodyLength * sinA

        val tipX = cx + (bodyLength + headLength) * cosA
        val tipY = cy + (bodyLength + headLength) * sinA

        val perpCos = cos(rad + Math.PI / 2).toFloat()
        val perpSin = sin(rad + Math.PI / 2).toFloat()

        val path = Path().apply {

            moveTo(
                cx + bodyWidth / 2 * perpCos,
                cy + bodyWidth / 2 * perpSin
            )
            lineTo(
                cx - bodyWidth / 2 * perpCos,
                cy - bodyWidth / 2 * perpSin
            )
            lineTo(
                bodyEndX - bodyWidth / 2 * perpCos,
                bodyEndY - bodyWidth / 2 * perpSin
            )

            lineTo(
                bodyEndX - headWidth / 2 * perpCos,
                bodyEndY - headWidth / 2 * perpSin
            )

            lineTo(tipX, tipY)

            lineTo(
                bodyEndX + headWidth / 2 * perpCos,
                bodyEndY + headWidth / 2 * perpSin
            )

            lineTo(
                bodyEndX + bodyWidth / 2 * perpCos,
                bodyEndY + bodyWidth / 2 * perpSin
            )

            close()
        }

        canvas.drawPath(path, arrowPaint)
    }


    private fun drawPhaseLabel(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        deg: Float,
        label: String
    ) {
        val rad = Math.toRadians(deg.toDouble())
        val x = cx + r * cos(rad).toFloat()
        val y = cy + r * sin(rad).toFloat() + 20f
        textPaint.color = Color.WHITE
        canvas.drawText(label, x, y, textPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val dim = min(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        setMeasuredDimension(dim, dim)
    }
}
