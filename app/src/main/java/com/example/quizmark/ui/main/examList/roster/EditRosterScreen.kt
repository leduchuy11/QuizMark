package com.example.quizmark.ui.main.examList.roster

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.quizmark.data.model.RosterModel
import com.example.quizmark.data.model.StudentModel
import com.example.quizmark.ui.main.exam.addExam.CustomToastUI
import com.example.quizmark.ui.main.exam.addExam.ToastType
import com.example.quizmark.ui.main.examList.RosterViewModel
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import kotlinx.coroutines.delay

@Composable
fun EditRosterScreen(
    rosterId: String,
    onNavigateBack: () -> Unit,
    viewModel: RosterViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    var rosterName by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var schoolYear by remember { mutableStateOf("") }

    // Mảng lưu danh sách sinh viên cũ để khi "Lưu" không bị mất học sinh
    var existingStudents by remember { mutableStateOf<List<StudentModel>>(emptyList()) }

    val scrollState = rememberScrollState()

    val isLoading by viewModel.isLoading.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()
    val currentRoster by viewModel.currentRoster.collectAsState()

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastType by remember { mutableStateOf(ToastType.ERROR) }
    var navigateAfterToast by remember { mutableStateOf(false) }

    LaunchedEffect(rosterId) {
        viewModel.loadRoster(rosterId)
    }

    LaunchedEffect(currentRoster) {
        currentRoster?.let { roster ->
            rosterName = roster.name
            grade = roster.grade
            subject = roster.subject
            schoolYear = roster.schoolYear
            existingStudents = roster.students
        }
    }

    // Lắng nghe kết quả cập nhật Firebase
    LaunchedEffect(saveResult) {
        saveResult?.let { result ->
            if (result.isSuccess) {
                toastMessage = context.getString(R.string.toast_update_list_success)
                toastType = ToastType.SUCCESS
                navigateAfterToast = true
            } else {
                toastMessage = context.getString(R.string.error_prefix, result.exceptionOrNull()?.message)
                toastType = ToastType.ERROR
            }
            viewModel.resetSaveResult()
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(1000)
            toastMessage = null
            if (navigateAfterToast) {
                onNavigateBack()
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
                        text = stringResource(id = R.string.edit_roster_title),
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
                            if (isLoading) return@Button

                            if (rosterName.isBlank() || grade.isBlank() || subject.isBlank() || schoolYear.isBlank()) {
                                toastMessage = context.getString(R.string.error_fill_all_blanks)
                                toastType = ToastType.ERROR
                                return@Button
                            }

                            val updatedRoster = RosterModel(
                                id = rosterId,
                                name = rosterName.trim(),
                                grade = grade.trim(),
                                subject = subject.trim(),
                                schoolYear = schoolYear.trim(),
                                students = existingStudents
                            )
                            viewModel.saveRoster(updatedRoster)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = stringResource(id = R.string.btn_save_changes),
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
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                RosterInputRow(
                    label = stringResource(id = R.string.label_roster_name),
                    value = rosterName,
                    onValueChange = { rosterName = it }
                )

                RosterInputRow(
                    label = stringResource(id = R.string.label_grade),
                    value = grade,
                    onValueChange = { grade = it },
                    inputModifier = Modifier.width(100.dp)
                )

                RosterInputRow(
                    label = stringResource(id = R.string.label_subject),
                    value = subject,
                    onValueChange = { subject = it }
                )

                RosterInputRow(
                    label = stringResource(id = R.string.label_school_year),
                    value = schoolYear,
                    onValueChange = { schoolYear = it }
                )

                Spacer(modifier = Modifier.height(40.dp))
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