package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.GoalItem
import com.example.data.model.HabitItem
import com.example.data.model.TaskItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

object BackupRestoreManager {

    fun exportDataToJson(
        context: Context,
        tasks: List<TaskItem>,
        habits: List<HabitItem>,
        goals: List<GoalItem>,
        waterIntakeMl: Int,
        totalXp: Int,
        level: Int
    ): Boolean {
        return try {
            val root = JSONObject().apply {
                put("app", "NEXORA")
                put("version", "1.0.0")
                put("exportDate", LocalDate.now().toString())

                val tasksArr = JSONArray()
                tasks.forEach { t ->
                    val tObj = JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("category", t.category)
                        put("priority", t.priority)
                        put("completed", t.completed)
                        put("dueDate", t.dueDate ?: "")
                        put("dueTime", t.dueTime ?: "")
                    }
                    tasksArr.put(tObj)
                }
                put("tasks", tasksArr)

                val habitsArr = JSONArray()
                habits.forEach { h ->
                    val hObj = JSONObject().apply {
                        put("id", h.id)
                        put("title", h.title)
                        put("notes", h.notes ?: "")
                        put("frequency", h.frequency)
                        put("targetDaysPerWeek", h.targetDaysPerWeek)
                        put("currentStreak", h.currentStreak)
                        put("completedDates", JSONArray(h.completedDates))
                    }
                    habitsArr.put(hObj)
                }
                put("habits", habitsArr)

                val statsObj = JSONObject().apply {
                    put("waterIntakeMl", waterIntakeMl)
                    put("totalXp", totalXp)
                    put("level", level)
                }
                put("stats", statsObj)
            }

            val cachePath = File(context.cacheDir, "backups")
            cachePath.mkdirs()
            val file = File(cachePath, "nexora_backup_${System.currentTimeMillis()}.json")
            val stream = FileOutputStream(file)
            stream.write(root.toString(2).toByteArray(Charsets.UTF_8))
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "NEXORA Data Backup (.json)")
                putExtra(Intent.EXTRA_TEXT, "Here is your exported NEXORA productivity & habit backup.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Save or Export Backup").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (_: Exception) {
            false
        }
    }
}
