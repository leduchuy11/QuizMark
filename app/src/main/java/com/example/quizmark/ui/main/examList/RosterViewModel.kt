package com.example.quizmark.ui.main.examList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizmark.data.model.RosterModel
import com.example.quizmark.data.model.StudentModel
import com.example.quizmark.data.repository.RosterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RosterViewModel @Inject constructor(
    private val repository: RosterRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveResult = MutableStateFlow<Result<Unit>?>(null)
    val saveResult: StateFlow<Result<Unit>?> = _saveResult.asStateFlow()

    val rosterList: StateFlow<List<RosterModel>> = repository.getAllRostersFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentRoster = MutableStateFlow<RosterModel?>(null)
    val currentRoster: StateFlow<RosterModel?> = _currentRoster.asStateFlow()

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
}