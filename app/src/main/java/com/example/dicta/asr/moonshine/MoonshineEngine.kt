package com.example.dicta.asr.moonshine

import android.content.Context
import android.util.Log
import ai.moonshine.voice.Transcriber
import ai.moonshine.voice.TranscriptEvent
import ai.moonshine.voice.TranscriptEventListener
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
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MoonshineEngine(private val context: Context) : AsrEngine {

    companion object {
        private const val TAG = "MoonshineEngine"
        const val SAMPLE_RATE = 16000
    }

    private var transcriber: Transcriber? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<AsrState>(AsrState.Uninitialized)
    override val state: StateFlow<AsrState> = _state.asStateFlow()

    private val _transcriptionResults = MutableSharedFlow<TranscriptionResult>(
        replay = 1,
        extraBufferCapacity = 10
    )
    override val transcriptionResults: Flow<TranscriptionResult> = _transcriptionResults.asSharedFlow()

    override suspend fun initialize(modelPath: String, modelArch: Int): Unit = withContext(Dispatchers.IO) {
        try {
            _state.value = AsrState.Loading
            Log.d(TAG, "Initializing with model path: $modelPath, arch: $modelArch")

            val t = Transcriber()
            t.loadFromFiles(modelPath, modelArch)
            t.addListener { event ->
                event.accept(object : TranscriptEventListener() {
                    override fun onLineTextChanged(e: TranscriptEvent.LineTextChanged) {
                        scope.launch {
                            _transcriptionResults.emit(TranscriptionResult.Partial(e.line.text))
                        }
                    }

                    override fun onLineCompleted(e: TranscriptEvent.LineCompleted) {
                        scope.launch {
                            _transcriptionResults.emit(TranscriptionResult.Final(e.line.text))
                        }
                    }

                    override fun onError(e: TranscriptEvent.Error) {
                        scope.launch {
                            _transcriptionResults.emit(
                                TranscriptionResult.Error(e.cause.message ?: "Transcription error")
                            )
                        }
                    }
                })
            }

            transcriber = t
            _state.value = AsrState.Ready
            Log.d(TAG, "Moonshine engine ready")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize: ${e.message}", e)
            _state.value = AsrState.Error("Failed to initialize: ${e.message}")
        }
    }

    override suspend fun startListening() {
        if (_state.value != AsrState.Ready) {
            Log.w(TAG, "Cannot start listening, state is: ${_state.value}")
            return
        }
        Log.d(TAG, "Starting listening")
        transcriber?.start()
        _state.value = AsrState.Listening
    }

    override suspend fun stopListening() {
        if (_state.value != AsrState.Listening) {
            Log.w(TAG, "Cannot stop listening, state is: ${_state.value}")
            return
        }
        Log.d(TAG, "Stopping listening")
        transcriber?.stop()
        _state.value = AsrState.Ready
    }

    override fun processAudioData(audioData: ShortArray) {
        if (_state.value != AsrState.Listening) return

        val floatData = FloatArray(audioData.size) { audioData[it].toFloat() / Short.MAX_VALUE }
        transcriber?.addAudio(floatData, SAMPLE_RATE)
    }

    override suspend fun transcribeFile(filePath: String): String = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            _transcriptionResults.emit(TranscriptionResult.Error("File not found: $filePath"))
            return@withContext ""
        }

        try {
            val shorts = readWavFile(file)
            val floatData = FloatArray(shorts.size) { shorts[it].toFloat() / Short.MAX_VALUE }
            val transcript = transcriber?.transcribeWithoutStreaming(floatData, SAMPLE_RATE)
            transcript?.lines?.joinToString(" ") { it.text } ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            _transcriptionResults.emit(TranscriptionResult.Error("Transcription failed: ${e.message}"))
            ""
        }
    }

    override fun release() {
        Log.d(TAG, "Releasing resources")
        transcriber = null
        _state.value = AsrState.Uninitialized
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
