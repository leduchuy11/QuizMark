package com.example.quizmark.ui.main.examList.roster

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.R
import com.example.quizmark.data.model.StudentModel
import com.example.quizmark.ui.main.examList.RosterViewModel
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue

@Composable
fun RosterDetailScreen(
    rosterId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddStudent: (String) -> Unit,
    onNavigateToEditRoster: (String) -> Unit,
    viewModel: RosterViewModel = hiltViewModel()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val currentRoster by viewModel.currentRoster.collectAsState()

    LaunchedEffect(rosterId) {
        viewModel.loadRoster(rosterId)
    }

    if (currentRoster == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundScreen),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = DarkBlue)
        }
        return
    }

    val roster = currentRoster!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundScreen)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBlue)
                .padding(top = 16.dp, bottom = 32.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(40.dp)
                        .background(Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.btn_back_text),
                        tint = Color.White
                    )
                }

                Text(
                    text = stringResource(id = R.string.detail_roster_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // DÙNG DỮ LIỆU THẬT
            Text(
                text = roster.name,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(
                    id = R.string.grade_subject_year_format,
                    roster.grade,
                    roster.subject,
                    roster.schoolYear
                ),
                fontSize = 14.sp,
                color = Color(0xFF94A3B8)
            )
        }

        // --- PHẦN 2: NỘI DUNG ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 2.1. DANH SÁCH HỌC SINH
            item {
                Text(
                    text = stringResource(id = R.string.section_student_list, roster.students.size),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorText,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (roster.students.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.empty_student_list),
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .background(Color.White, RoundedCornerShape(16.dp))
                    ) {
                        roster.students.forEachIndexed { index, student ->
                            StudentItemRow(student = student)
                            if (index < roster.students.size - 1) {
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }

            // 2.2. HÀNH ĐỘNG
            item {
                Text(
                    text = stringResource(id = R.string.section_actions),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorText,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                        .background(Color.White, RoundedCornerShape(16.dp))
                ) {
                    ActionItemRow(
                        iconRes = R.drawable.ic_add_person,
                        title = stringResource(id = R.string.action_add_student),
                        onClick = { onNavigateToAddStudent(rosterId) }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    ActionItemRow(
                        iconRes = R.drawable.ic_edit,
                        title = stringResource(id = R.string.action_edit_roster),
                        onClick = { onNavigateToEditRoster(rosterId) }
                    )
                }
            }

            // 2.3. XOÁ LỚP
            item {
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .border(width = 1.dp, color = Color(0xFFDC2626), shape = RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFCE8E8)),
                    elevation = ButtonDefaults.buttonElevation(0.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.action_delete_roster),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = Color.White,
                title = { Text(text = stringResource(id = R.string.action_delete_roster), fontWeight = FontWeight.Bold, color = ColorText) },
                text = { Text(text = stringResource(id = R.string.dialog_delete_roster_text), color = Color.Gray) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteRoster(rosterId)
                            onNavigateBack()
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.btn_delete),
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(id = R.string.btn_cancel), color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
fun StudentItemRow(student: StudentModel) {
    var showDeleteStudentDialog by remember { mutableStateOf(false) }
    val initials = student.name.split(" ").takeLast(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFE6F0FD), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initials, color = DarkBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = student.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ColorText)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = stringResource(id = R.string.sbd_prefix, student.studentCode), fontSize = 13.sp, color = Color.Gray)
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFFFCE8E8), RoundedCornerShape(8.dp))
                .clickable { showDeleteStudentDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_delete_person),
                contentDescription = stringResource(id = R.string.action_delete_student),
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showDeleteStudentDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteStudentDialog = false },
            containerColor = Color.White,
            title = { Text(text = stringResource(id = R.string.action_delete_student), fontWeight = FontWeight.Bold, color = ColorText) },
            text = { Text(text = stringResource(id = R.string.dialog_delete_student_text), color = Color.Gray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteStudentDialog = false
                        // TODO: Thêm logic xóa học sinh khỏi lớp
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.btn_delete),
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteStudentDialog = false }) {
                    Text(stringResource(id = R.string.btn_cancel), color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun ActionItemRow(iconRes: Int, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFF3F7FC), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = DarkBlue,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = ColorText, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}