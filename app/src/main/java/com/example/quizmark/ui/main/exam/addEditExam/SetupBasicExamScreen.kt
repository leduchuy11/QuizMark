package com.example.quizmark.ui.main.exam.addEditExam

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.R
import com.example.quizmark.data.model.ExamCodeModel
import com.example.quizmark.ui.main.exam.ExamViewModel
import com.example.quizmark.ui.main.exam.templateDownload.getTemplateDetailById
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.ui.theme.PrimaryDarkBlue
import kotlinx.coroutines.delay

@Composable
fun SetupBasicExamScreen(
    templateId: Int,
    examName: String,
    examCodes: List<String>,
    onNavigateBack: () -> Unit,
    onNavigateToExamHome: () -> Unit,
    viewModel: ExamViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val isLoading by viewModel.isLoading.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()

    val template = getTemplateDetailById(templateId)
    val questionCount = template.questionCount.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 20

    // Sử dụng trực tiếp ExamCodeModel từ data.model
    var answers by remember { mutableStateOf(mapOf<String, ExamCodeModel>()) }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastType by remember { mutableStateOf(ToastType.SUCCESS) }
    var navigateAfterToast by remember { mutableStateOf(false) }

    LaunchedEffect(saveResult) {
        saveResult?.let { result ->
            if (result.isSuccess) {
                toastMessage = context.getString(R.string.toast_create_success)
                toastType = ToastType.SUCCESS
                navigateAfterToast = true
            } else {
                toastMessage = "Lỗi: ${result.exceptionOrNull()?.message}"
                toastType = ToastType.ERROR
            }
            viewModel.resetSaveResult()
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(1000)
            toastMessage = null
            if (navigateAfterToast) {
                onNavigateToExamHome()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(id = R.string.btn_back_text), tint = ColorText)
                    }

                    Text(
                        text = stringResource(id = R.string.setup_exam_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorText,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 8.dp, spotColor = Color.LightGray, ambientColor = Color.Transparent)
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text(
                            text = stringResource(id = R.string.btn_cancel),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorText
                        )
                    }

                    Button(
                        onClick = {
                            // 1. Chặn bấm liên tục khi đang lưu
                            if (isLoading) return@Button

                            var isAllAnswered = true
                            for (code in examCodes) {
                                val codeState = answers[code] ?: ExamCodeModel(code = code)
                                if (codeState.answers.size < questionCount) {
                                    isAllAnswered = false
                                    break
                                }
                            }

                            if (!isAllAnswered) {
                                toastMessage = context.getString(R.string.error_missing_answers)
                                toastType = ToastType.ERROR
                                return@Button
                            }

                            // 2. Tạo đối tượng ExamModel chuẩn bị đẩy lên Firebase
                            val newExam = com.example.quizmark.data.model.ExamModel(
                                id = java.util.UUID.randomUUID().toString(), // Tạo ID ngẫu nhiên, duy nhất
                                name = examName,
                                templateId = templateId,
                                questionCount = questionCount,
                                codes = answers.values.toList() // Chuyển map answers thành danh sách mã đề
                            )

                            // 3. Đẩy sang ViewModel để lưu nền
                            viewModel.saveBasicExam(newExam)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        // 4. Hiển thị vòng xoay loading hoặc chữ "Lưu" tùy theo trạng thái
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(id = R.string.btn_save),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0x1A000000))
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = template.imageRes),
                            contentDescription = "Ảnh phiếu thi",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }

                items(examCodes) { code ->
                    // Đọc trạng thái từ Map, nếu chưa có tạo một Model rỗng cho mã đề này
                    val currentState = answers[code] ?: ExamCodeModel(code = code)

                    ExamCodeSetupCard(
                        examCode = code,
                        questionCount = questionCount,
                        currentState = currentState,
                        onUpdateState = { newState ->
                            val updatedAnswers = answers.toMutableMap()
                            updatedAnswers[code] = newState
                            answers = updatedAnswers
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = toastMessage != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .zIndex(1f)
        ) {
            toastMessage?.let {
                CustomToastUI(message = it, type = toastType)
            }
        }
    }
}

@Composable
fun ExamCodeSetupCard(
    examCode: String,
    questionCount: Int,
    currentState: ExamCodeModel, // Truyền hẳn Data Model
    onUpdateState: (ExamCodeModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0x1A000000))
            .background(Color.White, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.exam_code_header, examCode),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = PrimaryDarkBlue
            )
        }

        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 1..questionCount) {
                // Ép kiểu câu hỏi thành String để map với Model của Firestore
                val qKey = i.toString()

                QuestionRow(
                    questionNumber = i,
                    selectedOption = currentState.answers[qKey], // Lấy từ Model
                    onOptionSelected = { option ->
                        val newAnswers = currentState.answers.toMutableMap()
                        newAnswers[qKey] = option
                        // Cập nhật lại state của Model mẹ
                        onUpdateState(currentState.copy(answers = newAnswers))
                    }
                )
            }
        }
    }
}

@Composable
fun QuestionRow(
    questionNumber: Int,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit
) {
    val options = listOf("A", "B", "C", "D")

    val rowBgColor = if (selectedOption != null) Color(0xFFF0F9FF) else Color.Transparent
    val rowBorderColor = if (selectedOption != null) Color(0xFFBAE6FD) else Color(0xFFF75F53)
    val numberColor = if (selectedOption != null) PrimaryDarkBlue else Color(0xFFF75F53)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, rowBorderColor, RoundedCornerShape(12.dp))
            .background(rowBgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = String.format("%02d", questionNumber),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = numberColor,
            modifier = Modifier.width(32.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            options.forEach { option ->
                val isSelected = selectedOption == option
                val bubbleBgColor = if (isSelected) PrimaryDarkBlue else Color.White
                val bubbleBorderColor = if (isSelected) PrimaryDarkBlue else Color(0xFFCBD5E1)
                val textColor = if (isSelected) Color.White else Color(0xFF64748B)

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.dp, bubbleBorderColor, CircleShape)
                        .background(bubbleBgColor, CircleShape)
                        .clip(CircleShape)
                        .clickable { onOptionSelected(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = textColor
                    )
                }
            }
        }
    }
}