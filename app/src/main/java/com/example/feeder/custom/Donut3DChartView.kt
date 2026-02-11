package com.example.feeder.custom

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.feeder.utils.PhaseCount
import kotlin.math.min

class Donut3DChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private var phaseData: List<PhaseCount> = emptyList()
    private var totalCount = 0

    fun setData(total: Int, phases: List<PhaseCount>) {
        totalCount = total
        phaseData = phases
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (totalCount == 0 || phaseData.isEmpty()) return

        val size = min(width, height)
        val center = size / 2f
        val stroke = size * 0.18f

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.color = Color.parseColor("#55000000")
        rect.set(
            center - 200,
            center - 200 + 12,
            center + 200,
            center + 200 + 12
        )
        canvas.drawArc(rect, 0f, 360f, false, paint)

        rect.set(
            center - 200,
            center - 200,
            center + 200,
            center + 200
        )

        var startAngle = -90f
        for (item in phaseData) {
            val sweep = (item.count * 360f) / totalCount
            paint.color = item.color
            canvas.drawArc(rect, startAngle, sweep, false, paint)
            startAngle += sweep
        }

        paint.color = Color.parseColor("#2ECC71")
        paint.strokeWidth = stroke / 2
        rect.set(
            center - 230,
            center - 230,
            center + 230,
            center + 230
        )
        canvas.drawArc(rect, 0f, 360f, false, paint)
    }
}