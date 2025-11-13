package com.reminder.app

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.reminder.app.repository.ReminderRepository

class ReminderViewModelFactory(
    private val repository: ReminderRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(com.reminder.app.viewmodel.ReminderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return com.reminder.app.viewmodel.ReminderViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}