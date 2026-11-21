package com.example.quizmark.ui.main.home.statistics

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizmark.data.model.ClassroomModel
import com.example.quizmark.data.model.HistoryRecord
import com.example.quizmark.data.model.RosterModel
import com.example.quizmark.data.repository.ExamListRepository
import com.example.quizmark.data.repository.HistoryRepository
import com.example.quizmark.util.DriveBatchUploadSummary
import com.example.quizmark.util.DuplicateSbdCluster
import com.example.quizmark.util.DuplicateSbdClusterResolver
import com.example.quizmark.util.GoogleDriveHelper
import com.example.quizmark.util.MissingStudentSuggestion
import com.example.quizmark.util.ScoreSortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DriveUploadState {
    object Idle : DriveUploadState()
    data class Uploading(val current: Int, val total: Int) : DriveUploadState()
    data class Success(val summary: DriveBatchUploadSummary) : DriveUploadState()
    data class OpenedDriveApp(val message: String) : DriveUploadState()
    data class Error(val message: String) : DriveUploadState()
}

@HiltViewModel
class ScoreListViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val examListRepository: ExamListRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val rosterId: String = savedStateHandle.get<String>("rosterId") ?: "ALL"
    private val initialClassId: String = savedStateHandle.get<String>("classId") ?: "ALL"

    private var allRosterScopedRecords: List<HistoryRecord> = emptyList()
    private var cachedClassrooms: List<ClassroomModel> = emptyList()

    private val _selectedClassId = MutableStateFlow(initialClassId)
    val selectedClassId = _selectedClassId.asStateFlow()

    private val _availableClasses = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val availableClasses = _availableClasses.asStateFlow()

    private val _sortOrder = MutableStateFlow(ScoreSortOrder.SCORE_ASC)
    val sortOrder = _sortOrder.asStateFlow()

    private val _records = MutableStateFlow<List<HistoryRecord>>(emptyList())
    val records = _records.asStateFlow()

    private val _averageScore = MutableStateFlow(0.0)
    val averageScore = _averageScore.asStateFlow()

    private val _minScore = MutableStateFlow(0.0)
    val minScore = _minScore.asStateFlow()

    private val _maxScore = MutableStateFlow(0.0)
    val maxScore = _maxScore.asStateFlow()

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
                combine(
                    historyRepository.getAllRecords(),
                    examListRepository.getAllRostersFlow(),
                    examListRepository.getAllClassroomsFlow()
                ) { rawRecords, rosters, classrooms ->
                    Triple(rawRecords, rosters, classrooms)
                }.collect { (rawRecords, rosters, classrooms) ->
                    cachedClassrooms = classrooms

                    val enrichedAll = DuplicateSbdClusterResolver.enrichRecordsWithClassInfo(
                        records = rawRecords,
                        rosters = rosters,
                        classrooms = classrooms
                    )

                    val rosterFiltered = when (rosterId) {
                        "ALL" -> enrichedAll
                        "NONE" -> enrichedAll.filter { it.rosterId == "anonymous" }
                        else -> enrichedAll.filter { it.rosterId == rosterId }
                    }

                    allRosterScopedRecords = rosterFiltered

                    val classPairs = linkedMapOf<String, String>()
                    rosterFiltered.forEach { rec ->
                        if (rec.classId.isNotBlank() && rec.className.isNotBlank()) {
                            classPairs[rec.classId] = rec.className
                        }
                    }
                    // Nếu đang lọc theo 1 lớp cụ thể mà lớp đó có trong hệ thống thì luôn thêm vào
                    if (_selectedClassId.value != "ALL" && _selectedClassId.value != "UNASSIGNED") {
                        classrooms.find { it.id == _selectedClassId.value }?.let { cls ->
                            classPairs[cls.id] = cls.name
                        }
                    }
                    _availableClasses.value = classPairs.toList()

                    applyClassAndSortFilter()
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    fun selectClassFilter(classId: String) {
        _selectedClassId.value = classId
        applyClassAndSortFilter()
    }

    fun updateSortOrder(order: ScoreSortOrder) {
        _sortOrder.value = order
        applyClassAndSortFilter()
    }

    private fun applyClassAndSortFilter() {
        val currentClassId = _selectedClassId.value
        val classFiltered = when (currentClassId) {
            "ALL" -> allRosterScopedRecords
            "UNASSIGNED" -> allRosterScopedRecords.filter { it.classId.isBlank() }
            else -> allRosterScopedRecords.filter { it.classId == currentClassId }
        }

        val sorted = DuplicateSbdClusterResolver.sortRecords(classFiltered, _sortOrder.value)
        _records.value = sorted
        _duplicateClusters.value = DuplicateSbdClusterResolver.buildDuplicateClusters(classFiltered)

        if (sorted.isNotEmpty()) {
            _averageScore.value = sorted.sumOf { it.score } / sorted.size
            _minScore.value = sorted.minOf { it.score }
            _maxScore.value = sorted.maxOf { it.score }
        } else {
            _averageScore.value = 0.0
            _minScore.value = 0.0
            _maxScore.value = 0.0
        }
    }

    fun getCurrentGroupTitle(): String {
        val classId = _selectedClassId.value
        val className = if (classId != "ALL" && classId != "UNASSIGNED") {
            _availableClasses.value.find { it.first == classId }?.second
                ?: cachedClassrooms.find { it.id == classId }?.name
        } else null

        val rosterTitle = _currentRoster.value?.name
            ?: _records.value.firstOrNull { it.rosterId != "anonymous" }?.rosterName

        return when {
            !className.isNullOrBlank() && !rosterTitle.isNullOrBlank() -> "Lop_${className}_$rosterTitle"
            !className.isNullOrBlank() -> "Lop_$className"
            !rosterTitle.isNullOrBlank() -> rosterTitle
            else -> "BaiCham_QuizMark"
        }
    }

    fun getMissingStudentSuggestions(duplicatedSbd: String): List<MissingStudentSuggestion> {
        val rosterStudents = _currentRoster.value?.students
            ?: cachedClassrooms.find { it.id == _selectedClassId.value }?.students
            ?: emptyList()
        return DuplicateSbdClusterResolver.suggestMissingStudents(
            rosterStudents = rosterStudents,
            records = allRosterScopedRecords,
            duplicatedSbd = duplicatedSbd
        )
    }

    fun keepOnlyThisRecordInCluster(cluster: DuplicateSbdCluster, recordToKeep: HistoryRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            cluster.records
                .filter { it.id != recordToKeep.id }
                .forEach { duplicateToDelete ->
                    historyRepository.deleteRecord(context, duplicateToDelete.id)
                }
        }
    }

    fun reassignRecordToStudent(record: HistoryRecord, newStudentCode: String, newStudentName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanCode = newStudentCode.trim()
            if (cleanCode.isEmpty()) return@launch

            val matchedFromRoster = _currentRoster.value?.students?.find { it.studentCode.trim() == cleanCode }
            val matchedFromClass = cachedClassrooms.firstNotNullOfOrNull { cls ->
                cls.students.find { it.studentCode.trim() == cleanCode }?.copy(
                    classId = cls.id,
                    className = cls.name
                )
            }

            val resolvedName = newStudentName.trim().ifBlank {
                matchedFromRoster?.name ?: matchedFromClass?.name ?: record.studentName
            }
            val resolvedClassId = matchedFromRoster?.classId?.ifBlank { matchedFromClass?.classId.orEmpty() }
                ?: matchedFromClass?.classId
                ?: record.classId
            val resolvedClassName = matchedFromRoster?.className?.ifBlank { matchedFromClass?.className.orEmpty() }
                ?: matchedFromClass?.className
                ?: record.className

            historyRepository.updateRecordStudent(
                context = context,
                record = record,
                newStudentCode = cleanCode,
                newStudentName = resolvedName,
                newClassId = resolvedClassId,
                newClassName = resolvedClassName
            )
        }
    }

    fun deleteDuplicateRecord(recordId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            historyRepository.deleteRecord(context, recordId)
        }
    }

    /**
     * Đóng gói toàn bộ ảnh bài chấm + bảng điểm thành 1 tệp .zip và chuyển tiếp thẳng sang màn hình Google Drive.
     */
    fun pushZipPackageToGoogleDriveScreen() {
        val currentList = _records.value
        if (currentList.isEmpty()) return
        val groupTitle = getCurrentGroupTitle()

        viewModelScope.launch {
            _driveUploadState.value = DriveUploadState.Uploading(1, 1)
            val result = GoogleDriveHelper.exportZipToGoogleDriveScreen(
                context = context,
                groupTitle = groupTitle,
                records = currentList
            )
            result.onSuccess { zipFile ->
                _driveUploadState.value = DriveUploadState.OpenedDriveApp(
                    "Đã tạo tệp ${zipFile.name} (${currentList.size} bài) và mở màn hình Google Drive để lưu!"
                )
            }.onFailure { err ->
                _driveUploadState.value = DriveUploadState.Error(
                    err.localizedMessage ?: "Không thể tạo tệp nén để đẩy lên Google Drive"
                )
            }
        }
    }

    /**
     * Đẩy trực tiếp danh sách từng tệp ảnh (.jpg) sang màn hình "Lưu vào Drive" của ứng dụng Google Drive.
     */
    fun pushImagesToGoogleDriveScreen() {
        val currentList = _records.value
        if (currentList.isEmpty()) return
        val groupTitle = getCurrentGroupTitle()

        viewModelScope.launch {
            _driveUploadState.value = DriveUploadState.Uploading(1, currentList.size)
            val result = GoogleDriveHelper.exportImagesToGoogleDriveScreen(
                context = context,
                groupTitle = groupTitle,
                records = currentList
            )
            result.onSuccess { count ->
                _driveUploadState.value = DriveUploadState.OpenedDriveApp(
                    "Đã chuyển $count ảnh bài chấm sang màn hình Google Drive để lưu!"
                )
            }.onFailure { err ->
                _driveUploadState.value = DriveUploadState.Error(
                    err.localizedMessage ?: "Không thể chuyển ảnh sang Google Drive"
                )
            }
        }
    }

    fun resetDriveUploadState() {
        _driveUploadState.value = DriveUploadState.Idle
    }
}
