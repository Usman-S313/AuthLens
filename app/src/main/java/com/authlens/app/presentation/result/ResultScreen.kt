package com.authlens.app.presentation.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.authlens.app.domain.model.FraudResult
import com.authlens.app.presentation.components.AnomalyHeatmap
import com.authlens.app.presentation.components.FindingCard
import com.authlens.app.presentation.components.ScoreGauge
import com.authlens.app.presentation.components.SimpleButton

/**
 * Result screen: headline fraud gauge, per-stage findings, and the anomaly heatmap.
 *
 * Falls back to an empty state if no result is present (e.g. user navigated here directly).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    viewModel: ResultViewModel,
    onBack: () -> Unit,
    onNewScan: () -> Unit,
) {
    val result by viewModel.result.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analysis Result") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val r = result
        if (r == null) {
            EmptyResult(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                onNewScan = onNewScan,
            )
        } else {
            ResultContent(result = r, modifier = Modifier.padding(padding), onNewScan = onNewScan)
        }
    }
}

@Composable
private fun ResultContent(
    result: FraudResult,
    modifier: Modifier,
    onNewScan: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScoreGauge(score = result.score.score, level = result.score.level)

        // Format badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Detected format: ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = result.detectedFormat.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (result.score.wasTerminatedEarly) {
            Text(
                text = "Pipeline stopped early due to a terminal signal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Per-stage findings
        result.checks.forEach { check ->
            FindingCard(result = check)
        }

        // Heatmap (only present for the integrity stage)
        val heatmap = result.checks.firstOrNull { it.heatmapBytes != null }?.heatmapBytes
        AnomalyHeatmap(heatmapBytes = heatmap)

        Spacer(modifier = Modifier.height(8.dp))
        SimpleButton(text = "Scan Another Document", onClick = onNewScan)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EmptyResult(modifier: Modifier, onNewScan: () -> Unit) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No analysis to show",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Run a scan from the upload screen.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        SimpleButton(text = "Go to Upload", onClick = onNewScan)
    }
}
