package com.example.quizmark.util

import com.example.quizmark.data.model.ClassroomModel
import com.example.quizmark.data.model.HistoryRecord
import com.example.quizmark.data.model.RosterModel
import com.example.quizmark.data.model.StudentModel

data class DuplicateSbdCluster(
    val sbd: String,
    val studentName: String,
    val records: List<HistoryRecord>
)

data class MissingStudentSuggestion(
    val student: StudentModel,
    val hammingDistance: Int? // null nếu SBD độ dài khác nhau; 1 = lệch đúng 1 chữ số
)

enum class ScoreSortOrder {
    SCORE_ASC,   // Điểm từ thấp đến cao
    SCORE_DESC,  // Điểm từ cao xuống thấp
    SBD_ASC,     // Theo SBD
    NAME_ASC,    // Theo Tên
    NEWEST       // Mới chấm gần nhất
}

data class ClassroomScoreSummary(
    val classId: String,
    val className: String,
    val totalExams: Int,
    val averageScore: Double,
    val minScore: Double,
    val maxScore: Double,
    val passRate: Int,
    val records: List<HistoryRecord>
)

object DuplicateSbdClusterResolver {

    //Tự động ánh xạ/bổ sung classId và className cho cả các bản ghi chấm cũ
    fun enrichRecordsWithClassInfo(
        records: List<HistoryRecord>,
        rosters: List<RosterModel>,
        classrooms: List<ClassroomModel>
    ): List<HistoryRecord> {
        if (records.isEmpty()) return emptyList()
        val rosterMap = rosters.associateBy { it.id }
        val classById = classrooms.associateBy { it.id }

        return records.map { record ->
            if (record.classId.isNotBlank() && record.className.isNotBlank()) {
                return@map record
            }
            if (record.classId.isNotBlank()) {
                val matchedCls = classById[record.classId]
                if (matchedCls != null) {
                    return@map record.copy(className = matchedCls.name)
                }
            }

            val code = record.studentCode.trim()
            if (!isTrackableSbd(code)) return@map record

            // Ưu tiên tìm trong Roster của bài chấm đó
            val rosterStudent = rosterMap[record.rosterId]?.students?.find {
                it.studentCode.trim() == code
            }
            if (rosterStudent != null && rosterStudent.classId.isNotBlank()) {
                val clsName = rosterStudent.className.ifBlank {
                    classById[rosterStudent.classId]?.name.orEmpty()
                }
                return@map record.copy(
                    classId = rosterStudent.classId,
                    className = clsName,
                    studentName = record.studentName.takeIf { it.isNotBlank() && it != "Ẩn danh" }
                        ?: rosterStudent.name
                )
            }

            // Nếu trong Roster thuộc về 1 lớp duy nhất (sourceClassIds.size == 1), gán luôn lớp đó
            val singleSourceRoster = rosterMap[record.rosterId]
            if (singleSourceRoster != null && singleSourceRoster.sourceClassIds.size == 1) {
                val cid = singleSourceRoster.sourceClassIds.first()
                val cname = singleSourceRoster.sourceClassNames.firstOrNull()
                    ?: classById[cid]?.name.orEmpty()
                if (cid.isNotBlank()) {
                    return@map record.copy(classId = cid, className = cname)
                }
            }

            //Dò theo SBD trong danh sách Lớp học
            val candidateClasses = classrooms.mapNotNull { cls ->
                val student = cls.students.find { it.studentCode.trim() == code }
                if (student != null) cls to student else null
            }

            val bestMatch = candidateClasses.find { (_, student) ->
                record.studentName.isNotBlank() &&
                    record.studentName != "Ẩn danh" &&
                    student.name.equals(record.studentName, ignoreCase = true)
            } ?: candidateClasses.firstOrNull()

            if (bestMatch != null) {
                val (cls, student) = bestMatch
                record.copy(
                    classId = cls.id,
                    className = cls.name,
                    studentName = record.studentName.takeIf { it.isNotBlank() && it != "Ẩn danh" }
                        ?: student.name
                )
            } else {
                record
            }
        }
    }

    //Lọc toàn bộ bài chấm thuộc về một Lớp học cụ thể
    fun filterRecordsForClassroom(
        classroom: ClassroomModel,
        allRecords: List<HistoryRecord>,
        rosters: List<RosterModel>,
        allClassrooms: List<ClassroomModel>
    ): List<HistoryRecord> {
        val enriched = enrichRecordsWithClassInfo(allRecords, rosters, allClassrooms)
        val classStudentCodes = classroom.students
            .map { it.studentCode.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        return enriched.filter { rec ->
            rec.classId == classroom.id ||
                (rec.classId.isBlank() && rec.studentCode.trim() in classStudentCodes)
        }.map { rec ->
            if (rec.classId.isBlank()) {
                rec.copy(classId = classroom.id, className = classroom.name)
            } else {
                rec
            }
        }
    }

    //Tổng hợp thống kê điểm (Điểm TB, Thấp nhất, Cao nhất, Tỉ lệ đạt) cho từng Lớp học
    fun buildClassroomSummaries(
        records: List<HistoryRecord>,
        classrooms: List<ClassroomModel>
    ): List<ClassroomScoreSummary> {
        if (records.isEmpty()) return emptyList()
        val classById = classrooms.associateBy { it.id }

        val grouped = records.groupBy { rec ->
            rec.classId.ifBlank { "UNASSIGNED" }
        }

        return grouped.mapNotNull { (cid, classRecords) ->
            if (classRecords.isEmpty()) return@mapNotNull null
            val cname = if (cid == "UNASSIGNED") {
                "Chưa phân lớp"
            } else {
                classRecords.firstNotNullOfOrNull { it.className.takeIf { n -> n.isNotBlank() } }
                    ?: classById[cid]?.name
                    ?: "Lớp học"
            }

            val total = classRecords.size
            val avg = classRecords.sumOf { it.score } / total
            val min = classRecords.minOf { it.score }
            val max = classRecords.maxOf { it.score }
            val passCount = classRecords.count { it.score >= 5.0 }
            val passRate = ((passCount.toFloat() / total) * 100).toInt()

            ClassroomScoreSummary(
                classId = cid,
                className = cname,
                totalExams = total,
                averageScore = avg,
                minScore = min,
                maxScore = max,
                passRate = passRate,
                records = classRecords
            )
        }.sortedWith(
            compareBy<ClassroomScoreSummary> { it.classId == "UNASSIGNED" }
                .thenBy { it.className }
        )
    }

    //Sắp xếp danh sách điểm theo thứ tự người dùng chọn (Thấp -> Cao, Cao -> Thấp, SBD, Tên, Mới nhất)
    fun sortRecords(records: List<HistoryRecord>, order: ScoreSortOrder): List<HistoryRecord> {
        return when (order) {
            ScoreSortOrder.SCORE_ASC -> records.sortedWith(
                compareBy<HistoryRecord> { it.score }.thenBy { it.studentCode }.thenBy { it.studentName }
            )
            ScoreSortOrder.SCORE_DESC -> records.sortedWith(
                compareByDescending<HistoryRecord> { it.score }.thenBy { it.studentCode }.thenBy { it.studentName }
            )
            ScoreSortOrder.SBD_ASC -> records.sortedWith(
                compareBy<HistoryRecord> { it.studentCode }.thenBy { it.studentName }
            )
            ScoreSortOrder.NAME_ASC -> records.sortedWith(
                compareBy<HistoryRecord> { it.studentName }.thenBy { it.studentCode }
            )
            ScoreSortOrder.NEWEST -> records.sortedByDescending { it.timestamp }
        }
    }

    //Gom các bản ghi trong danh sách điểm có cùng SBD (khác rỗng/"Trống"/"Lỗi") và số lượng > 1 thành các cụm trùng SBD
    fun buildDuplicateClusters(records: List<HistoryRecord>): List<DuplicateSbdCluster> {
        return records
            .filter { isTrackableSbd(it.studentCode) }
            .groupBy { it.studentCode.trim() }
            .filterValues { it.size > 1 }
            .entries
            .sortedBy { it.key }
            .map { (sbd, group) ->
                val displayName = group.firstNotNullOfOrNull { rec ->
                    rec.studentName.takeIf { it.isNotBlank() && it != "Ẩn danh" }
                } ?: group.first().studentName.ifBlank { "Ẩn danh" }

                DuplicateSbdCluster(
                    sbd = sbd,
                    studentName = displayName,
                    records = group.sortedByDescending { it.timestamp }
                )
            }
    }

    //Tìm danh sách học sinh trong Roster CHƯA CÓ ĐIỂM
    fun suggestMissingStudents(
        rosterStudents: List<StudentModel>,
        records: List<HistoryRecord>,
        duplicatedSbd: String
    ): List<MissingStudentSuggestion> {
        val gradedSbds = records
            .map { it.studentCode.trim() }
            .filter { isTrackableSbd(it) }
            .toSet()

        val target = duplicatedSbd.trim()

        return rosterStudents
            .filter { it.studentCode.isNotBlank() && it.studentCode.trim() !in gradedSbds }
            .map { student ->
                val dist = hammingDistance(target, student.studentCode.trim())
                MissingStudentSuggestion(student = student, hammingDistance = dist)
            }
            .sortedWith(
                compareBy<MissingStudentSuggestion> { it.hammingDistance ?: Int.MAX_VALUE }
                    .thenBy { it.student.studentCode }
            )
    }

    fun hammingDistance(a: String, b: String): Int? {
        if (a.isEmpty() || a.length != b.length) return null
        var diff = 0
        for (i in a.indices) {
            if (a[i] != b[i]) diff++
        }
        return diff
    }

    private fun isTrackableSbd(sbd: String): Boolean {
        val trimmed = sbd.trim()
        return trimmed.isNotEmpty() &&
            !trimmed.equals("Trống", ignoreCase = true) &&
            !trimmed.equals("Empty", ignoreCase = true) &&
            !trimmed.equals("Lỗi", ignoreCase = true) &&
            !trimmed.equals("Err", ignoreCase = true)
    }
}
