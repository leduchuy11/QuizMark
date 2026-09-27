package com.example.quizmark.ui.main.examList.student

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quizmark.R
import com.example.quizmark.data.model.RosterModel
import com.example.quizmark.data.model.StudentModel
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.ui.theme.PrimaryDarkBlue
import com.example.quizmark.util.unAccent

@Composable
fun StudentTabContent(
    rosterList: List<RosterModel>,
    allStudentsList: List<StudentModel>,
    onNavigateToEditStudent: (String) -> Unit,
    onNavigateToImportFile: () -> Unit
) {
    val filterAllText = stringResource(id = R.string.filter_all)

    var selectedFilter by remember { mutableStateOf(filterAllText) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }


    // 1. Tạo danh sách tùy chọn cho Dropdown Lọc
    val filterOptions = remember(rosterList, filterAllText) {
        listOf(filterAllText) + rosterList.map { it.name }
    }

    if (selectedFilter !in filterOptions) {
        selectedFilter = filterAllText
    }

    // 2. Logic xử lý dữ liệu học sinh
    val displayedStudents = remember(selectedFilter, searchQuery, rosterList, allStudentsList, filterAllText) {
        val baseList = if (selectedFilter == filterAllText) {
            // NẾU LÀ "TẤT CẢ" -> LẤY LUÔN DANH SÁCH TỔNG TỪ FIREBASE
            allStudentsList
        } else {
            // NẾU CHỌN LỚP CỤ THỂ -> CHỈ LẤY HỌC SINH TRONG LỚP ĐÓ
            rosterList.find { it.name == selectedFilter }?.students ?: emptyList()
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            val queryText = searchQuery.trim().unAccent().lowercase()

            baseList.filter {
                val nameMatch = it.name.unAccent().lowercase().contains(queryText)
                val codeMatch = it.studentCode.unAccent().lowercase().contains(queryText)

                nameMatch || codeMatch
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- ROW 1: NÚT LỌC & NÚT NHẬP FILE ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dropdown Lọc
            Box {
                Row(
                    modifier = Modifier
                        .background(DarkBlue, RoundedCornerShape(20.dp))
                        .clickable { isDropdownExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_filter),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.filter_label, selectedFilter),
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    filterOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    color = if (selectedFilter == option) DarkBlue else ColorText,
                                    fontWeight = if (selectedFilter == option) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedFilter = option
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Nút Nhập File
            Row(
                modifier = Modifier
                    .background(DarkBlue, RoundedCornerShape(20.dp))
                    .clickable { onNavigateToImportFile() }
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_import_file),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = R.string.btn_import_file),
                    fontSize = 13.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- ROW 2: THANH TÌM KIẾM ---
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
                    .background(Color.White, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.search_student_hint),
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
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
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { searchQuery = "" }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- DANH SÁCH SCROLL ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (displayedStudents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.empty_student_list),
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(displayedStudents, key = { it.id }) { student ->
                    StudentGlobalItemCard(student = student, onClick = {
                        onNavigateToEditStudent(student.id)
                    })
                }
            }
        }
    }
}

@Composable
fun StudentGlobalItemCard(student: StudentModel, onClick: () -> Unit) {
    val initials = remember(student.name) {
        val words = student.name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        when {
            words.isEmpty() -> ""
            words.size == 1 -> words.first().take(1).uppercase()
            else -> "${words.first().take(1)}${words.last().take(1)}".uppercase()
        }
    }

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
                .border(1.dp, Color(0xFFD8D8DC), CircleShape)
                .background(Color(0xFFE6F0FD),CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = DarkBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = student.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColorText,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(id = R.string.sbd_prefix, student.studentCode),
                fontSize = 13.sp,
                color = Color.Gray
            )
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )
    }
}