package com.example.quizmark.ui.main.exam.editExam

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PunchClock
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
import com.example.quizmark.ui.main.exam.ExamViewModel
import com.example.quizmark.ui.main.exam.addExam.CustomToastUI
import com.example.quizmark.ui.main.exam.addExam.FixedScoreBox
import com.example.quizmark.ui.main.exam.addExam.SectionCard
import com.example.quizmark.ui.main.exam.addExam.ThptInputField
import com.example.quizmark.ui.main.exam.addExam.ToastType
import com.example.quizmark.ui.main.exam.addExam.formatScore
import com.example.quizmark.ui.main.exam.templateDownload.getTemplateDetailById
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.ui.theme.PrimaryDarkBlue
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun EditThptExamScreen(
    examId: String,
    templateId: Int,
    examName: String,
    examCodes: List<String>,
    onNavigateBack: () -> Unit,
    onNavigateToNext: (Int, Float, Int, Int, Float) -> Unit,
    viewModel: ExamViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val template = getTemplateDetailById(templateId)
    val scrollState = rememberScrollState()

    val editingExam by viewModel.editingThptExam.collectAsState()

    var p1QuestionsStr by remember { mutableStateOf("40") }
    var p1ScoreStr by remember { mutableStateOf("4") }
    var p2QuestionsStr by remember { mutableStateOf("8") }
    var p3QuestionsStr by remember { mutableStateOf("6") }
    var p3ScoreStr by remember { mutableStateOf("3") }

    // Load đề THPT khi vào màn hình
    LaunchedEffect(examId) {
        viewModel.loadThptExam(examId)
    }

    // Fill dữ liệu cũ vào form
    LaunchedEffect(editingExam) {
        editingExam?.config?.let { config ->
            p1QuestionsStr = config.p1Questions.toString()
            p1ScoreStr = formatScore(config.p1Score)
            p2QuestionsStr = config.p2Questions.toString()
            p3QuestionsStr = config.p3Questions.toString()
            p3ScoreStr = formatScore(config.p3Score)
        }
    }

    val p2CalculatedScore = (p2QuestionsStr.toIntOrNull() ?: 0) * 1f

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastType by remember { mutableStateOf(ToastType.WARNING) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2500)
            toastMessage = null
        }
    }

    val p1Q = p1QuestionsStr.toIntOrNull() ?: 0
    val p1S = p1ScoreStr.toFloatOrNull() ?: 0f
    val p2Q = p2QuestionsStr.toIntOrNull() ?: 0
    val p3Q = p3QuestionsStr.toIntOrNull() ?: 0
    val p3S = p3ScoreStr.toFloatOrNull() ?: 0f

    val totalQ = p1Q + p2Q + p3Q
    val totalS = p1S + p2CalculatedScore + p3S
    val isTotalValid = abs(totalS - 10f) < 0.01f

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
                        text = stringResource(id = R.string.edit_thpt_structure_title), // Đổi title
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorText,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth().shadow(16.dp, spotColor = Color.LightGray).background(Color.White)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(id = R.string.label_total_questions), color = Color.Gray, fontSize = 12.sp)
                            Text(text = totalQ.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
                        }
                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.LightGray))
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(id = R.string.label_total_score), color = Color.Gray, fontSize = 12.sp)
                            Text(text = formatScore(totalS), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (isTotalValid) PrimaryDarkBlue else Color(0xFFF75F53))
                        }
                    }

                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Button(
                            onClick = {
                                if (p1Q > 40) { toastMessage = context.getString(R.string.error_part1_max_q); toastType = ToastType.WARNING; return@Button }
                                if (p1S > 10f || p3S > 10f) { toastMessage = context.getString(R.string.error_section_max_score); toastType = ToastType.WARNING; return@Button }
                                if (p2Q > 8) { toastMessage = context.getString(R.string.error_part2_max_q); toastType = ToastType.WARNING; return@Button }
                                if (p3Q > 6) { toastMessage = context.getString(R.string.error_part3_max_q); toastType = ToastType.WARNING; return@Button }
                                if (!isTotalValid) { toastMessage = context.getString(R.string.error_total_score_invalid); toastType = ToastType.ERROR; return@Button }

                                onNavigateToNext(p1Q, p1S, p2Q, p3Q, p3S)
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.btn_continue_edit_answers),
                                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(scrollState).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0x1A000000)).background(Color.White, RoundedCornerShape(16.dp)).padding(20.dp), contentAlignment = Alignment.Center) {
                    Image(painter = painterResource(id = template.imageRes), contentDescription = stringResource(id = R.string.cd_exam_image), modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                }

                SectionCard(tag = stringResource(R.string.setup_thpt_part1_tag), tagBgColor = PrimaryDarkBlue, title = stringResource(R.string.setup_thpt_part1_title), description = stringResource(R.string.setup_thpt_part1_desc)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_question_count), fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            ThptInputField(value = p1QuestionsStr, onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) p1QuestionsStr = it })
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_section_score), fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            ThptInputField(value = p1ScoreStr, onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) p1ScoreStr = it })
                        }
                    }
                }

                SectionCard(tag = stringResource(R.string.setup_thpt_part2_tag), tagBgColor = Color(0xFFD97706), title = stringResource(R.string.setup_thpt_part2_title), description = stringResource(R.string.setup_thpt_part2_desc)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_question_count), fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            ThptInputField(value = p2QuestionsStr, onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) p2QuestionsStr = it })
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PunchClock, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                                Spacer(Modifier.width(2.dp))
                                Text(stringResource(R.string.label_auto_score), fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                            }
                            Spacer(Modifier.height(4.dp))
                            ThptInputField(value = stringResource(R.string.unit_point, formatScore(p2CalculatedScore)), onValueChange = {}, readOnly = true, isDashed = true)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.label_score_per_correct_item), fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FixedScoreBox(label = stringResource(R.string.label_correct_1), score = "0.1")
                        FixedScoreBox(label = stringResource(R.string.label_correct_2), score = "0.25")
                        FixedScoreBox(label = stringResource(R.string.label_correct_3), score = "0.5")
                        FixedScoreBox(label = stringResource(R.string.label_correct_4), score = "1")
                    }
                }

                SectionCard(tag = stringResource(R.string.setup_thpt_part3_tag), tagBgColor = PrimaryDarkBlue, title = stringResource(R.string.setup_thpt_part3_title), description = stringResource(R.string.setup_thpt_part3_desc)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_question_count), fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            ThptInputField(value = p3QuestionsStr, onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) p3QuestionsStr = it })
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_section_score), fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            ThptInputField(value = p3ScoreStr, onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) p3ScoreStr = it })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(60.dp))
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