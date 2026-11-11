package com.example.quizmark.data.model

import android.graphics.Bitmap
import com.example.quizmark.ui.main.scan.camera.QuestionDetailUI

data class FinalGradingResult(
    val studentName: String,
    val sbd: String,
    val examCode: String,   //Mã đề
    val score: Double,
    val correctCount: Int,
    val wrongCount: Int,
    val blankCount: Int,
    val totalQuestions: Int,
    val processedImage: Bitmap?,
    val detailedQuestions: List<QuestionDetailUI>,
    val templateId: Int,
    val originalImagePath: String? = null
)
