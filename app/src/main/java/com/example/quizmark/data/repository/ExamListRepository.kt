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

class ExamListRepository @Inject constructor(
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

    // Hàm Cập nhật học sinh (Offline-First)
    fun updateStudent(updatedStudent: StudentModel, initialRoster: RosterModel?, newRoster: RosterModel?): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Người dùng chưa đăng nhập!")
            val dbRef = db.collection("users").document(uid)

            dbRef.collection("students").document(updatedStudent.id).set(updatedStudent)

            // 1. Xử lý logic Danh sách học sinh
            if (initialRoster?.id == newRoster?.id) {
                // TRƯỜNG HỢP A: Không đổi lớp (Từ lớp A -> Lớp A, hoặc Không có -> Không có)
                if (newRoster != null) {
                    val updatedList = newRoster.students.map {
                        if (it.id == updatedStudent.id) updatedStudent else it
                    }
                    val updatedRoster = newRoster.copy(students = updatedList)
                    dbRef.collection("rosters").document(newRoster.id).set(updatedRoster)
                }
            } else {
                // TRƯỜNG HỢP B: Có sự thay đổi lớp
                // B1: Xóa học sinh khỏi lớp CŨ (nếu trước đó có lớp)
                if (initialRoster != null) {
                    val updatedOldList = initialRoster.students.filter { it.id != updatedStudent.id }
                    val updatedOldRoster = initialRoster.copy(students = updatedOldList)
                    dbRef.collection("rosters").document(initialRoster.id).set(updatedOldRoster)
                }
                // B2: Thêm học sinh vào lớp MỚI (nếu lớp mới không phải là "Không có")
                if (newRoster != null) {
                    val updatedNewList = newRoster.students.toMutableList()
                    updatedNewList.add(updatedStudent)
                    val updatedNewRoster = newRoster.copy(students = updatedNewList)
                    dbRef.collection("rosters").document(newRoster.id).set(updatedNewRoster)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Hàm Xóa học sinh (Offline-First)
    fun deleteStudent(studentId: String, initialRoster: RosterModel?): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Người dùng chưa đăng nhập!")
            val dbRef = db.collection("users").document(uid)

            // 1. Xóa khỏi bảng tổng
            dbRef.collection("students").document(studentId).delete()

            // 2. Xóa khỏi lớp (Nếu có)
            if (initialRoster != null) {
                val updatedList = initialRoster.students.filter { it.id != studentId }
                val updatedRoster = initialRoster.copy(students = updatedList)
                dbRef.collection("rosters").document(initialRoster.id).set(updatedRoster)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Hàm Import hàng loạt học sinh từ file Excel
    fun importStudents(
        importedStudents: List<StudentModel>,
        rosterId: String?,
        currentAllStudents: List<StudentModel>,
        rosterList: List<RosterModel>
    ): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Người dùng chưa đăng nhập!")
            val dbRef = db.collection("users").document(uid)
            val batch = db.batch()

            val rostersToUpdate = mutableMapOf<String, RosterModel>()

            if (rosterId != null) {
                val targetRoster = rosterList.find { it.id == rosterId }
                if (targetRoster != null) {
                    rostersToUpdate[rosterId] = targetRoster
                }
            }

            importedStudents.forEach { importedStudent ->

                // KIỂM TRA TRÙNG LẶP (CHỈ CHECK TRONG PHẠM VI DANH SÁCH ĐÍCH)
                val existingStudentInContext = if (rosterId != null) {
                    // Nếu import vào 1 danh sách cụ thể: Chỉ tìm học sinh có cùng SBD trong danh sách đó
                    rostersToUpdate[rosterId]?.students?.find { it.studentCode == importedStudent.studentCode }
                } else {
                    // Nếu import vào "Không có" (Không chọn danh sách)
                    null
                }

                // Nếu đã có trong danh sách này -> lấy ID cũ để ghi đè. Nếu chưa -> dùng ID mới toanh
                val finalStudentId = existingStudentInContext?.id ?: importedStudent.id
                val finalStudent = importedStudent.copy(id = finalStudentId)

                // 1. Ghi/Cập nhật học sinh vào bảng lưu trữ tổng
                val studentDocRef = dbRef.collection("students").document(finalStudentId)
                batch.set(studentDocRef, finalStudent)

                // 2. Thêm/Cập nhật học sinh vào danh sách thi (Tuyệt đối không đụng chạm danh sách khác)
                if (rosterId != null) {
                    val currentTargetRoster = rostersToUpdate[rosterId]!!
                    val targetList = currentTargetRoster.students.toMutableList()

                    val existingIndex = targetList.indexOfFirst { it.id == finalStudentId }
                    if (existingIndex != -1) {
                        targetList[existingIndex] = finalStudent // Ghi đè thông tin nếu trùng
                    } else {
                        targetList.add(finalStudent) // Thêm mới hoàn toàn
                    }
                    rostersToUpdate[rosterId] = currentTargetRoster.copy(students = targetList)
                }
            }

            rostersToUpdate.values.forEach { roster ->
                val rosterDocRef = dbRef.collection("rosters").document(roster.id)
                batch.set(rosterDocRef, roster)
            }

            batch.commit()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}