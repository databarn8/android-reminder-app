package com.reminder.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.reminder.app.data.RepeatType

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
    val triggerPoints: String? = null, // JSON string of trigger points, defaults to AT_DUE_TIME if null
    val repeatPattern: String? = null, // JSON string for RepeatPattern data structure
    val alertConfig: String? = null // JSON string for AlertConfig data structure
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
    
    // Helper methods for new alert configuration
    fun getAlertConfigData(): AlertConfig {
        return try {
            if (alertConfig.isNullOrBlank()) {
                AlertConfig() // Default configuration
            } else {
                AlertConfig.fromJson(alertConfig)
            }
        } catch (e: Exception) {
            AlertConfig() // Fallback to default
        }
    }
    
    fun getRepeatPatternData(): RepeatPattern {
        return try {
            if (repeatPattern.isNullOrBlank()) {
                // Convert legacy repeatType to new RepeatPattern
                val legacyType = when (repeatType.lowercase()) {
                    "daily" -> RepeatType.DAILY
                    "weekly" -> RepeatType.WEEKLY
                    "monthly" -> RepeatType.MONTHLY
                    "yearly" -> RepeatType.YEARLY
                    else -> RepeatType.NONE
                }
                RepeatPattern(type = legacyType, interval = repeatInterval)
            } else {
                RepeatPattern.fromJson(repeatPattern)
            }
        } catch (e: Exception) {
            RepeatPattern() // Fallback to default
        }
    }
    
    // Helper method to get next occurrence based on repeat pattern
    fun getNextOccurrence(afterDate: java.time.LocalDateTime = java.time.LocalDateTime.now()): java.time.LocalDateTime? {
        val pattern = getRepeatPatternData()
        if (pattern.type == RepeatType.NONE) return null
        
        val current = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(reminderTime),
            java.time.ZoneId.systemDefault()
        )
        
        return when (pattern.type) {
            RepeatType.MINUTELY -> {
                var next = current
                while (!next.isAfter(afterDate)) {
                    next = next.plusMinutes(pattern.interval.toLong())
                }
                next
            }
            RepeatType.HOURLY -> {
                var next = current
                while (!next.isAfter(afterDate)) {
                    next = next.plusHours(pattern.interval.toLong())
                }
                next
            }
            RepeatType.DAILY -> {
                var next = current
                while (!next.isAfter(afterDate)) {
                    next = next.plusDays(pattern.interval.toLong())
                }
                next
            }
            RepeatType.WEEKLY -> {
                var next = current
                while (!next.isAfter(afterDate)) {
                    next = next.plusWeeks(pattern.interval.toLong())
                }
                next
            }
            RepeatType.MONTHLY -> {
                var next = current
                while (!next.isAfter(afterDate)) {
                    next = next.plusMonths(pattern.interval.toLong())
                }
                next
            }
            RepeatType.YEARLY -> {
                var next = current
                while (!next.isAfter(afterDate)) {
                    next = next.plusYears(pattern.interval.toLong())
                }
                next
            }
            RepeatType.CUSTOM -> {
                // For custom patterns, use daysOfWeek if specified
                if (pattern.daysOfWeek != null) {
                    var next = current
                    while (!next.isAfter(afterDate)) {
                        next = next.plusDays(1)
                        // Find next matching day of week
                        while (next.dayOfWeek !in pattern.daysOfWeek!!) {
                            next = next.plusDays(1)
                        }
                    }
                    next
                } else {
                    current.plusDays(pattern.interval.toLong())
                }
            }
            else -> null
        }
    }
    
    // Helper method to get all future occurrences (up to a limit)
    fun getFutureOccurrences(limit: Int = 10): List<java.time.LocalDateTime> {
        val pattern = getRepeatPatternData()
        if (pattern.type == RepeatType.NONE) return emptyList()
        
        val occurrences = mutableListOf<java.time.LocalDateTime>()
        val current = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(reminderTime),
            java.time.ZoneId.systemDefault()
        )
        
        var next = current
        val now = java.time.LocalDateTime.now()
        
        while (occurrences.size < limit && (pattern.endDate == null || next.isBefore(pattern.endDate.atStartOfDay()))) {
            if (next.isAfter(now)) {
                occurrences.add(next)
            }
            next = when (pattern.type) {
                RepeatType.MINUTELY -> next.plusMinutes(pattern.interval.toLong())
                RepeatType.HOURLY -> next.plusHours(pattern.interval.toLong())
                RepeatType.DAILY -> next.plusDays(pattern.interval.toLong())
                RepeatType.WEEKLY -> next.plusWeeks(pattern.interval.toLong())
                RepeatType.MONTHLY -> next.plusMonths(pattern.interval.toLong())
                RepeatType.YEARLY -> next.plusYears(pattern.interval.toLong())
                RepeatType.CUSTOM -> {
                    if (pattern.daysOfWeek != null) {
                        var tempNext = next.plusDays(1)
                        while (tempNext.dayOfWeek !in pattern.daysOfWeek!!) {
                            tempNext = tempNext.plusDays(1)
                        }
                        tempNext
                    } else {
                        next.plusDays(pattern.interval.toLong())
                    }
                }
                else -> break
            }
        }
        
        return occurrences
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