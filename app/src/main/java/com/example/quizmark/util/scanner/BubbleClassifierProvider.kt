package com.example.quizmark.util.scanner

import android.content.Context
import android.util.Log
import com.example.quizmark.BuildConfig

/** The synthetic v1 model is for debug testing; release builds retain the original scanner. */
object BubbleClassifierProvider {
    private const val TAG = "BubbleClassifier"
    private const val PREFS = "QuizMarkPrefs"
    private const val KEY_ENABLED = "USE_BUBBLE_MODEL"

    private var appContext: Context? = null
    private var model: OnnxBubbleClassifier? = null

    fun init(context: Context, openCvReady: Boolean) {
        appContext = context.applicationContext
        if (!BuildConfig.DEBUG || !openCvReady) return
        try {
            model = OnnxBubbleClassifier.load(context)
            Log.i(TAG, "Practice model v1 ready (offline, OpenCV DNN)")
        } catch (e: Exception) {
            Log.e(TAG, "Could not load practice model; using the original threshold", e)
        }
    }

    val isModelReady: Boolean get() = BuildConfig.DEBUG && model?.isAvailable == true

    fun isEnabled(): Boolean = BuildConfig.DEBUG &&
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.getBoolean(KEY_ENABLED, true) == true

    fun setEnabled(enabled: Boolean) {
        if (BuildConfig.DEBUG) {
            appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
                ?.putBoolean(KEY_ENABLED, enabled)?.apply()
        }
    }

    fun current(): BubbleClassifier {
        val active = model?.takeIf { isEnabled() && it.isAvailable } ?: ThresholdBubbleClassifier
        Log.d(TAG, if (active is OnnxBubbleClassifier) "Scanning with practice AI v1" else "Scanning with original 50% rule")
        return active
    }
}
