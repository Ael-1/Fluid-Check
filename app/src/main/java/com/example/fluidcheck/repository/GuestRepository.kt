package com.example.fluidcheck.repository

import android.content.Context
import com.example.fluidcheck.model.FluidLog
import com.example.fluidcheck.model.QuickAddConfig
import com.example.fluidcheck.model.UserRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class GuestRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("GUEST_PREFS", Context.MODE_PRIVATE)
    private val guestLogsProviderName = "guest_fluid_logs"
    private val guestRecordProviderName = "guest_user_record"

    private val _guestLogsFlow = MutableStateFlow<List<FluidLog>>(emptyList())
    val guestLogsFlow: Flow<List<FluidLog>> = _guestLogsFlow

    private val _guestUserRecordFlow = MutableStateFlow<UserRecord?>(
        UserRecord(
            uid = "GUEST",
            username = "Guest",
            email = "",
            role = "USER",
            setupCompleted = false
        )
    )
    val guestUserRecordFlow: Flow<UserRecord?> = _guestUserRecordFlow
    val guestUserRecord: UserRecord? get() = _guestUserRecordFlow.value

    init {
        loadGuestData()
    }

    private fun loadGuestData() {
        try {
            // Load Record
            val recordJsonStr = prefs.getString(guestRecordProviderName, null)
            if (recordJsonStr != null) {
                val json = JSONObject(recordJsonStr)
                _guestUserRecordFlow.value = UserRecord(
                    uid = "GUEST",
                    username = json.optString("username", "Guest"),
                    email = "",
                    role = "USER",
                    dailyGoal = if (json.has("dailyGoal")) json.optInt("dailyGoal", 3000) else 3000,
                    streak = json.optInt("streak", 0),
                    highestStreak = json.optInt("highestStreak", 0),
                    totalFluidDrankAllTime = json.optInt("totalFluidDrankAllTime", 0),
                    totalRingsClosed = json.optInt("totalRingsClosed", 0),
                    lastRingClosedDate = json.optString("lastRingClosedDate", ""),
                    weight = json.optString("weight", ""),
                    height = json.optString("height", ""),
                    age = json.optString("age", ""),
                    sex = json.optString("sex", ""),
                    activity = json.optString("activity", ""),
                    environment = json.optString("environment", ""),
                    setupCompleted = json.optBoolean("setupCompleted", false),
                    quickAddConfig = parseQuickAddConfig(json.optJSONArray("quickAddConfig")) ?: com.example.fluidcheck.model.DEFAULT_QUICK_ADD_CONFIGS
                )
            }

            // Load Logs
            val logsJsonStr = prefs.getString(guestLogsProviderName, null)
            if (logsJsonStr != null) {
                val jsonArray = JSONArray(logsJsonStr)
                val loadedLogs = mutableListOf<FluidLog>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    loadedLogs.add(
                        FluidLog(
                            id = item.optLong("id", 0),
                            type = item.optString("type", "Water"),
                            time = item.optString("time", ""),
                            amount = item.optInt("amount", 0),
                            date = item.optString("date", "")
                        )
                    )
                }
                _guestLogsFlow.value = loadedLogs
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseQuickAddConfig(array: JSONArray?): List<QuickAddConfig>? {
        if (array == null) return null
        val list = mutableListOf<QuickAddConfig>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(QuickAddConfig(amount = obj.optInt("amount", 0), type = obj.optString("type", "Water")))
        }
        return list
    }

    fun saveUserRecord(record: UserRecord) {
        _guestUserRecordFlow.value = record
        saveGuestRecordToPrefs()
    }

    private fun saveGuestRecordToPrefs() {
        try {
            val record = _guestUserRecordFlow.value ?: return
            val json = JSONObject()
            json.put("username", record.username)
            record.dailyGoal?.let { json.put("dailyGoal", it) }
            json.put("streak", record.streak)
            json.put("highestStreak", record.highestStreak)
            json.put("totalFluidDrankAllTime", record.totalFluidDrankAllTime)
            json.put("totalRingsClosed", record.totalRingsClosed)
            json.put("lastRingClosedDate", record.lastRingClosedDate)
            json.put("weight", record.weight)
            json.put("height", record.height)
            json.put("age", record.age)
            json.put("sex", record.sex)
            json.put("activity", record.activity)
            json.put("environment", record.environment)
            json.put("setupCompleted", record.setupCompleted)
            
            if (record.quickAddConfig != null) {
                val qaArray = JSONArray()
                record.quickAddConfig.forEach { config ->
                    val qaObj = JSONObject()
                    qaObj.put("amount", config.amount)
                    qaObj.put("type", config.type)
                    qaArray.put(qaObj)
                }
                json.put("quickAddConfig", qaArray)
            }
            
            prefs.edit().putString(guestRecordProviderName, json.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveFluidLog(log: FluidLog) {
        val currentLogs = _guestLogsFlow.value.toMutableList()
        val newLog = log.copy(id = System.currentTimeMillis())
        currentLogs.add(newLog)
        _guestLogsFlow.value = currentLogs
        
        val currentRecord = _guestUserRecordFlow.value
        if (currentRecord != null) {
            _guestUserRecordFlow.value = currentRecord.copy(totalFluidDrankAllTime = currentRecord.totalFluidDrankAllTime + newLog.amount)
        }
        saveGuestLogsToPrefs()
        saveGuestRecordToPrefs()
    }

    fun updateFluidLog(oldLog: FluidLog, newLog: FluidLog) {
        val currentLogs = _guestLogsFlow.value.toMutableList()
        val index = currentLogs.indexOfFirst { it.id == oldLog.id }
        if (index != -1) {
            currentLogs[index] = newLog
            _guestLogsFlow.value = currentLogs
            val currentRecord = _guestUserRecordFlow.value
            if (currentRecord != null) {
                val amountDiff = newLog.amount - oldLog.amount
                _guestUserRecordFlow.value = currentRecord.copy(totalFluidDrankAllTime = currentRecord.totalFluidDrankAllTime + amountDiff)
            }
            saveGuestLogsToPrefs()
            saveGuestRecordToPrefs()
        }
    }

    fun deleteFluidLog(log: FluidLog) {
        val currentLogs = _guestLogsFlow.value.toMutableList()
        val index = currentLogs.indexOfFirst { it.id == log.id }
        if (index != -1) {
            currentLogs.removeAt(index)
            _guestLogsFlow.value = currentLogs
            val currentRecord = _guestUserRecordFlow.value
            if (currentRecord != null) {
                _guestUserRecordFlow.value = currentRecord.copy(totalFluidDrankAllTime = (currentRecord.totalFluidDrankAllTime - log.amount).coerceAtLeast(0))
            }
            saveGuestLogsToPrefs()
            saveGuestRecordToPrefs()
        }
    }

    private fun saveGuestLogsToPrefs() {
        try {
            val logs = _guestLogsFlow.value
            val jsonArray = JSONArray()
            logs.forEach { log ->
                val obj = JSONObject()
                obj.put("id", log.id)
                obj.put("type", log.type)
                obj.put("time", log.time)
                obj.put("amount", log.amount)
                obj.put("date", log.date)
                jsonArray.put(obj)
            }
            prefs.edit().putString(guestLogsProviderName, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveDailyGoal(goal: Int?) {
        val record = _guestUserRecordFlow.value
        if (record != null) {
            _guestUserRecordFlow.value = record.copy(dailyGoal = goal)
            saveGuestRecordToPrefs()
        }
    }

    fun updateQuickAddConfig(config: List<QuickAddConfig>) {
        val currentRecord = _guestUserRecordFlow.value
        if (currentRecord != null) {
            _guestUserRecordFlow.value = currentRecord.copy(quickAddConfig = config)
            saveGuestRecordToPrefs()
        }
    }

    fun markGoalAchievedToday(todayDate: String, yesterdayDate: String): Boolean {
        val currentRecord = _guestUserRecordFlow.value ?: return false
        if (currentRecord.lastRingClosedDate == todayDate) {
            return false // Already tracked for today
        }
        
        val newStreak = if (currentRecord.lastRingClosedDate == yesterdayDate) {
            currentRecord.streak + 1
        } else {
            1
        }
        
        val highestStreak = currentRecord.highestStreak
        _guestUserRecordFlow.value = currentRecord.copy(
            streak = newStreak,
            lastRingClosedDate = todayDate,
            highestStreak = if (newStreak > highestStreak) newStreak else highestStreak,
            totalRingsClosed = currentRecord.totalRingsClosed + 1
        )
        saveGuestRecordToPrefs()
        return true
    }

    fun getTodayFluidLogsFlow(): Flow<List<FluidLog>> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())
        return _guestLogsFlow.map { logs -> logs.filter { it.date == today }.sortedBy { it.id } }
    }

    fun getFluidLogs(): List<FluidLog> = _guestLogsFlow.value

    fun clearGuestData() {
        prefs.edit().clear().apply()
        _guestLogsFlow.value = emptyList()
        _guestUserRecordFlow.value = UserRecord(
            uid = "GUEST",
            username = "Guest",
            email = "",
            role = "USER",
            setupCompleted = false
        )
    }
}
