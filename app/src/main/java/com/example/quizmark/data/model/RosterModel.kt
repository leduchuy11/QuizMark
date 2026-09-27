package com.example.quizmark.data.model

data class StudentModel(
    val id: String = "",
    val studentCode: String = "", // Số báo danh (SBD)
    val name: String = "",
    val dob: String = "",
    val gender: String = ""
)

data class RosterModel(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val grade: String = "",
    val subject: String = "",
    val schoolYear: String = "",
    val students: List<StudentModel> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)