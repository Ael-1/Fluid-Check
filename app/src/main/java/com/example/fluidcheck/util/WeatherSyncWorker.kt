package com.example.fluidcheck.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fluidcheck.BuildConfig
import com.example.fluidcheck.ai.GeminiCoach
import com.example.fluidcheck.repository.FirestoreRepository
import com.example.fluidcheck.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Periodically fetches local temperature and humidity using Open-Meteo,
 * recalculating hydration goals via Gemini when significant changes occur.
 */
class WeatherSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val prefsRepository = UserPreferencesRepository(applicationContext)
        val firestoreRepository = FirestoreRepository(applicationContext)
        val userId = inputData.getString("user_id") ?: "GUEST"

        val dynamicWeatherEnabled = prefsRepository.isWeatherGoalAdjustmentEnabled(userId).first()
        val locationAccessEnabled = prefsRepository.isLocationAccessEnabled(userId).first()

        if (!dynamicWeatherEnabled || !locationAccessEnabled) {
            Log.d("WeatherSyncWorker", "Weather sync skipped: dynamic adjustment or location setting disabled.")
            return Result.success()
        }

        // Check runtime location permission
        val hasCoarse = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCoarse && !hasFine) {
            Log.d("WeatherSyncWorker", "Weather sync failed: Location permission not granted at runtime.")
            return Result.failure()
        }

        // Retrieve current location
        val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var bestLocation: Location? = null
        try {
            val providers = locationManager.getProviders(true)
            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                    bestLocation = loc
                }
            }
        } catch (e: SecurityException) {
            Log.e("WeatherSyncWorker", "Security exception getting last known location", e)
            return Result.failure()
        }

        val location = bestLocation
        if (location == null) {
            Log.e("WeatherSyncWorker", "Could not retrieve location, location was null.")
            return Result.success() // Succeed without adjustment if location is unavailable
        }

        val lat = location.latitude
        val lon = location.longitude

        // Fetch weather from Open-Meteo API
        var temp: Double? = null
        var humidity: Double? = null
        try {
            val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            conn.connect()

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val current = json.getJSONObject("current")
                temp = current.getDouble("temperature_2m")
                humidity = current.getDouble("relative_humidity_2m")
            } else {
                Log.e("WeatherSyncWorker", "API call failed with response code ${conn.responseCode}")
                return Result.retry()
            }
        } catch (e: Exception) {
            Log.e("WeatherSyncWorker", "Network loss or timeout while fetching weather data", e)
            return Result.retry()
        }

        if (temp == null) {
            return Result.success()
        }

        Log.d("WeatherSyncWorker", "Fetched weather: temp=${temp}°C, humidity=${humidity}%")

        if (temp > 30.0) {
            val userRecord = firestoreRepository.getUserRecord(userId) ?: return Result.success()
            val currentGoal = userRecord.dailyGoal ?: 3000

            // Trigger Gemini recalculation
            val apiKey = BuildConfig.GEMINI_API_KEY
            val coach = GeminiCoach(apiKey)
            val envPrompt = if (humidity != null && humidity > 60.0) "Hot and Humid" else "Hot"
            
            val newGoalStr = coach.calculateHydrationGoal(
                weight = userRecord.weight.ifEmpty { "70" },
                height = userRecord.height.ifEmpty { "175" },
                age = userRecord.age.ifEmpty { "25" },
                sex = userRecord.sex.ifEmpty { "Male" },
                activity = userRecord.activity.ifEmpty { "Moderate" },
                environment = envPrompt
            )

            val parsedGoal = newGoalStr?.replace(Regex("[^0-9]"), "")?.toIntOrNull()
            // Adjust goal by at least +500ml if temp is high
            val adjustedGoal = if (parsedGoal != null && parsedGoal > currentGoal) {
                parsedGoal
            } else {
                currentGoal + 500
            }

            // Save new goal
            firestoreRepository.saveDailyGoal(userId, adjustedGoal)
            prefsRepository.saveDailyGoal(userId, adjustedGoal)

            val displayTemp = temp.toInt()
            val adjustmentAmount = adjustedGoal - currentGoal
            NotificationHelper.showSmartReminder(
                applicationContext,
                "Daily Goal Adjusted! ☀️",
                "Daily goal adjusted by +${adjustmentAmount}ml today due to high local heat (${displayTemp}°C)."
            )
        }

        return Result.success()
    }
}
