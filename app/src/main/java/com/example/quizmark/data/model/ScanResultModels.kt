package com.example.quizmark.data.model

// Kết quả của 1 câu hỏi
data class QuestionResult(
    val questionNumber: Int,
    val selectedAnswer: String?, // "A", "B", "C", "D" hoặc null nếu không tô
    val isError: Boolean // true nếu tô đúp hoặc bỏ trống
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