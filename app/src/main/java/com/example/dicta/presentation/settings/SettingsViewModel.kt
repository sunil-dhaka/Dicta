package com.example.dicta.presentation.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dicta.data.preferences.UserPreferences
import com.example.dicta.data.repository.ModelRepositoryImpl
import com.example.dicta.domain.model.AsrModel
import com.example.dicta.domain.model.AsrModelType
import com.example.dicta.domain.repository.RecordingRepository
import com.example.dicta.util.AudioUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

data class ModelState(
    val model: AsrModel,
    val isDownloaded: Boolean,
    val isSelected: Boolean,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f
)

data class SettingsUiState(
    val models: List<ModelState> = emptyList(),
    val selectedModel: AsrModelType = AsrModelType.VOSK_MEDIUM_EN_US,
    val downloadingModel: AsrModelType? = null,
    val downloadProgress: Float = 0f,
    val error: String? = null,
    val clearDataSuccess: Boolean = false,
    val modelSwitched: Boolean = false,
    val isExporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val recordingCount: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelRepository: ModelRepositoryImpl,
    private val recordingRepository: RecordingRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadRecordingCount()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val selectedModel = userPreferences.selectedModel.first()
            val downloadedModels = modelRepository.getDownloadedModels()
            val allModels = modelRepository.getAvailableModels()

            val modelStates = allModels.map { model ->
                ModelState(
                    model = model,
                    isDownloaded = downloadedModels.contains(model.type),
                    isSelected = model.type == selectedModel
                )
            }

            _uiState.update {
                it.copy(
                    models = modelStates,
                    selectedModel = selectedModel
                )
            }
        }
    }

    private fun loadRecordingCount() {
        viewModelScope.launch {
            val recordings = recordingRepository.getAllRecordings().first()
            _uiState.update { it.copy(recordingCount = recordings.size) }
        }
    }

    fun downloadModel(modelType: AsrModelType) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    downloadingModel = modelType,
                    downloadProgress = 0f,
                    models = state.models.map {
                        if (it.model.type == modelType) {
                            it.copy(isDownloading = true, downloadProgress = 0f)
                        } else it
                    }
                )
            }

            val result = modelRepository.downloadModel(modelType) { progress ->
                _uiState.update { state ->
                    state.copy(
                        downloadProgress = progress,
                        models = state.models.map {
                            if (it.model.type == modelType) {
                                it.copy(downloadProgress = progress)
                            } else it
                        }
                    )
                }
            }

            result.fold(
                onSuccess = {
                    loadSettings()
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            downloadingModel = null,
                            error = "Download failed: ${error.message}",
                            models = state.models.map {
                                if (it.model.type == modelType) {
                                    it.copy(isDownloading = false, downloadProgress = 0f)
                                } else it
                            }
                        )
                    }
                }
            )
        }
    }

    fun selectModel(modelType: AsrModelType) {
        viewModelScope.launch {
            userPreferences.setSelectedModel(modelType)
            _uiState.update { state ->
                state.copy(
                    selectedModel = modelType,
                    modelSwitched = true,
                    models = state.models.map {
                        it.copy(isSelected = it.model.type == modelType)
                    }
                )
            }
        }
    }

    fun deleteModel(modelType: AsrModelType) {
        viewModelScope.launch {
            val result = modelRepository.deleteModel(modelType)

            result.fold(
                onSuccess = {
                    loadSettings()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = "Failed to delete: ${error.message}")
                    }
                }
            )
        }
    }

    fun exportAllRecordings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }

            try {
                val recordings = recordingRepository.getAllRecordings().first()

                if (recordings.isEmpty()) {
                    _uiState.update { it.copy(isExporting = false, error = "No recordings to export") }
                    return@launch
                }

                val exportFile = withContext(Dispatchers.IO) {
                    createExportZip(recordings)
                }

                shareFile(exportFile)

                _uiState.update { it.copy(isExporting = false, exportSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isExporting = false, error = "Export failed: ${e.message}")
                }
            }
        }
    }

    private fun createExportZip(recordings: List<com.example.dicta.domain.model.Recording>): File {
        val exportDir = File(context.cacheDir, "export")
        exportDir.mkdirs()

        val zipFile = File(exportDir, "dicta_export_${System.currentTimeMillis()}.zip")
        val formatter = DateTimeFormatter.ISO_INSTANT

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            val jsonArray = JSONArray()

            recordings.forEachIndexed { index, recording ->
                val jsonObj = JSONObject().apply {
                    put("id", recording.id)
                    put("title", recording.title)
                    put("transcription", recording.transcription)
                    put("durationMs", recording.durationMs)
                    put("createdAt", formatter.format(recording.createdAt))
                    put("modelUsed", recording.modelUsed)

                    recording.audioFilePath?.let { audioPath ->
                        val audioFile = File(audioPath)
                        if (audioFile.exists()) {
                            val audioFileName = "audio_${index + 1}_${audioFile.name}"
                            put("audioFile", audioFileName)

                            val entry = ZipEntry(audioFileName)
                            zos.putNextEntry(entry)
                            audioFile.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
                jsonArray.put(jsonObj)
            }

            val metadataJson = JSONObject().apply {
                put("exportedAt", formatter.format(java.time.Instant.now()))
                put("appVersion", "1.0")
                put("recordingCount", recordings.size)
                put("recordings", jsonArray)
            }

            val jsonEntry = ZipEntry("recordings.json")
            zos.putNextEntry(jsonEntry)
            zos.write(metadataJson.toString(2).toByteArray())
            zos.closeEntry()
        }

        return zipFile
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, "Export Recordings").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                recordingRepository.deleteAllRecordings()

                val recordingsDir = AudioUtils.getRecordingsDir(context)
                recordingsDir.deleteRecursively()
                recordingsDir.mkdirs()

                _uiState.update { it.copy(clearDataSuccess = true, recordingCount = 0) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to clear data: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(clearDataSuccess = false) }
    }

    fun clearModelSwitched() {
        _uiState.update { it.copy(modelSwitched = false) }
    }

    fun clearExportSuccess() {
        _uiState.update { it.copy(exportSuccess = false) }
    }
}
