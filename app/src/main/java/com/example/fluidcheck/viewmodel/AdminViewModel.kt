package com.example.fluidcheck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluidcheck.model.UserRecord
import com.example.fluidcheck.repository.AdminRepository
import com.example.fluidcheck.repository.UserRepository
import com.example.fluidcheck.util.DataResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _usersList = MutableStateFlow<List<UserRecord>>(emptyList())
    val usersList: StateFlow<List<UserRecord>> = _usersList.asStateFlow()

    private val _filteredUsers = MutableStateFlow<List<UserRecord>>(emptyList())
    val filteredUsers: StateFlow<List<UserRecord>> = _filteredUsers.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Sort states: 0 = UserID, 1 = Username, 2 = Default Goal, 3 = Role
    private val _currentSortBasis = MutableStateFlow(0)
    val currentSortBasis: StateFlow<Int> = _currentSortBasis.asStateFlow()
    
    private val _isAscending = MutableStateFlow(true)
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    init {
        loadAllUsers()
    }

    private fun loadAllUsers() {
        viewModelScope.launch {
            adminRepository.getAllUsersFlow().collectLatest { result ->
                if (result is DataResult.Success) {
                    _usersList.value = result.data
                    applyFiltersAndSorting()
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        applyFiltersAndSorting()
    }

    fun onSortChanged(basis: Int, ascending: Boolean) {
        _currentSortBasis.value = basis
        _isAscending.value = ascending
        applyFiltersAndSorting()
    }

    private fun applyFiltersAndSorting() {
        val query = _searchQuery.value.trim().lowercase()
        val allUsers = _usersList.value

        var newList = if (query.isEmpty()) {
            allUsers
        } else {
            allUsers.filter { user ->
                user.uid.lowercase().contains(query) ||
                user.username.lowercase().contains(query)
            }
        }

        newList = when (_currentSortBasis.value) {
            0 -> if (_isAscending.value) newList.sortedBy { it.uid } else newList.sortedByDescending { it.uid }
            1 -> if (_isAscending.value) newList.sortedBy { it.username } else newList.sortedByDescending { it.username }
            2 -> if (_isAscending.value) newList.sortedBy { it.dailyGoal ?: 0 } else newList.sortedByDescending { it.dailyGoal ?: 0 }
            3 -> if (_isAscending.value) newList.sortedBy { it.role } else newList.sortedByDescending { it.role }
            else -> newList
        }

        _filteredUsers.value = newList
    }

    fun updateUserRole(uid: String, currentRole: String) {
        viewModelScope.launch {
            val nextRole = when (currentRole) {
                "USER" -> "MODERATOR"
                "MODERATOR" -> "ADMIN"
                else -> "USER"
            }
            adminRepository.updateRole(uid, nextRole)
        }
    }

    fun removeAdminRole(uid: String) {
        viewModelScope.launch {
            adminRepository.updateRole(uid, "USER")
        }
    }

    suspend fun updateUsername(uid: String, oldUsername: String, newUsername: String): DataResult<Unit> {
        return userRepository.updateUsername(uid, oldUsername, newUsername)
    }

    fun removeProfilePicture(uid: String) {
        viewModelScope.launch {
            userRepository.removeProfilePicture(uid)
        }
    }

    suspend fun deleteUserSoftly(uid: String): DataResult<Unit> {
        return adminRepository.softDeleteUser(uid)
    }

    suspend fun deleteSelectedUsers(uids: Set<String>): DataResult<Unit> {
        return adminRepository.softDeleteUsers(uids)
    }
}
