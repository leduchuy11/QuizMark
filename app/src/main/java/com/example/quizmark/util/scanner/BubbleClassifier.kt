package com.example.quizmark.util.scanner

import org.opencv.core.Mat
import org.opencv.core.Rect

interface BubbleClassifier {
    /** Read the clean grayscale cell. The original masked threshold is the fallback. */
    fun isFilled(gray: Mat, cellRect: Rect, thresholdFilled: Boolean): Boolean
}

object ThresholdBubbleClassifier : BubbleClassifier {
    override fun isFilled(gray: Mat, cellRect: Rect, thresholdFilled: Boolean) = thresholdFilled
}
