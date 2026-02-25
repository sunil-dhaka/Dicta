package com.example.dicta.domain.model

import ai.moonshine.voice.JNI

enum class AsrModelType(val archConstant: Int) {
    MOONSHINE_TINY_STREAMING(JNI.MOONSHINE_MODEL_ARCH_TINY_STREAMING),
    MOONSHINE_SMALL_STREAMING(JNI.MOONSHINE_MODEL_ARCH_SMALL_STREAMING),
    MOONSHINE_MEDIUM_STREAMING(JNI.MOONSHINE_MODEL_ARCH_MEDIUM_STREAMING),
    MOONSHINE_BASE(JNI.MOONSHINE_MODEL_ARCH_BASE)
}

data class AsrModel(
    val type: AsrModelType,
    val displayName: String,
    val description: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val wer: String,
    val isDownloaded: Boolean = false,
    val downloadProgress: Float = 0f
) {
    companion object {
        private const val GITHUB_RELEASE_BASE =
            "https://github.com/sunil-dhaka/Dicta/releases/download/v2.0"

        val MOONSHINE_TINY_STREAM = AsrModel(
            type = AsrModelType.MOONSHINE_TINY_STREAMING,
            displayName = "Tiny Streaming",
            description = "Fastest. Good for quick notes.",
            sizeBytes = 49 * 1024 * 1024L,
            downloadUrl = "$GITHUB_RELEASE_BASE/moonshine-tiny-streaming-en.zip",
            wer = "12%"
        )

        val MOONSHINE_SMALL_STREAM = AsrModel(
            type = AsrModelType.MOONSHINE_SMALL_STREAMING,
            displayName = "Small Streaming",
            description = "Best balance of speed and accuracy.",
            sizeBytes = 158 * 1024 * 1024L,
            downloadUrl = "$GITHUB_RELEASE_BASE/moonshine-small-streaming-en.zip",
            wer = "7.84%"
        )

        val MOONSHINE_MEDIUM_STREAM = AsrModel(
            type = AsrModelType.MOONSHINE_MEDIUM_STREAMING,
            displayName = "Medium Streaming",
            description = "Highest accuracy for real-time use.",
            sizeBytes = 289 * 1024 * 1024L,
            downloadUrl = "$GITHUB_RELEASE_BASE/moonshine-medium-streaming-en.zip",
            wer = "6.65%"
        )

        val MOONSHINE_BASE_OFFLINE = AsrModel(
            type = AsrModelType.MOONSHINE_BASE,
            displayName = "Base (Offline)",
            description = "Non-streaming. Best for file transcription.",
            sizeBytes = 134 * 1024 * 1024L,
            downloadUrl = "$GITHUB_RELEASE_BASE/moonshine-base-en.zip",
            wer = "~10%"
        )

        fun getAll(): List<AsrModel> = listOf(
            MOONSHINE_TINY_STREAM,
            MOONSHINE_SMALL_STREAM,
            MOONSHINE_MEDIUM_STREAM,
            MOONSHINE_BASE_OFFLINE
        )

        fun getByType(type: AsrModelType): AsrModel? = getAll().find { it.type == type }
    }
}
