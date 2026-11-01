package com.example.quizmark.ui.main.home.history

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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import android.content.Context
import com.example.quizmark.R
import dagger.hilt.android.qualifiers.ApplicationContext

data class HistoryUiState(
    val isLoading: Boolean = true,
    val allRecords: List<HistoryRecord> = emptyList(),
    val groupedRecords: Map<String, List<HistoryRecord>> = emptyMap(),

    // Bộ lọc
    val availableRosters: List<Pair<String, String>> = emptyList(), // Pair<RosterId, RosterName>
    val selectedRosterId: String = "ALL", // "ALL", "anonymous", hoặc ID cụ thể
    val startDate: Long = 0L,
    val endDate: Long = 0L
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()
    private val _toastType = MutableStateFlow(com.example.quizmark.ui.auth.ToastType.SUCCESS)
    val toastType = _toastType.asStateFlow()



    init {
        // Thiết lập ngày mặc định: Từ 7 ngày trước -> Hôm nay
        val calendar = Calendar.getInstance()
        val endOfDay = setEndOfDay(calendar).timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startOfDay = setStartOfDay(calendar).timeInMillis

        _uiState.update { it.copy(startDate = startOfDay, endDate = endOfDay) }

        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }

            try {
                historyRepository.getAllRecords().collect { records ->

                    val rostersMap = mutableMapOf<String, String>()
                    records.forEach {
                        if (it.rosterId != "anonymous") rostersMap[it.rosterId] = it.rosterName
                    }
                    val rosterList = rostersMap.toList()

                    _uiState.update { state ->
                        state.copy(
                            allRecords = records,
                            availableRosters = rosterList,
                            isLoading = false
                        )
                    }

                    applyFilters()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun deleteRecord(recordId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                historyRepository.deleteRecord(context, recordId)

                val updatedRecords = _uiState.value.allRecords.filter { it.id != recordId }
                _uiState.update { it.copy(allRecords = updatedRecords) }
                applyFilters()
                // Kích hoạt thông báo thành công
                _toastMessage.value = context.getString(R.string.delete_record_success)
                _toastType.value = com.example.quizmark.ui.auth.ToastType.SUCCESS
            } catch (e: Exception) {
                e.printStackTrace()
                // Kích hoạt thông báo lỗi nếu có
                _toastMessage.value = context.getString(R.string.delete_record_error)
                _toastType.value = com.example.quizmark.ui.auth.ToastType.ERROR
            }
        }
    }

    fun updateRosterFilter(rosterId: String) {
        _uiState.update { it.copy(selectedRosterId = rosterId) }
        applyFilters()
    }

    fun updateDateFilter(start: Long, end: Long) {
        _uiState.update { it.copy(startDate = start, endDate = end) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filteredList = state.allRecords.filter { record ->
            // Lọc theo Lớp
            val matchRoster = when (state.selectedRosterId) {
                "ALL" -> true
                else -> record.rosterId == state.selectedRosterId
            }
            // Lọc theo Ngày (Timestamp)
            val matchDate = record.timestamp in state.startDate..state.endDate

            matchRoster && matchDate
        }.sortedByDescending { it.timestamp } // Sắp xếp mới nhất lên đầu

        // Gom nhóm theo ngày
        val grouped = filteredList.groupBy { record ->
            val recordDate = dateFormat.format(Date(record.timestamp))
            val todayDate = dateFormat.format(Date())

            if (recordDate == todayDate) "Hôm nay" else recordDate
        }

        _uiState.update { it.copy(groupedRecords = grouped) }
    }


    // Các hàm phụ trợ đưa mốc thời gian về đầu ngày/cuối ngày để query cho chuẩn
    private fun setStartOfDay(cal: Calendar): Calendar {
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0)
        return cal
    }
    private fun setEndOfDay(cal: Calendar): Calendar {
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
        return cal
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}