package com.example.quizmark.data.model

// 1. Model giao diện dùng chung cho cả 2 loại phiếu
data class ExamUiModel(
    val id: String,
    val name: String,
    val questionCount: Int,
    val isThpt: Boolean,
    val createdAt: Long
)