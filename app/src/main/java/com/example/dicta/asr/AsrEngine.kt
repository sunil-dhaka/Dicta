package com.example.dicta.asr

import com.example.dicta.domain.model.TranscriptionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AsrEngine {
    val state: StateFlow<AsrState>
    val transcriptionResults: Flow<TranscriptionResult>

    suspend fun initialize(modelPath: String, modelArch: Int)
    suspend fun startListening()
    suspend fun stopListening()
    fun processAudioData(audioData: ShortArray)
    suspend fun transcribeFile(filePath: String): String
    fun release()
}
