package com.example.quizmark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.quizmark.ui.auth.ForgotPasswordScreen
import com.example.quizmark.ui.auth.LoginScreen
import com.example.quizmark.ui.auth.RegisterScreen
import com.example.quizmark.ui.home.HomeScreen
import com.example.quizmark.ui.theme.QuizMarkTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuizMarkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Kiểm tra xem đã đăng nhập và đã xác thực email chưa
                    val currentUser = auth.currentUser
                    val startDest = if (currentUser != null && currentUser.isEmailVerified) {
                        "home"
                    } else {
                        "login"
                    }

                    NavHost(navController = navController, startDestination = startDest) {

                        composable("login") {
                            LoginScreen(
                                onNavigateToRegister = { navController.navigate("register") },
                                onNavigateToForgotPassword = { navController.navigate("forgot_password") },
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("register") {
                            RegisterScreen(
                                onNavigateToLogin = { navController.popBackStack() },
                                onRegisterSuccess = { navController.popBackStack() }
                            )
                        }

                        composable("forgot_password") {
                            ForgotPasswordScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("home") {
                            HomeScreen()
                        }
                    }
                }
            }
        }
    }
}