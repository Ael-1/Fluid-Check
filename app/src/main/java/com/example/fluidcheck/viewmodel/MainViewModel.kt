package com.example.fluidcheck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluidcheck.model.UserRecord
import com.example.fluidcheck.repository.UserPreferencesRepository
import com.example.fluidcheck.repository.UserRepository
import com.example.fluidcheck.util.DataResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(
    private val userId: String,
    private val userRepository: UserRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _userRecord = MutableStateFlow<UserRecord?>(null)
    val userRecord: StateFlow<UserRecord?> = _userRecord.asStateFlow()

    private val _isAdminMode = MutableStateFlow<Boolean?>(null)
    val isAdminMode: StateFlow<Boolean?> = _isAdminMode.asStateFlow()

    private val _hasPendingWrites = MutableStateFlow(true)
    val hasPendingWrites: StateFlow<Boolean> = _hasPendingWrites.asStateFlow()

    private val _roleChangeDetected = MutableStateFlow<Pair<String, String>?>(null)
    val roleChangeDetected: StateFlow<Pair<String, String>?> = _roleChangeDetected.asStateFlow()

    val isLoading: Boolean
        get() = _userRecord.value == null || _isAdminMode.value == null || userId.isEmpty()

    init {
        loadUserRecord()
        loadAdminMode()
        listenToPendingWrites()
    }

    private fun loadUserRecord() {
        viewModelScope.launch {
            userRepository.getUserRecordFlow(userId).collectLatest { record ->
                _userRecord.value = record
                
                // Track role changes
                if (record != null) {
                    val roleRes = preferencesRepository.getStoredRole(userId).first()
                    val lastStoredRole = (roleRes as? DataResult.Success)?.data ?: ""
                    val userRole = record.role
                    if (lastStoredRole.isNotEmpty() && lastStoredRole != userRole) {
                        _roleChangeDetected.value = Pair(lastStoredRole, userRole)
                        
                        val isPromoted = (userRole == "ADMIN" || userRole == "MODERATOR")
                        if (!isPromoted && _isAdminMode.value == true) {
                            setAdminMode(false)
                        }
                    }
                    preferencesRepository.saveStoredRole(userId, userRole)
                }
            }
        }
    }

    private fun loadAdminMode() {
        viewModelScope.launch {
            preferencesRepository.getAdminModeFlow(userId).collectLatest { res ->
                val prefValue = (res as? DataResult.Success)?.data
                
                // Need current user record to determine true fallback
                val currentRecord = _userRecord.value
                val userRole = currentRecord?.role ?: "USER"
                val isPrimaryAdmin = userRole == "ADMIN"
                val isDatabaseAdmin = userRole == "ADMIN" || userRole == "MODERATOR"
                
                _isAdminMode.value = if (isPrimaryAdmin) true else prefValue ?: isDatabaseAdmin
            }
        }
    }

    private fun listenToPendingWrites() {
        viewModelScope.launch {
            userRepository.hasPendingWritesFlow(userId).collectLatest {
                _hasPendingWrites.value = it
            }
        }
    }

    fun setAdminMode(isAdmin: Boolean) {
        _isAdminMode.value = isAdmin
        viewModelScope.launch {
            try {
                preferencesRepository.setAdminMode(userId, isAdmin)
            } catch (e: Exception) {
                // Ignore local pref error
            }
        }
    }

    fun clearRoleChangeNotification() {
        _roleChangeDetected.value = null
    }

    fun syncPendingProfilePhoto(isConnected: Boolean) {
        viewModelScope.launch {
            val res = preferencesRepository.hasPendingPhotoUpload(userId).first()
            val hasPending = (res as? DataResult.Success)?.data ?: false
            if (isConnected && hasPending) {
                val result = userRepository.syncPendingProfilePhoto(userId)
                if (result is DataResult.Success) {
                    preferencesRepository.setPendingPhotoUpload(userId, false)
                }
            }
        }
    }

    fun syncNotificationsState(hasPermission: Boolean) {
        viewModelScope.launch {
            val record = _userRecord.value ?: return@launch
            val enabled = record.notificationsEnabled ?: hasPermission
            val frequency = record.reminderFrequency
            // System-level worker scheduling will happen in the UI layer (MainScreen) 
            // since it depends on Context, but we sync local preferences here.
            
            if (enabled && hasPermission) {
                try {
                    preferencesRepository.setNotificationsEnabled(userId, true)
                    preferencesRepository.setReminderFrequency(userId, frequency)
                } catch (e: Exception) {}
            } else if (!enabled) {
                try {
                    preferencesRepository.setNotificationsEnabled(userId, false)
                } catch (e: Exception) {}
            }
        }
    }

    fun executeStreakCatchup(currentTodayDate: String) {
        viewModelScope.launch {
            val record = _userRecord.value ?: return@launch
            val lastDate = record.lastRingClosedDate
            val isAdmin = _isAdminMode.value ?: false
            
            if (lastDate.isNotEmpty() && !isAdmin) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("GMT+8") }
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"))
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val yesterday = sdf.format(calendar.time)
                
                if (lastDate != currentTodayDate && lastDate != yesterday) {
                    // Missed more than 1 day, reset streak
                    userRepository.resetStreak(userId)
                }
            }
        }
    }
}
