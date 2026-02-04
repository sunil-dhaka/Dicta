package com.example.dicta.domain.repository

import com.example.dicta.domain.model.AsrModel
import com.example.dicta.domain.model.AsrModelType
import kotlinx.coroutines.flow.Flow

interface ModelRepository {
    fun getAvailableModels(): List<AsrModel>
    fun isModelDownloaded(modelType: AsrModelType): Flow<Boolean>
    fun getModelPath(modelType: AsrModelType): String?
    suspend fun downloadModel(modelType: AsrModelType, onProgress: (Float) -> Unit): Result<Unit>
    suspend fun deleteModel(modelType: AsrModelType): Result<Unit>
}
