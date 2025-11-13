package com.reminder.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.reminder.app.data.ReminderDatabase
import com.reminder.app.repository.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "Boot received: ${intent.action}")
        
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == "android.intent.action.MY_PACKAGE_RESTARTED") {
            
            Log.d("BootReceiver", "Rescheduling alarms after boot")
            
            // Reschedule all existing reminders
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = ReminderDatabase.getDatabase(context)
                    val repository = ReminderRepository(database.reminderDao())
                    
                    repository.getAllReminders().collect { reminders ->
                        reminders.forEach { reminder ->
                            // Only schedule future reminders
                            if (reminder.reminderTime > System.currentTimeMillis()) {
                                Log.d("BootReceiver", "Rescheduling alarm for: ${reminder.content}")
                                NotificationScheduler.scheduleReminder(context, reminder)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error rescheduling alarms: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
}