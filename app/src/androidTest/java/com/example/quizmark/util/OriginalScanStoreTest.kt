package com.example.quizmark.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OriginalScanStoreTest {
    @Test fun savedOriginalIsLosslessAndUnaffectedByGrading() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.WHITE)
        source.setPixel(10, 10, Color.rgb(207, 207, 207)) // Faint erased pencil mark.
        source.setPixel(11, 10, Color.rgb(25, 25, 25))
        val path = OriginalScanStore.save(context, source)
        assertNotNull(path)
        val file = File(checkNotNull(path))
        val graded = source.copy(Bitmap.Config.ARGB_8888, true)
        try {
            Canvas(graded).drawCircle(10f, 10f, 4f, Paint().apply { color = Color.GREEN })
            val restored = BitmapFactory.decodeFile(file.absolutePath)
            try {
                val expectedPixels = IntArray(32 * 32)
                val actualPixels = IntArray(32 * 32)
                source.getPixels(expectedPixels, 0, 32, 0, 0, 32, 32)
                restored.getPixels(actualPixels, 0, 32, 0, 0, 32, 32)
                assertArrayEquals("Original pixels survive PNG storage", expectedPixels, actualPixels)
                assertFalse("Original contains no grading overlay", graded.sameAs(restored))
            } finally { restored.recycle() }
        } finally {
            graded.recycle()
            source.recycle()
            OriginalScanStore.delete(context, path)
        }
        assertFalse(file.exists())
    }

    @Test fun cleanupOnlyDeletesFilesInsideTheOriginalScanDirectory() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val unrelated = File.createTempFile("original-scan-guard", ".txt", context.cacheDir)
        try {
            unrelated.writeText("Keep this file")
            OriginalScanStore.delete(context, unrelated.absolutePath)
            assertTrue(unrelated.exists())
        } finally { unrelated.delete() }
    }
}
