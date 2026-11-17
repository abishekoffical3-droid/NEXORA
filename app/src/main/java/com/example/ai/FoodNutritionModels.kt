package com.example.ai

import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

enum class FoodCategory(val displayName: String) {
    GRAIN("Grain & Starch"),
    VEGETABLE("Vegetable"),
    FRUIT("Fruit"),
    MEAT_POULTRY_FISH("Meat / Poultry / Fish"),
    EGG("Egg"),
    TOFU_LEGUME("Tofu & Legume"),
    DAIRY("Dairy"),
    SAUCE_OIL("Sauce & Oil"),
    NUT_SEED("Nuts & Seeds"),
    MIXED_DISH("Mixed Dish"),
    OTHER("Other");

    companion object {
        fun fromString(raw: String?): FoodCategory {
            val normalized = raw?.trim()?.uppercase().orEmpty()
            return entries.firstOrNull { it.name == normalized } ?: when {
                normalized.contains("VEG") -> VEGETABLE
                normalized.contains("FRUIT") -> FRUIT
                normalized.contains("GRAIN") || normalized.contains("RICE") || normalized.contains("BREAD") || normalized.contains("POTATO") -> GRAIN
                normalized.contains("MEAT") || normalized.contains("CHICKEN") || normalized.contains("FISH") || normalized.contains("BEEF") -> MEAT_POULTRY_FISH
                normalized.contains("EGG") -> EGG
                normalized.contains("TOFU") || normalized.contains("DAL") || normalized.contains("LENTIL") || normalized.contains("BEAN") || normalized.contains("LEGUME") -> TOFU_LEGUME
                normalized.contains("DAIRY") || normalized.contains("YOGURT") || normalized.contains("CHEESE") -> DAIRY
                normalized.contains("SAUCE") || normalized.contains("OIL") || normalized.contains("DRESS") -> SAUCE_OIL
                normalized.contains("NUT") || normalized.contains("SEED") -> NUT_SEED
                normalized.contains("MIX") -> MIXED_DISH
                else -> OTHER
            }
        }
    }
}

enum class PreparationState(val label: String) {
    COOKED("Cooked"),
    RAW("Raw"),
    GRILLED("Grilled"),
    STEAMED("Steamed"),
    BAKED("Baked"),
    ROASTED("Roasted"),
    FRIED("Fried"),
    BOILED("Boiled"),
    UNKNOWN("Unspecified");

    companion object {
        fun fromString(raw: String?): PreparationState {
            val normalized = raw?.trim()?.uppercase().orEmpty()
            return entries.firstOrNull { it.name == normalized } ?: when {
                normalized.contains("RAW") || normalized.contains("FRESH") -> RAW
                normalized.contains("GRILL") -> GRILLED
                normalized.contains("STEAM") -> STEAMED
                normalized.contains("BAKE") -> BAKED
                normalized.contains("ROAST") -> ROASTED
                normalized.contains("FRY") || normalized.contains("FRIED") || normalized.contains("SAUTE") -> FRIED
                normalized.contains("BOIL") || normalized.contains("SIMMER") -> BOILED
                normalized.contains("COOK") -> COOKED
                else -> UNKNOWN
            }
        }
    }
}

data class AtwaterValidation(
    val reportedCalories: Int,
    val atwaterCalories: Int,
    val netCarbAdjustedCalories: Int,
    val variancePercent: Float,
    val isWithinTolerance: Boolean,
    val wasReconciled: Boolean,
    val note: String
)

data class ImageQualityAssessment(
    val lightingLabel: String,
    val meanLuminance: Float,
    val isLowLight: Boolean,
    val isVeryDark: Boolean,
    val enhancementApplied: Boolean,
    val advisory: String? = null
)

data class DetectedFoodItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: FoodCategory = FoodCategory.OTHER,
    val preparationState: PreparationState = PreparationState.UNKNOWN,
    val preparationMethod: String = "",
    val estimatedGrams: Float,
    val servingDescription: String = "",
    val isWeightUncertain: Boolean = false,
    val weightRangeGrams: String = "",
    val confidence: Float = 0.85f,
    val isUncertain: Boolean = false,
    val uncertaintyReason: String? = null,
    val caloriesKcal: Int,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    val fiberGrams: Float = 0f,
    val sugarGrams: Float = 0f,
    val saturatedFatGrams: Float = 0f,
    val referenceSource: String = "Vision + Atwater Verified",
    val isUserEdited: Boolean = false
)

data class FoodNutritionAnalysis(
    val foodName: String = "",
    val portionSize: String = "",
    val calories: Int = 0,
    val proteinGrams: Float = 0f,
    val carbsGrams: Float = 0f,
    val fatsGrams: Float = 0f,
    val vitamins: List<String> = emptyList(),
    val minerals: List<String> = emptyList(),
    val healthScore: Int = 0,
    val summary: String = "",
    val rickyTip: String = "",
    val foodDetected: Boolean = true,
    val items: List<DetectedFoodItem> = emptyList(),
    val totalGrams: Float = 0f,
    val fiberGrams: Float = 0f,
    val sugarGrams: Float = 0f,
    val saturatedFatGrams: Float = 0f,
    val overallConfidence: Float = 0f,
    val atwaterValidation: AtwaterValidation? = null,
    val imageQuality: ImageQualityAssessment? = null,
    val uncertainItemsSummary: List<String> = emptyList(),
    val noFoodReason: String? = null
)

object FoodNutritionCalculator {

    /**
     * Standard Atwater general factors: Protein 4 kcal/g, Carbohydrate 4 kcal/g, Fat 9 kcal/g.
     */
    fun computeAtwaterCalories(proteinGrams: Float, carbsGrams: Float, fatGrams: Float): Int {
        return ((proteinGrams.coerceAtLeast(0f) * 4f) +
            (carbsGrams.coerceAtLeast(0f) * 4f) +
            (fatGrams.coerceAtLeast(0f) * 9f)).roundToInt()
    }

    /**
     * Refined Atwater accounting for lower metabolizable energy of dietary fiber (~2 kcal/g vs 4 kcal/g).
     * Useful for high-fiber foods (dal, avocado, broccoli, oats, salads).
     */
    fun computeFiberAdjustedAtwaterCalories(
        proteinGrams: Float,
        carbsGrams: Float,
        fatGrams: Float,
        fiberGrams: Float
    ): Int {
        val clampedFiber = fiberGrams.coerceIn(0f, carbsGrams.coerceAtLeast(0f))
        val availableCarbs = (carbsGrams.coerceAtLeast(0f) - clampedFiber).coerceAtLeast(0f)
        return ((proteinGrams.coerceAtLeast(0f) * 4f) +
            (availableCarbs * 4f) +
            (clampedFiber * 2f) +
            (fatGrams.coerceAtLeast(0f) * 9f)).roundToInt()
    }

    /**
     * Validates a single food item's calories against Atwater factors and natural fiber/rounding tolerances.
     * Ensures sugar <= carbs and saturatedFat <= totalFat, and reconciles severe calorie discrepancies.
     */
    fun sanitizeAndValidateItem(raw: DetectedFoodItem): Pair<DetectedFoodItem, Boolean> {
        val grams = raw.estimatedGrams.coerceAtLeast(0f)
        val protein = round1(raw.proteinGrams.coerceAtLeast(0f))
        val carbs = round1(raw.carbsGrams.coerceAtLeast(0f))
        val fat = round1(raw.fatGrams.coerceAtLeast(0f))
        val fiber = round1(raw.fiberGrams.coerceIn(0f, carbs))
        val sugar = round1(raw.sugarGrams.coerceIn(0f, carbs))
        val satFat = round1(raw.saturatedFatGrams.coerceIn(0f, fat))

        val standardAtwater = computeAtwaterCalories(protein, carbs, fat)
        val fiberAtwater = computeFiberAdjustedAtwaterCalories(protein, carbs, fat, fiber)

        val reported = raw.caloriesKcal.coerceAtLeast(0)
        val minExpected = (minOf(standardAtwater, fiberAtwater) * 0.85f).roundToInt() - 12
        val maxExpected = (maxOf(standardAtwater, fiberAtwater) * 1.15f).roundToInt() + 12

        val isConsistent = reported in minExpected..maxExpected
        val finalCalories = if (isConsistent && reported > 0) {
            reported
        } else {
            // Reconcile to fiber-adjusted Atwater if fiber is substantial, otherwise standard 4-4-9
            if (fiber >= 2f) fiberAtwater else standardAtwater
        }

        val sanitized = raw.copy(
            estimatedGrams = round1(grams),
            proteinGrams = protein,
            carbsGrams = carbs,
            fatGrams = fat,
            fiberGrams = fiber,
            sugarGrams = sugar,
            saturatedFatGrams = satFat,
            caloriesKcal = finalCalories,
            confidence = raw.confidence.coerceIn(0f, 1f)
        )
        return sanitized to !isConsistent
    }

    /**
     * Deterministically rescales an item's nutritional profile when the user edits its weight in grams.
     * Never uses random values; preserves nutrient density per gram and reconciles Atwater calories.
     */
    fun scaleItemByGrams(item: DetectedFoodItem, newGrams: Float): DetectedFoodItem {
        val safeNewGrams = newGrams.coerceAtLeast(1f)
        val baseGrams = item.estimatedGrams.coerceAtLeast(1f)
        val factor = safeNewGrams / baseGrams

        val newProtein = round1(item.proteinGrams * factor)
        val newCarbs = round1(item.carbsGrams * factor)
        val newFat = round1(item.fatGrams * factor)
        val newFiber = round1((item.fiberGrams * factor).coerceIn(0f, newCarbs))
        val newSugar = round1((item.sugarGrams * factor).coerceIn(0f, newCarbs))
        val newSatFat = round1((item.saturatedFatGrams * factor).coerceIn(0f, newFat))
        val scaledKcal = (item.caloriesKcal * factor).roundToInt()

        val candidate = item.copy(
            estimatedGrams = round1(safeNewGrams),
            caloriesKcal = scaledKcal,
            proteinGrams = newProtein,
            carbsGrams = newCarbs,
            fatGrams = newFat,
            fiberGrams = newFiber,
            sugarGrams = newSugar,
            saturatedFatGrams = newSatFat,
            isWeightUncertain = false,
            weightRangeGrams = "",
            isUserEdited = true,
            referenceSource = "User Adjusted"
        )
        return sanitizeAndValidateItem(candidate).first
    }

    /**
     * Computes full meal totals strictly by summing individual validated food items and validates total
     * meal calories via Atwater (Protein*4 + Carbs*4 + Fat*9).
     */
    fun buildMealFromItems(
        mealName: String,
        rawItems: List<DetectedFoodItem>,
        vitamins: List<String> = emptyList(),
        minerals: List<String> = emptyList(),
        healthScore: Int = 80,
        summary: String = "",
        rickyTip: String = "",
        imageQuality: ImageQualityAssessment? = null
    ): FoodNutritionAnalysis {
        if (rawItems.isEmpty()) {
            return FoodNutritionAnalysis(
                foodName = mealName.ifBlank { "No Food Detected" },
                portionSize = "0g",
                foodDetected = false,
                items = emptyList(),
                imageQuality = imageQuality,
                noFoodReason = "No recognizable food items could be verified in this image."
            )
        }

        var anyItemReconciled = false
        val validatedItems = rawItems.map { item ->
            val (clean, reconciled) = sanitizeAndValidateItem(item)
            if (reconciled) anyItemReconciled = true
            clean
        }

        val totalGrams = round1(validatedItems.sumOf { it.estimatedGrams.toDouble() }.toFloat())
        val totalCalories = validatedItems.sumOf { it.caloriesKcal }
        val totalProtein = round1(validatedItems.sumOf { it.proteinGrams.toDouble() }.toFloat())
        val totalCarbs = round1(validatedItems.sumOf { it.carbsGrams.toDouble() }.toFloat())
        val totalFat = round1(validatedItems.sumOf { it.fatGrams.toDouble() }.toFloat())
        val totalFiber = round1(validatedItems.sumOf { it.fiberGrams.toDouble() }.toFloat())
        val totalSugar = round1(validatedItems.sumOf { it.sugarGrams.toDouble() }.toFloat())
        val totalSatFat = round1(validatedItems.sumOf { it.saturatedFatGrams.toDouble() }.toFloat())

        val standardAtwater = computeAtwaterCalories(totalProtein, totalCarbs, totalFat)
        val fiberAdjustedAtwater = computeFiberAdjustedAtwaterCalories(totalProtein, totalCarbs, totalFat, totalFiber)
        val baseline = if (totalFiber >= 3f) fiberAdjustedAtwater else standardAtwater

        val variancePct = if (baseline > 0) {
            (abs(totalCalories - baseline).toFloat() / baseline.toFloat()) * 100f
        } else 0f

        val withinTolerance = variancePct <= 12f || abs(totalCalories - baseline) <= 15

        val weightedConfidence = if (totalGrams > 0f) {
            (validatedItems.sumOf { (it.confidence * it.estimatedGrams).toDouble() } / totalGrams.toDouble()).toFloat()
        } else {
            validatedItems.map { it.confidence }.average().toFloat()
        }.coerceIn(0f, 1f)

        val uncertainNotes = validatedItems
            .filter { it.isUncertain || it.isWeightUncertain || it.confidence < 0.7f }
            .map { item ->
                val reason = item.uncertaintyReason?.takeIf { it.isNotBlank() }
                    ?: if (item.isWeightUncertain && item.weightRangeGrams.isNotBlank()) {
                        "Portion weight uncertain (${item.weightRangeGrams})"
                    } else {
                        "Visual identification uncertain (${(item.confidence * 100).roundToInt()}%)"
                    }
                "${item.name}: $reason"
            }

        val validationNote = when {
            anyItemReconciled ->
                "Calories reconciled to Atwater formula (P×4 + C×4 + F×9 = $standardAtwater kcal; fiber-adjusted $fiberAdjustedAtwater kcal)."
            withinTolerance ->
                "Atwater verified: $totalCalories kcal matches 4-4-9 macro energy ($standardAtwater kcal gross, $fiberAdjustedAtwater kcal fiber-adj)."
            else ->
                "Macro energy check: 4-4-9 predicts $standardAtwater kcal (fiber-adj $fiberAdjustedAtwater kcal)."
        }

        val derivedName = mealName.ifBlank {
            validatedItems.take(3).joinToString(", ") { it.name } +
                if (validatedItems.size > 3) " +${validatedItems.size - 3} more" else ""
        }

        return FoodNutritionAnalysis(
            foodName = derivedName,
            portionSize = "${validatedItems.size} item${if (validatedItems.size == 1) "" else "s"} (${totalGrams.roundToInt()}g total)",
            calories = totalCalories,
            proteinGrams = totalProtein,
            carbsGrams = totalCarbs,
            fatsGrams = totalFat,
            vitamins = vitamins,
            minerals = minerals,
            healthScore = healthScore.coerceIn(0, 100),
            summary = summary,
            rickyTip = rickyTip,
            foodDetected = true,
            items = validatedItems,
            totalGrams = totalGrams,
            fiberGrams = totalFiber,
            sugarGrams = totalSugar,
            saturatedFatGrams = totalSatFat,
            overallConfidence = weightedConfidence,
            atwaterValidation = AtwaterValidation(
                reportedCalories = totalCalories,
                atwaterCalories = standardAtwater,
                netCarbAdjustedCalories = fiberAdjustedAtwater,
                variancePercent = round1(variancePct),
                isWithinTolerance = withinTolerance,
                wasReconciled = anyItemReconciled,
                note = validationNote
            ),
            imageQuality = imageQuality,
            uncertainItemsSummary = uncertainNotes,
            noFoodReason = null
        )
    }

    fun recalculateMealAnalysis(
        current: FoodNutritionAnalysis,
        updatedItems: List<DetectedFoodItem>
    ): FoodNutritionAnalysis {
        return buildMealFromItems(
            mealName = current.foodName,
            rawItems = updatedItems,
            vitamins = current.vitamins,
            minerals = current.minerals,
            healthScore = current.healthScore,
            summary = current.summary,
            rickyTip = current.rickyTip,
            imageQuality = current.imageQuality
        )
    }

    fun round1(value: Float): Float = (value * 10f).roundToInt() / 10f
}
