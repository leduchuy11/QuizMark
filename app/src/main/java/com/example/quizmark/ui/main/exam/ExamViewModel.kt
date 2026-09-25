package com.example.quizmark.ui.main.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizmark.data.model.ExamModel
import com.example.quizmark.data.model.ExamUiModel
import com.example.quizmark.data.model.ThptExamModel
import com.example.quizmark.data.repository.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val repository: ExamRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveResult = MutableStateFlow<Result<Unit>?>(null)
    val saveResult: StateFlow<Result<Unit>?> = _saveResult.asStateFlow()

    private val _editingBasicExam = MutableStateFlow<ExamModel?>(null)
    val editingBasicExam: StateFlow<ExamModel?> = _editingBasicExam.asStateFlow()

    // 2. Gộp 2 bảng dữ liệu, map về chung 1 Model và sắp xếp thời gian
    val examList: StateFlow<List<ExamUiModel>> = combine(
        repository.getAllBasicExamsFlow(),
        repository.getAllThptExamsFlow()
    ) { basicList, thptList ->

        // Map Basic
        val mappedBasic = basicList.map { exam ->
            ExamUiModel(
                id = exam.id,
                name = exam.name,
                templateId = exam.templateId,
                questionCount = exam.questionCount,
                isThpt = false,
                codes = exam.codes.map { it.code },
                createdAt = exam.createdAt
            )
        }

        // Map THPT (Cộng tổng số câu 3 phần)
        val mappedThpt = thptList.map { thpt ->
            val totalQ = thpt.config.p1Questions + thpt.config.p2Questions + thpt.config.p3Questions
            ExamUiModel(
                id = thpt.id,
                name = thpt.name,
                templateId = thpt.templateId,
                questionCount = totalQ,
                isThpt = true,
                codes = thpt.codes.map { it.code },
                createdAt = thpt.createdAt
            )
        }

        // Trộn lại và xếp mới nhất lên trên
        (mappedBasic + mappedThpt).sortedByDescending { it.createdAt }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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

    fun deleteExam(examId: String, isThpt: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteExam(examId, isThpt)
            _saveResult.value = result
            _isLoading.value = false
        }
    }



    fun loadBasicExam(examId: String) {
        viewModelScope.launch {
            _editingBasicExam.value = repository.getBasicExamById(examId)
        }
    }

    fun resetEditingExam() {
        _editingBasicExam.value = null
    }

    fun resetSaveResult() {
        _saveResult.value = null
    }
}