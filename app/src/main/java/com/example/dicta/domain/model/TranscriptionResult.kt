package com.example.dicta.domain.model

sealed class TranscriptionResult {
    data class Partial(val text: String) : TranscriptionResult()
    data class Final(val text: String) : TranscriptionResult()
    data class Error(val message: String) : TranscriptionResult()
}
