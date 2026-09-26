package com.example.quizmark.data.repository

import com.example.quizmark.data.model.ExamModel
import com.example.quizmark.data.model.ThptExamModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.firestore.Source

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

    // 3. Lắng nghe danh sách phiếu Basic (Hỗ trợ Offline qua Cache)
    fun getAllBasicExamsFlow(): Flow<List<ExamModel>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(uid).collection("exams")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val exams = snapshot?.toObjects(ExamModel::class.java) ?: emptyList()
                trySend(exams)
            }

        awaitClose { listener.remove() }
    }

    // 4. Lắng nghe danh sách phiếu THPT
    fun getAllThptExamsFlow(): Flow<List<ThptExamModel>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(uid).collection("thpt_exams")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val exams = snapshot?.toObjects(ThptExamModel::class.java) ?: emptyList()
                trySend(exams)
            }

        awaitClose { listener.remove() }
    }

    // 5. Xoá đề thi (Hỗ trợ Offline)
    suspend fun deleteExam(examId: String, isThpt: Boolean): Result<Unit> {
        return try {
            val currentUserUid = auth.currentUser?.uid ?: throw Exception("Người dùng chưa đăng nhập!")

            val collectionName = if (isThpt) "thpt_exams" else "exams"

            db.collection("users")
                .document(currentUserUid)
                .collection(collectionName)
                .document(examId)
                .delete()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 6. Lấy chi tiết một đề Basic
    suspend fun getBasicExamById(examId: String): ExamModel? {
        return try {
            val uid = auth.currentUser?.uid ?: return null
            val snapshot = db.collection("users")
                .document(uid)
                .collection("exams")
                .document(examId)
                .get(Source.CACHE)
                .await()
            snapshot.toObject(ExamModel::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // 7. Lấy chi tiết một đề THPT (Hỗ trợ Offline)
    suspend fun getThptExamById(examId: String): ThptExamModel? {
        return try {
            val uid = auth.currentUser?.uid ?: return null
            val snapshot = db.collection("users")
                .document(uid)
                .collection("thpt_exams")
                .document(examId)
                .get(com.google.firebase.firestore.Source.CACHE)
                .await()
            snapshot.toObject(ThptExamModel::class.java)
        } catch (e: Exception) {
            null
        }
    }
}