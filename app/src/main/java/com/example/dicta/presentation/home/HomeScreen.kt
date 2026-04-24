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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dicta.asr.AsrState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
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

    val statusLine = when (uiState.asrState) {
        is AsrState.Loading -> "Loading model"
        is AsrState.Ready -> "Ready"
        is AsrState.Listening -> "Listening"
        is AsrState.Error -> "Error"
        else -> "Initializing"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
        ) {
            // Header — wordmark only.
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Dicta",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            // Transcript area.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 28.dp),
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = statusLine,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (uiState.asrState is AsrState.Ready)
                                "Tap the mic to begin dictation." else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    SelectionContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        ) {
                            if (uiState.transcription.isNotBlank()) {
                                Text(
                                    text = uiState.transcription,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                            if (uiState.partialText.isNotBlank()) {
                                Text(
                                    text = if (uiState.transcription.isNotBlank()) " ${uiState.partialText}" else uiState.partialText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Inline actions (only when there is a transcript).
            AnimatedVisibility(
                visible = uiState.transcription.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.Start),
                ) {
                    InlineAction(
                        icon = Icons.Default.ContentCopy,
                        label = "Copy",
                        enabled = true,
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Transcription", uiState.transcription))
                        },
                    )
                    InlineAction(
                        icon = Icons.Default.Save,
                        label = "Save",
                        enabled = !uiState.isRecording && !uiState.isSaving,
                        onClick = { viewModel.saveRecording() },
                    )
                    InlineAction(
                        icon = Icons.Default.Delete,
                        label = "Clear",
                        enabled = !uiState.isRecording,
                        onClick = { viewModel.clearTranscription() },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Record control — the hero of the screen.
            RecordControl(
                isRecording = uiState.isRecording,
                isEnabled = uiState.asrState is AsrState.Ready || uiState.asrState is AsrState.Listening,
                onClick = {
                    if (!audioPermissionState.status.isGranted) {
                        audioPermissionState.launchPermissionRequest()
                    } else {
                        viewModel.toggleRecording()
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            if (!audioPermissionState.status.isGranted) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (audioPermissionState.status.shouldShowRationale) {
                        "Microphone permission is needed for transcription."
                    } else {
                        "Tap the mic to grant microphone access."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InlineAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = tint)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = tint)
    }
}

@Composable
private fun RecordControl(
    isRecording: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ember = MaterialTheme.colorScheme.tertiary
    val emberSoft = MaterialTheme.colorScheme.tertiaryContainer
    val idleBg = MaterialTheme.colorScheme.onBackground
    val idleFg = MaterialTheme.colorScheme.background

    val bg by animateColorAsState(
        targetValue = if (isRecording) ember else idleBg,
        label = "bg",
    )
    val fg by animateColorAsState(
        targetValue = if (isRecording) Color.White else idleFg,
        label = "fg",
    )

    // Breathing halo only when live.
    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.35f else 1f,
        animationSpec = infiniteRepeatable(animation = tween(1800), repeatMode = RepeatMode.Reverse),
        label = "haloScale",
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isRecording) 0.35f else 0f,
        animationSpec = infiniteRepeatable(animation = tween(1800), repeatMode = RepeatMode.Reverse),
        label = "haloAlpha",
    )

    Box(
        modifier = modifier.size(132.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(haloScale)
                    .clip(CircleShape)
                    .background(emberSoft.copy(alpha = haloAlpha)),
            )
        }

        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(bg)
                .border(
                    width = if (isRecording) 0.dp else 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                )
                .clickable(enabled = isEnabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop" else "Record",
                tint = fg,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
