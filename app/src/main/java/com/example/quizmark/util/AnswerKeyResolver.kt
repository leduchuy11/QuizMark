package com.example.quizmark.util

import android.content.Context
import com.example.quizmark.R

class AnswerKeyMismatchException(
    val scannedCode: String?,
    val availableCodes: List<String>
) : IllegalArgumentException()

/** Never guess another key, even when the exam has only one code. Leading zeroes matter. */
fun <T> requireAnswerKey(scannedCode: String?, keys: List<T>, codeOf: (T) -> String): T =
    keys.firstOrNull { !scannedCode.isNullOrBlank() && codeOf(it) == scannedCode }
        ?: throw AnswerKeyMismatchException(scannedCode, keys.map(codeOf))

fun gradingErrorMessage(context: Context, error: Exception): String = when {
    error is AnswerKeyMismatchException && error.availableCodes.isEmpty() ->
        context.getString(R.string.scan_no_answer_keys)
    error is AnswerKeyMismatchException && error.scannedCode.isNullOrBlank() ->
        context.getString(R.string.scan_code_unreadable, error.availableCodes.joinToString(", "))
    error is AnswerKeyMismatchException ->
        context.getString(R.string.scan_code_mismatch, error.scannedCode, error.availableCodes.joinToString(", "))
    else -> error.message ?: context.getString(R.string.grading_error)
}
