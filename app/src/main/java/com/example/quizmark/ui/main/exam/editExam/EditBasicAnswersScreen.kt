package com.example.quizmark.ui.main.exam.editExam

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.quizmark.data.model.ExamModel
import com.example.quizmark.ui.main.exam.ExamViewModel
import com.example.quizmark.ui.main.exam.addExam.CustomToastUI
import com.example.quizmark.ui.main.exam.addExam.ExamCodeSetupCard
import com.example.quizmark.ui.main.exam.addExam.ToastType
import com.example.quizmark.ui.main.exam.templateDownload.getTemplateDetailById
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import kotlinx.coroutines.delay

@Composable
fun EditBasicAnswersScreen(
    examId: String,
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

    // Quan sát dữ liệu đề thi cũ từ Firebase
    val editingExam by viewModel.editingBasicExam.collectAsState()

    val template = getTemplateDetailById(templateId)
    val questionCount = template.questionCount.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 20

    var answers by remember { mutableStateOf(mapOf<String, ExamCodeModel>()) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastType by remember { mutableStateOf(ToastType.SUCCESS) }
    var navigateAfterToast by remember { mutableStateOf(false) }

    // 1. Tải dữ liệu cũ khi vừa vào màn hình
    LaunchedEffect(examId) {
        viewModel.loadBasicExam(examId)
    }

    // 2. LOGIC ĐIỀN LẠI ĐÁP ÁN:
    LaunchedEffect(editingExam) {
        editingExam?.let { oldExam ->
            val newAnswers = mutableMapOf<String, ExamCodeModel>()

            examCodes.forEach { newCodeStr ->
                // Tìm xem mã này có tồn tại ở đề cũ không
                val oldCodeModel = oldExam.codes.find { it.code == newCodeStr }

                if (oldCodeModel != null) {
                    // Nếu không đổi -> Lấy nguyên bộ đáp án cũ nhét vào
                    newAnswers[newCodeStr] = oldCodeModel
                } else {
                    // Nếu đổi (Mã mới) -> Tạo một model trống tinh cho điền lại từ đầu
                    newAnswers[newCodeStr] = ExamCodeModel(code = newCodeStr)
                }
            }
            answers = newAnswers
            viewModel.resetEditingExam()
        }
    }

    // Lắng nghe kết quả Lưu
    LaunchedEffect(saveResult) {
        saveResult?.let { result ->
            if (result.isSuccess) {
                toastMessage = context.getString(R.string.toast_update_answers_success)
                toastType = ToastType.SUCCESS
                navigateAfterToast = true
            } else {
                toastMessage = context.getString(R.string.error_prefix, result.exceptionOrNull()?.message)
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
                        .shadow(4.dp, spotColor = Color.LightGray)
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
                        text = stringResource(id = R.string.edit_answers_title),
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorText,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, spotColor = Color.LightGray)
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
                        border = BorderStroke(2.dp, Color(0xFFFC4949))
                    ) {
                        Text(
                            text = stringResource(id = R.string.btn_cancel),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFC4949)
                        )
                    }

                    Button(
                        onClick = {
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

                            val updatedExam = ExamModel(
                                id = examId,
                                name = examName,
                                templateId = templateId,
                                questionCount = questionCount,
                                codes = answers.values.toList()
                            )
                            viewModel.saveBasicExam(updatedExam)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(id = R.string.btn_update),
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
                            contentDescription = stringResource(id = R.string.cd_exam_image),
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }

                items(examCodes) { code ->
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
            toastMessage?.let { CustomToastUI(message = it, type = toastType) }
        }
    }
}