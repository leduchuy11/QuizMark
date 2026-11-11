package com.example.quizmark.util.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamProcessorFactoryTest {
    @Test fun knownTemplatesUseTheirOwnProcessor() {
        val expected = listOf(ExamThptProcessor::class, Exam20Processor::class, Exam40Processor::class,
            Exam50Processor::class, Exam120Processor::class)
        expected.forEachIndexed { index, type ->
            assertTrue(ExamProcessorFactory.supports(index + 1))
            assertEquals(type, ExamProcessorFactory.create(index + 1, ThresholdBubbleClassifier)!!::class)
        }
    }

    @Test fun unsupportedTemplatesCannotFallBackToTwentyQuestions() {
        for (id in listOf(-1, 0, 6, 99)) {
            assertFalse(ExamProcessorFactory.supports(id))
            assertNull(ExamProcessorFactory.create(id, ThresholdBubbleClassifier))
        }
    }
}
