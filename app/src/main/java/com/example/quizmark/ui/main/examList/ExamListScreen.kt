package com.example.quizmark.ui.main.examList

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.R
import com.example.quizmark.data.model.RosterModel
import com.example.quizmark.ui.main.examList.student.StudentTabContent
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.ColorText3
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.ui.theme.PrimaryDarkBlue

@Composable
fun ExamListScreen(
    onNavigateToAddRoster: () -> Unit,
    onNavigateToAddStudent: () -> Unit,
    onNavigateToRosterDetail: (String) -> Unit,
    viewModel: RosterViewModel = hiltViewModel()
) {
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    val allStudentsList by viewModel.allStudentsList.collectAsState()

    // Lắng nghe dữ liệu thật từ Firebase
    val rosterList by viewModel.rosterList.collectAsState()

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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTabIndex == 0) onNavigateToAddRoster() else onNavigateToAddStudent()
                },
                containerColor = DarkBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add_ver2),
                    contentDescription = if (selectedTabIndex == 0)
                        stringResource(id = R.string.cd_add_roster)
                    else
                        stringResource(id = R.string.cd_add_student),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CustomPillTabRow(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedTabIndex == 0) {
                RosterTabContent(
                    rosterList = rosterList,
                    onRosterClick = onNavigateToRosterDetail
                )
            } else {
                StudentTabContent(
                    rosterList = rosterList,
                    allStudentsList = allStudentsList,
                    onNavigateToImportFile = {
                        // TODO: Gọi chuyển trang nhập file
                    }
                )
            }
        }
    }
}

@Composable
private fun CustomPillTabRow(selectedTabIndex: Int, onTabSelected: (Int) -> Unit) {
    val tabs = listOf(stringResource(R.string.tab_roster), stringResource(R.string.tab_students))

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(2.dp, RoundedCornerShape(14.dp), spotColor = Color(0x26000000))
            .background(Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / tabs.size

        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedTabIndex,
            animationSpec = tween(durationMillis = 250),
            label = "tab_indicator"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .height(38.dp)
                .shadow(2.dp, RoundedCornerShape(10.dp), spotColor = Color(0x33000000))
                .background(Color.White, RoundedCornerShape(10.dp))
        )

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) DarkBlue else Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// ---------------- TAB 1: DANH SÁCH THI ---------------- //
@Composable
fun RosterTabContent(rosterList: List<RosterModel>, onRosterClick: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    val filteredRosters = remember(searchQuery, rosterList) {
        if (searchQuery.isBlank()) rosterList
        else rosterList.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
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

        if (rosterList.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.total_roster_count, rosterList.size),
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
            if (filteredRosters.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (rosterList.isEmpty())
                                stringResource(id = R.string.empty_roster_list)
                            else
                                "Không tìm thấy kết quả nào",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(filteredRosters) { roster ->
                    RosterItemCard(roster = roster, onClick = { onRosterClick(roster.id) })
                }
            }
        }
    }
}

@Composable
fun RosterItemCard(roster: RosterModel, onClick: () -> Unit) {
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
                painter = painterResource(id = R.drawable.ic_group),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = roster.name,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ColorText,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.student_count_suffix, roster.students.size),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color.Gray
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Xem chi tiết",
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(28.dp)
        )
    }
}

