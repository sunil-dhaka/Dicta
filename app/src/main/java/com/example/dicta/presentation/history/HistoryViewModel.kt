package com.example.dicta.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dicta.domain.model.Recording
import com.example.dicta.domain.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val recordings: List<Recording> = emptyList(),
    val isLoading: Boolean = true,
    val selectedRecording: Recording? = null,
    val error: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadRecordings()
    }

    private fun loadRecordings() {
        viewModelScope.launch {
            recordingRepository.getAllRecordings().collect { recordings ->
                _uiState.update { it.copy(recordings = recordings, isLoading = false) }
            }
        }
    }

    fun selectRecording(recording: Recording) {
        _uiState.update { it.copy(selectedRecording = recording) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedRecording = null) }
    }

    fun deleteRecording(recording: Recording) {
        viewModelScope.launch {
            try {
                recordingRepository.deleteRecording(recording.id)
                _uiState.update { it.copy(selectedRecording = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
