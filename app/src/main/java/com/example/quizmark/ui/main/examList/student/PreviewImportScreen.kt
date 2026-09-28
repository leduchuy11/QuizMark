package com.example.quizmark.ui.main.examList.student

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.R
import com.example.quizmark.data.model.StudentModel
import com.example.quizmark.ui.main.examList.RosterViewModel
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.util.UUID

@Composable
fun PreviewImportScreen(
    fileUri: Uri,
    rosterId: String?,
    onNavigateBack: () -> Unit,
    onNavigateToExamList: () -> Unit,
    viewModel: RosterViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val rosterList by viewModel.rosterList.collectAsState()

    var parsedStudents by remember { mutableStateOf<List<StudentModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var duplicateCount by remember { mutableStateOf(0) }

    // Logic đọc file Excel trong Background Thread
    LaunchedEffect(fileUri) {
        isLoading = true
        try {
            val list = withContext(Dispatchers.IO) {
                val inputStream: InputStream? = context.contentResolver.openInputStream(fileUri)
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)
                val tempList = mutableListOf<StudentModel>()

                val formatter = org.apache.poi.ss.usermodel.DataFormatter()

                for (i in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(i) ?: continue

                    val sbd = formatter.formatCellValue(row.getCell(0)).trim()
                    val name = formatter.formatCellValue(row.getCell(1)).trim()
                    val dobCell = row.getCell(2)
                    val dob = if (dobCell != null && dobCell.cellType == org.apache.poi.ss.usermodel.CellType.NUMERIC && org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(dobCell)) {
                        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(dobCell.dateCellValue)
                    } else {
                        formatter.formatCellValue(dobCell).trim()
                    }
                    val gender = formatter.formatCellValue(row.getCell(3)).trim()

                    if (sbd.isNotEmpty() && name.isNotEmpty()) {
                        tempList.add(
                            StudentModel(
                                id = UUID.randomUUID().toString(),
                                studentCode = sbd,
                                name = name,
                                dob = dob,
                                gender = gender
                            )
                        )
                    }
                }
                workbook.close()
                inputStream?.close()
                tempList
            }
            parsedStudents = list
        } catch (e: Throwable) {
            e.printStackTrace()
            errorMessage = context.getString(R.string.error_details, e.javaClass.simpleName, e.message ?: "")
        } finally {
            isLoading = false
        }
    }

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
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ColorText)
                }
                Text(
                    text = stringResource(id = R.string.preview_title, parsedStudents.size),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorText,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        },
        bottomBar = {
            if (!isLoading && parsedStudents.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 8.dp, spotColor = Color.LightGray, ambientColor = Color.Transparent)
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = {
                            val existingStudentsInTargetRoster = if (rosterId != null) {
                                rosterList.find { it.id == rosterId }?.students ?: emptyList()
                            } else emptyList()

                            val duplicateSBDs = parsedStudents.filter { newStudent ->
                                existingStudentsInTargetRoster.any { it.studentCode == newStudent.studentCode }
                            }

                            if (duplicateSBDs.isNotEmpty()) {
                                duplicateCount = duplicateSBDs.size
                                showConfirmDialog = true
                            } else {
                                viewModel.importStudents(parsedStudents, rosterId)
                                onNavigateToExamList()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.add_students_count, parsedStudents.size),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DarkBlue)
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = errorMessage!!, color = Color.Red, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header của Bảng
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE2E8F0))
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    Text(text = stringResource(id = R.string.table_header_sbd), fontWeight = FontWeight.Bold, color = ColorText, modifier = Modifier.weight(1f))
                    Text(text = stringResource(id = R.string.table_header_name), fontWeight = FontWeight.Bold, color = ColorText, modifier = Modifier.weight(2f))
                    Text(text = stringResource(id = R.string.table_header_dob), fontWeight = FontWeight.Bold, color = ColorText, modifier = Modifier.weight(1.5f))
                    Text(text = stringResource(id = R.string.table_header_gender), fontWeight = FontWeight.Bold, color = ColorText, modifier = Modifier.weight(1f))
                }

                // Danh sách
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(parsedStudents) { index, student ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (index % 2 == 0) Color.White else Color(0xFFF8FAFC))
                                .border(0.5.dp, Color(0xFFF1F5F9))
                                .padding(horizontal = 8.dp, vertical = 14.dp)
                        ) {
                            Text(text = student.studentCode, color = ColorText, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = student.name, color = ColorText, fontSize = 14.sp, modifier = Modifier.weight(2f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = student.dob, color = ColorText, fontSize = 14.sp, modifier = Modifier.weight(1.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = student.gender, color = ColorText, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // Dialog xác nhận ghi đè
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                containerColor = Color.White,
                title = { Text(text = stringResource(id = R.string.duplicate_warning_title), fontWeight = FontWeight.Bold, color = ColorText) },
                text = {
                    Text(
                        text = stringResource(id = R.string.duplicate_warning_message, duplicateCount),
                        color = Color.Gray
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showConfirmDialog = false
                            viewModel.importStudents(parsedStudents, rosterId)
                            onNavigateToExamList()
                        }
                    ) {
                        Text(text = stringResource(id = R.string.action_overwrite), color = DarkBlue, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text(text = stringResource(id = R.string.action_cancel), color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}