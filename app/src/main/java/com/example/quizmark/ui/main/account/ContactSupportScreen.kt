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
import androidx.compose.ui.draw.clip
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
import com.example.quizmark.ui.theme.PrimaryDarkBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSupportScreen(onBackClick: () -> Unit) {
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = BackgroundScreen,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.contact_title),
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
            Icon(
                painter = painterResource(id = R.drawable.ic_avatar_app),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Nội dung giới thiệu
            Text(text = stringResource(id = R.string.contact_intro), fontSize = 15.sp, color = Color.Gray, lineHeight = 22.sp, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(32.dp))

            // Các thẻ thông tin liên hệ
            ContactCard(iconRes = R.drawable.ic_email, title = stringResource(R.string.contact_email_title), value = stringResource(R.string.contact_email_value))
            Spacer(modifier = Modifier.height(16.dp))

            ContactCard(iconRes = R.drawable.ic_hotline, title = stringResource(R.string.contact_phone_title), value = stringResource(R.string.contact_phone_value))
            Spacer(modifier = Modifier.height(16.dp))
            ContactCard(iconRes = R.drawable.ic_group_ver2, title = stringResource(R.string.contact_zalo_title), value = stringResource(R.string.contact_zalo_value))

            Spacer(modifier = Modifier.height(40.dp))

            Text(text = stringResource(id = R.string.contact_time), fontSize = 13.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        }
    }
}

@Composable
fun ContactCard(iconRes: Int, title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(Color(0xFFF3F7FC), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(30.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorText)
            }
        }
    }
}