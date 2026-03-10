package com.example.fluidcheck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluidcheck.repository.UserPreferencesRepository
import com.example.fluidcheck.repository.UserRepository
import com.example.fluidcheck.util.DataResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userId: String,
    private val userRepository: UserRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _reminderFrequency = MutableStateFlow("60")
    val reminderFrequency: StateFlow<String> = _reminderFrequency.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val res = userRepository.getUserRecord(userId)
            val record = (res as? com.example.fluidcheck.util.DataResult.Success)?.data
            if (record != null) {
                _notificationsEnabled.value = record.notificationsEnabled ?: false
                _reminderFrequency.value = record.reminderFrequency
            }
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean): DataResult<Unit> {
        val result = userRepository.updateNotificationsEnabled(userId, enabled)
        if (result is DataResult.Success) {
            _notificationsEnabled.value = enabled
            preferencesRepository.setNotificationsEnabled(userId, enabled)
        }
        return result
    }

    suspend fun updateReminderFrequency(frequency: String): DataResult<Unit> {
        val result = userRepository.updateReminderFrequency(userId, frequency)
        if (result is DataResult.Success) {
            _reminderFrequency.value = frequency
            preferencesRepository.setReminderFrequency(userId, frequency)
        }
        return result
    }
}
