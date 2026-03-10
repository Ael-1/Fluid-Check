package com.example.fluidcheck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluidcheck.model.FluidLog
import com.example.fluidcheck.repository.FluidLogRepository
import com.example.fluidcheck.repository.UserRepository
import com.example.fluidcheck.util.DataResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date

class HomeViewModel(
    private val userId: String,
    private val fluidLogRepository: FluidLogRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _logs = MutableStateFlow<List<FluidLog>>(emptyList())
    val logs: StateFlow<List<FluidLog>> = _logs.asStateFlow()

    private val _totalIntake = MutableStateFlow(0)
    val totalIntake: StateFlow<Int> = _totalIntake.asStateFlow()

    private val _toastMessage = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val toastMessage: kotlinx.coroutines.flow.SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        loadTodayLogs()
        evaluateStreak()
    }

    private fun loadTodayLogs() {
        if (userId.isEmpty()) return
        viewModelScope.launch {
            fluidLogRepository.getTodayFluidLogsFlow(userId).collectLatest { result ->
                if (result is DataResult.Success) {
                    val logsList = result.data
                    _logs.value = logsList
                    _totalIntake.value = logsList.sumOf { it.amount }
                }
            }
        }
    }

    private fun evaluateStreak() {
        if (userId.isEmpty()) return
        viewModelScope.launch {
            userRepository.evaluateStreak(userId)
        }
    }

    fun addFluidLog(amount: Int, type: String) {
        val newLog = FluidLog(
            id = Date().time,
            type = type,
            amount = amount,
            date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { 
                timeZone = java.util.TimeZone.getTimeZone("GMT+8") 
            }.format(Date()),
            time = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US).apply { 
                timeZone = java.util.TimeZone.getTimeZone("GMT+8") 
            }.format(Date())
        )
        viewModelScope.launch {
            val result = fluidLogRepository.saveFluidLog(userId, newLog)
            if (result.isSuccess) {
                _toastMessage.emit("Logged $amount ml of $type")
            } else {
                _toastMessage.emit("Failed to log fluid")
            }
        }
    }

    fun saveDailyGoal(newGoal: Int) {
        if (userId.isEmpty()) return
        viewModelScope.launch {
            val result = userRepository.saveDailyGoal(userId, newGoal)
            if (result.isError) {
                _toastMessage.emit("Error saving goal to cloud")
            }
        }
    }

    fun updateQuickAddConfig(configs: List<com.example.fluidcheck.model.QuickAddConfig>) {
        if (userId.isEmpty()) return
        viewModelScope.launch {
            val result = userRepository.updateQuickAddConfig(userId, configs)
            if (result.isError) {
                _toastMessage.emit("Error updating Quick Add settings")
            }
        }
    }

    fun updateFluidLog(oldLog: FluidLog, newLog: FluidLog) {
        viewModelScope.launch {
            fluidLogRepository.updateFluidLog(userId, oldLog, newLog)
        }
    }

    fun deleteFluidLog(log: FluidLog) {
        viewModelScope.launch {
            fluidLogRepository.deleteFluidLog(userId, log)
        }
    }

    fun markGoalAchieved(todayDate: String, yesterdayDate: String) {
        viewModelScope.launch {
            userRepository.markGoalAchievedToday(userId, todayDate, yesterdayDate)
        }
    }
}
