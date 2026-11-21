package com.example.quizmark.ui.main.examList.classroom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.R
import com.example.quizmark.data.model.ClassroomModel
import com.example.quizmark.data.model.HistoryRecord
import com.example.quizmark.ui.main.exam.addExam.CustomToastUI
import com.example.quizmark.ui.main.exam.addExam.ToastType
import com.example.quizmark.ui.main.examList.ExamListViewModel
import com.example.quizmark.ui.main.examList.roster.ActionItemRow
import com.example.quizmark.ui.main.examList.roster.StudentItemRow
import com.example.quizmark.ui.main.home.statistics.ClassScoreOverviewBanner
import com.example.quizmark.ui.main.home.statistics.DuplicateClusterResolutionDialog
import com.example.quizmark.ui.main.home.statistics.DuplicateSbdClustersSection
import com.example.quizmark.ui.main.home.statistics.PushToGoogleDriveDialog
import com.example.quizmark.ui.main.home.statistics.ReassignStudentDialog
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.ui.theme.PrimaryDarkBlue
import com.example.quizmark.util.DuplicateSbdCluster
import com.example.quizmark.util.DuplicateSbdClusterResolver
import com.example.quizmark.util.ExportHelper
import com.example.quizmark.util.ScoreSortOrder
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClassroomDetailScreen(
    classId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddStudent: (String) -> Unit,
    onNavigateToImportStudent: (String) -> Unit,
    onNavigateToEditClassroom: (String) -> Unit,
    onNavigateToHistoryDetail: (String) -> Unit = {},
    viewModel: ExamListViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 0 = Tab Học sinh, 1 = Tab Điểm & Lịch sử chấm của Lớp
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val classroomList by viewModel.classroomList.collectAsState()
    val rosterList by viewModel.rosterList.collectAsState()
    val allHistoryRecords by viewModel.allHistoryRecords.collectAsState()
    val loadedClassroom by viewModel.currentClassroom.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastType by remember { mutableStateOf(ToastType.ERROR) }

    LaunchedEffect(classId) {
        viewModel.loadClassroom(classId)
    }

    LaunchedEffect(saveResult) {
        saveResult?.let { result ->
            if (result.isSuccess) {
                toastMessage = context.getString(R.string.msg_update_success)
                toastType = ToastType.SUCCESS
                viewModel.loadClassroom(classId)
            } else {
                toastMessage = context.getString(R.string.msg_update_error, result.exceptionOrNull()?.message)
                toastType = ToastType.ERROR
            }
            viewModel.resetSaveResult()
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2000)
            toastMessage = null
        }
    }

    val classroom = remember(classroomList, loadedClassroom, classId) {
        classroomList.find { it.id == classId } ?: loadedClassroom ?: ClassroomModel(id = classId)
    }

    // Toàn bộ bản ghi điểm thuộc về Lớp học này (tự động ánh xạ cả bài chấm cũ theo SBD/Danh sách thi)
    val allClassRecords = remember(classroom, allHistoryRecords, rosterList, classroomList) {
        DuplicateSbdClusterResolver.filterRecordsForClassroom(
            classroom = classroom,
            allRecords = allHistoryRecords,
            rosters = rosterList,
            allClassrooms = classroomList
        )
    }

    // Các đợt chấm / danh sách thi mà lớp này đã tham gia
    val availableRostersInClass = remember(allClassRecords) {
        val map = linkedMapOf<String, String>()
        allClassRecords.forEach { rec ->
            val rId = rec.rosterId.ifBlank { "anonymous" }
            val rName = if (rId == "anonymous") "Chấm lẻ" else rec.rosterName.ifBlank { "Đợt thi" }
            map[rId] = rName
        }
        map.toList()
    }

    var selectedRosterFilter by remember { mutableStateOf("ALL") }
    var sortOrder by remember { mutableStateOf(ScoreSortOrder.SCORE_ASC) }
    var showDriveOptionDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<HistoryRecord?>(null) }
    var selectedClusterForSheet by remember { mutableStateOf<DuplicateSbdCluster?>(null) }
    var recordToReassign by remember { mutableStateOf<HistoryRecord?>(null) }

    val filteredClassRecords = remember(allClassRecords, selectedRosterFilter, sortOrder) {
        val byRoster = if (selectedRosterFilter == "ALL") {
            allClassRecords
        } else {
            allClassRecords.filter { (it.rosterId.ifBlank { "anonymous" }) == selectedRosterFilter }
        }
        DuplicateSbdClusterResolver.sortRecords(byRoster, sortOrder)
    }

    val duplicateClusters = remember(filteredClassRecords) {
        DuplicateSbdClusterResolver.buildDuplicateClusters(filteredClassRecords)
    }

    val duplicateSbdSet = remember(duplicateClusters) {
        duplicateClusters.map { it.sbd }.toSet()
    }

    LaunchedEffect(duplicateClusters) {
        val current = selectedClusterForSheet
        if (current != null) {
            selectedClusterForSheet = duplicateClusters.find { it.sbd == current.sbd }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundScreen)
        ) {
            // HEADER LỚP HỌC
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBlue)
                    .padding(top = 16.dp, bottom = 20.dp, start = 20.dp, end = 20.dp),
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.btn_back_text),
                            tint = Color.White
                        )
                    }

                    Text(
                        text = stringResource(id = R.string.detail_classroom_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (allClassRecords.isNotEmpty()) {
                        IconButton(
                            onClick = { showDriveOptionDialog = true },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(40.dp)
                                .background(Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = "Đẩy ảnh lớp lên Google Drive",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = classroom.name,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                val metaText = listOf(classroom.grade, classroom.subject, classroom.schoolYear)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                if (metaText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = metaText,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // THANH CHUYỂN TAB: [Học sinh] | [Điểm & Lịch sử]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ClassroomTabPill(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.People,
                        title = "Học sinh (${classroom.students.size})",
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 }
                    )
                    ClassroomTabPill(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.History,
                        title = "Điểm (${allClassRecords.size})",
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 }
                    )
                }
            }

            if (selectedTabIndex == 0) {
                // ==================== TAB 1: DANH SÁCH HỌC SINH & QUẢN LÝ LỚP ====================
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(id = R.string.section_student_list, classroom.students.size),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorText,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        if (classroom.students.isEmpty()) {
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
                                classroom.students.forEachIndexed { index, student ->
                                    StudentItemRow(
                                        student = student,
                                        removeConfirmText = stringResource(id = R.string.dialog_remove_student_from_classroom_text),
                                        onRemoveConfirm = {
                                            viewModel.removeStudentFromClassroom(student, classroom)
                                        }
                                    )
                                    if (index < classroom.students.size - 1) {
                                        HorizontalDivider(
                                            color = Color(0xFFF1F5F9),
                                            thickness = 1.dp,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

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
                                onClick = { onNavigateToAddStudent(classId) }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            ActionItemRow(
                                iconRes = R.drawable.ic_import_file,
                                title = stringResource(id = R.string.action_import_students),
                                onClick = { onNavigateToImportStudent(classId) }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            ActionItemRow(
                                iconRes = R.drawable.ic_edit,
                                title = stringResource(id = R.string.action_edit_classroom),
                                onClick = { onNavigateToEditClassroom(classId) }
                            )
                        }
                    }

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
                                text = stringResource(id = R.string.action_delete_classroom),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            } else {
                // ==================== TAB 2: ĐIỂM, LỊCH SỬ CHẤM & ĐẨY GOOGLE DRIVE CỦA LỚP ====================
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Lọc theo Đợt thi / Danh sách thi (nếu lớp có nhiều đợt chấm)
                    if (availableRostersInClass.size > 1) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = selectedRosterFilter == "ALL",
                                    onClick = { selectedRosterFilter = "ALL" },
                                    label = { Text("Tất cả đợt thi (${allClassRecords.size})") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryDarkBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                                availableRostersInClass.forEach { (rId, rName) ->
                                    val selected = selectedRosterFilter == rId
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedRosterFilter = rId },
                                        label = { Text(rName) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryDarkBlue,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (filteredClassRecords.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 32.dp)
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .background(Color.White, RoundedCornerShape(16.dp))
                                    .padding(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.History,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Chưa có bài chấm nào",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = ColorText
                                    )
                                }
                            }
                        }
                    } else {
                        // 2. Thẻ Điểm Trung Bình, Thấp nhất, Cao nhất của Lớp
                        item {
                            val avg = filteredClassRecords.sumOf { it.score } / filteredClassRecords.size
                            val min = filteredClassRecords.minOf { it.score }
                            val max = filteredClassRecords.maxOf { it.score }
                            ClassScoreOverviewBanner(
                                classLabel = "Lớp ${classroom.name}",
                                totalCount = filteredClassRecords.size,
                                averageScore = avg,
                                minScore = min,
                                maxScore = max
                            )
                        }

                        // 3. Nút Đẩy ảnh bài chấm của Lớp lên Google Drive & Xuất Excel ngay tại Lớp học
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showDriveOptionDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkBlue),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CloudUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Đẩy Drive",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        ExportHelper.exportToExcelAndShare(
                                            context = context,
                                            records = filteredClassRecords,
                                            emailTo = "",
                                            rosterName = "Lớp ${classroom.name}"
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, PrimaryDarkBlue),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp),
                                        tint = PrimaryDarkBlue
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Xuất Excel",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryDarkBlue,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        // 4. Cụm trùng SBD trong lớp (nếu có)
                        if (duplicateClusters.isNotEmpty()) {
                            item {
                                DuplicateSbdClustersSection(
                                    clusters = duplicateClusters,
                                    onClusterClick = { cluster ->
                                        selectedClusterForSheet = cluster
                                    }
                                )
                            }
                        }

                        // 5. Thanh sắp xếp Điểm: Thấp -> Cao | Cao -> Thấp | Mới nhất | Theo SBD
                        item {
                            ClassroomHistorySortRow(
                                currentOrder = sortOrder,
                                onOrderSelected = { sortOrder = it }
                            )
                        }

                        // 6. Danh sách lịch sử & điểm từng học sinh trong lớp
                        items(filteredClassRecords, key = { it.id }) { record ->
                            val isDuplicated = record.studentCode.trim() in duplicateSbdSet
                            ClassroomHistoryRecordItem(
                                record = record,
                                isDuplicated = isDuplicated,
                                onViewClick = {
                                    if (isDuplicated) {
                                        selectedClusterForSheet = duplicateClusters.find {
                                            it.sbd == record.studentCode.trim()
                                        }
                                    } else {
                                        onNavigateToHistoryDetail(record.id)
                                    }
                                },
                                onDeleteClick = { recordToDelete = record }
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = toastMessage != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        ) {
            toastMessage?.let { CustomToastUI(message = it, type = toastType) }
        }

        // HỘP THOẠI ĐẨY ẢNH BÀI CHẤM CỦA LỚP LÊN GOOGLE DRIVE
        if (showDriveOptionDialog) {
            val groupTitle = buildString {
                append("Lop_${classroom.name}")
                if (selectedRosterFilter != "ALL") {
                    val rName = availableRostersInClass.find { it.first == selectedRosterFilter }?.second
                    if (!rName.isNullOrBlank()) append("_$rName")
                }
            }
            PushToGoogleDriveDialog(
                groupTitle = "Lớp ${classroom.name}",
                recordCount = filteredClassRecords.size,
                onDismiss = { showDriveOptionDialog = false },
                onPushZipFile = {
                    showDriveOptionDialog = false
                    viewModel.pushClassroomZipToDrive(
                        groupTitle = groupTitle,
                        records = filteredClassRecords
                    ) { res ->
                        res.onSuccess { msg ->
                            toastMessage = msg
                            toastType = ToastType.SUCCESS
                        }.onFailure { err ->
                            toastMessage = err.localizedMessage ?: "Lỗi khi tạo tệp đẩy lên Drive"
                            toastType = ToastType.ERROR
                        }
                    }
                },
                onPushIndividualImages = {
                    showDriveOptionDialog = false
                    viewModel.pushClassroomImagesToDrive(
                        groupTitle = groupTitle,
                        records = filteredClassRecords
                    ) { res ->
                        res.onSuccess { msg ->
                            toastMessage = msg
                            toastType = ToastType.SUCCESS
                        }.onFailure { err ->
                            toastMessage = err.localizedMessage ?: "Lỗi khi chuyển ảnh sang Drive"
                            toastType = ToastType.ERROR
                        }
                    }
                }
            )
        }

        // XỬ LÝ CỤM TRÙNG SBD NGAY TRONG LỚP HỌC
        selectedClusterForSheet?.let { cluster ->
            DuplicateClusterResolutionDialog(
                cluster = cluster,
                onDismiss = { selectedClusterForSheet = null },
                onViewDetail = { rec ->
                    selectedClusterForSheet = null
                    onNavigateToHistoryDetail(rec.id)
                },
                onKeepThisOnly = { recToKeep ->
                    viewModel.keepOnlyThisRecordInCluster(cluster, recToKeep)
                    selectedClusterForSheet = null
                },
                onReassignStudent = { rec ->
                    recordToReassign = rec
                },
                onDeleteSingleRecord = { rec ->
                    viewModel.deleteHistoryRecord(rec.id)
                }
            )
        }

        recordToReassign?.let { targetRecord ->
            val suggestions = remember(targetRecord.studentCode, classroom.students, filteredClassRecords) {
                DuplicateSbdClusterResolver.suggestMissingStudents(
                    rosterStudents = classroom.students,
                    records = filteredClassRecords,
                    duplicatedSbd = targetRecord.studentCode
                )
            }
            ReassignStudentDialog(
                record = targetRecord,
                suggestions = suggestions,
                onDismiss = { recordToReassign = null },
                onConfirmReassign = { newCode, newName ->
                    viewModel.reassignRecordInClassroom(classroom, targetRecord, newCode, newName)
                    recordToReassign = null
                }
            )
        }

        // DIALOG XÓA BẢN GHI LỊCH SỬ TRONG LỚP
        if (recordToDelete != null) {
            AlertDialog(
                onDismissRequest = { recordToDelete = null },
                containerColor = Color.White,
                title = { Text(stringResource(R.string.dialog_delete_history_title), fontWeight = FontWeight.Bold, color = ColorText) },
                text = { Text(stringResource(R.string.dialog_delete_history_desc), color = ColorText) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteHistoryRecord(recordToDelete!!.id)
                        recordToDelete = null
                    }) {
                        Text(stringResource(R.string.dialog_confirm), color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recordToDelete = null }) {
                        Text(stringResource(R.string.dialog_cancel), color = ColorText, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = Color.White,
                title = { Text(text = stringResource(id = R.string.action_delete_classroom), fontWeight = FontWeight.Bold, color = ColorText) },
                text = { Text(text = stringResource(id = R.string.dialog_delete_classroom_text), color = Color.Gray) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteClassroom(classroom)
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
private fun ClassroomTabPill(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (selected) Color.White else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) DarkBlue else Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) DarkBlue else Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ClassroomHistorySortRow(
    currentOrder: ScoreSortOrder,
    onOrderSelected: (ScoreSortOrder) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Sắp xếp:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            maxLines = 1,
            softWrap = false
        )

        ClassroomSortChip(
            label = "Điểm: Thấp → Cao",
            icon = Icons.Filled.ArrowUpward,
            selected = currentOrder == ScoreSortOrder.SCORE_ASC,
            onClick = { onOrderSelected(ScoreSortOrder.SCORE_ASC) }
        )
        ClassroomSortChip(
            label = "Điểm: Cao → Thấp",
            icon = Icons.Filled.ArrowDownward,
            selected = currentOrder == ScoreSortOrder.SCORE_DESC,
            onClick = { onOrderSelected(ScoreSortOrder.SCORE_DESC) }
        )
        ClassroomSortChip(
            label = "Mới chấm nhất",
            icon = null,
            selected = currentOrder == ScoreSortOrder.NEWEST,
            onClick = { onOrderSelected(ScoreSortOrder.NEWEST) }
        )
        ClassroomSortChip(
            label = "Theo SBD",
            icon = null,
            selected = currentOrder == ScoreSortOrder.SBD_ASC,
            onClick = { onOrderSelected(ScoreSortOrder.SBD_ASC) }
        )
    }
}

@Composable
private fun ClassroomSortChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Color(0xFFDBEAFE) else Color.White,
        border = BorderStroke(1.dp, if (selected) PrimaryDarkBlue else Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) PrimaryDarkBlue else Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) PrimaryDarkBlue else ColorText,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun ClassroomHistoryRecordItem(
    record: HistoryRecord,
    isDuplicated: Boolean,
    onViewClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()) }
    val scoreColor = if (record.score >= 5.0) Color(0xFF16A34A) else Color(0xFFDC2626)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDuplicated) Color(0xFFFFFBEB) else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (isDuplicated) Color(0xFFF59E0B) else Color(0xFFE2E8F0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Điểm số
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(scoreColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format("%.1f", record.score),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = scoreColor
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDuplicated) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Trùng SBD",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = record.studentName.ifBlank { "Ẩn danh" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorText
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "SBD: ${record.studentCode.ifBlank { "Trống" }} • Mã đề: ${record.examCode.ifBlank { "-" }}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkBlue
                )

                val rosterLabel = if (record.rosterId == "anonymous" || record.rosterName.isBlank()) {
                    "Chấm lẻ"
                } else {
                    record.rosterName
                }
                Text(
                    text = "$rosterLabel • ${dateFormat.format(Date(record.timestamp))}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            IconButton(onClick = onViewClick, modifier = Modifier.size(28.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_eye),
                    contentDescription = "Xem ảnh bài chấm",
                    tint = Color(0xFF616873)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(26.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_ver2),
                    contentDescription = "Xóa",
                    tint = Color(0xFFFA5353)
                )
            }
        }
    }
}
