package com.example.quizmark.ui.main.examList.roster

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.R
import com.example.quizmark.data.model.RosterModel
import com.example.quizmark.data.model.StudentModel
import com.example.quizmark.ui.main.exam.addExam.CustomToastUI
import com.example.quizmark.ui.main.exam.addExam.ToastType
import com.example.quizmark.ui.main.examList.ExamListViewModel
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.ui.theme.PrimaryDarkBlue
import com.example.quizmark.util.ExamRosterSelector
import kotlinx.coroutines.delay

@Composable
fun AddStudentToRosterScreen(
    rosterId: String,
    onNavigateBack: () -> Unit,
    viewModel: ExamListViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val currentRoster by viewModel.currentRoster.collectAsState()
    val rosterList by viewModel.rosterList.collectAsState()
    val allStudents by viewModel.allStudentsList.collectAsState()
    val classroomList by viewModel.classroomList.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()

    var selectedStudentIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingSaveRoster by remember { mutableStateOf<RosterModel?>(null) }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastType by remember { mutableStateOf(ToastType.ERROR) }

    LaunchedEffect(rosterId) {
        viewModel.loadRoster(rosterId)
    }

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

    val roster = remember(rosterList, currentRoster, rosterId) {
        rosterList.find { it.id == rosterId } ?: currentRoster
    }

    if (roster == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = DarkBlue)
        }
        return
    }

    // Ẩn những thí sinh ĐÃ có mặt trong chính danh sách thi hiện tại
    val existingIds = remember(roster) { roster.students.map { it.id }.toSet() }

    // Học sinh chưa thuộc lớp học nào
    val unassignedStudents = remember(allStudents, classroomList, existingIds) {
        val idsInClassrooms = classroomList.flatMap { it.students }.map { it.id }.toSet()
        allStudents.filter { it.id !in idsInClassrooms && it.id !in existingIds }
    }

    val selectionResult = remember(classroomList, unassignedStudents, selectedStudentIds, roster) {
        ExamRosterSelector.buildSelection(
            classrooms = classroomList,
            selectedIds = selectedStudentIds,
            extraStudents = unassignedStudents,
            existingStudents = roster.students
        )
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
                    IconButton(onClick = onNavigateBack, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ColorText)
                    }
                    Text(
                        text = stringResource(id = R.string.add_student_to_roster_title),
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
                            if (selectionResult.students.isEmpty()) return@Button
                            val combinedStudents = roster.students + selectionResult.students
                            val combinedClassIds = (roster.sourceClassIds + selectionResult.sourceClassIds).distinct()
                            val combinedClassNames = (roster.sourceClassNames + selectionResult.sourceClassNames).distinct()
                            val updatedRoster = roster.copy(
                                students = combinedStudents,
                                sourceClassIds = combinedClassIds,
                                sourceClassNames = combinedClassNames
                            )
                            if (selectionResult.duplicateSbds.isNotEmpty()) {
                                pendingSaveRoster = updatedRoster
                            } else {
                                viewModel.saveRoster(updatedRoster)
                            }
                        },
                        enabled = selectionResult.students.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkBlue,
                            disabledContainerColor = Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.add_students_count, selectionResult.students.size),
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.add_from_classes_subtitle),
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                ClassStudentPickerSection(
                    classrooms = classroomList,
                    selectedStudentIds = selectedStudentIds,
                    onSelectionChange = { selectedStudentIds = it },
                    excludedStudentIds = existingIds,
                    duplicateSbds = selectionResult.duplicateSbds
                )

                if (unassignedStudents.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(id = R.string.section_free_students, unassignedStudents.size),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorText
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        unassignedStudents.forEach { student ->
                            UnassignedStudentCheckboxRow(
                                student = student,
                                isChecked = student.id in selectedStudentIds,
                                onToggle = {
                                    selectedStudentIds = ExamRosterSelector.toggleStudent(student.id, selectedStudentIds)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        pendingSaveRoster?.let { rosterToSave ->
            AlertDialog(
                onDismissRequest = { pendingSaveRoster = null },
                containerColor = Color.White,
                title = {
                    Text(
                        text = stringResource(id = R.string.warning_duplicate_sbd_title),
                        fontWeight = FontWeight.Bold,
                        color = ColorText
                    )
                },
                text = {
                    Text(
                        text = stringResource(
                            id = R.string.warning_duplicate_sbd_desc,
                            selectionResult.duplicateSbds.size,
                            selectionResult.duplicateSbds.keys.take(6).joinToString(", ")
                        ),
                        color = Color.Gray
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingSaveRoster = null
                            viewModel.saveRoster(rosterToSave)
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.btn_continue_save),
                            color = DarkBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingSaveRoster = null }) {
                        Text(
                            text = stringResource(id = R.string.btn_cancel),
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
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
private fun UnassignedStudentCheckboxRow(
    student: StudentModel,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    val initials = remember(student.name) {
        val words = student.name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        when {
            words.isEmpty() -> ""
            words.size == 1 -> words.first().take(1).uppercase()
            else -> "${words.first().take(1)}${words.last().take(1)}".uppercase()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (isChecked) Color(0xFF93C5FD) else Color(0xFFE2E8F0),
                RoundedCornerShape(12.dp)
            )
            .background(if (isChecked) Color(0xFFF0F9FF) else Color.White)
            .clickable { onToggle() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = PrimaryDarkBlue,
                uncheckedColor = Color(0xFF94A3B8)
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFE6F0FD), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initials, color = DarkBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = student.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ColorText)
            Text(
                text = stringResource(id = R.string.sbd_prefix, student.studentCode),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
