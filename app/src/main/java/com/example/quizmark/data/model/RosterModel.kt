package com.example.quizmark.data.model

data class StudentModel(
    val id: String = "",
    val studentCode: String = "", // Số báo danh (SBD)
    val name: String = "",
    val dob: String = "",
    val gender: String = "",
    val classId: String = "",     // ID lớp học gốc (nếu thuộc lớp học)
    val className: String = ""    // Tên lớp học gốc (VD: "12A1")
)

//Lớp do gv quản lí
data class ClassroomModel(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val grade: String = "",
    val subject: String = "",
    val schoolYear: String = "",
    val students: List<StudentModel> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

// ds cho 1 đợt thi
data class RosterModel(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val grade: String = "",
    val subject: String = "",
    val schoolYear: String = "",
    val students: List<StudentModel> = emptyList(),
    val sourceClassIds: List<String> = emptyList(),
    val sourceClassNames: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
