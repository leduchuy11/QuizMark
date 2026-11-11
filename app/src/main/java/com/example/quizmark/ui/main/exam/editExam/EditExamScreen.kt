package com.example.quizmark.ui.main.exam.editExam

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.R
import com.example.quizmark.ui.main.exam.ExamViewModel
import com.example.quizmark.util.IncompleteAnswerKeyException
import com.example.quizmark.ui.main.exam.addExam.CustomToastUI
import com.example.quizmark.ui.main.exam.addExam.ToastType
import com.example.quizmark.ui.main.exam.addExam.getTemplateListForSelection
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.ui.theme.PrimaryDarkBlue
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExamScreen(
    examId: String,
    initialExamName: String,
    templateId: Int,
    initialExamCodes: List<String>,
    initialCustomQuestionCount: Int,
    onNavigateBack: () -> Unit,
    onNavigateToEditAnswers: (templateId: Int, examName: String, examCodes: List<String>, customQuestionCount: Int) -> Unit,
    onExamDeleted: () -> Unit,
    viewModel: ExamViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var examName by rememberSaveable(examId) { mutableStateOf(initialExamName) }
    val selectedTemplate = remember { getTemplateListForSelection().find { it.id == templateId } }

    var examCodes by rememberSaveable(examId) { mutableStateOf(initialExamCodes) }
    var examCodeCountStr by rememberSaveable(examId) { mutableStateOf(initialExamCodes.size.toString()) }
    var customQuestionCountStr by rememberSaveable(examId) { mutableStateOf(if (initialCustomQuestionCount > 0) initialCustomQuestionCount.toString() else "") }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastType by remember { mutableStateOf(ToastType.ERROR) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var navigateAfterSave by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()

    LaunchedEffect(saveResult) {
        saveResult?.let { result ->
            if (result.isSuccess) {
                toastMessage = context.getString(R.string.toast_update_exam_success)
                toastType = ToastType.SUCCESS
                navigateAfterSave = true
            } else {
                toastMessage = if (result.exceptionOrNull() is IncompleteAnswerKeyException) {
                    context.getString(R.string.edit_codes_need_answers)
                } else context.getString(R.string.error_prefix, result.exceptionOrNull()?.message)
                toastType = ToastType.ERROR
            }
            viewModel.resetSaveResult()
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2500)
            toastMessage = null
            if (navigateAfterSave) onNavigateBack()
        }
    }

    LaunchedEffect(examCodeCountStr) {
        val count = examCodeCountStr.toIntOrNull() ?: 0
        val safeCount = count.coerceIn(0, 24)
        if (safeCount != examCodes.size) {
            examCodes = List(safeCount) { index -> examCodes.getOrNull(index) ?: "" }
        }
    }

    fun validateInputs(): Boolean {
        val error = when {
            examName.isBlank() -> R.string.error_fill_all_fields
            examCodes.isEmpty() || examCodes.any { it.isBlank() } -> R.string.error_fill_all_codes
            examCodes.toSet().size != examCodes.size -> R.string.error_duplicate_exam_codes
            templateId == 6 && (customQuestionCountStr.toIntOrNull() ?: 0) !in 1..100 -> R.string.error_invalid_question_count
            examCodes.any { it.length != if (templateId == 1) 4 else 3 } ->
                if (templateId == 1) R.string.error_exam_code_length_4 else R.string.error_exam_code_length_3
            else -> null
        }
        if (error != null) {
            toastMessage = context.getString(error)
            toastType = ToastType.ERROR
        }
        return error == null
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
                        text = stringResource(id = R.string.edit_exam_title),
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
                        .shadow(elevation = 8.dp, spotColor = Color.LightGray, ambientColor = Color.Transparent)
                        .background(BackgroundScreen)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { if (validateInputs()) showUpdateDialog = true },
                        enabled = !isLoading && !navigateAfterSave,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.btn_save_changes), fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            if (!validateInputs()) return@Button
                            val customQ = if (templateId == 6) customQuestionCountStr.toIntOrNull() ?: 0 else 0

                            onNavigateToEditAnswers(templateId, examName, examCodes, customQ)
                        },
                        enabled = !isLoading && !navigateAfterSave,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_edit), contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = R.string.btn_edit_answers), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = { showDeleteDialog = true },
                        enabled = !isLoading && !navigateAfterSave,
                        modifier = Modifier.fillMaxWidth().height(54.dp).border(width = 1.dp, color = Color(0xFFDC2626),shape = RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFCE8E8)),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_delete) , contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFFDC2626))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(id = R.string.btn_delete_exam), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
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
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_exam), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = selectedTemplate?.name ?: stringResource(id = R.string.unknown_template),
                        color = Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                AnimatedVisibility(
                    visible = templateId == 6,
                    enter = slideInVertically(initialOffsetY = { -20 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -20 }) + fadeOut()
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = stringResource(id = R.string.input_custom_question_count_label), fontWeight = FontWeight.Bold, color = ColorText)
                            var isFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .height(40.dp)
                                    .border(if (isFocused) 2.dp else 1.dp, if (isFocused) PrimaryDarkBlue else Color.LightGray, RoundedCornerShape(12.dp))
                                    .background(Color.White, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = customQuestionCountStr,
                                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) customQuestionCountStr = it },
                                    modifier = Modifier.fillMaxWidth().onFocusChanged {
                                        isFocused = it.isFocused
                                        if (!it.isFocused && (customQuestionCountStr.toIntOrNull() ?: 0) > 100) customQuestionCountStr = "100"
                                    },
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = ColorText, fontSize = 16.sp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(id = R.string.input_exam_count_label), fontWeight = FontWeight.Bold, color = ColorText)
                    var isFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(40.dp)
                            .border(if (isFocused) 2.dp else 1.dp, if (isFocused) PrimaryDarkBlue else Color.LightGray, RoundedCornerShape(12.dp))
                            .background(Color.White, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = examCodeCountStr,
                            onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) examCodeCountStr = it },
                            modifier = Modifier.fillMaxWidth().onFocusChanged {
                                isFocused = it.isFocused
                                if (!it.isFocused && (examCodeCountStr.toIntOrNull() ?: 0) > 24) examCodeCountStr = "24"
                            },
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = ColorText, fontSize = 16.sp),
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
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(id = R.string.exam_code_prefix, index + 1),
                                    modifier = Modifier.weight(0.4f).padding(start = 16.dp),
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

        if (showUpdateDialog) {
            ConfirmExamUpdateDialog(
                summary = stringResource(
                    R.string.dialog_update_exam_text, examName,
                    initialExamCodes.joinToString(", "), examCodes.joinToString(", ")
                ),
                onConfirm = {
                    showUpdateDialog = false
                    viewModel.updateExamDetails(
                        examId, templateId, examName, examCodes,
                        customQuestionCountStr.toIntOrNull() ?: 0
                    )
                },
                onDismiss = { showUpdateDialog = false }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = Color.White,
                title = { Text(text = stringResource(id = R.string.dialog_delete_exam_title), fontWeight = FontWeight.Bold, color = ColorText) },
                text = { Text(text = stringResource(id = R.string.dialog_delete_exam_text), color = Color.Gray) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteExam(examId = examId, isThpt = templateId == 1)
                            onExamDeleted()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text(stringResource(id = R.string.btn_delete_exam), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(id = R.string.btn_cancel), color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            )
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
