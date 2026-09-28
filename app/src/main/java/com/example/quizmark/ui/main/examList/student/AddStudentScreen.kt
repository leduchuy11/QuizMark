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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import com.example.quizmark.ui.main.exam.addExam.CustomToastUI
import com.example.quizmark.ui.main.exam.addExam.ToastType
import com.example.quizmark.ui.main.examList.ExamListViewModel
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import kotlinx.coroutines.delay
import java.util.Calendar
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.platform.LocalDensity
import com.example.quizmark.data.model.RosterModel
import com.example.quizmark.data.model.StudentModel
import java.util.UUID

@Composable
fun AddStudentScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExamListViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val rosterList by viewModel.rosterList.collectAsState()

    // Form states
    var studentName by remember { mutableStateOf("") }
    var studentCode by remember { mutableStateOf("") }

    val optionNone = stringResource(id = R.string.option_none)
    var selectedRoster by remember { mutableStateOf(optionNone) }

    var dateOfBirth by remember { mutableStateOf("") }

    val genderOptions = listOf(stringResource(R.string.option_male), stringResource(R.string.option_female))
    var selectedGender by remember { mutableStateOf("") }

    // Toast states
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastType by remember { mutableStateOf(ToastType.ERROR) }

    val saveResult by viewModel.saveResult.collectAsState()
    var navigateAfterToast by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Logic DatePickerDialog
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            dateOfBirth = "$dayOfMonth/${month + 1}/$year"
        },
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
                toastMessage = "Thêm học sinh thành công"
                toastType = ToastType.SUCCESS
                navigateAfterToast = true
            } else {
                toastMessage = "Lỗi: ${result.exceptionOrNull()?.message}"
                toastType = ToastType.ERROR
            }
            viewModel.resetSaveResult()
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
                        text = stringResource(id = R.string.add_student_title),
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
                        .shadow(elevation = 8.dp, spotColor = Color.LightGray, ambientColor = Color.Transparent)
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = {
                            // 1. Validate
                            if (studentName.isBlank() || studentCode.isBlank() || selectedGender.isBlank() || selectedGender == "Chọn" || dateOfBirth.isBlank()) {
                                toastMessage = context.getString(R.string.error_fill_all_blanks)
                                toastType = ToastType.ERROR
                                return@Button
                            }

                            // 2. Lấy thông tin lớp được chọn
                            var selectedRosterModel: RosterModel? = null
                            if (selectedRoster != optionNone) {
                                selectedRosterModel = rosterList.find { it.name == selectedRoster }

                                // LOGIC CHECK TRÙNG SBD TRONG LỚP (Bảo vệ dữ liệu)
                                if (selectedRosterModel != null) {
                                    val isDuplicateSbd = selectedRosterModel.students.any {
                                        it.studentCode == studentCode.trim()
                                    }
                                    if (isDuplicateSbd) {
                                        toastMessage = "SBD $studentCode đã tồn tại trong lớp $selectedRoster!"
                                        toastType = ToastType.ERROR
                                        return@Button
                                    }
                                }
                            }

                            val newStudent = StudentModel(
                                id = UUID.randomUUID().toString(),
                                studentCode = studentCode.trim(),
                                name = studentName.trim(),
                                dob = dateOfBirth,
                                gender = selectedGender.takeIf { it != "Chọn" } ?: ""
                            )

                            viewModel.saveStudent(newStudent, selectedRosterModel)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.btn_add),
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

                // Họ và tên
                StudentInputField(
                    label = stringResource(id = R.string.label_student_name),
                    value = studentName,
                    onValueChange = { studentName = it }
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Số báo danh
                StudentInputField(
                    label = stringResource(id = R.string.label_student_code),
                    value = studentCode,
                    onValueChange = { studentCode = it }
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Thuộc danh sách (Dropdown)
                val rosterOptions = remember(rosterList) { listOf(optionNone) + rosterList.map { it.name } }
                StudentDropdownField(
                    label = stringResource(id = R.string.label_belong_to_roster),
                    selectedOption = selectedRoster,
                    options = rosterOptions,
                    onOptionSelected = { selectedRoster = it }
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Hàng chia đôi: Ngày sinh & Giới tính
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Cột Ngày sinh
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.label_dob),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = ColorText,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
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
                                Text(
                                    text = if (dateOfBirth.isEmpty()) "Chọn ngày" else dateOfBirth,
                                    color = if (dateOfBirth.isEmpty()) Color.Gray else ColorText,
                                    fontSize = 15.sp
                                )
                                Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Cột Giới tính
                    Column(modifier = Modifier.weight(1f)) {
                        StudentDropdownField(
                            label = stringResource(id = R.string.label_gender),
                            selectedOption = if (selectedGender.isEmpty()) "Chọn" else selectedGender,
                            options = genderOptions,
                            onOptionSelected = { selectedGender = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // Hiện Toast
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
fun StudentInputField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = ColorText,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = ColorText),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
@Composable
fun StudentDropdownField(label: String, selectedOption: String, options: List<String>, onOptionSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    // Biến để lưu kích thước của ô bấm
    var textFieldSize by remember { mutableStateOf(Size.Zero) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = ColorText,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier.onGloballyPositioned { coordinates ->
                textFieldSize = coordinates.size.toSize()
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedOption,
                    color = if (selectedOption == "Chọn" || selectedOption == stringResource(id = R.string.option_none)) Color.Gray else ColorText,
                    fontSize = 15.sp
                )
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(with(LocalDensity.current) { textFieldSize.width.toDp() })
                    .background(Color.White)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option, color = ColorText) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}