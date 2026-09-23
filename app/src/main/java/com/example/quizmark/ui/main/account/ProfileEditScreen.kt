package com.example.quizmark.ui.main.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.R
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.PrimaryDarkBlue
import com.example.quizmark.ui.theme.BackgroundScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    viewModel: AccountViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var fullName by remember { mutableStateOf(userProfile.fullName) }
    var school by remember { mutableStateOf(userProfile.school) }
    var bio by remember { mutableStateOf(userProfile.bio) }

    val scrollState = rememberScrollState()

    LaunchedEffect(userProfile) {
        fullName = userProfile.fullName
        school = userProfile.school
        bio = userProfile.bio
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PrimaryDarkBlue,
        unfocusedBorderColor = Color.LightGray,
        focusedLabelColor = PrimaryDarkBlue,
        unfocusedLabelColor = Color.Gray,
        focusedTextColor = ColorText,
        unfocusedTextColor = ColorText,
        cursorColor = PrimaryDarkBlue,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    Scaffold(
        containerColor = BackgroundScreen,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(id = R.string.profile_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDarkBlue),
                modifier = Modifier.shadow(elevation = 8.dp, spotColor = Color.LightGray)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .imePadding()
            ) {
                Button(
                    onClick = {
                        viewModel.saveUserProfile(fullName, school, bio) {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            stringResource(id = R.string.btn_save_profile),
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundScreen)
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .border(4.dp, Color(0xFFFFD700), CircleShape)
                    .background(Color(0xFFCCDCF5), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_user_avatar),
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text(stringResource(id = R.string.full_name)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = school,
                onValueChange = { school = it },
                label = { Text(stringResource(id = R.string.school)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text(stringResource(id = R.string.bio)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4,
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}