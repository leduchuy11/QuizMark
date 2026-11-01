package com.example.quizmark.ui.main.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
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
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    accountViewModel: AccountViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
    onNavigateToScan: () -> Unit,
    onNavigateToExam: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToStatistics: () -> Unit
) {
    val userProfile by accountViewModel.userProfile.collectAsState()
    val scrollState = rememberScrollState()

    // Thu thập State từ HomeViewModel
    val uiState by homeViewModel.uiState.collectAsState()

    val displayName = if (userProfile.fullName.isNotBlank()) {
        userProfile.fullName
    } else {
        accountViewModel.getUserEmail().substringBefore("@")
    }

    // State cho BottomSheet
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showGoalBottomSheet by remember { mutableStateOf(false) }

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
                    onClick = onNavigateToStatistics
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.average_score),
                    value = String.format("%.1f", uiState.averageScore),
                    suffix = "/10"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.classes),
                    value = uiState.rosterCount.toString(),
                    suffix = ""
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ĐẶT MỤC TIÊU HÔM NAY
            Text(text = stringResource(R.string.today_goal), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ColorText)
            Spacer(modifier = Modifier.height(12.dp))
            GoalCard(
                goal = uiState.todayGoal,
                current = uiState.todayScannedCount,
                onClick = { showGoalBottomSheet = true }
            )

            Spacer(modifier = Modifier.height(40.dp))

        }

        // BOTTOM SHEET ĐẶT MỤC TIÊU
        if (showGoalBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showGoalBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color.White,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                GoalBottomSheetContent(
                    initialGoal = uiState.todayGoal,
                    onSave = { newGoal ->
                        homeViewModel.updateTodayGoal(newGoal)
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showGoalBottomSheet = false
                        }
                    },
                    onCancel = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) showGoalBottomSheet = false
                        }
                    }
                )
            }
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
fun GoalCard(goal: Int, current: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_stars), contentDescription = null, modifier = Modifier.size(28.dp),tint = Color.Unspecified)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.grade_today_goal, goal), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorText)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = stringResource(R.string.goal_completed, current, goal), fontSize = 13.sp, color = Color.Gray)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ColorText3, modifier = Modifier.size(28.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalBottomSheetContent(initialGoal: Int, onSave: (Int) -> Unit, onCancel: () -> Unit) {
    var inputGoal by remember { mutableStateOf(initialGoal.toString()) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.today_goal_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ColorText)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = stringResource(R.string.today_goal_description), fontSize = 14.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.exam_count_label), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = inputGoal,
                onValueChange = {
                    if (it.isEmpty() || it.all { char -> char.isDigit() }) inputGoal = it
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF1F5F9),
                    unfocusedContainerColor = Color(0xFFF1F5F9),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorText)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.dialog_cancel),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorText,
                modifier = Modifier.clickable { onCancel() }.padding(12.dp)
            )

            Button(
                onClick = {
                    val goal = inputGoal.toIntOrNull() ?: 10
                    onSave(if (goal > 0) goal else 10)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(text = stringResource(R.string.save_goal), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}