package com.example.quizmark.ui.main.examList.classroom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quizmark.R
import com.example.quizmark.data.model.ClassroomModel
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.ColorText3
import com.example.quizmark.ui.theme.PrimaryDarkBlue
import com.example.quizmark.util.unAccent

@Composable
fun ClassroomTabContent(
    classroomList: List<ClassroomModel>,
    onClassroomClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    val filteredClassrooms = remember(searchQuery, classroomList) {
        if (searchQuery.isBlank()) {
            classroomList
        } else {
            val queryText = searchQuery.trim().unAccent().lowercase()
            classroomList.filter {
                it.name.unAccent().lowercase().contains(queryText) ||
                    it.subject.unAccent().lowercase().contains(queryText) ||
                    it.grade.unAccent().lowercase().contains(queryText)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(
                        width = 1.dp,
                        color = if (isSearchFocused) PrimaryDarkBlue else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(50)
                    )
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text(text = stringResource(id = R.string.search_roster_hint), color = Color.Gray, fontSize = 14.sp)
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = ColorText),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isSearchFocused = it.isFocused }
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { searchQuery = "" }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (classroomList.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.total_classroom_count, classroomList.size),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ColorText3,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = Color(0xFFE2E8F0),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredClassrooms.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp, start = 16.dp, end = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (classroomList.isEmpty()) {
                                stringResource(id = R.string.empty_classroom_list)
                            } else {
                                "Không tìm thấy kết quả nào"
                            },
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredClassrooms, key = { it.id }) { classroom ->
                    ClassroomItemCard(
                        classroom = classroom,
                        onClick = { onClassroomClick(classroom.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ClassroomItemCard(
    classroom: ClassroomModel,
    onClick: () -> Unit
) {
    val metaLine = listOf(classroom.grade, classroom.subject, classroom.schoolYear)
        .filter { it.isNotBlank() }
        .joinToString(" • ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp), spotColor = Color(0x1A000000))
            .background(Color.White, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color(0xFFE6F0FD), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_group_ver2),
                contentDescription = null,
                tint = PrimaryDarkBlue,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = classroom.name,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ColorText,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (metaLine.isNotBlank()) {
                    "${stringResource(id = R.string.student_count_suffix, classroom.students.size)}  •  $metaLine"
                } else {
                    stringResource(id = R.string.student_count_suffix, classroom.students.size)
                },
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color.Gray,
                maxLines = 1
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(28.dp)
        )
    }
}
