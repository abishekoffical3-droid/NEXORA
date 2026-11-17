package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.dao.HabitDao
import com.example.data.local.entity.HabitEntity

/**
 * Nexora local SQLite Database powered by Room.
 * Provides on-device persistence for habit tracking, completion histories, and offline access.
 */
@Database(
    entities = [HabitEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NexoraDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao

    companion object {
        private const val DATABASE_NAME = "nexora_local.db"

        @Volatile
        private var instance: NexoraDatabase? = null

        fun getInstance(context: Context): NexoraDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): NexoraDatabase {
            return Room.databaseBuilder(
                context,
                NexoraDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
