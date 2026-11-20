package com.example.quizmark.ui.main.home.statistics

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizmark.data.model.HistoryRecord
import com.example.quizmark.data.model.RosterModel
import com.example.quizmark.data.model.StudentModel
import com.example.quizmark.data.repository.ExamListRepository
import com.example.quizmark.data.repository.HistoryRepository
import com.example.quizmark.util.DriveBatchUploadSummary
import com.example.quizmark.util.DuplicateSbdCluster
import com.example.quizmark.util.DuplicateSbdClusterResolver
import com.example.quizmark.util.GoogleDriveHelper
import com.example.quizmark.util.MissingStudentSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DriveUploadState {
    object Idle : DriveUploadState()
    data class Uploading(val current: Int, val total: Int) : DriveUploadState()
    data class Success(val summary: DriveBatchUploadSummary) : DriveUploadState()
    data class Error(val message: String) : DriveUploadState()
}

@HiltViewModel
class ScoreListViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val examListRepository: ExamListRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Lấy rosterId được truyền sang từ Navigation
    val rosterId: String = savedStateHandle.get<String>("rosterId") ?: "ALL"

    private val _records = MutableStateFlow<List<HistoryRecord>>(emptyList())
    val records = _records.asStateFlow()

    private val _duplicateClusters = MutableStateFlow<List<DuplicateSbdCluster>>(emptyList())
    val duplicateClusters = _duplicateClusters.asStateFlow()

    private val _currentRoster = MutableStateFlow<RosterModel?>(null)
    val currentRoster = _currentRoster.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _driveUploadState = MutableStateFlow<DriveUploadState>(DriveUploadState.Idle)
    val driveUploadState = _driveUploadState.asStateFlow()

    init {
        loadRosterIfSpecific()
        loadScores()
    }

    private fun loadRosterIfSpecific() {
        if (rosterId != "ALL" && rosterId != "NONE") {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    _currentRoster.value = examListRepository.getRosterById(rosterId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
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

                    // Sắp xếp theo SBD rồi theo tên
                    val sorted = filtered.sortedWith(
                        compareBy<HistoryRecord> { it.studentCode }.thenBy { it.studentName }
                    )

                    _records.value = sorted
                    _duplicateClusters.value = DuplicateSbdClusterResolver.buildDuplicateClusters(sorted)
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    /**
     * Trả về danh sách học sinh trong Roster hiện tại CHƯA CÓ ĐIỂM,
     * ưu tiên những em có SBD lệch 1 chữ số so với [duplicatedSbd].
     */
    fun getMissingStudentSuggestions(duplicatedSbd: String): List<MissingStudentSuggestion> {
        val rosterStudents = _currentRoster.value?.students ?: emptyList()
        return DuplicateSbdClusterResolver.suggestMissingStudents(
            rosterStudents = rosterStudents,
            records = _records.value,
            duplicatedSbd = duplicatedSbd
        )
    }

    /**
     * TRƯỜNG HỢP 1: Giáo viên chọn "Giữ bài này" cho SBD đang bị trùng
     * -> Giữ lại [recordToKeep] và xóa các bản ghi dư thừa còn lại trong cùng cụm [cluster].
     */
    fun keepOnlyThisRecordInCluster(cluster: DuplicateSbdCluster, recordToKeep: HistoryRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            cluster.records
                .filter { it.id != recordToKeep.id }
                .forEach { duplicateToDelete ->
                    historyRepository.deleteRecord(context, duplicateToDelete.id)
                }
        }
    }

    /**
     * TRƯỜNG HỢP 2: Giáo viên gán bài bị tô nhầm sang một học sinh khác trong Roster (hoặc nhập SBD mới).
     */
    fun reassignRecordToStudent(record: HistoryRecord, newStudentCode: String, newStudentName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanCode = newStudentCode.trim()
            if (cleanCode.isEmpty()) return@launch

            // Nếu giáo viên chỉ gõ SBD mà không truyền tên, tự động dò tên trong Roster
            val resolvedName = newStudentName.trim().ifBlank {
                _currentRoster.value?.students
                    ?.find { it.studentCode.trim() == cleanCode }
                    ?.name
                    ?: record.studentName
            }

            historyRepository.updateRecordStudent(
                context = context,
                record = record,
                newStudentCode = cleanCode,
                newStudentName = resolvedName
            )
        }
    }

    /**
     * Xóa lẻ 1 bản ghi trong cụm bài trùng
     */
    fun deleteDuplicateRecord(recordId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            historyRepository.deleteRecord(context, recordId)
        }
    }

    /**
     * Đẩy toàn bộ ảnh bài chấm của danh sách hiện tại lên Google Drive
     */
    fun uploadCurrentRecordsToDrive() {
        val currentList = _records.value
        if (currentList.isEmpty()) return

        val folderTitle = _currentRoster.value?.name
            ?: currentList.firstOrNull { it.rosterId != "anonymous" }?.rosterName
            ?: "BaiCham_QuizMark"

        viewModelScope.launch(Dispatchers.IO) {
            _driveUploadState.value = DriveUploadState.Uploading(0, currentList.size)
            val result = GoogleDriveHelper.uploadRecordsToDrive(
                context = context,
                rosterName = folderTitle,
                records = currentList,
                onProgress = { current, total ->
                    _driveUploadState.value = DriveUploadState.Uploading(current, total)
                }
            )

            result.onSuccess { summary ->
                // Lưu lại driveFileId và driveWebViewLink vào từng HistoryRecord trên Firestore
                summary.uploadedRecords.forEach { uploaded ->
                    historyRepository.updateRecordDriveInfo(
                        context = context,
                        record = uploaded.record,
                        driveFileId = uploaded.driveFileId,
                        driveFolderId = uploaded.driveFolderId,
                        driveWebViewLink = uploaded.driveWebViewLink
                    )
                }
                _driveUploadState.value = DriveUploadState.Success(summary)
            }.onFailure { err ->
                _driveUploadState.value = DriveUploadState.Error(
                    err.localizedMessage ?: "Không thể tải ảnh lên Google Drive"
                )
            }
        }
    }

    fun resetDriveUploadState() {
        _driveUploadState.value = DriveUploadState.Idle
    }
}
