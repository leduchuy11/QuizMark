package com.example.quizmark.util.scanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleDecisionTest {
    @Test fun confidentClassesOverrideTheThreshold() {
        assertTrue(BubbleDecision.isFilled(floatArrayOf(-4f, 8f, -2f), false))
        assertFalse(BubbleDecision.isFilled(floatArrayOf(8f, -4f, -2f), true))
        assertFalse(BubbleDecision.isFilled(floatArrayOf(-4f, -2f, 8f), true))
    }

    @Test fun uncertainPredictionsKeepTheOriginalDecision() {
        for (fallback in listOf(false, true)) {
            assertTrue(BubbleDecision.isFilled(floatArrayOf(1f, 1f, 1f), fallback) == fallback)
            // Winner has about 58% probability, below the 60% cutoff.
            assertTrue(BubbleDecision.isFilled(floatArrayOf(0f, 1f, 0f), fallback) == fallback)
        }
    }

    @Test fun probabilityAboveTheCutoffUsesTheModel() {
        assertTrue(BubbleDecision.isFilled(floatArrayOf(0f, 1.2f, 0f), false))
    }

    @Test fun softmaxRemainsStableForLargeLogits() {
        assertTrue(BubbleDecision.isFilled(floatArrayOf(9990f, 10000f, 9990f), false))
        assertFalse(BubbleDecision.isFilled(floatArrayOf(-10000f, -10010f, -10010f), true))
    }

    @Test fun invalidOutputsKeepTheOriginalDecision() {
        for (bad in listOf(floatArrayOf(), floatArrayOf(1f, 2f),
            floatArrayOf(Float.NaN, 1f, 0f), floatArrayOf(0f, Float.POSITIVE_INFINITY, 0f))) {
            assertTrue(BubbleDecision.isFilled(bad, true))
            assertFalse(BubbleDecision.isFilled(bad, false))
        }
    }
}
