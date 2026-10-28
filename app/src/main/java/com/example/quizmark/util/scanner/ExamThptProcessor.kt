package com.example.quizmark.util.scanner

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import com.example.quizmark.data.model.ScanResult
import com.example.quizmark.data.model.ExamProcessor
import com.example.quizmark.data.model.QuestionResult
import org.opencv.imgproc.Imgproc

class ExamThptProcessor : ExamProcessor {

    // ================= TỌA ĐỘ ƯỚC LƯỢNG CHO PHIẾU THPT 2025 =================
    // 1. THÔNG TIN CÁ NHÂN
    private val roiSBD  = Rect(549, 78, 141, 255)
    private val roiCode = Rect(714, 78, 72, 256)

    // 2. PHẦN I: 40 CÂU (4 Cột, mỗi cột 10 câu)
    private val roiP1_Col1 = Rect(53, 435, 147, 197)
    private val roiP1_Col2 = Rect(241, 435, 147, 197)
    private val roiP1_Col3 = Rect(428, 435, 147, 197)
    private val roiP1_Col4 = Rect(615, 435, 147, 197)

    // 3. PHẦN II: ĐÚNG/SAI (8 Hộp - Mỗi hộp 4 hàng x 2 cột Đ/S)
    private val roiP2_Q1 = Rect(55, 719, 68, 79)
    private val roiP2_Q2 = Rect(132, 719, 68, 79)
    private val roiP2_Q3 = Rect(244, 719, 68, 79)
    private val roiP2_Q4 = Rect(319, 719, 68, 79)
    private val roiP2_Q5 = Rect(430, 719, 68, 79)
    private val roiP2_Q6 = Rect(505, 719, 68, 79)
    private val roiP2_Q7 = Rect(618, 719, 68, 79)
    private val roiP2_Q8 = Rect(693, 719, 68, 79)

    // 4. PHẦN III: ĐIỀN SỐ (6 Hộp - Mỗi hộp 4 cột x 12 hàng)
    private val roiP3_Q1 = Rect(53, 894, 101, 244)
    private val roiP3_Q2 = Rect(173, 894, 101, 244)
    private val roiP3_Q3 = Rect(293, 894, 101, 244)
    private val roiP3_Q4 = Rect(412, 894, 101, 244)
    private val roiP3_Q5 = Rect(533, 894, 101, 244)
    private val roiP3_Q6 = Rect(653, 894, 101, 244)

    override fun process(bitmap: Bitmap): ScanResult {
        val colorMat = Mat()
        var grayMat: Mat? = null
        var binaryMat: Mat? = null

        try {
            Utils.bitmapToMat(bitmap, colorMat)
            grayMat = Mat()
            Imgproc.cvtColor(colorMat, grayMat, Imgproc.COLOR_BGRA2GRAY)
            binaryMat = Mat()
// Dùng Adaptive Threshold để lọc sạch bóng râm và nhiễu sáng
            Imgproc.adaptiveThreshold(
                grayMat,
                binaryMat,
                255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV,
                51,   // Kích thước vùng quét (Block Size)
                10.0  // Hệ số trừ nhiễu bóng râm
            )
            val sbdResult = readGridNumber(Mat(binaryMat, roiSBD), colorMat, roiSBD, 10, 8)
            val codeResult = readGridNumber(Mat(binaryMat, roiCode), colorMat, roiCode, 10, 4)

            val answers = mutableListOf<QuestionResult>()

            answers.addAll(readPart1Answers(Mat(binaryMat, roiP1_Col1), colorMat, roiP1_Col1, 1, 10))
            answers.addAll(readPart1Answers(Mat(binaryMat, roiP1_Col2), colorMat, roiP1_Col2, 11, 20))
            answers.addAll(readPart1Answers(Mat(binaryMat, roiP1_Col3), colorMat, roiP1_Col3, 21, 30))
            answers.addAll(readPart1Answers(Mat(binaryMat, roiP1_Col4), colorMat, roiP1_Col4, 31, 40))

            val p2Rois = listOf(roiP2_Q1, roiP2_Q2, roiP2_Q3, roiP2_Q4, roiP2_Q5, roiP2_Q6, roiP2_Q7, roiP2_Q8)
            var currentP2Index = 41
            for (roi in p2Rois) {
                answers.addAll(readPart2Box(Mat(binaryMat, roi), colorMat, roi, currentP2Index))
                currentP2Index += 4
            }

            val p3Rois = listOf(roiP3_Q1, roiP3_Q2, roiP3_Q3, roiP3_Q4, roiP3_Q5, roiP3_Q6)
            var currentP3Index = 73
            for (roi in p3Rois) {
                answers.add(readPart3Box(Mat(binaryMat, roi), colorMat, roi, currentP3Index))
                currentP3Index++
            }

            val debugBitmap = Bitmap.createBitmap(colorMat.cols(), colorMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(colorMat, debugBitmap)

            return ScanResult(sbdResult, codeResult, answers, debugBitmap)

        } finally {
            colorMat.release()
            grayMat?.release()
            binaryMat?.release()
        }
    }

    // ================= CÁC HÀM XỬ LÝ LÕI =================

    private fun readGridNumber(binaryRoi: Mat, colorMat: Mat, globalRect: Rect, rows: Int, cols: Int): String? {
        val cellW = binaryRoi.width().toDouble() / cols
        val cellH = binaryRoi.height().toDouble() / rows
        val radius = (Math.min(cellW, cellH) * 0.38).toInt()
        val circleArea = Math.PI * radius * radius

        val mask = Mat.zeros(Size(cellW, cellH), CvType.CV_8UC1)
        Imgproc.circle(mask, Point(cellW / 2.0, cellH / 2.0), radius, Scalar(255.0), -1)

        val result = StringBuilder()
        var hasError = false

        for (c in 0 until cols) {
            var filledRow = -1
            var fillCount = 0
            for (r in 0 until rows) {
                val cell = Mat(binaryRoi, Rect((c * cellW).toInt(), (r * cellH).toInt(), cellW.toInt(), cellH.toInt()))
                val maskedCell = Mat()
                Core.bitwise_and(cell, mask, maskedCell)
                if (Core.countNonZero(maskedCell) > circleArea * 0.50) {
                    filledRow = r
                    fillCount++
                    Imgproc.circle(colorMat, Point(globalRect.x + c * cellW + cellW / 2.0, globalRect.y + r * cellH + cellH / 2.0), radius, Scalar(0.0, 255.0, 0.0), 3)
                } else {
                    Imgproc.circle(colorMat, Point(globalRect.x + c * cellW + cellW / 2.0, globalRect.y + r * cellH + cellH / 2.0), radius, Scalar(0.0, 255.0, 255.0), 2)
                }
                cell.release(); maskedCell.release()
            }
            if (fillCount != 1) hasError = true else result.append(filledRow)
        }
        mask.release()
        return if (hasError) null else result.toString()
    }

    private fun readPart1Answers(binaryRoi: Mat, colorMat: Mat, globalRect: Rect, startQ: Int, endQ: Int): List<QuestionResult> {
        val rows = endQ - startQ + 1
        val cols = 4
        val cellW = binaryRoi.width().toDouble() / cols
        val cellH = binaryRoi.height().toDouble() / rows
        val radius = (Math.min(cellW, cellH) * 0.4).toInt()
        val circleArea = Math.PI * radius * radius

        val mask = Mat.zeros(Size(cellW, cellH), CvType.CV_8UC1)
        Imgproc.circle(mask, Point(cellW / 2.0, cellH / 2.0), radius, Scalar(255.0), -1)

        val options = listOf("A", "B", "C", "D")
        val results = mutableListOf<QuestionResult>()

        for (r in 0 until rows) {
            var selectedAns: String? = null
            var fillCount = 0
            for (c in 0 until cols) {
                val cell = Mat(binaryRoi, Rect((c * cellW).toInt(), (r * cellH).toInt(), cellW.toInt(), cellH.toInt()))
                val maskedCell = Mat()
                Core.bitwise_and(cell, mask, maskedCell)
                if (Core.countNonZero(maskedCell) > circleArea * 0.50) {
                    selectedAns = options[c]
                    fillCount++
                    Imgproc.circle(colorMat, Point(globalRect.x + c * cellW + cellW / 2.0, globalRect.y + r * cellH + cellH / 2.0), radius, Scalar(0.0, 255.0, 0.0), 3)
                } else {
                    Imgproc.circle(colorMat, Point(globalRect.x + c * cellW + cellW / 2.0, globalRect.y + r * cellH + cellH / 2.0), radius, Scalar(0.0, 255.0, 255.0), 2)
                }
                cell.release(); maskedCell.release()
            }
            results.add(QuestionResult(startQ + r, if (fillCount != 1) null else selectedAns, fillCount != 1))
        }
        mask.release()
        return results
    }

    private fun readPart2Box(binaryRoi: Mat, colorMat: Mat, globalRect: Rect, startQ: Int): List<QuestionResult> {
        val rows = 4
        val cols = 2
        val cellW = binaryRoi.width().toDouble() / cols
        val cellH = binaryRoi.height().toDouble() / rows
        val radius = (Math.min(cellW, cellH) * 0.36).toInt()
        val circleArea = Math.PI * radius * radius

        val mask = Mat.zeros(Size(cellW, cellH), CvType.CV_8UC1)
        Imgproc.circle(mask, Point(cellW / 2.0, cellH / 2.0), radius, Scalar(255.0), -1)

        val options = listOf("D", "S") // Đúng hoặc Sai
        val results = mutableListOf<QuestionResult>()

        for (r in 0 until rows) {
            var selectedAns: String? = null
            var fillCount = 0
            for (c in 0 until cols) {
                val cell = Mat(binaryRoi, Rect((c * cellW).toInt(), (r * cellH).toInt(), cellW.toInt(), cellH.toInt()))
                val maskedCell = Mat()
                Core.bitwise_and(cell, mask, maskedCell)
                if (Core.countNonZero(maskedCell) > circleArea * 0.50) {
                    selectedAns = options[c]
                    fillCount++
                    Imgproc.circle(colorMat, Point(globalRect.x + c * cellW + cellW / 2.0, globalRect.y + r * cellH + cellH / 2.0), radius, Scalar(0.0, 255.0, 0.0), 3)
                } else {
                    Imgproc.circle(colorMat, Point(globalRect.x + c * cellW + cellW / 2.0, globalRect.y + r * cellH + cellH / 2.0), radius, Scalar(0.0, 255.0, 255.0), 2)
                }
                cell.release(); maskedCell.release()
            }
            results.add(QuestionResult(startQ + r, if (fillCount != 1) null else selectedAns, fillCount != 1))
        }
        mask.release()
        return results
    }

    private fun readPart3Box(binaryRoi: Mat, colorMat: Mat, globalRect: Rect, qIndex: Int): QuestionResult {
        val rows = 12 // Hàng 0: (-), Hàng 1: (,), Hàng 2-11: (0-9)
        val cols = 4
        val cellW = binaryRoi.width().toDouble() / cols
        val cellH = binaryRoi.height().toDouble() / rows
        val radius = (Math.min(cellW, cellH) * 0.36).toInt()
        val circleArea = Math.PI * radius * radius

        val mask = Mat.zeros(Size(cellW, cellH), CvType.CV_8UC1)
        Imgproc.circle(mask, Point(cellW / 2.0, cellH / 2.0), radius, Scalar(255.0), -1)

        val colResults = mutableListOf<String>()

        for (c in 0 until cols) {
            var filledChar = ""
            var fillCount = 0

            for (r in 0 until rows) {
                // Chỉ kiểm tra những vị trí thực sự có vòng tròn in trên giấy
                val isValidBubble = when (r) {
                    0 -> c == 0             // Dấu âm (-) chỉ có ở cột đầu
                    1 -> c == 1 || c == 2   // Dấu phẩy (,) chỉ có ở 2 cột giữa
                    else -> true            // Số (0-9) có ở mọi cột
                }

                if (!isValidBubble) continue

                val cell = Mat(binaryRoi, Rect((c * cellW).toInt(), (r * cellH).toInt(), cellW.toInt(), cellH.toInt()))
                val maskedCell = Mat()
                Core.bitwise_and(cell, mask, maskedCell)

                val globalCenter = Point(globalRect.x + c * cellW + cellW / 2.0, globalRect.y + r * cellH + cellH / 2.0)

                if (Core.countNonZero(maskedCell) > circleArea * 0.50) {
                    filledChar = when (r) {
                        0 -> "-"
                        1 -> ","
                        else -> (r - 2).toString()
                    }
                    fillCount++
                    Imgproc.circle(colorMat, globalCenter, radius, Scalar(0.0, 255.0, 0.0), 3)
                } else {
                    Imgproc.circle(colorMat, globalCenter, radius, Scalar(0.0, 255.0, 255.0), 2)
                }
                cell.release(); maskedCell.release()
            }

            if (fillCount == 1) {
                colResults.add(filledChar)
            } else if (fillCount == 0) {
                colResults.add(" ") // Cột trống
            } else {
                colResults.add("E") // Lỗi tô đúp
            }
        }
        mask.release()

        // QUY TẮC GHÉP CHUỖI ĐIỀN SỐ THPT
        // 1. Nếu cột 1 bỏ trống -> Trả về null (Không làm bài)
        if (colResults[0] == " ") {
            return QuestionResult(qIndex, null, false)
        }

        // 2. Đọc từ trái sang phải, gặp cột trống thì dừng
        val finalAnswerBuilder = StringBuilder()
        var hasError = false

        for (char in colResults) {
            if (char == " ") {
                break // Dừng ngay khi gặp khoảng trắng
            } else if (char == "E") {
                hasError = true // Lỗi tô đúp
                break
            } else {
                finalAnswerBuilder.append(char)
            }
        }

        val finalAnswer = finalAnswerBuilder.toString()
        if (hasError || finalAnswer.isEmpty()) {
            return QuestionResult(qIndex, null, true)
        }

        return QuestionResult(qIndex, finalAnswer, false)
    }
}