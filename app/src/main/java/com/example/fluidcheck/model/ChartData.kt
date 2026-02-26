package com.example.fluidcheck.model

data class ChartData(
    val points: List<Float>,
    val xOffsets: List<Float>? = null, // Optional: X-positions from 0.0 to 1.0
    val xLabels: List<String>,
    val yLabels: List<String>,
    val maxValue: Float
)
