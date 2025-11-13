package com.reminder.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reminder.app.data.Reminder
import com.reminder.app.repository.ReminderRepository
import com.reminder.app.utils.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReminderViewModel(
    private val repository: ReminderRepository,
    private val application: Application
) : AndroidViewModel(application) {
    
    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadReminders()
        // Schedule alarms for existing reminders
        scheduleAllReminders()
    }

    private fun loadReminders() {
        viewModelScope.launch {
            repository.getAllReminders().collect { reminderList ->
                _reminders.value = reminderList
            }
        }
    }

    fun addReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.insertReminder(reminder)
                // Schedule alarm for the new reminder
                NotificationScheduler.scheduleReminder(application, reminder)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add reminder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.updateReminder(reminder)
                // Cancel old alarm and schedule new one for updated reminder
                NotificationScheduler.cancelReminder(application, reminder.id)
                NotificationScheduler.scheduleReminder(application, reminder)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update reminder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deleteReminder(reminder)
                // Cancel alarm for the deleted reminder
                NotificationScheduler.cancelReminder(application, reminder.id)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete reminder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getRemindersByCategory(category: String) {
        viewModelScope.launch {
            repository.getRemindersByCategory(category).collect { reminderList ->
                _reminders.value = reminderList
            }
        }
    }

    fun getRemindersByImportance(minImportance: Int) {
        viewModelScope.launch {
            repository.getRemindersByImportance(minImportance).collect { reminderList ->
                _reminders.value = reminderList
            }
        }
    }

    suspend fun getReminderById(id: Int): Reminder? {
        return repository.getReminderById(id)
    }

    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Schedule alarms for all existing reminders (called on app startup)
     */
    private fun scheduleAllReminders() {
        viewModelScope.launch {
            try {
                repository.getAllReminders().collect { reminderList ->
                    reminderList.forEach { reminder ->
                        // Only schedule future reminders
                        if (reminder.reminderTime > System.currentTimeMillis()) {
                            NotificationScheduler.scheduleReminder(application, reminder)
                        }
                    }
                }
            } catch (e: Exception) {
                // Log error but don't crash app
                e.printStackTrace()
            }
        }
    }
}