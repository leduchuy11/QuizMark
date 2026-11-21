package com.example.quizmark.ui.main.examList.roster

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quizmark.R
import com.example.quizmark.data.model.ClassroomModel
import com.example.quizmark.data.model.StudentModel
import com.example.quizmark.ui.theme.ColorText
import com.example.quizmark.ui.theme.DarkBlue
import com.example.quizmark.ui.theme.PrimaryDarkBlue
import com.example.quizmark.util.ClassSelectionState
import com.example.quizmark.util.ExamRosterSelector
import com.example.quizmark.util.unAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassStudentPickerSection(
    classrooms: List<ClassroomModel>,
    selectedStudentIds: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    excludedStudentIds: Set<String> = emptySet(),
    duplicateSbds: Map<String, List<StudentModel>> = emptyMap()
) {
    var inspectingClassroom by remember { mutableStateOf<ClassroomModel?>(null) }

    val totalSelected = remember(classrooms, selectedStudentIds, excludedStudentIds) {
        classrooms.sumOf { ExamRosterSelector.selectedCountInClass(it, selectedStudentIds, excludedStudentIds) }
    }
    val selectedClassCount = remember(classrooms, selectedStudentIds, excludedStudentIds) {
        classrooms.count { ExamRosterSelector.selectedCountInClass(it, selectedStudentIds, excludedStudentIds) > 0 }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.section_select_from_classes),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorText
            )
            if (totalSelected > 0) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE6F0FD), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(
                            id = R.string.selected_students_summary,
                            totalSelected,
                            selectedClassCount
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDarkBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(id = R.string.select_from_classes_hint),
            fontSize = 13.sp,
            color = Color.Gray,
            lineHeight = 18.sp
        )

        if (duplicateSbds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFFBEB), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        id = R.string.warning_duplicate_sbd_inline,
                        duplicateSbds.keys.take(5).joinToString(", ")
                    ),
                    fontSize = 13.sp,
                    color = Color(0xFF92400E),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val visibleClassrooms = remember(classrooms, excludedStudentIds) {
            classrooms.filter { cls ->
                cls.students.any { it.id !in excludedStudentIds }
            }
        }

        if (classrooms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.empty_classes_for_roster),
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 19.sp
                )
            }
        } else if (visibleClassrooms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.empty_available_students_to_add),
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                visibleClassrooms.forEach { classroom ->
                    val eligibleCount = classroom.students.count { it.id !in excludedStudentIds }
                    val selectedCount = ExamRosterSelector.selectedCountInClass(
                        classroom,
                        selectedStudentIds,
                        excludedStudentIds
                    )
                    val state = ExamRosterSelector.classSelectionState(
                        classroom,
                        selectedStudentIds,
                        excludedStudentIds
                    )
                    val toggleState = when (state) {
                        ClassSelectionState.NONE -> ToggleableState.Off
                        ClassSelectionState.PARTIAL -> ToggleableState.Indeterminate
                        ClassSelectionState.ALL -> ToggleableState.On
                    }
                    val isHighlighted = state != ClassSelectionState.NONE

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isHighlighted) 1.5.dp else 1.dp,
                                color = if (isHighlighted) PrimaryDarkBlue else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .background(if (isHighlighted) Color(0xFFF0F7FF) else Color.White)
                            .clickable { inspectingClassroom = classroom }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TriStateCheckbox(
                            state = toggleState,
                            onClick = {
                                onSelectionChange(
                                    ExamRosterSelector.toggleEntireClass(
                                        classroom,
                                        selectedStudentIds,
                                        excludedStudentIds
                                    )
                                )
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PrimaryDarkBlue,
                                uncheckedColor = Color(0xFF94A3B8)
                            )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = classroom.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(
                                    id = R.string.class_selected_ratio,
                                    selectedCount,
                                    eligibleCount
                                ),
                                fontSize = 13.sp,
                                color = if (isHighlighted) PrimaryDarkBlue else Color.Gray,
                                fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE6F0FD))
                                .clickable { inspectingClassroom = classroom }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.btn_pick_individual),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDarkBlue
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = PrimaryDarkBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    inspectingClassroom?.let { cls ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var searchQuery by remember(cls.id) { mutableStateOf("") }

        val eligibleStudents = remember(cls, excludedStudentIds) {
            cls.students.filter { it.id !in excludedStudentIds }
        }
        val filteredStudents = remember(eligibleStudents, searchQuery) {
            if (searchQuery.isBlank()) {
                eligibleStudents
            } else {
                val q = searchQuery.trim().unAccent().lowercase()
                eligibleStudents.filter {
                    it.name.unAccent().lowercase().contains(q) ||
                        it.studentCode.unAccent().lowercase().contains(q)
                }
            }
        }
        val selectedInClass = ExamRosterSelector.selectedCountInClass(cls, selectedStudentIds, excludedStudentIds)

        ModalBottomSheet(
            onDismissRequest = { inspectingClassroom = null },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.82f)
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cls.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorText
                        )
                        Text(
                            text = stringResource(
                                id = R.string.class_selected_ratio,
                                selectedInClass,
                                eligibleStudents.size
                            ),
                            fontSize = 13.sp,
                            color = PrimaryDarkBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = {
                            onSelectionChange(
                                ExamRosterSelector.selectAllInClass(cls, selectedStudentIds, excludedStudentIds)
                            )
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.btn_select_all),
                            color = PrimaryDarkBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    TextButton(
                        onClick = {
                            onSelectionChange(
                                ExamRosterSelector.clearAllInClass(cls, selectedStudentIds)
                            )
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.btn_deselect_all),
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
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
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(filteredStudents, key = { it.id }) { student ->
                        val isChecked = student.id in selectedStudentIds
                        val initials = student.name.trim().split("\\s+".toRegex())
                            .filter { it.isNotEmpty() }
                            .let { words ->
                                when {
                                    words.isEmpty() -> ""
                                    words.size == 1 -> words.first().take(1).uppercase()
                                    else -> "${words.first().take(1)}${words.last().take(1)}".uppercase()
                                }
                            }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    1.dp,
                                    if (isChecked) Color(0xFF93C5FD) else Color(0xFFE2E8F0),
                                    RoundedCornerShape(12.dp)
                                )
                                .background(if (isChecked) Color(0xFFF0F9FF) else Color.White)
                                .clickable {
                                    onSelectionChange(ExamRosterSelector.toggleStudent(student.id, selectedStudentIds))
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    onSelectionChange(ExamRosterSelector.toggleStudent(student.id, selectedStudentIds))
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = PrimaryDarkBlue,
                                    uncheckedColor = Color(0xFF94A3B8)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFE6F0FD), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = initials, color = DarkBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ColorText
                                )
                                Text(
                                    text = stringResource(id = R.string.sbd_prefix, student.studentCode),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { inspectingClassroom = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 24.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.btn_done),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
