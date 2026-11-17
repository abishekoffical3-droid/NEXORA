package com.example.ai

import kotlin.math.roundToInt

/**
 * Verified per-100g food composition reference derived from USDA FoodData Central & ICMR-NIN tables.
 * Used for:
 * 1. Cross-validating AI vision macro density against real-world cooked vs raw composition.
 * 2. Empowering users to add or correct missing ingredients in grams with verified nutrition math.
 */
data class UsdaFoodReferenceEntry(
    val id: String,
    val displayName: String,
    val aliases: List<String>,
    val category: FoodCategory,
    val preparationState: PreparationState,
    val kcalPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val fiberPer100g: Float,
    val sugarPer100g: Float,
    val satFatPer100g: Float,
    val defaultServingGrams: Float,
    val defaultServingLabel: String
)

object UsdaFoodReferenceDatabase {

    val entries: List<UsdaFoodReferenceEntry> = listOf(
        // Grains & Starches (Cooked vs Raw explicitly distinguished)
        UsdaFoodReferenceEntry(
            id = "white_rice_cooked",
            displayName = "White Rice (Cooked)",
            aliases = listOf("white rice", "steamed rice", "basmati rice", "jasmine rice", "bhat", "chawal"),
            category = FoodCategory.GRAIN,
            preparationState = PreparationState.COOKED,
            kcalPer100g = 130f, proteinPer100g = 2.7f, carbsPer100g = 28.2f, fatPer100g = 0.3f,
            fiberPer100g = 0.4f, sugarPer100g = 0.1f, satFatPer100g = 0.1f,
            defaultServingGrams = 160f, defaultServingLabel = "~1 cup cooked"
        ),
        UsdaFoodReferenceEntry(
            id = "brown_rice_cooked",
            displayName = "Brown Rice (Cooked)",
            aliases = listOf("brown rice", "whole grain rice"),
            category = FoodCategory.GRAIN,
            preparationState = PreparationState.COOKED,
            kcalPer100g = 112f, proteinPer100g = 2.6f, carbsPer100g = 23.5f, fatPer100g = 0.9f,
            fiberPer100g = 1.8f, sugarPer100g = 0.4f, satFatPer100g = 0.2f,
            defaultServingGrams = 160f, defaultServingLabel = "~1 cup cooked"
        ),
        UsdaFoodReferenceEntry(
            id = "quinoa_cooked",
            displayName = "Quinoa (Cooked)",
            aliases = listOf("quinoa", "grain bowl quinoa"),
            category = FoodCategory.GRAIN,
            preparationState = PreparationState.COOKED,
            kcalPer100g = 120f, proteinPer100g = 4.4f, carbsPer100g = 21.3f, fatPer100g = 1.9f,
            fiberPer100g = 2.8f, sugarPer100g = 0.9f, satFatPer100g = 0.2f,
            defaultServingGrams = 150f, defaultServingLabel = "~3/4 cup cooked"
        ),
        UsdaFoodReferenceEntry(
            id = "sweet_potato_baked",
            displayName = "Sweet Potato (Baked/Cooked)",
            aliases = listOf("sweet potato", "roasted sweet potato", "yam", "shakarkand"),
            category = FoodCategory.GRAIN,
            preparationState = PreparationState.BAKED,
            kcalPer100g = 90f, proteinPer100g = 2.0f, carbsPer100g = 20.7f, fatPer100g = 0.2f,
            fiberPer100g = 3.3f, sugarPer100g = 6.5f, satFatPer100g = 0.0f,
            defaultServingGrams = 130f, defaultServingLabel = "1 medium sweet potato"
        ),
        UsdaFoodReferenceEntry(
            id = "whole_wheat_bread",
            displayName = "Whole Wheat Bread",
            aliases = listOf("whole wheat bread", "brown bread", "toast", "bread slice"),
            category = FoodCategory.GRAIN,
            preparationState = PreparationState.BAKED,
            kcalPer100g = 247f, proteinPer100g = 13.0f, carbsPer100g = 41.0f, fatPer100g = 3.4f,
            fiberPer100g = 6.8f, sugarPer100g = 4.3f, satFatPer100g = 0.7f,
            defaultServingGrams = 60f, defaultServingLabel = "2 slices"
        ),
        UsdaFoodReferenceEntry(
            id = "white_bread",
            displayName = "White Bread / Toast",
            aliases = listOf("white bread", "sourdough", "sandwich bread"),
            category = FoodCategory.GRAIN,
            preparationState = PreparationState.BAKED,
            kcalPer100g = 265f, proteinPer100g = 9.0f, carbsPer100g = 49.0f, fatPer100g = 3.2f,
            fiberPer100g = 2.7f, sugarPer100g = 5.0f, satFatPer100g = 0.7f,
            defaultServingGrams = 60f, defaultServingLabel = "2 slices"
        ),
        UsdaFoodReferenceEntry(
            id = "roti_chapati",
            displayName = "Whole Wheat Roti / Chapati",
            aliases = listOf("roti", "chapati", "phulka", "whole wheat flatbread"),
            category = FoodCategory.GRAIN,
            preparationState = PreparationState.COOKED,
            kcalPer100g = 297f, proteinPer100g = 9.8f, carbsPer100g = 55.8f, fatPer100g = 3.7f,
            fiberPer100g = 9.0f, sugarPer100g = 1.5f, satFatPer100g = 0.6f,
            defaultServingGrams = 40f, defaultServingLabel = "1 medium roti"
        ),
        UsdaFoodReferenceEntry(
            id = "oats_cooked",
            displayName = "Oatmeal (Cooked in Water)",
            aliases = listOf("oats", "oatmeal", "porridge"),
            category = FoodCategory.GRAIN,
            preparationState = PreparationState.COOKED,
            kcalPer100g = 71f, proteinPer100g = 2.5f, carbsPer100g = 12.0f, fatPer100g = 1.5f,
            fiberPer100g = 1.7f, sugarPer100g = 0.3f, satFatPer100g = 0.3f,
            defaultServingGrams = 200f, defaultServingLabel = "1 bowl"
        ),

        // Legumes, Dal & Soy
        UsdaFoodReferenceEntry(
            id = "dal_lentils_cooked",
            displayName = "Dal / Cooked Lentils",
            aliases = listOf("dal", "lentils", "yellow dal", "moong dal", "masoor dal", "toor dal", "daal"),
            category = FoodCategory.TOFU_LEGUME,
            preparationState = PreparationState.COOKED,
            kcalPer100g = 116f, proteinPer100g = 9.0f, carbsPer100g = 20.1f, fatPer100g = 0.4f,
            fiberPer100g = 7.9f, sugarPer100g = 1.8f, satFatPer100g = 0.1f,
            defaultServingGrams = 180f, defaultServingLabel = "1 medium bowl"
        ),
        UsdaFoodReferenceEntry(
            id = "tofu_firm",
            displayName = "Firm Tofu",
            aliases = listOf("tofu", "pan-seared tofu", "grilled tofu", "bean curd"),
            category = FoodCategory.TOFU_LEGUME,
            preparationState = PreparationState.COOKED,
            kcalPer100g = 144f, proteinPer100g = 15.8f, carbsPer100g = 3.9f, fatPer100g = 8.7f,
            fiberPer100g = 2.3f, sugarPer100g = 0.7f, satFatPer100g = 1.3f,
            defaultServingGrams = 120f, defaultServingLabel = "~1/2 block"
        ),
        UsdaFoodReferenceEntry(
            id = "chickpeas_cooked",
            displayName = "Chickpeas (Cooked)",
            aliases = listOf("chickpeas", "garbanzo", "chana"),
            category = FoodCategory.TOFU_LEGUME,
            preparationState = PreparationState.COOKED,
            kcalPer100g = 164f, proteinPer100g = 8.9f, carbsPer100g = 27.4f, fatPer100g = 2.6f,
            fiberPer100g = 7.6f, sugarPer100g = 4.8f, satFatPer100g = 0.3f,
            defaultServingGrams = 100f, defaultServingLabel = "~2/3 cup"
        ),
        UsdaFoodReferenceEntry(
            id = "black_beans_cooked",
            displayName = "Black Beans (Cooked)",
            aliases = listOf("black beans", "rajma", "kidney beans"),
            category = FoodCategory.TOFU_LEGUME,
            preparationState = PreparationState.COOKED,
            kcalPer100g = 132f, proteinPer100g = 8.9f, carbsPer100g = 23.7f, fatPer100g = 0.5f,
            fiberPer100g = 8.7f, sugarPer100g = 0.3f, satFatPer100g = 0.1f,
            defaultServingGrams = 100f, defaultServingLabel = "~1/2 cup"
        ),

        // Meat, Fish, Eggs & Dairy
        UsdaFoodReferenceEntry(
            id = "chicken_breast_grilled",
            displayName = "Chicken Breast (Grilled/Cooked)",
            aliases = listOf("chicken breast", "grilled chicken", "chicken", "roasted chicken"),
            category = FoodCategory.MEAT_POULTRY_FISH,
            preparationState = PreparationState.GRILLED,
            kcalPer100g = 165f, proteinPer100g = 31.0f, carbsPer100g = 0.0f, fatPer100g = 3.6f,
            fiberPer100g = 0.0f, sugarPer100g = 0.0f, satFatPer100g = 1.0f,
            defaultServingGrams = 140f, defaultServingLabel = "1 medium breast"
        ),
        UsdaFoodReferenceEntry(
            id = "salmon_cooked",
            displayName = "Salmon Fillet (Cooked)",
            aliases = listOf("salmon", "baked salmon", "grilled salmon"),
            category = FoodCategory.MEAT_POULTRY_FISH,
            preparationState = PreparationState.BAKED,
            kcalPer100g = 206f, proteinPer100g = 22.1f, carbsPer100g = 0.0f, fatPer100g = 12.4f,
            fiberPer100g = 0.0f, sugarPer100g = 0.0f, satFatPer100g = 2.5f,
            defaultServingGrams = 140f, defaultServingLabel = "1 fillet"
        ),
        UsdaFoodReferenceEntry(
            id = "egg_boiled",
            displayName = "Whole Egg (Boiled/Poached)",
            aliases = listOf("boiled egg", "egg", "eggs", "poached egg"),
            category = FoodCategory.EGG,
            preparationState = PreparationState.BOILED,
            kcalPer100g = 155f, proteinPer100g = 12.6f, carbsPer100g = 1.1f, fatPer100g = 10.6f,
            fiberPer100g = 0.0f, sugarPer100g = 1.1f, satFatPer100g = 3.3f,
            defaultServingGrams = 100f, defaultServingLabel = "2 large eggs"
        ),
        UsdaFoodReferenceEntry(
            id = "egg_fried_scrambled",
            displayName = "Fried / Scrambled Egg",
            aliases = listOf("fried egg", "scrambled egg", "omelette", "omelet"),
            category = FoodCategory.EGG,
            preparationState = PreparationState.FRIED,
            kcalPer100g = 196f, proteinPer100g = 13.6f, carbsPer100g = 1.6f, fatPer100g = 14.8f,
            fiberPer100g = 0.0f, sugarPer100g = 1.4f, satFatPer100g = 4.3f,
            defaultServingGrams = 100f, defaultServingLabel = "2 large eggs"
        ),
        UsdaFoodReferenceEntry(
            id = "paneer_cooked",
            displayName = "Paneer (Cottage Cheese)",
            aliases = listOf("paneer", "cottage cheese"),
            category = FoodCategory.DAIRY,
            preparationState = PreparationState.COOKED,
            kcalPer100g = 265f, proteinPer100g = 18.3f, carbsPer100g = 1.2f, fatPer100g = 20.8f,
            fiberPer100g = 0.0f, sugarPer100g = 1.2f, satFatPer100g = 13.2f,
            defaultServingGrams = 80f, defaultServingLabel = "~1/2 cup cubes"
        ),
        UsdaFoodReferenceEntry(
            id = "greek_yogurt_plain",
            displayName = "Plain Greek Yogurt",
            aliases = listOf("greek yogurt", "yogurt", "curd", "dahi"),
            category = FoodCategory.DAIRY,
            preparationState = PreparationState.RAW,
            kcalPer100g = 73f, proteinPer100g = 10.0f, carbsPer100g = 3.6f, fatPer100g = 1.9f,
            fiberPer100g = 0.0f, sugarPer100g = 3.6f, satFatPer100g = 1.2f,
            defaultServingGrams = 150f, defaultServingLabel = "1 small cup"
        ),

        // Vegetables (Cooked vs Raw)
        UsdaFoodReferenceEntry(
            id = "broccoli_cooked",
            displayName = "Broccoli (Steamed/Cooked)",
            aliases = listOf("broccoli", "steamed broccoli", "roasted broccoli"),
            category = FoodCategory.VEGETABLE,
            preparationState = PreparationState.STEAMED,
            kcalPer100g = 35f, proteinPer100g = 2.4f, carbsPer100g = 7.2f, fatPer100g = 0.4f,
            fiberPer100g = 3.3f, sugarPer100g = 1.4f, satFatPer100g = 0.1f,
            defaultServingGrams = 90f, defaultServingLabel = "~1 cup florets"
        ),
        UsdaFoodReferenceEntry(
            id = "mixed_veg_cooked",
            displayName = "Mixed Stir-Fry / Curry Vegetables",
            aliases = listOf("mixed vegetables", "sabzi", "tarkari", "stir fry vegetables", "vegetables"),
            category = FoodCategory.VEGETABLE,
            preparationState = PreparationState.COOKED,
            kcalPer100g = 72f, proteinPer100g = 2.6f, carbsPer100g = 10.5f, fatPer100g = 2.5f,
            fiberPer100g = 3.2f, sugarPer100g = 4.1f, satFatPer100g = 0.4f,
            defaultServingGrams = 120f, defaultServingLabel = "~1 cup"
        ),
        UsdaFoodReferenceEntry(
            id = "mixed_salad_greens",
            displayName = "Mixed Salad Greens (Raw)",
            aliases = listOf("salad", "lettuce", "mixed greens", "spinach", "arugula", "kale"),
            category = FoodCategory.VEGETABLE,
            preparationState = PreparationState.RAW,
            kcalPer100g = 18f, proteinPer100g = 1.5f, carbsPer100g = 3.3f, fatPer100g = 0.2f,
            fiberPer100g = 1.8f, sugarPer100g = 1.0f, satFatPer100g = 0.0f,
            defaultServingGrams = 80f, defaultServingLabel = "~2 cups raw"
        ),
        UsdaFoodReferenceEntry(
            id = "tomato_raw",
            displayName = "Fresh Tomato (Raw)",
            aliases = listOf("tomato", "cherry tomatoes", "tomatoes"),
            category = FoodCategory.VEGETABLE,
            preparationState = PreparationState.RAW,
            kcalPer100g = 18f, proteinPer100g = 0.9f, carbsPer100g = 3.9f, fatPer100g = 0.2f,
            fiberPer100g = 1.2f, sugarPer100g = 2.6f, satFatPer100g = 0.0f,
            defaultServingGrams = 75f, defaultServingLabel = "1 small tomato"
        ),
        UsdaFoodReferenceEntry(
            id = "cucumber_raw",
            displayName = "Cucumber (Raw)",
            aliases = listOf("cucumber", "sliced cucumber"),
            category = FoodCategory.VEGETABLE,
            preparationState = PreparationState.RAW,
            kcalPer100g = 15f, proteinPer100g = 0.7f, carbsPer100g = 3.6f, fatPer100g = 0.1f,
            fiberPer100g = 0.5f, sugarPer100g = 1.7f, satFatPer100g = 0.0f,
            defaultServingGrams = 60f, defaultServingLabel = "~1/2 cup sliced"
        ),

        // Fruits
        UsdaFoodReferenceEntry(
            id = "avocado_raw",
            displayName = "Avocado (Raw)",
            aliases = listOf("avocado", "sliced avocado", "guacamole"),
            category = FoodCategory.FRUIT,
            preparationState = PreparationState.RAW,
            kcalPer100g = 160f, proteinPer100g = 2.0f, carbsPer100g = 8.5f, fatPer100g = 14.7f,
            fiberPer100g = 6.7f, sugarPer100g = 0.7f, satFatPer100g = 2.1f,
            defaultServingGrams = 75f, defaultServingLabel = "1/2 medium avocado"
        ),
        UsdaFoodReferenceEntry(
            id = "banana_raw",
            displayName = "Banana (Raw)",
            aliases = listOf("banana", "sliced banana"),
            category = FoodCategory.FRUIT,
            preparationState = PreparationState.RAW,
            kcalPer100g = 89f, proteinPer100g = 1.1f, carbsPer100g = 22.8f, fatPer100g = 0.3f,
            fiberPer100g = 2.6f, sugarPer100g = 12.2f, satFatPer100g = 0.1f,
            defaultServingGrams = 118f, defaultServingLabel = "1 medium banana"
        ),
        UsdaFoodReferenceEntry(
            id = "apple_raw",
            displayName = "Apple (Raw)",
            aliases = listOf("apple", "sliced apple"),
            category = FoodCategory.FRUIT,
            preparationState = PreparationState.RAW,
            kcalPer100g = 52f, proteinPer100g = 0.3f, carbsPer100g = 13.8f, fatPer100g = 0.2f,
            fiberPer100g = 2.4f, sugarPer100g = 10.4f, satFatPer100g = 0.0f,
            defaultServingGrams = 150f, defaultServingLabel = "1 medium apple"
        ),
        UsdaFoodReferenceEntry(
            id = "mixed_berries_raw",
            displayName = "Mixed Berries (Raw)",
            aliases = listOf("berries", "blueberries", "strawberries", "raspberries"),
            category = FoodCategory.FRUIT,
            preparationState = PreparationState.RAW,
            kcalPer100g = 48f, proteinPer100g = 0.8f, carbsPer100g = 11.2f, fatPer100g = 0.3f,
            fiberPer100g = 3.0f, sugarPer100g = 7.4f, satFatPer100g = 0.0f,
            defaultServingGrams = 100f, defaultServingLabel = "~2/3 cup"
        ),

        // Oils, Butter & Dressings
        UsdaFoodReferenceEntry(
            id = "olive_oil",
            displayName = "Olive / Cooking Oil",
            aliases = listOf("olive oil", "oil", "cooking oil", "vegetable oil"),
            category = FoodCategory.SAUCE_OIL,
            preparationState = PreparationState.RAW,
            kcalPer100g = 884f, proteinPer100g = 0.0f, carbsPer100g = 0.0f, fatPer100g = 100.0f,
            fiberPer100g = 0.0f, sugarPer100g = 0.0f, satFatPer100g = 13.8f,
            defaultServingGrams = 10f, defaultServingLabel = "~2 tsp"
        ),
        UsdaFoodReferenceEntry(
            id = "vinaigrette_dressing",
            displayName = "Salad Vinaigrette Dressing",
            aliases = listOf("vinaigrette", "salad dressing", "dressing", "tahini dressing"),
            category = FoodCategory.SAUCE_OIL,
            preparationState = PreparationState.RAW,
            kcalPer100g = 320f, proteinPer100g = 0.5f, carbsPer100g = 8.0f, fatPer100g = 32.0f,
            fiberPer100g = 0.2f, sugarPer100g = 5.0f, satFatPer100g = 4.5f,
            defaultServingGrams = 20f, defaultServingLabel = "~1.5 tbsp"
        ),
        UsdaFoodReferenceEntry(
            id = "ghee_butter",
            displayName = "Butter / Ghee",
            aliases = listOf("ghee", "butter", "clarified butter"),
            category = FoodCategory.SAUCE_OIL,
            preparationState = PreparationState.COOKED,
            kcalPer100g = 876f, proteinPer100g = 0.3f, carbsPer100g = 0.0f, fatPer100g = 99.0f,
            fiberPer100g = 0.0f, sugarPer100g = 0.0f, satFatPer100g = 61.9f,
            defaultServingGrams = 10f, defaultServingLabel = "~2 tsp"
        )
    )

    fun createItemFromReference(entry: UsdaFoodReferenceEntry, grams: Float = entry.defaultServingGrams): DetectedFoodItem {
        val factor = grams.coerceAtLeast(1f) / 100f
        val protein = FoodNutritionCalculator.round1(entry.proteinPer100g * factor)
        val carbs = FoodNutritionCalculator.round1(entry.carbsPer100g * factor)
        val fat = FoodNutritionCalculator.round1(entry.fatPer100g * factor)
        val fiber = FoodNutritionCalculator.round1(entry.fiberPer100g * factor)
        val sugar = FoodNutritionCalculator.round1(entry.sugarPer100g * factor)
        val satFat = FoodNutritionCalculator.round1(entry.satFatPer100g * factor)
        val kcal = (entry.kcalPer100g * factor).roundToInt()

        return FoodNutritionCalculator.sanitizeAndValidateItem(
            DetectedFoodItem(
                name = entry.displayName,
                category = entry.category,
                preparationState = entry.preparationState,
                preparationMethod = entry.preparationState.label,
                estimatedGrams = FoodNutritionCalculator.round1(grams),
                servingDescription = entry.defaultServingLabel,
                isWeightUncertain = false,
                confidence = 1.0f,
                isUncertain = false,
                caloriesKcal = kcal,
                proteinGrams = protein,
                carbsGrams = carbs,
                fatGrams = fat,
                fiberGrams = fiber,
                sugarGrams = sugar,
                saturatedFatGrams = satFat,
                referenceSource = "USDA FoodData Central",
                isUserEdited = true
            )
        ).first
    }

    fun search(query: String): List<UsdaFoodReferenceEntry> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return entries
        return entries.filter { entry ->
            entry.displayName.lowercase().contains(q) ||
                entry.aliases.any { it.contains(q) } ||
                entry.category.displayName.lowercase().contains(q)
        }
    }

    fun findBestMatch(name: String, prepState: PreparationState = PreparationState.UNKNOWN): UsdaFoodReferenceEntry? {
        val q = name.trim().lowercase()
        if (q.isBlank()) return null

        val exactOrAliasMatches = entries.filter { entry ->
            entry.displayName.lowercase() == q ||
                entry.aliases.any { alias -> q.contains(alias) || alias.contains(q) }
        }
        if (exactOrAliasMatches.isEmpty()) return null

        return exactOrAliasMatches.firstOrNull { it.preparationState == prepState }
            ?: exactOrAliasMatches.first()
    }
}
