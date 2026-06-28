package com.example.fluidcheck.model

enum class MissionDifficulty(
    val label: String,
    val appearanceChance: Double,     // Probability per board slot
    val shieldFragmentChance: Double, // Drop rate on completion
    val badgeRarity: BadgeRarity,
    val durationDays: Int             // Number of days before mission expires
) {
    EASY("Easy", 0.50, 0.125, BadgeRarity.COMMON, 1),
    MODERATE("Moderate", 0.30, 0.33, BadgeRarity.UNCOMMON, 1),
    HARD("Hard", 0.14, 0.66, BadgeRarity.RARE, 3),
    EPIC("Epic", 0.06, 1.0, BadgeRarity.EPIC, 7)
}

enum class MissionTargetType {
    TOTAL_VOLUME,       // Drink X ml total
    LOG_COUNT,          // Log X drinks
    FLUID_TYPE_VOLUME,  // Drink X ml of a specific fluid type
    FLUID_VARIETY,      // Log X different fluid types
    SINGLE_LOG_AMOUNT,  // Log a single drink of at least X ml
    TIME_WINDOW,        // Log X ml before/after a specific hour
    GOAL_PERCENTAGE     // Reach X% of daily goal
}

data class MissionDefinition(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: MissionDifficulty,
    val targetType: MissionTargetType,
    val targetValue: Int,
    val targetFluidType: String? = null,
    val targetHour: Int? = null,
    val isBeforeHour: Boolean = true,
    val badgeId: String,
    val isMultiDay: Boolean = false // If true, progress carries over across daily resets
)

fun getMissionExpirationTime(acceptedAt: Long, missionDef: MissionDefinition): Long {
    if (missionDef.targetType == MissionTargetType.TIME_WINDOW) {
        val targetHour = missionDef.targetHour ?: 24
        val isBefore = missionDef.isBeforeHour
        
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+8")).apply {
            timeInMillis = acceptedAt
            set(java.util.Calendar.HOUR_OF_DAY, targetHour)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        
        if (isBefore) {
            return calendar.timeInMillis
        } else {
            val midnight = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+8")).apply {
                timeInMillis = acceptedAt
                set(java.util.Calendar.HOUR_OF_DAY, 23)
                set(java.util.Calendar.MINUTE, 59)
                set(java.util.Calendar.SECOND, 59)
                set(java.util.Calendar.MILLISECOND, 999)
            }
            return midnight.timeInMillis
        }
    }
    return acceptedAt + (missionDef.difficulty.durationDays * 24L * 60 * 60 * 1000)
}

data class ActiveMission(
    val missionId: String = "",
    val progress: Int = 0,
    val completed: Boolean = false,
    val aborted: Boolean = false,
    val acceptedAt: Long = 0L
)

data class DailyMissionBoard(
    val date: String = "",
    val boardMissionIds: List<String> = emptyList(),
    val activeMissions: List<ActiveMission> = emptyList(),
    val completedMissionIds: List<String> = emptyList(),
    val abortedMissionIds: List<String> = emptyList()
)
