package com.example.quizmark.ui.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizmark.data.repository.ExamListRepository
import com.example.quizmark.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val averageScore: Double = 0.0,
    val rosterCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val examListRepository: ExamListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        // 1. Tính toán điểm trung bình từ History
        viewModelScope.launch(Dispatchers.IO) {
            try {
                historyRepository.getAllRecords().collect { records ->
                    if (records.isEmpty()) {
                        _uiState.update { it.copy(averageScore = 0.0) }
                    } else {
                        // Tính tổng tất cả điểm
                        val totalScore = records.sumOf { it.score }
                        // Tính trung bình cộng
                        val average = totalScore / records.size
                        _uiState.update { it.copy(averageScore = average) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Đếm số lượng danh sách lớp học (Roster)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                examListRepository.getAllRostersFlow().collect { rosters ->
                    _uiState.update { it.copy(rosterCount = rosters.size) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}