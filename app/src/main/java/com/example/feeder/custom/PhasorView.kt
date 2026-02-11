package com.example.feeder.custom

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class PhasorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.parseColor("#11DDF8")
        setShadowLayer(15f, 0f, 0f, Color.parseColor("#11DDF8"))
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 6f
        color = Color.GRAY // inactive line color
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 50f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private var activePhases: List<String> = emptyList()

    private var phaseRotation = 0f

    // Phase color mapping
    private val phaseColors = mapOf(
        "A" to Color.RED,
        "B" to Color.YELLOW,
        "C" to Color.BLUE
    )

    fun setPhases(phases: List<String>) {
        activePhases = phases
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = min(width, height)
        val radius = size / 2.8f
        val cx = width / 2f
        val cy = height / 2f

        // Draw outer circle
        canvas.drawCircle(cx, cy, radius, circlePaint)

        // Draw rotating phase lines
        canvas.save()
        canvas.rotate(phaseRotation, cx, cy)

        drawPhaseLine(canvas, cx, cy, radius - 30f, 0.0, "A")
        drawPhaseLine(canvas, cx, cy, radius - 30f, 120.0, "B")
        drawPhaseLine(canvas, cx, cy, radius - 30f, 240.0, "C")

        canvas.restore()

        // Draw phase labels inside circle
        drawPhaseText(canvas, cx, cy, radius - 70f, 0.0, "A")
        drawPhaseText(canvas, cx, cy, radius - 70f, 120.0, "B")
        drawPhaseText(canvas, cx, cy, radius - 70f, 240.0, "C")
    }

    private fun drawPhaseLine(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        angle: Double,
        label: String
    ) {
        val rad = Math.toRadians(angle)
        val lineX = cx + radius * cos(rad).toFloat()
        val lineY = cy + radius * sin(rad).toFloat()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 8f
            color = if (activePhases.contains(label)) phaseColors[label]!! else Color.LTGRAY
        }

        canvas.drawLine(cx, cy, lineX, lineY, paint)
    }

    private fun drawPhaseText(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        angle: Double,
        label: String
    ) {
        val rad = Math.toRadians(angle)
        val textX = cx + radius * cos(rad).toFloat()
        val textY = cy + radius * sin(rad).toFloat() -
                (textPaint.descent() + textPaint.ascent()) / 2

        textPaint.color =
            if (activePhases.contains(label)) phaseColors[label]!! else Color.DKGRAY

        canvas.drawText(label, textX, textY, textPaint)
    }
}
