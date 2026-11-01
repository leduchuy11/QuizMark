package com.example.quizmark.ui.main.home.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.R
import com.example.quizmark.data.model.HistoryRecord
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.ColorText3
import com.example.quizmark.ui.theme.DarkBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreListScreen(
    viewModel: ScoreListViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val records by viewModel.records.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Tỉ lệ chiều rộng của 3 cột (SBD : Tên : Điểm)
    val sbdWeight = 1.2f
    val nameWeight = 2f
    val scoreWeight = 0.8f

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
                actions = { Spacer(modifier = Modifier.width(48.dp)) },
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
                // HEADER BẢNG
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = stringResource(id = R.string.column_sbd), fontWeight = FontWeight.Bold, color = ColorText, fontSize = 15.sp, modifier = Modifier.weight(sbdWeight))
                    Text(text = stringResource(id = R.string.column_name), fontWeight = FontWeight.Bold, color = ColorText, fontSize = 15.sp, modifier = Modifier.weight(nameWeight))
                    Text(text = stringResource(id = R.string.column_score), fontWeight = FontWeight.Bold, color = ColorText, fontSize = 15.sp, textAlign = TextAlign.End, modifier = Modifier.weight(scoreWeight))
                }

                HorizontalDivider(color = ColorText3, thickness = 1.dp)

                // NỘI DUNG BẢNG
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DarkBlue)
                    }
                } else if (records.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(id = R.string.empty_student_list), color = Color.Gray, fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(records) { record ->
                            ScoreRowItem(
                                record = record,
                                sbdWeight = sbdWeight,
                                nameWeight = nameWeight,
                                scoreWeight = scoreWeight
                            )
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreRowItem(record: HistoryRecord, sbdWeight: Float, nameWeight: Float, scoreWeight: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cột SBD
        Text(
            text = record.studentCode.ifBlank { "Trống" },
            color = Color.Gray,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(sbdWeight)
        )

        // Cột Tên
        Text(
            text = record.studentName.ifBlank { "Ẩn danh" },
            color = ColorText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(nameWeight).padding(end = 8.dp)
        )

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