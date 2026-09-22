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
import com.example.quizmark.ui.auth.LoginScreen
import com.example.quizmark.ui.theme.QuizMarkTheme // Đảm bảo đúng tên Theme của project bạn
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuizMarkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "login") {

                        composable("login") {
                            LoginScreen(
                                onNavigateToRegister = {
                                    // TODO: Chuyển sang màn hình đăng ký
                                    // navController.navigate("register")
                                },
                                onLoginSuccess = {
                                    // TODO: Chuyển sang màn hình chính của app
                                    // navController.navigate("home")
                                }
                            )
                        }

                        // Khai báo sẵn chỗ cho màn hình Đăng ký (chưa code nên tạm comment lại)
                        /*
                        composable("register") {
                            RegisterScreen(...)
                        }
                        */
                    }
                }
            }
        }
    }
}