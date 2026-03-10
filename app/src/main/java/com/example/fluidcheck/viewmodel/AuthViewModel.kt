package com.example.fluidcheck.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluidcheck.repository.AuthRepository
import com.example.fluidcheck.util.DataResult
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkCurrentUser()
    }

    fun checkCurrentUser() {
        _currentUser.value = authRepository.currentUser
        if (authRepository.currentUser != null) {
            _authState.value = AuthState.Success(authRepository.currentUser!!)
        }
    }

    fun signIn(email: String, pass: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.signIn(email, pass)
            handleAuthResult(result)
        }
    }

    fun signUp(email: String, pass: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.signUp(email, pass)
            handleAuthResult(result)
        }
    }

    fun signInWithGoogle(idToken: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            handleAuthResult(result)
        }
    }

    private fun handleAuthResult(result: DataResult<FirebaseUser?>) {
        when (result) {
            is DataResult.Success -> {
                if (result.data != null) {
                    _currentUser.value = result.data
                    _authState.value = AuthState.Success(result.data)
                } else {
                    _authState.value = AuthState.Error("Unknown error occurred")
                }
            }
            is DataResult.Error -> {
                _authState.value = AuthState.Error(result.exception.localizedMessage ?: "Authentication failed")
            }
            is DataResult.Loading -> _authState.value = AuthState.Loading
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun signOut() {
        authRepository.signOut()
        _currentUser.value = null
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}
