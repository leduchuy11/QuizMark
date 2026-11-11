package com.example.quizmark.data.model

data class HistoryRecord(
    val id: String = "",
    val rosterId: String = "anonymous",
    val rosterName: String = "Ẩn danh",
    val studentName: String = "",
    val studentCode: String = "",
    val examCode: String = "",
    val score: Double = 0.0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val blankCount: Int = 0,
    val totalQuestions: Int = 0,
    val templateId: Int = 1,
    val localImagePath: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val localOriginalImagePath: String = ""
)
