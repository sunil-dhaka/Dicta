package com.example.dicta.domain.model

import java.time.Instant

data class Recording(
    val id: Long = 0,
    val title: String,
    val transcription: String,
    val audioFilePath: String?,
    val durationMs: Long,
    val createdAt: Instant,
    val modelUsed: String
)
