package com.example.fluidcheck.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

data class FluidType(val name: String, val icon: ImageVector)

val ALL_FLUID_TYPES = listOf(
    FluidType("Water", Icons.Outlined.WaterDrop),
    FluidType("Tea", Icons.Outlined.EmojiFoodBeverage),
    FluidType("Coffee", Icons.Outlined.Coffee),
    FluidType("Smoothie", Icons.Outlined.LocalDrink),
    FluidType("Carbonated water", Icons.Outlined.Water),
    FluidType("Soda", Icons.Outlined.LocalDrink),
    FluidType("Coconut water", Icons.Outlined.WaterDrop),
    FluidType("Milk", Icons.Outlined.WaterDrop),
    FluidType("Juice", Icons.Outlined.WineBar),
    FluidType("Protein shake", Icons.Outlined.FitnessCenter),
    FluidType("Sports drink", Icons.Outlined.Sports),
    FluidType("Energy drink", Icons.Outlined.Bolt),
    FluidType("Soup", Icons.Outlined.SoupKitchen),
    FluidType("Hot chocolate", Icons.Outlined.Coffee),
    FluidType("Wine", Icons.Outlined.WineBar),
    FluidType("Beer", Icons.Outlined.SportsBar),
    FluidType("Liquor", Icons.Outlined.Liquor),
    FluidType("Sparkling water", Icons.Outlined.Water),
)

fun getIconForFluidType(name: String): ImageVector {
    return ALL_FLUID_TYPES.find { it.name == name }?.icon ?: Icons.Outlined.WaterDrop
}
