package com.reminder.app.utils

import android.util.Log
import java.util.*
import java.util.regex.Pattern
import kotlin.math.abs

object DateTimeParser {
    
    private val calendar = Calendar.getInstance()
    
    fun parseDateTime(text: String): Long? {
        if (text.isBlank()) return null
        
        return try {
            val lowerText = text.lowercase(Locale.getDefault())
            Log.d("DateTimeParser", "Parsing: '$text' -> '$lowerText'")
            
            // Try different parsing strategies in order of specificity
            val explicitResult = parseExplicitDateTime(lowerText)
            if (explicitResult != null) {
                Log.d("DateTimeParser", "Explicit parsing result: $explicitResult")
                return explicitResult
            }
            
            val relativeResult = parseRelativeDateTime(lowerText)
            if (relativeResult != null) {
                Log.d("DateTimeParser", "Relative parsing result: $relativeResult")
                return relativeResult
            }
            
            val commonResult = parseCommonPhrases(lowerText)
            if (commonResult != null) {
                Log.d("DateTimeParser", "Common phrases result: $commonResult")
                return commonResult
            }
            
            Log.d("DateTimeParser", "No parsing match found for: '$text'")
            null
        } catch (e: Exception) {
            Log.e("DateTimeParser", "Error parsing date/time: ${e.message}")
            null
        }
    }
    
    private fun parseExplicitDateTime(text: String): Long? {
        // Pattern for "tomorrow at 3pm", "tomorrow 4:00pm", "next monday at 2:30pm", etc.
        val timePattern = Pattern.compile(
            "(?:tomorrow|today|tonight|tonite|\\b(mon|tue|wed|thu|fri|sat|sun)day?|next (mon|tue|wed|thu|fri|sat|sun)day?)\\s+(?:at\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?",
            Pattern.CASE_INSENSITIVE
        )
        
        Log.d("DateTimeParser", "Trying explicit pattern on: '$text'")
        val matcher = timePattern.matcher(text)
        if (matcher.find()) {
            val dayPart = matcher.group(1) ?: matcher.group(2)
            val hour = matcher.group(3)?.toIntOrNull()
            val minute = matcher.group(4)?.toIntOrNull() ?: 0
            val ampm = matcher.group(5)
            
            Log.d("DateTimeParser", "EXPLICIT MATCH: dayPart=$dayPart, hour=$hour, minute=$minute, ampm=$ampm")
            
            if (hour != null) {
                val result = calculateDateTime(dayPart, hour, minute, ampm)
                Log.d("DateTimeParser", "EXPLICIT RESULT: $result (${Date(result)})")
                return result
            }
        } else {
            Log.d("DateTimeParser", "No explicit match found")
        }
        
        // Pattern for "3pm", "2:30pm", "14:30", etc.
        val simpleTimePattern = Pattern.compile(
            "\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b",
            Pattern.CASE_INSENSITIVE
        )
        
        val simpleMatcher = simpleTimePattern.matcher(text)
        if (simpleMatcher.find()) {
            val hour = simpleMatcher.group(1)?.toIntOrNull()
            val minute = simpleMatcher.group(2)?.toIntOrNull() ?: 0
            val ampm = simpleMatcher.group(3)
            
            if (hour != null) {
                return calculateDateTime(null, hour, minute, ampm)
            }
        }
        
        return null
    }
    
    private fun parseRelativeDateTime(text: String): Long? {
        val now = System.currentTimeMillis()
        
        when {
            text.contains("tomorrow") -> {
                val tomorrow = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 9) // Default to 9 AM
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return tomorrow.timeInMillis
            }
            
            text.contains("today") -> {
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 14) // Default to 2 PM
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                // If it's already past 2 PM, set to tomorrow
                if (today.timeInMillis <= now) {
                    today.add(Calendar.DAY_OF_YEAR, 1)
                }
                return today.timeInMillis
            }
            
            text.contains("tonight") || text.contains("tonite") -> {
                val tonight = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 19) // Default to 7 PM
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                // If it's already past 7 PM, set to tomorrow
                if (tonight.timeInMillis <= now) {
                    tonight.add(Calendar.DAY_OF_YEAR, 1)
                }
                return tonight.timeInMillis
            }
            
            text.contains("next week") -> {
                return now + (7 * 24 * 60 * 60 * 1000)
            }
            
            text.contains("in an hour") || text.contains("in 1 hour") -> {
                return now + (60 * 60 * 1000)
            }
            
            text.matches(Regex("in \\d+ hours?")) -> {
                val hours = text.extractNumber()
                if (hours != null) {
                    return now + (hours * 60 * 60 * 1000)
                }
            }
            
            text.matches(Regex("in \\d+ minutes?")) -> {
                val minutes = text.extractNumber()
                if (minutes != null) {
                    return now + (minutes * 60 * 1000)
                }
            }
            
            text.contains("morning") -> {
                val morning = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 8) // 8 AM
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (morning.timeInMillis <= now) {
                    morning.add(Calendar.DAY_OF_YEAR, 1)
                }
                return morning.timeInMillis
            }
            
            text.contains("afternoon") -> {
                val afternoon = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 15) // 3 PM
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (afternoon.timeInMillis <= now) {
                    afternoon.add(Calendar.DAY_OF_YEAR, 1)
                }
                return afternoon.timeInMillis
            }
            
            text.contains("evening") -> {
                val evening = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 18) // 6 PM
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (evening.timeInMillis <= now) {
                    evening.add(Calendar.DAY_OF_YEAR, 1)
                }
                return evening.timeInMillis
            }
        }
        
        return null
    }
    
    private fun parseCommonPhrases(text: String): Long? {
        val now = System.currentTimeMillis()
        
        when {
            text.contains("remind me") -> {
                // Default to tomorrow if no specific time found
                val tomorrow = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return tomorrow.timeInMillis
            }
            
            text.contains("wake me up") || text.contains("wake up") -> {
                val tomorrow = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 7) // 7 AM
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return tomorrow.timeInMillis
            }
        }
        
        return null
    }
    
    private fun calculateDateTime(dayPart: String?, hour: Int, minute: Int, ampm: String?): Long {
        val result = Calendar.getInstance()
        
        // Set the time
        var finalHour = hour
        if (ampm != null) {
            when (ampm.lowercase()) {
                "am" -> {
                    if (hour == 12) finalHour = 0 // 12 AM is midnight
                }
                "pm" -> {
                    if (hour != 12) finalHour += 12 // Convert to 24-hour format
                }
            }
        } else if (hour <= 12 && hour > 0) {
            // If no AM/PM specified, assume PM for times 1-6, AM for 7-12
            finalHour = if (hour <= 6) hour + 12 else hour
        }
        
        Log.d("DateTimeParser", "TIME CONVERSION: hour=$hour, ampm=$ampm -> finalHour=$finalHour")
        
        result.set(Calendar.HOUR_OF_DAY, finalHour)
        result.set(Calendar.MINUTE, minute)
        result.set(Calendar.SECOND, 0)
        result.set(Calendar.MILLISECOND, 0)
        
        // Handle the day part
        when (dayPart?.lowercase()) {
            "mon", "monday" -> setToNextDay(result, Calendar.MONDAY)
            "tue", "tuesday" -> setToNextDay(result, Calendar.TUESDAY)
            "wed", "wednesday" -> setToNextDay(result, Calendar.WEDNESDAY)
            "thu", "thursday" -> setToNextDay(result, Calendar.THURSDAY)
            "fri", "friday" -> setToNextDay(result, Calendar.FRIDAY)
            "sat", "saturday" -> setToNextDay(result, Calendar.SATURDAY)
            "sun", "sunday" -> setToNextDay(result, Calendar.SUNDAY)
            "tomorrow" -> result.add(Calendar.DAY_OF_YEAR, 1)
            "today" -> {
                // If the time has already passed today, move to tomorrow
                if (result.timeInMillis <= System.currentTimeMillis()) {
                    result.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            else -> {
                // If no specific day, check if the time has passed today
                if (result.timeInMillis <= System.currentTimeMillis()) {
                    result.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }
        
        val finalResult = result.timeInMillis
        Log.d("DateTimeParser", "FINAL RESULT: $finalResult (${Date(finalResult)})")
        return finalResult
    }
    
    private fun setToNextDay(cal: Calendar, targetDay: Int) {
        val currentDay = cal.get(Calendar.DAY_OF_WEEK)
        var daysToAdd = targetDay - currentDay
        
        if (daysToAdd <= 0) {
            daysToAdd += 7 // Next week
        }
        
        cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
    }
    
    private fun String.extractNumber(): Int? {
        val pattern = Pattern.compile("\\d+")
        val matcher = pattern.matcher(this)
        return if (matcher.find()) {
            matcher.group()?.toIntOrNull()
        } else {
            null
        }
    }
}