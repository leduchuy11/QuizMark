package com.example.quizmark.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizmark.R
import com.example.quizmark.data.AuthRepository
import com.example.quizmark.data.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error(R.string.empty_fields)
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _authState.value = repository.login(email.trim(), pass.trim())
        }
    }

    fun register(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error(R.string.empty_fields)
            return
        }
        if (pass.length < 6) {
            _authState.value = AuthState.Error(R.string.pass_less)
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            _authState.value = repository.register(email.trim(), pass.trim())
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}