package com.example.quizmark.data.model

// 1. Mô hình tổng thể cho một Đề thi THPT
data class ThptExamModel(
    val id: String = "",
    val name: String = "",
    val templateId: Int = 1,
    val config: ThptExamConfig = ThptExamConfig(),
    val codes: List<ThptExamCodeModel> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

// 2. Cấu hình số câu và điểm số các phần (Lưu từ màn SetupThptExamScreen)
data class ThptExamConfig(
    val p1Questions: Int = 0,
    val p1Score: Float = 0f,
    val p2Questions: Int = 0,
    val p3Questions: Int = 0,
    val p3Score: Float = 0f
) : java.io.Serializable

// 3. Chi tiết đáp án cho từng Mã đề THPT
data class ThptExamCodeModel(
    val code: String = "",

    val part1: Map<String, String> = emptyMap(),                  // Câu (VD: "1") -> Đáp án ("A","B","C","D")
    val part2: Map<String, Map<String, String>> = emptyMap(),    // Câu (VD: "1") -> Ý (VD: "a" -> "Đ")
    val part3: Map<String, String> = emptyMap()                  // Câu (VD: "1") -> Giá trị (VD: "-3,8")
)