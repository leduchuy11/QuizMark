package com.example.quizmark.util

import java.text.Normalizer

// Hàm mở rộng giúp xóa dấu tiếng Việt
fun String.unAccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    val regex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    return regex.replace(temp, "")
        .replace("đ", "d")
        .replace("Đ", "D")
}