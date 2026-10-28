package com.example.quizmark.util.scanner

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import com.example.quizmark.data.model.ScanResult
import com.example.quizmark.data.model.ExamProcessor
import com.example.quizmark.data.model.QuestionResult
import org.opencv.imgproc.Imgproc

class Exam20Processor : ExamProcessor {

    // Tọa độ cắt vùng (ROI) trên ảnh 800x1200
    private val roiSBD = Rect(186, 337, 201, 381)
    private val roiCode = Rect(417, 338, 104, 382)
    private val roiCol1 = Rect(221, 759, 131, 380)
    private val roiCol2 = Rect(418, 760, 131, 380)

    override fun process(bitmap: Bitmap): ScanResult {
        val colorMat = Mat()
        var grayMat: Mat? = null
        var binaryMat: Mat? = null
        var sbdMat: Mat? = null
        var codeMat: Mat? = null
        var col1Mat: Mat? = null
        var col2Mat: Mat? = null

        try {
            // Dùng colorMat làm bản vẽ để lát nữa vẽ vòng tròn màu Xanh/Vàng lên
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
            // Truyền thêm colorMat và biến roiRect để hàm biết vị trí mà vẽ vòng tròn
            sbdMat = Mat(binaryMat, roiSBD)
            val sbdResult = readGridNumber(sbdMat, colorMat, roiSBD, 10, 6)

            codeMat = Mat(binaryMat, roiCode)
            val codeResult = readGridNumber(codeMat, colorMat, roiCode, 10, 3)

            val answers = mutableListOf<QuestionResult>()

            col1Mat = Mat(binaryMat, roiCol1)
            answers.addAll(readAnswers(col1Mat, colorMat, roiCol1, 1, 10))

            col2Mat = Mat(binaryMat, roiCol2)
            answers.addAll(readAnswers(col2Mat, colorMat, roiCol2, 11, 20))

            // Lưu lại ảnh màu đã được vẽ các vòng tròn debug
            val debugBitmap = Bitmap.createBitmap(colorMat.cols(), colorMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(colorMat, debugBitmap)

            return ScanResult(
                sbd = sbdResult,
                examCode = codeResult,
                answers = answers,
                processedBitmap = debugBitmap // Trả về ảnh có khoanh tròn
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
        val cellW = binaryRoi.width() / cols
        val cellH = binaryRoi.height() / rows
        // Bán kính hình tròn bằng 40% cạnh nhỏ nhất của ô (để lọt thỏm bên trong ô)
        val radius = (Math.min(cellW, cellH) * 0.4).toInt()
        val circleArea = Math.PI * radius * radius

        // TẠO MẶT NẠ (MASK): Một hình vuông đen chứa 1 vòng tròn trắng ở giữa
        val mask = Mat.zeros(Size(cellW.toDouble(), cellH.toDouble()), CvType.CV_8UC1)
        Imgproc.circle(mask, Point(cellW / 2.0, cellH / 2.0), radius, Scalar(255.0), -1)

        val result = StringBuilder()

        for (c in 0 until cols) {
            var filledRow = -1
            var fillCount = 0

            for (r in 0 until rows) {
                val cell = Mat(binaryRoi, Rect(c * cellW, r * cellH, cellW, cellH))

                // Lọc điểm ảnh: Chỉ giữ lại các pixel Trắng nằm TRONG vòng tròn mask
                val maskedCell = Mat()
                Core.bitwise_and(cell, mask, maskedCell)

                val whitePixels = Core.countNonZero(maskedCell)
                // Ngưỡng: Cần tô chì ít nhất 50% diện tích hình tròn
                val isFilled = whitePixels > circleArea * 0.50

                // Tọa độ tâm của ô tròn trên bức ảnh gốc 800x1200
                val globalCenter = Point(globalRect.x + c * cellW + cellW / 2.0, globalRect.y + r * cellH + cellH / 2.0)

                if (isFilled) {
                    filledRow = r
                    fillCount++
                    // VẼ MÀU XANH LÁ (OpenCV dùng chuẩn BGR -> R=0, G=255, B=0)
                    Imgproc.circle(colorMat, globalCenter, radius, Scalar(0.0, 255.0, 0.0), 3)
                } else {
                    // VẼ MÀU VÀNG (BGR -> R=255, G=255, B=0)
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
        val circleArea = Math.PI * radius * radius

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