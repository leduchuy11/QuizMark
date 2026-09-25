package com.example.quizmark.data.model

// 1. Model giao diện dùng chung cho cả 2 loại phiếu
data class ExamUiModel(
    val id: String,
    val name: String,
    val templateId: Int,
    val questionCount: Int,
    val isThpt: Boolean,
    val codes: List<String>,
    val createdAt: Long
)