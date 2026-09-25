package com.example.quizmark.ui.main.exam.addEditExam

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PunchClock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.quizmark.R
import com.example.quizmark.ui.main.exam.templateDownload.getTemplateDetailById
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.ui.theme.PrimaryDarkBlue
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun SetupThptExamScreen(
    templateId: Int,
    examName: String,
    examCodes: List<String>,
    onNavigateBack: () -> Unit,
    onNavigateToNext: (Int, Float, Int, Int, Float) -> Unit
) {
    val context = LocalContext.current
    val template = getTemplateDetailById(templateId)
    val scrollState = rememberScrollState()

    // --- STATES CHO CÁC Ô NHẬP ---
    var p1QuestionsStr by remember { mutableStateOf("40") }
    var p1ScoreStr by remember { mutableStateOf("4") }

    var p2QuestionsStr by remember { mutableStateOf("8") }
    // Điểm Phần II tự tính = số câu * 1
    val p2CalculatedScore = (p2QuestionsStr.toIntOrNull() ?: 0) * 1f

    var p3QuestionsStr by remember { mutableStateOf("6") }
    var p3ScoreStr by remember { mutableStateOf("3") }

    // --- STATES CHO TOAST CẢNH BÁO ---
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastType by remember { mutableStateOf(ToastType.WARNING) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2500)
            toastMessage = null
        }
    }

    // Tính toán tổng
    val p1Q = p1QuestionsStr.toIntOrNull() ?: 0
    val p1S = p1ScoreStr.toFloatOrNull() ?: 0f
    val p2Q = p2QuestionsStr.toIntOrNull() ?: 0
    val p3Q = p3QuestionsStr.toIntOrNull() ?: 0
    val p3S = p3ScoreStr.toFloatOrNull() ?: 0f

    val totalQ = p1Q + p2Q + p3Q
    val totalS = p1S + p2CalculatedScore + p3S

    // Kiểm tra tổng điểm có bằng 10 không
    val isTotalValid = abs(totalS - 10f) < 0.01f

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
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Close", tint = ColorText)
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 16.dp, spotColor = Color.LightGray, ambientColor = Color.Transparent)
                        .background(Color.White)
                ) {
                    // Phần hiển thị Tổng kết
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(stringResource(id = R.string.label_total_questions), color = Color.Gray, fontSize = 12.sp)
                            Text(
                                text = totalQ.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDarkBlue
                            )
                        }

                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.LightGray))

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(stringResource(id = R.string.label_total_score), color = Color.Gray, fontSize = 12.sp)
                            Text(
                                text = formatScore(totalS),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                // Hiển thị màu đỏ nếu tổng điểm khác 10
                                color = if (isTotalValid) PrimaryDarkBlue else Color(0xFFF75F53)
                            )
                        }
                    }

                    // Nút hành động
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Button(
                            onClick = {
                                // --- VALIDATION ---
                                if (p1Q > 40) {
                                    toastMessage = context.getString(R.string.error_part1_max_q)
                                    toastType = ToastType.WARNING
                                    return@Button
                                }
                                if (p1S > 10f || p3S > 10f) {
                                    toastMessage = context.getString(R.string.error_section_max_score)
                                    toastType = ToastType.WARNING
                                    return@Button
                                }
                                if (p2Q > 8) {
                                    toastMessage = context.getString(R.string.error_part2_max_q)
                                    toastType = ToastType.WARNING
                                    return@Button
                                }
                                if (p3Q > 6) {
                                    toastMessage = context.getString(R.string.error_part3_max_q)
                                    toastType = ToastType.WARNING
                                    return@Button
                                }
                                if (!isTotalValid) {
                                    toastMessage = context.getString(R.string.error_total_score_invalid)
                                    toastType = ToastType.ERROR
                                    return@Button
                                }

                                // Hợp lệ -> Chuyển sang màn nhập đáp án chi tiết
                                onNavigateToNext(p1Q, p1S, p2Q, p3Q, p3S)
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.btn_continue_input),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Ảnh phiếu thi
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

                // KHỐI PHẦN I
                SectionCard(
                    tag = stringResource(R.string.setup_thpt_part1_tag),
                    tagBgColor = PrimaryDarkBlue,
                    title = stringResource(R.string.setup_thpt_part1_title),
                    description = stringResource(R.string.setup_thpt_part1_desc)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_question_count), fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            ThptInputField(
                                value = p1QuestionsStr,
                                onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) p1QuestionsStr = it }
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_section_score), fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            ThptInputField(
                                value = p1ScoreStr,
                                onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) p1ScoreStr = it }
                            )
                        }
                    }
                }

                // KHỐI PHẦN II
                SectionCard(
                    tag = stringResource(R.string.setup_thpt_part2_tag),
                    tagBgColor = Color(0xFFD97706), // Màu cam
                    title = stringResource(R.string.setup_thpt_part2_title),
                    description = stringResource(R.string.setup_thpt_part2_desc)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_question_count), fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            ThptInputField(
                                value = p2QuestionsStr,
                                onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) p2QuestionsStr = it }
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PunchClock, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.Gray) // Icon giả khóa
                                Spacer(Modifier.width(2.dp))
                                Text(stringResource(R.string.label_auto_score), fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                            }
                            Spacer(Modifier.height(4.dp))
                            // Ô hiển thị điểm tự tính với viền đứt đoạn
                            ThptInputField(
                                value = stringResource(R.string.unit_point, formatScore(p2CalculatedScore)),
                                onValueChange = {},
                                readOnly = true,
                                isDashed = true
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.label_score_per_correct_item), fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))

                    // 4 khối điểm fix cứng
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FixedScoreBox(label = stringResource(R.string.label_correct_1), score = "0.1")
                        FixedScoreBox(label = stringResource(R.string.label_correct_2), score = "0.25")
                        FixedScoreBox(label = stringResource(R.string.label_correct_3), score = "0.5")
                        FixedScoreBox(label = stringResource(R.string.label_correct_4), score = "1")
                    }
                }

                // KHỐI PHẦN III
                SectionCard(
                    tag = stringResource(R.string.setup_thpt_part3_tag),
                    tagBgColor = PrimaryDarkBlue,
                    title = stringResource(R.string.setup_thpt_part3_title),
                    description = stringResource(R.string.setup_thpt_part3_desc)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_question_count), fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            ThptInputField(
                                value = p3QuestionsStr,
                                onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) p3QuestionsStr = it }
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_section_score), fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            ThptInputField(
                                value = p3ScoreStr,
                                onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) p3ScoreStr = it }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }

        // Lớp nổi hiển thị Toast Cảnh báo
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

// Hàm format điểm để bỏ số .0 ở đuôi nếu là số nguyên
fun formatScore(score: Float): String {
    return if (score == score.toInt().toFloat()) score.toInt().toString() else score.toString()
}

// Khối Card dùng chung cho 3 Phần
@Composable
fun SectionCard(
    tag: String,
    tagBgColor: Color,
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x1A000000))
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(tagBgColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = tag, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorText)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = description, fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

// Ô nhập liệu tùy chỉnh cho các thông số
@Composable
fun ThptInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    isDashed: Boolean = false
) {
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                color = if (isDashed) Color(0xFFF8FAFC) else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(12.dp)
            )
            .then(
                if (isDashed) Modifier.drawBehind {
                    drawRoundRect(
                        color = Color(0xFF94A3B8),
                        style = Stroke(width = 3f, pathEffect = pathEffect),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = { if (!readOnly) onValueChange(it) },
            textStyle = LocalTextStyle.current.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (readOnly && isDashed) PrimaryDarkBlue else ColorText
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            readOnly = readOnly,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Ô điểm fix cứng ở Phần II
@Composable
fun RowScope.FixedScoreBox(label: String, score: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Text(text = label, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = score, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ColorText)
        }
    }
}