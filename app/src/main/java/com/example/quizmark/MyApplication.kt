package com.example.quizmark

import android.app.Application
import android.util.Log
import com.example.quizmark.util.scanner.BubbleDatasetDumper
import com.example.quizmark.util.scanner.BubbleClassifierProvider
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Khởi tạo OpenCV ngay khi app vừa bật
        val openCvReady = OpenCVLoader.initDebug()
        if (openCvReady) {
            Log.d("OpenCV_Setup", "Khởi tạo OpenCV thành công! Sẵn sàng quét phiếu.")
        } else {
            Log.e("OpenCV_Setup", "Lỗi: Không thể khởi tạo OpenCV.")
        }

        // Công cụ thu dữ liệu ô tô (chỉ hoạt động ở bản debug khi bật trong màn Tài khoản)
        BubbleDatasetDumper.init(this)
        BubbleClassifierProvider.init(this, openCvReady)
    }
}
