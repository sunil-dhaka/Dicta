package com.example.dicta.asr.vosk

import android.content.Context
import android.util.Log
import com.example.dicta.asr.AsrEngine
import com.example.dicta.asr.AsrState
import com.example.dicta.domain.model.TranscriptionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VoskEngine(private val context: Context) : AsrEngine {

    companion object {
        private const val TAG = "VoskEngine"
        const val SAMPLE_RATE = 16000
    }

    private var model: Model? = null
    private var recognizer: Recognizer? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<AsrState>(AsrState.Uninitialized)
    override val state: StateFlow<AsrState> = _state.asStateFlow()

    private val _transcriptionResults = MutableSharedFlow<TranscriptionResult>(
        replay = 1,
        extraBufferCapacity = 10
    )
    override val transcriptionResults: Flow<TranscriptionResult> = _transcriptionResults.asSharedFlow()

    override suspend fun initialize(modelPath: String) = withContext(Dispatchers.IO) {
        try {
            _state.value = AsrState.Loading
            Log.d(TAG, "Initializing with model path: $modelPath")

            val modelDir = findModelDirectory(File(modelPath))
            if (modelDir == null) {
                Log.e(TAG, "Model directory not found at: $modelPath")
                listDirectoryContents(File(modelPath))
                _state.value = AsrState.Error("Model directory not found")
                return@withContext
            }

            Log.d(TAG, "Found model directory: ${modelDir.absolutePath}")

            model = Model(modelDir.absolutePath)
            Log.d(TAG, "Model loaded successfully")

            recognizer = Recognizer(model, SAMPLE_RATE.toFloat())
            Log.d(TAG, "Recognizer created successfully")

            _state.value = AsrState.Ready
            Log.d(TAG, "ASR Engine ready")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize: ${e.message}", e)
            _state.value = AsrState.Error("Failed to initialize: ${e.message}")
        }
    }

    private fun listDirectoryContents(dir: File, indent: String = "") {
        if (!dir.exists()) {
            Log.d(TAG, "${indent}Directory does not exist: ${dir.absolutePath}")
            return
        }
        Log.d(TAG, "${indent}${dir.name}/")
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                listDirectoryContents(file, "$indent  ")
            } else {
                Log.d(TAG, "$indent  ${file.name} (${file.length()} bytes)")
            }
        }
    }

    override suspend fun startListening() {
        if (_state.value != AsrState.Ready) {
            Log.w(TAG, "Cannot start listening, state is: ${_state.value}")
            return
        }
        Log.d(TAG, "Starting listening")
        recognizer?.reset()
        _state.value = AsrState.Listening
    }

    override suspend fun stopListening() {
        if (_state.value != AsrState.Listening) {
            Log.w(TAG, "Cannot stop listening, state is: ${_state.value}")
            return
        }

        Log.d(TAG, "Stopping listening")

        recognizer?.let { rec ->
            val finalResult = rec.finalResult
            Log.d(TAG, "Final result JSON: $finalResult")
            val text = parseResult(finalResult, isFinal = true)
            if (text.isNotBlank()) {
                Log.d(TAG, "Emitting final text: $text")
                _transcriptionResults.emit(TranscriptionResult.Final(text))
            }
        }

        _state.value = AsrState.Ready
    }

    override fun processAudioData(audioData: ShortArray) {
        if (_state.value != AsrState.Listening) return

        recognizer?.let { rec ->
            val bytes = ByteArray(audioData.size * 2)
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(audioData)

            val accepted = rec.acceptWaveForm(bytes, bytes.size)

            if (accepted) {
                val result = rec.result
                val text = parseResult(result, isFinal = true)
                if (text.isNotBlank()) {
                    Log.d(TAG, "Final segment: $text")
                    scope.launch {
                        _transcriptionResults.emit(TranscriptionResult.Final(text))
                    }
                }
            } else {
                val partialResult = rec.partialResult
                val text = parseResult(partialResult, isFinal = false)
                if (text.isNotBlank()) {
                    scope.launch {
                        _transcriptionResults.emit(TranscriptionResult.Partial(text))
                    }
                }
            }
        }
    }

    override suspend fun transcribeFile(filePath: String): String = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            _transcriptionResults.emit(TranscriptionResult.Error("File not found: $filePath"))
            return@withContext ""
        }

        val transcriptionBuilder = StringBuilder()

        try {
            val audioData = readWavFile(file)
            recognizer?.reset()

            val chunkSize = SAMPLE_RATE
            var offset = 0

            while (offset < audioData.size) {
                val end = minOf(offset + chunkSize, audioData.size)
                val chunk = audioData.copyOfRange(offset, end)
                val bytes = ByteArray(chunk.size * 2)
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(chunk)

                if (recognizer?.acceptWaveForm(bytes, bytes.size) == true) {
                    val text = parseResult(recognizer?.result ?: "", isFinal = true)
                    if (text.isNotBlank()) {
                        transcriptionBuilder.append(text).append(" ")
                    }
                }
                offset = end
            }

            val finalText = parseResult(recognizer?.finalResult ?: "", isFinal = true)
            if (finalText.isNotBlank()) {
                transcriptionBuilder.append(finalText)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            _transcriptionResults.emit(TranscriptionResult.Error("Transcription failed: ${e.message}"))
        }

        transcriptionBuilder.toString().trim()
    }

    override fun release() {
        Log.d(TAG, "Releasing resources")
        recognizer?.close()
        model?.close()
        recognizer = null
        model = null
        _state.value = AsrState.Uninitialized
    }

    private fun parseResult(json: String, isFinal: Boolean): String {
        return try {
            val jsonObj = JSONObject(json)
            if (isFinal) {
                jsonObj.optString("text", "")
            } else {
                jsonObj.optString("partial", "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON: $json", e)
            ""
        }
    }

    private fun findModelDirectory(dir: File): File? {
        if (!dir.exists() || !dir.isDirectory) {
            Log.d(TAG, "Not a valid directory: ${dir.absolutePath}")
            return null
        }

        val files = dir.listFiles() ?: return null

        // Check if this directory contains model files
        val hasModelFiles = files.any {
            it.name == "am" ||
            it.name == "conf" ||
            it.name == "graph" ||
            it.name == "ivector" ||
            it.name.endsWith(".mdl")
        }

        if (hasModelFiles) {
            Log.d(TAG, "Found model files in: ${dir.absolutePath}")
            return dir
        }

        // Recursively search subdirectories
        for (child in files) {
            if (child.isDirectory) {
                val found = findModelDirectory(child)
                if (found != null) return found
            }
        }

        return null
    }

    private fun readWavFile(file: File): ShortArray {
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(44)

            val dataSize = (file.length() - 44).toInt()
            val bytes = ByteArray(dataSize)
            raf.readFully(bytes)

            val shorts = ShortArray(dataSize / 2)
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
            return shorts
        }
    }
}
