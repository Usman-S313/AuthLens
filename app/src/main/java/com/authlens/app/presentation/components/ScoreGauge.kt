package com.authlens.app.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.authlens.app.domain.model.RiskLevel
import com.authlens.app.core.theme.RiskClean
import com.authlens.app.core.theme.RiskHigh
import com.authlens.app.core.theme.RiskLikely
import com.authlens.app.core.theme.RiskSuspicious

/**
 * A circular progress gauge that animates from 0 to [score] and is tinted according to
 * the [level]'s risk color. The headline number sits in the center.
 */
@Composable
fun ScoreGauge(
    score: Int,
    level: RiskLevel,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(durationMillis = 900),
        label = "score",
    )
    val color = colorForLevel(level)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val stroke = 18.dp.toPx()
                val diameter = size.minDimension - stroke
                val topLeft = Offset(
                    x = (size.width - diameter) / 2f,
                    y = (size.height - diameter) / 2f,
                )
                val arcSize = Size(diameter, diameter)

                // Background track
                drawArc(
                    color = Color.Gray.copy(alpha = 0.15f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                // Progress arc
                val sweep = 270f * (animated / 100f)
                drawArc(
                    brush = Brush.sweepGradient(listOf(color, color.copy(alpha = 0.6f), color)),
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${animated.toInt()}",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                Text(text = "/ 100", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = level.label,
            style = MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = level.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Maps a risk level to its display color. */
fun colorForLevel(level: RiskLevel): Color = when (level) {
    RiskLevel.CLEAN -> RiskClean
    RiskLevel.SUSPICIOUS -> RiskSuspicious
    RiskLevel.LIKELY_FRAUD -> RiskLikely
    RiskLevel.HIGH_RISK -> RiskHigh
}
