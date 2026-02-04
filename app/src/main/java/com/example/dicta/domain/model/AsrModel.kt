package com.example.dicta.domain.model

enum class AsrModelType {
    VOSK_SMALL_EN_US,
    VOSK_MEDIUM_EN_US,
    VOSK_LARGE_EN_US,
    VOSK_XLARGE_EN_US
}

data class AsrModel(
    val type: AsrModelType,
    val displayName: String,
    val description: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val isDownloaded: Boolean = false,
    val downloadProgress: Float = 0f
) {
    companion object {
        val VOSK_SMALL = AsrModel(
            type = AsrModelType.VOSK_SMALL_EN_US,
            displayName = "Vosk Small",
            description = "Fast, real-time streaming. Basic accuracy.",
            sizeBytes = 50 * 1024 * 1024L,
            downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
        )

        val VOSK_MEDIUM = AsrModel(
            type = AsrModelType.VOSK_MEDIUM_EN_US,
            displayName = "Vosk Medium",
            description = "Real-time streaming. Good accuracy.",
            sizeBytes = 128 * 1024 * 1024L,
            downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-en-us-0.22-lgraph.zip"
        )

        val VOSK_LARGE = AsrModel(
            type = AsrModelType.VOSK_LARGE_EN_US,
            displayName = "Vosk Large",
            description = "Real-time streaming. Better accuracy.",
            sizeBytes = 1600 * 1024 * 1024L,
            downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-en-us-0.21.zip"
        )

        val VOSK_XLARGE = AsrModel(
            type = AsrModelType.VOSK_XLARGE_EN_US,
            displayName = "Vosk XLarge",
            description = "Real-time streaming. Best accuracy.",
            sizeBytes = 1800 * 1024 * 1024L,
            downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-en-us-0.22.zip"
        )

        fun getAll(): List<AsrModel> = listOf(
            VOSK_SMALL, VOSK_MEDIUM, VOSK_LARGE, VOSK_XLARGE
        )

        fun getByType(type: AsrModelType): AsrModel? = getAll().find { it.type == type }
    }
}
