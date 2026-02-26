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
        private fun sanitize(key: String): String = key.replace(Regex("[^a-zA-Z0-9_]"), "_")

        fun weightKey(userId: String) = stringPreferencesKey(sanitize("${userId}_weight"))
        fun heightKey(userId: String) = stringPreferencesKey(sanitize("${userId}_height"))
        fun ageKey(userId: String) = stringPreferencesKey(sanitize("${userId}_age"))
        fun sexKey(userId: String) = stringPreferencesKey(sanitize("${userId}_sex"))
        fun activityKey(userId: String) = stringPreferencesKey(sanitize("${userId}_activity"))
        fun environmentKey(userId: String) = stringPreferencesKey(sanitize("${userId}_environment"))
        fun dailyGoalKey(userId: String) = intPreferencesKey(sanitize("${userId}_daily_goal"))
        fun setupCompleteKey(userId: String) = booleanPreferencesKey(sanitize("${userId}_setup_complete"))
        fun adminModeKey(userId: String) = booleanPreferencesKey(sanitize("${userId}_admin_mode"))
    }

    fun getUserRecord(userId: String): Flow<UserRecord> = context.dataStore.data.map { preferences ->
        UserRecord(
            uid = userId,
            weight = preferences[PreferencesKeys.weightKey(userId)] ?: "",
            height = preferences[PreferencesKeys.heightKey(userId)] ?: "",
            age = preferences[PreferencesKeys.ageKey(userId)] ?: "",
            sex = preferences[PreferencesKeys.sexKey(userId)] ?: "",
            activity = preferences[PreferencesKeys.activityKey(userId)] ?: "",
            environment = preferences[PreferencesKeys.environmentKey(userId)] ?: "",
            setupCompleted = preferences[PreferencesKeys.setupCompleteKey(userId)] ?: false
        )
    }

    suspend fun saveUserRecord(userId: String, record: UserRecord) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.weightKey(userId)] = record.weight
            preferences[PreferencesKeys.heightKey(userId)] = record.height
            preferences[PreferencesKeys.ageKey(userId)] = record.age
            preferences[PreferencesKeys.sexKey(userId)] = record.sex
            preferences[PreferencesKeys.activityKey(userId)] = record.activity
            preferences[PreferencesKeys.environmentKey(userId)] = record.environment
            preferences[PreferencesKeys.setupCompleteKey(userId)] = record.setupCompleted
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

    suspend fun setAdminMode(userId: String, isAdminMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.adminModeKey(userId)] = isAdminMode
        }
    }
}
