package com.example.fluidcheck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fluidcheck.repository.AdminRepository
import com.example.fluidcheck.repository.AuthRepository
import com.example.fluidcheck.repository.FluidLogRepository
import com.example.fluidcheck.repository.UserRepository
import com.example.fluidcheck.repository.UserPreferencesRepository

/**
 * A centralized factory to create all ViewModels in the app, injecting the necessary repositories.
 * This pattern avoids the need for a full Dependency Injection framework like Hilt/Dagger 
 * for this exact scope, whilst keeping the architecture clean.
 */
class AppViewModelFactory(
    private val userId: String = "",
    private val userRepository: UserRepository? = null,
    private val fluidLogRepository: FluidLogRepository? = null,
    private val adminRepository: AdminRepository? = null,
    private val authRepository: AuthRepository? = null,
    private val preferencesRepository: UserPreferencesRepository? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                requireNotNull(userRepository) { "UserRepository is required" }
                requireNotNull(preferencesRepository) { "UserPreferencesRepository is required" }
                require(userId.isNotEmpty()) { "UserId is required" }
                MainViewModel(userId, userRepository, preferencesRepository) as T
            }
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                requireNotNull(authRepository) { "AuthRepository is required" }
                AuthViewModel(authRepository) as T
            }
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                requireNotNull(fluidLogRepository) { "FluidLogRepository is required" }
                requireNotNull(userRepository) { "UserRepository is required" }
                require(userId.isNotEmpty()) { "UserId is required" }
                HomeViewModel(userId, fluidLogRepository, userRepository) as T
            }
            modelClass.isAssignableFrom(AdminViewModel::class.java) -> {
                requireNotNull(adminRepository) { "AdminRepository is required" }
                requireNotNull(userRepository) { "UserRepository is required" }
                AdminViewModel(adminRepository, userRepository) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                requireNotNull(userRepository) { "UserRepository is required" }
                requireNotNull(authRepository) { "AuthRepository is required" }
                requireNotNull(preferencesRepository) { "UserPreferencesRepository is required" }
                require(userId.isNotEmpty()) { "UserId is required" }
                ProfileViewModel(userId, userRepository, authRepository, preferencesRepository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                requireNotNull(userRepository) { "UserRepository is required" }
                requireNotNull(preferencesRepository) { "UserPreferencesRepository is required" }
                require(userId.isNotEmpty()) { "UserId is required" }
                SettingsViewModel(userId, userRepository, preferencesRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
