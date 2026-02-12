package com.example.feeder.utils

import android.graphics.Color

object PhaseUtils {

    // Normalize input
    fun normalizePhase(input: String?): String {
        return input?.trim()?.uppercase() ?: ""
    }

    // Map to A / B / C
    fun getMappedPhase(input: String?): String? {

        return when (normalizePhase(input)) {

            "RYB" -> "A"

            "RY", "Y" -> "B"

            "B", "RB", "YB" -> "C"

            else -> null
        }
    }

    // Get display color
    fun getPhaseColor(input: String?): Int {

        return when (getMappedPhase(input)) {

            "A" -> Color.parseColor("#D32F2F") // Red
            "B" -> Color.parseColor("#FFEB3B") // Yellow
            "C" -> Color.parseColor("#1976D2") // Blue

            else -> Color.BLACK
        }
    }
}
