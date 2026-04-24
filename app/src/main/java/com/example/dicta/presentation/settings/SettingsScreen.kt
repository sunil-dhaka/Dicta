package com.example.dicta.presentation.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dicta.domain.model.AsrModelType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var modelToDelete by remember { mutableStateOf<AsrModelType?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }
    LaunchedEffect(uiState.clearDataSuccess) {
        if (uiState.clearDataSuccess) { snackbarHostState.showSnackbar("All recordings cleared"); viewModel.clearSuccess() }
    }
    LaunchedEffect(uiState.modelSwitched) {
        if (uiState.modelSwitched) { snackbarHostState.showSnackbar("Model changed. Restart to apply."); viewModel.clearModelSwitched() }
    }
    LaunchedEffect(uiState.exportSuccess) {
        if (uiState.exportSuccess) { snackbarHostState.showSnackbar("Recordings exported"); viewModel.clearExportSuccess() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(32.dp))

            SectionLabel("MODELS")
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            uiState.models.forEach { modelState ->
                ModelRow(
                    modelState = modelState,
                    onDownload = { viewModel.downloadModel(modelState.model.type) },
                    onSelect = { viewModel.selectModel(modelState.model.type) },
                    onDelete = { modelToDelete = modelState.model.type },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }

            Spacer(Modifier.height(40.dp))

            SectionLabel("DATA")
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            DataRow(
                title = "Export all recordings",
                subtitle = "Save a ZIP with JSON metadata",
                actionLabel = if (uiState.isExporting) "…" else if (uiState.recordingCount > 0) "Export" else "—",
                actionEnabled = !uiState.isExporting && uiState.recordingCount > 0,
                onClick = { viewModel.exportAllRecordings() },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            DataRow(
                title = "Clear all recordings",
                subtitle = "Permanently delete audio and transcripts",
                actionLabel = "Clear",
                actionEmber = true,
                actionEnabled = uiState.recordingCount > 0,
                onClick = { showClearDataDialog = true },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Spacer(Modifier.height(48.dp))

            Text(
                text = "Dicta · v1.1.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Clear all recordings?") },
            text = { Text("This permanently deletes all saved recordings and audio files.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearAllData(); showClearDataDialog = false },
                ) { Text("Delete all", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) { Text("Cancel") }
            },
        )
    }

    modelToDelete?.let { modelType ->
        val isSelected = uiState.selectedModel == modelType
        AlertDialog(
            onDismissRequest = { modelToDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Delete model?") },
            text = {
                Text(
                    if (isSelected) "This is your active model. You'll need to download one again."
                    else "Remove this model to free storage?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteModel(modelType); modelToDelete = null },
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { modelToDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ModelRow(
    modelState: ModelState,
    onDownload: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    val model = modelState.model
    val sizeMB = model.sizeBytes / (1024 * 1024)
    val sizeStr = if (sizeMB >= 1024) String.format("%.1f GB", sizeMB / 1024.0) else "$sizeMB MB"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Left ember accent bar marks the active model.
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(56.dp)
                .background(
                    if (modelState.isSelected && modelState.isDownloaded)
                        MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.background,
                ),
        )
        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (modelState.isSelected && modelState.isDownloaded) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "IN USE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = model.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "$sizeStr   ·   WER ${model.wer}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (modelState.isDownloading) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { modelState.downloadProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onBackground,
                    trackColor = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Downloading · ${(modelState.downloadProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Right-side action: Download / Use / (delete icon secondary)
        Column(horizontalAlignment = Alignment.End) {
            when {
                modelState.isDownloading -> {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp),
                    )
                }
                !modelState.isDownloaded -> {
                    Text(
                        text = "Download",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable(onClick = onDownload)
                            .padding(vertical = 4.dp),
                    )
                }
                modelState.isDownloaded && !modelState.isSelected -> {
                    Text(
                        text = "Use",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable(onClick = onSelect)
                            .padding(vertical = 4.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(onClick = onDelete),
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(onClick = onDelete),
                    )
                }
            }
        }
    }
}

@Composable
private fun DataRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    actionEnabled: Boolean = true,
    actionEmber: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = actionEnabled, onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = actionLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = when {
                !actionEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                actionEmber -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onBackground
            },
        )
    }
}
