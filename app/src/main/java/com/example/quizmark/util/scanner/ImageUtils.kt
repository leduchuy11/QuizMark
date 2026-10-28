package com.example.quizmark.util.scanner

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.util.Log

import android.os.Build
import java.io.File
import java.io.FileOutputStream

fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
    val filename = "QuizMark_Debug_${System.currentTimeMillis()}.png"
    
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QuizMark")
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri).use { outputStream ->
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        Log.d("ScannerCore", "✅ Đã lưu ảnh Debug vào thư viện: $filename")
                    }
                }
            } else {
                Log.e("ScannerCore", "Lỗi: Không thể tạo URI để lưu ảnh (null).")
            }
        } else {
            // Dành cho Android 9 trở xuống (cần cấp quyền WRITE_EXTERNAL_STORAGE)
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "QuizMark")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, filename)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                Log.d("ScannerCore", "✅ Đã lưu ảnh Debug vào file: ${file.absolutePath}")
            }
        }
    } catch (e: Exception) {
        Log.e("ScannerCore", "Lỗi khi lưu ảnh: ${e.message}", e)
    }
}