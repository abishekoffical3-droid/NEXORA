package com.example

import com.example.ai.DetectedFoodItem
import com.example.ai.FoodCategory
import com.example.ai.FoodNutritionCalculator
import com.example.ai.PreparationState
import com.example.ai.UsdaFoodReferenceDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Accuracy & scientific validation suite for the upgraded NEXORA Food Scanner.
 * Tests all 6 requested meal archetypes against verified USDA FoodData Central / ICMR-NIN densities,
 * per-item summation invariants, Atwater calorie validation (P*4 + C*4 + F*9), raw vs cooked handling,
 * manual gram portion scaling, and zero-hallucination no-food handling.
 */
class FoodScannerAccuracyTest {

    @Test
    fun meal1_riceDalVegetablesAndCookingOil_accurateSummationAndAtwaterCheck() {
        val rice = itemFromUsda("white_rice_cooked", 160f)
        val dal = itemFromUsda("dal_lentils_cooked", 180f)
        val veg = itemFromUsda("mixed_veg_cooked", 120f)
        val oil = itemFromUsda("olive_oil", 8f)

        val meal = FoodNutritionCalculator.buildMealFromItems(
            mealName = "Rice, Dal & Mixed Vegetables",
            rawItems = listOf(rice, dal, veg, oil)
        )

        assertTrue(meal.foodDetected)
        assertEquals(4, meal.items.size)
        assertEquals(468f, meal.totalGrams, 0.2f)

        // Strict sum of individual item values
        val expectedCalories = rice.caloriesKcal + dal.caloriesKcal + veg.caloriesKcal + oil.caloriesKcal
        assertEquals(expectedCalories, meal.calories)
        // Realistic range for 160g rice (208 kcal) + 180g dal (209 kcal) + 120g sabzi (86 kcal) + 8g oil (71 kcal) ~ 574 kcal
        assertTrue("Total calories (${meal.calories}) should be realistic (~540-610 kcal)", meal.calories in 540..610)

        assertEquals(
            rice.proteinGrams + dal.proteinGrams + veg.proteinGrams + oil.proteinGrams,
            meal.proteinGrams,
            0.2f
        )
        assertEquals(
            rice.fiberGrams + dal.fiberGrams + veg.fiberGrams + oil.fiberGrams,
            meal.fiberGrams,
            0.2f
        )
        assertTrue("Dal + veg should contribute >15g fiber", meal.fiberGrams > 15f)
        assertNotNull(meal.atwaterValidation)
        assertTrue(meal.atwaterValidation!!.isWithinTolerance)
    }

    @Test
    fun meal2_tofuAvocadoAndSweetPotato_verifiedHighFiberAndFatBalance() {
        val tofu = itemFromUsda("tofu_firm", 120f)
        val avocado = itemFromUsda("avocado_raw", 75f)
        val sweetPotato = itemFromUsda("sweet_potato_baked", 130f)

        val meal = FoodNutritionCalculator.buildMealFromItems(
            mealName = "Tofu, Avocado & Sweet Potato Plate",
            rawItems = listOf(tofu, avocado, sweetPotato)
        )

        assertTrue(meal.foodDetected)
        assertEquals(3, meal.items.size)
        assertEquals(tofu.caloriesKcal + avocado.caloriesKcal + sweetPotato.caloriesKcal, meal.calories)
        // ~173 (tofu) + ~120 (avocado) + ~117 (sweet potato) = ~410 kcal
        assertTrue("Expected ~390-430 kcal, got ${meal.calories}", meal.calories in 390..430)
        assertTrue("Avocado + sweet potato fiber should be >= 11g", meal.fiberGrams >= 11f)
        assertTrue("Natural sugars from sweet potato (~8.5g) should be tracked", meal.sugarGrams >= 8f)
        assertTrue("Protein from tofu should keep total >= 21g", meal.proteinGrams >= 21f)
        assertTrue(meal.atwaterValidation!!.isWithinTolerance)
    }

    @Test
    fun meal3_chickenRiceAndBroccoli_leanProteinAndSteamedStateAccuracy() {
        val chicken = itemFromUsda("chicken_breast_grilled", 140f)
        val brownRice = itemFromUsda("brown_rice_cooked", 160f)
        val broccoli = itemFromUsda("broccoli_cooked", 90f)

        val meal = FoodNutritionCalculator.buildMealFromItems(
            mealName = "Grilled Chicken, Brown Rice & Broccoli",
            rawItems = listOf(chicken, brownRice, broccoli)
        )

        assertTrue(meal.foodDetected)
        assertEquals(PreparationState.GRILLED, chicken.preparationState)
        assertEquals(PreparationState.COOKED, brownRice.preparationState)
        assertEquals(PreparationState.STEAMED, broccoli.preparationState)

        // ~231 (chicken) + ~179 (brown rice) + ~32 (broccoli) = ~442 kcal
        assertTrue("Expected ~420-465 kcal, got ${meal.calories}", meal.calories in 420..465)
        assertTrue("Expected high lean protein >= 47g, got ${meal.proteinGrams}", meal.proteinGrams >= 47f)
        assertTrue("Broccoli + brown rice fiber >= 5.5g", meal.fiberGrams >= 5.5f)
        assertTrue(meal.saturatedFatGrams < 3.0f)
    }

    @Test
    fun meal4_eggsBreadAndFruits_breakfastMacroAndSugarValidation() {
        val eggs = itemFromUsda("egg_boiled", 100f) // ~2 large boiled eggs
        val bread = itemFromUsda("whole_wheat_bread", 60f) // 2 slices
        val banana = itemFromUsda("banana_raw", 118f) // 1 medium banana

        val meal = FoodNutritionCalculator.buildMealFromItems(
            mealName = "Boiled Eggs, Whole Wheat Toast & Banana",
            rawItems = listOf(eggs, bread, banana)
        )

        assertTrue(meal.foodDetected)
        assertEquals(3, meal.items.size)
        // ~155 + ~148 + ~105 = ~408 kcal
        assertTrue("Expected ~390-430 kcal, got ${meal.calories}", meal.calories in 390..430)
        assertTrue("Fruit + bread sugar should exceed 15g", meal.sugarGrams > 15f)
        assertTrue("Eggs + whole wheat bread protein >= 20g", meal.proteinGrams >= 20f)
        assertTrue(meal.atwaterValidation!!.isWithinTolerance)
    }

    @Test
    fun meal5_mixedVegetableSaladWithVinaigrette_capturesHiddenDressingCalories() {
        val greens = itemFromUsda("mixed_salad_greens", 80f)
        val tomato = itemFromUsda("tomato_raw", 75f)
        val cucumber = itemFromUsda("cucumber_raw", 60f)
        val dressing = itemFromUsda("vinaigrette_dressing", 20f)

        val meal = FoodNutritionCalculator.buildMealFromItems(
            mealName = "Mixed Garden Salad with Vinaigrette",
            rawItems = listOf(greens, tomato, cucumber, dressing)
        )

        assertTrue(meal.foodDetected)
        assertEquals(4, meal.items.size)
        // Greens (14) + Tomato (14) + Cucumber (9) + Vinaigrette (64) = ~101 kcal
        assertTrue("Dressing should drive majority of fat/kcal (~90-120 kcal), got ${meal.calories}", meal.calories in 90..120)
        assertEquals(PreparationState.RAW, greens.preparationState)
        assertEquals(FoodCategory.SAUCE_OIL, dressing.category)
        assertTrue(meal.fatsGrams >= 6f)
    }

    @Test
    fun meal6_quinoaChickpeaGrainBowl_multiComponentAccuracy() {
        val quinoa = itemFromUsda("quinoa_cooked", 150f)
        val chickpeas = itemFromUsda("chickpeas_cooked", 100f)
        val avocado = itemFromUsda("avocado_raw", 75f)
        val greens = itemFromUsda("mixed_salad_greens", 60f)

        val meal = FoodNutritionCalculator.buildMealFromItems(
            mealName = "Quinoa & Chickpea Power Bowl",
            rawItems = listOf(quinoa, chickpeas, avocado, greens)
        )

        assertTrue(meal.foodDetected)
        assertEquals(4, meal.items.size)
        // ~180 + ~164 + ~120 + ~11 = ~475 kcal
        assertTrue("Expected ~450-500 kcal, got ${meal.calories}", meal.calories in 450..500)
        assertTrue("Grain bowl fiber should be >= 17g, got ${meal.fiberGrams}", meal.fiberGrams >= 17f)
        assertTrue("Grain + legume complete protein >= 17g", meal.proteinGrams >= 17f)
        assertTrue(meal.atwaterValidation!!.isWithinTolerance)
    }

    @Test
    fun atwaterValidationReconcilesGrossCalorieMismatch() {
        val mismatchedItem = DetectedFoodItem(
            name = "Cooked Basmati Rice",
            category = FoodCategory.GRAIN,
            preparationState = PreparationState.COOKED,
            estimatedGrams = 150f,
            caloriesKcal = 999, // Deliberately corrupted calorie figure
            proteinGrams = 4.0f,
            carbsGrams = 42.0f,
            fatGrams = 0.5f,
            fiberGrams = 0.6f
        )

        val (sanitized, wasReconciled) = FoodNutritionCalculator.sanitizeAndValidateItem(mismatchedItem)
        assertTrue("Gross Atwater mismatch must trigger reconciliation", wasReconciled)
        // 4*4 + 42*4 + 0.5*9 = 16 + 168 + 4.5 = 189 kcal
        assertEquals(189, sanitized.caloriesKcal)

        val meal = FoodNutritionCalculator.buildMealFromItems("Test Rice", listOf(mismatchedItem))
        assertTrue(meal.atwaterValidation!!.wasReconciled)
        assertEquals(189, meal.calories)
    }

    @Test
    fun manualGramEditScalesAllNutrientsProportionally() {
        val originalRice = itemFromUsda("white_rice_cooked", 100f)
        val editedRice = FoodNutritionCalculator.scaleItemByGrams(originalRice, 200f)

        assertEquals(200f, editedRice.estimatedGrams, 0.1f)
        assertEquals(originalRice.caloriesKcal * 2, editedRice.caloriesKcal)
        assertEquals(originalRice.proteinGrams * 2f, editedRice.proteinGrams, 0.2f)
        assertEquals(originalRice.carbsGrams * 2f, editedRice.carbsGrams, 0.2f)
        assertTrue(editedRice.isUserEdited)
    }

    @Test
    fun emptyItemsReturnsHonestNoFoodDetectedStateWithoutFakeCalories() {
        val meal = FoodNutritionCalculator.buildMealFromItems(
            mealName = "",
            rawItems = emptyList()
        )
        assertFalse(meal.foodDetected)
        assertEquals(0, meal.calories)
        assertEquals(0f, meal.proteinGrams, 0.01f)
        assertTrue(meal.items.isEmpty())
        assertNotNull(meal.noFoodReason)
    }

    private fun itemFromUsda(entryId: String, grams: Float): DetectedFoodItem {
        val entry = UsdaFoodReferenceDatabase.entries.first { it.id == entryId }
        return UsdaFoodReferenceDatabase.createItemFromReference(entry, grams)
    }
}
