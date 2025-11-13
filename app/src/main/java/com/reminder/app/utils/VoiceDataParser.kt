package com.reminder.app.utils

import android.util.Log
import java.util.regex.Pattern

/**
 * Voice Data Parser - Extracts structured information from raw voice input
 * This can be enhanced over time as we collect more voice samples
 */
class VoiceDataParser {
    
    companion object {
        private const val TAG = "VoiceDataParser"
        
        // Common time patterns
        private val TIME_PATTERNS = listOf(
            Pattern.compile("(\\d{1,2}):(\\d{2})\\s*(am|pm)?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d{1,2})\\s*(am|pm)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("at\\s+(\\d{1,2}):?(\\d{2})?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE)
        )
        
        // Common date patterns
        private val DATE_PATTERNS = listOf(
            Pattern.compile("today", Pattern.CASE_INSENSITIVE),
            Pattern.compile("tomorrow", Pattern.CASE_INSENSITIVE),
            Pattern.compile("next\\s+(week|month|year)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(monday|tuesday|wednesday|thursday|friday|saturday|sunday)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d{1,2})\\s+(january|february|march|april|may|june|july|august|september|october|november|december)", Pattern.CASE_INSENSITIVE)
        )
        
        // Priority patterns
        private val PRIORITY_PATTERNS = listOf(
            Pattern.compile("urgent|important|asap|emergency", Pattern.CASE_INSENSITIVE),
            Pattern.compile("high priority", Pattern.CASE_INSENSITIVE),
            Pattern.compile("low priority|when possible", Pattern.CASE_INSENSITIVE)
        )
    }
    
    data class ParsedReminder(
        val title: String,
        val timeString: String? = null,
        val dateString: String? = null,
        val priority: Int = 5,
        val category: String = "Personal",
        val confidence: Float = 0.0f
    )
    
    /**
     * Parse raw voice input into structured reminder data
     */
    fun parseVoiceInput(rawText: String): ParsedReminder {
        val text = rawText.trim()
        Log.d(TAG, "Parsing voice input: '$text'")
        
        var extractedTime: String? = null
        var extractedDate: String? = null
        var priority = 5
        var category = "Personal"
        var confidence = 0.0f
        
        // Extract time information
        for (pattern in TIME_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                extractedTime = matcher.group()
                confidence += 0.2f
                Log.d(TAG, "Found time: $extractedTime")
                break
            }
        }
        
        // Extract date information
        for (pattern in DATE_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                extractedDate = matcher.group()
                confidence += 0.2f
                Log.d(TAG, "Found date: $extractedDate")
                break
            }
        }
        
        // Extract priority
        for (pattern in PRIORITY_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                when {
                    matcher.group().contains("urgent", ignoreCase = true) -> {
                        priority = 9
                        confidence += 0.1f
                    }
                    matcher.group().contains("high", ignoreCase = true) -> {
                        priority = 8
                        confidence += 0.1f
                    }
                    matcher.group().contains("low", ignoreCase = true) -> {
                        priority = 3
                        confidence += 0.1f
                    }
                }
                Log.d(TAG, "Found priority: $priority")
                break
            }
        }
        
        // Extract category
        category = when {
            text.contains("work", ignoreCase = true) || text.contains("meeting", ignoreCase = true) -> "Work"
            text.contains("family", ignoreCase = true) || text.contains("home", ignoreCase = true) -> "Family"
            else -> "Personal"
        }
        confidence += 0.1f
        
        // Extract title (remove time/date/priority info)
        var title = text
        extractedTime?.let { title = title.replace(it, "").trim() }
        extractedDate?.let { title = title.replace(it, "").trim() }
        
        // Remove common reminder phrases
        title = title.replace(Regex("(remind me to|remember to|don't forget to)"), "").trim()
        
        // Clean up extra spaces
        title = title.replace(Regex("\\s+"), " ")
        
        if (title.isBlank()) {
            title = "Voice Reminder"
        }
        
        Log.d(TAG, "Parsed: title='$title', time=$extractedTime, date=$extractedDate, priority=$priority, category=$category")
        
        return ParsedReminder(
            title = title,
            timeString = extractedTime,
            dateString = extractedDate,
            priority = priority,
            category = category,
            confidence = confidence.coerceAtMost(1.0f)
        )
    }
    
    /**
     * Convert parsed reminder to actual timestamp
     */
    fun calculateReminderTime(parsed: ParsedReminder): Long {
        val now = System.currentTimeMillis()
        
        // Simple time calculation - can be enhanced
        return when {
            parsed.dateString?.contains("today", ignoreCase = true) == true -> {
                // Today + time if specified
                now + (24 * 60 * 60 * 1000) // Tomorrow for now
            }
            parsed.dateString?.contains("tomorrow", ignoreCase = true) == true -> {
                now + (24 * 60 * 60 * 1000) // Tomorrow
            }
            else -> {
                // Default to tomorrow
                now + (24 * 60 * 60 * 1000)
            }
        }
    }
    
    /**
     * Get parsing confidence level
     */
    fun getConfidenceLevel(confidence: Float): String {
        return when {
            confidence >= 0.8f -> "High"
            confidence >= 0.5f -> "Medium"
            else -> "Low"
        }
    }
}