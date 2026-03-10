package com.example.fluidcheck.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluidcheck.model.UserRecord
import com.example.fluidcheck.repository.AuthRepository
import com.example.fluidcheck.repository.UserPreferencesRepository
import com.example.fluidcheck.repository.UserRepository
import com.example.fluidcheck.util.DataResult
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userId: String,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    fun queueOfflineProfileUpdate(userRecord: UserRecord) {
        viewModelScope.launch {
            userRepository.queueOfflineUserRecordUpdate(userId, userRecord)
        }
    }
    
    suspend fun updateUsername(oldUsername: String, newUsername: String): DataResult<Unit> {
        return userRepository.updateUsername(userId, oldUsername, newUsername)
    }

    suspend fun verifyBeforeUpdateEmail(newEmail: String): DataResult<Unit> {
        return authRepository.verifyBeforeUpdateEmail(newEmail)
    }
    
    suspend fun reauthenticate(password: String): DataResult<Unit> {
        return authRepository.reauthenticate(password)
    }

    suspend fun updatePassword(newPassword: String): DataResult<Unit> {
        return authRepository.updatePassword(newPassword)
    }

    suspend fun sendEmailVerification(): DataResult<Unit> {
        return authRepository.sendEmailVerification()
    }

    fun removeProfilePicture() {
        viewModelScope.launch {
            userRepository.removeProfilePicture(userId)
            val res = userRepository.getUserRecord(userId)
            val currentRecord = (res as? com.example.fluidcheck.util.DataResult.Success)?.data
            currentRecord?.let {
                userRepository.queueOfflineUserRecordUpdate(userId, it.copy(profilePictureUrl = ""))
            }
        }
    }

    suspend fun uploadProfilePicture(uri: Uri, isOnline: Boolean): DataResult<String> {
        val result = userRepository.uploadProfilePicture(userId, uri, isOnline)
        if (result is DataResult.Success && !isOnline) {
            // Flag that an upload is pending when connection returns
            preferencesRepository.setPendingPhotoUpload(userId, true)
        }
        return result
    }
    
    suspend fun saveDailyGoal(goal: Int?): DataResult<Unit> {
        return userRepository.saveDailyGoal(userId, goal)
    }
}
