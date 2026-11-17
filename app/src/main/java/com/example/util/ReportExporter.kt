package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ReportExporter {

    fun generateAndShareWeeklyReport(
        context: Context,
        steps: Int,
        distanceKm: Float,
        caloriesBurned: Int,
        waterMl: Int,
        tasksCompleted: Int,
        habitsCompleted: Int,
        currentStreak: Int,
        level: Int,
        xp: Int
    ): Boolean {
        return try {
            val width = 1080
            val height = 1600
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Deep background
            val bgPaint = Paint().apply {
                color = Color.parseColor("#0F0C20")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Header Banner
            val headerPaint = Paint().apply {
                color = Color.parseColor("#8B5CF6")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(40f, 40f, (width - 40).toFloat(), 220f, 24f, 24f, headerPaint)

            val textPaint = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 54f
            }
            canvas.drawText("NEXORA WEEKLY REPORT", 80f, 130f, textPaint)

            val subTextPaint = Paint().apply {
                color = Color.parseColor("#E0E7FF")
                isAntiAlias = true
                textSize = 32f
            }
            val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
            canvas.drawText("Generated on $dateStr • Master Life Dashboard", 80f, 185f, subTextPaint)

            // Section Card 1: Fitness & Health
            drawCard(canvas, 40f, 260f, (width - 40).toFloat(), 620f, "#1A1633")
            val sectionTitle = Paint().apply {
                color = Color.parseColor("#C4B5FD")
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 38f
            }
            canvas.drawText("🏃 HEALTH & ACTIVITY", 80f, 320f, sectionTitle)

            drawMetric(canvas, "Today's Steps", "%,d".format(steps), 80f, 380f, 440f)
            drawMetric(canvas, "Distance Covered", "%.2f km".format(distanceKm), 560f, 380f, 440f)
            drawMetric(canvas, "Calories Burned", "$caloriesBurned kcal", 80f, 500f, 560f)
            drawMetric(canvas, "Water Intake", "$waterMl ml", 560f, 500f, 560f)

            // Section Card 2: Productivity & Discipline
            drawCard(canvas, 40f, 660f, (width - 40).toFloat(), 1020f, "#1A1633")
            canvas.drawText("⚡ PRODUCTIVITY & DISCIPLINE", 80f, 720f, sectionTitle)

            drawMetric(canvas, "Tasks Completed", "$tasksCompleted tasks", 80f, 780f, 840f)
            drawMetric(canvas, "Habits Checked", "$habitsCompleted times", 560f, 780f, 840f)
            drawMetric(canvas, "Current Streak", "$currentStreak Days 🔥", 80f, 900f, 960f)
            drawMetric(canvas, "Hero Level", "Level $level ($xp XP)", 560f, 900f, 960f)

            // Quote Box
            drawCard(canvas, 40f, 1060f, (width - 40).toFloat(), 1260f, "#251F4A")
            val quoteTitle = Paint().apply {
                color = Color.parseColor("#A78BFA")
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 34f
            }
            canvas.drawText("🤖 RICKY AI SUMMARY", 80f, 1120f, quoteTitle)

            val quoteBody = Paint().apply {
                color = Color.parseColor("#F3F4F6")
                isAntiAlias = true
                textSize = 30f
            }
            canvas.drawText("\"Consistency is the key to mastery. You stayed on top of", 80f, 1170f, quoteBody)
            canvas.drawText("your habits and daily movement this week. Keep pressing forward!\"", 80f, 1220f, quoteBody)

            // Footer
            val footerPaint = Paint().apply {
                color = Color.parseColor("#6B7280")
                isAntiAlias = true
                textSize = 28f
            }
            canvas.drawText("Powered by NEXORA & RICKY AI • Private & Encrypted", 80f, 1340f, footerPaint)

            // Save to cache file and launch share intent
            val cachePath = File(context.cacheDir, "reports")
            cachePath.mkdirs()
            val file = File(cachePath, "nexora_report_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "My NEXORA Health & Productivity Report")
                putExtra(Intent.EXTRA_TEXT, "Here is my weekly activity and habit summary from NEXORA!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Weekly Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun drawCard(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, hexColor: String) {
        val paint = Paint().apply {
            color = Color.parseColor(hexColor)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(left, top, right, bottom, 20f, 20f, paint)

        val borderPaint = Paint().apply {
            color = Color.parseColor("#3730A3")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(left, top, right, bottom, 20f, 20f, borderPaint)
    }

    private fun drawMetric(canvas: Canvas, title: String, value: String, x: Float, titleY: Float, valY: Float) {
        val tPaint = Paint().apply {
            color = Color.parseColor("#9CA3AF")
            isAntiAlias = true
            textSize = 28f
        }
        val vPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 42f
        }
        canvas.drawText(title, x, titleY, tPaint)
        canvas.drawText(value, x, valY, vPaint)
    }
}
