package com.example.quizmark.util.scanner

import android.graphics.Bitmap
import com.example.quizmark.data.model.BubbleInfo
import org.opencv.android.Utils
import org.opencv.core.*
import com.example.quizmark.data.model.ScanResult
import com.example.quizmark.data.model.ExamProcessor
import com.example.quizmark.data.model.QuestionResult
import org.opencv.imgproc.Imgproc

class Exam120Processor : ExamProcessor {

    private val roiSBD  = Rect(601, 78, 107, 254)    // SBD: 6 cột, 10 hàng
    private val roiCode = Rect(731, 78, 55, 254)      // Mã đề: 3 cột, 10 hàng

    private val roiCol1 = Rect(61, 389, 129, 779)   // Cột 1: Câu 1-30 (30 hàng)
    private val roiCol2 = Rect(257, 389, 129, 779)   // Cột 2: Câu 31-60 (30 hàng)
    private val roiCol3 = Rect(448, 389, 129, 779)   // Cột 3: Câu 61-90 (30 hàng)
    private val roiCol4 = Rect(642, 389, 129, 779)   // Cột 4: Câu 91-120 (30 hàng)

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

            // 2. Quét 120 câu đáp án (4 cột, mỗi cột 30 câu)
            val answers = mutableListOf<QuestionResult>()

            col1Mat = Mat(binaryMat, roiCol1)
            answers.addAll(readAnswers(col1Mat, colorMat, roiCol1, 1, 30))

            col2Mat = Mat(binaryMat, roiCol2)
            answers.addAll(readAnswers(col2Mat, colorMat, roiCol2, 31, 60))

            col3Mat = Mat(binaryMat, roiCol3)
            answers.addAll(readAnswers(col3Mat, colorMat, roiCol3, 61, 90))

            col4Mat = Mat(binaryMat, roiCol4)
            answers.addAll(readAnswers(col4Mat, colorMat, roiCol4, 91, 120))

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
            val bubbles = mutableListOf<Pair<Point, Boolean>>()

            for (r in 0 until rows) {
                val cell = Mat(binaryRoi, Rect((c * cellW).toInt(), (r * cellH).toInt(), cellW.toInt(), cellH.toInt()))
                val maskedCell = Mat()
                Core.bitwise_and(cell, mask, maskedCell)

                val isFilled = Core.countNonZero(maskedCell) > circleArea * 0.50
                if (isFilled) {
                    filledRow = r
                    fillCount++
                }

                val globalCenter = Point(globalRect.x + c * cellW + cellW / 2.0, globalRect.y + r * cellH + cellH / 2.0)
                bubbles.add(Pair(globalCenter, isFilled))

                cell.release()
                maskedCell.release()
            }

            val isColError = fillCount != 1
            if (isColError) hasError = true else result.append(filledRow)

            // VẼ GIAO DIỆN SBD VÀ MÃ ĐỀ NGAY TRÊN OPENCV
            for (bubble in bubbles) {
                if (bubble.second) {
                    val color = if (isColError) Scalar(0.0, 255.0, 255.0) else Scalar(0.0, 255.0, 0.0)
                    Imgproc.circle(colorMat, bubble.first, radius, color, 3)
                } else {
                    Imgproc.circle(colorMat, bubble.first, radius, Scalar(255.0, 255.0, 255.0), 1)
                }
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
            val bubbles = mutableListOf<BubbleInfo>()

            for (c in 0 until cols) {
                val cell = Mat(binaryRoi, Rect((c * cellW).toInt(), (r * cellH).toInt(), cellW.toInt(), cellH.toInt()))
                val maskedCell = Mat()
                Core.bitwise_and(cell, mask, maskedCell)

                val isFilled = Core.countNonZero(maskedCell) > circleArea * 0.50
                if (isFilled) {
                    selectedAns = options[c]
                    fillCount++
                }

                val cx = (globalRect.x + c * cellW + cellW / 2.0).toFloat()
                val cy = (globalRect.y + r * cellH + cellH / 2.0).toFloat()
                bubbles.add(BubbleInfo(cx, cy, radius.toFloat(), isFilled))

                cell.release()
                maskedCell.release()
            }

            val isError = fillCount > 1
            results.add(QuestionResult(startQ + r, if (fillCount != 1) null else selectedAns, isError, bubbles))
        }
        mask.release()
        return results
    }
}