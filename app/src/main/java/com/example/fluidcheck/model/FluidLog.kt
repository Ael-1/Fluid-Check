package com.example.fluidcheck.model

import androidx.compose.ui.graphics.vector.ImageVector

data class FluidLog(
    val id: Long = System.currentTimeMillis(),
    val type: String,
    val time: String,
    val amount: Int,
    val icon: ImageVector
)
