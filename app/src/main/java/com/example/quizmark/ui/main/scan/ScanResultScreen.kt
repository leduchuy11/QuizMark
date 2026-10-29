package com.example.quizmark.ui.main.scan

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quizmark.R
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue

val GreenCorrect = Color(0xFF4CAF50)
val RedWrong = Color(0xFFF44336)
val YellowBlank = Color(0xFFFFC107)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    studentName: String,
    sbd: String,
    examCode: String,
    score: Double,
    correctCount: Int,
    wrongCount: Int,
    blankCount: Int,
    totalQuestions: Int,
    processedImage: Bitmap?,
    onViewDetailsClick: () -> Unit,
    onNextScanClick: () -> Unit,
    onBackClick: () -> Unit
) {

    BackHandler {
        onBackClick()
    }

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
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
        },
        bottomBar = {
            BottomActionRow(onViewDetailsClick, onNextScanClick)
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

            // 1. Tiêu đề trạng thái (Chấm thành công)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GreenCorrect, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.grading_success),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorText
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Card Thông tin thí sinh
            StudentInfoCard(studentName, sbd, score, correctCount, wrongCount, blankCount)

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Vòng tròn điểm số
            ScoreCircleIndicator(score, correctCount, totalQuestions)

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Card Ảnh bài đã chấm
            ScannedImageCard(examCode, processedImage)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StudentInfoCard(
    studentName: String, sbd: String, score: Double, correct: Int, wrong: Int, blank: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Box Điểm / Avatar
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .background(DarkBlue, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = String.format("%.1f", score), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(text = "/ 10", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(text = studentName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorText)

                Text(text = "SBD: $sbd", fontSize = 13.sp, color = Color.Gray)


                // Hàng thống kê
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatItem(GreenCorrect, stringResource(R.string.stat_correct, correct))
                    Spacer(modifier = Modifier.width(12.dp))
                    StatItem(RedWrong, stringResource(R.string.stat_wrong, wrong))
                    Spacer(modifier = Modifier.width(12.dp))
                    StatItem(YellowBlank, stringResource(R.string.stat_blank, blank))
                }
            }
        }
    }
}

@Composable
fun StatItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ScoreCircleIndicator(score: Double, correct: Int, total: Int) {
    val progress = (score / 10).toFloat()
    val arcColor = if (score >= 5.0) Color(0xFFF9A825) else RedWrong // Đổi màu nếu điểm dưới trung bình

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(200.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            // Vòng tròn nền
            drawArc(
                color = Color(0xFFE0E0E0),
                startAngle = 0f, sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Vòng tròn điểm
            drawArc(
                color = arcColor,
                startAngle = -90f, sweepAngle = progress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(R.string.score_label), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 2.sp)
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = DarkBlue)) {
                        append(String.format("%.1f", score))
                    }
                    withStyle(style = SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Gray)) {
                        append("/10")
                    }
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.correct_questions, correct, total), fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ScannedImageCard(examCode: String, bitmap: Bitmap?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.graded_exam_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = ColorText
                )

                Text(
                    text = stringResource(R.string.exam_code_label, examCode),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBlue
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(504.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Scanned Paper",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("Không có ảnh", modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun BottomActionRow(onViewDetailsClick: () -> Unit, onNextScanClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nút con mắt xem chi tiết
        IconButton(
            onClick = onViewDetailsClick,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFDDE9F6))
        ) {
            Icon(
                Icons.Filled.RemoveRedEye,
                contentDescription = "Xem chi tiết",
                tint = DarkBlue
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Nút Quét phiếu kế tiếp
        Button(
            onClick = onNextScanClick,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = "Phiếu kế tiếp", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}