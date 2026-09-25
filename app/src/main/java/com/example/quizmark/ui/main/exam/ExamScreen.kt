package com.example.quizmark.ui.main.exam

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.ui.theme.PrimaryDarkBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    onNavigateToTemplates: () -> Unit,
    onNavigateToCreateExam: () -> Unit,
    viewModel: ExamViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()

    // Lắng nghe dữ liệu từ Firebase
    val examList by viewModel.examList.collectAsState()
    val totalExams = examList.size

    Scaffold(
        containerColor = BackgroundScreen,
        // 1.TOOLBAR
        topBar = {
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToCreateExam() },
                containerColor = DarkBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add_ver2),
                    contentDescription = "Tạo đề thi mới",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // 2. Nút Tạo mới
                Button(
                    onClick = { onNavigateToCreateExam() },
                    modifier = Modifier
                        .align(Alignment.End)
                        .height(34.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_ver2),
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(id = R.string.btn_create_new),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Thống kê
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(id = R.string.stat_total_exams),
                        value = totalExams.toString(),
                        valueColor = PrimaryDarkBlue,
                        iconRes = R.drawable.ic_chart
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(id = R.string.stat_total_lists),
                        value = "0",
                        valueColor = Color(0xFFD97706),
                        iconRes = R.drawable.ic_sum_list_exam
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Tải mẫu phiếu
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Color(0x33000000))
                        .background(Color(0xFF0D3A75), RoundedCornerShape(16.dp))
                        .clickable { onNavigateToTemplates() }
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE7AF20), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.template_badge_new),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.template_subtitle_1),
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = stringResource(id = R.string.template_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = stringResource(id = R.string.template_subtitle_2),
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Image(
                            painter = painterResource(id = R.drawable.ic_papers_template),
                            contentDescription = "Mẫu phiếu",
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.recent_exams_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorText
                    )
                    Text(
                        text = stringResource(id = R.string.btn_see_all),
                        fontSize = 13.sp,
                        color = PrimaryDarkBlue,
                        modifier = Modifier.clickable { /* TODO: Xem tất cả */ }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Danh sách đề
                if (examList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.empty_exam_list), color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    examList.take(5).forEach { exam ->
                        RecentExamItem(
                            examName = exam.name,
                            questionCount = exam.questionCount,
                            onClick = { /* TODO: Điều hướng sang trang sửa đề */ }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// --- COMPONENT CON CHO MÀN ĐỀ ---

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    valueColor: Color,
    iconRes: Int
) {
    Box(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Color(0x1A000000))
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
        }
    }
}

// 4. Item đề gần đây
@Composable
fun RecentExamItem(
    examName: String,
    questionCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0x1A000000))
            .background(Color.White, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_book),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = examName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorText
            )
            Text(
                text = "$questionCount câu",
                fontSize = 13.sp,
                color = Color.Gray
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_edit),
            contentDescription = "Edit",
            tint = Color.Gray,
            modifier = Modifier
                .size(18.dp)
        )
    }
}