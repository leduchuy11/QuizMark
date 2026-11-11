package com.example.quizmark.util

import com.example.quizmark.data.model.ExamCodeModel
import com.example.quizmark.data.model.ThptExamCodeModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamCodeUpdatesTest {
    @Test fun renaming001To101KeepsAllTwentyAnswers() {
        val answers = (1..20).associate { it.toString() to listOf("A", "B", "C", "D")[(it - 1) % 4] }
        val original = ExamCodeModel("001", answers)
        val updated = ExamCodeUpdates.basic(listOf(original), listOf("101"))
        assertEquals(listOf(ExamCodeModel("101", answers)), updated)
        assertEquals("001", original.code)
        assertEquals(answers, requireAnswerKey("101", updated) { it.code }.answers)
    }

    @Test fun swappingCodesKeepsAnswersOnTheirOriginalRows() {
        val first = ExamCodeModel("001", mapOf("1" to "A"))
        val second = ExamCodeModel("002", mapOf("1" to "D"))
        val updated = ExamCodeUpdates.basic(listOf(first, second), listOf("002", "001"))
        assertEquals("A", updated[0].answers["1"])
        assertEquals("D", updated[1].answers["1"])
        assertEquals(listOf("002", "001"), updated.map { it.code })
    }

    @Test fun appendedCodesStartEmptyAndRemovedRowsDoNotReturn() {
        val original = listOf(ExamCodeModel("001", mapOf("1" to "B")))
        val expanded = ExamCodeUpdates.basic(original, listOf("101", "102"))
        assertEquals(original[0].answers, expanded[0].answers)
        assertTrue(expanded[1].answers.isEmpty())
        assertEquals(listOf(expanded[0]), ExamCodeUpdates.basic(expanded, listOf("101")))
    }

    @Test fun thptRenameKeepsAllThreeParts() {
        val original = ThptExamCodeModel(
            "0001", mapOf("1" to "A"),
            mapOf("1" to mapOf("a" to "Đ", "b" to "S", "c" to "Đ", "d" to "S")),
            mapOf("1" to "-3,8")
        )
        assertEquals(listOf(original.copy(code = "0101")), ExamCodeUpdates.thpt(listOf(original), listOf("0101")))
    }
}
