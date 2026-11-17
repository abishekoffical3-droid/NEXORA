package com.example.ui.screens.food

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.DetectedFoodItem
import com.example.ai.FoodNutritionAnalysis
import com.example.ui.components.GlassCard
import com.example.ui.theme.NexoraErrorRed
import com.example.ui.theme.NexoraNeonPurpleGlow
import com.example.ui.theme.NexoraPurpleHighlight
import com.example.ui.theme.NexoraPurplePrimary
import com.example.ui.theme.NexoraSuccessGreen
import com.example.ui.theme.NexoraWarningAmber
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodNutritionDashboardCards(
    food: FoodNutritionAnalysis,
    onEditItem: (DetectedFoodItem) -> Unit,
    onDeleteItem: (String) -> Unit,
    onOpenAddItem: () -> Unit,
    onLogMeal: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. No Food Detected Honest Card
        if (!food.foodDetected || food.items.isEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = NexoraWarningAmber.copy(alpha = 0.5f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "No food detected",
                            tint = NexoraWarningAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No Recognizable Food Verified",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = food.noFoodReason
                            ?: "No clear food ingredients were visible. Improve lighting, center the bowl/plate, or add items manually.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    food.imageQuality?.advisory?.let { adv ->
                        Text(
                            text = "Lighting note: $adv",
                            color = NexoraWarningAmber,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = onOpenAddItem,
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Food Items Manually (USDA)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            return
        }

        // 2. Hero Totals & Confidence Header Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                            text = food.foodName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Total Portion: ${food.portionSize}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(NexoraNeonPurpleGlow.copy(alpha = 0.2f))
                            .border(1.dp, NexoraNeonPurpleGlow, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${food.calories} kcal",
                            color = NexoraPurpleHighlight,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Confidence, Atwater & Lighting Telemetry Pills
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val confPct = (food.overallConfidence * 100f).roundToInt()
                    val confColor = when {
                        confPct >= 80 -> NexoraSuccessGreen
                        confPct >= 60 -> NexoraWarningAmber
                        else -> NexoraErrorRed
                    }
                    StatusPill(
                        text = "AI Confidence $confPct%",
                        color = confColor
                    )

                    food.atwaterValidation?.let { av ->
                        StatusPill(
                            text = if (av.wasReconciled) "Atwater Reconciled (${av.atwaterCalories} kcal)"
                            else "4-4-9 Verified",
                            color = if (av.isWithinTolerance || av.wasReconciled) NexoraSuccessGreen else NexoraWarningAmber
                        )
                    }

                    food.imageQuality?.let { iq ->
                        StatusPill(
                            text = "Light: ${iq.lightingLabel}",
                            color = if (iq.isLowLight) NexoraWarningAmber else NexoraPurpleHighlight
                        )
                    }

                    StatusPill(
                        text = "Balance ${food.healthScore}/100",
                        color = NexoraPurpleHighlight
                    )
                }

                // Uncertainty or lighting advisory notice
                if (food.uncertainItemsSummary.isNotEmpty() || food.imageQuality?.advisory != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NexoraWarningAmber.copy(alpha = 0.12f))
                            .border(1.dp, NexoraWarningAmber.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = NexoraWarningAmber,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Portion & Visual Uncertainty Notes",
                                    color = NexoraWarningAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            food.imageQuality?.advisory?.let { adv ->
                                Text(text = "• $adv", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            food.uncertainItemsSummary.take(4).forEach { note ->
                                Text(text = "• $note", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }

        // 3. Macro Summary + Extended Fiber/Sugar/SatFat Grid + Donut Chart
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "MACRONUTRIENT & ENERGY DISTRIBUTION",
                    color = NexoraPurpleHighlight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                MacroCalorieDonutChart(
                    totalCalories = food.calories,
                    proteinGrams = food.proteinGrams,
                    carbsGrams = food.carbsGrams,
                    fatGrams = food.fatsGrams
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NutrientMiniCard(
                        label = "PROTEIN",
                        amount = "${food.proteinGrams}g",
                        color = NexoraSuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    NutrientMiniCard(
                        label = "CARBS",
                        amount = "${food.carbsGrams}g",
                        color = NexoraWarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                    NutrientMiniCard(
                        label = "TOTAL FAT",
                        amount = "${food.fatsGrams}g",
                        color = NexoraPurpleHighlight,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NutrientMiniCard(
                        label = "FIBER",
                        amount = "${food.fiberGrams}g",
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                    NutrientMiniCard(
                        label = "SUGAR",
                        amount = "${food.sugarGrams}g",
                        color = Color(0xFFF472B6),
                        modifier = Modifier.weight(1f)
                    )
                    NutrientMiniCard(
                        label = "SAT. FAT",
                        amount = "${food.saturatedFatGrams}g",
                        color = Color(0xFFFB923C),
                        modifier = Modifier.weight(1f)
                    )
                }

                food.atwaterValidation?.let { av ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = NexoraSuccessGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = av.note,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4. Per-Item Calorie Contribution Chart & Editable Detected Food Items
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DETECTED INGREDIENTS (${food.items.size})",
                            color = NexoraPurpleHighlight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Tap edit to adjust grams, cooked/raw state, or add missing items",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onOpenAddItem,
                        colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_food_item_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add food item", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                PerItemCalorieBarChart(items = food.items)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    food.items.forEach { item ->
                        DetectedFoodItemRow(
                            item = item,
                            canDelete = food.items.size > 1,
                            onEdit = { onEditItem(item) },
                            onDelete = { onDeleteItem(item.id) }
                        )
                    }
                }
            }
        }

        // 5. Vitamins, Minerals, Summary & Log Action
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (food.vitamins.isNotEmpty() || food.minerals.isNotEmpty()) {
                    Text(
                        text = "KEY VITAMINS & MINERALS",
                        color = NexoraPurpleHighlight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        (food.vitamins + food.minerals).forEach { nutrient ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = nutrient,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (food.summary.isNotBlank() || food.rickyTip.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NexoraPurplePrimary.copy(alpha = 0.12f))
                            .border(1.dp, NexoraPurplePrimary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = NexoraPurpleHighlight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "NUTRITION INSIGHT",
                                    color = NexoraPurpleHighlight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (food.summary.isNotBlank()) {
                                Text(
                                    text = food.summary,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                            if (food.rickyTip.isNotBlank()) {
                                Text(
                                    text = food.rickyTip,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onLogMeal,
                    colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("log_meal_button")
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Meal to NEXORA", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Nutrition estimates follow USDA FoodData Central & Atwater energy formulas for informational wellness tracking only — not medical diagnosis or clinical dietary advice.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetectedFoodItemRow(
    item: DetectedFoodItem,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val confPct = (item.confidence * 100f).roundToInt()
    val borderColor = if (item.isUncertain || item.isWeightUncertain) {
        NexoraWarningAmber.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.outline
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.estimatedGrams.roundToInt()}g (${item.servingDescription}) • ${item.preparationMethod.ifBlank { item.preparationState.label }}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${item.caloriesKcal} kcal",
                    color = NexoraPurpleHighlight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(38.dp).testTag("edit_food_item_${item.name}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit ${item.name}",
                        tint = NexoraPurpleHighlight,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove ${item.name}",
                            tint = NexoraErrorRed.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StatusPill(text = item.category.displayName, color = NexoraPurpleHighlight)
            StatusPill(
                text = if (item.isUserEdited) "Edited" else "Conf $confPct%",
                color = when {
                    item.isUserEdited || confPct >= 80 -> NexoraSuccessGreen
                    confPct >= 60 -> NexoraWarningAmber
                    else -> NexoraErrorRed
                }
            )
            if (item.isWeightUncertain && item.weightRangeGrams.isNotBlank()) {
                StatusPill(text = "Weight ~${item.weightRangeGrams}", color = NexoraWarningAmber)
            } else if (item.isUncertain) {
                StatusPill(text = "Uncertain ID", color = NexoraWarningAmber)
            }
        }

        // Individual item macros breakdown (P, C, F, Fiber, Sugar, SatFat)
        Text(
            text = "P: ${item.proteinGrams}g  •  C: ${item.carbsGrams}g  •  F: ${item.fatGrams}g  •  Fib: ${item.fiberGrams}g  •  Sug: ${item.sugarGrams}g  •  SatF: ${item.saturatedFatGrams}g",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NutrientMiniCard(
    label: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = amount, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
