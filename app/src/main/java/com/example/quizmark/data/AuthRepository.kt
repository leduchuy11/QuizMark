package com.example.quizmark.data

import com.example.quizmark.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// Lớp chứa các trạng thái của luồng đăng nhập
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(@param:androidx.annotation.StringRes val message: Int) : AuthState()
}

interface AuthRepository {
    suspend fun login(email: String, pass: String): AuthState
    suspend fun register(email: String, pass: String): AuthState
}

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {
    override suspend fun login(email: String, pass: String): AuthState {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            AuthState.Success
        } catch (e: Exception) {
            AuthState.Error(R.string.login_failed)
        }
    }

    override suspend fun register(email: String, pass: String): AuthState {
        return try {
            auth.createUserWithEmailAndPassword(email, pass).await()
            AuthState.Success
        } catch (e: Exception) {
            AuthState.Error(R.string.register_failed)
        }
    }
}