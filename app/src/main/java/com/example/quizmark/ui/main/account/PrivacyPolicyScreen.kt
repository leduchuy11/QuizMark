package com.example.quizmark.ui.main.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quizmark.R
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBackClick: () -> Unit) {
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = BackgroundScreen,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.privacy_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = ColorText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ColorText, modifier = Modifier.size(28.dp))
                    }
                },
                actions = { Spacer(modifier = Modifier.width(48.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundScreen)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon minh họa
            Box(
                modifier = Modifier.size(80.dp).background(Color(0xFFE6F0FD), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_lock), contentDescription = null, tint = DarkBlue, modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nội dung
            Text(text = stringResource(id = R.string.privacy_intro), fontSize = 15.sp, color = Color.Gray, lineHeight = 22.sp, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                PrivacySection(title = stringResource(id = R.string.privacy_h1), body = stringResource(id = R.string.privacy_b1))
                PrivacySection(title = stringResource(id = R.string.privacy_h2), body = stringResource(id = R.string.privacy_b2))
                PrivacySection(title = stringResource(id = R.string.privacy_h3), body = stringResource(id = R.string.privacy_b3))
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun PrivacySection(title: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = body, fontSize = 15.sp, color = ColorText, lineHeight = 24.sp)
    }
}