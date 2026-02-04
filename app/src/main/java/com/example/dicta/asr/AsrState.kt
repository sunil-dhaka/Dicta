package com.example.dicta.asr

sealed class AsrState {
    data object Uninitialized : AsrState()
    data object Loading : AsrState()
    data object Ready : AsrState()
    data object Listening : AsrState()
    data class Error(val message: String) : AsrState()
}
