package com.example.quizmark.ui.main.home.statistics

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.example.quizmark.R
import com.example.quizmark.data.model.HistoryRecord
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.ColorText3
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.ui.theme.PrimaryDarkBlue
import com.example.quizmark.util.DuplicateSbdCluster
import com.example.quizmark.util.MissingStudentSuggestion
import com.example.quizmark.util.ScoreSortOrder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreListScreen(
    viewModel: ScoreListViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val records by viewModel.records.collectAsState()
    val duplicateClusters by viewModel.duplicateClusters.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val driveUploadState by viewModel.driveUploadState.collectAsState()

    val availableClasses by viewModel.availableClasses.collectAsState()
    val selectedClassId by viewModel.selectedClassId.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val averageScore by viewModel.averageScore.collectAsState()
    val minScore by viewModel.minScore.collectAsState()
    val maxScore by viewModel.maxScore.collectAsState()

    var selectedClusterForSheet by remember { mutableStateOf<DuplicateSbdCluster?>(null) }
    var recordToReassign by remember { mutableStateOf<HistoryRecord?>(null) }
    var showDriveOptionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(duplicateClusters) {
        val currentCluster = selectedClusterForSheet
        if (currentCluster != null) {
            selectedClusterForSheet = duplicateClusters.find { it.sbd == currentCluster.sbd }
        }
    }

    val sbdWeight = 1.15f
    val nameWeight = 2.05f
    val scoreWeight = 0.8f

    val duplicateSbdSet = remember(duplicateClusters) {
        duplicateClusters.map { it.sbd }.toSet()
    }

    val currentClassLabel = remember(selectedClassId, availableClasses) {
        when (selectedClassId) {
            "ALL" -> "Tất cả các lớp"
            "UNASSIGNED" -> "Chưa phân lớp"
            else -> availableClasses.find { it.first == selectedClassId }?.second?.let { "Lớp $it" } ?: "Lớp học"
        }
    }

    Scaffold(
        containerColor = BackgroundScreen,
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.score_list_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = ColorText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (selectedClassId != "ALL") {
                            Text(
                                text = currentClassLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryDarkBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
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
                actions = {
                    IconButton(
                        enabled = records.isNotEmpty() && driveUploadState !is DriveUploadState.Uploading,
                        onClick = { showDriveOptionDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudUpload,
                            contentDescription = "Tải ảnh lên Google Drive",
                            tint = if (records.isNotEmpty()) PrimaryDarkBlue else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundScreen)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 6.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // 1. THANH CHỌN LỚP HỌC (FILTER CHIPS THEO LỚP)
                if (availableClasses.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedClassId == "ALL",
                            onClick = { viewModel.selectClassFilter("ALL") },
                            label = {
                                Text(
                                    text = "Tất cả lớp",
                                    fontWeight = if (selectedClassId == "ALL") FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryDarkBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                        availableClasses.forEach { (cid, cname) ->
                            val selected = selectedClassId == cid
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.selectClassFilter(cid) },
                                label = {
                                    Text(
                                        text = "Lớp $cname",
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryDarkBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // 2. THẺ ĐIỂM TRUNG BÌNH & THỐNG KÊ NHANH CỦA LỚP ĐANG XEM
                if (records.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ClassScoreOverviewBanner(
                        classLabel = currentClassLabel,
                        totalCount = records.size,
                        averageScore = averageScore,
                        minScore = minScore,
                        maxScore = maxScore
                    )
                }

                // 3. THANH ĐẨY ẢNH LÊN GOOGLE DRIVE
                if (records.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    DriveSyncBannerCard(
                        totalRecords = records.size,
                        hasUnresolvedDuplicates = duplicateClusters.isNotEmpty(),
                        driveUploadState = driveUploadState,
                        onUploadClick = { showDriveOptionDialog = true },
                        onOpenDriveFolder = { folderUrl ->
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(folderUrl)))
                            }
                        },
                        onDismissStatus = { viewModel.resetDriveUploadState() }
                    )
                }

                // 4. KHU VỰC GOM CỤM BÀI TRÙNG SBD CẦN XỬ LÝ
                if (duplicateClusters.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    DuplicateSbdClustersSection(
                        clusters = duplicateClusters,
                        onClusterClick = { cluster ->
                            selectedClusterForSheet = cluster
                        }
                    )
                }

                // 5. THANH SẮP XẾP ĐIỂM (TỪ THẤP ĐẾN CAO / CAO XUỐNG THẤP / THEO SBD / THEO TÊN)
                if (records.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ScoreSortFilterRow(
                        currentOrder = sortOrder,
                        onOrderSelected = { viewModel.updateSortOrder(it) }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // HEADER BẢNG
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.column_sbd),
                        fontWeight = FontWeight.Bold,
                        color = ColorText,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(sbdWeight)
                    )
                    Text(
                        text = stringResource(id = R.string.column_name),
                        fontWeight = FontWeight.Bold,
                        color = ColorText,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(nameWeight)
                    )
                    Text(
                        text = stringResource(id = R.string.column_score),
                        fontWeight = FontWeight.Bold,
                        color = ColorText,
                        fontSize = 14.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(scoreWeight)
                    )
                }

                HorizontalDivider(color = ColorText3, thickness = 1.dp)

                // NỘI DUNG BẢNG
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DarkBlue)
                    }
                } else if (records.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(id = R.string.empty_student_list),
                            color = Color.Gray,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(records, key = { it.id }) { record ->
                            val isDuplicated = record.studentCode.trim() in duplicateSbdSet
                            ScoreRowItem(
                                record = record,
                                isDuplicated = isDuplicated,
                                sbdWeight = sbdWeight,
                                nameWeight = nameWeight,
                                scoreWeight = scoreWeight,
                                onClick = {
                                    if (isDuplicated) {
                                        selectedClusterForSheet = duplicateClusters.find {
                                            it.sbd == record.studentCode.trim()
                                        }
                                    } else if (record.id.isNotBlank()) {
                                        onNavigateToDetail(record.id)
                                    }
                                }
                            )
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }

    // HỘP THOẠI CHỌN CÁCH ĐẨY LÊN GOOGLE DRIVE (CHUYỂN TIẾP SANG MÀN HÌNH GOOGLE DRIVE)
    if (showDriveOptionDialog) {
        PushToGoogleDriveDialog(
            groupTitle = currentClassLabel,
            recordCount = records.size,
            onDismiss = { showDriveOptionDialog = false },
            onPushZipFile = {
                showDriveOptionDialog = false
                viewModel.pushZipPackageToGoogleDriveScreen()
            },
            onPushIndividualImages = {
                showDriveOptionDialog = false
                viewModel.pushImagesToGoogleDriveScreen()
            }
        )
    }

    // BOTTOM SHEET XỬ LÝ CỤM BÀI TRÙNG SBD
    selectedClusterForSheet?.let { cluster ->
        DuplicateClusterResolutionDialog(
            cluster = cluster,
            onDismiss = { selectedClusterForSheet = null },
            onViewDetail = { rec ->
                selectedClusterForSheet = null
                onNavigateToDetail(rec.id)
            },
            onKeepThisOnly = { recToKeep ->
                viewModel.keepOnlyThisRecordInCluster(cluster, recToKeep)
                selectedClusterForSheet = null
            },
            onReassignStudent = { rec ->
                recordToReassign = rec
            },
            onDeleteSingleRecord = { rec ->
                viewModel.deleteDuplicateRecord(rec.id)
            }
        )
    }

    // DIALOG GÁN LẠI HỌC SINH / ĐỔI SBD CHO BÀI BỊ TÔ NHẦM
    recordToReassign?.let { targetRecord ->
        val suggestions = remember(targetRecord.studentCode, records) {
            viewModel.getMissingStudentSuggestions(targetRecord.studentCode)
        }
        ReassignStudentDialog(
            record = targetRecord,
            suggestions = suggestions,
            onDismiss = { recordToReassign = null },
            onConfirmReassign = { newCode, newName ->
                viewModel.reassignRecordToStudent(targetRecord, newCode, newName)
                recordToReassign = null
            }
        )
    }
}

@Composable
fun ClassScoreOverviewBanner(
    classLabel: String,
    totalCount: Int,
    averageScore: Double,
    minScore: Double,
    maxScore: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFBAE6FD))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Hàng 1: Tên lớp + Tổng số bài
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = classLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDarkBlue,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE0F2FE)
                ) {
                    Text(
                        text = "$totalCount bài",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0369A1),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFE0F2FE), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Hàng 2: Chia đều 3 cột cho Điểm TB / Thấp nhất / Cao nhất (không bao giờ bị cắt ...)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OverviewMetricColumn(
                    modifier = Modifier.weight(1f),
                    label = "Điểm TB",
                    value = String.format("%.2f", averageScore),
                    valueColor = PrimaryDarkBlue
                )
                VerticalDivider(
                    modifier = Modifier.height(30.dp),
                    thickness = 1.dp,
                    color = Color(0xFFE2E8F0)
                )
                OverviewMetricColumn(
                    modifier = Modifier.weight(1f),
                    label = "Thấp nhất",
                    value = String.format("%.1f", minScore),
                    valueColor = Color(0xFFDC2626)
                )
                VerticalDivider(
                    modifier = Modifier.height(30.dp),
                    thickness = 1.dp,
                    color = Color(0xFFE2E8F0)
                )
                OverviewMetricColumn(
                    modifier = Modifier.weight(1f),
                    label = "Cao nhất",
                    value = String.format("%.1f", maxScore),
                    valueColor = Color(0xFF16A34A)
                )
            }
        }
    }
}

@Composable
private fun OverviewMetricColumn(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
    }
}

@Composable
fun ScoreSortFilterRow(
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

        SortChipButton(
            label = "Điểm: Thấp → Cao",
            icon = Icons.Filled.ArrowUpward,
            selected = currentOrder == ScoreSortOrder.SCORE_ASC,
            onClick = { onOrderSelected(ScoreSortOrder.SCORE_ASC) }
        )
        SortChipButton(
            label = "Điểm: Cao → Thấp",
            icon = Icons.Filled.ArrowDownward,
            selected = currentOrder == ScoreSortOrder.SCORE_DESC,
            onClick = { onOrderSelected(ScoreSortOrder.SCORE_DESC) }
        )
        SortChipButton(
            label = "Theo SBD",
            icon = null,
            selected = currentOrder == ScoreSortOrder.SBD_ASC,
            onClick = { onOrderSelected(ScoreSortOrder.SBD_ASC) }
        )
        SortChipButton(
            label = "Theo Tên",
            icon = null,
            selected = currentOrder == ScoreSortOrder.NAME_ASC,
            onClick = { onOrderSelected(ScoreSortOrder.NAME_ASC) }
        )
    }
}

@Composable
private fun SortChipButton(
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
fun PushToGoogleDriveDialog(
    groupTitle: String,
    recordCount: Int,
    onDismiss: () -> Unit,
    onPushZipFile: () -> Unit,
    onPushIndividualImages: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column {
                Text(
                    text = "Đẩy bài chấm lên Google Drive",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ColorText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$groupTitle • $recordCount bài đã chấm",
                    fontSize = 13.sp,
                    color = PrimaryDarkBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Lựa chọn 1: Đẩy thành 1 tệp nén .zip duy nhất
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPushZipFile() },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.5.dp, PrimaryDarkBlue)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(PrimaryDarkBlue, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FolderZip,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tệp nén (.zip)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = PrimaryDarkBlue
                            )
                            Text(
                                text = "$recordCount ảnh + Bảng điểm",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Lựa chọn 2: Đẩy từng tệp ảnh .jpg rời
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPushIndividualImages() },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE2E8F0), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Image,
                                contentDescription = null,
                                tint = DarkBlue
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Từng ảnh (.jpg)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = ColorText
                            )
                            Text(
                                text = "$recordCount ảnh rời",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun DriveSyncBannerCard(
    totalRecords: Int,
    hasUnresolvedDuplicates: Boolean,
    driveUploadState: DriveUploadState,
    onUploadClick: () -> Unit,
    onOpenDriveFolder: (String) -> Unit,
    onDismissStatus: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFDBEAFE))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudUpload,
                        contentDescription = null,
                        tint = PrimaryDarkBlue
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Google Drive ($totalRecords bài)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorText
                    )
                    if (hasUnresolvedDuplicates) {
                        Text(
                            text = "Còn bài trùng SBD",
                            fontSize = 11.sp,
                            color = Color(0xFFD97706),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onUploadClick,
                    enabled = driveUploadState !is DriveUploadState.Uploading,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkBlue),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (driveUploadState is DriveUploadState.Uploading) "Đang xử lý..." else "Đẩy Drive",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            when (driveUploadState) {
                is DriveUploadState.Uploading -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PrimaryDarkBlue,
                        trackColor = Color(0xFFDBEAFE)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Đang đóng gói ảnh và chuyển sang màn hình Google Drive...",
                        fontSize = 12.sp,
                        color = PrimaryDarkBlue
                    )
                }

                is DriveUploadState.OpenedDriveApp -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFECFDF5), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = driveUploadState.message,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF166534),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismissStatus) {
                            Text("Đóng", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                is DriveUploadState.Success -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFECFDF5), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Đã tải ${driveUploadState.summary.uploadedCount} ảnh vào thư mục ${driveUploadState.summary.folderName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF166534)
                            )
                        }
                        TextButton(
                            onClick = { onOpenDriveFolder(driveUploadState.summary.folderWebLink) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = PrimaryDarkBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mở Drive", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkBlue)
                        }
                    }
                }

                is DriveUploadState.Error -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = driveUploadState.message,
                            fontSize = 12.sp,
                            color = Color(0xFF991B1B),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismissStatus) {
                            Text("Đóng", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                DriveUploadState.Idle -> Unit
            }
        }
    }
}

@Composable
fun DuplicateSbdClustersSection(
    clusters: List<DuplicateSbdCluster>,
    onClusterClick: (DuplicateSbdCluster) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        border = BorderStroke(1.dp, Color(0xFFF59E0B))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Trùng SBD (${clusters.size} cụm)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF92400E)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            clusters.forEach { cluster ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onClusterClick(cluster) },
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "SBD ${cluster.sbd}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = Color(0xFFB45309)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${cluster.studentName} (${cluster.records.size} bài trùng)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = ColorText
                            )
                            val scoresPreview = cluster.records.joinToString(" • ") {
                                "Mã ${it.examCode.ifBlank { "?" }}: ${String.format("%.1fđ", it.score)}"
                            }
                            Text(
                                text = scoresPreview,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Xử lý >",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDarkBlue,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DuplicateClusterResolutionDialog(
    cluster: DuplicateSbdCluster,
    onDismiss: () -> Unit,
    onViewDetail: (HistoryRecord) -> Unit,
    onKeepThisOnly: (HistoryRecord) -> Unit,
    onReassignStudent: (HistoryRecord) -> Unit,
    onDeleteSingleRecord: (HistoryRecord) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column {
                Text(
                    text = "Cụm trùng SBD: ${cluster.sbd}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ColorText
                )
                Text(
                    text = "Đang khớp với: ${cluster.studentName} (${cluster.records.size} phiếu)",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cluster.records, key = { it.id }) { rec ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val previewPath = rec.localOriginalImagePath.ifBlank { rec.localImagePath }
                                Box(
                                    modifier = Modifier
                                        .size(64.dp, 86.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE2E8F0))
                                        .clickable { onViewDetail(rec) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (previewPath.isNotBlank() && File(previewPath).exists()) {
                                        SubcomposeAsyncImage(
                                            model = File(previewPath),
                                            contentDescription = "Ảnh phiếu",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.RemoveRedEye,
                                            contentDescription = null,
                                            tint = Color.Gray
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${String.format("%.1f", rec.score)} điểm • Mã đề ${rec.examCode.ifBlank { "?" }}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = if (rec.score >= 5.0) Color(0xFF16A34A) else Color(0xFFDC2626)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Đúng ${rec.correctCount} • Sai ${rec.wrongCount} • Trống ${rec.blankCount}",
                                        fontSize = 12.sp,
                                        color = ColorText
                                    )
                                    Text(
                                        text = timeFormat.format(Date(rec.timestamp)),
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    TextButton(
                                        onClick = { onViewDetail(rec) },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.RemoveRedEye,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                            tint = PrimaryDarkBlue
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Xem ảnh phiếu",
                                            fontSize = 12.sp,
                                            color = PrimaryDarkBlue
                                        )
                                    }
                                }

                                IconButton(onClick = { onDeleteSingleRecord(rec) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Xóa phiếu này",
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onKeepThisOnly(rec) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF16A34A)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Giữ bài",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                Button(
                                    onClick = { onReassignStudent(rec) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkBlue),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Đổi SBD",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", fontWeight = FontWeight.Bold, color = ColorText)
            }
        }
    )
}

@Composable
fun ReassignStudentDialog(
    record: HistoryRecord,
    suggestions: List<MissingStudentSuggestion>,
    onDismiss: () -> Unit,
    onConfirmReassign: (newStudentCode: String, newStudentName: String) -> Unit
) {
    var manualSbd by remember(record.id) { mutableStateOf(record.studentCode) }
    var manualName by remember(record.id) { mutableStateOf(record.studentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column {
                Text(
                    text = "Gán bài chấm sang học sinh khác",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = ColorText
                )
                Text(
                    text = "Phiếu đang chọn: ${String.format("%.1f điểm", record.score)} • Mã đề ${record.examCode}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (suggestions.isNotEmpty()) {
                    Text(
                        text = "Học sinh chưa có điểm:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDarkBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(suggestions, key = { it.student.id.ifBlank { it.student.studentCode } }) { item ->
                            val isLikelyMisbubbled = item.hammingDistance == 1
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onConfirmReassign(item.student.studentCode, item.student.name)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isLikelyMisbubbled) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    1.dp,
                                    if (isLikelyMisbubbled) PrimaryDarkBlue else Color(0xFFE2E8F0)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.student.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = ColorText
                                        )
                                        val classInfo = if (item.student.className.isNotBlank()) " • Lớp ${item.student.className}" else ""
                                        Text(
                                            text = "SBD: ${item.student.studentCode}$classInfo",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    if (isLikelyMisbubbled) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFDBEAFE), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Lệch 1 số",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryDarkBlue,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = "Nhập thủ công:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorText
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = manualSbd,
                    onValueChange = { manualSbd = it },
                    label = { Text("Số báo danh (SBD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualName,
                    onValueChange = { manualName = it },
                    label = { Text("Họ và tên học sinh") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmReassign(manualSbd, manualName) },
                enabled = manualSbd.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkBlue)
            ) {
                Text("Lưu SBD mới", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = Color.Gray)
            }
        }
    )
}

@Composable
fun ScoreRowItem(
    record: HistoryRecord,
    isDuplicated: Boolean = false,
    sbdWeight: Float,
    nameWeight: Float,
    scoreWeight: Float,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDuplicated) Color(0xFFFFFBEB) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cột SBD + icon cảnh báo nếu đang thuộc cụm trùng
        Row(
            modifier = Modifier.weight(sbdWeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDuplicated) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Trùng SBD",
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = record.studentCode.ifBlank { "Trống" },
                color = if (isDuplicated) Color(0xFFB45309) else Color.Gray,
                fontWeight = if (isDuplicated) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Cột Tên + Lớp + Mã đề
        Column(
            modifier = Modifier
                .weight(nameWeight)
                .padding(end = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = record.studentName.ifBlank { "Ẩn danh" },
                    color = ColorText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (record.driveFileId.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.CloudDone,
                        contentDescription = "Đã lưu trên Google Drive",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            val subInfo = buildList {
                if (record.className.isNotBlank()) add("Lớp ${record.className}")
                if (record.examCode.isNotBlank()) add("Mã đề ${record.examCode}")
            }.joinToString(" • ")
            if (subInfo.isNotBlank()) {
                Text(
                    text = subInfo,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        // Cột Điểm
        Text(
            text = String.format("%.1f", record.score),
            color = if (record.score >= 5.0) Color(0xFF16A34A) else Color(0xFFDC2626),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(scoreWeight)
        )
    }
}
