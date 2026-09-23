package com.example.quizmark.data.repository

import androidx.annotation.StringRes
import com.example.quizmark.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider


// Lớp chứa các trạng thái của luồng đăng nhập
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(@param:StringRes val message: Int) : AuthState()
}

interface AuthRepository {
    suspend fun login(email: String, pass: String): AuthState
    suspend fun register(email: String, pass: String): AuthState
    suspend fun resetPassword(email: String): AuthState
    suspend fun loginWithGoogle(idToken: String): AuthState
}


class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override suspend fun login(email: String, pass: String): AuthState {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            // Kiểm tra xem email đã được bấm link xác thực chưa
            if (result.user?.isEmailVerified == true) {
                AuthState.Success
            } else {
                auth.signOut()
                AuthState.Error(R.string.email_not_verified)
            }
        } catch (e: Exception) {
            AuthState.Error(R.string.login_failed)
        }
    }

    override suspend fun register(email: String, pass: String): AuthState {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            result.user?.sendEmailVerification()?.await()
            auth.signOut()
            AuthState.Success
        } catch (e: Exception) {
            AuthState.Error(R.string.register_failed)
        }
    }

    override suspend fun resetPassword(email: String): AuthState {
        return try {
            auth.sendPasswordResetEmail(email).await()
            AuthState.Success
        } catch (e: FirebaseAuthInvalidUserException) {
            AuthState.Error(R.string.email_not_found)
        } catch (e: Exception) {
            AuthState.Error(R.string.reset_password_failed)
        }
    }

    override suspend fun loginWithGoogle(idToken: String): AuthState {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            AuthState.Success
        } catch (e: Exception) {
            AuthState.Error(R.string.login_failed)
        }
    }
}