package com.example.quizmark.util.scanner

import android.graphics.Bitmap
import com.example.quizmark.data.model.BubbleInfo
import org.opencv.android.Utils
import org.opencv.core.*
import com.example.quizmark.data.model.ScanResult
import com.example.quizmark.data.model.ExamProcessor
import com.example.quizmark.data.model.QuestionResult
import org.opencv.imgproc.Imgproc

class ExamThptProcessor : ExamProcessor {

    // 1. THÔNG TIN CÁ NHÂN
    private val roiSBD  = Rect(549, 84, 138, 253)
    private val roiCode = Rect(713, 84, 70, 253)

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
        // Chỉ khác null ở bản debug khi bật "Thu dữ liệu ô tô" (xem BubbleDatasetDumper)
        val dump = BubbleDatasetDumper.beginSheet(templateId = 1)
        val colorMat = Mat()
        var grayMat: Mat? = null
        var binaryMat: Mat? = null

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
            val sbdResult = readGridNumber(Mat(binaryMat, roiSBD), colorMat, roiSBD, 10, 8, grayMat, "sbd", dump)
            val codeResult = readGridNumber(Mat(binaryMat, roiCode), colorMat, roiCode, 10, 4, grayMat, "code", dump)

            val answers = mutableListOf<QuestionResult>()

            answers.addAll(readPart1Answers(Mat(binaryMat, roiP1_Col1), colorMat, roiP1_Col1, 1, 10, grayMat, dump))
            answers.addAll(readPart1Answers(Mat(binaryMat, roiP1_Col2), colorMat, roiP1_Col2, 11, 20, grayMat, dump))
            answers.addAll(readPart1Answers(Mat(binaryMat, roiP1_Col3), colorMat, roiP1_Col3, 21, 30, grayMat, dump))
            answers.addAll(readPart1Answers(Mat(binaryMat, roiP1_Col4), colorMat, roiP1_Col4, 31, 40, grayMat, dump))

            val p2Rois = listOf(roiP2_Q1, roiP2_Q2, roiP2_Q3, roiP2_Q4, roiP2_Q5, roiP2_Q6, roiP2_Q7, roiP2_Q8)
            var currentP2Index = 41
            for (roi in p2Rois) {
                answers.addAll(readPart2Box(Mat(binaryMat, roi), colorMat, roi, currentP2Index, grayMat, dump))
                currentP2Index += 4
            }

            val p3Rois = listOf(roiP3_Q1, roiP3_Q2, roiP3_Q3, roiP3_Q4, roiP3_Q5, roiP3_Q6)
            var currentP3Index = 73
            for (roi in p3Rois) {
                answers.add(readPart3Box(Mat(binaryMat, roi), colorMat, roi, currentP3Index, grayMat, dump))
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

    // ================= CÁC HÀM XỬ LÝ =================

    private fun readGridNumber(
        binaryRoi: Mat, colorMat: Mat, globalRect: Rect, rows: Int, cols: Int,
        gray: Mat, region: String, dump: BubbleDatasetDumper.Session?
    ): String? {
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
            val bubbles = mutableListOf<Pair<Point, Boolean>>()

            for (r in 0 until rows) {
                val cell = Mat(binaryRoi, Rect((c * cellW).toInt(), (r * cellH).toInt(), cellW.toInt(), cellH.toInt()))
                val maskedCell = Mat()
                Core.bitwise_and(cell, mask, maskedCell)

                val isFilled = Core.countNonZero(maskedCell) > circleArea * 0.50
                dump?.save(gray, Rect(globalRect.x + (c * cellW).toInt(), globalRect.y + (r * cellH).toInt(), cellW.toInt(), cellH.toInt()), region, "c$c", "$r", isFilled)
                if (isFilled) {
                    filledRow = r
                    fillCount++
                }

                val globalCenter = Point(globalRect.x + c * cellW + cellW / 2.0, globalRect.y + r * cellH + cellH / 2.0)
                bubbles.add(Pair(globalCenter, isFilled))

                cell.release(); maskedCell.release()
            }

            val isColError = fillCount != 1
            if (isColError) hasError = true else result.append(filledRow)


            // VẼ GIAO DIỆN CHO CỘT SBD / MÃ ĐỀ
            for (bubble in bubbles) {
                if (bubble.second) {
                    val color = if (isColError) Scalar(0.0, 255.0, 0.0) else Scalar(0.0, 255.0, 0.0)
                    Imgproc.circle(colorMat, bubble.first, radius, color, 2)
                } else {
                    Imgproc.circle(colorMat, bubble.first, radius, Scalar(255.0, 255.0, 255.0), 1)
                }
            }
        }
        mask.release()
        return if (hasError) null else result.toString()
    }

    private fun readPart1Answers(
        binaryRoi: Mat, colorMat: Mat, globalRect: Rect, startQ: Int, endQ: Int,
        gray: Mat, dump: BubbleDatasetDumper.Session?
    ): List<QuestionResult> {
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
            val bubbles = mutableListOf<BubbleInfo>()

            for (c in 0 until cols) {
                val cell = Mat(binaryRoi, Rect((c * cellW).toInt(), (r * cellH).toInt(), cellW.toInt(), cellH.toInt()))
                val maskedCell = Mat()
                Core.bitwise_and(cell, mask, maskedCell)

                val isFilled = Core.countNonZero(maskedCell) > circleArea * 0.50
                dump?.save(gray, Rect(globalRect.x + (c * cellW).toInt(), globalRect.y + (r * cellH).toInt(), cellW.toInt(), cellH.toInt()), "p1", "q${startQ + r}", options[c], isFilled)
                if (isFilled) { selectedAns = options[c]; fillCount++ }

                val cx = (globalRect.x + c * cellW + cellW / 2.0).toFloat()
                val cy = (globalRect.y + r * cellH + cellH / 2.0).toFloat()
                bubbles.add(BubbleInfo(cx, cy, radius.toFloat(), isFilled))

                cell.release(); maskedCell.release()
            }
            results.add(QuestionResult(startQ + r, if (fillCount != 1) null else selectedAns, fillCount > 1, bubbles))
        }
        mask.release()
        return results
    }

    private fun readPart2Box(
        binaryRoi: Mat, colorMat: Mat, globalRect: Rect, startQ: Int,
        gray: Mat, dump: BubbleDatasetDumper.Session?
    ): List<QuestionResult> {
        val rows = 4
        val cols = 2
        val cellW = binaryRoi.width().toDouble() / cols
        val cellH = binaryRoi.height().toDouble() / rows
        val radius = (Math.min(cellW, cellH) * 0.36).toInt()
        val circleArea = Math.PI * radius * radius

        val mask = Mat.zeros(Size(cellW, cellH), CvType.CV_8UC1)
        Imgproc.circle(mask, Point(cellW / 2.0, cellH / 2.0), radius, Scalar(255.0), -1)

        val options = listOf("Đ", "S")
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
                // Tên file chỉ dùng ký tự ASCII: Đ -> T (true), S -> F (false)
                dump?.save(gray, Rect(globalRect.x + (c * cellW).toInt(), globalRect.y + (r * cellH).toInt(), cellW.toInt(), cellH.toInt()), "p2", "q${startQ + r}", if (c == 0) "T" else "F", isFilled)
                if (isFilled) { selectedAns = options[c]; fillCount++ }

                val cx = (globalRect.x + c * cellW + cellW / 2.0).toFloat()
                val cy = (globalRect.y + r * cellH + cellH / 2.0).toFloat()
                bubbles.add(BubbleInfo(cx, cy, radius.toFloat(), isFilled))

                cell.release(); maskedCell.release()
            }
            results.add(QuestionResult(startQ + r, if (fillCount != 1) null else selectedAns, fillCount > 1, bubbles))
        }
        mask.release()
        return results
    }

    private fun readPart3Box(
        binaryRoi: Mat, colorMat: Mat, globalRect: Rect, qIndex: Int,
        gray: Mat, dump: BubbleDatasetDumper.Session?
    ): QuestionResult {
        val rows = 12
        val cols = 4
        val cellW = binaryRoi.width().toDouble() / cols
        val cellH = binaryRoi.height().toDouble() / rows
        val radius = (Math.min(cellW, cellH) * 0.36).toInt()
        val circleArea = Math.PI * radius * radius

        val mask = Mat.zeros(Size(cellW, cellH), CvType.CV_8UC1)
        Imgproc.circle(mask, Point(cellW / 2.0, cellH / 2.0), radius, Scalar(255.0), -1)

        val colResults = mutableListOf<String>()
        val allBubbles = mutableListOf<BubbleInfo>()

        for (c in 0 until cols) {
            var filledChar = ""
            var fillCount = 0

            for (r in 0 until rows) {
                val isValidBubble = when (r) {
                    0 -> c == 0
                    1 -> c == 1 || c == 2
                    else -> true
                }

                if (!isValidBubble) continue

                val cell = Mat(binaryRoi, Rect((c * cellW).toInt(), (r * cellH).toInt(), cellW.toInt(), cellH.toInt()))
                val maskedCell = Mat()
                Core.bitwise_and(cell, mask, maskedCell)

                val isFilled = Core.countNonZero(maskedCell) > circleArea * 0.50
                // Hàng 0 là dấu "-", hàng 1 là dấu thập phân ".", các hàng sau là chữ số 0-9
                val optionLabel = when (r) { 0 -> "neg"; 1 -> "dot"; else -> (r - 2).toString() }
                dump?.save(gray, Rect(globalRect.x + (c * cellW).toInt(), globalRect.y + (r * cellH).toInt(), cellW.toInt(), cellH.toInt()), "p3", "q${qIndex}c$c", optionLabel, isFilled)
                if (isFilled) {
                    filledChar = when (r) {
                        0 -> "-"
                        1 -> "."
                        else -> (r - 2).toString()
                    }
                    fillCount++
                }

                val cx = (globalRect.x + c * cellW + cellW / 2.0).toFloat()
                val cy = (globalRect.y + r * cellH + cellH / 2.0).toFloat()
                allBubbles.add(BubbleInfo(cx, cy, radius.toFloat(), isFilled))

                cell.release(); maskedCell.release()
            }

            if (fillCount == 1) colResults.add(filledChar)
            else if (fillCount == 0) colResults.add(" ")
            else colResults.add("E")
        }
        mask.release()

        if (colResults[0] == " ") return QuestionResult(qIndex, null, false, allBubbles)

        val finalAnswerBuilder = StringBuilder()
        var hasError = false
        for (char in colResults) {
            if (char == " ") break
            else if (char == "E") { hasError = true; break }
            else finalAnswerBuilder.append(char)
        }

        val finalAnswer = finalAnswerBuilder.toString()
        if (hasError || finalAnswer.isEmpty()) return QuestionResult(qIndex, null, true, allBubbles)

        return QuestionResult(qIndex, finalAnswer, false, allBubbles)
    }
}