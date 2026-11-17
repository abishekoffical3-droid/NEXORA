package com.example.ui.screens.food

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.DetectedFoodItem
import com.example.ui.theme.NexoraPurpleHighlight
import com.example.ui.theme.NexoraSuccessGreen
import com.example.ui.theme.NexoraWarningAmber
import kotlin.math.roundToInt

private val ChartProteinColor = NexoraSuccessGreen
private val ChartCarbsColor = NexoraWarningAmber
private val ChartFatColor = NexoraPurpleHighlight

@Composable
fun MacroCalorieDonutChart(
    totalCalories: Int,
    proteinGrams: Float,
    carbsGrams: Float,
    fatGrams: Float,
    modifier: Modifier = Modifier
) {
    val proteinKcal = (proteinGrams * 4f).coerceAtLeast(0f)
    val carbsKcal = (carbsGrams * 4f).coerceAtLeast(0f)
    val fatKcal = (fatGrams * 9f).coerceAtLeast(0f)
    val macroTotalKcal = (proteinKcal + carbsKcal + fatKcal).coerceAtLeast(1f)

    val pShare = proteinKcal / macroTotalKcal
    val cShare = carbsKcal / macroTotalKcal
    val fShare = fatKcal / macroTotalKcal

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(124.dp)
        ) {
            Canvas(modifier = Modifier.size(114.dp)) {
                val strokeWidth = 14.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                // Background track
                drawArc(
                    color = Color(0x28FFFFFF),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )

                var startAngle = -90f
                val segments = listOf(
                    pShare to ChartProteinColor,
                    cShare to ChartCarbsColor,
                    fShare to ChartFatColor
                )
                segments.forEach { (fraction, color) ->
                    val sweep = (fraction * 360f).coerceAtLeast(0f)
                    if (sweep > 1f) {
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = (sweep - 3f).coerceAtLeast(1f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        startAngle += sweep
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$totalCalories",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "kcal",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MacroLegendRow(
                label = "Protein (4 kcal/g)",
                grams = proteinGrams,
                percent = (pShare * 100f).roundToInt(),
                color = ChartProteinColor
            )
            MacroLegendRow(
                label = "Carbs (4 kcal/g)",
                grams = carbsGrams,
                percent = (cShare * 100f).roundToInt(),
                color = ChartCarbsColor
            )
            MacroLegendRow(
                label = "Fat (9 kcal/g)",
                grams = fatGrams,
                percent = (fShare * 100f).roundToInt(),
                color = ChartFatColor
            )
        }
    }
}

@Composable
private fun MacroLegendRow(
    label: String,
    grams: Float,
    percent: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "${grams}g ($percent%)",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PerItemCalorieBarChart(
    items: List<DetectedFoodItem>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val maxCalories = items.maxOf { it.caloriesKcal }.coerceAtLeast(1)
    val palette = listOf(
        NexoraPurpleHighlight,
        NexoraSuccessGreen,
        NexoraWarningAmber,
        Color(0xFF38BDF8),
        Color(0xFFF472B6),
        Color(0xFF818CF8)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEachIndexed { idx, item ->
            val fraction = (item.caloriesKcal.toFloat() / maxCalories.toFloat()).coerceIn(0.06f, 1f)
            val barColor = palette[idx % palette.size]

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.name} (${item.estimatedGrams.roundToInt()}g)",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Text(
                        text = "${item.caloriesKcal} kcal",
                        color = barColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor)
                    )
                }
            }
        }
    }
}
