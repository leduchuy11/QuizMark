package com.example.quizmark.util.scanner

import android.content.Context
import android.util.Log
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgproc.Imgproc

class OnnxBubbleClassifier private constructor(private val net: Net) : BubbleClassifier {
    @Volatile
    var isAvailable: Boolean = true
        private set

    // Camera and gallery may share this Net. setInput + forward must stay together.
    @Synchronized
    override fun isFilled(gray: Mat, cellRect: Rect, thresholdFilled: Boolean): Boolean {
        if (!isAvailable) return thresholdFilled
        return try {
            val cell = Mat(gray, cellRect)
            try {
                BubbleDecision.isFilled(infer(cell), thresholdFilled)
            } finally {
                cell.release()
            }
        } catch (e: Exception) {
            isAvailable = false
            Log.e(TAG, "Model failed; using the original threshold until app restart", e)
            thresholdFilled
        }
    }

    private fun infer(grayCell: Mat): FloatArray {
        val resized = Mat()
        var blob: Mat? = null
        var output: Mat? = null
        try {
            // Must match quizmark-ml/src/data.py; do not threshold or invert the input.
            Imgproc.resize(grayCell, resized, Size(32.0, 32.0), 0.0, 0.0, Imgproc.INTER_AREA)
            blob = Dnn.blobFromImage(resized, 1.0 / 255.0)
            net.setInput(blob)
            output = net.forward()
            check(output.total() == 3L && output.type() == CvType.CV_32FC1) { "Expected three float logits" }
            val logits = FloatArray(3)
            output.get(0, 0, logits)
            check(logits.all { it.isFinite() }) { "Non-finite model output" }
            return logits
        } finally {
            output?.release()
            blob?.release()
            resized.release()
        }
    }

    companion object {
        private const val TAG = "BubbleClassifier"
        const val MODEL_ASSET = "models/bubble_classifier_v1.onnx"

        /** Call only after OpenCVLoader.initDebug() succeeds. Also checks a full forward pass. */
        fun load(context: Context): OnnxBubbleClassifier {
            val bytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
            val buffer = MatOfByte(*bytes)
            val net = try {
                Dnn.readNetFromONNX(buffer)
            } finally {
                buffer.release()
            }
            check(!net.empty()) { "Empty bubble model" }
            net.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV)
            net.setPreferableTarget(Dnn.DNN_TARGET_CPU)
            val classifier = OnnxBubbleClassifier(net)
            val probe = Mat(32, 32, CvType.CV_8UC1, Scalar(255.0))
            try {
                classifier.infer(probe)
            } finally {
                probe.release()
            }
            return classifier
        }
    }
}
