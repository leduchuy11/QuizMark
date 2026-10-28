package com.example.quizmark.util.scanner

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import com.example.quizmark.data.model.ScanResult
import com.example.quizmark.data.model.ExamProcessor
import com.example.quizmark.data.model.QuestionResult
import org.opencv.imgproc.Imgproc

class Exam50Processor : ExamProcessor {

    private val roiSBD  = Rect(601, 80, 107, 254)   // SBD: 6 cột, 10 hàng
    private val roiCode = Rect(730, 80, 54, 254)    // Mã đề: 3 cột, 10 hàng

    private val roiCol1 = Rect(128, 445, 233, 717)   // Cột 1: Câu 1-25
    private val roiCol2 = Rect(495, 445, 233, 717)   // Cột 2: Câu 26-50

    override fun process(bitmap: Bitmap): ScanResult {
        val colorMat = Mat()
        var grayMat: Mat? = null
        var binaryMat: Mat? = null
        var sbdMat: Mat? = null
        var codeMat: Mat? = null
        var col1Mat: Mat? = null
        var col2Mat: Mat? = null

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
            sbdMat = Mat(binaryMat, roiSBD)
            val sbdResult = readGridNumber(sbdMat, colorMat, roiSBD, 10, 6)

            codeMat = Mat(binaryMat, roiCode)
            val codeResult = readGridNumber(codeMat, colorMat, roiCode, 10, 3)

            val answers = mutableListOf<QuestionResult>()

            col1Mat = Mat(binaryMat, roiCol1)
            answers.addAll(readAnswers(col1Mat, colorMat, roiCol1, 1, 25))

            col2Mat = Mat(binaryMat, roiCol2)
            answers.addAll(readAnswers(col2Mat, colorMat, roiCol2, 26, 50))

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
        }
    }

    private fun readGridNumber(binaryRoi: Mat, colorMat: Mat, globalRect: Rect, rows: Int, cols: Int): String? {
        val cellW = binaryRoi.width().toDouble() / cols
        val cellH = binaryRoi.height().toDouble() / rows
        val radius = (Math.min(cellW, cellH) * 0.4).toInt()
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

            if (fillCount != 1) {
                hasError = true
            } else {
                result.append(filledRow)
            }
        }
        mask.release()
        return if (hasError) null else result.toString()
    }

    private fun readAnswers(binaryRoi: Mat, colorMat: Mat, globalRect: Rect, startQ: Int, endQ: Int): List<QuestionResult> {
        val rows = endQ - startQ + 1
        val cols = 4
        val cellW = binaryRoi.width().toDouble() / cols
        val cellH = binaryRoi.height().toDouble() / rows
        val radius = (Math.min(cellW, cellH) * 0.36).toInt()
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