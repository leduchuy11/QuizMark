package com.example.quizmark.ui.main.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.R

enum class AnswerStatus { CORRECT, WRONG, BLANK }

data class QuestionDetailUI(
    val displayLabel: String,    // STT
    val studentAns: String,      // Đáp án hs chọn
    val correctAns: String,      // Đáp án đề
    val status: AnswerStatus     // Kết quả
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailScreen(
    studentName: String,
    sbd: String,
    examCode: String,
    score: Double,
    correctCount: Int,
    wrongCount: Int,
    blankCount: Int,
    totalQuestions: Int,
    isThpt: Boolean,
    questions: List<QuestionDetailUI>,
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = BackgroundScreen,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.question_details),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = ColorText
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundScreen)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            item {
                DetailHeaderCard(studentName, sbd, examCode, score, correctCount, totalQuestions, wrongCount, blankCount)
            }

            if (isThpt) {
                val part1 = questions.filter { it.displayLabel.toIntOrNull() != null && it.displayLabel.toInt() <= 40 }
                val part2 = questions.filter { it.displayLabel.contains(Regex("[a-d]")) }
                val part3 = questions.filter { it.displayLabel.toIntOrNull() != null && it.displayLabel.toInt() > 40 }

                if (part1.isNotEmpty()) {
                    item {
                        SectionTitle(
                            stringResource(R.string.part1_title, part1.size)
                        )
                    }
                    item { QuestionListCard(part1) }
                }

                if (part2.isNotEmpty()) {
                    item {
                        SectionTitle(
                            stringResource(R.string.part2_title, part2.size / 4)
                        )
                    }
                    item { QuestionListCard(part2) }
                }

                if (part3.isNotEmpty()) {
                    item {
                        SectionTitle(
                            stringResource(R.string.part3_title, part3.size)
                        )
                    }
                    item {
                        val formattedPart3 = part3.map {
                            it.copy(displayLabel = (it.displayLabel.toInt() - 40).toString())
                        }
                        QuestionListCard(formattedPart3)
                    }
                }
            } else {
                item {
                    SectionTitle(
                        stringResource(R.string.answer_key_title, totalQuestions)
                    )
                }
                item { QuestionListCard(questions) }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun DetailHeaderCard(
    name: String, sbd: String, code: String, score: Double,
    correct: Int, total: Int, wrong: Int, blank: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(DarkBlue, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = String.format("%.1f", score), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text(text = stringResource(R.string.score_label), color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(text = name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ColorText)
                Text(text = stringResource(R.string.student_exam_info, sbd, code), fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text(text = stringResource(R.string.correct_answers_count, correct, total), fontSize = 13.sp, color = Color.Gray)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    DetailStatItem(GreenCorrect, stringResource(R.string.stat_correct, correct))
                    Spacer(modifier = Modifier.width(8.dp))
                    DetailStatItem(RedWrong, stringResource(R.string.stat_wrong, wrong))
                    Spacer(modifier = Modifier.width(8.dp))
                    DetailStatItem(YellowBlank, stringResource(R.string.stat_blank, blank))
                }
            }
        }
    }
}

@Composable
fun DetailStatItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
fun QuestionListCard(questions: List<QuestionDetailUI>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            questions.forEachIndexed { index, question ->
                QuestionRowItem(question)
                if (index < questions.lastIndex) {
                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
fun QuestionRowItem(item: QuestionDetailUI) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFF3F4F6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = item.displayLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorText)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.width(80.dp)) {
            Text(text = stringResource(R.string.student_selected), fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = item.studentAns, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorText)
        }

        Column(modifier = Modifier.width(80.dp)) {
            Text(text = stringResource(R.string.correct_answer_label), fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = item.correctAns, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
        }

        Spacer(modifier = Modifier.weight(1f))

        StatusBadge(item.status)
    }
}

@Composable
fun StatusBadge(status: AnswerStatus) {
    val (bgColor, contentColor, text, icon) = when (status) {
        AnswerStatus.CORRECT -> listOf(
            Color(0xFFE8F5E9),
            GreenCorrect,
            stringResource(R.string.answer_status_correct),
            Icons.Filled.Check
        )

        AnswerStatus.WRONG -> listOf(
            Color(0xFFFFEBEE),
            RedWrong,
            stringResource(R.string.answer_status_wrong),
            Icons.Filled.Close
        )

        AnswerStatus.BLANK -> listOf(
            Color(0xFFFFF8E1),
            Color(0xFFF57F17),
            stringResource(R.string.answer_status_blank),
            null
        )
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor as Color)
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector, contentDescription = null, tint = contentColor as Color, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
        } else {
            Text(text = "?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = contentColor as Color)
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(text = text as String, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = contentColor as Color)
    }
}