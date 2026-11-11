package com.example.quizmark.ui.main.scan.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quizmark.R
import com.example.quizmark.data.model.FinalGradingResult
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchResultScreen(
    results: List<FinalGradingResult>,
    warningMessage: String?,
    onItemClick: (FinalGradingResult) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = BackgroundScreen,
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.grading_results,
                                results.size
                            ),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = ColorText,
                            textAlign = TextAlign.Center
                        )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundScreen
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Khung chứa Danh sách bài thi
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(results) { index, result ->
                        ResultRowItem(index = index + 1, result = result, onClick = { onItemClick(result) })
                        if (index < results.size - 1) {
                            Divider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Khung cảnh báo nét đứt
            if (!warningMessage.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .drawBehind {
                            drawRoundRect(
                                color = Color(0xFFF44336),
                                style = Stroke(
                                    width = 3f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                            )
                        }
                        .background(Color(0xFFFEE2E2).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = warningMessage,
                        color = Color(0xFFD32F2F),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ResultRowItem(index: Int, result: FinalGradingResult,onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = result.errorMessage == null) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cột 1: Vòng tròn Số thứ tự
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFF0F4F8), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = index.toString(), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = DarkBlue)
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Cột 2: SBD (Có tiêu đề nhỏ ở trên)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "SBD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
            Text(text = result.sbd, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorText)
        }

        // Cột 3: Tên (Có tiêu đề nhỏ ở trên)
        Column(modifier = Modifier.weight(1.5f)) {
            Text(text =  stringResource(R.string.name), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
            Text(
                text = result.errorMessage ?: result.studentName,
                fontSize = if (result.errorMessage == null) 16.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (result.errorMessage == null) ColorText else Color(0xFFDC2626),
                maxLines = if (result.errorMessage == null) 1 else 5,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Cột 4: Điểm (Badge màu xanh/đỏ)
        val scoreColor = if (result.score >= 5.0) Color(0xFF16A34A) else Color(0xFFDC2626)
        val scoreBgColor = if (result.score >= 5.0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)

        Box(
            modifier = Modifier
                .background(scoreBgColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (result.errorMessage != null || (result.score == 0.0 && result.sbd == "Lỗi"))
                    stringResource(R.string.grading_error)
                else
                    String.format("%.1f", result.score),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = scoreColor
            )
        }
    }
}
