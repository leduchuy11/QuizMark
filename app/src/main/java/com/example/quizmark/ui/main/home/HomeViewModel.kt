package com.example.quizmark.ui.main.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizmark.data.repository.ExamListRepository
import com.example.quizmark.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val averageScore: Double = 0.0,
    val rosterCount: Int = 0,
    val todayGoal: Int = 10,
    val todayScannedCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val examListRepository: ExamListRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val sharedPreferences = context.getSharedPreferences("QuizMarkPrefs", Context.MODE_PRIVATE)

    // Đọc mục tiêu đã lưu (nếu chưa có thì mặc định là 10)
    private val _uiState = MutableStateFlow(
        HomeUiState(todayGoal = sharedPreferences.getInt("TODAY_GOAL", 10))
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        // 1. Tính toán điểm trung bình & đếm bài chấm hôm nay từ History
        viewModelScope.launch(Dispatchers.IO) {
            try {
                historyRepository.getAllRecords().collect { records ->
                    if (records.isEmpty()) {
                        _uiState.update { it.copy(averageScore = 0.0, todayScannedCount = 0) }
                    } else {
                        // Tính điểm TB
                        val totalScore = records.sumOf { it.score }
                        val average = totalScore / records.size

                        // Lấy mốc thời gian bắt đầu và kết thúc của ngày hôm nay
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        val startOfDay = calendar.timeInMillis

                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                        calendar.set(Calendar.MINUTE, 59)
                        calendar.set(Calendar.SECOND, 59)
                        calendar.set(Calendar.MILLISECOND, 999)
                        val endOfDay = calendar.timeInMillis

                        // Lọc và đếm số lượng bài có timestamp nằm trong khoảng hôm nay
                        val scannedToday = records.count { it.timestamp in startOfDay..endOfDay }

                        _uiState.update {
                            it.copy(
                                averageScore = average,
                                todayScannedCount = scannedToday
                            )
                        }
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

    // 3. Hàm cập nhật mục tiêu mới và lưu vào máy
    fun updateTodayGoal(newGoal: Int) {
        sharedPreferences.edit().putInt("TODAY_GOAL", newGoal).apply()
        _uiState.update { it.copy(todayGoal = newGoal) }
    }
}