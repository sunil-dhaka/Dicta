package com.example.dicta.presentation.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dicta.asr.AsrEngine
import com.example.dicta.asr.AsrState
import com.example.dicta.audio.AudioRecorder
import com.example.dicta.data.preferences.UserPreferences
import com.example.dicta.domain.model.Recording
import com.example.dicta.domain.model.TranscriptionResult
import com.example.dicta.domain.repository.ModelRepository
import com.example.dicta.domain.repository.RecordingRepository
import com.example.dicta.util.AudioUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class HomeUiState(
    val isRecording: Boolean = false,
    val transcription: String = "",
    val partialText: String = "",
    val asrState: AsrState = AsrState.Uninitialized,
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val asrEngine: AsrEngine,
    private val audioRecorder: AudioRecorder,
    private val modelRepository: ModelRepository,
    private val recordingRepository: RecordingRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val audioChunks = mutableListOf<ShortArray>()
    private var recordingStartTime: Instant? = null
    private var audioChunkCount = 0

    init {
        initializeAsr()
        observeAsrState()
        observeTranscription()
        observeAudioData()
    }

    private fun initializeAsr() {
        viewModelScope.launch {
            val selectedModel = userPreferences.selectedModel.first()
            val modelPath = modelRepository.getModelPath(selectedModel)

            Log.d(TAG, "Selected model: $selectedModel, path: $modelPath")

            if (modelPath != null) {
                asrEngine.initialize(modelPath, selectedModel.archConstant)
            } else {
                Log.e(TAG, "Model path is null!")
                _uiState.update { it.copy(error = "Model not found. Please download it first.") }
            }
        }
    }

    private fun observeAsrState() {
        viewModelScope.launch {
            asrEngine.state.collect { state ->
                Log.d(TAG, "ASR state changed: $state")
                _uiState.update { it.copy(asrState = state) }

                if (state is AsrState.Error) {
                    _uiState.update { it.copy(error = state.message) }
                }
            }
        }
    }

    private fun observeTranscription() {
        viewModelScope.launch {
            asrEngine.transcriptionResults.collect { result ->
                Log.d(TAG, "Transcription result: $result")
                when (result) {
                    is TranscriptionResult.Partial -> {
                        _uiState.update { it.copy(partialText = result.text) }
                    }
                    is TranscriptionResult.Final -> {
                        _uiState.update { current ->
                            val newTranscription = if (current.transcription.isEmpty()) {
                                result.text
                            } else {
                                "${current.transcription} ${result.text}"
                            }
                            current.copy(
                                transcription = newTranscription.trim(),
                                partialText = ""
                            )
                        }
                    }
                    is TranscriptionResult.Error -> {
                        _uiState.update { it.copy(error = result.message) }
                    }
                }
            }
        }
    }

    private fun observeAudioData() {
        viewModelScope.launch {
            audioRecorder.audioData.collect { data ->
                audioChunks.add(data.copyOf())
                audioChunkCount++
                if (audioChunkCount % 50 == 0) {
                    Log.d(TAG, "Audio chunks received: $audioChunkCount, latest size: ${data.size}")
                }
                asrEngine.processAudioData(data)
            }
        }
    }

    fun toggleRecording() {
        if (_uiState.value.isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        val currentState = _uiState.value.asrState
        Log.d(TAG, "Attempting to start recording, ASR state: $currentState")

        if (currentState != AsrState.Ready) {
            _uiState.update { it.copy(error = "ASR not ready (state: $currentState)") }
            return
        }

        viewModelScope.launch {
            audioChunks.clear()
            audioChunkCount = 0
            recordingStartTime = Instant.now()
            asrEngine.startListening()
            audioRecorder.startRecording()
            _uiState.update { it.copy(isRecording = true, partialText = "") }
            Log.d(TAG, "Recording started")
        }
    }

    private fun stopRecording() {
        Log.d(TAG, "Stopping recording, chunks collected: $audioChunkCount")
        viewModelScope.launch {
            audioRecorder.stopRecording()
            asrEngine.stopListening()
            _uiState.update { it.copy(isRecording = false) }
            Log.d(TAG, "Recording stopped")
        }
    }

    fun saveRecording() {
        viewModelScope.launch {
            if (_uiState.value.transcription.isBlank()) {
                _uiState.update { it.copy(error = "Nothing to save") }
                return@launch
            }

            _uiState.update { it.copy(isSaving = true) }

            try {
                val audioFile = if (audioChunks.isNotEmpty()) {
                    AudioUtils.saveAsWav(context, audioChunks)
                } else {
                    null
                }

                val durationMs = AudioUtils.getDurationMs(audioChunks)
                val selectedModel = userPreferences.selectedModel.first()

                val recording = Recording(
                    title = generateTitle(),
                    transcription = _uiState.value.transcription,
                    audioFilePath = audioFile?.absolutePath,
                    durationMs = durationMs,
                    createdAt = recordingStartTime ?: Instant.now(),
                    modelUsed = selectedModel.name
                )

                recordingRepository.saveRecording(recording)
                Log.d(TAG, "Recording saved: ${recording.title}")

                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save recording", e)
                _uiState.update { it.copy(isSaving = false, error = "Failed to save: ${e.message}") }
            }
        }
    }

    fun clearTranscription() {
        _uiState.update { it.copy(transcription = "", partialText = "", saveSuccess = false) }
        audioChunks.clear()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    private fun generateTitle(): String {
        val time = recordingStartTime ?: Instant.now()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, h:mm a")
            .withZone(java.time.ZoneId.systemDefault())
        return "Recording - ${formatter.format(time)}"
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.release()
        asrEngine.release()
    }
}
