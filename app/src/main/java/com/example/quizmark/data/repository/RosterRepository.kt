package com.example.quizmark.data.repository

import com.example.quizmark.data.model.RosterModel
import com.example.quizmark.data.model.StudentModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class RosterRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    // 1. Lưu Danh sách thi (Hỗ trợ Offline )
    fun saveRoster(roster: RosterModel): Result<Unit> {
        return try {
            val currentUserUid = auth.currentUser?.uid ?: throw Exception("Người dùng chưa đăng nhập!")
            val finalRoster = roster.copy(userId = currentUserUid)

            db.collection("users")
                .document(currentUserUid)
                .collection("rosters")
                .document(finalRoster.id)
                .set(finalRoster)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. Lắng nghe danh sách Roster (Tự động cập nhật Realtime + Load Offline qua Snapshot)
    fun getAllRostersFlow(): Flow<List<RosterModel>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(uid).collection("rosters")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val rosters = snapshot?.toObjects(RosterModel::class.java) ?: emptyList()
                trySend(rosters)
            }

        awaitClose { listener.remove() }
    }

    // 3. Lấy chi tiết một Roster (Giải quyết bài toán Cache trống)
    suspend fun getRosterById(rosterId: String): RosterModel? {
        val uid = auth.currentUser?.uid ?: return null
        val docRef = db.collection("users")
            .document(uid)
            .collection("rosters")
            .document(rosterId)

        return try {
            // Bước 1: Ưu tiên kéo từ Cache để đạt tốc độ 0 mili-giây
            val snapshot = docRef.get(Source.CACHE).await()
            snapshot.toObject(RosterModel::class.java)
        } catch (e: Exception) {
            // Bước 2: Nếu Cache trống (văng Exception), lập tức kéo từ Server làm phao cứu sinh
            try {
                val snapshot = docRef.get(Source.SERVER).await()
                snapshot.toObject(RosterModel::class.java)
            } catch (ex: Exception) {
                null
            }
        }
    }

    // Xóa danh sách thi (Hỗ trợ Offline)
    fun deleteRoster(rosterId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Người dùng chưa đăng nhập!")

            db.collection("users")
                .document(uid)
                .collection("rosters")
                .document(rosterId)
                .delete()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Hàm lưu học sinh (Hỗ trợ Offline-First)
    fun saveStudent(student: StudentModel, roster: RosterModel?): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Người dùng chưa đăng nhập!")
            val dbRef = db.collection("users").document(uid)

            // 1. Lưu vào Collection "students"
            dbRef.collection("students").document(student.id).set(student)

            // 2. Nếu có chọn lớp -> Cập nhật luôn học sinh này vào danh sách của lớp đó
            if (roster != null) {
                val updatedStudents = roster.students.toMutableList()

                updatedStudents.add(student)

                val updatedRoster = roster.copy(students = updatedStudents)

                dbRef.collection("rosters").document(roster.id).set(updatedRoster)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Lắng nghe toàn bộ Học sinh (Bao gồm cả có lớp và không có lớp)
    fun getAllStudentsFlow(): Flow<List<StudentModel>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(uid).collection("students")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val students = snapshot?.toObjects(StudentModel::class.java) ?: emptyList()
                trySend(students)
            }

        awaitClose { listener.remove() }
    }
}