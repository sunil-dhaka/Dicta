package com.example.dicta.util

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AudioUtils {

    private const val SAMPLE_RATE = 16000
    private const val BITS_PER_SAMPLE = 16
    private const val CHANNELS = 1

    fun getRecordingsDir(context: Context): File =
        File(context.filesDir, "recordings").also { it.mkdirs() }

    fun generateFileName(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneId.systemDefault())
        return "recording_${formatter.format(Instant.now())}.wav"
    }

    fun saveAsWav(context: Context, audioChunks: List<ShortArray>): File {
        val dir = getRecordingsDir(context)
        val file = File(dir, generateFileName())

        val totalSamples = audioChunks.sumOf { it.size }
        val dataSize = totalSamples * 2

        FileOutputStream(file).use { fos ->
            writeWavHeader(fos, dataSize)

            audioChunks.forEach { chunk ->
                val bytes = ByteArray(chunk.size * 2)
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(chunk)
                fos.write(bytes)
            }
        }

        return file
    }

    fun getDurationMs(audioChunks: List<ShortArray>): Long {
        val totalSamples = audioChunks.sumOf { it.size }
        return (totalSamples * 1000L) / SAMPLE_RATE
    }

    private fun writeWavHeader(fos: FileOutputStream, dataSize: Int) {
        val totalSize = dataSize + 36
        val byteRate = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8
        val blockAlign = CHANNELS * BITS_PER_SAMPLE / 8

        fos.write("RIFF".toByteArray())
        fos.write(intToBytes(totalSize))
        fos.write("WAVE".toByteArray())
        fos.write("fmt ".toByteArray())
        fos.write(intToBytes(16))
        fos.write(shortToBytes(1))
        fos.write(shortToBytes(CHANNELS))
        fos.write(intToBytes(SAMPLE_RATE))
        fos.write(intToBytes(byteRate))
        fos.write(shortToBytes(blockAlign))
        fos.write(shortToBytes(BITS_PER_SAMPLE))
        fos.write("data".toByteArray())
        fos.write(intToBytes(dataSize))
    }

    private fun intToBytes(value: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()

    private fun shortToBytes(value: Int): ByteArray =
        ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value.toShort()).array()

    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
