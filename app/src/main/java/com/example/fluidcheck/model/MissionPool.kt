package com.example.fluidcheck.model

object MissionPool {

    // ── Milestone Badges (13 Total) ──
    val milestoneBadges = listOf(
        BadgeDefinition("milestone_streak_7", "Streak Starter", "Reach a 7-day active streak", BadgeRarity.MILESTONE, true, "LocalFireDepartment"),
        BadgeDefinition("milestone_streak_25", "Quarter Century", "Reach a 25-day active streak", BadgeRarity.MILESTONE, true, "LocalFireDepartment"),
        BadgeDefinition("milestone_streak_50", "Half Century", "Reach a 50-day active streak", BadgeRarity.MILESTONE, true, "LocalFireDepartment"),
        BadgeDefinition("milestone_streak_100", "Centurion", "Reach a 100-day active streak", BadgeRarity.MILESTONE, true, "LocalFireDepartment"),
        BadgeDefinition("milestone_streak_365", "Year of Hydration", "Reach a 365-day active streak", BadgeRarity.MILESTONE, true, "WorkspacePremium"),
        
        BadgeDefinition("milestone_volume_50", "50L Club", "Log 50 Liters of fluid lifetime", BadgeRarity.MILESTONE, true, "WaterDrop"),
        BadgeDefinition("milestone_volume_100", "100L Club", "Log 100 Liters of fluid lifetime", BadgeRarity.MILESTONE, true, "Waves"),
        BadgeDefinition("milestone_volume_500", "500L Club", "Log 500 Liters of fluid lifetime", BadgeRarity.MILESTONE, true, "Tsunami"),
        
        BadgeDefinition("milestone_early_bird", "Early Bird", "Log water before 8 AM for 10 consecutive days", BadgeRarity.MILESTONE, true, "WbSunny"),
        BadgeDefinition("milestone_perfect_week", "Perfect Week", "Close progress ring 7 days in a row without using a shield", BadgeRarity.MILESTONE, true, "CheckCircle"),
        BadgeDefinition("milestone_hydration_master", "Hydration Master", "Log 3+ fluid types daily for 14 consecutive days", BadgeRarity.MILESTONE, true, "Palette"),
        BadgeDefinition("milestone_weekend_warrior", "Weekend Warrior", "Meet goal on Sat & Sun for 4 consecutive weeks", BadgeRarity.MILESTONE, true, "Weekend"),
        BadgeDefinition("milestone_night_owl", "Night Owl", "Log after 10 PM for 7 consecutive days", BadgeRarity.MILESTONE, true, "NightsStay")
    )

    // ── Mission Definitions (50 Total) ──
    val missions = listOf(
        // EASY (20)
        MissionDefinition("easy_01", "Hydration Rookie", "Log 1 drink today", MissionDifficulty.EASY, MissionTargetType.LOG_COUNT, 1, badgeId = "badge_easy_01"),
        MissionDefinition("easy_02", "Quick Sip", "Drink 250ml of water", MissionDifficulty.EASY, MissionTargetType.FLUID_TYPE_VOLUME, 250, "Water", badgeId = "badge_easy_02"),
        MissionDefinition("easy_03", "Morning Dew", "Drink 250ml before 10 AM", MissionDifficulty.EASY, MissionTargetType.TIME_WINDOW, 250, targetHour = 10, isBeforeHour = true, badgeId = "badge_easy_03"),
        MissionDefinition("easy_04", "Steady Pace", "Log 2 drinks today", MissionDifficulty.EASY, MissionTargetType.LOG_COUNT, 2, badgeId = "badge_easy_04"),
        MissionDefinition("easy_05", "Quarter Way", "Reach 25% of your daily goal", MissionDifficulty.EASY, MissionTargetType.GOAL_PERCENTAGE, 25, badgeId = "badge_easy_05"),
        MissionDefinition("easy_06", "Water Focused", "Drink 500ml of water", MissionDifficulty.EASY, MissionTargetType.FLUID_TYPE_VOLUME, 500, "Water", badgeId = "badge_easy_06"),
        MissionDefinition("easy_07", "Caffeine Kick", "Log 1 Coffee or Tea", MissionDifficulty.EASY, MissionTargetType.FLUID_TYPE_VOLUME, 100, "Coffee", badgeId = "badge_easy_07"), // Simplifying condition for now
        MissionDefinition("easy_08", "Evening Sip", "Log 250ml after 6 PM", MissionDifficulty.EASY, MissionTargetType.TIME_WINDOW, 250, targetHour = 18, isBeforeHour = false, badgeId = "badge_easy_08"),
        MissionDefinition("easy_09", "Big Gulp", "Log a single drink of at least 400ml", MissionDifficulty.EASY, MissionTargetType.SINGLE_LOG_AMOUNT, 400, badgeId = "badge_easy_09"),
        MissionDefinition("easy_10", "Half Liter", "Drink 500ml total", MissionDifficulty.EASY, MissionTargetType.TOTAL_VOLUME, 500, badgeId = "badge_easy_10"),
        MissionDefinition("easy_11", "Three's Company", "Log 3 drinks today", MissionDifficulty.EASY, MissionTargetType.LOG_COUNT, 3, badgeId = "badge_easy_11"),
        MissionDefinition("easy_12", "Variety Spice", "Log 2 different fluid types", MissionDifficulty.EASY, MissionTargetType.FLUID_VARIETY, 2, badgeId = "badge_easy_12"),
        MissionDefinition("easy_13", "Halfway There", "Reach 50% of your daily goal", MissionDifficulty.EASY, MissionTargetType.GOAL_PERCENTAGE, 50, badgeId = "badge_easy_13"),
        MissionDefinition("easy_14", "Nightcap", "Log 200ml after 8 PM", MissionDifficulty.EASY, MissionTargetType.TIME_WINDOW, 200, targetHour = 20, isBeforeHour = false, badgeId = "badge_easy_14"),
        MissionDefinition("easy_15", "Liter Mark", "Drink 1000ml total", MissionDifficulty.EASY, MissionTargetType.TOTAL_VOLUME, 1000, badgeId = "badge_easy_15"),
        MissionDefinition("easy_16", "Four in a Row", "Log 4 drinks today", MissionDifficulty.EASY, MissionTargetType.LOG_COUNT, 4, badgeId = "badge_easy_16"),
        MissionDefinition("easy_17", "Early Start", "Log 300ml before 9 AM", MissionDifficulty.EASY, MissionTargetType.TIME_WINDOW, 300, targetHour = 9, isBeforeHour = true, badgeId = "badge_easy_17"),
        MissionDefinition("easy_18", "Pure Hydration", "Drink 750ml of water", MissionDifficulty.EASY, MissionTargetType.FLUID_TYPE_VOLUME, 750, "Water", badgeId = "badge_easy_18"),
        MissionDefinition("easy_19", "Thirst Quencher", "Log a single drink of at least 500ml", MissionDifficulty.EASY, MissionTargetType.SINGLE_LOG_AMOUNT, 500, badgeId = "badge_easy_19"),
        MissionDefinition("easy_20", "Consistent Sips", "Log 5 drinks today", MissionDifficulty.EASY, MissionTargetType.LOG_COUNT, 5, badgeId = "badge_easy_20"),

        // MODERATE (15)
        MissionDefinition("mod_01", "Liter and a Half", "Drink 1500ml total", MissionDifficulty.MODERATE, MissionTargetType.TOTAL_VOLUME, 1500, badgeId = "badge_mod_01"),
        MissionDefinition("mod_02", "Almost Done", "Reach 75% of your daily goal", MissionDifficulty.MODERATE, MissionTargetType.GOAL_PERCENTAGE, 75, badgeId = "badge_mod_02"),
        MissionDefinition("mod_03", "Six Pack", "Log 6 drinks today", MissionDifficulty.MODERATE, MissionTargetType.LOG_COUNT, 6, badgeId = "badge_mod_03"),
        MissionDefinition("mod_04", "Fluid Explorer", "Log 3 different fluid types", MissionDifficulty.MODERATE, MissionTargetType.FLUID_VARIETY, 3, badgeId = "badge_mod_04"),
        MissionDefinition("mod_05", "Water Champion", "Drink 1500ml of water", MissionDifficulty.MODERATE, MissionTargetType.FLUID_TYPE_VOLUME, 1500, "Water", badgeId = "badge_mod_05"),
        MissionDefinition("mod_06", "Morning Achiever", "Drink 1000ml before 12 PM", MissionDifficulty.MODERATE, MissionTargetType.TIME_WINDOW, 1000, targetHour = 12, isBeforeHour = true, badgeId = "badge_mod_06"),
        MissionDefinition("mod_07", "Two Liters", "Drink 2000ml total", MissionDifficulty.MODERATE, MissionTargetType.TOTAL_VOLUME, 2000, badgeId = "badge_mod_07"),
        MissionDefinition("mod_08", "Frequent Flyer", "Log 7 drinks today", MissionDifficulty.MODERATE, MissionTargetType.LOG_COUNT, 7, badgeId = "badge_mod_08"),
        MissionDefinition("mod_09", "Chug", "Log a single drink of at least 750ml", MissionDifficulty.MODERATE, MissionTargetType.SINGLE_LOG_AMOUNT, 750, badgeId = "badge_mod_09"),
        MissionDefinition("mod_10", "Evening Catchup", "Log 1000ml after 4 PM", MissionDifficulty.MODERATE, MissionTargetType.TIME_WINDOW, 1000, targetHour = 16, isBeforeHour = false, badgeId = "badge_mod_10"),
        MissionDefinition("mod_11", "Drink Diversity", "Log 4 different fluid types", MissionDifficulty.MODERATE, MissionTargetType.FLUID_VARIETY, 4, badgeId = "badge_mod_11"),
        MissionDefinition("mod_12", "Water Lover", "Drink 2000ml of water", MissionDifficulty.MODERATE, MissionTargetType.FLUID_TYPE_VOLUME, 2000, "Water", badgeId = "badge_mod_12"),
        MissionDefinition("mod_13", "Eight is Great", "Log 8 drinks today", MissionDifficulty.MODERATE, MissionTargetType.LOG_COUNT, 8, badgeId = "badge_mod_13"),
        MissionDefinition("mod_14", "Goal Crusher", "Reach 90% of your daily goal", MissionDifficulty.MODERATE, MissionTargetType.GOAL_PERCENTAGE, 90, badgeId = "badge_mod_14"),
        MissionDefinition("mod_15", "Two and a Half", "Drink 2500ml total", MissionDifficulty.MODERATE, MissionTargetType.TOTAL_VOLUME, 2500, badgeId = "badge_mod_15"),

        // HARD (10) - 3 Days
        MissionDefinition("hard_01", "Ring Closer III", "Reach 300% of your daily goal", MissionDifficulty.HARD, MissionTargetType.GOAL_PERCENTAGE, 300, badgeId = "badge_hard_01"),
        MissionDefinition("hard_02", "Nine Liters", "Drink 9000ml total", MissionDifficulty.HARD, MissionTargetType.TOTAL_VOLUME, 9000, badgeId = "badge_hard_02"),
        MissionDefinition("hard_03", "Hydration Expert", "Log 30 drinks", MissionDifficulty.HARD, MissionTargetType.LOG_COUNT, 30, badgeId = "badge_hard_03"),
        MissionDefinition("hard_04", "Water Only", "Drink 7500ml of water", MissionDifficulty.HARD, MissionTargetType.FLUID_TYPE_VOLUME, 7500, "Water", badgeId = "badge_hard_04"),
        MissionDefinition("hard_05", "Fluid Connoisseur", "Log 10 different fluid types", MissionDifficulty.HARD, MissionTargetType.FLUID_VARIETY, 10, badgeId = "badge_hard_05"),
        MissionDefinition("hard_06", "Morning Legend", "Drink 4500ml before 12 PM", MissionDifficulty.HARD, MissionTargetType.TIME_WINDOW, 4500, targetHour = 12, isBeforeHour = true, badgeId = "badge_hard_06"),
        MissionDefinition("hard_07", "Overachiever", "Reach 360% of your daily goal", MissionDifficulty.HARD, MissionTargetType.GOAL_PERCENTAGE, 360, badgeId = "badge_hard_07"),
        MissionDefinition("hard_08", "Persistent", "Log 36 drinks", MissionDifficulty.HARD, MissionTargetType.LOG_COUNT, 36, badgeId = "badge_hard_08"),
        MissionDefinition("hard_09", "Ten Liters", "Drink 10000ml total", MissionDifficulty.HARD, MissionTargetType.TOTAL_VOLUME, 10000, badgeId = "badge_hard_09"),
        MissionDefinition("hard_10", "Camel", "Drink 12000ml total", MissionDifficulty.HARD, MissionTargetType.TOTAL_VOLUME, 12000, badgeId = "badge_hard_10"),

        // EPIC (5) - 7 Days
        MissionDefinition("epic_01", "Flawless Week", "Reach 1000% of your daily goal", MissionDifficulty.EPIC, MissionTargetType.GOAL_PERCENTAGE, 1000, badgeId = "badge_epic_01"),
        MissionDefinition("epic_02", "Ocean Drinker", "Drink 28000ml total", MissionDifficulty.EPIC, MissionTargetType.TOTAL_VOLUME, 28000, badgeId = "badge_epic_02"),
        MissionDefinition("epic_03", "Hydration God", "Log 100 drinks", MissionDifficulty.EPIC, MissionTargetType.LOG_COUNT, 100, badgeId = "badge_epic_03"),
        MissionDefinition("epic_04", "Water Deity", "Drink 25000ml of water", MissionDifficulty.EPIC, MissionTargetType.FLUID_TYPE_VOLUME, 25000, "Water", badgeId = "badge_epic_04"),
        MissionDefinition("epic_05", "Rainbow Drinker", "Log 15 different fluid types", MissionDifficulty.EPIC, MissionTargetType.FLUID_VARIETY, 15, badgeId = "badge_epic_05")
    )

    // Automatically generate badge definitions for the 50 missions
    val missionBadges = missions.map { mission ->
        val icon = when (mission.difficulty) {
            MissionDifficulty.EASY -> "Done"
            MissionDifficulty.MODERATE -> "ThumbUp"
            MissionDifficulty.HARD -> "Stars"
            MissionDifficulty.EPIC -> "WorkspacePremium"
        }
        BadgeDefinition(
            id = mission.badgeId,
            name = mission.title,
            description = "Completed: ${mission.description}",
            rarity = mission.difficulty.badgeRarity,
            isMilestone = false,
            iconName = icon
        )
    }

    val allBadges = milestoneBadges + missionBadges

    fun getBadge(id: String): BadgeDefinition? = allBadges.find { it.id == id }
    fun getMission(id: String): MissionDefinition? = missions.find { it.id == id }
}
