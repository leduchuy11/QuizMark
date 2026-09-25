package com.example.quizmark.ui.main.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizmark.data.model.ExamModel
import com.example.quizmark.data.model.ThptExamModel
import com.example.quizmark.data.repository.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val repository: ExamRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveResult = MutableStateFlow<Result<Unit>?>(null)
    val saveResult: StateFlow<Result<Unit>?> = _saveResult.asStateFlow()

    fun saveBasicExam(exam: ExamModel) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.saveBasicExam(exam)
            _saveResult.value = result
            _isLoading.value = false
        }
    }

    fun saveThptExam(exam: ThptExamModel) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.saveThptExam(exam)
            _saveResult.value = result
            _isLoading.value = false
        }
    }

    fun resetSaveResult() {
        _saveResult.value = null
    }
}