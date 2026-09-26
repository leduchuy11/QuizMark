package com.example.quizmark.ui.main.exam.editExam

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.R
import com.example.quizmark.data.model.ThptExamCodeModel
import com.example.quizmark.data.model.ThptExamConfig
import com.example.quizmark.data.model.ThptExamModel
import com.example.quizmark.ui.main.exam.ExamViewModel
import com.example.quizmark.ui.main.exam.addExam.CustomToastUI
import com.example.quizmark.ui.main.exam.addExam.ThptExamCodeCard
import com.example.quizmark.ui.main.exam.addExam.ToastType
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.ui.theme.PrimaryDarkBlue
import kotlinx.coroutines.delay

@Composable
fun EditThptAnswersScreen(
    examId: String,
    templateId: Int,
    examName: String,
    p1Q: Int,
    p1S: Float,
    p2Q: Int,
    p3Q: Int,
    p3S: Float,
    examCodes: List<String>,
    onNavigateBack: () -> Unit,
    onNavigateToExamHome: () -> Unit,
    viewModel: ExamViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var answers by remember { mutableStateOf(mapOf<String, ThptExamCodeModel>()) }

    val isLoading by viewModel.isLoading.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()
    val editingExam by viewModel.editingThptExam.collectAsState()

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastType by remember { mutableStateOf(ToastType.ERROR) }
    var navigateAfterToast by remember { mutableStateOf(false) }

    // Dùng chung state load đề THPT từ màn Sửa cấu trúc nên không cần gọi loadThptExam lại.

    LaunchedEffect(examId) {
        viewModel.loadThptExam(examId)
    }

    LaunchedEffect(editingExam) {
        editingExam?.let { oldExam ->
            val newAnswers = mutableMapOf<String, ThptExamCodeModel>()
            val oldConfig = oldExam.config

            // LOGIC KÉP: Kiểm tra cấu trúc phần 1,2,3 CÓ THAY ĐỔI KHÔNG?
            val isStructureUnchanged = (oldConfig.p1Questions == p1Q) &&
                    (oldConfig.p2Questions == p2Q) &&
                    (oldConfig.p3Questions == p3Q)

            examCodes.forEach { newCodeStr ->
                val oldCodeModel = oldExam.codes.find { it.code == newCodeStr }

                // Chỉ giữ đáp án NẾU MÃ TỒN TẠI VÀ CẤU TRÚC KHÔNG ĐỔI
                if (oldCodeModel != null && isStructureUnchanged) {
                    newAnswers[newCodeStr] = oldCodeModel
                } else {
                    newAnswers[newCodeStr] = ThptExamCodeModel(code = newCodeStr)
                }
            }
            answers = newAnswers
            viewModel.resetEditingThptExam() // Xóa cache sau khi dùng
        }
    }

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
            if (navigateAfterToast) onNavigateToExamHome()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = BackgroundScreen,
            topBar = {
                Box(
                    modifier = Modifier.fillMaxWidth().shadow(4.dp, spotColor = Color.LightGray).background(Color.White).padding(vertical = 6.dp)
                ) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.align(Alignment.CenterStart)) {
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
                    modifier = Modifier.fillMaxWidth().shadow(8.dp, spotColor = Color.LightGray).background(Color.White).padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack, modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(16.dp), border = BorderStroke(2.dp, Color(0xFFFC4949))
                    ) {
                        Text(text = stringResource(id = R.string.btn_cancel), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFC4949))
                    }

                    Button(
                        onClick = {
                            if (isLoading) return@Button
                            var isAllValid = true
                            for (code in examCodes) {
                                val codeState = answers[code] ?: ThptExamCodeModel(code = code)
                                if (codeState.part1.size < p1Q) isAllValid = false
                                if (codeState.part2.size < p2Q || codeState.part2.values.any { it.size < 4 }) isAllValid = false
                                if (codeState.part3.size < p3Q || codeState.part3.values.any { it.isBlank() }) isAllValid = false
                                if (!isAllValid) break
                            }

                            if (!isAllValid) {
                                toastMessage = context.getString(R.string.error_missing_thpt_answers)
                                toastType = ToastType.ERROR
                                return@Button
                            }

                            val config = ThptExamConfig(p1Questions = p1Q, p1Score = p1S, p2Questions = p2Q, p3Questions = p3Q, p3Score = p3S)

                            // Dùng lại ID để ghi đè (Cập nhật)
                            val updatedExam = ThptExamModel(
                                id = examId,
                                name = examName,
                                templateId = templateId,
                                config = config,
                                codes = answers.values.toList()
                            )
                            viewModel.saveThptExam(updatedExam)
                        },
                        modifier = Modifier.weight(1f).height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text(text = stringResource(id = R.string.btn_update), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(examCodes) { code ->
                    val currentState = answers[code] ?: ThptExamCodeModel(code = code)
                    ThptExamCodeCard(
                        examCode = code,
                        p1Q = p1Q, p2Q = p2Q, p3Q = p3Q,
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
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp).zIndex(1f)
        ) {
            toastMessage?.let { CustomToastUI(message = it, type = toastType) }
        }
    }
}