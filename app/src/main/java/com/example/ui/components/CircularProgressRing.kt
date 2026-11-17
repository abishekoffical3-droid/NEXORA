package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalNexoraMotion
import com.example.ui.theme.NexoraNeonPurpleGlow
import com.example.ui.theme.NexoraPurpleHighlight
import com.example.ui.theme.NexoraPurplePrimary
import com.example.ui.theme.NexoraTextWhite

@Composable
fun CircularProgressRing(
    progress: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    size: Dp = 110.dp,
    strokeWidth: Dp = 10.dp,
    trackColor: Color = Color(0x268B5CF6),
    gradientColors: List<Color> = listOf(NexoraPurplePrimary, NexoraPurpleHighlight, NexoraNeonPurpleGlow),
    centerText: String? = null,
    subtitle: String? = null
) {
    val motion = LocalNexoraMotion.current
    val targetProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = if (motion.reduceAnimations) 0 else 650),
        label = "progress_anim"
    )
    val cachedBrush = remember(gradientColors) {
        Brush.sweepGradient(gradientColors)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val diameter = this.size.minDimension - stroke
            val radius = diameter / 2f
            val center = this.center

            // Background track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = stroke)
            )

            // Progress arc using pre-cached SweepGradient to prevent per-frame shader allocations
            if (animatedProgress > 0f) {
                drawArc(
                    brush = cachedBrush,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        if (centerText != null) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = centerText,
                    color = NexoraTextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
