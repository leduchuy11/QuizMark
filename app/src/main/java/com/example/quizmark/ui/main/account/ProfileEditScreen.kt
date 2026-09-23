package com.example.quizmark.ui.main.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.ui.theme.PrimaryDarkBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    viewModel: AccountViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    // Lấy dữ liệu hiện tại từ ViewModel
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Khởi tạo state cho các ô nhập liệu
    var fullName by remember { mutableStateOf(userProfile.fullName) }
    var school by remember { mutableStateOf(userProfile.school) }
    var bio by remember { mutableStateOf(userProfile.bio) }

    // Đồng bộ data khi load xong từ Firebase
    LaunchedEffect(userProfile) {
        fullName = userProfile.fullName
        school = userProfile.school
        bio = userProfile.bio
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hồ sơ cá nhân", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // Nút Lưu hồ sơ ở dưới cùng
            Box(modifier = Modifier.padding(24.dp)) {
                Button(
                    onClick = {
                        viewModel.saveUserProfile(fullName, school, bio) {
                            onNavigateBack() // Lưu xong tự động quay về
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Lưu hồ sơ", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Avatar tròn to ở giữa
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFFE3E8FC), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = PrimaryDarkBlue, modifier = Modifier.size(50.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Form nhập liệu
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Họ và tên") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = school,
                onValueChange = { school = it },
                label = { Text("Trường") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Giới thiệu bản thân") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp), // Ô to hơn để nhập nhiều dòng
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )
        }
    }
}