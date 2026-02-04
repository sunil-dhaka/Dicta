package com.example.dicta.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.dicta.domain.model.Recording
import java.time.Instant

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val transcription: String,
    val audioFilePath: String?,
    val durationMs: Long,
    val createdAt: Long,
    val modelUsed: String
) {
    fun toDomain(): Recording = Recording(
        id = id,
        title = title,
        transcription = transcription,
        audioFilePath = audioFilePath,
        durationMs = durationMs,
        createdAt = Instant.ofEpochMilli(createdAt),
        modelUsed = modelUsed
    )

    companion object {
        fun fromDomain(recording: Recording): RecordingEntity = RecordingEntity(
            id = recording.id,
            title = recording.title,
            transcription = recording.transcription,
            audioFilePath = recording.audioFilePath,
            durationMs = recording.durationMs,
            createdAt = recording.createdAt.toEpochMilli(),
            modelUsed = recording.modelUsed
        )
    }
}
