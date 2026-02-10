package com.example.fluidcheck.model

data class ChartData(
    val points: List<Float>,
    val xLabels: List<String>,
    val yLabels: List<String>,
    val maxValue: Float
)
