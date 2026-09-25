package com.example.quizmark.data.repository

import com.example.quizmark.data.model.ExamModel
import com.example.quizmark.data.model.ThptExamModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ExamRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun saveBasicExam(exam: ExamModel): Result<Unit> {
        return try {
            val currentUserUid = auth.currentUser?.uid ?: throw Exception("Người dùng chưa đăng nhập!")

            val finalExam = exam.copy(userId = currentUserUid)

            db.collection("users")
                .document(currentUserUid)
                .collection("exams")
                .document(finalExam.id)
                .set(finalExam)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. Lưu Phiếu THPT
    suspend fun saveThptExam(exam: ThptExamModel): Result<Unit> {
        return try {
            val currentUserUid = auth.currentUser?.uid ?: throw Exception("Người dùng chưa đăng nhập!")

            val finalExam = exam.copy(userId = currentUserUid)

            db.collection("users")
                .document(currentUserUid)
                .collection("thpt_exams")
                .document(finalExam.id)
                .set(finalExam)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}