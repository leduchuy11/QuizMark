package com.example.quizmark.ui.main.exam.addEditExam

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.quizmark.R
import com.example.quizmark.data.model.ThptExamCodeModel
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.ui.theme.PrimaryDarkBlue
import kotlinx.coroutines.delay

@Composable
fun InputThptAnswersScreen(
    p1Q: Int,
    p2Q: Int,
    p3Q: Int,
    examCodes: List<String>,
    onNavigateBack: () -> Unit,
    onNavigateToExamHome: () -> Unit
) {
    val context = LocalContext.current

    var answers by remember { mutableStateOf(mapOf<String, ThptExamCodeModel>()) }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastType by remember { mutableStateOf(ToastType.ERROR) }
    var navigateAfterToast by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(1500)
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
                        text = stringResource(id = R.string.input_thpt_answers_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorText,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .shadow(elevation = 8.dp, spotColor = Color.LightGray, ambientColor = Color.Transparent)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = {
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

                            // TODO: Lưu vào Database
                            toastMessage = context.getString(R.string.toast_create_success)
                            toastType = ToastType.SUCCESS
                            navigateAfterToast = true
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.btn_save),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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
                items(examCodes) { code ->
                    val currentState = answers[code] ?: ThptExamCodeModel(code = code)

                    ThptExamCodeCard(
                        examCode = code,
                        p1Q = p1Q,
                        p2Q = p2Q,
                        p3Q = p3Q,
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

@Composable
fun ThptExamCodeCard(
    examCode: String,
    p1Q: Int,
    p2Q: Int,
    p3Q: Int,
    currentState: ThptExamCodeModel,
    onUpdateState: (ThptExamCodeModel) -> Unit
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

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            // PHẦN 1
            if (p1Q > 0) {
                ThptSectionBlock(title = stringResource(R.string.header_part1)) {
                    for (i in 1..p1Q) {
                        val qKey = i.toString()
                        Part1QuestionRow(
                            qNum = i,
                            selectedOption = currentState.part1[qKey],
                            onOptionSelected = { ans ->
                                val newP1 = currentState.part1.toMutableMap()
                                newP1[qKey] = ans
                                onUpdateState(currentState.copy(part1 = newP1))
                            }
                        )
                    }
                }
            }

            // PHẦN 2
            if (p2Q > 0) {
                ThptSectionBlock(title = stringResource(R.string.header_part2)) {
                    for (i in 1..p2Q) {
                        val qKey = i.toString()
                        Part2QuestionBlock(
                            qNum = i,
                            answers = currentState.part2[qKey] ?: emptyMap(),
                            onSelect = { subQ, ans ->
                                val newP2 = currentState.part2.toMutableMap()
                                val subMap = (newP2[qKey] ?: emptyMap()).toMutableMap()
                                subMap[subQ] = ans
                                newP2[qKey] = subMap
                                onUpdateState(currentState.copy(part2 = newP2))
                            }
                        )
                    }
                }
            }

            // PHẦN 3
            if (p3Q > 0) {
                ThptSectionBlock(title = stringResource(R.string.header_part3)) {
                    for (i in 1..p3Q) {
                        val qKey = i.toString()
                        Part3QuestionRow(
                            qNum = i,
                            text = currentState.part3[qKey] ?: "",
                            onTextChange = { newText ->
                                val newP3 = currentState.part3.toMutableMap()
                                newP3[qKey] = newText
                                onUpdateState(currentState.copy(part3 = newP3))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThptSectionBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ColorText)
        }
        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

// UI Phần 1
@Composable
fun Part1QuestionRow(
    qNum: Int,
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
            text = String.format("%02d", qNum),
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

// UI Phần 2
@Composable
fun Part2QuestionBlock(qNum: Int, answers: Map<String, String>, onSelect: (String, String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = stringResource(R.string.lbl_question_prefix, qNum), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ColorText)
        Spacer(modifier = Modifier.height(8.dp))
        val subQs = listOf("a", "b", "c", "d")
        subQs.forEach { sub ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = sub, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ColorText, modifier = Modifier.width(24.dp))
                Spacer(modifier = Modifier.width(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val trueTxt = stringResource(R.string.lbl_true_option)
                    val falseTxt = stringResource(R.string.lbl_false_option)

                    val isTrueSelected = answers[sub] == trueTxt
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(32.dp)
                            .background(
                                color = if (isTrueSelected) PrimaryDarkBlue else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelect(sub, trueTxt) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = trueTxt,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isTrueSelected) Color.White else Color(0xFF64748B)
                        )
                    }

                    val isFalseSelected = answers[sub] == falseTxt
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(32.dp)
                            .background(
                                color = if (isFalseSelected) Color(0xFFE04F38) else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelect(sub, falseTxt) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = falseTxt,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isFalseSelected) Color.White else Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

// UI Phần 3
@Composable
fun Part3QuestionRow(qNum: Int, text: String, onTextChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.lbl_question_prefix, qNum),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = ColorText,
            modifier = Modifier.width(50.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                .border(1.dp, if (text.isEmpty()) Color(0xFFF75F53) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (text.isEmpty()) {
                Text(text = stringResource(R.string.placeholder_part3), color = Color(0xFFCBD5E1), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            BasicTextField(
                value = text,
                onValueChange = { newVal ->
                    val allowed = setOf('-', ',', '.')
                    if (newVal.length <= 4 && newVal.all { it.isDigit() || allowed.contains(it) }) {
                        onTextChange(newVal)
                    }
                },
                textStyle = LocalTextStyle.current.copy(
                    color = PrimaryDarkBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}