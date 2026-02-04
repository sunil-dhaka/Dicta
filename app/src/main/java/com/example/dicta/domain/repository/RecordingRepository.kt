package com.example.dicta.domain.repository

import com.example.dicta.domain.model.Recording
import kotlinx.coroutines.flow.Flow

interface RecordingRepository {
    fun getAllRecordings(): Flow<List<Recording>>
    suspend fun getRecordingById(id: Long): Recording?
    suspend fun saveRecording(recording: Recording): Long
    suspend fun updateRecording(recording: Recording)
    suspend fun deleteRecording(id: Long)
    suspend fun deleteAllRecordings()
}
