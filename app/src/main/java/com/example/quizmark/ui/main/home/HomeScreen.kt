package com.example.quizmark.ui.main.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.R
import com.example.quizmark.ui.main.account.AccountViewModel
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.ColorText2
import com.example.quizmark.ui.theme.ColorText3
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.ui.theme.PrimaryDarkBlue

@Composable
fun HomeScreen(
    accountViewModel: AccountViewModel = hiltViewModel(),
    onNavigateToScan: () -> Unit,
    onNavigateToExam: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val userProfile by accountViewModel.userProfile.collectAsState()
    val scrollState = rememberScrollState()

    val displayName = if (userProfile.fullName.isNotBlank()) {
        userProfile.fullName
    } else {
        accountViewModel.getUserEmail().substringBefore("@")
    }

    Scaffold(
        containerColor = BackgroundScreen,
        topBar = {
            // Thanh Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundScreen)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_avatar_app),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(4.dp, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "QuizMark",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorText
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Lời chào
            Text(
                text = stringResource(
                    id = R.string.greeting,
                    displayName
                ),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = ColorText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.welcome_subtitle),
                fontSize = 17.sp,
                color = ColorText3
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Banner Hành động chính (Màu Xanh Đậm)
            MainActionBanner(onNavigateToScan, onNavigateToExam)

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Grid Thống kê 4 ô
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    title = stringResource(R.string.dashboard_history),
                    subtitle = "",
                    icon = Icons.Filled.History,
                    iconTint = Color(0xFF5B67C6),
                    iconBgColor = Color(0xFFEEF0FA),
                    onClick = onNavigateToHistory
                )
                DashboardCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    title = stringResource(R.string.dashboard_statistics),
                    subtitle = "",
                    icon = Icons.Rounded.Analytics,
                    iconTint = Color(0xFFE88B4B),
                    iconBgColor = Color(0xFFFDF3ED),
                    onClick = { /* TODO */ }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(modifier = Modifier.weight(1f), title = stringResource(R.string.average_score), value = "0.0", suffix = "/10")
                StatCard(modifier = Modifier.weight(1f), title = stringResource(R.string.classes), value = "2", suffix = "")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Hoạt động gần đây
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.recent_activity), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = ColorText)
                Text(
                    text = stringResource(R.string.view_all),
                    fontSize = 13.sp,
                    color = Color(0xFF1873B9),
                    modifier = Modifier.clickable { onNavigateToHistory() }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Render danh sách hoạt động (Mẫu)
            ActivityItem(title = "Đã chấm 1 bài", score = "Điểm 0.6 / 10 (3/36)", timeAgo = "1 ngày trước")
            Spacer(modifier = Modifier.height(10.dp))
            ActivityItem(title = "Đã chấm 1 bài", score = "Điểm 7.5 / 10 (30/40)", timeAgo = "Hôm qua")

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun MainActionBanner(onScanClick: () -> Unit, onExamClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        // Tạo dải màu gradient từ trái sang phải
        val gradientBrush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF194A8D),
                Color(0xFF0F2C5A)
            )
        )
        Column(modifier = Modifier
            .background(brush = gradientBrush)
            .padding(20.dp)) {
            Text(
                text = stringResource(R.string.ready_to_grade),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.grade_description),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFB0C0D0)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Nút Quét Ngay
                Button(
                    onClick = onScanClick,
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BackgroundScreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_scanner),
                        contentDescription = null,
                        tint = ColorText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.scan_now), color = ColorText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                // Nút Đề thi
                Button(
                    onClick = onExamClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .border(
                            width = 1.dp,
                            color = Color(0xFFA8A6A6),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25416E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_document),
                        contentDescription = null,
                        tint = BackgroundScreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.create_exam), color = BackgroundScreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun DashboardCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBgColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorText2)
                if (subtitle.isNotEmpty()) {
                    Text(text = subtitle, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = ColorText)
                }
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String, suffix: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = DarkBlue)
                if (suffix.isNotEmpty()) {
                    Text(text = suffix, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ColorText3, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }
}

@Composable
fun ActivityItem(title: String, score: String, timeAgo: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFDCFCE7), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ColorText)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = score, fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(text = timeAgo, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    }
}