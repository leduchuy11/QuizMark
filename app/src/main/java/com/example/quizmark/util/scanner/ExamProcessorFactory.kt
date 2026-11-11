package com.example.quizmark.util.scanner

import com.example.quizmark.data.model.ExamProcessor

object ExamProcessorFactory {
    fun supports(templateId: Int): Boolean = templateId in 1..5

    fun create(templateId: Int, classifier: BubbleClassifier = BubbleClassifierProvider.current()): ExamProcessor? =
        when (templateId) {
            1 -> ExamThptProcessor(classifier)
            2 -> Exam20Processor(classifier)
            3 -> Exam40Processor(classifier)
            4 -> Exam50Processor(classifier)
            5 -> Exam120Processor(classifier)
            else -> null
        }
}
