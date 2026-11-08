package com.example.quizmark.util.scanner

import android.content.Context
import android.util.Log
import com.example.quizmark.BuildConfig
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.imgcodecs.Imgcodecs
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * CÔNG CỤ CHỈ DÙNG Ở BẢN DEBUG: lưu ảnh xám của từng ô tròn mà processor đọc được,
 * làm dữ liệu train model phân loại ô (repo quizmark-ml, train trên Google Colab).
 *
 * Cấu trúc thư mục:
 *   filesDir/bubble_dataset/<empty|filled>/<sheetId>_t<template>_<vùng>_<vị trí>_<lựa chọn>.png
 *
 * - Thư mục <empty|filled> là NHÃN TẠM theo ngưỡng 50% hiện tại, sẽ được sửa lại trên Colab
 *   (thêm lớp "erased" cho ô đã tẩy/gạch).
 * - <sheetId> khác nhau cho mỗi lần quét, để lúc train chia tập theo phiếu (tránh rò rỉ dữ liệu).
 * - Ảnh được cắt từ grayMat (tạo trước khi vẽ vòng màu) nên sạch, giữ nguyên kích thước ô.
 */
object BubbleDatasetDumper {

    private const val TAG = "BubbleDataset"
    private const val PREFS = "QuizMarkPrefs"
    private const val KEY_ENABLED = "DUMP_BUBBLES"
    private const val DIR_NAME = "bubble_dataset"

    @Volatile
    private var appContext: Context? = null

    /** Gọi một lần trong MyApplication.onCreate(). */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** Chỉ bản debug mới có công cụ này. */
    val isAvailable: Boolean get() = BuildConfig.DEBUG

    fun isEnabled(): Boolean {
        val ctx = appContext ?: return false
        return isAvailable && ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(enabled: Boolean) {
        val ctx = appContext ?: return
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /**
     * Gọi ở đầu mỗi lần processor đọc một phiếu.
     * Trả về null khi không thu dữ liệu (bản release hoặc công tắc đang tắt) -> processor bỏ qua hoàn toàn.
     */
    fun beginSheet(templateId: Int): Session? {
        val ctx = appContext ?: return null
        if (!isEnabled()) return null
        val stamp = SimpleDateFormat("yyMMdd-HHmmss", Locale.US).format(Date())
        val suffix = (1..4).map { "abcdefghijkmnpqrstuvwxyz23456789".random() }.joinToString("")
        return Session(File(ctx.filesDir, DIR_NAME), "$stamp-$suffix", templateId)
    }

    class Session internal constructor(
        root: File,
        val sheetId: String,
        private val templateId: Int
    ) {
        private val filledDir = File(root, "filled").apply { mkdirs() }
        private val emptyDir = File(root, "empty").apply { mkdirs() }

        /**
         * @param gray     ảnh xám của cả phiếu (đã nắn phẳng)
         * @param cellRect toạ độ ô trên ảnh xám (toạ độ toàn cục)
         * @param region   vùng: sbd, code, ans, p1, p2, p3
         * @param position vị trí trong vùng: q12 (câu 12), c3 (cột 3)...
         * @param option   lựa chọn: A-D, chữ số, T/F...
         */
        fun save(gray: Mat, cellRect: Rect, region: String, position: String, option: String, isFilled: Boolean) {
            try {
                val dir = if (isFilled) filledDir else emptyDir
                val file = File(dir, "${sheetId}_t${templateId}_${region}_${position}_${option}.png")
                val cell = Mat(gray, cellRect)
                try {
                    Imgcodecs.imwrite(file.absolutePath, cell)
                } finally {
                    cell.release()
                }
            } catch (e: Exception) {
                // Thu dữ liệu không bao giờ được làm hỏng việc chấm bài
                Log.w(TAG, "Không lưu được ô $region/$position/$option", e)
            }
        }
    }

    private fun datasetDir(): File? = appContext?.let { File(it.filesDir, DIR_NAME) }

    private fun sampleFiles(): List<File> =
        datasetDir()?.walkTopDown()?.filter { it.isFile && it.extension == "png" }?.toList() ?: emptyList()

    /** Số ô đã thu (gọi trên luồng nền). */
    fun countSamples(): Int = sampleFiles().size

    /**
     * Nén toàn bộ dữ liệu thành cacheDir/exports/bubble_dataset_<thời gian>.zip (gọi trên luồng nền).
     * Trong zip: bubble_dataset/<empty|filled>/<tên file>.png
     * Trả về null nếu chưa có dữ liệu.
     */
    fun exportZip(): File? {
        val ctx = appContext ?: return null
        val root = datasetDir() ?: return null
        val files = sampleFiles()
        if (files.isEmpty()) return null

        val outDir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        // Xoá các file zip cũ để không chiếm bộ nhớ
        outDir.listFiles { f -> f.name.startsWith("bubble_dataset_") && f.name.endsWith(".zip") }
            ?.forEach { it.delete() }

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val zipFile = File(outDir, "bubble_dataset_$stamp.zip")
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            zos.setLevel(Deflater.BEST_SPEED) // PNG vốn đã nén sẵn
            for (f in files) {
                zos.putNextEntry(ZipEntry("$DIR_NAME/${f.relativeTo(root).invariantSeparatorsPath}"))
                f.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        return zipFile
    }

    /** Xoá toàn bộ dữ liệu đã thu (gọi trên luồng nền). */
    fun clear() {
        datasetDir()?.deleteRecursively()
    }
}
