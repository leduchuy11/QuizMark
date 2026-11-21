package com.example.quizmark.util

import com.example.quizmark.data.model.ClassroomModel
import com.example.quizmark.data.model.StudentModel

enum class ClassSelectionState {
    NONE,
    PARTIAL,
    ALL
}

data class RosterSelectionBuildResult(
    val students: List<StudentModel>,
    val sourceClassIds: List<String>,
    val sourceClassNames: List<String>,
    val duplicateSbds: Map<String, List<StudentModel>>
)

object ExamRosterSelector {
    //Xác định trạng thái chọn của 1 lớp học (chưa chọn ai / chọn một phần / chọn tất cả)
    fun classSelectionState(
        classroom: ClassroomModel,
        selectedIds: Set<String>,
        excludedStudentIds: Set<String> = emptySet()
    ): ClassSelectionState {
        val eligible = classroom.students.filter { it.id !in excludedStudentIds }
        if (eligible.isEmpty()) return ClassSelectionState.NONE
        val selectedCount = eligible.count { it.id in selectedIds }
        return when (selectedCount) {
            0 -> ClassSelectionState.NONE
            eligible.size -> ClassSelectionState.ALL
            else -> ClassSelectionState.PARTIAL
        }
    }

    fun selectedCountInClass(
        classroom: ClassroomModel,
        selectedIds: Set<String>,
        excludedStudentIds: Set<String> = emptySet()
    ): Int = classroom.students.count { it.id !in excludedStudentIds && it.id in selectedIds }

    //Nếu lớp đang được chọn TẤT CẢ -> bỏ chọn toàn bộ học sinh của lớp đó
    //Nếu lớp đang chọn 0 hoặc chọn 1 phần -> chọn TẤT CẢ học sinh hợp lệ của lớp đó
    fun toggleEntireClass(
        classroom: ClassroomModel,
        selectedIds: Set<String>,
        excludedStudentIds: Set<String> = emptySet()
    ): Set<String> {
        val eligibleIds = classroom.students
            .asSequence()
            .map { it.id }
            .filter { it !in excludedStudentIds }
            .toSet()
        if (eligibleIds.isEmpty()) return selectedIds

        return if (classSelectionState(classroom, selectedIds, excludedStudentIds) == ClassSelectionState.ALL) {
            selectedIds - eligibleIds
        } else {
            selectedIds + eligibleIds
        }
    }

    fun selectAllInClass(
        classroom: ClassroomModel,
        selectedIds: Set<String>,
        excludedStudentIds: Set<String> = emptySet()
    ): Set<String> {
        val eligibleIds = classroom.students
            .asSequence()
            .map { it.id }
            .filter { it !in excludedStudentIds }
            .toSet()
        return selectedIds + eligibleIds
    }

    fun clearAllInClass(
        classroom: ClassroomModel,
        selectedIds: Set<String>
    ): Set<String> {
        val ids = classroom.students.map { it.id }.toSet()
        return selectedIds - ids
    }

    fun toggleStudent(studentId: String, selectedIds: Set<String>): Set<String> =
        if (studentId in selectedIds) selectedIds - studentId else selectedIds + studentId

    //Tổng hợp danh sách thí sinh đã chọn từ nhiều lớp
    fun buildSelection(
        classrooms: List<ClassroomModel>,
        selectedIds: Set<String>,
        extraStudents: List<StudentModel> = emptyList(),
        existingStudents: List<StudentModel> = emptyList()
    ): RosterSelectionBuildResult {
        val collectedById = LinkedHashMap<String, StudentModel>()
        val sourceIds = mutableListOf<String>()
        val sourceNames = mutableListOf<String>()

        for (classroom in classrooms) {
            var pickedFromThisClass = false
            for (student in classroom.students) {
                if (student.id in selectedIds && student.id !in collectedById) {
                    collectedById[student.id] = student.copy(
                        classId = classroom.id,
                        className = classroom.name
                    )
                    pickedFromThisClass = true
                }
            }
            if (pickedFromThisClass) {
                sourceIds.add(classroom.id)
                sourceNames.add(classroom.name)
            }
        }

        for (student in extraStudents) {
            if (student.id in selectedIds && student.id !in collectedById) {
                collectedById[student.id] = student
            }
        }

        val selectedList = collectedById.values.toList()
        val combinedForDupCheck = existingStudents + selectedList
        val duplicateSbds = combinedForDupCheck
            .filter { it.studentCode.isNotBlank() }
            .groupBy { it.studentCode.trim() }
            .filterValues { group -> group.distinctBy { it.id }.size > 1 }

        return RosterSelectionBuildResult(
            students = selectedList,
            sourceClassIds = sourceIds,
            sourceClassNames = sourceNames,
            duplicateSbds = duplicateSbds
        )
    }
}
