package com.example.quizmark.ui.main.home.history

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizmark.data.model.HistoryRecord
import com.example.quizmark.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val recordId: String = savedStateHandle.get<String>("recordId") ?: ""

    private val _recordDetail = MutableStateFlow<HistoryRecord?>(null)
    val recordDetail = _recordDetail.asStateFlow()

    private val _recordBitmap = MutableStateFlow<Bitmap?>(null)
    val recordBitmap = _recordBitmap.asStateFlow()

    init {
        loadRecordDetail()
    }

    private fun loadRecordDetail() {
        if (recordId.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val recordsList = historyRepository.getAllRecords().firstOrNull()
                val targetRecord = recordsList?.find { it.id == recordId }

                if (targetRecord != null) {
                    _recordDetail.value = targetRecord

                    val imgFile = File(targetRecord.localImagePath)

                    if (imgFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                        _recordBitmap.value = bitmap
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}