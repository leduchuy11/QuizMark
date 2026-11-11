package com.example.quizmark.util.scanner

import kotlin.math.exp

/** Output order is fixed by quizmark-ml: empty, filled, erased. */
internal object BubbleDecision {
    fun isFilled(logits: FloatArray, thresholdFilled: Boolean, minConfidence: Double = 0.6): Boolean {
        if (logits.size != 3 || logits.any { !it.isFinite() }) return thresholdFilled
        val best = logits.indices.maxByOrNull { logits[it] } ?: return thresholdFilled
        val max = logits[best].toDouble()
        val confidence = 1.0 / logits.sumOf { exp(it.toDouble() - max) }
        return if (confidence < minConfidence) thresholdFilled else best == 1
    }
}
