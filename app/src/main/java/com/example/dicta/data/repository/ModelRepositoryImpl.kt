package com.example.dicta.data.repository

import android.content.Context
import android.util.Log
import com.example.dicta.data.preferences.UserPreferences
import com.example.dicta.domain.model.AsrModel
import com.example.dicta.domain.model.AsrModelType
import com.example.dicta.domain.repository.ModelRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import javax.inject.Inject

class ModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
) : ModelRepository {

    companion object {
        private const val TAG = "ModelRepositoryImpl"
    }

    private val modelsDir: File
        get() = File(context.filesDir, "models").also { it.mkdirs() }

    override fun getAvailableModels(): List<AsrModel> = AsrModel.getAll()

    override fun isModelDownloaded(modelType: AsrModelType): Flow<Boolean> = flow {
        val dir = getModelDirectory(modelType)
        val exists = dir.exists() && hasModelFiles(dir)
        Log.d(TAG, "isModelDownloaded: $modelType -> $exists")
        emit(exists)
    }

    override fun getModelPath(modelType: AsrModelType): String? {
        val modelDir = getModelDirectory(modelType)
        Log.d(TAG, "getModelPath for $modelType: ${modelDir.absolutePath}")

        if (!modelDir.exists()) {
            Log.e(TAG, "Model directory does not exist: ${modelDir.absolutePath}")
            return null
        }

        if (!hasModelFiles(modelDir)) {
            Log.e(TAG, "Model directory exists but no model files found")
            return null
        }

        return modelDir.absolutePath
    }

    fun isAnyModelDownloaded(): Boolean {
        return AsrModel.getAll().any { model ->
            val dir = getModelDirectory(model.type)
            dir.exists() && hasModelFiles(dir)
        }
    }

    fun getDownloadedModels(): List<AsrModelType> {
        return AsrModel.getAll()
            .filter { model ->
                val dir = getModelDirectory(model.type)
                dir.exists() && hasModelFiles(dir)
            }
            .map { it.type }
    }

    private fun hasModelFiles(dir: File): Boolean {
        if (!dir.exists() || !dir.isDirectory) return false
        val files = dir.listFiles() ?: return false
        val fileNames = files.map { it.name }.toSet()
        // Streaming models: 7 files including encoder.ort, decoder_kv.ort, frontend.ort
        // Base model: 3 files including encoder_model.ort, decoder_model_merged.ort
        val isStreaming = fileNames.containsAll(
            listOf("encoder.ort", "decoder_kv.ort", "frontend.ort", "tokenizer.bin")
        )
        val isBase = fileNames.containsAll(
            listOf("encoder_model.ort", "decoder_model_merged.ort", "tokenizer.bin")
        )
        return isStreaming || isBase
    }

    override suspend fun downloadModel(
        modelType: AsrModelType,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val model = AsrModel.getByType(modelType)
                ?: return@withContext Result.failure(IllegalArgumentException("Unknown model type"))

            Log.d(TAG, "Starting download: ${model.displayName} from ${model.downloadUrl}")

            val tempFile = File(context.cacheDir, "model_download_${modelType.name}.zip")
            downloadFile(model.downloadUrl, tempFile, model.sizeBytes, onProgress)

            Log.d(TAG, "Download complete, size: ${tempFile.length()}")

            val modelDir = getModelDirectory(modelType)
            modelDir.deleteRecursively()
            modelDir.mkdirs()

            Log.d(TAG, "Extracting to: ${modelDir.absolutePath}")
            extractZip(tempFile, modelDir)
            tempFile.delete()

            Log.d(TAG, "Extraction complete for ${model.displayName}")

            userPreferences.setModelDownloaded(true)
            userPreferences.setSelectedModel(modelType)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteModel(modelType: AsrModelType): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val modelDir = getModelDirectory(modelType)
                modelDir.deleteRecursively()
                Log.d(TAG, "Deleted model: $modelType")

                if (!isAnyModelDownloaded()) {
                    userPreferences.setModelDownloaded(false)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun getModelDirectory(modelType: AsrModelType): File =
        File(modelsDir, modelType.name.lowercase())

    private fun downloadFile(
        urlString: String,
        destination: File,
        expectedSize: Long,
        onProgress: (Float) -> Unit
    ) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 60_000
        connection.readTimeout = 60_000

        val actualSize = connection.contentLengthLong.takeIf { it > 0 } ?: expectedSize

        connection.inputStream.use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    onProgress(totalBytesRead.toFloat() / actualSize)
                }
            }
        }
    }

    private fun extractZip(zipFile: File, destinationDir: File) {
        ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zis ->
            var entry = zis.nextEntry
            var count = 0
            while (entry != null) {
                val newFile = File(destinationDir, entry.name)

                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                count++
                if (count % 100 == 0) {
                    Log.d(TAG, "Extracted $count files...")
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            Log.d(TAG, "Total files extracted: $count")
        }
    }
}
