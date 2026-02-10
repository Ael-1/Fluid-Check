package com.example.fluidcheck.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.fluidcheck.model.UserRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {
    
    private object PreferencesKeys {
        fun weightKey(username: String) = stringPreferencesKey("${username}_weight")
        fun heightKey(username: String) = stringPreferencesKey("${username}_height")
        fun ageKey(username: String) = stringPreferencesKey("${username}_age")
        fun sexKey(username: String) = stringPreferencesKey("${username}_sex")
        fun activityKey(username: String) = stringPreferencesKey("${username}_activity")
        fun environmentKey(username: String) = stringPreferencesKey("${username}_environment")
        fun dailyGoalKey(username: String) = intPreferencesKey("${username}_daily_goal")
        fun setupCompleteKey(username: String) = booleanPreferencesKey("${username}_setup_complete")
    }

    fun getUserRecord(username: String): Flow<UserRecord> = context.dataStore.data.map { preferences ->
        UserRecord(
            weight = preferences[PreferencesKeys.weightKey(username)] ?: "",
            height = preferences[PreferencesKeys.heightKey(username)] ?: "",
            age = preferences[PreferencesKeys.ageKey(username)] ?: "",
            sex = preferences[PreferencesKeys.sexKey(username)] ?: "",
            activity = preferences[PreferencesKeys.activityKey(username)] ?: "",
            environment = preferences[PreferencesKeys.environmentKey(username)] ?: ""
        )
    }

    suspend fun saveUserRecord(username: String, record: UserRecord) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.weightKey(username)] = record.weight
            preferences[PreferencesKeys.heightKey(username)] = record.height
            preferences[PreferencesKeys.ageKey(username)] = record.age
            preferences[PreferencesKeys.sexKey(username)] = record.sex
            preferences[PreferencesKeys.activityKey(username)] = record.activity
            preferences[PreferencesKeys.environmentKey(username)] = record.environment
            // If any record is saved, we consider setup as complete (even if skipped with empty record)
            preferences[PreferencesKeys.setupCompleteKey(username)] = true
        }
    }

    fun isSetupComplete(username: String): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.setupCompleteKey(username)] ?: false
    }

    suspend fun setSetupComplete(username: String, complete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.setupCompleteKey(username)] = complete
        }
    }

    fun getDailyGoal(username: String): Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.dailyGoalKey(username)] ?: 3000
    }

    suspend fun saveDailyGoal(username: String, goal: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.dailyGoalKey(username)] = goal
        }
    }
}
