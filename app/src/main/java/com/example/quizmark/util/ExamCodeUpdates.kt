package com.example.quizmark.util

import com.example.quizmark.data.model.ExamCodeModel
import com.example.quizmark.data.model.ThptExamCodeModel

/** Each editable row retains its answers when its code changes, including code swaps. */
object ExamCodeUpdates {
    fun basic(original: List<ExamCodeModel>, edited: List<String>): List<ExamCodeModel> =
        edited.mapIndexed { index, code ->
            original.getOrNull(index)?.copy(code = code) ?: ExamCodeModel(code = code)
        }

    fun thpt(original: List<ThptExamCodeModel>, edited: List<String>): List<ThptExamCodeModel> =
        edited.mapIndexed { index, code ->
            original.getOrNull(index)?.copy(code = code) ?: ThptExamCodeModel(code = code)
        }
}

class IncompleteAnswerKeyException : IllegalArgumentException()
