package com.example.quizmark.ui.main.examList.roster

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
import com.example.quizmark.data.model.StudentModel
import com.example.quizmark.ui.main.exam.addExam.CustomToastUI
import com.example.quizmark.ui.main.exam.addExam.ToastType
import com.example.quizmark.ui.main.examList.ExamListViewModel
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.util.unAccent
import kotlinx.coroutines.delay

@Composable
fun AddStudentToRosterScreen(
    rosterId: String,
    onNavigateBack: () -> Unit,
    viewModel: ExamListViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val currentRoster by viewModel.currentRoster.collectAsState()
    val allStudents by viewModel.allStudentsList.collectAsState()
    val allRosters by viewModel.rosterList.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()

    var selectedStudentIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showConflictDialog by remember { mutableStateOf(false) }
    var duplicateCount by remember { mutableStateOf(0) }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastType by remember { mutableStateOf(ToastType.ERROR) }

    // Biến trạng thái cho thanh tìm kiếm
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(rosterId) {
        viewModel.loadRoster(rosterId)
    }

    // Logic xử lý kết quả lưu
    LaunchedEffect(saveResult) {
        saveResult?.let { result ->
            if (result.isSuccess) {
                toastMessage = context.getString(R.string.msg_update_success)
                toastType = ToastType.SUCCESS
                delay(1000)
                viewModel.resetSaveResult()
                onNavigateBack()
            } else {
                toastMessage = context.getString(R.string.msg_update_error, result.exceptionOrNull()?.message)
                toastType = ToastType.ERROR
                viewModel.resetSaveResult()
            }
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null && toastType == ToastType.ERROR) {
            delay(2000)
            toastMessage = null
        }
    }

    if (currentRoster == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = DarkBlue)
        }
        return
    }

    // 1. Logic lọc học sinh "Tự do"
    val freeStudents = remember(allStudents, allRosters) {
        val studentIdsInAnyRoster = allRosters.flatMap { it.students }.map { it.id }.toSet()
        allStudents.filter { it.id !in studentIdsInAnyRoster }
    }

    // 2. Logic Tìm kiếm
    val displayedStudents = remember(freeStudents, searchQuery) {
        if (searchQuery.isBlank()) {
            freeStudents
        } else {
            val queryRaw = searchQuery.trim().lowercase().unAccent()
            freeStudents.filter { student ->
                val nameRaw = student.name.lowercase().unAccent()
                nameRaw.contains(queryRaw)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = BackgroundScreen,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 4.dp, spotColor = Color.LightGray, ambientColor = Color.Transparent)
                        .background(Color.White)
                ) {
                    // Toolbar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.align(Alignment.CenterStart)) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ColorText)
                        }
                        Text(
                            text = stringResource(id = R.string.add_student_to_roster_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorText,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Thanh tìm kiếm
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(44.dp)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = ColorText),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(text = context.getString(R.string.text_search), color = Color.Gray, fontSize = 15.sp)
                                    }
                                    innerTextField()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Clear",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (freeStudents.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 8.dp, spotColor = Color.LightGray, ambientColor = Color.Transparent)
                            .background(Color.White)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Button(
                            onClick = {
                                if (selectedStudentIds.isEmpty()) return@Button

                                val selectedStudents = freeStudents.filter { it.id in selectedStudentIds }

                                // KIỂM TRA TRÙNG LẶP NỘI BỘ
                                val hasInternalDuplicates = selectedStudents
                                    .groupBy { it.studentCode }
                                    .any { it.value.size > 1 }

                                if (hasInternalDuplicates) {
                                    toastMessage = context.getString(R.string.error_internal_duplicate_sbd)
                                    toastType = ToastType.ERROR
                                    return@Button
                                }

                                // Kiểm tra trùng SBD với học sinh ĐANG CÓ
                                val existingSbdList = currentRoster!!.students.map { it.studentCode }
                                val duplicates = selectedStudents.filter { it.studentCode in existingSbdList }

                                if (duplicates.isNotEmpty()) {
                                    duplicateCount = duplicates.size
                                    showConflictDialog = true
                                } else {
                                    val updatedRoster = currentRoster!!.copy(
                                        students = currentRoster!!.students + selectedStudents
                                    )
                                    viewModel.saveRoster(updatedRoster)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            enabled = selectedStudentIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkBlue,
                                disabledContainerColor = Color.LightGray
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.btn_add_selected_count, selectedStudentIds.size),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            if (freeStudents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(id = R.string.empty_free_students), color = Color.Gray, fontSize = 15.sp)
                }
            } else if (displayedStudents.isEmpty()) {
                // Hiển thị khi tìm kiếm không ra kết quả
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text(text = context.getString(R.string.result_search), color = Color.Gray, fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color.White)
                ) {
                    itemsIndexed(displayedStudents) { index, student ->
                        val isSelected = student.id in selectedStudentIds

                        SelectableStudentItem(
                            student = student,
                            isSelected = isSelected,
                            onClick = {
                                selectedStudentIds = if (isSelected) {
                                    selectedStudentIds - student.id
                                } else {
                                    selectedStudentIds + student.id
                                }
                            }
                        )

                        if (index < displayedStudents.size - 1) {
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }

        // Dialog Xử lý ghi đè
        if (showConflictDialog) {
            AlertDialog(
                onDismissRequest = { showConflictDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(12.dp),
                title = { Text(text = stringResource(id = R.string.dialog_conflict_sbd_title), fontWeight = FontWeight.Bold, color = ColorText) },
                text = { Text(text = stringResource(id = R.string.dialog_conflict_sbd_message, duplicateCount), color = Color.Gray) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showConflictDialog = false
                            val selectedStudents = freeStudents.filter { it.id in selectedStudentIds }

                            val updatedStudents = currentRoster!!.students.toMutableList()
                            selectedStudents.forEach { newStudent ->
                                val existingIndex = updatedStudents.indexOfFirst { it.studentCode == newStudent.studentCode }
                                if (existingIndex != -1) {
                                    updatedStudents[existingIndex] = newStudent
                                } else {
                                    updatedStudents.add(newStudent)
                                }
                            }
                            viewModel.saveRoster(currentRoster!!.copy(students = updatedStudents))
                        }
                    ) {
                        Text(text = stringResource(id = R.string.btn_overwrite), color = DarkBlue, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConflictDialog = false }) {
                        Text(stringResource(id = R.string.btn_cancel), color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Animated Custom Toast
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
fun SelectableStudentItem(
    student: StudentModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val words = student.name.trim().split("\\s+".toRegex())
    val initials = if (words.isNotEmpty() && words[0].isNotEmpty()) {
        if (words.size == 1) {
            words.first().take(1).uppercase()
        } else {
            words.first().take(1).uppercase() + words.last().take(1).uppercase()
        }
    } else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFFF3F7FC), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initials, color = DarkBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = student.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = ColorText)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = student.studentCode, fontSize = 13.sp, color = Color(0xFF94A3B8))
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = DarkBlue,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.RadioButtonUnchecked,
                contentDescription = "Unselected",
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}