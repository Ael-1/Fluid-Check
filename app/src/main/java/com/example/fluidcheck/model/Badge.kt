package com.example.fluidcheck.model

enum class BadgeRarity(val label: String, val color: Long) {
    COMMON("Common", 0xFF94A3B8),       // Slate gray
    UNCOMMON("Uncommon", 0xFF3B82F6),   // Blue
    RARE("Rare", 0xFF8B5CF6),           // Purple
    EPIC("Epic", 0xFFF59E0B),           // Gold
    MILESTONE("Milestone", 0xFF10B981)  // Emerald green
}

data class BadgeDefinition(
    val id: String,
    val name: String,
    val description: String,
    val rarity: BadgeRarity,
    val isMilestone: Boolean = false,
    val iconName: String = "Star" // Reference to Material Icon representation
)

data class EarnedBadge(
    val badgeId: String = "",
    val count: Int = 1,
    val firstEarnedAt: Long = 0L
)
