package com.reminder.app.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(entities = [Reminder::class], version = 11, exportSchema = false)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        fun getDatabase(context: Context): ReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "reminder_database"
                ).fallbackToDestructiveMigration() // This will recreate DB on schema changes
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        // Helper function to clear database for debugging
        fun clearDatabase(context: Context) {
            context.deleteDatabase("reminder_database")
            INSTANCE = null
        }
    }
}