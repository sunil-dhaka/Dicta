package com.example.dicta.presentation.onboarding

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dicta.domain.model.AsrModel
import com.example.dicta.domain.model.AsrModelType

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 28.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(72.dp))

        // Wordmark
        Text(
            text = "Dicta",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Offline dictation. Your voice never leaves the device.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(56.dp))

        Text(
            text = "CHOOSE A MODEL",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(14.dp))

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        uiState.availableModels.forEach { model ->
            ModelRow(
                model = model,
                isSelected = model.type == uiState.selectedModel,
                isRecommended = model.type == AsrModelType.MOONSHINE_SMALL_STREAMING,
                enabled = !uiState.isDownloading,
                onSelect = { viewModel.selectModel(model.type) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }

        Spacer(Modifier.height(40.dp))

        if (uiState.isDownloading) {
            val selectedModel = uiState.availableModels.find { it.type == uiState.selectedModel }
            val downloadedMB = (uiState.downloadProgress * (selectedModel?.sizeBytes ?: 0) / (1024 * 1024)).toInt()
            val totalMB = ((selectedModel?.sizeBytes ?: 0) / (1024 * 1024)).toInt()

            LinearProgressIndicator(
                progress = { uiState.downloadProgress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onBackground,
                trackColor = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Downloading $downloadedMB of $totalMB MB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Button(
                onClick = { viewModel.downloadSelectedModel() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                ),
            ) {
                Text(
                    text = "Download & Continue",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        uiState.downloadError?.let { error ->
            Spacer(Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun ModelRow(
    model: AsrModel,
    isSelected: Boolean,
    isRecommended: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    val sizeMB = model.sizeBytes / (1024 * 1024)
    val sizeStr = if (sizeMB >= 1024) String.format("%.1f GB", sizeMB / 1024.0) else "$sizeMB MB"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onSelect)
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Selection dot — simple filled circle, no ring chrome.
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.outline
                ),
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (isRecommended) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "RECOMMENDED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = model.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = sizeStr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "WER ${model.wer}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
