package com.example.quizmark.ui.main.home.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizmark.data.model.ClassroomModel
import com.example.quizmark.data.model.HistoryRecord
import com.example.quizmark.data.model.RosterModel
import com.example.quizmark.data.repository.ExamListRepository
import com.example.quizmark.data.repository.HistoryRepository
import com.example.quizmark.util.ClassroomScoreSummary
import com.example.quizmark.util.DuplicateSbdClusterResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val allRecords: List<HistoryRecord> = emptyList(),

    // Bộ lọc danh sách thi
    val availableRosters: List<Pair<String, String>> = emptyList(), // Pair<RosterId, RosterName>
    val selectedRosterId: String = "ALL", // ALL, NONE (Ẩn danh), hoặc ID thực tế

    // Bộ lọc Lớp học
    val availableClasses: List<Pair<String, String>> = emptyList(), // Pair<ClassId, ClassName>
    val selectedClassId: String = "ALL", // ALL hoặc ID lớp cụ thể

    // Thống kê điểm theo từng lớp học
    val classroomSummaries: List<ClassroomScoreSummary> = emptyList(),

    // Chỉ số tổng hợp của bộ lọc hiện tại
    val totalExams: Int = 0,
    val averageScore: Double = 0.0,
    val minScore: Double = 0.0,
    val maxScore: Double = 0.0,
    val passRate: Int = 0,

    // Dữ liệu đồ thị: 10 mốc từ [0,1] đến (9,10]
    val scoreDistribution: List<Int> = List(10) { 0 },
    val filteredRecords: List<HistoryRecord> = emptyList()
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val examListRepository: ExamListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState = _uiState.asStateFlow()

    private var cachedClassrooms: List<ClassroomModel> = emptyList()
    private var cachedRosters: List<RosterModel> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            try {
                combine(
                    historyRepository.getAllRecords(),
                    examListRepository.getAllRostersFlow(),
                    examListRepository.getAllClassroomsFlow()
                ) { rawRecords, rosters, classrooms ->
                    Triple(rawRecords, rosters, classrooms)
                }.collect { (rawRecords, rosters, classrooms) ->
                    cachedRosters = rosters
                    cachedClassrooms = classrooms

                    // Tự động bổ sung classId / className cho cả các bản ghi cũ
                    val enrichedRecords = DuplicateSbdClusterResolver.enrichRecordsWithClassInfo(
                        records = rawRecords,
                        rosters = rosters,
                        classrooms = classrooms
                    )

                    val rostersMap = linkedMapOf<String, String>()
                    enrichedRecords.forEach {
                        if (it.rosterId != "anonymous") rostersMap[it.rosterId] = it.rosterName
                    }

                    val classesMap = linkedMapOf<String, String>()
                    classrooms.forEach { cls ->
                        classesMap[cls.id] = cls.name
                    }
                    enrichedRecords.forEach { rec ->
                        if (rec.classId.isNotBlank() && rec.className.isNotBlank()) {
                            classesMap[rec.classId] = rec.className
                        }
                    }

                    _uiState.update { state ->
                        state.copy(
                            allRecords = enrichedRecords,
                            availableRosters = rostersMap.toList(),
                            availableClasses = classesMap.toList(),
                            isLoading = false
                        )
                    }
                    calculateStatistics()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateFilter(rosterId: String) {
        _uiState.update { it.copy(selectedRosterId = rosterId) }
        calculateStatistics()
    }

    fun updateClassFilter(classId: String) {
        _uiState.update { it.copy(selectedClassId = classId) }
        calculateStatistics()
    }

    private fun calculateStatistics() {
        val state = _uiState.value

        // Lọc theo Danh sách thi
        val rosterFiltered = when (state.selectedRosterId) {
            "ALL" -> state.allRecords
            "NONE" -> state.allRecords.filter { it.rosterId == "anonymous" }
            else -> state.allRecords.filter { it.rosterId == state.selectedRosterId }
        }

        // Tính bảng thống kê từng lớp trong phạm vi Danh sách thi đang chọn
        val classSummaries = DuplicateSbdClusterResolver.buildClassroomSummaries(
            records = rosterFiltered,
            classrooms = cachedClassrooms
        )

        // Lọc tiếp theo Lớp học nếu người dùng chọn 1 lớp cụ thể
        val filteredList = when (state.selectedClassId) {
            "ALL" -> rosterFiltered
            "UNASSIGNED" -> rosterFiltered.filter { it.classId.isBlank() }
            else -> rosterFiltered.filter { it.classId == state.selectedClassId }
        }

        val total = filteredList.size

        if (total == 0) {
            _uiState.update {
                it.copy(
                    totalExams = 0,
                    averageScore = 0.0,
                    minScore = 0.0,
                    maxScore = 0.0,
                    passRate = 0,
                    scoreDistribution = List(10) { 0 },
                    filteredRecords = emptyList(),
                    classroomSummaries = classSummaries
                )
            }
            return
        }

        // Tính các chỉ số Điểm TB, Thấp nhất, Cao nhất
        val avgScore = filteredList.sumOf { it.score } / total
        val minScore = filteredList.minOf { it.score }
        val maxScore = filteredList.maxOf { it.score }
        val passCount = filteredList.count { it.score >= 5.0 }
        val passRatePct = (passCount.toFloat() / total * 100).toInt()

        // Phân bổ điểm 10 mốc
        val buckets = IntArray(10)
        filteredList.forEach { record ->
            val index = when {
                record.score <= 1.0 -> 0
                record.score <= 2.0 -> 1
                record.score <= 3.0 -> 2
                record.score <= 4.0 -> 3
                record.score <= 5.0 -> 4
                record.score <= 6.0 -> 5
                record.score <= 7.0 -> 6
                record.score <= 8.0 -> 7
                record.score <= 9.0 -> 8
                else -> 9
            }
            buckets[index]++
        }

        _uiState.update {
            it.copy(
                totalExams = total,
                averageScore = avgScore,
                minScore = minScore,
                maxScore = maxScore,
                passRate = passRatePct,
                scoreDistribution = buckets.toList(),
                filteredRecords = filteredList,
                classroomSummaries = classSummaries
            )
        }
    }

    fun getCurrentUserEmail(): String {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""
    }
}
