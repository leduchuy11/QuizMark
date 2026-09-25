package com.example.quizmark.data.model

// 1. Mô hình tổng thể cho một Đề thi loại Basic (20, 40, 50, 120 câu)
data class ExamModel(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val templateId: Int = 0,
    val questionCount: Int = 0,
    val codes: List<ExamCodeModel> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

// 2. Chi tiết đáp án cho từng Mã đề của loại Basic
data class ExamCodeModel(
    val code: String = "",
    val answers: Map<String, String> = emptyMap()
)