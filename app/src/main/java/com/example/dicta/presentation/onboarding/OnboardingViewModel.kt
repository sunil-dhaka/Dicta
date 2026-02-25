package com.example.dicta.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dicta.data.preferences.UserPreferences
import com.example.dicta.domain.model.AsrModel
import com.example.dicta.domain.model.AsrModelType
import com.example.dicta.domain.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val availableModels: List<AsrModel> = emptyList(),
    val selectedModel: AsrModelType = AsrModelType.MOONSHINE_SMALL_STREAMING,
    val downloadProgress: Float = 0f,
    val isDownloading: Boolean = false,
    val downloadError: String? = null,
    val isComplete: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadModels()
    }

    private fun loadModels() {
        val models = modelRepository.getAvailableModels()
        _uiState.update { it.copy(availableModels = models) }
    }

    fun selectModel(modelType: AsrModelType) {
        _uiState.update { it.copy(selectedModel = modelType) }
    }

    fun downloadSelectedModel() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, downloadError = null, downloadProgress = 0f) }

            val result = modelRepository.downloadModel(_uiState.value.selectedModel) { progress ->
                _uiState.update { it.copy(downloadProgress = progress) }
            }

            result.fold(
                onSuccess = {
                    userPreferences.setSelectedModel(_uiState.value.selectedModel)
                    userPreferences.setOnboardingCompleted(true)
                    _uiState.update { it.copy(isDownloading = false, isComplete = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDownloading = false,
                            downloadError = error.message ?: "Download failed"
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(downloadError = null) }
    }
}
