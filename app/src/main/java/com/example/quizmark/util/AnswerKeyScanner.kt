package com.example.quizmark.util

import android.graphics.Bitmap
import com.example.quizmark.data.model.ExamCodeModel
import com.example.quizmark.data.model.ScanResult
import com.example.quizmark.data.model.ThptExamCodeModel
import com.example.quizmark.util.scanner.ExamProcessorFactory
import com.example.quizmark.util.scanner.ScannerCore

data class AnswerKeyImportSummary(
    val targetCode: String,
    val filledCount: Int,
    val totalCount: Int,
    val blankLabels: List<String>,
    val errorLabels: List<String>,
    val scannedExamCode: String?
) {
    val hasCodeMismatch: Boolean
        get() = !scannedExamCode.isNullOrBlank() && scannedExamCode != targetCode

    val hasIssues: Boolean
        get() = blankLabels.isNotEmpty() || errorLabels.isNotEmpty() || hasCodeMismatch
}

data class BasicKeyImportResult(
    val codeModel: ExamCodeModel,
    val summary: AnswerKeyImportSummary
)

data class ThptKeyImportResult(
    val codeModel: ThptExamCodeModel,
    val summary: AnswerKeyImportSummary
)

sealed class StaticKeyScanResult {
    data class Success(val scanResult: ScanResult) : StaticKeyScanResult()
    object CornersNotFound : StaticKeyScanResult()
    object UnsupportedTemplate : StaticKeyScanResult()
}

object AnswerKeyScanner {

    fun extractBasic(
        scanResult: ScanResult,
        targetCode: String,
        questionCount: Int
    ): BasicKeyImportResult {
        val byNumber = scanResult.answers.associateBy { it.questionNumber }
        val answersMap = LinkedHashMap<String, String>()
        val blanks = mutableListOf<String>()
        val errors = mutableListOf<String>()

        for (qNum in 1..questionCount) {
            val q = byNumber[qNum]
            when {
                q == null -> blanks.add(qNum.toString())
                q.isError -> errors.add(qNum.toString())
                !q.selectedAnswer.isNullOrBlank() -> answersMap[qNum.toString()] = q.selectedAnswer
                else -> blanks.add(qNum.toString())
            }
        }

        return BasicKeyImportResult(
            codeModel = ExamCodeModel(code = targetCode, answers = answersMap),
            summary = AnswerKeyImportSummary(
                targetCode = targetCode,
                filledCount = answersMap.size,
                totalCount = questionCount,
                blankLabels = blanks,
                errorLabels = errors,
                scannedExamCode = scanResult.examCode
            )
        )
    }

    fun extractThpt(
        scanResult: ScanResult,
        targetCode: String,
        p1Q: Int,
        p2Q: Int,
        p3Q: Int
    ): ThptKeyImportResult {
        val byNumber = scanResult.answers.associateBy { it.questionNumber }
        val part1 = LinkedHashMap<String, String>()
        val part2 = LinkedHashMap<String, Map<String, String>>()
        val part3 = LinkedHashMap<String, String>()
        val blanks = mutableListOf<String>()
        val errors = mutableListOf<String>()
        var filledCount = 0

        // Phần I: câu 1..p1Q (questionNumber 1..40)
        for (qNum in 1..p1Q) {
            val q = byNumber[qNum]
            when {
                q == null -> blanks.add("I.$qNum")
                q.isError -> errors.add("I.$qNum")
                !q.selectedAnswer.isNullOrBlank() -> {
                    part1[qNum.toString()] = q.selectedAnswer
                    filledCount++
                }
                else -> blanks.add("I.$qNum")
            }
        }

        // Phần II: p2Q câu, mỗi câu 4 ý a/b/c/d (questionNumber 41..72)
        val subKeys = listOf("a", "b", "c", "d")
        for (cau in 1..p2Q) {
            val subMap = LinkedHashMap<String, String>()
            for (j in 0 until 4) {
                val qIndex = 41 + (cau - 1) * 4 + j
                val subKey = subKeys[j]
                val label = "II.$cau$subKey"
                val q = byNumber[qIndex]
                when {
                    q == null -> blanks.add(label)
                    q.isError -> errors.add(label)
                    !q.selectedAnswer.isNullOrBlank() -> {
                        subMap[subKey] = q.selectedAnswer
                        filledCount++
                    }
                    else -> blanks.add(label)
                }
            }
            if (subMap.isNotEmpty()) {
                part2[cau.toString()] = subMap
            }
        }

        // Phần III: p3Q câu điền số (questionNumber 73..78)
        for (cau in 1..p3Q) {
            val qIndex = 72 + cau
            val label = "III.$cau"
            val q = byNumber[qIndex]
            when {
                q == null -> blanks.add(label)
                q.isError -> errors.add(label)
                !q.selectedAnswer.isNullOrBlank() -> {
                    part3[cau.toString()] = q.selectedAnswer
                    filledCount++
                }
                else -> blanks.add(label)
            }
        }

        val totalItems = p1Q + (p2Q * 4) + p3Q
        return ThptKeyImportResult(
            codeModel = ThptExamCodeModel(
                code = targetCode,
                part1 = part1,
                part2 = part2,
                part3 = part3
            ),
            summary = AnswerKeyImportSummary(
                targetCode = targetCode,
                filledCount = filledCount,
                totalCount = totalItems,
                blankLabels = blanks,
                errorLabels = errors,
                scannedExamCode = scanResult.examCode
            )
        )
    }

    fun scanStaticBitmap(bitmap: Bitmap, templateId: Int): StaticKeyScanResult {
        val processor = ExamProcessorFactory.create(templateId)
            ?: return StaticKeyScanResult.UnsupportedTemplate
        val warpedBitmap = ScannerCore().processStaticImage(bitmap)
            ?: return StaticKeyScanResult.CornersNotFound

        return try {
            val scanResult = processor.process(warpedBitmap)
            scanResult.processedBitmap?.takeIf { it !== warpedBitmap }?.recycle()
            StaticKeyScanResult.Success(
                scanResult.copy(processedBitmap = null, originalBitmap = null)
            )
        } finally {
            warpedBitmap.recycle()
        }
    }

    fun formatLabelPreview(labels: List<String>, maxItems: Int = 12): String {
        if (labels.size <= maxItems) return labels.joinToString(", ")
        val head = labels.take(maxItems).joinToString(", ")
        return "$head… (+${labels.size - maxItems})"
    }
}
