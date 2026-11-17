package com.example.ui.screens.food

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.DetectedFoodItem
import com.example.ai.FoodCategory
import com.example.ai.FoodNutritionCalculator
import com.example.ai.PreparationState
import com.example.ai.UsdaFoodReferenceDatabase
import com.example.ai.UsdaFoodReferenceEntry
import com.example.ui.theme.NexoraPurpleHighlight
import com.example.ui.theme.NexoraPurplePrimary
import com.example.ui.theme.NexoraSuccessGreen
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditFoodItemDialog(
    item: DetectedFoodItem,
    onDismiss: () -> Unit,
    onSave: (DetectedFoodItem) -> Unit
) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var gramsText by remember(item.id) { mutableStateOf(item.estimatedGrams.roundToInt().toString()) }
    var prepState by remember(item.id) { mutableStateOf(item.preparationState) }
    var manualMacroMode by remember(item.id) { mutableStateOf(false) }

    val parsedGrams = gramsText.toFloatOrNull()?.coerceAtLeast(1f) ?: item.estimatedGrams
    val previewScaled = remember(item, parsedGrams, name, prepState) {
        FoodNutritionCalculator.scaleItemByGrams(item, parsedGrams).copy(
            name = name.ifBlank { item.name },
            preparationState = prepState,
            preparationMethod = prepState.label
        )
    }

    var proteinText by remember(previewScaled.proteinGrams) { mutableStateOf(previewScaled.proteinGrams.toString()) }
    var carbsText by remember(previewScaled.carbsGrams) { mutableStateOf(previewScaled.carbsGrams.toString()) }
    var fatText by remember(previewScaled.fatGrams) { mutableStateOf(previewScaled.fatGrams.toString()) }
    var fiberText by remember(previewScaled.fiberGrams) { mutableStateOf(previewScaled.fiberGrams.toString()) }
    var sugarText by remember(previewScaled.sugarGrams) { mutableStateOf(previewScaled.sugarGrams.toString()) }
    var satFatText by remember(previewScaled.saturatedFatGrams) { mutableStateOf(previewScaled.saturatedFatGrams.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Edit Food Item & Portion", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ingredient Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Portion Weight (grams)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexoraPurpleHighlight
                )

                OutlinedTextField(
                    value = gramsText,
                    onValueChange = { input ->
                        gramsText = input.filter { it.isDigit() || it == '.' }
                    },
                    label = { Text("Grams (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_food_grams_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-25, -10, 10, 25).forEach { delta ->
                        Button(
                            onClick = {
                                val next = (parsedGrams + delta).coerceAtLeast(1f)
                                gramsText = next.roundToInt().toString()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp)
                        ) {
                            Text(if (delta > 0) "+${delta}g" else "${delta}g", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(
                    text = "Preparation State (Cooked vs Raw)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexoraPurpleHighlight
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        PreparationState.COOKED,
                        PreparationState.RAW,
                        PreparationState.GRILLED,
                        PreparationState.STEAMED,
                        PreparationState.BAKED,
                        PreparationState.FRIED
                    ).forEach { state ->
                        val selected = prepState == state
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) NexoraPurplePrimary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { prepState = state }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = state.label,
                                fontSize = 11.sp,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Live auto-scaled nutrition preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(NexoraPurplePrimary.copy(alpha = 0.12f))
                        .border(1.dp, NexoraPurplePrimary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Scaled Nutrition (${parsedGrams.roundToInt()}g): ${previewScaled.caloriesKcal} kcal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = NexoraPurpleHighlight
                        )
                        Text(
                            text = "P: ${previewScaled.proteinGrams}g  |  C: ${previewScaled.carbsGrams}g  |  F: ${previewScaled.fatGrams}g",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Fiber: ${previewScaled.fiberGrams}g  |  Sugar: ${previewScaled.sugarGrams}g  |  Sat Fat: ${previewScaled.saturatedFatGrams}g",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = { manualMacroMode = !manualMacroMode },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (manualMacroMode) "Hide Manual Macro Overrides" else "Customize Exact Macros Manually",
                        fontSize = 12.sp,
                        color = NexoraPurpleHighlight
                    )
                }

                if (manualMacroMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = proteinText,
                            onValueChange = { proteinText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Protein (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = carbsText,
                            onValueChange = { carbsText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Carbs (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fatText,
                            onValueChange = { fatText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Fat (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fiberText,
                            onValueChange = { fiberText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Fiber (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sugarText,
                            onValueChange = { sugarText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Sugar (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = satFatText,
                            onValueChange = { satFatText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Sat Fat") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = if (!manualMacroMode) {
                        previewScaled
                    } else {
                        val p = proteinText.toFloatOrNull() ?: previewScaled.proteinGrams
                        val c = carbsText.toFloatOrNull() ?: previewScaled.carbsGrams
                        val f = fatText.toFloatOrNull() ?: previewScaled.fatGrams
                        val fib = fiberText.toFloatOrNull() ?: previewScaled.fiberGrams
                        val sug = sugarText.toFloatOrNull() ?: previewScaled.sugarGrams
                        val sf = satFatText.toFloatOrNull() ?: previewScaled.saturatedFatGrams
                        val kcal = FoodNutritionCalculator.computeFiberAdjustedAtwaterCalories(p, c, f, fib)
                        FoodNutritionCalculator.sanitizeAndValidateItem(
                            previewScaled.copy(
                                caloriesKcal = kcal,
                                proteinGrams = p,
                                carbsGrams = c,
                                fatGrams = f,
                                fiberGrams = fib,
                                sugarGrams = sug,
                                saturatedFatGrams = sf,
                                isUserEdited = true,
                                referenceSource = "User Adjusted"
                            )
                        ).first
                    }
                    onSave(result)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary),
                modifier = Modifier.testTag("save_food_item_edit_btn")
            ) {
                Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddFoodIngredientDialog(
    onDismiss: () -> Unit,
    onAddItem: (DetectedFoodItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedEntry by remember { mutableStateOf<UsdaFoodReferenceEntry?>(null) }
    var customGramsText by remember { mutableStateOf("100") }

    // Custom non-catalog fields
    var customName by remember { mutableStateOf("") }
    var customProtein by remember { mutableStateOf("") }
    var customCarbs by remember { mutableStateOf("") }
    var customFat by remember { mutableStateOf("") }
    var isCustomManual by remember { mutableStateOf(false) }

    val matchingEntries = remember(searchQuery) {
        UsdaFoodReferenceDatabase.search(searchQuery)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Ingredient (USDA Verified)", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isCustomManual) NexoraPurplePrimary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { isCustomManual = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "USDA Database",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isCustomManual) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCustomManual) NexoraPurplePrimary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { isCustomManual = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Custom Item",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCustomManual) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (!isCustomManual) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search rice, dal, tofu, avocado, oil...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(matchingEntries) { entry ->
                            val selected = selectedEntry?.id == entry.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selected) NexoraPurplePrimary.copy(alpha = 0.22f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) NexoraPurpleHighlight else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedEntry = entry
                                        customGramsText = entry.defaultServingGrams.roundToInt().toString()
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.displayName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${entry.kcalPer100g.roundToInt()} kcal / 100g • P ${entry.proteinPer100g}g • F ${entry.fiberPer100g}g fib",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = entry.preparationState.label,
                                    fontSize = 10.sp,
                                    color = NexoraSuccessGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    selectedEntry?.let { entry ->
                        OutlinedTextField(
                            value = customGramsText,
                            onValueChange = { customGramsText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Weight in grams (${entry.defaultServingLabel})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Food Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customGramsText,
                        onValueChange = { customGramsText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Weight (grams)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = customProtein,
                            onValueChange = { customProtein = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("P (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customCarbs,
                            onValueChange = { customCarbs = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("C (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customFat,
                            onValueChange = { customFat = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("F (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val grams = customGramsText.toFloatOrNull()?.coerceAtLeast(1f) ?: 100f
                    if (!isCustomManual) {
                        val entry = selectedEntry ?: return@Button
                        onAddItem(UsdaFoodReferenceDatabase.createItemFromReference(entry, grams))
                    } else {
                        val p = customProtein.toFloatOrNull()?.coerceAtLeast(0f) ?: 0f
                        val c = customCarbs.toFloatOrNull()?.coerceAtLeast(0f) ?: 0f
                        val f = customFat.toFloatOrNull()?.coerceAtLeast(0f) ?: 0f
                        val kcal = FoodNutritionCalculator.computeAtwaterCalories(p, c, f)
                        val newItem = DetectedFoodItem(
                            name = customName.trim().ifBlank { "Custom Ingredient" },
                            category = FoodCategory.OTHER,
                            preparationState = PreparationState.COOKED,
                            preparationMethod = "User Entry",
                            estimatedGrams = grams,
                            servingDescription = "${grams.roundToInt()}g",
                            confidence = 1f,
                            caloriesKcal = kcal,
                            proteinGrams = p,
                            carbsGrams = c,
                            fatGrams = f,
                            referenceSource = "User Entry + Atwater 4-4-9",
                            isUserEdited = true
                        )
                        onAddItem(FoodNutritionCalculator.sanitizeAndValidateItem(newItem).first)
                    }
                },
                enabled = if (!isCustomManual) selectedEntry != null else customName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NexoraPurplePrimary)
            ) {
                Text("Add Item", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
