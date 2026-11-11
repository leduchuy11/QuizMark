package com.example.quizmark.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.util.UUID

/** Preserve faint/erased marks losslessly, without retaining another bitmap per batch result. */
object OriginalScanStore {
    private const val TAG = "OriginalScan"
    private const val DIRECTORY = "history_original_images"

    /** Call on an IO dispatcher. A storage failure must not prevent grading. */
    fun save(context: Context, bitmap: Bitmap?): String? {
        if (bitmap == null) return null
        var output: File? = null
        return try {
            val directory = File(context.filesDir, DIRECTORY)
            check(directory.isDirectory || directory.mkdirs()) { "Cannot create original scan directory" }
            val file = File(directory, "original_${UUID.randomUUID()}.png")
            output = file
            file.outputStream().buffered().use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "Cannot encode original scan" }
            }
            file.absolutePath
        } catch (e: Exception) {
            output?.delete()
            Log.w(TAG, "Could not save the original scan", e)
            null
        }
    }

    fun delete(context: Context, path: String) {
        if (path.isBlank()) return
        try {
            val file = File(path)
            val directory = File(context.filesDir, DIRECTORY).canonicalFile
            if (file.canonicalFile.parentFile == directory) file.delete()
        } catch (e: Exception) {
            Log.w(TAG, "Could not delete the original scan", e)
        }
    }
}
