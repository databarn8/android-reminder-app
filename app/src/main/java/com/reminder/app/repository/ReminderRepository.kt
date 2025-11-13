package com.reminder.app.repository

import com.reminder.app.data.Reminder
import com.reminder.app.data.ReminderDao
import kotlinx.coroutines.flow.Flow
class ReminderRepository(
    private val reminderDao: ReminderDao
) {
    fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAllReminders()

    suspend fun getReminderById(id: Int): Reminder? = reminderDao.getReminderById(id)

    fun getRemindersByCategory(category: String): Flow<List<Reminder>> = 
        reminderDao.getRemindersByCategory(category)

    fun getRemindersByImportance(minImportance: Int): Flow<List<Reminder>> = 
        reminderDao.getRemindersByImportance(minImportance)

    suspend fun insertReminder(reminder: Reminder): Long = reminderDao.insertReminder(reminder)

    suspend fun updateReminder(reminder: Reminder) = reminderDao.updateReminder(reminder)

    suspend fun deleteReminder(reminder: Reminder) = reminderDao.deleteReminder(reminder)

    suspend fun deleteReminderById(id: Int) = reminderDao.deleteReminderById(id)

    suspend fun deleteAllReminders() = reminderDao.deleteAllReminders()
}