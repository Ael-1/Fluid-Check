package com.example.fluidcheck.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.fluidcheck.model.UserRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {
    
    private object PreferencesKeys {
        // Sanitize keys to ensure they only contain alphanumeric characters and underscores
        fun sanitize(key: String): String = key.replace(Regex("[^a-zA-Z0-9_]"), "_")

        fun usernameKey(userId: String) = stringPreferencesKey(sanitize("${userId}_username"))
        fun emailKey(userId: String) = stringPreferencesKey(sanitize("${userId}_email"))
        fun weightKey(userId: String) = stringPreferencesKey(sanitize("${userId}_weight"))
        fun heightKey(userId: String) = stringPreferencesKey(sanitize("${userId}_height"))
        fun ageKey(userId: String) = stringPreferencesKey(sanitize("${userId}_age"))
        fun sexKey(userId: String) = stringPreferencesKey(sanitize("${userId}_sex"))
        fun activityKey(userId: String) = stringPreferencesKey(sanitize("${userId}_activity"))
        fun environmentKey(userId: String) = stringPreferencesKey(sanitize("${userId}_environment"))
        fun dailyGoalKey(userId: String) = intPreferencesKey(sanitize("${userId}_daily_goal"))
        fun setupCompleteKey(userId: String) = booleanPreferencesKey(sanitize("${userId}_setup_complete"))
        fun adminModeKey(userId: String) = booleanPreferencesKey(sanitize("${userId}_admin_mode"))
        fun notificationsEnabledKey(userId: String) = booleanPreferencesKey(sanitize("${userId}_notifications_enabled"))
        fun reminderFrequencyKey(userId: String) = stringPreferencesKey(sanitize("${userId}_reminder_frequency"))
        fun pendingPhotoUploadKey(userId: String) = booleanPreferencesKey(sanitize("${userId}_pending_photo_upload"))
        fun roleKey(userId: String) = stringPreferencesKey(sanitize("${userId}_role"))
    }

    fun getUserRecord(userId: String): Flow<UserRecord> = context.dataStore.data.map { preferences ->
        UserRecord(
            uid = userId,
            username = preferences[PreferencesKeys.usernameKey(userId)] ?: "",
            email = preferences[PreferencesKeys.emailKey(userId)] ?: "",
            weight = preferences[PreferencesKeys.weightKey(userId)] ?: "",
            height = preferences[PreferencesKeys.heightKey(userId)] ?: "",
            age = preferences[PreferencesKeys.ageKey(userId)] ?: "",
            sex = preferences[PreferencesKeys.sexKey(userId)] ?: "",
            activity = preferences[PreferencesKeys.activityKey(userId)] ?: "",
            environment = preferences[PreferencesKeys.environmentKey(userId)] ?: "",
            setupCompleted = preferences[PreferencesKeys.setupCompleteKey(userId)] ?: false,
            notificationsEnabled = preferences[PreferencesKeys.notificationsEnabledKey(userId)],
            reminderFrequency = preferences[PreferencesKeys.reminderFrequencyKey(userId)] ?: "60",
            role = preferences[PreferencesKeys.roleKey(userId)] ?: "USER"
        )
    }

    suspend fun saveUserRecord(userId: String, record: UserRecord) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.usernameKey(userId)] = record.username
            preferences[PreferencesKeys.emailKey(userId)] = record.email
            preferences[PreferencesKeys.weightKey(userId)] = record.weight
            preferences[PreferencesKeys.heightKey(userId)] = record.height
            preferences[PreferencesKeys.ageKey(userId)] = record.age
            preferences[PreferencesKeys.sexKey(userId)] = record.sex
            preferences[PreferencesKeys.activityKey(userId)] = record.activity
            preferences[PreferencesKeys.environmentKey(userId)] = record.environment
            preferences[PreferencesKeys.setupCompleteKey(userId)] = record.setupCompleted
            if (record.notificationsEnabled != null) {
                preferences[PreferencesKeys.notificationsEnabledKey(userId)] = record.notificationsEnabled
            } else {
                preferences.remove(PreferencesKeys.notificationsEnabledKey(userId))
            }
            preferences[PreferencesKeys.reminderFrequencyKey(userId)] = record.reminderFrequency
            preferences[PreferencesKeys.roleKey(userId)] = record.role
        }
    }

    fun isSetupComplete(userId: String): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.setupCompleteKey(userId)] ?: false
    }

    suspend fun setSetupComplete(userId: String, complete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.setupCompleteKey(userId)] = complete
        }
    }

    fun getDailyGoal(userId: String): Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.dailyGoalKey(userId)] ?: 3000
    }

    suspend fun saveDailyGoal(userId: String, goal: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.dailyGoalKey(userId)] = goal
        }
    }

    fun isAdminMode(userId: String, defaultValue: Boolean): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.adminModeKey(userId)] ?: defaultValue
    }

    fun getAdminModeFlow(userId: String): Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.adminModeKey(userId)]
    }

    suspend fun setAdminMode(userId: String, isAdminMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.adminModeKey(userId)] = isAdminMode
        }
    }

    fun getNotificationsEnabled(userId: String): Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.notificationsEnabledKey(userId)]
    }

    suspend fun setNotificationsEnabled(userId: String, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.notificationsEnabledKey(userId)] = enabled
        }
    }

    fun getReminderFrequency(userId: String): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.reminderFrequencyKey(userId)] ?: "60"
    }

    suspend fun setReminderFrequency(userId: String, frequency: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.reminderFrequencyKey(userId)] = frequency
        }
    }

    fun hasPendingPhotoUpload(userId: String): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.pendingPhotoUploadKey(userId)] ?: false
    }

    suspend fun setPendingPhotoUpload(userId: String, pending: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.pendingPhotoUploadKey(userId)] = pending
        }
    }

    fun getStoredRole(userId: String): Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.roleKey(userId)] ?: "USER"
    }

    suspend fun saveStoredRole(userId: String, role: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.roleKey(userId)] = role
        }
    }
}
