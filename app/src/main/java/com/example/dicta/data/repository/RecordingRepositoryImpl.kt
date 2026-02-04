package com.example.dicta.data.repository

import com.example.dicta.data.local.dao.RecordingDao
import com.example.dicta.data.local.entity.RecordingEntity
import com.example.dicta.domain.model.Recording
import com.example.dicta.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecordingRepositoryImpl @Inject constructor(
    private val recordingDao: RecordingDao
) : RecordingRepository {

    override fun getAllRecordings(): Flow<List<Recording>> =
        recordingDao.getAllRecordings().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getRecordingById(id: Long): Recording? =
        recordingDao.getRecordingById(id)?.toDomain()

    override suspend fun saveRecording(recording: Recording): Long =
        recordingDao.insertRecording(RecordingEntity.fromDomain(recording))

    override suspend fun updateRecording(recording: Recording) =
        recordingDao.updateRecording(RecordingEntity.fromDomain(recording))

    override suspend fun deleteRecording(id: Long) =
        recordingDao.deleteRecordingById(id)

    override suspend fun deleteAllRecordings() =
        recordingDao.deleteAllRecordings()
}
