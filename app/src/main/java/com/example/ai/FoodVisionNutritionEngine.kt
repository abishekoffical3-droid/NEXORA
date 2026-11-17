package com.example.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "FoodVisionEngine"

class FoodVisionNutritionEngine {

    private val foodItemSchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string(description = "Specific ingredient or food item name, e.g. 'Cooked Basmati Rice' or 'Pan-Seared Firm Tofu'"),
            "category" to Schema.enumeration(
                values = listOf(
                    "GRAIN", "VEGETABLE", "FRUIT", "MEAT_POULTRY_FISH",
                    "EGG", "TOFU_LEGUME", "DAIRY", "SAUCE_OIL", "NUT_SEED", "MIXED_DISH", "OTHER"
                ),
                description = "Ingredient food category"
            ),
            "preparationState" to Schema.enumeration(
                values = listOf("COOKED", "RAW", "GRILLED", "STEAMED", "BAKED", "ROASTED", "FRIED", "BOILED", "UNKNOWN"),
                description = "Visual cooking or raw state"
            ),
            "preparationMethod" to Schema.string(description = "Concise visual prep method, e.g. 'Steamed', 'Pan-seared in oil', 'Fresh sliced'"),
            "estimatedGrams" to Schema.double(description = "Realistic portion weight in grams based on plate/bowl geometry"),
            "servingDescription" to Schema.string(description = "Human serving estimate, e.g. '~1 cup', '1 medium fillet'"),
            "isWeightUncertain" to Schema.boolean(description = "True if depth, bowl depth, or hidden layers make weight hard to judge"),
            "weightRangeGrams" to Schema.string(description = "Plausible range if uncertain, e.g. '120-180g', else empty string"),
            "confidence" to Schema.double(description = "Identification confidence from 0.0 to 1.0"),
            "isUncertain" to Schema.boolean(description = "True if ingredient could be a visually similar alternative (e.g. tofu vs paneer, oil sheen)"),
            "uncertaintyReason" to Schema.string(description = "Why weight or identity is uncertain, or null", nullable = true),
            "caloriesKcal" to Schema.integer(description = "Calories (kcal) for this exact gram weight using USDA FoodData Central density"),
            "proteinGrams" to Schema.double(description = "Protein in grams for this weight"),
            "carbsGrams" to Schema.double(description = "Total carbohydrates in grams for this weight"),
            "fatGrams" to Schema.double(description = "Total fat in grams for this weight"),
            "fiberGrams" to Schema.double(description = "Dietary fiber in grams for this weight"),
            "sugarGrams" to Schema.double(description = "Total sugars in grams for this weight"),
            "saturatedFatGrams" to Schema.double(description = "Saturated fat in grams for this weight")
        ),
        optionalProperties = listOf("uncertaintyReason")
    )

    private val foodAnalysisSchema = Schema.obj(
        properties = mapOf(
            "foodDetected" to Schema.boolean(description = "True only if actual food/drink is clearly visible; false for empty/dark/non-food scenes"),
            "noFoodReason" to Schema.string(description = "Explanation if foodDetected is false, else null", nullable = true),
            "mealName" to Schema.string(description = "Concise accurate meal title based only on visible foods"),
            "healthScore" to Schema.integer(description = "Nutritional balance score from 0 to 100"),
            "summary" to Schema.string(description = "Brief factual nutritional summary of visible ingredients"),
            "rickyTip" to Schema.string(description = "Actionable, practical metabolic or pairing tip"),
            "vitamins" to Schema.array(
                items = Schema.string(),
                description = "3-5 genuinely present vitamins with realistic % DV based on visible foods"
            ),
            "minerals" to Schema.array(
                items = Schema.string(),
                description = "3-5 genuinely present minerals with realistic % DV based on visible foods"
            ),
            "items" to Schema.array(
                items = foodItemSchema,
                description = "Every distinct visible food item, sauce, or visible cooking oil analyzed separately"
            )
        ),
        optionalProperties = listOf("noFoodReason")
    )

    private val systemPrompt = """
        You are an expert Computer Vision Engineer and Clinical Nutrition Data Scientist using USDA FoodData Central & ICMR-NIN composition standards.
        Analyze the supplied meal photograph carefully and output structured JSON matching the schema.

        STRICT VISUAL & SCIENTIFIC RULES:
        1. MULTI-ITEM DETECTION:
           - Separate EVERY distinct visible component on the plate, thali, or bowl (grains, dal/legumes, each vegetable, fruit, meat/fish, egg, tofu/paneer, sauces, dressings, visible cooking oils/ghee/butter, nuts/seeds).
        2. DISTINGUISH SIMILAR INGREDIENTS & COOKING STATE:
           - Carefully differentiate paneer vs tofu, white rice vs brown rice vs quinoa, yellow dal vs curry sauce, sweet potato vs carrot/squash, avocado vs cucumber.
           - Explicitly determine COOKED vs RAW vs GRILLED vs FRIED vs STEAMED vs BAKED state because water/oil absorption changes nutrient density per 100g.
        3. ZERO HALLUCINATION / UNCERTAINTY HONESTY:
           - NEVER invent hidden ingredients that cannot be seen.
           - If an ingredient is ambiguous, mixed into a stew/sauce, poorly lit, or viewed at an awkward angle, set isUncertain=true, lower confidence accordingly (< 0.70), and explain clearly in uncertaintyReason.
           - If container depth makes volume ambiguous, set isWeightUncertain=true and provide weightRangeGrams.
           - If NO recognizable food or beverage is present (e.g., blank wall, black/dark lens, keyboard, face), set foodDetected=false and items=[] with a clear noFoodReason.
        4. VERIFIED USDA COMPOSITION & ATWATER MATH:
           - Estimate portion in grams realistically using plate/bowl/utensil visual scale.
           - Compute calories, protein, carbs, total fat, fiber, sugar, and saturated fat for EACH item's exact gram weight using USDA FoodData Central references.
           - Ensure macro energy satisfies Atwater balance (Protein*4 + Carbs*4 + Fat*9, accounting for dietary fiber).
    """.trimIndent()

    private val visionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.15f
                responseMimeType = "application/json"
                responseSchema = foodAnalysisSchema
            },
            systemInstruction = content { text(systemPrompt) }
        )
    }

    suspend fun analyzeFoodImage(rawBitmap: Bitmap): FoodNutritionAnalysis {
        val (processedBitmap, quality) = prepareImageForVision(rawBitmap)

        val visionPrompt = buildString {
            append("Analyze this food image for NEXORA Nutrition Telemetry. ")
            append("Image lighting assessment: ${quality.lightingLabel} (mean luminance ${quality.meanLuminance.roundToInt()}/255). ")
            if (quality.isVeryDark) {
                append("The image is very dark; only report foods you can genuinely see, and mark uncertain areas honestly. ")
            } else if (quality.isLowLight) {
                append("Lighting is somewhat dim; account for potential visual ambiguity in sauces or dark greens. ")
            }
            append("Identify each visible ingredient separately with gram weight, raw/cooked state, confidence, and complete macros including fiber, sugar, and saturated fat.")
        }

        val input = content {
            image(processedBitmap)
            text(visionPrompt)
        }

        return try {
            val response = withTimeout(45_000L) {
                visionModel.generateContent(input)
            }
            val rawJson = response.text?.trim()
            if (rawJson.isNullOrBlank()) {
                throw IllegalStateException("Vision model returned an empty response. Please try scanning again.")
            }
            parseStructuredFoodJson(rawJson, quality)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Food vision analysis timed out", e)
            throw IllegalStateException("Analysis timed out. Please check your connection and retry.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Food vision analysis failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Could not complete AI food analysis. Please retry.")
        }
    }

    /**
     * Parses raw structured JSON from Gemini Vision, validates every item against Atwater 4-4-9 rules,
     * and calculates total meal nutrition strictly by summing individual items.
     */
    fun parseStructuredFoodJson(
        rawJson: String,
        imageQuality: ImageQualityAssessment? = null
    ): FoodNutritionAnalysis {
        val cleanJson = rawJson
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val obj = JSONObject(cleanJson)
        val foodDetected = obj.optBoolean("foodDetected", true)
        val mealName = obj.optString("mealName", "").trim()
        val noFoodReason = if (obj.isNull("noFoodReason")) null else obj.optString("noFoodReason", "").trim().ifBlank { null }

        val itemsArray = obj.optJSONArray("items")
        if (!foodDetected || itemsArray == null || itemsArray.length() == 0) {
            return FoodNutritionAnalysis(
                foodName = mealName.ifBlank { "No Food Recognized" },
                portionSize = "0g",
                foodDetected = false,
                items = emptyList(),
                imageQuality = imageQuality,
                noFoodReason = noFoodReason
                    ?: "No recognizable food items were found. Center your plate in clear light and try again."
            )
        }

        val parsedItems = mutableListOf<DetectedFoodItem>()
        for (i in 0 until itemsArray.length()) {
            val itemObj = itemsArray.optJSONObject(i) ?: continue
            val name = itemObj.optString("name", "").trim()
            if (name.isBlank()) continue

            val category = FoodCategory.fromString(itemObj.optString("category"))
            val prepState = PreparationState.fromString(itemObj.optString("preparationState"))
            val prepMethod = itemObj.optString("preparationMethod", prepState.label).trim()
            val grams = itemObj.optDouble("estimatedGrams", 0.0).toFloat().coerceAtLeast(1f)
            val servingDesc = itemObj.optString("servingDescription", "").trim()
            val isWeightUncertain = itemObj.optBoolean("isWeightUncertain", false)
            val weightRange = itemObj.optString("weightRangeGrams", "").trim()
            val confidence = itemObj.optDouble("confidence", 0.8).toFloat().coerceIn(0f, 1f)
            val isUncertain = itemObj.optBoolean("isUncertain", confidence < 0.7f)
            val uncertaintyReason = if (itemObj.isNull("uncertaintyReason")) {
                null
            } else {
                itemObj.optString("uncertaintyReason", "").trim().ifBlank { null }
            }

            val calories = itemObj.optInt("caloriesKcal", 0).coerceAtLeast(0)
            val protein = itemObj.optDouble("proteinGrams", 0.0).toFloat().coerceAtLeast(0f)
            val carbs = itemObj.optDouble("carbsGrams", 0.0).toFloat().coerceAtLeast(0f)
            val fat = itemObj.optDouble("fatGrams", 0.0).toFloat().coerceAtLeast(0f)
            var fiber = itemObj.optDouble("fiberGrams", 0.0).toFloat().coerceAtLeast(0f)
            var sugar = itemObj.optDouble("sugarGrams", 0.0).toFloat().coerceAtLeast(0f)
            var satFat = itemObj.optDouble("saturatedFatGrams", 0.0).toFloat().coerceAtLeast(0f)

            // Cross-check against USDA database if micronutrients/fiber/satFat were omitted for a matched staple
            val refMatch = UsdaFoodReferenceDatabase.findBestMatch(name, prepState)
            val refLabel = if (refMatch != null) {
                if (fiber == 0f && refMatch.fiberPer100g > 0f && carbs > 0f) {
                    fiber = FoodNutritionCalculator.round1(refMatch.fiberPer100g * (grams / 100f))
                }
                if (sugar == 0f && refMatch.sugarPer100g > 0f && carbs > 0f) {
                    sugar = FoodNutritionCalculator.round1(refMatch.sugarPer100g * (grams / 100f))
                }
                if (satFat == 0f && refMatch.satFatPer100g > 0f && fat > 0f) {
                    satFat = FoodNutritionCalculator.round1(refMatch.satFatPer100g * (grams / 100f))
                }
                "Vision + USDA Cross-Checked"
            } else {
                "Vision + Atwater Verified"
            }

            parsedItems.add(
                DetectedFoodItem(
                    name = name,
                    category = category,
                    preparationState = prepState,
                    preparationMethod = prepMethod.ifBlank { prepState.label },
                    estimatedGrams = grams,
                    servingDescription = servingDesc.ifBlank { "${grams.roundToInt()}g" },
                    isWeightUncertain = isWeightUncertain,
                    weightRangeGrams = weightRange,
                    confidence = confidence,
                    isUncertain = isUncertain,
                    uncertaintyReason = uncertaintyReason,
                    caloriesKcal = calories,
                    proteinGrams = protein,
                    carbsGrams = carbs,
                    fatGrams = fat,
                    fiberGrams = fiber,
                    sugarGrams = sugar,
                    saturatedFatGrams = satFat,
                    referenceSource = refLabel
                )
            )
        }

        val vitamins = mutableListOf<String>()
        obj.optJSONArray("vitamins")?.let { arr ->
            for (i in 0 until arr.length()) {
                arr.optString(i).trim().takeIf { it.isNotEmpty() }?.let { vitamins.add(it) }
            }
        }

        val minerals = mutableListOf<String>()
        obj.optJSONArray("minerals")?.let { arr ->
            for (i in 0 until arr.length()) {
                arr.optString(i).trim().takeIf { it.isNotEmpty() }?.let { minerals.add(it) }
            }
        }

        val healthScore = obj.optInt("healthScore", 75).coerceIn(0, 100)
        val summary = obj.optString("summary", "").trim()
        val rickyTip = obj.optString("rickyTip", "").trim()

        return FoodNutritionCalculator.buildMealFromItems(
            mealName = mealName,
            rawItems = parsedItems,
            vitamins = vitamins,
            minerals = minerals,
            healthScore = healthScore,
            summary = summary,
            rickyTip = rickyTip,
            imageQuality = imageQuality
        )
    }

    /**
     * Downscales oversized bitmaps to <= 1280px, measures luminance, and gently lifts shadows
     * if lighting is dim so poor lighting photos remain legible to the vision model.
     */
    fun prepareImageForVision(rawBitmap: Bitmap): Pair<Bitmap, ImageQualityAssessment> {
        val maxDim = 1280
        val w = rawBitmap.width.coerceAtLeast(1)
        val h = rawBitmap.height.coerceAtLeast(1)
        val scale = if (max(w, h) > maxDim) {
            maxDim.toFloat() / max(w, h).toFloat()
        } else 1f

        val resized = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                rawBitmap,
                (w * scale).roundToInt().coerceAtLeast(1),
                (h * scale).roundToInt().coerceAtLeast(1),
                true
            )
        } else {
            rawBitmap
        }

        val meanLuma = estimateMeanLuminance(resized)
        val isVeryDark = meanLuma < 24f
        val isLowLight = meanLuma < 58f
        val isBright = meanLuma > 228f

        var enhancementApplied = false
        val finalBitmap = if (isLowLight && !isVeryDark) {
            enhancementApplied = true
            applyGentleLowLightBoost(resized)
        } else {
            resized
        }

        val lightingLabel = when {
            isVeryDark -> "Very Dark"
            isLowLight -> "Low Light (Enhanced)"
            isBright -> "High Exposure"
            else -> "Balanced"
        }

        val advisory = when {
            isVeryDark -> "Image is very dark. Enable the flashlight or move to brighter light for higher accuracy."
            isLowLight -> "Dim lighting detected; shadow clarity enhanced automatically, though sauce/oil estimates may carry uncertainty."
            isBright -> "Strong highlights detected; check glossy ingredients and adjust grams if needed."
            else -> null
        }

        val assessment = ImageQualityAssessment(
            lightingLabel = lightingLabel,
            meanLuminance = meanLuma,
            isLowLight = isLowLight,
            isVeryDark = isVeryDark,
            enhancementApplied = enhancementApplied,
            advisory = advisory
        )
        return finalBitmap to assessment
    }

    private fun estimateMeanLuminance(bitmap: Bitmap): Float {
        val stepX = (bitmap.width / 24).coerceAtLeast(1)
        val stepY = (bitmap.height / 24).coerceAtLeast(1)
        var sum = 0.0
        var count = 0
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                sum += (0.299 * r + 0.587 * g + 0.114 * b)
                count++
                x += stepX
            }
            y += stepY
        }
        return if (count > 0) (sum / count).toFloat() else 128f
    }

    private fun applyGentleLowLightBoost(source: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cm = ColorMatrix(
            floatArrayOf(
                1.18f, 0f, 0f, 0f, 22f,
                0f, 1.18f, 0f, 0f, 22f,
                0f, 0f, 1.18f, 0f, 22f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return out
    }
}
