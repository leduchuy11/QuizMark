package com.example.quizmark.data.model

// class này để lưu tọa độ từng ô tròn
data class BubbleInfo(
    val x: Float,
    val y: Float,
    val radius: Float,
    val isFilled: Boolean
)

// Cập nhật lại QuestionResult
data class QuestionResult(
    val questionNumber: Int,
    val selectedAnswer: String?,
    val isError: Boolean,
    val bubbles: List<BubbleInfo> = emptyList() // <--- THÊM DÒNG NÀY
)

// Tổng hợp kết quả của toàn bộ bài thi
data class ScanResult(
    val sbd: String?,      // Số báo danh (null nếu lỗi)
    val examCode: String?, // Mã đề (null nếu lỗi)
    val answers: List<QuestionResult>,
    val processedBitmap: android.graphics.Bitmap? = null // Giữ lại ảnh để debug hoặc hiển thị lên UI
)

// Interface chung cho tất cả các loại phiếu
interface ExamProcessor {
    fun process(bitmap: android.graphics.Bitmap): ScanResult
}