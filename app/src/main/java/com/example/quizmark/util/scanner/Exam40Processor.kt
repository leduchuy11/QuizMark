package com.example.quizmark.util.scanner

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import com.example.quizmark.data.model.ScanResult
import com.example.quizmark.data.model.ExamProcessor
import com.example.quizmark.data.model.QuestionResult
import org.opencv.imgproc.Imgproc

class Exam40Processor : ExamProcessor {

    // TỌA ĐỘ CHUẨN ĐÃ MỞ RỘNG CHIỀU RỘNG ĐỂ TRÙM KÍN CÁC CỘT SBD VÀ MÃ ĐỀ
    // Cấu trúc: Rect(x, y, width, height)
    private val roiSBD  = Rect(90, 338, 203, 386)   // SBD: 6 cột
    private val roiCode = Rect(319, 338, 103, 386)   // Mã đề: 3 cột

    private val roiCol1 = Rect(520, 337, 130, 386)   // Cột 1: Câu 1-10
    private val roiCol2 = Rect(123, 757, 130, 386)   // Cột 2: Câu 11-20
    private val roiCol3 = Rect(321, 757, 130, 386)   // Cột 3: Câu 21-30
    private val roiCol4 = Rect(520, 757, 130, 386)   // Cột 4: Câu 31-40

    override fun process(bitmap: Bitmap): ScanResult {
        val colorMat = Mat()
        var grayMat: Mat? = null
        var binaryMat: Mat? = null
        var sbdMat: Mat? = null
        var codeMat: Mat? = null
        var col1Mat: Mat? = null
        var col2Mat: Mat? = null
        var col3Mat: Mat? = null
        var col4Mat: Mat? = null

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
            // 1. Quét Số Báo Danh (6 cột) và Mã Đề (3 cột)
            sbdMat = Mat(binaryMat, roiSBD)
            val sbdResult = readGridNumber(sbdMat, colorMat, roiSBD, 10, 6)

            codeMat = Mat(binaryMat, roiCode)
            val codeResult = readGridNumber(codeMat, colorMat, roiCode, 10, 3)

            // 2. Quét 40 câu đáp án
            val answers = mutableListOf<QuestionResult>()

            col1Mat = Mat(binaryMat, roiCol1)
            answers.addAll(readAnswers(col1Mat, colorMat, roiCol1, 1, 10))

            col2Mat = Mat(binaryMat, roiCol2)
            answers.addAll(readAnswers(col2Mat, colorMat, roiCol2, 11, 20))

            col3Mat = Mat(binaryMat, roiCol3)
            answers.addAll(readAnswers(col3Mat, colorMat, roiCol3, 21, 30))

            col4Mat = Mat(binaryMat, roiCol4)
            answers.addAll(readAnswers(col4Mat, colorMat, roiCol4, 31, 40))

            val debugBitmap = Bitmap.createBitmap(colorMat.cols(), colorMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(colorMat, debugBitmap)

            return ScanResult(
                sbd = sbdResult,
                examCode = codeResult,
                answers = answers,
                processedBitmap = debugBitmap
            )
        } finally {
            colorMat.release()
            grayMat?.release()
            binaryMat?.release()
            sbdMat?.release()
            codeMat?.release()
            col1Mat?.release()
            col2Mat?.release()
            col3Mat?.release()
            col4Mat?.release()
        }
    }

    private fun readGridNumber(binaryRoi: Mat, colorMat: Mat, globalRect: Rect, rows: Int, cols: Int): String? {
        val cellW = binaryRoi.width() / cols
        val cellH = binaryRoi.height() / rows
        val radius = (Math.min(cellW, cellH) * 0.4).toInt()
        val circleArea = Math.PI * radius * radius // Đã sửa lỗi thiếu dấu nhân (*)

        val mask = Mat.zeros(Size(cellW.toDouble(), cellH.toDouble()), CvType.CV_8UC1)
        Imgproc.circle(mask, Point(cellW / 2.0, cellH / 2.0), radius, Scalar(255.0), -1)

        val result = StringBuilder()

        for (c in 0 until cols) {
            var filledRow = -1
            var fillCount = 0

            for (r in 0 until rows) {
                val cell = Mat(binaryRoi, Rect(c * cellW, r * cellH, cellW, cellH))
                val maskedCell = Mat()
                Core.bitwise_and(cell, mask, maskedCell)

                val whitePixels = Core.countNonZero(maskedCell)
                val isFilled = whitePixels > circleArea * 0.50

                val globalCenter = Point(globalRect.x + c * cellW + cellW / 2.0, globalRect.y + r * cellH + cellH / 2.0)

                if (isFilled) {
                    filledRow = r
                    fillCount++
                    Imgproc.circle(colorMat, globalCenter, radius, Scalar(0.0, 255.0, 0.0), 3)
                } else {
                    Imgproc.circle(colorMat, globalCenter, radius, Scalar(0.0, 255.0, 255.0), 2)
                }

                cell.release()
                maskedCell.release()
            }

            if (fillCount != 1) return null
            result.append(filledRow)
        }
        mask.release()
        return result.toString()
    }

    private fun readAnswers(binaryRoi: Mat, colorMat: Mat, globalRect: Rect, startQ: Int, endQ: Int): List<QuestionResult> {
        val rows = endQ - startQ + 1
        val cols = 4
        val cellW = binaryRoi.width() / cols
        val cellH = binaryRoi.height() / rows
        val radius = (Math.min(cellW, cellH) * 0.4).toInt()
        val circleArea = Math.PI * radius * radius // Đã sửa lỗi thiếu dấu nhân (*)

        val mask = Mat.zeros(Size(cellW.toDouble(), cellH.toDouble()), CvType.CV_8UC1)
        Imgproc.circle(mask, Point(cellW / 2.0, cellH / 2.0), radius, Scalar(255.0), -1)

        val options = listOf("A", "B", "C", "D")
        val results = mutableListOf<QuestionResult>()

        for (r in 0 until rows) {
            var selectedAns: String? = null
            var fillCount = 0

            for (c in 0 until cols) {
                val cell = Mat(binaryRoi, Rect(c * cellW, r * cellH, cellW, cellH))
                val maskedCell = Mat()
                Core.bitwise_and(cell, mask, maskedCell)

                val whitePixels = Core.countNonZero(maskedCell)
                val isFilled = whitePixels > circleArea * 0.50

                val globalCenter = Point(globalRect.x + c * cellW + cellW / 2.0, globalRect.y + r * cellH + cellH / 2.0)

                if (isFilled) {
                    selectedAns = options[c]
                    fillCount++
                    Imgproc.circle(colorMat, globalCenter, radius, Scalar(0.0, 255.0, 0.0), 3)
                } else {
                    Imgproc.circle(colorMat, globalCenter, radius, Scalar(0.0, 255.0, 255.0), 2)
                }

                cell.release()
                maskedCell.release()
            }

            val isError = fillCount != 1
            results.add(QuestionResult(startQ + r, if (isError) null else selectedAns, isError))
        }
        mask.release()
        return results
    }
}