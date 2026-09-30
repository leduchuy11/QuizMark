package com.example.quizmark.ui.main.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizmark.data.model.ExamUiModel
import com.example.quizmark.data.model.RosterModel
import com.example.quizmark.data.repository.ExamListRepository
import com.example.quizmark.data.repository.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val examListRepository: ExamListRepository,
    private val examRepository: ExamRepository
) : ViewModel() {

    // Lấy danh sách lớp trực tiếp từ Repository
    val rosterList: StateFlow<List<RosterModel>> = examListRepository.getAllRostersFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Gộp 2 luồng và ánh xạ sang ExamUiModel chung
    val examList: StateFlow<List<ExamUiModel>> = combine(
        examRepository.getAllBasicExamsFlow(),
        examRepository.getAllThptExamsFlow()
    ) { basicExams, thptExams ->

        val mappedBasic = basicExams.map {
            ExamUiModel(
                id = it.id,
                name = it.name,
                templateId = it.templateId,
                questionCount = it.questionCount,
                isThpt = false,
                codes = it.codes.map { codeModel -> codeModel.code },
                createdAt = it.createdAt
            )
        }

        val mappedThpt = thptExams.map {
            ExamUiModel(
                id = it.id,
                name = it.name,
                templateId = it.templateId,
                questionCount = 54,
                isThpt = true,
                codes = it.codes.map { codeModel -> codeModel.code },
                createdAt = it.createdAt
            )
        }

        // Gộp lại và ưu tiên những đề thi tạo gần đây nhất lên đầu (giảm thời gian lướt tìm)
        (mappedBasic + mappedThpt).sortedByDescending { it.createdAt }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    // State lưu cấu hình người dùng chọn sử dụng ExamUiModel
    private val _selectedExam = MutableStateFlow<ExamUiModel?>(null)
    val selectedExam: StateFlow<ExamUiModel?> = _selectedExam.asStateFlow()

    private val _selectedRoster = MutableStateFlow<RosterModel?>(null)
    val selectedRoster: StateFlow<RosterModel?> = _selectedRoster.asStateFlow()

    fun selectExam(exam: ExamUiModel) {
        _selectedExam.value = exam
    }

    fun selectRoster(roster: RosterModel?) {
        _selectedRoster.value = roster
    }
}