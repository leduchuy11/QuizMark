package com.example.quizmark.ui.main.home.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizmark.data.model.HistoryRecord
import com.example.quizmark.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val allRecords: List<HistoryRecord> = emptyList(),

    // Bộ lọc
    val availableRosters: List<Pair<String, String>> = emptyList(), // Pair<RosterId, RosterName>
    val selectedRosterId: String = "ALL", // ALL, NONE (Ẩn danh), hoặc ID thực tế

    // Chỉ số
    val totalExams: Int = 0,
    val averageScore: Double = 0.0,
    val passRate: Int = 0,

    // Dữ liệu đồ thị: 10 mốc từ [0,1] đến (9,10]
    val scoreDistribution: List<Int> = List(10) { 0 }
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            try {
                historyRepository.getAllRecords().collect { records ->
                    // Lọc lấy danh sách các lớp duy nhất
                    val rostersMap = mutableMapOf<String, String>()
                    records.forEach {
                        if (it.rosterId != "anonymous") rostersMap[it.rosterId] = it.rosterName
                    }

                    _uiState.update { state ->
                        state.copy(
                            allRecords = records,
                            availableRosters = rostersMap.toList(),
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

    private fun calculateStatistics() {
        val state = _uiState.value

        // 1. Lọc theo Dropdown
        val filteredList = when (state.selectedRosterId) {
            "ALL" -> state.allRecords
            "NONE" -> state.allRecords.filter { it.rosterId == "anonymous" }
            else -> state.allRecords.filter { it.rosterId == state.selectedRosterId }
        }

        val total = filteredList.size

        if (total == 0) {
            _uiState.update {
                it.copy(
                    totalExams = 0, averageScore = 0.0, passRate = 0,
                    scoreDistribution = List(10) { 0 }
                )
            }
            return
        }

        // 2. Tính 3 thẻ chỉ số
        val avgScore = filteredList.sumOf { it.score } / total
        val passCount = filteredList.count { it.score >= 5.0 }
        val passRatePct = (passCount.toFloat() / total * 100).toInt()

        // 3. Phân bổ điểm 10 mốc
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
                passRate = passRatePct,
                scoreDistribution = buckets.toList()
            )
        }
    }
}