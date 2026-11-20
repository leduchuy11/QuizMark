package com.example.quizmark.ui.main.home.statistics

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.quizmark.util.GoogleDriveHelper
import com.example.quizmark.util.MissingStudentSuggestion
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

    var selectedClusterForSheet by remember { mutableStateOf<DuplicateSbdCluster?>(null) }
    var recordToReassign by remember { mutableStateOf<HistoryRecord?>(null) }

    // Khi danh sách cụm trùng thay đổi (ví dụ giáo viên vừa xử lý xong 1 bài), cập nhật lại BottomSheet
    LaunchedEffect(duplicateClusters) {
        val currentCluster = selectedClusterForSheet
        if (currentCluster != null) {
            selectedClusterForSheet = duplicateClusters.find { it.sbd == currentCluster.sbd }
        }
    }

    // Launcher xin quyền Google Drive (DriveScopes.DRIVE_FILE)
    val driveSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && GoogleDriveHelper.hasDrivePermission(context)) {
            viewModel.uploadCurrentRecordsToDrive()
        }
    }

    // Tỉ lệ chiều rộng của 3 cột (SBD : Tên : Điểm)
    val sbdWeight = 1.2f
    val nameWeight = 2f
    val scoreWeight = 0.8f

    val duplicateSbdSet = remember(duplicateClusters) {
        duplicateClusters.map { it.sbd }.toSet()
    }

    Scaffold(
        containerColor = BackgroundScreen,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.score_list_title),
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
                actions = {
                    // Nút đẩy ảnh lên Google Drive trên góc phải TopBar
                    IconButton(
                        enabled = records.isNotEmpty() && driveUploadState !is DriveUploadState.Uploading,
                        onClick = {
                            if (GoogleDriveHelper.hasDrivePermission(context)) {
                                viewModel.uploadCurrentRecordsToDrive()
                            } else {
                                driveSignInLauncher.launch(GoogleDriveHelper.getDriveSignInIntent(context))
                            }
                        }
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
            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 8.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // 1. THANH ĐẨY ẢNH LÊN GOOGLE DRIVE & TIẾN TRÌNH
                if (records.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    DriveSyncBannerCard(
                        totalRecords = records.size,
                        syncedCount = records.count { it.driveFileId.isNotBlank() },
                        hasUnresolvedDuplicates = duplicateClusters.isNotEmpty(),
                        driveUploadState = driveUploadState,
                        onUploadClick = {
                            if (GoogleDriveHelper.hasDrivePermission(context)) {
                                viewModel.uploadCurrentRecordsToDrive()
                            } else {
                                driveSignInLauncher.launch(GoogleDriveHelper.getDriveSignInIntent(context))
                            }
                        },
                        onOpenDriveFolder = { folderUrl ->
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(folderUrl)))
                            }
                        },
                        onDismissStatus = { viewModel.resetDriveUploadState() }
                    )
                }

                // 2. KHU VỰC GOM CỤM BÀI TRÙNG SBD CẦN XỬ LÝ
                if (duplicateClusters.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    DuplicateSbdClustersSection(
                        clusters = duplicateClusters,
                        onClusterClick = { cluster ->
                            selectedClusterForSheet = cluster
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // HEADER BẢNG
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.column_sbd),
                        fontWeight = FontWeight.Bold,
                        color = ColorText,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(sbdWeight)
                    )
                    Text(
                        text = stringResource(id = R.string.column_name),
                        fontWeight = FontWeight.Bold,
                        color = ColorText,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(nameWeight)
                    )
                    Text(
                        text = stringResource(id = R.string.column_score),
                        fontWeight = FontWeight.Bold,
                        color = ColorText,
                        fontSize = 15.sp,
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
private fun DriveSyncBannerCard(
    totalRecords: Int,
    syncedCount: Int,
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
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (syncedCount == totalRecords && totalRecords > 0)
                            Icons.Filled.CloudDone
                        else
                            Icons.Filled.CloudUpload,
                        contentDescription = null,
                        tint = PrimaryDarkBlue
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lưu trữ ảnh lên Google Drive",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorText
                    )
                    Text(
                        text = if (hasUnresolvedDuplicates)
                            "Nên xử lý xong cụm trùng SBD trước khi đẩy lên Drive ($syncedCount/$totalRecords ảnh đã lưu)"
                        else
                            "Đã đồng bộ $syncedCount/$totalRecords ảnh bài chấm lên thư mục Drive",
                        fontSize = 12.sp,
                        color = if (hasUnresolvedDuplicates) Color(0xFFD97706) else Color.Gray
                    )
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
                        text = if (driveUploadState is DriveUploadState.Uploading) "Đang tải..." else "Đẩy lên Drive",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            when (driveUploadState) {
                is DriveUploadState.Uploading -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    val progress = if (driveUploadState.total > 0) {
                        driveUploadState.current.toFloat() / driveUploadState.total.toFloat()
                    } else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PrimaryDarkBlue,
                        trackColor = Color(0xFFDBEAFE)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Đang tải ảnh ${driveUploadState.current}/${driveUploadState.total} lên Google Drive...",
                        fontSize = 12.sp,
                        color = PrimaryDarkBlue
                    )
                }

                is DriveUploadState.Success -> {
                    Spacer(modifier = Modifier.height(10.dp))
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
                    Spacer(modifier = Modifier.height(10.dp))
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
private fun DuplicateSbdClustersSection(
    clusters: List<DuplicateSbdCluster>,
    onClusterClick: (DuplicateSbdCluster) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        border = BorderStroke(1.dp, Color(0xFFF59E0B))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Phát hiện ${clusters.size} cụm trùng SBD cần xử lý",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF92400E)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Bấm vào từng cụm để đối chiếu ảnh bài làm, chọn giữ bài đúng hoặc gán bài bị tô nhầm sang học sinh chưa có điểm.",
                fontSize = 12.sp,
                color = Color(0xFFB45309)
            )

            Spacer(modifier = Modifier.height(10.dp))

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
                                color = ColorText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val scoresPreview = cluster.records.joinToString(" • ") {
                                "Mã ${it.examCode.ifBlank { "?" }}: ${String.format("%.1fđ", it.score)}"
                            }
                            Text(
                                text = scoresPreview,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = "Xử lý >",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDarkBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateClusterResolutionDialog(
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
                                // Ảnh thu nhỏ của phiếu để giáo viên nhìn nhanh chữ viết tay
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${String.format("%.1f", rec.score)} điểm",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = if (rec.score >= 5.0) Color(0xFF16A34A) else Color(0xFFDC2626)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "• Mã đề ${rec.examCode.ifBlank { "?" }}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DarkBlue
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Đúng ${rec.correctCount} • Sai ${rec.wrongCount} • Trống ${rec.blankCount}",
                                        fontSize = 12.sp,
                                        color = ColorText
                                    )
                                    Text(
                                        text = "Quét lúc: ${timeFormat.format(Date(rec.timestamp))}",
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
                                        Text("Xem ảnh phiếu lớn", fontSize = 12.sp, color = PrimaryDarkBlue)
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

                            // 2 Nút hành động chính cho mỗi phiếu trong cụm
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
                                        text = "Giữ bài này",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A)
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
                                        text = "Đổi SBD / HS khác",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
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
private fun ReassignStudentDialog(
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
                        text = "Học sinh trong danh sách thi CHƯA CÓ ĐIỂM (Bấm để chọn):",
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
                                        Text(
                                            text = "SBD: ${item.student.studentCode}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    if (isLikelyMisbubbled) {
                                        Box(
                                            modifier = Modifier
                                                .background( Color(0xFFDBEAFE), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Gợi ý (lệch 1 số)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryDarkBlue
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
                    text = "Hoặc nhập SBD / Họ tên thủ công:",
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
            .padding(vertical = 14.dp, horizontal = 4.dp),
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
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Cột Tên + dấu đã đồng bộ Drive
        Row(
            modifier = Modifier
                .weight(nameWeight)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = record.studentName.ifBlank { "Ẩn danh" },
                color = ColorText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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

        // Cột Điểm
        Text(
            text = String.format("%.1f", record.score),
            color = if (record.score > 0.0) Color(0xFF16A34A) else Color(0xFFDC2626),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(scoreWeight)
        )
    }
}
