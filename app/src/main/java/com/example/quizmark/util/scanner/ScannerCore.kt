package com.example.quizmark.util.scanner

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot

class ScannerCore {

    // --- CÁC BIẾN THEO DÕI ĐỘ ỔN ĐỊNH CỦA KHUNG HÌNH ---
    private var stableCount = 0            // Đếm số khung hình liên tiếp mà các góc được xác định là ổn định
    private var lastCorners: List<Point>? = null  // Lưu 4 góc của lần kiểm tra trước
    private val STABILITY_TOLERANCE = 100.0 // Cho phép các góc dao động tối đa 100 pixel (do rung tay)
    private val REQUIRED_STABLE_FRAMES = 3 // Yêu cầu giữ im trong 3 khung hình (~ 0.3 giây)

    fun processFrame(bitmap: Bitmap): Bitmap? {
        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)

        val grayMat = Mat()
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_BGR2GRAY)

        val blurredMat = Mat()
        Imgproc.GaussianBlur(grayMat, blurredMat, Size(5.0, 5.0), 0.0)

        val edgesMat = Mat()
        Imgproc.Canny(blurredMat, edgesMat, 75.0, 200.0)

        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(edgesMat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        // Tìm 4 góc
        val currentCorners = findExamCorners(contours)

        // Dọn dẹp bộ nhớ ngay nếu không tìm thấy góc để tránh rò rỉ RAM
        if (currentCorners == null || currentCorners.size != 4) {
            stableCount = 0
            lastCorners = null
            releaseMats(srcMat, grayMat, blurredMat, edgesMat)
            return null
        }

        // Kiểm tra độ ổn định của 4 góc so với khung hình trước đó
        if (isStable(currentCorners)) {
            stableCount++
        } else {
            stableCount = 1
            lastCorners = currentCorners
            releaseMats(srcMat, grayMat, blurredMat, edgesMat)
            return null // Bắt đầu đếm lại, chưa vội cắt ảnh
        }

        // Nếu người dùng giữ tay đủ vững trong 4 khung hình liên tiếp
        if (stableCount >= REQUIRED_STABLE_FRAMES) {
            stableCount = 0

            // Tiến hành nắn ảnh theo 4 góc, KHÔNG tự động xoay nữa
            val warpedMat = warpImage(grayMat, currentCorners)

            val resultBitmap = Bitmap.createBitmap(warpedMat.cols(), warpedMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(warpedMat, resultBitmap)

            warpedMat.release()
            releaseMats(srcMat, grayMat, blurredMat, edgesMat)

            return resultBitmap
        }

        releaseMats(srcMat, grayMat, blurredMat, edgesMat)
        return null
    }

    /**
     * So sánh 4 góc hiện tại với 4 góc của khung hình trước.
     * Nếu độ lệch của TẤT CẢ các góc đều nhỏ hơn STABILITY_TOLERANCE thì coi là ổn định.
     */
    private fun isStable(currentCorners: List<Point>): Boolean {
        if (lastCorners == null) return false

        for (i in 0 until 4) {
            val dist = hypot(currentCorners[i].x - lastCorners!![i].x, currentCorners[i].y - lastCorners!![i].y)
            if (dist > STABILITY_TOLERANCE) {
                return false
            }
        }
        return true
    }

    private fun releaseMats(vararg mats: Mat) {
        for (mat in mats) {
            mat.release()
        }
    }

    /**
     * Tìm 4 điểm tâm ở 4 góc ngoài cùng.
     * CHỈ KÍCH HOẠT KHI NHÌN THẤY ĐỦ CÁC MARKER VÀ HÌNH THÀNH ĐƯỢC CHỮ NHẬT.
     */
    private fun findExamCorners(contours: List<MatOfPoint>): List<Point>? {
        val squares = mutableListOf<MatOfPoint2f>()

        for (contour in contours) {
            val contour2f = MatOfPoint2f(*contour.toArray())
            val peri = Imgproc.arcLength(contour2f, true)
            val approx = MatOfPoint2f()

            Imgproc.approxPolyDP(contour2f, approx, 0.03 * peri, true)

            if (approx.toArray().size in 4..6) {
                val area = Imgproc.contourArea(approx)
                // ÉP KÍCH THƯỚC: Chỉ lấy các ô vuông có diện tích từ 100 đến 30.000 pixel
                // Việc này sẽ tự động bỏ qua tờ giấy (diện tích > 200.000) và các hạt bụi liti
                if (area in 100.0..30000.0) {
                    val rect = Imgproc.boundingRect(contour)
                    val aspectRatio = rect.width.toFloat() / rect.height.toFloat()
                    if (aspectRatio in 0.5..2.0) {
                        squares.add(approx)
                    }
                }
            }
        }

        if (squares.size >= 4) {
            val sortedAreas = squares.map { Imgproc.contourArea(it) }.sorted()
            val medianArea = sortedAreas[sortedAreas.size / 2]

            // Cho phép các ô marker bự gấp 15 lần ô đáp án
            val validSquares = squares.filter {
                val area = Imgproc.contourArea(it)
                area >= medianArea * 0.1 && area <= medianArea * 15.0
            }

            if (validSquares.size >= 4) {
                val centers = validSquares.map {
                    val rect = Imgproc.boundingRect(MatOfPoint(*it.toArray()))
                    Point(rect.x + rect.width / 2.0, rect.y + rect.height / 2.0)
                }

                // Nhặt 4 góc ngoài cùng nhất (4 điểm cực trị)
                val topLeft = centers.minByOrNull { it.x + it.y } ?: return null
                val bottomRight = centers.maxByOrNull { it.x + it.y } ?: return null
                val topRight = centers.maxByOrNull { it.x - it.y } ?: return null
                val bottomLeft = centers.minByOrNull { it.x - it.y } ?: return null

                val widthTop = Math.hypot(topRight.x - topLeft.x, topRight.y - topLeft.y)
                val widthBottom = Math.hypot(bottomRight.x - bottomLeft.x, bottomRight.y - bottomLeft.y)
                val heightLeft = Math.hypot(bottomLeft.x - topLeft.x, bottomLeft.y - topLeft.y)
                val heightRight = Math.hypot(bottomRight.x - topRight.x, bottomRight.y - topRight.y)

                // Hạ mức quét tối thiểu xuống 80px để bắt từ xa
                if (widthTop < 80 || heightLeft < 100) return null
                if (heightLeft < widthTop * 1.05) return null

                if (kotlin.math.abs(widthTop - widthBottom) > widthTop * 0.25) return null
                if (kotlin.math.abs(heightLeft - heightRight) > heightLeft * 0.25) return null

                return sortCorners(listOf(topLeft, topRight, bottomRight, bottomLeft))
            }
        }

        return null
    }

    /**
     * Sắp xếp 4 điểm theo thứ tự kim đồng hồ (Bắt đầu từ Top-Left).
     */
    private fun sortCorners(points: List<Point>): List<Point> {
        // Chia thành 2 điểm trên và 2 điểm dưới dựa theo trục Y
        val sortedByY = points.sortedBy { it.y }
        val topPoints = sortedByY.take(2).sortedBy { it.x }    // [TopLeft, TopRight]
        val bottomPoints = sortedByY.drop(2).sortedBy { it.x } // [BottomLeft, BottomRight]

        // Trả về thứ tự: TL, TR, BR, BL
        return listOf(topPoints[0], topPoints[1], bottomPoints[1], bottomPoints[0])
    }

    /**
     * Kéo phẳng ảnh thành kích thước chuẩn 800x1200 pixel.
     */
    private fun warpImage(src: Mat, corners: List<Point>): Mat {
        // Kích thước chuẩn của phiếu thi (Tỷ lệ 2:3)
        val dstWidth = 800.0
        val dstHeight = 1200.0

        val srcMat = MatOfPoint2f(*corners.toTypedArray())
        val dstMat = MatOfPoint2f(
            Point(0.0, 0.0),             // Top-Left
            Point(dstWidth, 0.0),        // Top-Right
            Point(dstWidth, dstHeight),  // Bottom-Right
            Point(0.0, dstHeight)        // Bottom-Left
        )

        val transformMatrix = Imgproc.getPerspectiveTransform(srcMat, dstMat)
        val warpedMat = Mat()
        Imgproc.warpPerspective(src, warpedMat, transformMatrix, Size(dstWidth, dstHeight))

        return warpedMat
    }

    /**
     * Dùng riêng cho ảnh tĩnh (chọn từ thư viện).
     * Bỏ qua chống rung, tìm 4 góc và nắn phẳng ngay lập tức.
     */
    fun processStaticImage(bitmap: Bitmap): Bitmap? {
        val srcMat = Mat()
        Utils.bitmapToMat(bitmap, srcMat)

        val grayMat = Mat()
        Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_BGR2GRAY)

        val blurredMat = Mat()
        Imgproc.GaussianBlur(grayMat, blurredMat, Size(5.0, 5.0), 0.0)

        val edgesMat = Mat()
        Imgproc.Canny(blurredMat, edgesMat, 75.0, 200.0)

        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(edgesMat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

        // Tìm 4 góc bằng hàm cũ
        val corners = findExamCorners(contours)

        if (corners == null || corners.size != 4) {
            releaseMats(srcMat, grayMat, blurredMat, edgesMat, hierarchy)
            return null // Không tìm thấy 4 góc trên ảnh tĩnh
        }

        // Nắn phẳng ngay lập tức
        val warpedMat = warpImage(grayMat, corners)

        val resultBitmap = Bitmap.createBitmap(warpedMat.cols(), warpedMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(warpedMat, resultBitmap)

        warpedMat.release()
        releaseMats(srcMat, grayMat, blurredMat, edgesMat, hierarchy)

        return resultBitmap
    }
}