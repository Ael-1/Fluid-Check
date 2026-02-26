package com.example.fluidcheck.model

import com.google.firebase.firestore.Exclude
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.*

data class FluidLog(
    val id: Long = System.currentTimeMillis(),
    val type: String = "",
    val time: String = "", // Display time like "10:30 AM"
    val amount: Int = 0,
    val date: String = "" // Explicitly empty default to ensure we set it GMT+8 in code
) {
    @get:Exclude
    val icon: ImageVector
        get() = getIconForFluidType(type)

    @get:Exclude
    val isEditable: Boolean
        get() = (System.currentTimeMillis() - id) < 24 * 60 * 60 * 1000
}
