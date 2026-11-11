package com.example.quizmark.util.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.quizmark.data.model.HistoryRecord
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SyncRecordWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            // 1. Lấy dữ liệu từ lúc offline do Repository truyền sang
            val userId = inputData.getString("USER_ID") ?: return Result.failure()
            val recordId = inputData.getString("ID") ?: return Result.failure()

            val record = HistoryRecord(
                id = recordId,
                rosterId = inputData.getString("ROSTER_ID") ?: "anonymous",
                rosterName = inputData.getString("ROSTER_NAME") ?: "Ẩn danh",
                studentName = inputData.getString("STUDENT_NAME") ?: "",
                studentCode = inputData.getString("STUDENT_CODE") ?: "",
                examCode = inputData.getString("EXAM_CODE") ?: "",
                score = inputData.getDouble("SCORE", 0.0),
                correctCount = inputData.getInt("CORRECT", 0),
                wrongCount = inputData.getInt("WRONG", 0),
                blankCount = inputData.getInt("BLANK", 0),
                totalQuestions = inputData.getInt("TOTAL", 0),
                templateId = inputData.getInt("TEMPLATE_ID", 1),
                localImagePath = inputData.getString("IMAGE_PATH") ?: "",
                localOriginalImagePath = inputData.getString("ORIGINAL_IMAGE_PATH") ?: "",
                timestamp = inputData.getLong("TIMESTAMP", System.currentTimeMillis())
            )

            // 2. Bắn thẳng lên Server
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("history_records")
                .document(recordId)
                .set(record)
                .await()

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}
