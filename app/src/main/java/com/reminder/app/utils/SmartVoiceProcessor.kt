package com.reminder.app.utils

import android.util.Log
import java.util.regex.Pattern
import kotlinx.coroutines.delay

/**
 * Smart Voice Processor - Converts raw voice input into structured reminders
 * Processes in background to extract task, time, category, and reminder method
 */
class SmartVoiceProcessor {
    
    companion object {
        private const val TAG = "SmartVoiceProcessor"
        
        // Task patterns - what needs to be done
        private val TASK_PATTERNS = listOf(
            Pattern.compile("(call|text|email|message)\\s+(\\w+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(buy|get|pick up|purchase)\\s+(.+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(meet|meeting|appointment)\\s+(with|at)?\\s*(.+)?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(go to|visit|check)\\s+(.+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(finish|complete|do|work on)\\s+(.+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(remind me to|remember to|don't forget to)\\s+(.+)", Pattern.CASE_INSENSITIVE)
        )
        
        // Time patterns - when to do it
        private val TIME_PATTERNS = listOf(
            Pattern.compile("(today|tonight)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(tomorrow|tmrw)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(next\\s+week|next\\s+month|next\\s+year)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(monday|tuesday|wednesday|thursday|friday|saturday|sunday)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d{1,2})\\s*(am|pm)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d{1,2}):(\\d{2})\\s*(am|pm)?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(morning|afternoon|evening|night)", Pattern.CASE_INSENSITIVE)
        )
        
        // Repeat patterns - how often
        private val REPEAT_PATTERNS = listOf(
            Pattern.compile("(every|each)\\s+(day|daily)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(every|each)\\s+week", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(every|each)\\s+month", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(every|each)\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)", Pattern.CASE_INSENSITIVE)
        )
        
        // Priority patterns - urgency level
        private val PRIORITY_PATTERNS = listOf(
            Pattern.compile("(urgent|asap|emergency|critical)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(important|high priority|soon)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(low priority|when possible|eventually)", Pattern.CASE_INSENSITIVE)
        )
        
        // Reminder method patterns - how to notify
        private val METHOD_PATTERNS = listOf(
            Pattern.compile("(text|sms)\\s+me", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(email|mail)\\s+me", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(call|phone)\\s+me", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(alarm|alert|popup)", Pattern.CASE_INSENSITIVE)
        )
    }
    
    data class ProcessedReminder(
        val rawText: String,
        val task: String,
        val category: String,
        val timeString: String?,
        val repeatString: String,
        val reminderMethod: String,
        val priority: Int,
        val confidence: Float
    )
    
    /**
     * Process raw voice input into structured reminder
     */
    suspend fun processVoiceInput(rawText: String): ProcessedReminder {
        Log.d(TAG, "Processing: '$rawText'")
        
        var task = rawText
        var timeString: String? = null
        var repeatString = "none"
        var priority = 5
        var category = "Personal"
        var reminderMethod = "notification"
        var confidence = 0.0f
        
        // Extract task
        task = extractTask(rawText)
        confidence += 0.2f
        
        // Extract time
        timeString = extractTime(rawText)
        if (timeString != null) confidence += 0.2f
        
        // Extract repeat
        repeatString = extractRepeat(rawText)
        if (repeatString != "none") confidence += 0.1f
        
        // Extract priority
        priority = extractPriority(rawText)
        confidence += 0.1f
        
        // Extract category
        category = extractCategory(rawText, task)
        confidence += 0.1f
        
        // Extract reminder method
        reminderMethod = extractReminderMethod(rawText)
        confidence += 0.1f
        
        // Clean up task
        task = cleanTask(task, timeString, repeatString)
        
        val processed = ProcessedReminder(
            rawText = rawText,
            task = task,
            category = category,
            timeString = timeString,
            repeatString = repeatString,
            reminderMethod = reminderMethod,
            priority = priority,
            confidence = confidence.coerceAtMost(1.0f)
        )
        
        Log.d(TAG, "Processed: task='$task', time=$timeString, repeat=$repeatString, method=$reminderMethod, priority=$priority")
        
        return processed
    }
    
    private fun extractTask(text: String): String {
        for (pattern in TASK_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return when {
                    matcher.groupCount() >= 2 -> matcher.group(2) ?: text
                    matcher.groupCount() >= 1 -> matcher.group(1) ?: text
                    else -> text
                }
            }
        }
        return text.replace(Regex("(remind me to|remember to|don't forget to)"), "").trim()
    }
    
    private fun extractTime(text: String): String? {
        for (pattern in TIME_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group()
            }
        }
        return null
    }
    
    private fun extractRepeat(text: String): String {
        for (pattern in REPEAT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return when {
                    matcher.group().contains("day", ignoreCase = true) -> "daily"
                    matcher.group().contains("week", ignoreCase = true) -> "weekly"
                    matcher.group().contains("month", ignoreCase = true) -> "monthly"
                    else -> "weekly"
                }
            }
        }
        return "none"
    }
    
    private fun extractPriority(text: String): Int {
        for (pattern in PRIORITY_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return when {
                    matcher.group().contains("urgent", ignoreCase = true) -> 9
                    matcher.group().contains("important", ignoreCase = true) -> 8
                    matcher.group().contains("low", ignoreCase = true) -> 3
                    else -> 5
                }
            }
        }
        return 5
    }
    
    private fun extractCategory(text: String, task: String): String {
        val combinedText = "$text $task".lowercase()
        return when {
            combinedText.contains("work") || combinedText.contains("meeting") || 
            combinedText.contains("boss") || combinedText.contains("office") -> "Work"
            combinedText.contains("family") || combinedText.contains("mom") || 
            combinedText.contains("dad") || combinedText.contains("home") -> "Family"
            combinedText.contains("buy") || combinedText.contains("store") || 
            combinedText.contains("groceries") || combinedText.contains("shopping") -> "Shopping"
            combinedText.contains("health") || combinedText.contains("doctor") || 
            combinedText.contains("medicine") -> "Health"
            else -> "Personal"
        }
    }
    
    private fun extractReminderMethod(text: String): String {
        for (pattern in METHOD_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return when {
                    matcher.group().contains("text", ignoreCase = true) -> "text"
                    matcher.group().contains("email", ignoreCase = true) -> "email"
                    matcher.group().contains("call", ignoreCase = true) -> "call"
                    matcher.group().contains("alarm", ignoreCase = true) -> "alarm"
                    else -> "notification"
                }
            }
        }
        return "notification"
    }
    
    private fun cleanTask(task: String, timeString: String?, repeatString: String): String {
        var cleaned = task
        
        // Remove time and repeat info from task
        timeString?.let { cleaned = cleaned.replace(it, "").trim() }
        if (repeatString != "none") {
            cleaned = cleaned.replace(Regex("(every|each)\\s+(day|week|month)"), "").trim()
        }
        
        // Remove extra words
        cleaned = cleaned.replace(Regex("\\b(at|on|in|for|to|the)\\b"), "").trim()
        cleaned = cleaned.replace(Regex("\\s+"), " ")
        
        return cleaned.ifBlank { "Reminder" }
    }
    
    /**
     * Calculate reminder time from extracted time string
     */
    fun calculateReminderTime(timeString: String?, repeatString: String): Long {
        val now = System.currentTimeMillis()
        
        return when {
            timeString?.contains("today", ignoreCase = true) == true -> {
                now + (2 * 60 * 60 * 1000) // 2 hours from now
            }
            timeString?.contains("tomorrow", ignoreCase = true) == true -> {
                now + (24 * 60 * 60 * 1000) // Tomorrow
            }
            timeString?.contains("next week", ignoreCase = true) == true -> {
                now + (7 * 24 * 60 * 60 * 1000) // Next week
            }
            timeString?.contains("next month", ignoreCase = true) == true -> {
                now + (30 * 24 * 60 * 60 * 1000) // Next month
            }
            repeatString == "daily" -> {
                now + (24 * 60 * 60 * 1000) // Tomorrow for daily
            }
            repeatString == "weekly" -> {
                now + (7 * 24 * 60 * 60 * 1000) // Next week for weekly
            }
            repeatString == "monthly" -> {
                now + (30 * 24 * 60 * 60 * 1000) // Next month for monthly
            }
            else -> {
                now + (24 * 60 * 60 * 1000) // Default to tomorrow
            }
        }
    }
    
    /**
     * Get confidence level description
     */
    fun getConfidenceDescription(confidence: Float): String {
        return when {
            confidence >= 0.8f -> "High confidence - AI understood well"
            confidence >= 0.5f -> "Medium confidence - AI understood most"
            else -> "Low confidence - Please check details"
        }
    }
}