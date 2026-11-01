package com.example.quizmark.ui.main.home.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.R
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue

// Import các thẻ giao diện từ ScanResultScreen
import com.example.quizmark.ui.main.scan.camera.GreenCorrect
import com.example.quizmark.ui.main.scan.camera.ScannedImageCard
import com.example.quizmark.ui.main.scan.camera.ScoreCircleIndicator
import com.example.quizmark.ui.main.scan.camera.StudentInfoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    viewModel: HistoryDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val record by viewModel.recordDetail.collectAsState()
    val recordBitmap by viewModel.recordBitmap.collectAsState()

    if (record == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = DarkBlue)
        }
        return
    }

    val currentRecord = record!!

    Scaffold(
        containerColor = BackgroundScreen,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.scan_result_title),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Tiêu đề
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GreenCorrect, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(id = R.string.saved_to_history),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorText
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Card Thông tin thí sinh
            StudentInfoCard(
                studentName = currentRecord.studentName.ifBlank { stringResource(R.string.anonymous_student) },
                sbd = currentRecord.studentCode.ifBlank { stringResource(R.string.empty) },
                score = currentRecord.score,
                correct = currentRecord.correctCount,
                wrong = currentRecord.wrongCount,
                blank = currentRecord.blankCount
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Vòng tròn điểm số
            ScoreCircleIndicator(
                score = currentRecord.score,
                correct = currentRecord.correctCount,
                total = currentRecord.totalQuestions
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Card Ảnh bài đã chấm
            ScannedImageCard(
                examCode = currentRecord.examCode,
                bitmap = recordBitmap
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}