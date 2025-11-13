package com.reminder.app.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.reminder.app.R
import com.reminder.app.MainActivity
import com.reminder.app.data.Reminder
import com.reminder.app.utils.EnhancedEmailService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONException

class NotificationScheduler : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("NotificationScheduler", "Alarm received!")
        
        val title = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: return
        val content = intent.getStringExtra(EXTRA_REMINDER_CONTENT) ?: return
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1)
        val reminderJson = intent.getStringExtra(EXTRA_REMINDER_JSON)
        val triggerType = intent.getStringExtra(EXTRA_TRIGGER_TYPE) ?: "AT_DUE_TIME"
        val enableFlash = intent.getBooleanExtra(EXTRA_ENABLE_FLASH, true)
        val enableSound = intent.getBooleanExtra(EXTRA_ENABLE_SOUND, true)
        val enableVibration = intent.getBooleanExtra(EXTRA_ENABLE_VIBRATION, true)
        
        android.util.Log.d("NotificationScheduler", "Showing notification for: $title")
        android.util.Log.d("NotificationScheduler", "Reminder ID: $reminderId")
        android.util.Log.d("NotificationScheduler", "Trigger type: $triggerType")
        
        // Trigger screen flash if enabled
        if (enableFlash) {
            val flashColor = when (triggerType) {
                "MINUTES_BEFORE" -> android.graphics.Color.YELLOW
                "HOURS_BEFORE" -> android.graphics.Color.BLUE
                "DAYS_BEFORE" -> android.graphics.Color.GREEN
                "WEEKS_BEFORE" -> android.graphics.Color.MAGENTA
                else -> android.graphics.Color.RED
            }
            ScreenFlashManager.triggerFlash(
                context = context,
                flashColor = androidx.compose.ui.graphics.Color(flashColor),
                flashDurationMs = 800, // Increased duration
                flashCount = when (triggerType) {
                    "MINUTES_BEFORE" -> 4
                    "HOURS_BEFORE" -> 6
                    "DAYS_BEFORE" -> 8
                    "WEEKS_BEFORE" -> 10
                    else -> 6 // Increased counts
                },
                intervalMs = 150 // Reduced interval for more intense flashing
            )
        }
        
        // Trigger vibration if enabled
        if (enableVibration) {
            val vibrationPattern = when (triggerType) {
                "MINUTES_BEFORE" -> longArrayOf(0, 100, 50, 100)
                "HOURS_BEFORE" -> longArrayOf(0, 200, 100, 200, 100, 200)
                "DAYS_BEFORE" -> longArrayOf(0, 300, 150, 300, 150, 300)
                "WEEKS_BEFORE" -> longArrayOf(0, 400, 200, 400, 200, 400, 200, 400)
                else -> longArrayOf(0, 200, 100, 200, 100, 200)
            }
            ScreenFlashManager.triggerVibration(context, vibrationPattern)
        }
        
        // Trigger sound if enabled
        if (enableSound) {
            ScreenFlashManager.triggerSound(context)
        }
        
        showNotification(context, title, content, reminderId)
        
        // Send email if reminder data is available
        reminderJson?.let { json ->
            try {
                val reminder = parseReminderFromJson(json)
                val emailService = EnhancedEmailService()
                CoroutineScope(Dispatchers.IO).launch {
                    emailService.sendReminderEmail(context, reminder)
                }
            } catch (e: Exception) {
                // Log error but don't crash notification
                android.util.Log.e("NotificationScheduler", "Error sending email: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "reminder_channel"
        private const val CHANNEL_NAME = "Reminders"
        private const val EXTRA_REMINDER_TITLE = "reminder_title"
        private const val EXTRA_REMINDER_CONTENT = "reminder_content"
        private const val EXTRA_REMINDER_ID = "reminder_id"
        private const val EXTRA_REMINDER_JSON = "reminder_json"
        private const val EXTRA_TRIGGER_TYPE = "trigger_type"
        private const val EXTRA_ENABLE_FLASH = "enable_flash"
        private const val EXTRA_ENABLE_SOUND = "enable_sound"
        private const val EXTRA_ENABLE_VIBRATION = "enable_vibration"

        fun scheduleReminder(context: Context, reminder: Reminder) {
            val triggerPoints = reminder.getTriggerPointsList()
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val reminderJson = reminderToJson(reminder)
            
            android.util.Log.d("NotificationScheduler", "Scheduling reminder: ${reminder.content}")
            android.util.Log.d("NotificationScheduler", "Reminder ID: ${reminder.id}")
            android.util.Log.d("NotificationScheduler", "Number of trigger points: ${triggerPoints.size}")
            
            // Check exact alarm permission for Android 12+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    android.util.Log.e("NotificationScheduler", "EXACT_ALARM permission not granted - alarms may not work reliably")
                    // Show toast to user
                    try {
                        android.widget.Toast.makeText(context, "Please enable exact alarm permission for reliable reminders", android.widget.Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationScheduler", "Could not show toast: ${e.message}")
                    }
                } else {
                    android.util.Log.d("NotificationScheduler", "EXACT_ALARM permission granted")
                }
            }
            
            // Cancel existing alarms for this reminder
            cancelReminder(context, reminder.id)
            
            triggerPoints.forEachIndexed { index, triggerPoint ->
                val triggerTime = triggerPoint.calculateTriggerTime(reminder.reminderTime)
                val currentTime = System.currentTimeMillis()
                
                // Only schedule if trigger time is in the future
                if (triggerTime > currentTime) {
                    val intent = Intent(context, NotificationScheduler::class.java).apply {
                        putExtra(EXTRA_REMINDER_TITLE, reminder.content)
                        putExtra(EXTRA_REMINDER_CONTENT, reminder.content)
                        putExtra(EXTRA_REMINDER_ID, reminder.id)
                        putExtra(EXTRA_REMINDER_JSON, reminderJson)
                        putExtra(EXTRA_TRIGGER_TYPE, triggerPoint.type.name)
                        putExtra(EXTRA_ENABLE_FLASH, triggerPoint.enableFlash)
                        putExtra(EXTRA_ENABLE_SOUND, triggerPoint.enableSound)
                        putExtra(EXTRA_ENABLE_VIBRATION, triggerPoint.enableVibration)
                    }

                    // Use unique request code for each trigger point
                    val requestCode = "${reminder.id}_${index}".hashCode()
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val timeUntilTrigger = triggerTime - currentTime
                    
                    android.util.Log.d("NotificationScheduler", "Trigger point ${index + 1}: ${triggerPoint.getDescription()}")
                    android.util.Log.d("NotificationScheduler", "Trigger time: ${java.util.Date(triggerTime)}")
                    android.util.Log.d("NotificationScheduler", "Time until trigger: ${timeUntilTrigger}ms (${timeUntilTrigger / 1000 / 60} minutes)")

                    try {
                        // Verify alarm can be scheduled
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            if (!alarmManager.canScheduleExactAlarms()) {
                                android.util.Log.w("NotificationScheduler", "Cannot schedule exact alarms - permission missing for trigger ${index + 1}")
                                // Fall back to inexact alarm
                                alarmManager.setAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    triggerTime,
                                    pendingIntent
                                )
                                android.util.Log.d("NotificationScheduler", "Alarm ${index + 1} scheduled with setAndAllowWhileIdle (fallback)")
                            } else {
                                alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    triggerTime,
                                    pendingIntent
                                )
                                android.util.Log.d("NotificationScheduler", "Alarm ${index + 1} scheduled with setExactAndAllowWhileIdle (API 31+)")
                            }
                        } else {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                triggerTime,
                                pendingIntent
                            )
                            android.util.Log.d("NotificationScheduler", "Alarm ${index + 1} scheduled with setExact (pre-API 31)")
                        }
                        
                        // Verify alarm was set by checking if we can retrieve it
                        android.util.Log.d("NotificationScheduler", "Alarm ${index + 1} verification - Request code: $requestCode, Target time: ${java.util.Date(triggerTime)}")
                        
                    } catch (e: SecurityException) {
                        android.util.Log.e("NotificationScheduler", "SecurityException for trigger ${index + 1}: ${e.message}")
                        try {
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                triggerTime,
                                pendingIntent
                            )
                            android.util.Log.d("NotificationScheduler", "Alarm ${index + 1} scheduled with setAndAllowWhileIdle (security fallback)")
                        } catch (e2: Exception) {
                            android.util.Log.e("NotificationScheduler", "Final fallback to set for trigger ${index + 1}: ${e2.message}")
                            try {
                                alarmManager.set(
                                    AlarmManager.RTC_WAKEUP,
                                    triggerTime,
                                    pendingIntent
                                )
                                android.util.Log.d("NotificationScheduler", "Alarm ${index + 1} scheduled with set (inexact fallback)")
                            } catch (e3: Exception) {
                                android.util.Log.e("NotificationScheduler", "All alarm scheduling methods failed for trigger ${index + 1}: ${e3.message}")
                                e3.printStackTrace()
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationScheduler", "Failed to schedule alarm ${index + 1}: ${e.message}")
                        e.printStackTrace()
                    }
                } else {
                    android.util.Log.d("NotificationScheduler", "Skipping trigger point ${index + 1} - time is in the past")
                }
            }
        }

        fun cancelReminder(context: Context, reminderId: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            // Cancel multiple potential alarms for this reminder (for different trigger points)
            for (i in 0..10) { // Cancel up to 10 potential trigger points
                val requestCode = "${reminderId}_${i}".hashCode()
                val intent = Intent(context, NotificationScheduler::class.java)
                
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                alarmManager.cancel(pendingIntent)
            }
            
            android.util.Log.d("NotificationScheduler", "Cancelled all alarms for reminder ID: $reminderId")
        }

        private fun showNotification(context: Context, title: String, content: String, reminderId: Int) {
            createNotificationChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL) // Enable sound, vibration, lights
                .setOngoing(false) // Make sure it's not ongoing
                .setOnlyAlertOnce(false) // Alert every time
                .setWhen(System.currentTimeMillis()) // Show timestamp
                .setShowWhen(true) // Display timestamp
                .setLights(android.graphics.Color.RED, 1000, 1000) // Blink lights
                .setVibrate(longArrayOf(0, 500, 200, 500)) // Vibration pattern
                .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)) // Sound
                .setFullScreenIntent(pendingIntent, true) // Try to show as heads-up
                .setPriority(NotificationCompat.PRIORITY_MAX) // Maximum priority
                .setCategory(NotificationCompat.CATEGORY_ALARM) // Use alarm category for higher priority
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            
            // Check if we have notification permission
            if (notificationManager.areNotificationsEnabled()) {
                android.util.Log.d("NotificationScheduler", "Notifications are enabled, showing notification")
                try {
                    notificationManager.notify(reminderId, notification)
                    android.util.Log.d("NotificationScheduler", "Notification displayed successfully for reminder: $title")
                } catch (e: Exception) {
                    android.util.Log.e("NotificationScheduler", "Error showing notification: ${e.message}")
                    e.printStackTrace()
                }
            } else {
                android.util.Log.w("NotificationScheduler", "Notifications are DISABLED - cannot show notification")
                // Try to create a system toast as fallback
                try {
                    android.widget.Toast.makeText(context, "Reminder: $title", android.widget.Toast.LENGTH_LONG).show()
                    android.util.Log.d("NotificationScheduler", "Fallback toast displayed")
                } catch (e: Exception) {
                    android.util.Log.e("NotificationScheduler", "Even toast failed: ${e.message}")
                }
            }
        }

        private fun createNotificationChannel(context: Context) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for reminder notifications"
                    enableVibration(true)
                    enableLights(true)
                    lightColor = android.graphics.Color.RED
                    setShowBadge(true)
                    setBypassDnd(true) // Bypass Do Not Disturb for reminders
                    setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION), android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .build())
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    // Set maximum importance for heads-up notifications
                    importance = NotificationManager.IMPORTANCE_HIGH
                }

                val notificationManager = context.getSystemService(NotificationManager::class.java)
                
                // Check if channel already exists and update it
                val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
                if (existingChannel != null) {
                    android.util.Log.d("NotificationScheduler", "Updating existing notification channel")
                    notificationManager.deleteNotificationChannel(CHANNEL_ID)
                }
                
                notificationManager.createNotificationChannel(channel)
                android.util.Log.d("NotificationScheduler", "Notification channel created/updated with HIGH importance")
                
                // Verify channel was created properly
                val createdChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
                if (createdChannel != null) {
                    android.util.Log.d("NotificationScheduler", "Channel verification - Importance: ${createdChannel.importance}, BypassDND: ${createdChannel.canBypassDnd()}")
                } else {
                    android.util.Log.e("NotificationScheduler", "Failed to create notification channel!")
                }
            }
        }
        
        /**
         * Convert Reminder object to JSON string for passing in Intent
         */
        private fun reminderToJson(reminder: Reminder): String {
            return try {
                JSONObject().apply {
                    put("id", reminder.id)
                    put("content", reminder.content)
                    put("category", reminder.category)
                    put("importance", reminder.importance)
                    put("reminderTime", reminder.reminderTime)
                    put("whenDay", reminder.whenDay ?: "")
                    put("whenTime", reminder.whenTime ?: "")
                    put("repeatType", reminder.repeatType)
                    put("repeatInterval", reminder.repeatInterval)
                    put("createdAt", reminder.createdAt)
                }.toString()
            } catch (e: JSONException) {
                ""
            }
        }
        
        /**
         * Test function to trigger alarm immediately (for debugging)
         */
        fun testAlarm(context: Context) {
            android.util.Log.d("NotificationScheduler", "Testing alarm with immediate trigger")
            
            // Test 1: Direct flash trigger (immediate)
            android.util.Log.d("NotificationScheduler", "Testing direct flash trigger")
            ScreenFlashManager.triggerFlash(
                context = context,
                flashColor = androidx.compose.ui.graphics.Color.Red,
                flashDurationMs = 800,
                flashCount = 8,
                intervalMs = 150
            )
            
            // Test 2: Vibration trigger
            android.util.Log.d("NotificationScheduler", "Testing vibration trigger")
            ScreenFlashManager.triggerVibration(context, longArrayOf(0, 200, 100, 200, 100, 200))
            
            // Test 3: Sound trigger
            android.util.Log.d("NotificationScheduler", "Testing sound trigger")
            ScreenFlashManager.triggerSound(context)
            
            // Test 4: Also schedule a quick alarm (2 seconds from now) to test alarm system
            android.util.Log.d("NotificationScheduler", "Testing scheduled alarm (2 seconds from now)")
            val triggerPointsJson = org.json.JSONArray().apply {
                put(org.json.JSONObject().apply {
                    put("type", "AT_DUE_TIME")
                    put("value", 0)
                    put("customOffsetMs", 0)
                    put("enableFlash", true)
                    put("enableSound", true)
                    put("enableVibration", true)
                })
            }.toString()
            
            val testReminder = Reminder(
                id = 999999,
                content = "Test Screen Flash - Scheduled",
                category = "test",
                importance = 5,
                reminderTime = System.currentTimeMillis() + 2000, // 2 seconds from now
                whenDay = null,
                whenTime = null,
                repeatType = "none",
                repeatInterval = 0,
                createdAt = System.currentTimeMillis(),
                triggerPoints = triggerPointsJson
            )
            
            scheduleReminder(context, testReminder)
        }

        /**
         * Parse Reminder object from JSON string
         */
        private fun parseReminderFromJson(json: String): Reminder {
            return try {
                val jsonObject = JSONObject(json)
                Reminder(
                    id = jsonObject.getInt("id"),
                    content = jsonObject.getString("content"),
                    category = jsonObject.getString("category"),
                    importance = jsonObject.getInt("importance"),
                    reminderTime = jsonObject.getLong("reminderTime"),
                    whenDay = jsonObject.getString("whenDay").takeIf { it.isNotEmpty() },
                    whenTime = jsonObject.getString("whenTime").takeIf { it.isNotEmpty() },
                    repeatType = jsonObject.getString("repeatType"),
                    repeatInterval = jsonObject.getInt("repeatInterval"),
                    createdAt = jsonObject.getLong("createdAt")
                )
            } catch (e: JSONException) {
                throw IllegalArgumentException("Invalid reminder JSON format", e)
            }
        }
    }
}