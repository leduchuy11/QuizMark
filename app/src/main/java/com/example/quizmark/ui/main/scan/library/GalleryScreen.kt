package com.example.quizmark.ui.main.scan.library

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.data.model.FinalGradingResult
import com.example.quizmark.ui.main.scan.camera.ScanResultScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.quizmark.R
import com.example.quizmark.ui.main.scan.camera.ExamDetailScreen
import androidx.compose.ui.text.font.FontStyle
import com.example.quizmark.util.scanner.ExamProcessorFactory

@Composable
fun GalleryScreen(
    examId: String,
    rosterId: String?,
    templateId: Int,
    viewModel: BatchGradingViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onNavigateToScan: () -> Unit
) {
    if (!ExamProcessorFactory.supports(templateId)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(R.string.scan_template_unsupported))
            Button(onClick = onBackClick) { Text(stringResource(R.string.btn_back_text)) }
        }
        return
    }
    val batchState by viewModel.batchState.collectAsState()
    var hasLaunchedPicker by remember { mutableStateOf(false) }

    var selectedResult by remember { mutableStateOf<FinalGradingResult?>(null) }
    var showExamDetail by remember { mutableStateOf(false) }

    // 1. Trình chọn nhiều ảnh
    // maxItems = 50 để giới hạn giáo viên không chọn quá 50 ảnh 1 lúc tránh tràn RAM
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            // Người dùng đã chọn xong danh sách ảnh -> Ném vào ViewModel xử lý
            viewModel.processImagesBatch(uris, examId, rosterId, templateId)
        } else {
            // Người dùng bấm Hủy (không chọn ảnh nào) -> Thoát về màn hình ScanScreen
            onBackClick()
        }
    }

    // 2. Tự động kích hoạt thư viện ảnh ngay khi vào màn hình này
    LaunchedEffect(Unit) {
        if (!hasLaunchedPicker) {
            hasLaunchedPicker = true
            multiplePhotoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    BackHandler(enabled = selectedResult != null) {
        if (showExamDetail) {
            showExamDetail = false
        } else {
            selectedResult = null
        }
    }

    // 3. Xử lý giao diện chờ và giao diện kết quả
    when (val state = batchState) {
        is BatchState.Idle -> {
            Box(modifier = Modifier.fillMaxSize())
        }

        is BatchState.Processing -> {
            // Giao diện Loading hiển thị bộ đếm "Đang chấm 5 / 40"
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F6F8)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = DarkBlue, strokeWidth = 4.dp)

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(id = R.string.grading_in_progress),
                        fontSize = 18.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF999999)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${state.current} / ${state.total}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DarkBlue
                    )
                }
            }
        }

        is BatchState.Finished -> {
            AnimatedContent(
                targetState = selectedResult != null,
                transitionSpec = {
                    if (targetState) {
                        slideInHorizontally(
                            animationSpec = tween(300),
                            initialOffsetX = { fullWidth -> fullWidth }
                        ) togetherWith slideOutHorizontally(
                            animationSpec = tween(300),
                            targetOffsetX = { fullWidth -> -fullWidth }
                        )
                    } else {
                        slideInHorizontally(
                            animationSpec = tween(300),
                            initialOffsetX = { fullWidth -> -fullWidth }
                        ) togetherWith slideOutHorizontally(
                            animationSpec = tween(300),
                            targetOffsetX = { fullWidth -> fullWidth }
                        )
                    }
                },
                label = "detail_transition"
            ) { isDetailMode ->

                if (isDetailMode) {
                    val result = selectedResult
                    if (result != null) {
                        AnimatedContent(
                            targetState = showExamDetail,
                            transitionSpec = {
                                if (targetState) {
                                    slideInHorizontally(animationSpec = tween(300), initialOffsetX = { fullWidth -> fullWidth }) togetherWith
                                            slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { fullWidth -> -fullWidth })
                                } else {
                                    slideInHorizontally(animationSpec = tween(300), initialOffsetX = { fullWidth -> -fullWidth }) togetherWith
                                            slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { fullWidth -> fullWidth })
                                }
                            },
                            label = "exam_detail_transition"
                        ) { isShowingDetail ->

                            if (isShowingDetail) {
                                // MÀN HÌNH XEM ĐÁP ÁN TỪNG CÂU
                                ExamDetailScreen(
                                    studentName = result.studentName,
                                    sbd = result.sbd,
                                    examCode = result.examCode,
                                    score = result.score,
                                    correctCount = result.correctCount,
                                    wrongCount = result.wrongCount,
                                    blankCount = result.blankCount,
                                    totalQuestions = result.totalQuestions,
                                    isThpt = result.templateId == 1,
                                    questions = result.detailedQuestions,
                                    onBackClick = {
                                        showExamDetail = false
                                    }
                                )
                            } else {
                                // MÀN HÌNH TỔNG KẾT 1 BÀI
                                ScanResultScreen(
                                    studentName = result.studentName,
                                    sbd = result.sbd,
                                    examCode = result.examCode,
                                    score = result.score,
                                    correctCount = result.correctCount,
                                    wrongCount = result.wrongCount,
                                    blankCount = result.blankCount,
                                    totalQuestions = result.totalQuestions,
                                    processedImage = result.processedImage,
                                    originalImagePath = result.originalImagePath,
                                    onViewDetailsClick = {
                                        showExamDetail = true
                                    },
                                    onNextScanClick = {
                                        onNavigateToScan()
                                    },
                                    onBackClick = {
                                        selectedResult = null
                                    },
                                    onSaveResult = { }
                                )
                            }
                        }
                    }
                } else {
                    BatchResultScreen(
                        results = state.results,
                        warningMessage = state.warningMessage,
                        onItemClick = { clickedItem ->
                            selectedResult = clickedItem
                        },
                        onBackClick = onBackClick
                    )
                }
            }
        }
    }
}
