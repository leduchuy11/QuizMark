package com.example.quizmark

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Khởi tạo OpenCV ngay khi app vừa bật
        if (OpenCVLoader.initDebug()) {
            Log.d("OpenCV_Setup", "Khởi tạo OpenCV thành công! Sẵn sàng quét phiếu.")
        } else {
            Log.e("OpenCV_Setup", "Lỗi: Không thể khởi tạo OpenCV.")
        }
    }
}