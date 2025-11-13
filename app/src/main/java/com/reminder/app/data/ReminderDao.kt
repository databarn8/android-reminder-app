package com.reminder.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY createdAt DESC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Int): Reminder?

    @Query("SELECT * FROM reminders WHERE category = :category ORDER BY createdAt DESC")
    fun getRemindersByCategory(category: String): Flow<List<Reminder>>
    
    @Query("SELECT * FROM reminders WHERE importance >= :minImportance ORDER BY createdAt DESC")
    fun getRemindersByImportance(minImportance: Int): Flow<List<Reminder>>

    @Insert
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Int)

    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()
}