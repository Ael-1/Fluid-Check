package com.example.fluidcheck.util

import android.content.Context
import com.example.fluidcheck.R
import kotlin.math.roundToInt

enum class VolumeUnit(val displayName: String) {
    METRIC("ml"),
    IMPERIAL("oz");
    companion object {
        fun fromString(value: String): VolumeUnit = entries.find { it.name.equals(value, ignoreCase = true) } ?: METRIC
    }
}

enum class WeightUnit(val displayName: String) {
    METRIC("kg"),
    IMPERIAL("lbs");
    companion object {
        fun fromString(value: String): WeightUnit = entries.find { it.name.equals(value, ignoreCase = true) } ?: METRIC
    }
}

enum class HeightUnit(val displayName: String) {
    METRIC("cm"),
    IMPERIAL("ft/in");
    companion object {
        fun fromString(value: String): HeightUnit = entries.find { it.name.equals(value, ignoreCase = true) } ?: METRIC
    }
}

data class MeasurementPreferences(
    val volume: VolumeUnit = VolumeUnit.METRIC,
    val weight: WeightUnit = WeightUnit.METRIC,
    val height: HeightUnit = HeightUnit.METRIC
)

/**
 * Centralized utility for all measurement conversions, formatting, and labels.
 *
 * Design principle: ALL data is stored internally in metric (ml for volume, kg for weight, cm for height).
 * This utility converts between internal metric values and the user's preferred display units.
 */
object MeasurementUtils {

    // ==================== Conversion Constants ====================
    private const val ML_PER_FL_OZ = 29.5735
    private const val LBS_PER_KG = 2.20462
    private const val CM_PER_INCH = 2.54
    private const val INCHES_PER_FOOT = 12

    // ==================== Volume (ml ↔ oz) ====================

    /**
     * Convert an internal ml value to the display unit value.
     */
    fun convertVolumeForDisplay(valueMl: Int, unit: VolumeUnit): Double {
        return when (unit) {
            VolumeUnit.METRIC -> valueMl.toDouble()
            VolumeUnit.IMPERIAL -> valueMl / ML_PER_FL_OZ
        }
    }

    /**
     * Convert a user-entered display value back to internal ml.
     */
    fun convertVolumeToMl(displayValue: Double, unit: VolumeUnit): Int {
        return when (unit) {
            VolumeUnit.METRIC -> displayValue.roundToInt()
            VolumeUnit.IMPERIAL -> (displayValue * ML_PER_FL_OZ).roundToInt()
        }
    }

    /**
     * Format an internal ml value for display with unit suffix.
     * Examples: "250 ml", "8.5 oz"
     */
    fun formatVolume(context: Context, valueMl: Int, unit: VolumeUnit): String {
        return when (unit) {
            VolumeUnit.METRIC -> context.getString(R.string.value_with_unit, valueMl.toString(), context.getString(R.string.ml_unit))
            VolumeUnit.IMPERIAL -> {
                val flOz = valueMl / ML_PER_FL_OZ
                if (flOz == flOz.toLong().toDouble()) {
                    context.getString(R.string.value_with_unit, flOz.toLong().toString(), context.getString(R.string.oz_unit))
                } else {
                    context.getString(R.string.value_with_unit, "%.1f".format(flOz), context.getString(R.string.oz_unit))
                }
            }
        }
    }

    /**
     * Format a volume value for compact display (e.g., on quick-add buttons).
     * Examples: "250ml", "8oz"
     */
    fun formatVolumeCompact(context: Context, valueMl: Int, unit: VolumeUnit): String {
        return when (unit) {
            VolumeUnit.METRIC -> context.getString(R.string.value_with_unit_compact, valueMl.toString(), context.getString(R.string.ml_unit))
            VolumeUnit.IMPERIAL -> {
                val flOz = valueMl / ML_PER_FL_OZ
                if (flOz == flOz.toLong().toDouble()) {
                    context.getString(R.string.value_with_unit_compact, flOz.toLong().toString(), context.getString(R.string.oz_unit))
                } else {
                    context.getString(R.string.value_with_unit_compact, "%.1f".format(flOz), context.getString(R.string.oz_unit))
                }
            }
        }
    }

    /**
     * Get the volume unit abbreviation string.
     */
    fun volumeUnit(context: Context, unit: VolumeUnit): String {
        return when (unit) {
            VolumeUnit.METRIC -> context.getString(R.string.ml_unit)
            VolumeUnit.IMPERIAL -> context.getString(R.string.oz_unit)
        }
    }

    /**
     * Format the progress ring display string.
     * Examples: "1,500 / 3,000 ml", "50.7 / 101.4 oz"
     */
    fun formatProgressRing(context: Context, currentMl: Int, goalMl: Int, unit: VolumeUnit): String {
        return when (unit) {
            VolumeUnit.METRIC -> context.getString(
                R.string.progress_ring_format,
                "%,d".format(currentMl),
                "%,d".format(goalMl),
                context.getString(R.string.ml_unit)
            )
            VolumeUnit.IMPERIAL -> {
                val currentFlOz = currentMl / ML_PER_FL_OZ
                val goalFlOz = goalMl / ML_PER_FL_OZ
                context.getString(
                    R.string.progress_ring_format,
                    "%.1f".format(currentFlOz),
                    "%.1f".format(goalFlOz),
                    context.getString(R.string.oz_unit)
                )
            }
        }
    }

    // ==================== Weight (kg ↔ lbs) ====================

    /**
     * Convert an internal kg string value for display.
     * Returns the converted numeric string (e.g., "70" → "154.3" for lbs).
     */
    fun convertWeightForDisplay(valueKg: String, unit: WeightUnit): String {
        if (valueKg.isBlank()) return ""
        val kg = valueKg.toDoubleOrNull() ?: return valueKg
        return when (unit) {
            WeightUnit.METRIC -> valueKg
            WeightUnit.IMPERIAL -> {
                val lbs = kg * LBS_PER_KG
                if (lbs == lbs.toLong().toDouble()) lbs.toLong().toString()
                else "%.1f".format(lbs)
            }
        }
    }

    /**
     * Convert a user-entered weight display value back to internal kg string.
     */
    fun convertWeightToKg(displayValue: String, unit: WeightUnit): String {
        if (displayValue.isBlank()) return ""
        val value = displayValue.toDoubleOrNull() ?: return displayValue
        return when (unit) {
            WeightUnit.METRIC -> displayValue
            WeightUnit.IMPERIAL -> {
                val kg = value / LBS_PER_KG
                if (kg == kg.toLong().toDouble()) kg.toLong().toString()
                else "%.1f".format(kg)
            }
        }
    }

    /**
     * Get the weight unit abbreviation string.
     */
    fun weightUnit(context: Context, unit: WeightUnit): String {
        return when (unit) {
            WeightUnit.METRIC -> context.getString(R.string.kg_unit)
            WeightUnit.IMPERIAL -> context.getString(R.string.lbs_unit)
        }
    }

    // ==================== Height (cm ↔ ft′in″) ====================

    /**
     * Convert an internal cm string value for display.
     * For imperial, returns feet as a decimal (e.g., "170" → "67" inches for input field).
     * Use [formatHeightDisplay] for pretty ft′in″ display.
     */
    fun convertHeightForDisplay(valueCm: String, unit: HeightUnit): String {
        if (valueCm.isBlank()) return ""
        val cm = valueCm.toDoubleOrNull() ?: return valueCm
        return when (unit) {
            HeightUnit.METRIC -> valueCm
            HeightUnit.IMPERIAL -> {
                val totalInches = cm / CM_PER_INCH
                val feet = (totalInches / INCHES_PER_FOOT).toInt()
                val inches = (totalInches % INCHES_PER_FOOT).roundToInt()
                "${feet}'${inches}\""
            }
        }
    }

    /**
     * Format height for pretty display.
     * Examples: "170 cm", "5'7\""
     */
    fun formatHeightDisplay(context: Context, valueCm: String, unit: HeightUnit): String {
        if (valueCm.isBlank()) return ""
        val cm = valueCm.toDoubleOrNull() ?: return valueCm
        return when (unit) {
            HeightUnit.METRIC -> context.getString(R.string.value_with_unit, valueCm, context.getString(R.string.cm_unit))
            HeightUnit.IMPERIAL -> {
                val totalInches = cm / CM_PER_INCH
                val feet = (totalInches / INCHES_PER_FOOT).toInt()
                val inches = (totalInches % INCHES_PER_FOOT).roundToInt()
                "${feet}'${inches}\""
            }
        }
    }

    /**
     * Convert a user-entered height display value back to internal cm string.
     * Accepts formats: "170" (cm), "5'7" or "5'7\"" (ft/in), or just a number (inches in imperial).
     */

    fun convertHeightToCm(displayValue: String, unit: HeightUnit): String {
        if (displayValue.isBlank()) return ""
        return when (unit) {
            HeightUnit.METRIC -> displayValue
            HeightUnit.IMPERIAL -> {
                // Try to parse ft'in" format
                val ftInRegex = """(\d+)'(\d+)""".toRegex()
                val match = ftInRegex.find(displayValue)
                if (match != null) {
                    val feet = match.groupValues[1].toInt()
                    val inches = match.groupValues[2].toInt()
                    val totalCm = (feet * INCHES_PER_FOOT + inches) * CM_PER_INCH
                    "%.1f".format(totalCm)
                } else {
                    // Try as total inches
                    val inches = displayValue.toDoubleOrNull()
                    if (inches != null) {
                        val cm = inches * CM_PER_INCH
                        "%.1f".format(cm)
                    } else {
                        displayValue
                    }
                }
            }
        }
    }

    /**
     * Get the height unit abbreviation string.
     */
    fun heightUnit(context: Context, unit: HeightUnit): String {
        return when (unit) {
            HeightUnit.METRIC -> context.getString(R.string.cm_unit)
            HeightUnit.IMPERIAL -> context.getString(R.string.ft_unit)
        }
    }

    // ==================== Labels ====================

    fun volumeLabel(context: Context, unit: VolumeUnit): String {
        return when (unit) {
            VolumeUnit.METRIC -> context.getString(R.string.amount_ml_label)
            VolumeUnit.IMPERIAL -> context.getString(R.string.amount_oz_label)
        }
    }

    fun dailyGoalLabel(context: Context, unit: VolumeUnit): String {
        return when (unit) {
            VolumeUnit.METRIC -> context.getString(R.string.daily_goal_ml_label)
            VolumeUnit.IMPERIAL -> context.getString(R.string.daily_goal_oz_label)
        }
    }

    fun weightLabel(context: Context, unit: WeightUnit): String {
        return when (unit) {
            WeightUnit.METRIC -> context.getString(R.string.weight_kg_label)
            WeightUnit.IMPERIAL -> context.getString(R.string.weight_lbs_label)
        }
    }

    fun heightLabel(context: Context, unit: HeightUnit): String {
        return when (unit) {
            HeightUnit.METRIC -> context.getString(R.string.height_cm_label)
            HeightUnit.IMPERIAL -> context.getString(R.string.height_ft_in_label)
        }
    }

    // ==================== Validation Ranges ====================

    /**
     * Returns the valid weight range in the display unit.
     * Metric: 1–500 kg, Imperial: 2.2–1102.3 lbs
     */
    fun weightRange(unit: WeightUnit): Pair<Float, Float> {
        return when (unit) {
            WeightUnit.METRIC -> 1f to 500f
            WeightUnit.IMPERIAL -> 2.2f to 1102.3f
        }
    }

    /**
     * Returns the valid height range in the display unit.
     * Metric: 30–300 cm, Imperial: 12–118 inches
     */
    fun heightRange(unit: HeightUnit): Pair<Float, Float> {
        return when (unit) {
            HeightUnit.METRIC -> 30f to 300f
            HeightUnit.IMPERIAL -> 12f to 118f
        }
    }

    /**
     * Returns the valid daily goal range in the display unit.
     * Metric: 100–20000 ml, Imperial: 3–676 oz
     */
    fun goalRange(unit: VolumeUnit): Pair<Int, Int> {
        return when (unit) {
            VolumeUnit.METRIC -> 100 to 20000
            VolumeUnit.IMPERIAL -> 3 to 676
        }
    }

    /**
     * Default daily goal value in internal ml.
     */
    fun defaultDailyGoalMl(): Int = 3000

    /**
     * Format the default daily goal for display.
     * Examples: "3000ml", "101.4 oz"
     */
    fun formatDefaultDailyGoal(context: Context, unit: VolumeUnit): String {
        return when (unit) {
            VolumeUnit.METRIC -> context.getString(R.string.value_with_unit, "3000", context.getString(R.string.ml_unit))
            VolumeUnit.IMPERIAL -> {
                val defaultFlOz = 3000 / ML_PER_FL_OZ
                context.getString(R.string.value_with_unit, defaultFlOz.roundToInt().toString(), context.getString(R.string.oz_unit))
            }
        }
    }

    // ==================== Chart Helpers ====================

    /**
     * Generate Y-axis label for chart values.
     * Converts internal ml to display unit with compact formatting.
     */
    fun formatChartLabel(valueMl: Int, unit: VolumeUnit): String {
        return when (unit) {
            VolumeUnit.METRIC -> {
                if (valueMl >= 1000) {
                    val k = valueMl / 1000.0
                    if (k == k.toLong().toDouble()) "${k.toLong()}k"
                    else "${"%.1f".format(k)}k"
                } else {
                    valueMl.toString()
                }
            }
            VolumeUnit.IMPERIAL -> {
                val flOz = valueMl / ML_PER_FL_OZ
                if (flOz >= 100) {
                    "%.0f".format(flOz)
                } else if (flOz == flOz.toLong().toDouble()) {
                    flOz.toLong().toString()
                } else {
                    "%.1f".format(flOz)
                }
            }
        }
    }

    /**
     * Convert a chart data value from ml to the display unit (for Y-axis scaling).
     */
    fun convertChartValue(valueMl: Float, unit: VolumeUnit): Float {
        return when (unit) {
            VolumeUnit.METRIC -> valueMl
            VolumeUnit.IMPERIAL -> (valueMl / ML_PER_FL_OZ).toFloat()
        }
    }
}
