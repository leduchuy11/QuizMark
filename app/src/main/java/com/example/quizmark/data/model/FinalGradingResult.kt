package com.example.quizmark.data.model

import android.graphics.Bitmap

data class FinalGradingResult(
    val studentName: String,
    val sbd: String,
    val examCode: String,   //Mã đề
    val score: Double,
    val correctCount: Int,
    val wrongCount: Int,
    val blankCount: Int,
    val totalQuestions: Int,
    val processedImage: Bitmap?
)