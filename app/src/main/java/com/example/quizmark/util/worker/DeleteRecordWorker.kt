package com.example.quizmark.util.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DeleteRecordWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val userId = inputData.getString("USER_ID") ?: return Result.failure()
            val recordId = inputData.getString("ID") ?: return Result.failure()

            // Bắn lệnh xóa thẳng lên Server
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("history_records")
                .document(recordId)
                .delete()
                .await()

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}