package com.example.quizmark.ui.main.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizmark.data.model.FinalGradingResult
import com.example.quizmark.data.model.ScanResult
import com.example.quizmark.data.model.ThptExamCodeModel
import com.example.quizmark.data.model.ThptExamConfig
import com.example.quizmark.data.repository.ExamListRepository
import com.example.quizmark.data.repository.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


sealed class GradingState {
    object Idle : GradingState()
    object Loading : GradingState()
    data class Success(val result: FinalGradingResult) : GradingState()
    data class Error(val message: String) : GradingState()
}

// Data class nội bộ để gom số liệu chấm THPT
private data class ThptStats(val score: Double, val correct: Int, val wrong: Int, val blank: Int)

@HiltViewModel
class GradingViewModel @Inject constructor(
    private val examRepository: ExamRepository,
    private val examListRepository: ExamListRepository
) : ViewModel() {

    private val _gradingState = MutableStateFlow<GradingState>(GradingState.Idle)
    val gradingState = _gradingState.asStateFlow()

    fun gradeExam(scanResult: ScanResult, examId: String, rosterId: String?, templateId: Int) {
        viewModelScope.launch {
            _gradingState.value = GradingState.Loading
            try {
                // BƯỚC 1 TÌM HỌC SINH
                val scannedSbd = scanResult.sbd ?: "Trống"
                var finalStudentName = "Ẩn danh"
                if (rosterId != null && scannedSbd != "Trống") {
                    val roster = examListRepository.getRosterById(rosterId)
                    if (roster != null) {
                        val matchedStudent = roster.students.find { it.studentCode == scannedSbd }
                        if (matchedStudent != null) finalStudentName = matchedStudent.name
                    }
                }

                val scannedCode = scanResult.examCode ?: "Trống"
                val isThpt = templateId == 1
                var correctCount = 0; var wrongCount = 0; var blankCount = 0; var finalScore = 0.0; var totalQuestions = 0

                // KHỞI TẠO BÚT VẼ (CANVAS) TỪ ẢNH GỐC
                val mutableBitmap = scanResult.processedBitmap?.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
                val canvas = mutableBitmap?.let { android.graphics.Canvas(it) }

                // ĐỊNH NGHĨA 4 MÀU UI
                val paintCorrect = android.graphics.Paint().apply { color = android.graphics.Color.GREEN; style = android.graphics.Paint.Style.STROKE; strokeWidth = 3.5f; isAntiAlias = true }
                val paintWrong = android.graphics.Paint().apply { color = android.graphics.Color.RED; style = android.graphics.Paint.Style.STROKE; strokeWidth = 3.5f; isAntiAlias = true }
                val paintError = android.graphics.Paint().apply { color = android.graphics.Color.YELLOW; style = android.graphics.Paint.Style.STROKE; strokeWidth = 3.5f; isAntiAlias = true }
                val paintEmpty = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; style = android.graphics.Paint.Style.STROKE; strokeWidth = 1.2f; isAntiAlias = true }

                if (isThpt) {
                    val thptExam = examRepository.getThptExamById(examId)
                    if (thptExam != null) {
                        totalQuestions = thptExam.config.p1Questions + (thptExam.config.p2Questions * 4) + thptExam.config.p3Questions
                        val matchedCode = thptExam.codes.find { it.code == scannedCode }

                        if (scannedCode == "Trống" || matchedCode == null) {
                            scanResult.answers.forEach { if (it.selectedAnswer == null) blankCount++ else wrongCount++ }
                        } else {
                            val stats = calculateThptStats(scanResult, matchedCode, thptExam.config, canvas, paintCorrect, paintWrong, paintError, paintEmpty)
                            correctCount = stats.correct; wrongCount = stats.wrong; blankCount = stats.blank; finalScore = stats.score
                        }
                    }
                } else {
                    // Logic cho phiếu Basic
                    val basicExam = examRepository.getBasicExamById(examId)
                    if (basicExam != null) {
                        totalQuestions = basicExam.questionCount
                        val matchedCode = basicExam.codes.find { it.code == scannedCode }

                        if (scannedCode == "Trống" || matchedCode == null) {
                            scanResult.answers.forEach { if (it.selectedAnswer == null) blankCount++ else wrongCount++ }
                        } else {
                            val correctAnswersMap = matchedCode.answers
                            scanResult.answers.forEach { q ->
                                val correctAns = correctAnswersMap[q.questionNumber.toString()]
                                var statusPaint = paintEmpty

                                if (q.isError) { statusPaint = paintError; wrongCount++ }
                                else if (q.selectedAnswer != null && q.selectedAnswer == correctAns) { correctCount++; statusPaint = paintCorrect }
                                else if (q.selectedAnswer != null) { wrongCount++; statusPaint = paintWrong }
                                else { blankCount++; statusPaint = paintEmpty }

                                q.bubbles.forEach { b ->
                                    if (b.isFilled) canvas?.drawCircle(b.x, b.y, b.radius, statusPaint)
                                    else canvas?.drawCircle(b.x, b.y, b.radius, paintEmpty)
                                }
                            }
                            if (totalQuestions > 0) finalScore = (correctCount.toDouble() / totalQuestions) * 10.0
                        }
                    }
                }

                val resultData = FinalGradingResult(
                    studentName = finalStudentName, sbd = scannedSbd, examCode = scannedCode, score = finalScore,
                    correctCount = correctCount, wrongCount = wrongCount, blankCount = blankCount, totalQuestions = totalQuestions,
                    processedImage = mutableBitmap // <--- ẢNH ĐÃ VẼ
                )
                _gradingState.value = GradingState.Success(resultData)
            } catch (e: Exception) {
                _gradingState.value = GradingState.Error(e.message ?: "Lỗi chấm thi")
            }
        }
    }

    private fun calculateThptStats(
        scanResult: ScanResult, codeModel: ThptExamCodeModel, config: ThptExamConfig,
        canvas: android.graphics.Canvas?, paintCorrect: android.graphics.Paint, paintWrong: android.graphics.Paint,
        paintError: android.graphics.Paint, paintEmpty: android.graphics.Paint
    ): ThptStats {
        var score = 0.0; var correct = 0; var wrong = 0; var blank = 0
        val p1Weight = if (config.p1Questions > 0) (config.p1Score / config.p1Questions.toDouble()) else 0.0
        val p3Weight = if (config.p3Questions > 0) (config.p3Score / config.p3Questions.toDouble()) else 0.0

        val validP1Questions = 1..config.p1Questions
        val validP2Questions = 41 until (41 + config.p2Questions * 4)
        val validP3Questions = 73 until (73 + config.p3Questions)

        scanResult.answers.forEach { q ->
            var realAns: String? = null
            var statusPaint = paintEmpty

            when (q.questionNumber) {
                in validP1Questions -> {
                    realAns = codeModel.part1[q.questionNumber.toString()]
                    if (q.isError) { wrong++; statusPaint = paintError }
                    else if (q.selectedAnswer == null) { blank++; statusPaint = paintEmpty }
                    else if (q.selectedAnswer == realAns) { correct++; score += p1Weight; statusPaint = paintCorrect }
                    else { wrong++; statusPaint = paintWrong }
                }

                in validP2Questions -> {
                    val offset = q.questionNumber - 41
                    val cau = (offset / 4) + 1
                    val y = when (offset % 4) { 0 -> "a"; 1 -> "b"; 2 -> "c"; else -> "d" }
                    realAns = codeModel.part2[cau.toString()]?.get(y)

                    if (q.isError) { wrong++; statusPaint = paintError }
                    else if (q.selectedAnswer == null) { blank++; statusPaint = paintEmpty }
                    else if (q.selectedAnswer == realAns) { correct++; statusPaint = paintCorrect }
                    else { wrong++; statusPaint = paintWrong }
                }

                in validP3Questions -> {
                    val cau = q.questionNumber - 72
                    realAns = codeModel.part3[cau.toString()]

                    if (q.isError) { wrong++; statusPaint = paintError }
                    else if (q.selectedAnswer == null) { blank++; statusPaint = paintEmpty }
                    else if (q.selectedAnswer == realAns) { correct++; score += p3Weight; statusPaint = paintCorrect }
                    else { wrong++; statusPaint = paintWrong }
                }
            }

            q.bubbles.forEach { b ->
                if (b.isFilled) canvas?.drawCircle(b.x, b.y, b.radius, statusPaint)
                else canvas?.drawCircle(b.x, b.y, b.radius, paintEmpty)
            }
        }

        // TÍNH ĐIỂM LŨY TIẾN CHO PHẦN II
        val p2Answers = scanResult.answers.filter { it.questionNumber in validP2Questions }
        for (i in 0 until config.p2Questions) {
            var correctPartsInCluster = 0
            for (j in 0 until 4) {
                val qIndex = 41 + (i * 4) + j
                val studentAns = p2Answers.find { it.questionNumber == qIndex }?.selectedAnswer
                val cau = i + 1
                val y = when (j) { 0 -> "a"; 1 -> "b"; 2 -> "c"; else -> "d" }
                val realAns = codeModel.part2[cau.toString()]?.get(y)
                if (studentAns != null && !p2Answers.find { it.questionNumber == qIndex }!!.isError && studentAns == realAns) {
                    correctPartsInCluster++
                }
            }
            score += when (correctPartsInCluster) { 1 -> 0.1; 2 -> 0.25; 3 -> 0.5; 4 -> 1.0; else -> 0.0 }
        }

        return ThptStats(Math.min(score, 10.0), correct, wrong, blank)
    }

    fun resetState() {
        _gradingState.value = GradingState.Idle
    }
}