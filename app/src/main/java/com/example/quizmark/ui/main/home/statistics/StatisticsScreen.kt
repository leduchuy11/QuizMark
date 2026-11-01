package com.example.quizmark.ui.main.home.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.quizmark.R
import com.example.quizmark.ui.theme.BackgroundScreen
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.ColorText2
import com.example.quizmark.ui.theme.ColorText3
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.ui.theme.PrimaryDarkBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onNavigateToScoreList: (String) -> Unit,
    onExportFile: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = BackgroundScreen,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.statistics_title),
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
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Dropdown Chọn Bộ Lọc
            StatFilterDropdown(
                availableRosters = uiState.availableRosters,
                selectedRosterId = uiState.selectedRosterId,
                onRosterSelected = { viewModel.updateFilter(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Cụm 3 thẻ tóm tắt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(id = R.string.stat_total_exams_upper),
                    value = uiState.totalExams.toString(),
                    valueColor = DarkBlue
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(id = R.string.stat_avg_score_upper),
                    value = String.format("%.1f", uiState.averageScore),
                    valueColor = Color(0xFF16A34A)
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(id = R.string.stat_pass_rate_upper),
                    value = "${uiState.passRate}%",
                    valueColor = Color(0xFFD97706)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 3. Biểu đồ phân bố điểm
            Text(
                text = stringResource(id = R.string.score_distribution),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ColorText
            )
            Spacer(modifier = Modifier.height(16.dp))
            BarChartComponent(data = uiState.scoreDistribution)

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Khối Hành động
            Text(
                text = stringResource(id = R.string.section_actions),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ColorText
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
            ) {
                ActionRowItem(
                    iconRes = R.drawable.ic_document,
                    title = stringResource(id = R.string.action_view_score_list),
                    onClick = { onNavigateToScoreList(uiState.selectedRosterId) }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                ActionRowItem(
                    iconRes = R.drawable.ic_sum_list_exam,
                    title = stringResource(id = R.string.action_export_score_file),
                    onClick = { onExportFile(uiState.selectedRosterId) }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatFilterDropdown(
    availableRosters: List<Pair<String, String>>,
    selectedRosterId: String,
    onRosterSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val filterAllText = stringResource(id = R.string.filter_all)
    val filterNoneText = stringResource(id = R.string.filter_none)

    val selectedText = when (selectedRosterId) {
        "ALL" -> filterAllText
        "NONE" -> filterNoneText
        else -> availableRosters.find { it.first == selectedRosterId }?.second ?: ""
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            leadingIcon = { Icon(Icons.Filled.Group, contentDescription = null, tint = ColorText2) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = ColorText2) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedBorderColor = Color(0xFFE2E8F0)
            ),
            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Medium, color = ColorText)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            DropdownMenuItem(
                text = { Text(filterAllText, color = ColorText) },
                onClick = { onRosterSelected("ALL"); expanded = false }
            )
            DropdownMenuItem(
                text = { Text(filterNoneText, color = ColorText) },
                onClick = { onRosterSelected("NONE"); expanded = false }
            )
            availableRosters.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name, color = ColorText) },
                    onClick = { onRosterSelected(id); expanded = false }
                )
            }
        }
    }
}

@Composable
fun SummaryCard(modifier: Modifier = Modifier, title: String, value: String, valueColor: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
        }
    }
}

@Composable
fun BarChartComponent(data: List<Int>) {
    val labels = listOf("[0,1]", "(1,2]", "(2,3]", "(3,4]", "(4,5]", "(5,6]", "(6,7]", "(7,8]", "(8,9]", "(9,10]")
    val maxValue = data.maxOrNull() ?: 0
    val yStep = if (maxValue <= 4) 1 else (maxValue / 4) + 1
    val chartMax = (yStep * 4).coerceAtLeast(4)
    val yAxisLabels = listOf(chartMax.toString(), (yStep * 3).toString(), (yStep * 2).toString(), yStep.toString(), "0")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).height(240.dp).fillMaxWidth()
        ) {
            // Cột Text Trục Y
            Column(
                modifier = Modifier.fillMaxHeight().padding(bottom = 20.dp, end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                yAxisLabels.forEach { label ->
                    Text(text = label, fontSize = 10.sp, color = Color.Gray)
                }
            }

            // Khu vực Biểu đồ & Trục X
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Vẽ các đường lưới ngang
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        repeat(5) {
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                        }
                    }

                    // Vẽ các cột dữ liệu
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        data.forEach { count ->
                            val heightFraction = if (chartMax > 0) count.toFloat() / chartMax.toFloat() else 0f
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            ) {
                                if (count > 0) {
                                    Text(text = count.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .fillMaxHeight(heightFraction)
                                        .background(Color(0xFFDC2626), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Cột Nhãn Trục X
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    labels.forEach { label ->
                        Text(
                            text = label,
                            fontSize = 8.sp,
                            color = Color.Gray,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionRowItem(iconRes: Int, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = PrimaryDarkBlue, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = ColorText, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = ColorText3)
    }
}