package com.example.quizmark.ui.main.examList

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizmark.data.model.ClassroomModel
import com.example.quizmark.data.model.HistoryRecord
import com.example.quizmark.data.model.RosterModel
import com.example.quizmark.data.model.StudentModel
import com.example.quizmark.data.repository.ExamListRepository
import com.example.quizmark.data.repository.HistoryRepository
import com.example.quizmark.util.DuplicateSbdCluster
import com.example.quizmark.util.GoogleDriveHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExamListViewModel @Inject constructor(
    private val repository: ExamListRepository,
    private val historyRepository: HistoryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveResult = MutableStateFlow<Result<Unit>?>(null)
    val saveResult: StateFlow<Result<Unit>?> = _saveResult.asStateFlow()

    val rosterList: StateFlow<List<RosterModel>> = repository.getAllRostersFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val classroomList: StateFlow<List<ClassroomModel>> = repository.getAllClassroomsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allHistoryRecords: StateFlow<List<HistoryRecord>> = historyRepository.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentRoster = MutableStateFlow<RosterModel?>(null)
    val currentRoster: StateFlow<RosterModel?> = _currentRoster.asStateFlow()

    private val _currentClassroom = MutableStateFlow<ClassroomModel?>(null)
    val currentClassroom: StateFlow<ClassroomModel?> = _currentClassroom.asStateFlow()

    val allStudentsList: StateFlow<List<StudentModel>> = repository.getAllStudentsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun saveRoster(roster: RosterModel) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.saveRoster(roster)
            _saveResult.value = result
            _isLoading.value = false
        }
    }

    fun deleteRoster(rosterId: String) {
        repository.deleteRoster(rosterId)
    }

    fun loadRoster(rosterId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val roster = repository.getRosterById(rosterId)
            _currentRoster.value = roster
            _isLoading.value = false
        }
    }

    fun saveStudent(student: StudentModel, roster: RosterModel?) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.saveStudent(student, roster)
            _saveResult.value = result
            _isLoading.value = false
        }
    }

    fun updateStudent(student: StudentModel, initialRoster: RosterModel?, newRoster: RosterModel?) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.updateStudent(student, initialRoster, newRoster)
            _saveResult.value = result
            _isLoading.value = false
        }
    }

    fun deleteStudent(studentId: String, initialRoster: RosterModel?) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteStudent(studentId, initialRoster)
            _saveResult.value = result
            _isLoading.value = false
        }
    }

    fun importStudents(importedStudents: List<StudentModel>, rosterId: String?) {
        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.importStudents(
                importedStudents = importedStudents,
                rosterId = rosterId,
                currentAllStudents = allStudentsList.value,
                rosterList = rosterList.value
            )

            _saveResult.value = result
            _isLoading.value = false
        }
    }

    fun resetSaveResult() {
        _saveResult.value = null
    }

    // quản lí lớp học và lịch sử điểm
    fun saveClassroom(classroom: ClassroomModel) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.saveClassroom(classroom)
            _saveResult.value = result
            _isLoading.value = false
        }
    }

    fun deleteClassroom(classroom: ClassroomModel) {
        repository.deleteClassroom(classroom)
    }

    fun loadClassroom(classId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val classroom = repository.getClassroomById(classId)
            _currentClassroom.value = classroom
            _isLoading.value = false
        }
    }

    fun removeStudentFromClassroom(student: StudentModel, classroom: ClassroomModel) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.removeStudentFromClassroom(student, classroom)
            _saveResult.value = result
            _isLoading.value = false
        }
    }

    fun saveStudentToClassroom(student: StudentModel, classroom: ClassroomModel?) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.saveStudentToClassroom(student, classroom)
            _saveResult.value = result
            _isLoading.value = false
        }
    }

    fun updateStudentInClassroom(
        student: StudentModel,
        initialClassroom: ClassroomModel?,
        newClassroom: ClassroomModel?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.updateStudentInClassroom(student, initialClassroom, newClassroom)
            _saveResult.value = result
            _isLoading.value = false
        }
    }

    fun deleteStudentFromClassroom(studentId: String, initialClassroom: ClassroomModel?) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteStudentFromClassroom(studentId, initialClassroom)
            _saveResult.value = result
            _isLoading.value = false
        }
    }

    fun importStudentsToClassroom(importedStudents: List<StudentModel>, classroomId: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.importStudentsToClassroom(
                importedStudents = importedStudents,
                classroomId = classroomId,
                classroomList = classroomList.value
            )
            _saveResult.value = result
            _isLoading.value = false
        }
    }

    fun deleteHistoryRecord(recordId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            historyRepository.deleteRecord(context, recordId)
        }
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

    fun reassignRecordInClassroom(
        classroom: ClassroomModel,
        record: HistoryRecord,
        newStudentCode: String,
        newStudentName: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanCode = newStudentCode.trim()
            if (cleanCode.isEmpty()) return@launch
            val matchedInClass = classroom.students.find { it.studentCode.trim() == cleanCode }
            val resolvedName = newStudentName.trim().ifBlank {
                matchedInClass?.name ?: record.studentName
            }
            historyRepository.updateRecordStudent(
                context = context,
                record = record,
                newStudentCode = cleanCode,
                newStudentName = resolvedName,
                newClassId = classroom.id,
                newClassName = classroom.name
            )
        }
    }

    fun pushClassroomZipToDrive(
        groupTitle: String,
        records: List<HistoryRecord>,
        onFinished: (Result<String>) -> Unit
    ) {
        if (records.isEmpty()) return
        viewModelScope.launch {
            val res = GoogleDriveHelper.exportZipToGoogleDriveScreen(
                context = context,
                groupTitle = groupTitle,
                records = records
            )
            onFinished(
                res.map { zip -> "Đã đóng gói ${zip.name} và mở màn hình Google Drive!" }
            )
        }
    }

    fun pushClassroomImagesToDrive(
        groupTitle: String,
        records: List<HistoryRecord>,
        onFinished: (Result<String>) -> Unit
    ) {
        if (records.isEmpty()) return
        viewModelScope.launch {
            val res = GoogleDriveHelper.exportImagesToGoogleDriveScreen(
                context = context,
                groupTitle = groupTitle,
                records = records
            )
            onFinished(
                res.map { count -> "Đã chuyển $count ảnh bài chấm sang màn hình Google Drive!" }
            )
        }
    }
}
