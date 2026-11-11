package com.example.quizmark.ui.main.scan.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizmark.data.model.FinalGradingResult
import com.example.quizmark.data.model.ScanResult
import com.example.quizmark.data.model.ThptExamCodeModel
import com.example.quizmark.data.model.ThptExamConfig
import com.example.quizmark.data.repository.ExamListRepository
import com.example.quizmark.data.repository.ExamRepository
import com.example.quizmark.data.repository.HistoryRepository
import com.example.quizmark.ui.main.scan.camera.AnswerStatus
import com.example.quizmark.ui.main.scan.camera.QuestionDetailUI
import com.example.quizmark.R
import com.example.quizmark.util.scanner.ExamProcessorFactory
import com.example.quizmark.util.OriginalScanStore
import com.example.quizmark.util.requireAnswerKey
import com.example.quizmark.util.gradingErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.graphics.Canvas
import android.graphics.Paint

sealed class BatchState {
    object Idle : BatchState()
    data class Processing(val current: Int, val total: Int) : BatchState()
    data class Finished(val results: List<FinalGradingResult>, val warningMessage: String?) : BatchState()
}

// Data class phụ trợ để tính điểm THPT
private data class ThptStats(val score: Double, val correct: Int, val wrong: Int, val blank: Int)

@HiltViewModel
class BatchGradingViewModel @Inject constructor(
    private val examRepository: ExamRepository,
    private val examListRepository: ExamListRepository,
    private val historyRepository: HistoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _batchState = MutableStateFlow<BatchState>(BatchState.Idle)
    val batchState = _batchState.asStateFlow()

    fun processImagesBatch(uris: List<Uri>, examId: String, rosterId: String?, templateId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val total = uris.size
            val resultsList = mutableListOf<FinalGradingResult>()

            // Khởi tạo Processor phù hợp với loại đề
            val isThpt = templateId == 1
            val processor = ExamProcessorFactory.create(templateId) ?: run {
                _batchState.value = BatchState.Finished(emptyList(), context.getString(R.string.scan_template_unsupported))
                return@launch
            }

            var finalRosterName = "Ẩn danh"
            val roster = if (rosterId != null) examListRepository.getRosterById(rosterId) else null
            if (roster != null) finalRosterName = roster.name

            val thptExam = if (isThpt) examRepository.getThptExamById(examId) else null
            val basicExam = if (!isThpt) examRepository.getBasicExamById(examId) else null

            for ((index, uri) in uris.withIndex()) {
                _batchState.value = BatchState.Processing(index + 1, total)

                try {
                    // 1. CHUYỂN URI THÀNH BITMAP
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    // Chuyển sang dạng ARGB_8888 để OpenCV xử lý được
                    val rawBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

                    // SƠ CHẾ ẢNH BẰNG SCANNER_CORE
                    val scannerCore = com.example.quizmark.util.scanner.ScannerCore()
                    val processedBitmap = scannerCore.processStaticImage(rawBitmap)

                    // Nếu sơ chế thất bại (ảnh mờ, ko thấy 4 góc), quăng lỗi nhảy xuống Catch (0 điểm)
                    if (processedBitmap == null) {
                        throw Exception("Không thể nhận diện 4 góc của phiếu thi trong ảnh")
                    }

                    // 2. CHẠY OPENCV NHẬN DIỆN (Sử dụng ảnh đã được nắn thẳng)
                    val scanResult = processor.process(processedBitmap)

                    // 3. TÌM THÔNG TIN HỌC SINH TỪ SBD
                    val scannedSbd = scanResult.sbd ?: "Trống"
                    var finalStudentName = "Ẩn danh"
                    if (roster != null && scannedSbd != "Trống") {
                        val matchedStudent = roster.students.find { it.studentCode == scannedSbd }
                        if (matchedStudent != null) finalStudentName = matchedStudent.name
                    }

                    // 4. TÍNH ĐIỂM
                    val scannedCode = scanResult.examCode ?: "Trống"
                    var correctCount = 0; var wrongCount = 0; var blankCount = 0; var finalScore = 0.0; var totalQuestions = 0
                    var uiList = emptyList<QuestionDetailUI>()

                    if (isThpt && thptExam != null) {
                        totalQuestions = thptExam.config.p1Questions + (thptExam.config.p2Questions * 4) + thptExam.config.p3Questions
                        val matchedCode = requireAnswerKey(scanResult.examCode, thptExam.codes) { it.code }
                        val stats = calculateThptStats(scanResult, matchedCode, thptExam.config)
                        correctCount = stats.correct; wrongCount = stats.wrong; blankCount = stats.blank; finalScore = stats.score
                        uiList = getDetailedQuestionsUI(scanResult, matchedCode, null, true, thptConfig = thptExam.config)
                    } else if (!isThpt && basicExam != null) {
                        totalQuestions = basicExam.questionCount
                        val matchedCode = requireAnswerKey(scanResult.examCode, basicExam.codes) { it.code }
                        val correctAnswersMap = matchedCode.answers
                        scanResult.answers.filter { it.questionNumber in 1..totalQuestions }.forEach { q ->
                            val correctAns = correctAnswersMap[q.questionNumber.toString()]
                            if (q.isError) wrongCount++
                            else if (q.selectedAnswer != null && q.selectedAnswer == correctAns) correctCount++
                            else if (q.selectedAnswer != null) wrongCount++
                            else blankCount++
                        }
                        if (totalQuestions > 0) finalScore = (correctCount.toDouble() / totalQuestions) * 10.0
                        uiList = getDetailedQuestionsUI(scanResult, null, matchedCode.answers, false, basicQuestionCount = basicExam.questionCount)
                    } else {
                        throw IllegalStateException(context.getString(R.string.scan_exam_unavailable))
                    }

                    // Truyền ảnh CỦA OPENCV (đã có sẵn viền SBD/Mã đề) vào để vẽ tiếp đáp án
                    val finalPaintedBitmap = scanResult.processedBitmap?.let {
                        drawAnswersOnImage(it, scanResult, uiList)
                    }

                    // 5. ĐÓNG GÓI KẾT QUẢ VÀ LƯU OFFLINE
                    val finalResult = FinalGradingResult(
                        studentName = finalStudentName,
                        sbd = scannedSbd,
                        examCode = scannedCode,
                        score = finalScore,
                        correctCount = correctCount,
                        wrongCount = wrongCount,
                        blankCount = blankCount,
                        totalQuestions = totalQuestions,
                        processedImage = finalPaintedBitmap,
                        detailedQuestions = uiList,
                        templateId = templateId,
                        originalImagePath = OriginalScanStore.save(context, scanResult.originalBitmap)
                    )

                    historyRepository.saveRecord(context, finalResult, rosterId, finalRosterName)
                    resultsList.add(finalResult)

                } catch (e: Exception) {
                    e.printStackTrace()
                    resultsList.add(
                        FinalGradingResult(
                            studentName = "Lỗi ảnh", sbd = "Lỗi", examCode = "Lỗi", score = 0.0,
                            correctCount = 0, wrongCount = 0, blankCount = 0, totalQuestions = 0,
                            processedImage = null, detailedQuestions = emptyList(), templateId = templateId,
                            errorMessage = gradingErrorMessage(context, e)
                        )
                    )
                }
            }

            // 6. KIỂM TRA TRÙNG LẶP SBD (ĐỂ XUẤT CẢNH BÁO)
            val errors = resultsList.mapIndexedNotNull { index, result ->
                result.errorMessage?.let { context.getString(R.string.scan_image_error, index + 1, it) }
            }
            val warningMsg = (errors + listOfNotNull(generateDuplicateWarning(resultsList, rosterId)))
                .joinToString("\n").takeIf { it.isNotBlank() }
            _batchState.value = BatchState.Finished(resultsList, warningMsg)
        }
    }

    private fun generateDuplicateWarning(results: List<FinalGradingResult>, rosterId: String?): String? {
        if (rosterId == null) return null
        val sbdMap = mutableMapOf<String, MutableList<Int>>()
        results.forEachIndexed { index, result ->
            if (result.sbd != "Lỗi" && result.sbd.isNotBlank()) {
                sbdMap.getOrPut(result.sbd) { mutableListOf() }.add(index + 1)
            }
        }
        val duplicates = sbdMap.filter { it.value.size > 1 }
        if (duplicates.isEmpty()) return null

        val warningBuilder = StringBuilder("- Note: ")
        val duplicateDetails = duplicates.map { (sbd, positions) -> "Bài thi số ${positions.joinToString(" và ")} trùng SBD ($sbd)" }
        warningBuilder.append(duplicateDetails.joinToString("; "))
        return warningBuilder.toString()
    }

    // Tái sử dụng logic tính điểm từ GradingViewModel
    private fun calculateThptStats(scanResult: ScanResult, codeModel: ThptExamCodeModel, config: ThptExamConfig): ThptStats {
        var score = 0.0; var correct = 0; var wrong = 0; var blank = 0
        val p1Weight = if (config.p1Questions > 0) (config.p1Score / config.p1Questions.toDouble()) else 0.0
        val p3Weight = if (config.p3Questions > 0) (config.p3Score / config.p3Questions.toDouble()) else 0.0

        val validP1Questions = 1..config.p1Questions
        val validP2Questions = 41 until (41 + config.p2Questions * 4)
        val validP3Questions = 73 until (73 + config.p3Questions)

        scanResult.answers.forEach { q ->
            var realAns: String? = null
            when (q.questionNumber) {
                in validP1Questions -> {
                    realAns = codeModel.part1[q.questionNumber.toString()]
                    if (q.isError) wrong++
                    else if (q.selectedAnswer == null) blank++
                    else if (q.selectedAnswer == realAns) { correct++; score += p1Weight }
                    else wrong++
                }
                in validP2Questions -> {
                    val offset = q.questionNumber - 41
                    val cau = (offset / 4) + 1
                    val y = when (offset % 4) { 0 -> "a"; 1 -> "b"; 2 -> "c"; else -> "d" }
                    realAns = codeModel.part2[cau.toString()]?.get(y)

                    if (q.isError) wrong++
                    else if (q.selectedAnswer == null) blank++
                    else if (q.selectedAnswer == realAns) correct++
                    else wrong++
                }
                in validP3Questions -> {
                    val cau = q.questionNumber - 72
                    realAns = codeModel.part3[cau.toString()]
                    if (q.isError) wrong++
                    else if (q.selectedAnswer == null) blank++
                    else if (q.selectedAnswer == realAns) { correct++; score += p3Weight }
                    else wrong++
                }
            }
        }

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

    private fun getDetailedQuestionsUI(scanResult: ScanResult, codeModel: ThptExamCodeModel?, basicAnswers: Map<String, String>?, isThpt: Boolean, thptConfig: ThptExamConfig? = null, basicQuestionCount: Int = 0): List<QuestionDetailUI> {
        val uiList = mutableListOf<QuestionDetailUI>()
        val validP1 = if (thptConfig != null) 1..thptConfig.p1Questions else 1..0
        val validP2 = if (thptConfig != null) 41 until (41 + thptConfig.p2Questions * 4) else 1..0
        val validP3 = if (thptConfig != null) 73 until (73 + thptConfig.p3Questions) else 1..0
        val validBasic = 1..basicQuestionCount

        scanResult.answers.forEach { q ->
            val num = q.questionNumber
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
                        correctAns = codeModel.part2[cau.toString()]?.get(y) ?: "?"
                        val status = if (q.isError || q.selectedAnswer == null) AnswerStatus.BLANK else if (q.selectedAnswer == correctAns) AnswerStatus.CORRECT else AnswerStatus.WRONG
                        uiList.add(QuestionDetailUI("${cau}${y}", studentAns, correctAns, status))
                    }
                    in validP3 -> {
                        val cau = num - 72
                        correctAns = codeModel.part3[cau.toString()] ?: "?"
                        val status = if (q.isError || q.selectedAnswer == null) AnswerStatus.BLANK else if (q.selectedAnswer == correctAns) AnswerStatus.CORRECT else AnswerStatus.WRONG
                        uiList.add(QuestionDetailUI((cau + 40).toString(), studentAns, correctAns, status))
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

    private fun drawAnswersOnImage(
        originalBitmap: Bitmap,
        scanResult: ScanResult,
        uiList: List<QuestionDetailUI>
    ): Bitmap {
        val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val paintCorrect = Paint().apply { color = android.graphics.Color.GREEN; style = Paint.Style.STROKE; strokeWidth = 3.5f; isAntiAlias = true }
        val paintWrong = Paint().apply { color = android.graphics.Color.RED; style = Paint.Style.STROKE; strokeWidth = 3.5f; isAntiAlias = true }
        val paintError = Paint().apply { color = android.graphics.Color.YELLOW; style = Paint.Style.STROKE; strokeWidth = 3.5f; isAntiAlias = true }
        val paintEmpty = Paint().apply { color = android.graphics.Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 1.2f; isAntiAlias = true }

        // Vẽ màu cho các ô đáp án
        for (qResult in scanResult.answers) {
            val qNumber = qResult.questionNumber.toString()

            val detailUi = uiList.find { it.displayLabel == qNumber || it.displayLabel.startsWith(qNumber) }
            val status = detailUi?.status ?: AnswerStatus.BLANK

            var statusPaint = paintEmpty

            if (qResult.isError) {
                statusPaint = paintError
            } else {
                statusPaint = when (status) {
                    AnswerStatus.CORRECT -> paintCorrect
                    AnswerStatus.WRONG -> paintWrong
                    AnswerStatus.BLANK -> paintEmpty
                }
            }

            for (bubble in qResult.bubbles) {
                if (bubble.isFilled) {
                    canvas.drawCircle(bubble.x, bubble.y, bubble.radius, statusPaint)
                } else {
                    canvas.drawCircle(bubble.x, bubble.y, bubble.radius, paintEmpty)
                }
            }
        }

        return mutableBitmap
    }
}
