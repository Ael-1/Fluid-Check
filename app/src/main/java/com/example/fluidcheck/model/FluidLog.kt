package com.example.fluidcheck.model

import com.google.firebase.firestore.Exclude
import com.example.fluidcheck.model.getIconForFluidType
import androidx.compose.ui.graphics.vector.ImageVector

data class FluidLog(
    val id: Long = System.currentTimeMillis(),
    val type: String = "",
    val time: String = "",
    val amount: Int = 0
) {
    @get:Exclude
    val icon: ImageVector
        get() = getIconForFluidType(type)
}
