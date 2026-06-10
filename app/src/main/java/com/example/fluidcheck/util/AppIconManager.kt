package com.example.fluidcheck.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object AppIconManager {

    private const val ALIAS_DEFAULT = "com.example.fluidcheck.MainActivity" // the default activity
    private const val ALIAS_DARK = "com.example.fluidcheck.MainActivityDark"
    private const val ALIAS_MONOCHROME = "com.example.fluidcheck.MainActivityMonochrome"
    private const val ALIAS_OCEAN = "com.example.fluidcheck.MainActivityOcean"
    private const val ALIAS_SUNSET = "com.example.fluidcheck.MainActivitySunset"
    private const val ALIAS_NEON = "com.example.fluidcheck.MainActivityNeon"
    private const val ALIAS_GOLD = "com.example.fluidcheck.MainActivityGold"
    private const val ALIAS_MINIMAL = "com.example.fluidcheck.MainActivityMinimal"

    private val allAliases = listOf(
        ALIAS_DEFAULT,
        ALIAS_DARK,
        ALIAS_MONOCHROME,
        ALIAS_OCEAN,
        ALIAS_SUNSET,
        ALIAS_NEON,
        ALIAS_GOLD,
        ALIAS_MINIMAL
    )

    fun changeAppIcon(context: Context, iconVariant: String) {
        val targetAlias = when (iconVariant.uppercase()) {
            "DARK" -> ALIAS_DARK
            "MONOCHROME" -> ALIAS_MONOCHROME
            "OCEAN" -> ALIAS_OCEAN
            "SUNSET" -> ALIAS_SUNSET
            "NEON" -> ALIAS_NEON
            "GOLD" -> ALIAS_GOLD
            "MINIMAL" -> ALIAS_MINIMAL
            else -> ALIAS_DEFAULT
        }

        val pm = context.packageManager

        // Enable the target alias
        pm.setComponentEnabledSetting(
            ComponentName(context, targetAlias),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // Disable all other aliases
        for (alias in allAliases) {
            if (alias != targetAlias) {
                pm.setComponentEnabledSetting(
                    ComponentName(context, alias),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}
