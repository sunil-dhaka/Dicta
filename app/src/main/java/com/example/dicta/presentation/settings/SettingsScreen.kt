package com.example.dicta.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dicta.domain.model.AsrModelType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var modelToDelete by remember { mutableStateOf<AsrModelType?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.clearDataSuccess) {
        if (uiState.clearDataSuccess) {
            snackbarHostState.showSnackbar("All recordings cleared")
            viewModel.clearSuccess()
        }
    }

    LaunchedEffect(uiState.modelSwitched) {
        if (uiState.modelSwitched) {
            snackbarHostState.showSnackbar("Model changed. Restart app to apply.")
            viewModel.clearModelSwitched()
        }
    }

    LaunchedEffect(uiState.exportSuccess) {
        if (uiState.exportSuccess) {
            snackbarHostState.showSnackbar("Recordings exported successfully")
            viewModel.clearExportSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Speech Recognition Models",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Download and manage models. Lower WER means higher accuracy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            uiState.models.forEach { modelState ->
                ModelCard(
                    modelState = modelState,
                    onDownload = { viewModel.downloadModel(modelState.model.type) },
                    onSelect = { viewModel.selectModel(modelState.model.type) },
                    onDelete = { modelToDelete = modelState.model.type }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Data",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                ListItem(
                    headlineContent = { Text("Export All Recordings") },
                    supportingContent = { Text("Export as ZIP with JSON metadata") },
                    trailingContent = {
                        Button(
                            onClick = { viewModel.exportAllRecordings() },
                            enabled = !uiState.isExporting && uiState.recordingCount > 0
                        ) {
                            if (uiState.isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.width(20.dp).height(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.Share, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (uiState.recordingCount > 0)
                                    "${uiState.recordingCount} rec."
                                else
                                    "None"
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                ListItem(
                    headlineContent = { Text("Clear All Recordings") },
                    supportingContent = { Text("Delete all saved recordings and audio files") },
                    trailingContent = {
                        OutlinedButton(
                            onClick = { showClearDataDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Dicta v1.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Recordings?") },
            text = { Text("This will permanently delete all saved recordings and audio files.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    modelToDelete?.let { modelType ->
        val isSelected = uiState.selectedModel == modelType
        AlertDialog(
            onDismissRequest = { modelToDelete = null },
            title = { Text("Delete Model?") },
            text = {
                Text(
                    if (isSelected) {
                        "This is your currently selected model. Deleting it will require you to download a model again."
                    } else {
                        "Delete this model to free up storage space?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteModel(modelType)
                        modelToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { modelToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ModelCard(
    modelState: ModelState,
    onDownload: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val model = modelState.model
    val sizeMB = model.sizeBytes / (1024 * 1024)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (modelState.isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = model.displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (modelState.isSelected && modelState.isDownloaded) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = model.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = formatSize(sizeMB),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "WER: ${model.wer}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (modelState.isDownloading) {
                LinearProgressIndicator(
                    progress = { modelState.downloadProgress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Downloading... ${(modelState.downloadProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall
                )
            } else if (modelState.isDownloaded) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!modelState.isSelected) {
                        Button(
                            onClick = onSelect,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Use This")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Active")
                        }
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete"
                        )
                    }
                }
            } else {
                Button(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download")
                }
            }
        }
    }
}

private fun formatSize(sizeMB: Long): String {
    return if (sizeMB >= 1024) {
        String.format("%.1f GB", sizeMB / 1024.0)
    } else {
        "$sizeMB MB"
    }
}
