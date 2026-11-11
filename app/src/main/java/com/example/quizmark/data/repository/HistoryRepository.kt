package com.example.quizmark.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.quizmark.data.model.FinalGradingResult
import com.example.quizmark.data.model.HistoryRecord
import com.example.quizmark.util.OriginalScanStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface HistoryRepository {
    suspend fun saveRecord(context: Context, result: FinalGradingResult, rosterId: String?, rosterName: String?): String
    fun getAllRecords(): Flow<List<HistoryRecord>>
    suspend fun deleteRecord(context: Context, recordId: String)
}

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : HistoryRepository {
    override suspend fun saveRecord(
        context: Context,
        result: FinalGradingResult,
        rosterId: String?,
        rosterName: String?
    ): String {
        val userId = auth.currentUser?.uid ?: throw Exception("Người dùng chưa đăng nhập")
        val finalRosterId = rosterId ?: "anonymous"
        val finalRosterName = rosterName ?: "Ẩn danh"

        val collectionRef = firestore.collection("users").document(userId).collection("history_records")

        // 1. Lưu ảnh vĩnh viễn vào filesDir
        val imagesDir = File(context.filesDir, "history_images")
        if (!imagesDir.exists()) imagesDir.mkdirs()

        val fileName = "scan_${System.currentTimeMillis()}.jpg"
        val file = File(imagesDir, fileName)

        // Nén và lưu ảnh xuống máy
        result.processedImage?.let { bitmap ->
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
        }


        // 2. Logic tạo Document ID
        val documentId = if (rosterId != null) {
            "${finalRosterId}_${result.sbd}_${result.examCode}" // ID duy nhất
        } else {
            collectionRef.document().id // Tạo ID ngẫu nhiên nếu quét lẻ
        }

        val docRef = collectionRef.document(documentId)

        // 3. Đóng gói dữ liệu
        val record = HistoryRecord(
            id = documentId,
            rosterId = finalRosterId,
            rosterName = finalRosterName,
            studentName = result.studentName,
            studentCode = result.sbd,
            examCode = result.examCode,
            score = result.score,
            correctCount = result.correctCount,
            wrongCount = result.wrongCount,
            blankCount = result.blankCount,
            totalQuestions = result.totalQuestions,
            templateId = result.templateId,
            localImagePath = file.absolutePath,
            localOriginalImagePath = result.originalImagePath.orEmpty(),
            timestamp = System.currentTimeMillis()
        )

        // 4. Đồng bộ dữ liệu (KHÔNG DÙNG .await())
        // Gọi lệnh set() mà không có .await() -> Hàm này sẽ ném dữ liệu cho Firestore xử lý ngầm.
        // Nếu có mạng: Firestore tự đẩy lên ngay.
        // Nếu mất mạng: Firestore tự lưu vào Cache máy, hàm vẫn kết thúc ngay lập tức,
        // UI thoát Loading liền tay. Khi nào có 3G lại, Firestore tự đồng bộ.
        docRef.set(record)

        // 5. HẸN GIỜ ĐỒNG BỘ NGẦM (Dù tắt app vẫn chạy khi có mạng)
        val inputData = androidx.work.Data.Builder()
            .putString("USER_ID", userId)
            .putString("ID", record.id)
            .putString("ROSTER_ID", record.rosterId)
            .putString("ROSTER_NAME", record.rosterName)
            .putString("STUDENT_NAME", record.studentName)
            .putString("STUDENT_CODE", record.studentCode)
            .putString("EXAM_CODE", record.examCode)
            .putDouble("SCORE", record.score)
            .putInt("CORRECT", record.correctCount)
            .putInt("WRONG", record.wrongCount)
            .putInt("BLANK", record.blankCount)
            .putInt("TOTAL", record.totalQuestions)
            .putInt("TEMPLATE_ID", record.templateId)
            .putString("IMAGE_PATH", record.localImagePath)
            .putString("ORIGINAL_IMAGE_PATH", record.localOriginalImagePath)
            .putLong("TIMESTAMP", record.timestamp)
            .build()

        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val syncWork = androidx.work.OneTimeWorkRequestBuilder<com.example.quizmark.util.worker.SyncRecordWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        androidx.work.WorkManager.getInstance(context).enqueue(syncWork)

        return documentId
    }

    override fun getAllRecords(): Flow<List<HistoryRecord>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val collectionRef = firestore.collection("users").document(userId).collection("history_records")

        // addSnapshotListener làm 2 nhiệm vụ cùng lúc:
        // 1. Nhả ngay dữ liệu từ Cache Local ra trước.
        // 2. Lắng nghe Server. Nếu có mạng và Server có bản ghi mới, nó tự gọi lại khối lệnh này.
        val listenerRegistration = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val records = snapshot.documents.mapNotNull { it.toObject(HistoryRecord::class.java) }
                trySend(records) // Bắn dữ liệu mới vào luồng Flow
            }
        }
        awaitClose {
            listenerRegistration.remove()
        }
    }

    override suspend fun deleteRecord(context: Context, recordId: String) {
        val userId = auth.currentUser?.uid ?: return
        val docRef = firestore.collection("users").document(userId).collection("history_records").document(recordId)

        try {
            // 1. Lấy thông tin từ Cache để xóa file ảnh cục bộ
            val snapshot = try {
                docRef.get(Source.CACHE).await()
            } catch (e: Exception) {
                null
            }

            if (snapshot != null && snapshot.exists()) {
                val record = snapshot.toObject(HistoryRecord::class.java)
                record?.localOriginalImagePath?.let { OriginalScanStore.delete(context, it) }
                record?.localImagePath?.let { path ->
                    if (path.isNotBlank()) {
                        val file = File(path)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                }
            }

            // 2. Xóa Cache Local ngay lập tức
            docRef.delete()

            // 3. Đặt lịch WorkManager đảm bảo đồng bộ lệnh xóa lên Server
            val inputData = androidx.work.Data.Builder()
                .putString("USER_ID", userId)
                .putString("ID", recordId)
                .build()

            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val deleteWork = androidx.work.OneTimeWorkRequestBuilder<com.example.quizmark.util.worker.DeleteRecordWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            androidx.work.WorkManager.getInstance(context).enqueue(deleteWork)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
