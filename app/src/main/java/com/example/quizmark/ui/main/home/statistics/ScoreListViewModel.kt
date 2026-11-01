package com.example.quizmark.ui.main.home.statistics

import androidx.lifecycle.SavedStateHandle
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

@HiltViewModel
class ScoreListViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Lấy rosterId được truyền sang từ Navigation
    private val rosterId: String = savedStateHandle.get<String>("rosterId") ?: "ALL"

    private val _records = MutableStateFlow<List<HistoryRecord>>(emptyList())
    val records = _records.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadScores()
    }

    private fun loadScores() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                historyRepository.getAllRecords().collect { allRecords ->
                    // Lọc theo rosterId
                    val filtered = when (rosterId) {
                        "ALL" -> allRecords
                        "NONE" -> allRecords.filter { it.rosterId == "anonymous" }
                        else -> allRecords.filter { it.rosterId == rosterId }
                    }

                    // Sắp xếp theo tên
                    val sorted = filtered.sortedBy { it.studentName }

                    _records.value = sorted
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }
}