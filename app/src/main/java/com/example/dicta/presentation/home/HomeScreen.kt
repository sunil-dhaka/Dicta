package com.example.dicta.presentation.home

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dicta.asr.AsrState
import com.example.dicta.presentation.theme.RecordingRed
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("Recording saved")
            viewModel.clearSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Dicta") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Transcription area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                val displayText = buildString {
                    append(uiState.transcription)
                    if (uiState.partialText.isNotBlank()) {
                        if (isNotEmpty()) append(" ")
                        append(uiState.partialText)
                    }
                }

                if (displayText.isBlank()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (uiState.asrState) {
                                is AsrState.Loading -> "Loading model..."
                                is AsrState.Ready -> "Tap to start recording"
                                is AsrState.Listening -> "Listening..."
                                is AsrState.Error -> "Error: ${(uiState.asrState as AsrState.Error).message}"
                                else -> "Initializing..."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    SelectionContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (uiState.transcription.isNotBlank()) {
                                Text(
                                    text = uiState.transcription,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            if (uiState.partialText.isNotBlank()) {
                                Text(
                                    text = if (uiState.transcription.isNotBlank()) " ${uiState.partialText}" else uiState.partialText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // Action buttons
            AnimatedVisibility(
                visible = uiState.transcription.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Transcription", uiState.transcription)
                            clipboard.setPrimaryClip(clip)
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy")
                    }

                    FilledTonalButton(
                        onClick = { viewModel.saveRecording() },
                        enabled = !uiState.isRecording && !uiState.isSaving
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save")
                    }

                    FilledTonalButton(
                        onClick = { viewModel.clearTranscription() },
                        enabled = !uiState.isRecording
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear")
                    }
                }
            }

            // Record button
            Spacer(modifier = Modifier.height(8.dp))

            RecordButton(
                isRecording = uiState.isRecording,
                isEnabled = uiState.asrState == AsrState.Ready || uiState.asrState == AsrState.Listening,
                onClick = {
                    if (!audioPermissionState.status.isGranted) {
                        audioPermissionState.launchPermissionRequest()
                    } else {
                        viewModel.toggleRecording()
                    }
                }
            )

            if (!audioPermissionState.status.isGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (audioPermissionState.status.shouldShowRationale) {
                        "Microphone permission is needed for transcription"
                    } else {
                        "Tap the microphone and grant permission"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RecordButton(
    isRecording: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isRecording) RecordingRed else MaterialTheme.colorScheme.primaryContainer,
        label = "backgroundColor"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer,
        label = "iconTint"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LargeFloatingActionButton(
        onClick = onClick,
        containerColor = backgroundColor,
        modifier = Modifier.scale(if (isRecording) scale else 1f),
        shape = CircleShape
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = if (isRecording) "Stop recording" else "Start recording",
            tint = iconTint,
            modifier = Modifier.size(36.dp)
        )
    }
}
