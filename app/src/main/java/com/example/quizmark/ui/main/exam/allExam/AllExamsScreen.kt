package com.example.quizmark.ui.main.exam.allExam

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.R
import com.example.quizmark.ui.main.exam.ExamViewModel
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.PrimaryDarkBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllExamsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExamViewModel = hiltViewModel()
) {
    // Lấy danh sách từ ViewModel (đã tự động sắp xếp mới nhất lên đầu)
    val allExams by viewModel.examList.collectAsState()

    // State quản lý thanh tìm kiếm
    var searchQuery by remember { mutableStateOf("") }
    // State quản lý focus để đổi màu viền khi bấm vào
    var isSearchFocused by remember { mutableStateOf(false) }

    // Logic lọc theo tên
    val filteredExams = remember(searchQuery, allExams) {
        if (searchQuery.isBlank()) {
            allExams
        } else {
            allExams.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    Scaffold(
        containerColor = BackgroundScreen,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 4.dp, spotColor = Color.LightGray, ambientColor = Color.Transparent)
                    .background(Color.White)
                    .padding(vertical = 6.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ColorText)
                }

                Text(
                    text = stringResource(id = R.string.all_exams_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorText,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Thanh Tìm kiếm tuỳ chỉnh
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp) // Chiều cao nhỏ gọn
                        .border(
                            width = 1.dp,
                            color = if (isSearchFocused) PrimaryDarkBlue else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(50)
                        )
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(id = R.string.search_exam_hint),
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = ColorText),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isSearchFocused = it.isFocused }
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            // Danh sách đề thi
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredExams.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (allExams.isEmpty()) "Bạn chưa tạo đề thi nào" else stringResource(R.string.empty_search_result),
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(filteredExams) { exam ->
                        AllExamItem(
                            examName = exam.name,
                            questionCount = exam.questionCount,
                            isThpt = exam.isThpt,
                            onClick = { /* TODO: Mở chi tiết đề */ }
                        )
                    }
                }
            }
        }
    }
}

// Item hiển thị trong danh sách Tất cả đề thi
@Composable
fun AllExamItem(
    examName: String,
    questionCount: Int,
    isThpt: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp), spotColor = Color(0x1A000000))
            .background(Color.White, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon bên trái
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(if (isThpt) Color(0xFFFCEDDA) else Color(0xFFDBEAFD), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_book),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Nội dung giữa
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = examName,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ColorText,
                maxLines = 1
            )

            Text(text = "$questionCount câu", fontSize = 12.sp, lineHeight = 18.sp, color = Color.Gray)
        }

        // Mũi tên bên phải
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Xem chi tiết",
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(28.dp)
        )
    }
}