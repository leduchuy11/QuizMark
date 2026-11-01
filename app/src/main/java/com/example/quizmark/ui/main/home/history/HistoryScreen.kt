package com.example.quizmark.ui.main.home.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.R
import com.example.quizmark.data.model.HistoryRecord
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import com.example.quizmark.ui.auth.CustomToastUI
import com.example.quizmark.ui.theme.ColorText2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var recordToDelete by remember { mutableStateOf<HistoryRecord?>(null) }

    // State cho DatePicker
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Lắng nghe Toast từ ViewModel
    val toastMessage by viewModel.toastMessage.collectAsState()
    val toastType by viewModel.toastType.collectAsState()

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            kotlinx.coroutines.delay(1500)
            viewModel.clearToast()
        }
    }

    Scaffold(
        containerColor = BackgroundScreen,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.history_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = ColorText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.btn_back_text),
                            tint = ColorText,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                actions = { Spacer(modifier = Modifier.width(48.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundScreen)
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // KHU VỰC BỘ LỌC
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // 1. Dropdown Chọn Lớp
                    RosterDropdownMenu(
                        availableRosters = uiState.availableRosters,
                        selectedRosterId = uiState.selectedRosterId,
                        onRosterSelected = { viewModel.updateRosterFilter(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Chọn Ngày Từ - Đến
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DateFilterBox(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.date_from),
                            dateString = dateFormat.format(Date(uiState.startDate)),
                            onClick = { showStartDatePicker = true }
                        )
                        DateFilterBox(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.date_to),
                            dateString = dateFormat.format(Date(uiState.endDate)),
                            onClick = { showEndDatePicker = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 6.dp)

                // KHU VỰC DANH SÁCH KẾT QUẢ
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.groupedRecords.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.empty_history), color = Color.Gray, fontSize = 16.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        val groupedEntries = uiState.groupedRecords.entries.toList()

                        groupedEntries.forEachIndexed { index, entry ->
                            val dateHeader = entry.key
                            val records = entry.value
                            val isFirstGroup = (index == 0)

                            if (!isFirstGroup) {
                                item {
                                    HorizontalDivider(
                                        color = Color(0xFFE5E7EB),
                                        thickness = 8.dp
                                    )
                                }
                            }

                            // 2. Header Ngày (Hôm nay, 16/08/2026...)
                            item {
                                Text(
                                    text = dateHeader,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2A51A1),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF3F4F6))
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                HorizontalDivider(
                                    color = Color(0xFFE5E7EB),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }

                            // 3. Danh sách Item trong ngày đó
                            items(records) { record ->
                                HistoryListItem(
                                    record = record,
                                    onViewClick = { onNavigateToDetail(record.id) },
                                    onDeleteClick = { recordToDelete = record }
                                )
                                if (record != records.last()) {
                                    HorizontalDivider(
                                        color = Color(0xFFE5E7EB),
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(start = 70.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Giao diện Toast trượt từ trên xuống
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .zIndex(1f)
            ) {
                toastMessage?.let {
                    CustomToastUI(message = it, type = toastType)
                }
            }
        }
        

        // --- DIALOGS XÓA ---
        if (recordToDelete != null) {
            AlertDialog(
                onDismissRequest = { recordToDelete = null },
                title = { Text(stringResource(R.string.dialog_delete_history_title), fontWeight = FontWeight.Bold, color = ColorText) },
                text = { Text(stringResource(R.string.dialog_delete_history_desc), color = ColorText) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteRecord(recordToDelete!!.id)
                        recordToDelete = null
                    }) { Text(stringResource(R.string.dialog_confirm), color = Color.Red, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { recordToDelete = null }) { Text(stringResource(R.string.dialog_cancel), color = ColorText, fontWeight = FontWeight.Bold) }
                },
                containerColor = Color.White
            )
        }

        // --- BỘ CHỌN NGÀY (Material 3 DatePicker) ---
        if (showStartDatePicker) {
            CustomDatePickerDialog(
                initialDate = uiState.startDate,
                onDismiss = { showStartDatePicker = false },
                onDateSelected = { timeInMillis ->
                    viewModel.updateDateFilter(start = timeInMillis, end = uiState.endDate)
                    showStartDatePicker = false
                }
            )
        }
        if (showEndDatePicker) {
            CustomDatePickerDialog(
                initialDate = uiState.endDate,
                onDismiss = { showEndDatePicker = false },
                onDateSelected = { timeInMillis ->
                    viewModel.updateDateFilter(start = uiState.startDate, end = timeInMillis)
                    showEndDatePicker = false
                }
            )
        }
    }
}

// ================= CÁC COMPONENT GIAO DIỆN CON =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterDropdownMenu(
    availableRosters: List<Pair<String, String>>,
    selectedRosterId: String,
    onRosterSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Tìm tên để hiển thị
    val selectedText = when (selectedRosterId) {
        "ALL" -> stringResource(R.string.filter_all)
        "anonymous" -> stringResource(R.string.filter_anonymous)
        else -> availableRosters.find { it.first == selectedRosterId }?.second ?: ""
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            leadingIcon = { Icon(Icons.Filled.Group, contentDescription = null, tint = ColorText2) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = ColorText
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ColorText,
                unfocusedTextColor = ColorText,
                focusedContainerColor = Color(0xFFEDEEF3),
                unfocusedContainerColor = Color(0xFFEDEEF3),
                unfocusedBorderColor = Color(0xFFE1E2E8),
                focusedBorderColor = Color(0xFFE1E2E8)
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.filter_all), color = ColorText) },
                onClick = { onRosterSelected("ALL"); expanded = false }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.filter_anonymous), color = ColorText) },
                onClick = { onRosterSelected("anonymous"); expanded = false }
            )
            availableRosters.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name, color = ColorText) },
                    onClick = { onRosterSelected(id); expanded = false }
                )
            }
        }
    }
}

@Composable
fun DateFilterBox(modifier: Modifier, label: String, dateString: String, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .border(1.dp, Color(0xFFE1E2E8), RoundedCornerShape(12.dp))
            .background(Color(0xFFEDEEF3), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = dateString, fontSize = 14.sp, color = ColorText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HistoryListItem(record: HistoryRecord, onViewClick: () -> Unit, onDeleteClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cột 1: Khung bo góc hiển thị Điểm số
        val scoreColor = if (record.score != 0.0) Color(0xFF16A34A) else Color(0xFFDC2626)
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color = scoreColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = String.format("%.1f", record.score), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = scoreColor)
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Cột 2: Tên & SBD
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (record.studentName.isBlank()) stringResource(R.string.anonymous_student) else record.studentName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.student_code_format, if (record.studentCode.isBlank()) "Trống" else record.studentCode),
                fontSize = 13.sp,
                color = Color.Gray
            )
        }

        // Cột 3: Nút Xem và Xóa
        IconButton(onClick = onViewClick, modifier = Modifier.size(25.dp)) {
            Icon(painter = painterResource(R.drawable.ic_eye), contentDescription = "Xem", tint = Color(
                0xFF616873
            )
            )

        }
        Spacer(modifier = Modifier.width(10.dp))
        IconButton(onClick = onDeleteClick, modifier = Modifier.size(23.dp)) {
            Icon(painter = painterResource(R.drawable.ic_delete_ver2), contentDescription = "Xóa", tint = Color(
                0xFFFA5353
            )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(initialDate: Long, onDismiss: () -> Unit, onDateSelected: (Long) -> Unit) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { onDateSelected(it) }
            }) { Text(stringResource(R.string.dialog_confirm),color = ColorText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel),color = ColorText) } },
        colors = DatePickerDefaults.colors(containerColor = Color.White)
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                containerColor = Color.White,
                titleContentColor = Color.Black,
                headlineContentColor = Color.Black,
                weekdayContentColor = Color.Black,
                dayContentColor = Color.Black,
                yearContentColor = Color.Black,
                currentYearContentColor = Color.Black,
                selectedDayContainerColor = Color(0xFF2196F3),
                selectedDayContentColor = Color.White,
                todayContentColor = Color(0xFF2196F3)
            )
        )
    }
}