package com.example.quizmark.ui.main.examList.student

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.R
import com.example.quizmark.data.model.RosterModel
import com.example.quizmark.data.model.StudentModel
import com.example.quizmark.ui.main.exam.addExam.CustomToastUI
import com.example.quizmark.ui.main.exam.addExam.ToastType
import com.example.quizmark.ui.main.examList.RosterViewModel
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun EditStudentScreen(
    studentId: String,
    onNavigateBack: () -> Unit,
    viewModel: RosterViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val rosterList by viewModel.rosterList.collectAsState()
    val allStudentsList by viewModel.allStudentsList.collectAsState()

    val rawStudentToEdit = remember(allStudentsList, studentId) {
        allStudentsList.find { it.id == studentId }
    }
    val rawInitialRoster = remember(rosterList, studentId) {
        rosterList.find { roster -> roster.students.any { it.id == studentId } }
    }

    var cachedStudent by remember { mutableStateOf<StudentModel?>(null) }
    var cachedRoster by remember { mutableStateOf<RosterModel?>(null) }

    LaunchedEffect(rawStudentToEdit) {
        if (rawStudentToEdit != null) cachedStudent = rawStudentToEdit
    }
    LaunchedEffect(rawInitialRoster) {
        if (rawInitialRoster != null) cachedRoster = rawInitialRoster
    }

    val studentToEdit = rawStudentToEdit ?: cachedStudent
    val initialRoster = rawInitialRoster ?: cachedRoster

    // Form states
    var studentName by remember { mutableStateOf("") }
    var studentCode by remember { mutableStateOf("") }

    val optionNone = stringResource(id = R.string.option_none)
    var selectedRoster by remember { mutableStateOf(optionNone) }
    var dateOfBirth by remember { mutableStateOf("") }

    val genderOptions = listOf(stringResource(R.string.option_male), stringResource(R.string.option_female))
    var selectedGender by remember { mutableStateOf("") }

    // Đổ dữ liệu cũ vào Form khi tìm thấy học sinh
    LaunchedEffect(studentToEdit, initialRoster) {
        studentToEdit?.let {
            studentName = it.name
            studentCode = it.studentCode
            dateOfBirth = it.dob
            selectedGender = it.gender.ifEmpty { "Chọn" }
        }
        initialRoster?.let {
            selectedRoster = it.name
        }
    }

    // Toast & Dialog states
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastType by remember { mutableStateOf(ToastType.ERROR) }
    val saveResult by viewModel.saveResult.collectAsState()
    var navigateAfterToast by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Date Picker Logic
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth -> dateOfBirth = "$dayOfMonth/${month + 1}/$year" },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(1000)
            toastMessage = null
            if (navigateAfterToast) {
                onNavigateBack()
            }
        }
    }

    LaunchedEffect(saveResult) {
        saveResult?.let { result ->
            if (result.isSuccess) {
                toastMessage = if (isDeleting) {
                    context.getString(R.string.toast_delete_student_success)
                } else {
                    context.getString(R.string.toast_update_student_success)
                }
                toastType = ToastType.SUCCESS
                navigateAfterToast = true
            } else {
                toastMessage = "Lỗi: ${result.exceptionOrNull()?.message}"
                toastType = ToastType.ERROR
            }
            viewModel.resetSaveResult()
        }
    }

    // Bao bọc toàn bộ bằng Box tổng
    Box(modifier = Modifier.fillMaxSize()) {

        if (studentToEdit == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DarkBlue)
            }
        } else {
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
                        IconButton(onClick = onNavigateBack, modifier = Modifier.align(Alignment.CenterStart)) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(id = R.string.btn_back_text), tint = ColorText)
                        }

                        Text(
                            text = stringResource(id = R.string.edit_student_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorText,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        // Nút Xóa trên TopBar
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete_ver2),
                                contentDescription = stringResource(id = R.string.action_delete),
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                bottomBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 8.dp, spotColor = Color.LightGray, ambientColor = Color.Transparent)
                            .background(Color.White)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Button(
                            onClick = {
                                if (studentName.isBlank() || studentCode.isBlank() || selectedGender.isBlank() || selectedGender == "Chọn") {
                                    toastMessage = context.getString(R.string.error_fill_all_blanks)
                                    toastType = ToastType.ERROR
                                    return@Button
                                }

                                var selectedRosterModel: RosterModel? = null
                                if (selectedRoster != optionNone) {
                                    selectedRosterModel = rosterList.find { it.name == selectedRoster }

                                    // Check trùng SBD (bỏ qua chính học sinh này)
                                    if (selectedRosterModel != null) {
                                        val isDuplicateSbd = selectedRosterModel.students.any {
                                            it.studentCode == studentCode.trim() && it.id != studentId
                                        }
                                        if (isDuplicateSbd) {
                                            toastMessage = "SBD $studentCode đã tồn tại trong lớp $selectedRoster!"
                                            toastType = ToastType.ERROR
                                            return@Button
                                        }
                                    }
                                }

                                val updatedStudent = studentToEdit.copy(
                                    studentCode = studentCode.trim(),
                                    name = studentName.trim(),
                                    dob = dateOfBirth,
                                    gender = selectedGender
                                )

                                isDeleting = false
                                viewModel.updateStudent(updatedStudent, initialRoster, selectedRosterModel)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.btn_update),
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

                    StudentInputField(
                        label = stringResource(id = R.string.label_student_name),
                        value = studentName,
                        onValueChange = { studentName = it }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    StudentInputField(
                        label = stringResource(id = R.string.label_student_code),
                        value = studentCode,
                        onValueChange = { studentCode = it }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val rosterOptions = remember(rosterList) { listOf(optionNone) + rosterList.map { it.name } }
                    StudentDropdownField(
                        label = stringResource(id = R.string.label_belong_to_roster),
                        selectedOption = selectedRoster,
                        options = rosterOptions,
                        onOptionSelected = { selectedRoster = it }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.label_dob), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = ColorText, modifier = Modifier.padding(bottom = 8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .clickable { datePickerDialog.show() }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(text = if (dateOfBirth.isEmpty()) "Chọn ngày" else dateOfBirth, color = if (dateOfBirth.isEmpty()) Color.Gray else ColorText, fontSize = 15.sp)
                                    Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            StudentDropdownField(
                                label = stringResource(id = R.string.label_gender),
                                selectedOption = selectedGender.ifEmpty { "Chọn" },
                                options = genderOptions,
                                onOptionSelected = { selectedGender = it }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }


        // Dialog Xóa
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = Color.White,
                title = { Text(text = stringResource(id = R.string.dialog_delete_student_title), fontWeight = FontWeight.Bold, color = ColorText) },
                text = { Text(text = stringResource(id = R.string.dig_delete_student_text), color = Color.Gray) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            isDeleting = true

                            viewModel.deleteStudent(studentId, initialRoster)
                        }
                    ) {
                        Text(text = stringResource(id = R.string.action_delete), color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(id = R.string.btn_cancel), color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Hiện Toast
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