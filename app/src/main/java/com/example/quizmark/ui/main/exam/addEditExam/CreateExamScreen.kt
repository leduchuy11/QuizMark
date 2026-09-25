package com.example.quizmark.ui.main.exam.addEditExam

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
import com.example.quizmark.ui.main.exam.templateDownload.OmrTemplate
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.ui.theme.PrimaryDarkBlue
import kotlinx.coroutines.delay

enum class ToastType { SUCCESS, ERROR, WARNING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExamScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSetup: (templateId: Int, examName: String, examCodes: List<String>, customQuestionCount: Int) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // --- STATES ---
    var examName by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf<OmrTemplate?>(null) }
    var examCodeCountStr by remember { mutableStateOf("") }
    var examCodes by remember { mutableStateOf(listOf<String>()) }

    var customQuestionCountStr by remember { mutableStateOf("") }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastType by remember { mutableStateOf(ToastType.ERROR) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2500)
            toastMessage = null
        }
    }

    LaunchedEffect(examCodeCountStr) {
        val count = examCodeCountStr.toIntOrNull() ?: 0
        val safeCount = count.coerceIn(0, 24)
        if (safeCount != examCodes.size) {
            examCodes = List(safeCount) { index -> examCodes.getOrNull(index) ?: "" }
        }
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                        text = stringResource(id = R.string.create_exam_title),
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
                        .background(BackgroundScreen)
                        .padding(20.dp)
                ) {
                    Button(
                        onClick = {
                            if (examName.isBlank() || selectedTemplate == null) {
                                toastMessage = context.getString(R.string.error_fill_all_fields)
                                toastType = ToastType.ERROR
                                return@Button
                            }
                            if (examCodes.isEmpty() || examCodes.any { it.isBlank() }) {
                                toastMessage = context.getString(R.string.error_fill_all_codes)
                                toastType = ToastType.ERROR
                                return@Button
                            }

                            if (examCodes.toSet().size != examCodes.size) {
                                toastMessage = context.getString(R.string.error_duplicate_exam_codes)
                                toastType = ToastType.WARNING
                                return@Button
                            }

                            // Validate riêng cho loại phiếu "Khác" (ID = 6)
                            if (selectedTemplate!!.id == 6) {
                                val qCount = customQuestionCountStr.toIntOrNull() ?: 0
                                if (qCount <= 0 || qCount > 100) {
                                    toastMessage = context.getString(R.string.error_invalid_question_count)
                                    toastType = ToastType.ERROR
                                    return@Button
                                }
                            }

                            val requiredLength = if (selectedTemplate!!.id == 1) 4 else 3
                            val hasInvalidLength = examCodes.any { it.trim().length != requiredLength }

                            if (hasInvalidLength) {
                                toastMessage = if (requiredLength == 4) {
                                    context.getString(R.string.error_exam_code_length_4)
                                } else {
                                    context.getString(R.string.error_exam_code_length_3)
                                }
                                toastType = ToastType.WARNING
                                return@Button
                            }

                            val customQ = if (selectedTemplate!!.id == 6) customQuestionCountStr.toIntOrNull() ?: 0 else 0
                            onNavigateToSetup(selectedTemplate!!.id, examName, examCodes, customQ)
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.btn_create_quiz),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(text = stringResource(id = R.string.input_exam_name_label), fontWeight = FontWeight.Bold, color = ColorText)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = examName,
                    onValueChange = { examName = it },
                    placeholder = { Text(stringResource(id = R.string.input_exam_name_hint), color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(color = ColorText),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryDarkBlue,
                        unfocusedBorderColor = Color.LightGray,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(text = stringResource(id = R.string.input_template_label), fontWeight = FontWeight.Bold, color = ColorText)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showBottomSheet = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_exam),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = selectedTemplate?.name ?: stringResource(id = R.string.input_template_hint),
                        color = if (selectedTemplate == null) Color.Gray else ColorText,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- GIAO DIỆN NHẬP SỐ CÂU (CHỈ HIỆN KHI CHỌN PHIẾU KHÁC ID = 6) ---
                AnimatedVisibility(
                    visible = selectedTemplate?.id == 6,
                    enter = slideInVertically(initialOffsetY = { -20 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -20 }) + fadeOut()
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = stringResource(id = R.string.input_custom_question_count_label), fontWeight = FontWeight.Bold, color = ColorText)

                            // Biến state để theo dõi focus đổi màu viền
                            var isFocused by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(40.dp)
                                    .border(
                                        width = if (isFocused) 2.dp else 1.dp,
                                        color = if (isFocused) PrimaryDarkBlue else Color.LightGray,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .background(Color.White, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = customQuestionCountStr,
                                    onValueChange = { newValue ->
                                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                            customQuestionCountStr = newValue
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { focusState ->
                                            isFocused = focusState.isFocused
                                            if (!focusState.isFocused) {
                                                val count = customQuestionCountStr.toIntOrNull() ?: 0
                                                if (count > 100) customQuestionCountStr = "100"
                                            }
                                        },
                                    textStyle = LocalTextStyle.current.copy(
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorText,
                                        fontSize = 16.sp
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // --- GIAO DIỆN NHẬP SỐ MÃ ĐỀ ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(id = R.string.input_exam_count_label), fontWeight = FontWeight.Bold, color = ColorText)

                    var isFocused by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(40.dp)
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) PrimaryDarkBlue else Color.LightGray,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(Color.White, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = examCodeCountStr,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                    examCodeCountStr = newValue
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    isFocused = focusState.isFocused
                                    if (!focusState.isFocused) {
                                        val count = examCodeCountStr.toIntOrNull() ?: 0
                                        if (count > 24) examCodeCountStr = "24"
                                    }
                                },
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = ColorText,
                                fontSize = 16.sp
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (examCodes.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        examCodes.forEachIndexed { index, code ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.exam_code_prefix, index + 1),
                                    modifier = Modifier
                                        .weight(0.4f)
                                        .padding(start = 16.dp),
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )

                                Box(modifier = Modifier.width(1.dp).height(48.dp).background(Color.LightGray))

                                TextField(
                                    value = code,
                                    onValueChange = { newValue ->
                                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                            val newList = examCodes.toMutableList()
                                            newList[index] = newValue
                                            examCodes = newList
                                        }
                                    },
                                    modifier = Modifier.weight(0.6f),
                                    textStyle = LocalTextStyle.current.copy(color = ColorText),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    placeholder = { Text(stringResource(id = R.string.input_exam_code_hint), color = Color(0xFFE2E8F0)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }

                            if (index < examCodes.lastIndex) {
                                HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
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

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = BackgroundScreen
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.sheet_select_template_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorText
                )
                Text(
                    text = stringResource(id = R.string.sheet_select_template_subtitle),
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                val templates = getTemplateListForSelection()

                templates.forEach { template ->
                    val isSelected = selectedTemplate?.id == template.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) PrimaryDarkBlue else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(
                                color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedTemplate = template
                                showBottomSheet = false
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Image(
                                painter = painterResource(id = template.imageRes),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(50.dp, 60.dp)
                                    .border(1.dp, Color.LightGray)
                            )
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(16.dp)
                                    .offset(x = 4.dp, y = 4.dp)
                                    .background(PrimaryDarkBlue, RoundedCornerShape(8.dp))
                                    .padding(2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = template.name, fontWeight = FontWeight.Bold, color = ColorText)
                            Text(text = template.questionCount, fontSize = 13.sp, color = Color.Gray)
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                selectedTemplate = template
                                showBottomSheet = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryDarkBlue)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CustomToastUI(message: String, type: ToastType) {
    val backgroundColor = when (type) {
        ToastType.SUCCESS -> Color(0xFF3BB54A)
        ToastType.ERROR -> Color(0xFFF75F53)
        ToastType.WARNING -> Color(0xFFF5A623)
    }

    val icon = when (type) {
        ToastType.SUCCESS -> R.drawable.ic_check
        ToastType.ERROR -> R.drawable.ic_fail
        ToastType.WARNING -> R.drawable.ic_warning
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .shadow(6.dp, RoundedCornerShape(8.dp))
            .background(color = backgroundColor, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(Color.White, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = backgroundColor,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = message,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

fun getTemplateListForSelection(): List<OmrTemplate> {
    return listOf(
        OmrTemplate(1, "Phiếu THPT 2025 (bản chuẩn)", "54 câu", "A-D", "A4", R.drawable.form_2025),
        OmrTemplate(2, "Phiếu 20 câu (bản chuẩn)", "20 câu", "A-D", "A4", R.drawable.form_20),
        OmrTemplate(3, "Phiếu 40 câu (bản chuẩn)", "40 câu", "A-D", "A4", R.drawable.form_40),
        OmrTemplate(4, "Phiếu 50 câu (bản chuẩn)", "50 câu", "A-D", "A4", R.drawable.form_50),
        OmrTemplate(5, "Phiếu 120 câu (bản chuẩn)", "120 câu", "A-D", "A4", R.drawable.form_120),
        OmrTemplate(6, "Phiếu khác (Chấm bằng AI)", "Tùy chọn số câu", "A-D", "A4", R.drawable.ic_paper)
    )
}