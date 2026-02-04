package com.example.dicta.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AndroidAudioRecorder(private val context: Context) : AudioRecorder {

    companion object {
        private const val TAG = "AndroidAudioRecorder"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _audioData = MutableSharedFlow<ShortArray>(
        replay = 0,
        extraBufferCapacity = 64
    )
    override val audioData: Flow<ShortArray> = _audioData.asSharedFlow()

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: Flow<Boolean> = _isRecording.asStateFlow()

    override fun startRecording() {
        if (_isRecording.value) {
            Log.w(TAG, "Already recording")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "RECORD_AUDIO permission not granted")
            return
        }

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
        Log.d(TAG, "Buffer size: $bufferSize")

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            _isRecording.value = true
            Log.d(TAG, "Recording started")

            recordingJob = scope.launch {
                val buffer = ShortArray(bufferSize / 2)
                var chunkCount = 0
                while (isActive && _isRecording.value) {
                    val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readCount > 0) {
                        val chunk = buffer.copyOf(readCount)
                        _audioData.emit(chunk)
                        chunkCount++
                        if (chunkCount % 50 == 0) {
                            Log.d(TAG, "Emitted $chunkCount audio chunks")
                        }
                    } else if (readCount < 0) {
                        Log.e(TAG, "AudioRecord read error: $readCount")
                    }
                }
                Log.d(TAG, "Recording loop ended, total chunks: $chunkCount")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            audioRecord?.release()
            audioRecord = null
        }
    }

    override fun stopRecording() {
        if (!_isRecording.value) {
            Log.w(TAG, "Not recording")
            return
        }

        Log.d(TAG, "Stopping recording")
        _isRecording.value = false
        runBlocking {
            recordingJob?.cancelAndJoin()
        }
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        Log.d(TAG, "Recording stopped")
    }

    override fun release() {
        stopRecording()
    }
}
