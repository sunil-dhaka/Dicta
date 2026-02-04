package com.example.dicta.audio

import kotlinx.coroutines.flow.Flow

interface AudioRecorder {
    val audioData: Flow<ShortArray>
    val isRecording: Flow<Boolean>

    fun startRecording()
    fun stopRecording()
    fun release()
}
