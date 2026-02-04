package com.example.dicta.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.dicta.data.preferences.UserPreferences
import com.example.dicta.domain.model.AsrModel
import com.example.dicta.domain.model.AsrModelType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val userPreferences: UserPreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelTypeName = inputData.getString(KEY_MODEL_TYPE) ?: return@withContext Result.failure()
        val modelType = AsrModelType.valueOf(modelTypeName)
        val model = AsrModel.getAll().find { it.type == modelType } ?: return@withContext Result.failure()

        try {
            val modelsDir = File(applicationContext.filesDir, "models").also { it.mkdirs() }
            val modelDir = File(modelsDir, modelType.name.lowercase())
            val tempFile = File(applicationContext.cacheDir, "model_download.zip")

            downloadFile(model.downloadUrl, tempFile, model.sizeBytes)

            modelDir.deleteRecursively()
            modelDir.mkdirs()

            extractZip(tempFile, modelDir)
            tempFile.delete()

            userPreferences.setModelDownloaded(true)
            Result.success()
        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        }
    }

    private suspend fun downloadFile(urlString: String, destination: File, expectedSize: Long) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 30_000
        connection.readTimeout = 30_000

        connection.inputStream.use { input ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    val progress = (totalBytesRead.toFloat() / expectedSize * 100).toInt()
                    setProgressAsync(workDataOf(KEY_PROGRESS to progress))
                }
            }
        }
    }

    private fun extractZip(zipFile: File, destinationDir: File) {
        ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zis ->
            var entry = zis.nextEntry
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
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    companion object {
        const val KEY_MODEL_TYPE = "model_type"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"

        fun createInputData(modelType: AsrModelType): Data =
            workDataOf(KEY_MODEL_TYPE to modelType.name)
    }
}
