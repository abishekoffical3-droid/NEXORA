package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Habit operations in Room SQLite database.
 */
@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAtTimestamp DESC")
    fun getAllActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE userId = :userId AND isArchived = 0 ORDER BY createdAtTimestamp DESC")
    fun getActiveHabitsForUser(userId: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY createdAtTimestamp DESC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    fun getHabitByIdFlow(id: String): Flow<HabitEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: String): Int

    @Query("UPDATE habits SET isArchived = :isArchived, updatedAtTimestamp = :timestamp WHERE id = :id")
    suspend fun setHabitArchived(id: String, isArchived: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM habits")
    suspend fun clearAllHabits()
}
