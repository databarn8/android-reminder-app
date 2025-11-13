package com.reminder.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TriggerType {
    AT_DUE_TIME,           // Exactly at due time
    MINUTES_BEFORE,        // X minutes before due time
    HOURS_BEFORE,          // X hours before due time
    DAYS_BEFORE,           // X days before due time
    WEEKS_BEFORE,          // X weeks before due time
    CUSTOM_OFFSET          // Custom milliseconds offset
}

data class TriggerPoint(
    val type: TriggerType,
    val value: Int = 0,           // Number of minutes/hours/days/weeks
    val customOffsetMs: Long = 0L, // For CUSTOM_OFFSET type
    val enableFlash: Boolean = true,
    val enableSound: Boolean = true,
    val enableVibration: Boolean = true
) {
    fun calculateTriggerTime(dueTime: Long): Long {
        return when (type) {
            TriggerType.AT_DUE_TIME -> dueTime
            TriggerType.MINUTES_BEFORE -> dueTime - (value * 60 * 1000L)
            TriggerType.HOURS_BEFORE -> dueTime - (value * 60 * 60 * 1000L)
            TriggerType.DAYS_BEFORE -> dueTime - (value * 24 * 60 * 60 * 1000L)
            TriggerType.WEEKS_BEFORE -> dueTime - (value * 7 * 24 * 60 * 60 * 1000L)
            TriggerType.CUSTOM_OFFSET -> dueTime - customOffsetMs
        }
    }
    
    fun getDescription(): String {
        return when (type) {
            TriggerType.AT_DUE_TIME -> "At due time"
            TriggerType.MINUTES_BEFORE -> "$value minute${if (value != 1) "s" else ""} before"
            TriggerType.HOURS_BEFORE -> "$value hour${if (value != 1) "s" else ""} before"
            TriggerType.DAYS_BEFORE -> "$value day${if (value != 1) "s" else ""} before"
            TriggerType.WEEKS_BEFORE -> "$value week${if (value != 1) "s" else ""} before"
            TriggerType.CUSTOM_OFFSET -> "${customOffsetMs / 1000}s before"
        }
    }
}

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) 
    val id: Int = 0,
    val content: String, // Only content field (no title)
    val category: String,
    val importance: Int,
    val reminderTime: Long,
    val whenDay: String? = null, // Day information: "Today", "Tomorrow", "Monday", etc.
    val whenTime: String? = null, // Time information: "3pm", "2:30pm", "10am", etc.
    val repeatType: String = "none", // "none", "daily", "weekly", "monthly", "yearly"
    val repeatInterval: Int = 1, // Every X days/weeks/months/years
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val voiceInput: String? = null, // Raw voice input for future processing
    val isProcessed: Boolean = false, // Whether voice data has been parsed
    val triggerPoints: String? = null // JSON string of trigger points, defaults to AT_DUE_TIME if null
) {
    fun getTriggerPointsList(): List<TriggerPoint> {
        return try {
            if (triggerPoints.isNullOrBlank()) {
                listOf(TriggerPoint(TriggerType.AT_DUE_TIME))
            } else {
                // Parse JSON string to list of TriggerPoint objects
                parseTriggerPointsFromJson(triggerPoints)
            }
        } catch (e: Exception) {
            listOf(TriggerPoint(TriggerType.AT_DUE_TIME))
        }
    }
    
    private fun parseTriggerPointsFromJson(json: String): List<TriggerPoint> {
        // Simple JSON parsing for trigger points
        // Format: [{"type":"MINUTES_BEFORE","value":15,"enableFlash":true}, ...]
        val result = mutableListOf<TriggerPoint>()
        try {
            val jsonArray = org.json.JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val type = TriggerType.valueOf(jsonObject.getString("type"))
                val value = jsonObject.optInt("value", 0)
                val customOffsetMs = jsonObject.optLong("customOffsetMs", 0L)
                val enableFlash = jsonObject.optBoolean("enableFlash", true)
                val enableSound = jsonObject.optBoolean("enableSound", true)
                val enableVibration = jsonObject.optBoolean("enableVibration", true)
                
                result.add(TriggerPoint(type, value, customOffsetMs, enableFlash, enableSound, enableVibration))
            }
        } catch (e: Exception) {
            // If parsing fails, return default
        }
        return result
    }
}