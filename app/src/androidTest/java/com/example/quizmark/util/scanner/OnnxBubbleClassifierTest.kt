package com.example.quizmark.util.scanner

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

@RunWith(AndroidJUnit4::class)
class OnnxBubbleClassifierTest {
    @Test fun everyProcessorUsesTheInjectedClassifierForAllItsCells() {
        assertTrue(OpenCVLoader.initDebug())
        val collectionEnabled = BubbleDatasetDumper.isEnabled()
        BubbleDatasetDumper.setEnabled(false)
        try {
            for ((template, expectedCells) in mapOf(1 to 602, 2 to 170, 3 to 250, 4 to 290, 5 to 570)) {
                var calls = 0
                val allFilled = object : BubbleClassifier {
                    override fun isFilled(gray: Mat, cellRect: Rect, thresholdFilled: Boolean): Boolean {
                        calls++
                        assertEquals(1, gray.channels())
                        assertFalse(thresholdFilled) // Blank white page: old rule finds no fills.
                        assertEquals(255.0, gray.get(cellRect.y, cellRect.x)[0], 0.0)
                        return true // Force every answer to be multiply marked.
                    }
                }
                val bitmap = Bitmap.createBitmap(800, 1200, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                val beforeGrading = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                try {
                    val result = checkNotNull(ExamProcessorFactory.create(template, allFilled)).process(bitmap)
                    assertTrue("Template $template preserves the original pixels", beforeGrading.sameAs(checkNotNull(result.originalBitmap)))
                    assertTrue("The input scan stays unchanged", beforeGrading.sameAs(bitmap))
                    assertFalse("Grading overlays are separate", beforeGrading.sameAs(result.processedBitmap))
                    assertEquals("Template $template", expectedCells, calls)
                    assertTrue(result.answers.all { it.isError && it.bubbles.all { bubble -> bubble.isFilled } })
                    result.processedBitmap?.recycle()
                } finally {
                    beforeGrading.recycle()
                    bitmap.recycle()
                }
            }
        } finally { BubbleDatasetDumper.setEnabled(collectionEnabled) }
    }

    @Test fun nativeInferenceReadsCropsFromEveryTemplate() {
        assertTrue(OpenCVLoader.initDebug())
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val classifier = OnnxBubbleClassifier.load(instrumentation.targetContext)
        for (template in 1..5) for (label in listOf("empty", "filled", "erased")) {
            val name = "bubble_model/t${template}_${label}.png"
            val bitmap = instrumentation.context.assets.open(name).use { BitmapFactory.decodeStream(it) }
            val rgba = Mat()
            val gray = Mat()
            val sheet = Mat(100, 100, CvType.CV_8UC1, Scalar(0.0))
            try {
                Utils.bitmapToMat(bitmap, rgba)
                Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_BGRA2GRAY)
                // Nonzero offset catches accidentally classifying the full page or binary ROI.
                val rect = Rect(13, 9, gray.cols(), gray.rows())
                val target = Mat(sheet, rect)
                try { gray.copyTo(target) } finally { target.release() }
                for (fallback in listOf(false, true)) {
                    assertEquals(name, label == "filled", classifier.isFilled(sheet, rect, fallback))
                }
                assertTrue(classifier.isAvailable)
            } finally {
                sheet.release()
                gray.release()
                rgba.release()
                bitmap.recycle()
            }
        }
    }

    @Test fun nativeFailureFallsBackForBothDecisions() {
        assertTrue(OpenCVLoader.initDebug())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val classifier = OnnxBubbleClassifier.load(context)
        val gray = Mat(32, 32, CvType.CV_8UC1, Scalar(255.0))
        try {
            assertTrue(classifier.isFilled(gray, Rect(40, 40, 10, 10), true))
            assertFalse(classifier.isAvailable)
            assertFalse(classifier.isFilled(gray, Rect(0, 0, 32, 32), false))
        } finally { gray.release() }
    }
}
