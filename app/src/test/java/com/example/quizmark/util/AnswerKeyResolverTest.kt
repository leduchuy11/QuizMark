package com.example.quizmark.util

import com.example.quizmark.data.model.ExamCodeModel
import com.example.quizmark.data.model.ThptExamCodeModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class AnswerKeyResolverTest {
    @Test fun mismatchedSingleKeyMustNotProduceAZeroScoreOrUseAnotherCode() {
        val error = assertThrows(AnswerKeyMismatchException::class.java) {
            requireAnswerKey("101", listOf(ExamCodeModel("001"))) { it.code }
        }
        assertEquals("101", error.scannedCode)
        assertEquals(listOf("001"), error.availableCodes)
    }

    @Test fun unreadableCodeMustNotChooseTheOnlyKey() {
        for (code in listOf(null, "", " ")) {
            assertThrows(AnswerKeyMismatchException::class.java) {
                requireAnswerKey(code, listOf(ExamCodeModel("001"))) { it.code }
            }
        }
    }

    @Test fun missingKeysMustFailBeforeGrading() {
        val error = assertThrows(AnswerKeyMismatchException::class.java) {
            requireAnswerKey("101", emptyList<ExamCodeModel>()) { it.code }
        }
        assertEquals(emptyList<String>(), error.availableCodes)
    }

    @Test fun leadingZeroesIdentifyDifferentKeys() {
        val key = ExamCodeModel("001", mapOf("1" to "B"))
        assertSame(key, requireAnswerKey("001", listOf(key)) { it.code })
        assertThrows(AnswerKeyMismatchException::class.java) {
            requireAnswerKey("1", listOf(key)) { it.code }
        }
    }

    @Test fun resolvesCorrectKeyAmongMultipleBasicAndThptCodes() {
        val basic = listOf(ExamCodeModel("001"), ExamCodeModel("101"))
        val thpt = listOf(ThptExamCodeModel("0001"), ThptExamCodeModel("0101"))
        assertSame(basic[1], requireAnswerKey("101", basic) { it.code })
        assertSame(thpt[1], requireAnswerKey("0101", thpt) { it.code })
    }
}
