package com.example.quizmark.ui.main.scan

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.R
import com.example.quizmark.data.model.ExamModel
import com.example.quizmark.data.model.RosterModel
import com.example.quizmark.ui.main.exam.addExam.CustomToastUI
import com.example.quizmark.ui.main.exam.addExam.ToastType
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.ColorText2
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.ui.theme.PrimaryDarkBlue
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel = hiltViewModel(),
    onNavigateToCamera: (String, String?) -> Unit,
    onNavigateToGallery: (String, String?) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val examList by viewModel.examList.collectAsState()
    val rosterList by viewModel.rosterList.collectAsState()

    val selectedExam by viewModel.selectedExam.collectAsState()
    val selectedRoster by viewModel.selectedRoster.collectAsState()

    // Quản lý trạng thái Dropdown
    var expandedExam by remember { mutableStateOf(false) }
    var expandedRoster by remember { mutableStateOf(false) }

    // Quản lý Custom Toast cảnh báo
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastType by remember { mutableStateOf(ToastType.ERROR) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2000)
            toastMessage = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = BackgroundScreen,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundScreen)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_avatar_app),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(4.dp, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "QuizMark",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorText
                    )
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
                Spacer(modifier = Modifier.height(14.dp))

                // --- TEXT HƯỚNG DẪN 2 DÒNG ---
                Text(
                    text = stringResource(id = R.string.scan_header_title),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorText,
                    lineHeight = 42.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.scan_header_subtitle),
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // --- TÙY CHỌN NÂNG CAO: DANH SÁCH THI (Tùy chọn) ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Info",
                        tint = DarkBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.scan_roster_label),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBlue
                    )


                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = context.getString(R.string.scan_roster_label_2),
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedRoster,
                    onExpandedChange = { expandedRoster = !expandedRoster },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedRoster?.name ?: stringResource(id = R.string.scan_roster_no_selection),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRoster) },
                        textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            focusedTextColor = ColorText,
                            unfocusedTextColor = ColorText,
                            focusedBorderColor = PrimaryDarkBlue,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expandedRoster,
                        onDismissRequest = { expandedRoster = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        // Option: Không chọn lớp
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.scan_roster_no_selection), color = ColorText) },
                            onClick = {
                                viewModel.selectRoster(null)
                                expandedRoster = false
                            }
                        )

                        rosterList.forEach { roster ->
                            DropdownMenuItem(
                                text = { Text(roster.name, color = ColorText) },
                                onClick = {
                                    viewModel.selectRoster(roster)
                                    expandedRoster = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- CHỌN ĐỀ THI (BẮT BUỘC)[cite: 4] ---
                Text(
                    text = stringResource(id = R.string.scan_exam_label),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.scan_exam_subtitle),
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedExam,
                    onExpandedChange = { expandedExam = !expandedExam },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedExam?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.scan_exam_placeholder),
                                color = Color.Gray.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Normal
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedExam) },
                        textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            focusedTextColor = ColorText,
                            unfocusedTextColor = ColorText,
                            focusedBorderColor = PrimaryDarkBlue,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expandedExam,
                        onDismissRequest = { expandedExam = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        examList.forEach { exam ->
                            DropdownMenuItem(
                                text = { Text(exam.name, color = ColorText) },
                                onClick = {
                                    viewModel.selectExam(exam)
                                    expandedExam = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(44.dp))

                // --- NÚT MỞ CAMERA[cite: 4] ---
                Button(
                    onClick = {
                        if (selectedExam == null) {
                            toastMessage = context.getString(R.string.error_exam_not_selected)
                            toastType = ToastType.ERROR
                        } else {
                            // Truyền thêm templateId hoặc cờ isThpt nếu sau này Core chấm điểm cần biết dạng phiếu
                            onNavigateToCamera(selectedExam!!.id, selectedRoster?.id)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(painter = painterResource(R.drawable.ic_camera), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.btn_open_camera),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- NÚT CHỌN TỪ THƯ VIỆN[cite: 4] ---
                OutlinedButton(
                    onClick = {
                        if (selectedExam == null) {
                            toastMessage = context.getString(R.string.error_exam_not_selected)
                            toastType = ToastType.ERROR
                        } else {
                            onNavigateToGallery(selectedExam!!.id, selectedRoster?.id)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryDarkBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(painter = painterResource(R.drawable.ic_img), contentDescription = null, tint = DarkBlue, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.btn_open_gallery),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBlue
                    )
                }

                Spacer(modifier = Modifier.height(32.dp)) // Khoảng cách an toàn với Bottom Navigation
            }
        }

        // Custom Toast hiển thị thông báo lỗi nếu chưa chọn Đề thi
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