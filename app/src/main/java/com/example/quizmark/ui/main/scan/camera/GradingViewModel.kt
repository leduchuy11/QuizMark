package com.example.quizmark.ui.main.scan.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizmark.data.model.FinalGradingResult
import com.example.quizmark.data.model.ScanResult
import com.example.quizmark.data.model.ThptExamCodeModel
import com.example.quizmark.data.model.ThptExamConfig
import com.example.quizmark.data.repository.ExamListRepository
import com.example.quizmark.data.repository.ExamRepository
import com.example.quizmark.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import dagger.hilt.android.qualifiers.ApplicationContext


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
    private val examListRepository: ExamListRepository,
    private val historyRepository: HistoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _gradingState = MutableStateFlow<GradingState>(GradingState.Idle)
    val gradingState = _gradingState.asStateFlow()

    private var currentRosterId: String? = null
    private var isResultSaved = false

    fun gradeExam(scanResult: ScanResult, examId: String, rosterId: String?, templateId: Int) {
        currentRosterId = rosterId
        isResultSaved = false

        viewModelScope.launch {
            _gradingState.value = GradingState.Loading
            try {
                // BƯỚC 1: TÌM HỌC SINH
                val scannedSbd = scanResult.sbd ?: "Trống"
                var finalStudentName = "Ẩn danh"
                var finalRosterName = "Ẩn danh"

                if (rosterId != null) {
                    try {
                        val roster = examListRepository.getRosterById(rosterId)
                        if (roster != null) {
                            finalRosterName = roster.name
                            if (scannedSbd != "Trống") {
                                val matchedStudent = roster.students.find { it.studentCode == scannedSbd }
                                if (matchedStudent != null) finalStudentName = matchedStudent.name
                            }
                        }
                    } catch (e: Exception) {
                        // Nếu lấy danh sách thất bại (do mất mạng + không có cache), vẫn tiếp tục chấm bài!
                        e.printStackTrace()
                    }
                }

                val scannedCode = scanResult.examCode ?: "Trống"
                val isThpt = templateId == 1
                var correctCount = 0; var wrongCount = 0; var blankCount = 0; var finalScore = 0.0; var totalQuestions = 0

                val mutableBitmap = scanResult.processedBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = mutableBitmap?.let { Canvas(it) }

                val paintCorrect = Paint().apply { color = Color.GREEN; style = Paint.Style.STROKE; strokeWidth = 3.5f; isAntiAlias = true }
                val paintWrong = Paint().apply { color = Color.RED; style = Paint.Style.STROKE; strokeWidth = 3.5f; isAntiAlias = true }
                val paintError = Paint().apply { color = Color.YELLOW; style = Paint.Style.STROKE; strokeWidth = 3.5f; isAntiAlias = true }
                val paintEmpty = Paint().apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 1.2f; isAntiAlias = true }

                // BƯỚC 2: TÍNH ĐIỂM
                var uiList = emptyList<QuestionDetailUI>()

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

                        uiList = getDetailedQuestionsUI(scanResult, matchedCode, null, true, thptConfig = thptExam.config)
                    } else {
                        throw Exception("Không tìm thấy dữ liệu Đề thi. Vui lòng kết nối mạng để đồng bộ!")
                    }
                } else {
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

                        uiList = getDetailedQuestionsUI(scanResult, null, matchedCode?.answers, false, basicQuestionCount = basicExam.questionCount)
                    } else {
                        throw Exception("Không tìm thấy dữ liệu Đề thi. Vui lòng kết nối mạng để đồng bộ!")
                    }
                }

                // BƯỚC 3: ĐÓNG GÓI KẾT QUẢ ĐỂ HIỂN THỊ
                val resultData = FinalGradingResult(
                    studentName = finalStudentName, sbd = scannedSbd, examCode = scannedCode, score = finalScore,
                    correctCount = correctCount, wrongCount = wrongCount, blankCount = blankCount, totalQuestions = totalQuestions,
                    processedImage = mutableBitmap,
                    detailedQuestions = uiList,
                    templateId = templateId
                )


                // 5. KÍCH HOẠT GIAO DIỆN HIỂN THỊ
                _gradingState.value = GradingState.Success(resultData)
            } catch (e: Exception) {
                _gradingState.value = GradingState.Error(e.message ?: "Lỗi chấm thi")
            }
        }
    }

    fun saveCurrentResult() {
        // NẾU ĐÃ LƯU RỒI THÌ BỎ QUA NGAY LẬP TỨC
        if (isResultSaved) return

        val currentState = _gradingState.value
        if (currentState is GradingState.Success) {

            isResultSaved = true // ĐÁNH DẤU LÀ ĐÃ LƯU ĐỂ CHẶN CÁC LẦN GỌI SAU

            viewModelScope.launch {
                try {
                    val resultData = currentState.result
                    var finalRosterName = "Ẩn danh"

                    if (currentRosterId != null) {
                        try {
                            val roster = examListRepository.getRosterById(currentRosterId!!)
                            if (roster != null) finalRosterName = roster.name
                        } catch (e: Exception) { e.printStackTrace() }
                    }

                    historyRepository.saveRecord(context, resultData, currentRosterId, finalRosterName)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun calculateThptStats(
        scanResult: ScanResult, codeModel: ThptExamCodeModel, config: ThptExamConfig,
        canvas: Canvas?, paintCorrect: Paint, paintWrong: Paint,
        paintError: Paint, paintEmpty: Paint
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

    // Hàm này sẽ dùng để map dữ liệu từ ScanResult sang List UI cho màn hình con mắt
    fun getDetailedQuestionsUI(
        scanResult: ScanResult,
        codeModel: ThptExamCodeModel?,
        basicAnswers: Map<String, String>?,
        isThpt: Boolean,
        thptConfig: ThptExamConfig? = null,
        basicQuestionCount: Int = 0
    ): List<QuestionDetailUI> {
        val uiList = mutableListOf<QuestionDetailUI>()

        val validP1 = if (thptConfig != null) 1..thptConfig.p1Questions else 1..0
        val validP2 = if (thptConfig != null) 41 until (41 + thptConfig.p2Questions * 4) else 1..0
        val validP3 = if (thptConfig != null) 73 until (73 + thptConfig.p3Questions) else 1..0
        val validBasic = 1..basicQuestionCount

        scanResult.answers.forEach { q ->
            val num = q.questionNumber

            // LỌC: BỎ QUA NGAY NHỮNG CÂU VƯỢT QUÁ SỐ LƯỢNG TRONG CẤU HÌNH
            if (isThpt && (num !in validP1 && num !in validP2 && num !in validP3)) return@forEach
            if (!isThpt && num !in validBasic) return@forEach

            var studentAns = q.selectedAnswer ?: "-"
            if (q.isError) studentAns = "Lỗi"

            var correctAns = ""

            if (isThpt && codeModel != null) {
                when (num) {
                    in validP1 -> {
                        correctAns = codeModel.part1[num.toString()] ?: "?"
                        val status = if (q.isError || q.selectedAnswer == null) AnswerStatus.BLANK else if (q.selectedAnswer == correctAns) AnswerStatus.CORRECT else AnswerStatus.WRONG
                        uiList.add(QuestionDetailUI(num.toString(), studentAns, correctAns, status))
                    }
                    in validP2 -> {
                        val offset = num - 41
                        val cau = (offset / 4) + 1
                        val y = when (offset % 4) { 0 -> "a"; 1 -> "b"; 2 -> "c"; else -> "d" }
                        val displayLabel = "${cau}${y}"
                        correctAns = codeModel.part2[cau.toString()]?.get(y) ?: "?"
                        val status = if (q.isError || q.selectedAnswer == null) AnswerStatus.BLANK else if (q.selectedAnswer == correctAns) AnswerStatus.CORRECT else AnswerStatus.WRONG
                        uiList.add(QuestionDetailUI(displayLabel, studentAns, correctAns, status))
                    }
                    in validP3 -> {
                        val cau = num - 72
                        correctAns = codeModel.part3[cau.toString()] ?: "?"
                        val status = if (q.isError || q.selectedAnswer == null) AnswerStatus.BLANK else if (q.selectedAnswer == correctAns) AnswerStatus.CORRECT else AnswerStatus.WRONG
                        uiList.add(
                            QuestionDetailUI(
                                (cau + 40).toString(),
                                studentAns,
                                correctAns,
                                status
                            )
                        )
                    }
                }
            } else if (!isThpt && basicAnswers != null) {
                correctAns = basicAnswers[num.toString()] ?: "?"
                val status = if (q.isError || q.selectedAnswer == null) AnswerStatus.BLANK else if (q.selectedAnswer == correctAns) AnswerStatus.CORRECT else AnswerStatus.WRONG
                uiList.add(QuestionDetailUI(num.toString(), studentAns, correctAns, status))
            }
        }
        return uiList
    }

    fun resetState() {
        _gradingState.value = GradingState.Idle
    }
}