package com.reminder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.Blue
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import com.reminder.app.utils.SpeechManager
import com.reminder.app.utils.SmartVoiceProcessor
import com.reminder.app.viewmodel.ReminderViewModel
import kotlinx.coroutines.launch

// Enhanced extraction functions for common reminder patterns

fun extractCategory(text: String): String {
    return when {
        text.contains("work", ignoreCase = true) || text.contains("meeting", ignoreCase = true) || 
        text.contains("office", ignoreCase = true) || text.contains("client", ignoreCase = true) -> "Work"
        text.contains("family", ignoreCase = true) || text.contains("mom", ignoreCase = true) || 
        text.contains("dad", ignoreCase = true) || text.contains("kids", ignoreCase = true) -> "Family"
        text.contains("buy", ignoreCase = true) || text.contains("store", ignoreCase = true) || 
        text.contains("shop", ignoreCase = true) || text.contains("grocery", ignoreCase = true) -> "Shopping"
        text.contains("doctor", ignoreCase = true) || text.contains("appointment", ignoreCase = true) || 
        text.contains("health", ignoreCase = true) -> "Health"
        text.contains("call", ignoreCase = true) || text.contains("email", ignoreCase = true) || 
        text.contains("text", ignoreCase = true) -> "Communication"
        else -> "Personal"
    }
}

fun extractTime(text: String): String {
    return when {
        text.contains("today", ignoreCase = true) -> {
            when {
                text.contains("morning", ignoreCase = true) -> "Today Morning"
                text.contains("afternoon", ignoreCase = true) -> "Today Afternoon"
                text.contains("evening", ignoreCase = true) -> "Today Evening"
                text.contains("night", ignoreCase = true) -> "Tonight"
                else -> "Today"
            }
        }
        text.contains("tomorrow", ignoreCase = true) -> {
            when {
                text.contains("morning", ignoreCase = true) -> "Tomorrow Morning"
                text.contains("afternoon", ignoreCase = true) -> "Tomorrow Afternoon"
                text.contains("evening", ignoreCase = true) -> "Tomorrow Evening"
                text.contains("night", ignoreCase = true) -> "Tomorrow Night"
                else -> "Tomorrow"
            }
        }
        text.contains("next week", ignoreCase = true) -> "Next Week"
        text.contains("monday", ignoreCase = true) -> "Monday"
        text.contains("tuesday", ignoreCase = true) -> "Tuesday"
        text.contains("wednesday", ignoreCase = true) -> "Wednesday"
        text.contains("thursday", ignoreCase = true) -> "Thursday"
        text.contains("friday", ignoreCase = true) -> "Friday"
        text.contains("saturday", ignoreCase = true) -> "Saturday"
        text.contains("sunday", ignoreCase = true) -> "Sunday"
        text.contains("morning", ignoreCase = true) -> "Morning"
        text.contains("afternoon", ignoreCase = true) -> "Afternoon"
        text.contains("evening", ignoreCase = true) -> "Evening"
        text.contains("night", ignoreCase = true) -> "Night"
        else -> ""
    }
}

fun extractDay(text: String): String {
    val lowerText = text.lowercase()
    return when {
        lowerText.contains("today") -> "Today"
        lowerText.contains("tomorrow") -> "Tomorrow"
        lowerText.contains("monday") -> "Monday"
        lowerText.contains("tuesday") -> "Tuesday"
        lowerText.contains("wednesday") -> "Wednesday"
        lowerText.contains("thursday") -> "Thursday"
        lowerText.contains("friday") -> "Friday"
        lowerText.contains("saturday") -> "Saturday"
        lowerText.contains("sunday") -> "Sunday"
        lowerText.contains("next week") -> "Next Week"
        else -> ""
    }
}

fun extractTimeOnly(text: String): String {
    val timePatterns = listOf(
        Regex("(\\d{1,2})\\s*[:\\.]?\\s*\\d{0,2}\\s*(a\\.?m\\.?|p\\.?m\\.?)", RegexOption.IGNORE_CASE),
        Regex("(\\d{1,2})\\s*(am|pm)", RegexOption.IGNORE_CASE),
        Regex("(\\d{1,2}):(\\d{2})\\s*(am|pm)", RegexOption.IGNORE_CASE),
        Regex("(\\d{1,2})\\s*o'clock", RegexOption.IGNORE_CASE)
    )
    
    for (pattern in timePatterns) {
        val match = pattern.find(text)
        if (match != null) {
            val timeText = match.value.lowercase()
                .replace(".", "")
                .replace(" ", "")
                .trim()
            Log.d("TimeExtraction", "Found time: '$timeText' in text: '$text'")
            return timeText
        }
    }
    
    Log.d("TimeExtraction", "No time found in text: '$text'")
    return ""
}

fun extractPriority(text: String): String {
    return when {
        text.contains("urgent", ignoreCase = true) || text.contains("asap", ignoreCase = true) || 
        text.contains("important", ignoreCase = true) -> "High"
        text.contains("sometime", ignoreCase = true) || text.contains("when possible", ignoreCase = true) -> "Low"
        else -> "Medium"
    }
}

fun calculateReminderTime(text: String): Long {
    val now = System.currentTimeMillis()
    val oneDay = 24 * 60 * 60 * 1000L
    val oneHour = 60 * 60 * 1000L
    val calendar = java.util.Calendar.getInstance()
    
    // Extract specific time like "3pm", "3:00", etc.
    val timePattern = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE)
    val timeMatch = timePattern.find(text)
    
    var targetHour = -1
    var targetMinute = 0
    
    if (timeMatch != null) {
        val hour = timeMatch.groupValues[1].toInt()
        val minute = timeMatch.groupValues[2].takeIf { it.isNotBlank() }?.toInt() ?: 0
        val ampm = timeMatch.groupValues[3].lowercase()
        
        targetHour = when {
            ampm == "am" -> if (hour == 12) 0 else hour
            ampm == "pm" -> if (hour == 12) 12 else hour + 12
            hour <= 12 -> hour // Default to AM for single digit hours
            else -> hour
        }
        targetMinute = minute
    }
    
    // Calculate base time (day offset)
    val baseTime = when {
        text.contains("today", ignoreCase = true) -> now
        text.contains("tomorrow", ignoreCase = true) -> now + oneDay
        text.contains("next week", ignoreCase = true) -> now + 7 * oneDay
        text.contains("monday", ignoreCase = true) -> now + getDaysUntilDayOfWeek(1) * oneDay
        text.contains("tuesday", ignoreCase = true) -> now + getDaysUntilDayOfWeek(2) * oneDay
        text.contains("wednesday", ignoreCase = true) -> now + getDaysUntilDayOfWeek(3) * oneDay
        text.contains("thursday", ignoreCase = true) -> now + getDaysUntilDayOfWeek(4) * oneDay
        text.contains("friday", ignoreCase = true) -> now + getDaysUntilDayOfWeek(5) * oneDay
        text.contains("saturday", ignoreCase = true) -> now + getDaysUntilDayOfWeek(6) * oneDay
        text.contains("sunday", ignoreCase = true) -> now + getDaysUntilDayOfWeek(7) * oneDay
        else -> now + oneDay // Default to tomorrow
    }
    
    // If we have a specific time, set it
    return if (targetHour != -1) {
        val targetCalendar = java.util.Calendar.getInstance()
        targetCalendar.timeInMillis = baseTime
        targetCalendar.set(java.util.Calendar.HOUR_OF_DAY, targetHour)
        targetCalendar.set(java.util.Calendar.MINUTE, targetMinute)
        targetCalendar.set(java.util.Calendar.SECOND, 0)
        targetCalendar.set(java.util.Calendar.MILLISECOND, 0)
        
        // If the time is in the past for today, move to tomorrow
        if (text.contains("today", ignoreCase = true) && targetCalendar.timeInMillis <= now) {
            targetCalendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        
        targetCalendar.timeInMillis
    } else {
        // Use time-of-day defaults if no specific time
        when {
            text.contains("morning", ignoreCase = true) -> baseTime + 9 * oneHour
            text.contains("afternoon", ignoreCase = true) -> baseTime + 14 * oneHour
            text.contains("evening", ignoreCase = true) -> baseTime + 18 * oneHour
            text.contains("night", ignoreCase = true) -> baseTime + 20 * oneHour
            else -> baseTime + 12 * oneHour // Default to noon
        }
    }
}

fun getDaysUntilDayOfWeek(targetDay: Int): Int {
    val calendar = java.util.Calendar.getInstance()
    val currentDay = calendar.get(java.util.Calendar.DAY_OF_WEEK)
    val daysUntil = if (targetDay >= currentDay) targetDay - currentDay else 7 - (currentDay - targetDay)
    return if (daysUntil == 0) 7 else daysUntil // If today, schedule for next week
}

// Compact Slider Date Picker Dialog Component
@Composable
fun DatePickerDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val today = LocalDate.now()
    var tempYear by remember { mutableStateOf(selectedDate.year.toFloat()) }
    var tempMonth by remember { mutableStateOf((selectedDate.monthValue - 1).toFloat()) } // 0-based
    var tempDay by remember { mutableStateOf(selectedDate.dayOfMonth.toFloat()) }
    
    // Helper functions for date formatting
    fun formatMonth(month: Float): String {
        return java.time.Month.of((month.toInt() + 1)).name.take(3)
    }
    
    fun formatDate(y: Float, m: Float, d: Float): String {
        val year = y.toInt()
        val month = java.time.Month.of((m.toInt() + 1)).name.take(3)
        val day = d.toInt()
        return "$month $day, $year"
    }
    
    // Get max days for current month/year
    fun getMaxDays(): Int {
        return try {
            val year = tempYear.toInt()
            val month = tempMonth.toInt() + 1
            LocalDate.of(year, month, 1).lengthOfMonth()
        } catch (e: Exception) {
            31
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title with current date preview
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Text(
                        text = "📅 Select Date",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Compact date display
                    Text(
                        text = formatDate(tempYear, tempMonth, tempDay),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Year with +/- buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Year",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                val newYear = (tempYear - 1).coerceAtLeast(today.year - 5f)
                                tempYear = newYear
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(
                                text = "−",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Slider(
                            value = tempYear,
                            onValueChange = { tempYear = it },
                            valueRange = (today.year - 5).toFloat()..(today.year + 10).toFloat(),
                            steps = 14,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                        
                        IconButton(
                            onClick = { 
                                val newYear = (tempYear + 1).coerceAtMost(today.year + 10f)
                                tempYear = newYear
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Text(
                        text = "${tempYear.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Month with +/- buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                val newMonth = if (tempMonth > 0) tempMonth - 1 else 11f
                                tempMonth = newMonth
                                // Adjust day if it exceeds max days in new month
                                val maxDays = getMaxDays()
                                if (tempDay > maxDays) {
                                    tempDay = maxDays.toFloat()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(
                                text = "−",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        
                        // Smaller Priority Slider
                        Slider(
                            value = tempMonth,
                            onValueChange = { 
                                tempMonth = it
                                // Adjust day if it exceeds max days in new month
                                val maxDays = getMaxDays()
                                if (tempDay > maxDays) {
                                    tempDay = maxDays.toFloat()
                                }
                            },
                            valueRange = 0f..11f,
                            steps = 10,
                            modifier = Modifier.weight(0.8f).padding(horizontal = 4.dp), // Smaller - takes less space
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.secondary,
                                activeTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                        
                        IconButton(
                            onClick = { 
                                val newMonth = if (tempMonth < 11) tempMonth + 1 else 0f
                                tempMonth = newMonth
                                // Adjust day if it exceeds max days in new month
                                val maxDays = getMaxDays()
                                if (tempDay > maxDays) {
                                    tempDay = maxDays.toFloat()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    
                    Text(
                        text = formatMonth(tempMonth),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Day with +/- buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    val maxDays = getMaxDays()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                val newDay = if (tempDay > 1) tempDay - 1 else maxDays.toFloat()
                                tempDay = newDay
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(
                                text = "−",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        
                        Slider(
                            value = tempDay,
                            onValueChange = { tempDay = it },
                            valueRange = 1f..maxDays.toFloat(),
                            steps = maxDays - 2,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.tertiary,
                                activeTrackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                        
                        IconButton(
                            onClick = { 
                                val newDay = if (tempDay < maxDays) tempDay + 1 else 1f
                                tempDay = newDay
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    
                    Text(
                        text = "${tempDay.toInt()}${getDaySuffix(tempDay.toInt())}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Quick Date Selection Buttons
                Text(
                    text = "Quick Select",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(listOf("Today", "Tomorrow", "Next Week")) { quickDate ->
                        val isToday = quickDate == "Today"
                        OutlinedButton(
                            onClick = {
                                when (quickDate) {
                                    "Today" -> {
                                        tempYear = today.year.toFloat()
                                        tempMonth = (today.monthValue - 1).toFloat()
                                        tempDay = today.dayOfMonth.toFloat()
                                    }
                                    "Tomorrow" -> {
                                        val tomorrow = today.plusDays(1)
                                        tempYear = tomorrow.year.toFloat()
                                        tempMonth = (tomorrow.monthValue - 1).toFloat()
                                        tempDay = tomorrow.dayOfMonth.toFloat()
                                    }
                                    "Next Week" -> {
                                        val nextWeek = today.plusDays(7)
                                        tempYear = nextWeek.year.toFloat()
                                        tempMonth = (nextWeek.monthValue - 1).toFloat()
                                        tempDay = nextWeek.dayOfMonth.toFloat()
                                    }
                                }
                            },
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isToday) {
                                    // Animated Today icon
                                    Text(
                                        text = "🌟",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text(
                                        text = "Today",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    when (quickDate) {
                                        "Tomorrow" -> {
                                            Text(
                                                text = "📅",
                                                style = MaterialTheme.typography.titleSmall,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                        }
                                        "Next Week" -> {
                                            Text(
                                                text = "🗓️",
                                                style = MaterialTheme.typography.titleSmall,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = quickDate,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            try {
                                val newDate = LocalDate.of(tempYear.toInt(), tempMonth.toInt() + 1, tempDay.toInt())
                                onDateSelected(newDate)
                                onDismiss()
                            } catch (e: Exception) {
                                // Invalid date, ignore
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Set Date")
                    }
                }
            }
        }
    }
}

// Helper function to get day suffix (1st, 2nd, 3rd, 4th, etc.)
fun getDaySuffix(day: Int): String {
    return when (day % 100) {
        11, 12, 13 -> "th"
        else -> when (day % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
}

// Modern Slider Time Picker Dialog Component
@Composable
fun TimePickerDialog(
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    // Directly use selectedTime values - no separate state needed
    val hour = remember { derivedStateOf { selectedTime.hour.toFloat() } }
    val minute = remember { derivedStateOf { selectedTime.minute.toFloat() } }
    
    android.util.Log.d("TimePickerDialog", "Dialog opened with selectedTime=$selectedTime, hour=$hour, minute=$minute")
    
    // Helper functions for time formatting
    fun formatHour(h: Float): String {
        val hour24 = h.toInt()
        val displayHour = when {
            hour24 == 0 -> "12"
            hour24 <= 12 -> hour24.toString()
            else -> (hour24 - 12).toString()
        }
        val period = if (hour24 < 12) "AM" else "PM"
        return "$displayHour $period"
    }
    
    fun formatTime(h: Float, m: Float): String {
        val hour24 = h.toInt()
        val displayHour = when {
            hour24 == 0 -> "12"
            hour24 <= 12 -> hour24.toString()
            else -> (hour24 - 12).toString()
        }
        val period = if (hour24 < 12) "AM" else "PM"
        val hourInt = displayHour.toIntOrNull() ?: 12
        return String.format("%02d:%02d %s", hourInt, m.toInt(), period)
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title with current time preview
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "⏰ Select Time",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Large time display
                    Text(
                        text = formatTime(hour, minute),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Hour Slider
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Hour",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                     Row(
                         modifier = Modifier.fillMaxWidth(),
                         verticalAlignment = Alignment.CenterVertically,
                         horizontalArrangement = Arrangement.spacedBy(8.dp)
                     ) {
                         IconButton(
                             onClick = { 
                                 val newHour = (hour.toInt() - 1).coerceIn(0, 23)
                                 hour = newHour.toFloat()
                             },
                             modifier = Modifier.size(48.dp)
                         ) {
                             Text("−", style = MaterialTheme.typography.titleLarge)
                         }
                         
                         Slider(
                             value = hour,
                             onValueChange = { hour = it },
                             valueRange = 0f..23f,
                             steps = 22, // 24 values - 2 endpoints = 22 steps
                             modifier = Modifier.weight(1f),
                             colors = SliderDefaults.colors(
                                 thumbColor = MaterialTheme.colorScheme.primary,
                                 activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                 inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                             )
                         )
                         
                         IconButton(
                             onClick = { 
                                 val newHour = (hour.toInt() + 1).coerceIn(0, 23)
                                 hour = newHour.toFloat()
                             },
                             modifier = Modifier.size(48.dp)
                         ) {
                             Text("+", style = MaterialTheme.typography.titleLarge)
                         }
                     }
                    
                    Text(
                        text = formatHour(hour),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Minute Slider
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Minute",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                     Row(
                         modifier = Modifier.fillMaxWidth(),
                         verticalAlignment = Alignment.CenterVertically,
                         horizontalArrangement = Arrangement.spacedBy(8.dp)
                     ) {
                         IconButton(
                             onClick = { 
                                 val newMinute = (minute.toInt() - 1).coerceIn(0, 59)
                                 minute = newMinute.toFloat()
                             },
                             modifier = Modifier.size(48.dp)
                         ) {
                             Text("−", style = MaterialTheme.typography.titleLarge)
                         }
                         
                         Slider(
                             value = minute,
                             onValueChange = { minute = it },
                             valueRange = 0f..59f,
                             steps = 58, // 60 values - 2 endpoints = 58 steps
                             modifier = Modifier.weight(1f),
                             colors = SliderDefaults.colors(
                                 thumbColor = MaterialTheme.colorScheme.secondary,
                                 activeTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                 inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                             )
                         )
                         
                         IconButton(
                             onClick = { 
                                 val newMinute = (minute.toInt() + 1).coerceIn(0, 59)
                                 minute = newMinute.toFloat()
                             },
                             modifier = Modifier.size(48.dp)
                         ) {
                             Text("+", style = MaterialTheme.typography.titleLarge)
                         }
                     }
                    
                    Text(
                        text = String.format("%02d minutes", minute.toInt()),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Quick Time Selection Chips
                Text(
                    text = "Quick Select",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf("9AM", "12PM", "3PM", "6PM", "8AM", "2PM", "5PM", "7PM")) { time ->
                        OutlinedButton(
                            onClick = {
                                val (h, m) = when (time) {
                                    "8AM" -> Pair(8f, 0f)
                                    "9AM" -> Pair(9f, 0f)
                                    "12PM" -> Pair(12f, 0f)
                                    "2PM" -> Pair(14f, 0f)
                                    "3PM" -> Pair(15f, 0f)
                                    "5PM" -> Pair(17f, 0f)
                                    "6PM" -> Pair(18f, 0f)
                                    "7PM" -> Pair(19f, 0f)
                                    else -> Pair(hour, minute)
                                }
                                hour = h
                                minute = m
                            },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = time,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            onTimeSelected(LocalTime.of(hour.toInt(), minute.toInt()))
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Set Time")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(
    viewModel: ReminderViewModel,
    speechManager: SpeechManager,
    reminderId: Int?,
    onBack: () -> Unit,
    onConfirm: (String, Long) -> Unit,
    onCalendarClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Single content field - supports both typing and voice
    var content by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var loadedReminder by remember { mutableStateOf<com.reminder.app.data.Reminder?>(null) }
    
    // Voice input enhancements
    var speechToText by remember { mutableStateOf("") }
    var isVoiceActive by remember { mutableStateOf(false) }
    
    // Priority selection
    var selectedPriority by remember { mutableStateOf(5) }
    
    // Trigger configuration state
    var showTriggerConfig by remember { mutableStateOf(false) }
    var enableAtDueTime by remember { mutableStateOf(true) }
    var enableMinutesBefore by remember { mutableStateOf(false) }
    var minutesBeforeValue by remember { mutableStateOf(15) }
    var enableHoursBefore by remember { mutableStateOf(false) }
    var hoursBeforeValue by remember { mutableStateOf(1) }
    var enableDaysBefore by remember { mutableStateOf(false) }
    var daysBeforeValue by remember { mutableStateOf(1) }
    
    // Enhanced date/time state
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.NOON) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showTimeSuggestions by remember { mutableStateOf(false) }
    var whenDay by remember { mutableStateOf("") }
    var whenTime by remember { mutableStateOf("") }
    
    // Common time suggestions
    val timeSuggestions = listOf(
        "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
        "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM",
        "5:00 PM", "6:00 PM", "7:00 PM", "8:00 PM",
        "Morning", "Afternoon", "Evening", "Night"
    )
    val priorityOptions = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val priorityLabels = mapOf(
        1 to "Very Low", 2 to "Low", 3 to "Low-Medium", 4 to "Medium-Low",
        5 to "Medium", 6 to "Medium-High", 7 to "High-Medium", 
        8 to "High", 9 to "Very High", 10 to "Urgent"
    )
    
    // Load existing reminder data if editing
    LaunchedEffect(reminderId) {
        reminderId?.let { id ->
            scope.launch {
                try {
                    // Add a small delay to ensure database operations are complete
                    kotlinx.coroutines.delay(100)
                    val reminder = viewModel.getReminderById(id)
                    if (reminder != null) {
                        loadedReminder = reminder
                        content = reminder.content
                        selectedPriority = reminder.importance
                        whenDay = reminder.whenDay ?: ""
                        whenTime = reminder.whenTime ?: ""
                        android.util.Log.d("InputScreen", "Loaded whenDay='$whenDay', whenTime='$whenTime' from database")
                        
                        // Restore selectedDate and selectedTime from reminderTime
                        try {
                            // Always restore date from reminderTime
                            val reminderDateTime = java.time.Instant.ofEpochMilli(reminder.reminderTime)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime()
                            selectedDate = reminderDateTime.toLocalDate()
                            
                            // Try to parse whenTime if it exists, otherwise use reminderTime
                            if (!reminder.whenTime.isNullOrBlank()) {
                                android.util.Log.d("InputScreen", "Attempting to parse whenTime: '${reminder.whenTime}'")
                                
                                // Try multiple time formats
                                val timeFormats = listOf(
                                    Regex("(\\d{1,2}):(\\d{2})\\s*(am|pm)", RegexOption.IGNORE_CASE),  // 3:30pm, 11:45 am
                                    Regex("(\\d{1,2})\\s*(am|pm)", RegexOption.IGNORE_CASE),           // 3pm, 11 am
                                    Regex("(\\d{1,2}):(\\d{2})"),                                   // 15:30, 09:45
                                    Regex("(\\d{1,2})")                                              // 15, 9
                                )
                                
                                var parsed = false
                                for (format in timeFormats) {
                                    val match = format.find(reminder.whenTime)
                                    if (match != null) {
                                        try {
                                            val hour = match.groupValues[1].toInt()
                                            val minute = match.groupValues.getOrNull(2)?.toInt() ?: 0
                                            val ampm = match.groupValues.getOrNull(3)?.lowercase()
                                            
                                            val parsedHour = when {
                                                ampm == "am" -> if (hour == 12) 0 else hour
                                                ampm == "pm" -> if (hour == 12) 12 else hour + 12
                                                hour > 12 -> hour // Already 24-hour format
                                                else -> if (hour <= 12 && ampm == null && hour != 12) hour + 12 else hour
                                            }
                                            
                                            selectedTime = java.time.LocalTime.of(parsedHour.coerceIn(0, 23), minute.coerceIn(0, 59))
                                            android.util.Log.d("InputScreen", "Successfully parsed selectedTime='$selectedTime' from whenTime='${reminder.whenTime}'")
                                            parsed = true
                                            break
                                        } catch (e: Exception) {
                                            android.util.Log.d("InputScreen", "Failed to parse with format: ${e.message}")
                                            continue
                                        }
                                    }
                                }
                                
                                if (!parsed) {
                                    android.util.Log.d("InputScreen", "Could not parse whenTime, using reminderTime")
                                    selectedTime = reminderDateTime.toLocalTime()
                                }
                            } else {
                                // No whenTime saved, use reminderTime
                                selectedTime = reminderDateTime.toLocalTime()
                                android.util.Log.d("InputScreen", "No whenTime saved, using selectedTime='$selectedTime' from reminderTime")
                            }
                            
                        } catch (e: Exception) {
                            // Fallback to current date/time if parsing fails
                            selectedDate = java.time.LocalDate.now()
                            selectedTime = java.time.LocalTime.NOON
                            android.util.Log.d("InputScreen", "Failed to parse time: ${e.message}, using NOON")
                        }
                        
                        android.util.Log.d("InputScreen", "Loaded reminder for editing: id=${reminder.id}, reminderTime=${reminder.reminderTime}, whenDay=${reminder.whenDay}, whenTime=${reminder.whenTime}")
                        android.util.Log.d("InputScreen", "Final selectedTime='$selectedTime', selectedDate='$selectedDate'")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("InputScreen", "Error loading reminder: ${e.message}")
                }
            }
        }
    }
    
    // Reload data when screen becomes visible (handle potential stale data)
    LaunchedEffect(reminderId, loadedReminder) {
        reminderId?.let { id ->
            if (loadedReminder != null) {
                scope.launch {
                    // Double-check the data after a short delay to catch any race conditions
                    kotlinx.coroutines.delay(300)
                    val freshReminder = viewModel.getReminderById(id)
                    if (freshReminder != null && freshReminder.reminderTime != loadedReminder.reminderTime) {
                        android.util.Log.d("InputScreen", "Detected stale data, reloading: old=${loadedReminder.reminderTime}, new=${freshReminder.reminderTime}")
                        loadedReminder = freshReminder
                        content = freshReminder.content
                        selectedPriority = freshReminder.importance
                        whenDay = freshReminder.whenDay ?: ""
                        whenTime = freshReminder.whenTime ?: ""
                        
                        // Update date/time fields
                        try {
                            val reminderDateTime = java.time.Instant.ofEpochMilli(freshReminder.reminderTime)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime()
                            selectedDate = reminderDateTime.toLocalDate()
                            selectedTime = reminderDateTime.toLocalTime()
                        } catch (e: Exception) {
                            selectedDate = java.time.LocalDate.now()
                            selectedTime = java.time.LocalTime.NOON
                        }
                    }
                }
            }
        }
    }
    
    // Speech states
    val isListening by speechManager.isListening.collectAsState()
    val speechResult by speechManager.speechResult.collectAsState()
    
    // Function to launch keyboard voice input
    fun launchKeyboardVoiceInput() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "What do you need to remember?")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            
            (context as? Activity)?.startActivityForResult(intent, 1002)
        } catch (e: Exception) {
            Log.e("InputScreen", "Error launching keyboard voice input: ${e.message}")
        }
    }
    
    // Simple processing for user content
    LaunchedEffect(content) {
        if (content.isNotBlank() && !isProcessing) {
            isProcessing = true
            scope.launch {
                try {
                    // Simple processing with delay for better UX
                    kotlinx.coroutines.delay(300)
                    
                    // Extract basic info
                    val processedCategory = extractCategory(content)
                    val processedTime = extractTime(content)
                    val processedPriority = extractPriority(content)
                    
                    // Extract day and time separately
                    val extractedDay = extractDay(content)
                    val extractedTime = extractTimeOnly(content)
                    
                    // Always auto-fill day and time fields when content changes
                    whenDay = extractedDay
                    whenTime = extractedTime
                    
                    Log.d("InputScreen", "Processed: category='$processedCategory', time='$processedTime', priority='$processedPriority', day='$extractedDay', timeOnly='$extractedTime'")
                } catch (e: Exception) {
                    Log.e("InputScreen", "Error processing content: ${e.message}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }
    

    
    // Handle speech results
    LaunchedEffect(speechResult) {
        speechResult?.let { result ->
            if (!result.contains("permission") && !result.contains("not available") && !result.contains("error") && !result.contains("Try:") && !result.contains("Hey Google")) {
                content = result
            }
            speechManager.clearSpeechResult()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Reminder") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onCalendarClick) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar")
                    }
                    Button(
                        onClick = {
                            if (content.isNotBlank()) {
                                // Create reminder with enhanced date/time
                                val reminderDateTime = java.time.LocalDateTime.of(selectedDate, selectedTime)
                                val reminderTimeMillis = reminderDateTime
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()
                                
                                // Only update whenDay and whenTime if they are empty (preserve user input)
                                if (whenDay.isBlank()) {
                                    whenDay = when {
                                        selectedDate == LocalDate.now() -> "Today"
                                        selectedDate == LocalDate.now().plusDays(1) -> "Tomorrow"
                                        else -> selectedDate.format(DateTimeFormatter.ofPattern("EEEE"))
                                    }
                                }
                                if (whenTime.isBlank()) {
                                    whenTime = selectedTime.format(DateTimeFormatter.ofPattern("h:mm a"))
                                }
                                
                                // Build trigger points JSON
                                val triggerPoints = mutableListOf<com.reminder.app.data.TriggerPoint>()
                                if (enableAtDueTime) {
                                    triggerPoints.add(com.reminder.app.data.TriggerPoint(com.reminder.app.data.TriggerType.AT_DUE_TIME))
                                }
                                if (enableMinutesBefore) {
                                    triggerPoints.add(com.reminder.app.data.TriggerPoint(com.reminder.app.data.TriggerType.MINUTES_BEFORE, minutesBeforeValue))
                                }
                                if (enableHoursBefore) {
                                    triggerPoints.add(com.reminder.app.data.TriggerType.HOURS_BEFORE, hoursBeforeValue))
                                }
                                if (enableDaysBefore) {
                                    triggerPoints.add(com.reminder.app.data.TriggerType.DAYS_BEFORE, daysBeforeValue))
                                }
                                
                                // Convert to JSON
                                val triggerPointsJson = org.json.JSONArray().apply {
                                    triggerPoints.forEach { trigger ->
                                        put(org.json.JSONObject().apply {
                                            put("type", trigger.type.name)
                                            put("value", trigger.value)
                                            put("customOffsetMs", trigger.customOffsetMs)
                                            put("enableFlash", true)
                                            put("enableSound", true)
                                            put("enableVibration", true)
                                        })
                                    }
                                }.toString()
                                
                                val reminder = com.reminder.app.data.Reminder(
                                    content = content,
                                    category = extractCategory(content),
                                    importance = selectedPriority,
                                    reminderTime = reminderTimeMillis,
                                    whenDay = whenDay.ifBlank { null },
                                    whenTime = whenTime.ifBlank { null },
                                    voiceInput = content,
                                    isProcessed = true,
                                    triggerPoints = triggerPointsJson
                                )
                                
                                if (reminderId != null) {
                                    // Update existing reminder
                                    val updatedReminder = reminder.copy(id = reminderId)
                                    android.util.Log.d("InputScreen", "Updating reminder: id=${reminderId}, newReminderTime=${reminder.reminderTime}, whenDay=${whenDay}, whenTime=${whenTime}")
                                    viewModel.updateReminder(updatedReminder)
                                    // Add a small delay before navigating back to ensure database update is complete
                                    kotlinx.coroutines.delay(200)
                                } else {
                                    // Add new reminder
                                    android.util.Log.d("InputScreen", "Adding new reminder: reminderTime=${reminder.reminderTime}, whenDay=${whenDay}, whenTime=${whenTime}")
                                    viewModel.addReminder(reminder)
                                }
                                onBack()
                            }
                        },
                        enabled = content.isNotBlank() && !isProcessing
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Voice Input Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📝 Create Your Reminder",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Single content field (typing + voice)
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("What do you need to remember?") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        placeholder = { Text("e.g., Call mom tomorrow at 3pm or Buy groceries after work") }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Modern Date Picker
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "📅 Date",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Select Date",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Modern Time Picker
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true },
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "⏰ Time",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = selectedTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = "Select Time",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Quick Time Suggestions Dropdown
                    ExposedDropdownMenuBox(
                        expanded = showTimeSuggestions,
                        onExpandedChange = { showTimeSuggestions = it }
                    ) {
                        OutlinedTextField(
                            value = whenTime,
                            onValueChange = { 
                                whenTime = it
                                showTimeSuggestions = false
                            },
                            label = { Text("Quick Time (optional)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            placeholder = { Text("e.g., Morning, 3pm, 2:30pm") },
                            trailingIcon = {
                                IconButton(onClick = { showTimeSuggestions = !showTimeSuggestions }) {
                                    Icon(
                                        Icons.Default.ExpandMore,
                                        contentDescription = "Show Suggestions"
                                    )
                                }
                            }
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showTimeSuggestions,
                            onDismissRequest = { showTimeSuggestions = false }
                        ) {
                            timeSuggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion) },
                                    onClick = {
                                        whenTime = suggestion
                                        showTimeSuggestions = false
                                        
                                        // Parse and update selected time if it's a specific time
                                        val timePattern = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE)
                                        val match = timePattern.find(suggestion)
                                        if (match != null) {
                                            val hour = match.groupValues[1].toInt()
                                            val minute = match.groupValues[2].takeIf { it.isNotBlank() }?.toInt() ?: 0
                                            val ampm = match.groupValues[3].lowercase()
                                            
                                            val parsedHour = when {
                                                ampm == "am" -> if (hour == 12) 0 else hour
                                                ampm == "pm" -> if (hour == 12) 12 else hour + 12
                                                else -> hour
                                            }
                                            selectedTime = LocalTime.of(parsedHour, minute)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Priority selector with slider
                    Text(
                        text = "Priority Level: ${priorityLabels[selectedPriority]} ($selectedPriority)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Slider(
                        value = selectedPriority.toFloat(),
                        onValueChange = { selectedPriority = it.toInt() },
                        valueRange = 1f..10f,
                        steps = 8, // 10 values - 2 endpoints = 8 steps
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = when (selectedPriority) {
                                in 8..10 -> Red
                                in 6..7 -> Color(0xFFFFA500) // Orange
                                in 4..5 -> Blue
                                else -> Green
                            },
                            activeTrackColor = when (selectedPriority) {
                                in 8..10 -> Red.copy(alpha = 0.5f)
                                in 6..7 -> Color(0xFFFFA500).copy(alpha = 0.5f) // Orange
                                in 4..5 -> Blue.copy(alpha = 0.5f)
                                else -> Green.copy(alpha = 0.5f)
                            }
                        )
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1", style = MaterialTheme.typography.bodySmall)
                        Text("5", style = MaterialTheme.typography.bodySmall)
                        Text("10", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Trigger Configuration Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⏰ Alert Settings",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = { showTriggerConfig = !showTriggerConfig }
                                ) {
                                    Text(if (showTriggerConfig) "Hide" else "Configure")
                                }
                            }
                            
                            if (showTriggerConfig) {
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // At Due Time
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = enableAtDueTime,
                                        onCheckedChange = { enableAtDueTime = it }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "At due time",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                
                                // Minutes Before
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = enableMinutesBefore,
                                        onCheckedChange = { enableMinutesBefore = it }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Minutes before:",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Slider(
                                        value = minutesBeforeValue.toFloat(),
                                        onValueChange = { minutesBeforeValue = it.toInt() },
                                        valueRange = 5f..60f,
                                        steps = 11,
                                        modifier = Modifier.weight(1f),
                                        enabled = enableMinutesBefore
                                    )
                                    Text(
                                        text = "${minutesBeforeValue}m",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(30.dp)
                                    )
                                }
                                
                                // Hours Before
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = enableHoursBefore,
                                        onCheckedChange = { enableHoursBefore = it }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Hours before:",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Slider(
                                        value = hoursBeforeValue.toFloat(),
                                        onValueChange = { hoursBeforeValue = it.toInt() },
                                        valueRange = 1f..24f,
                                        steps = 22,
                                        modifier = Modifier.weight(1f),
                                        enabled = enableHoursBefore
                                    )
                                    Text(
                                        text = "${hoursBeforeValue}h",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(30.dp)
                                    )
                                }
                                
                                // Days Before
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = enableDaysBefore,
                                        onCheckedChange = { enableDaysBefore = it }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Days before:",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Slider(
                                        value = daysBeforeValue.toFloat(),
                                        onValueChange = { daysBeforeValue = it.toInt() },
                                        valueRange = 1f..7f,
                                        steps = 5,
                                        modifier = Modifier.weight(1f),
                                        enabled = enableDaysBefore
                                    )
                                    Text(
                                        text = "${daysBeforeValue}d",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(30.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Single voice input button - ONE CLICK
                    // Enhanced Voice Input Button with Active Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Active indicator - visual sign when voice is active
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isVoiceActive) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isVoiceActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🎤",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isVoiceActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // Voice input button - smaller when inactive
                        Button(
                            onClick = {
                                if (isListening) {
                                    speechManager.stopListening()
                                    isVoiceActive = false
                                } else {
                                    // Try keyboard voice input first (most reliable)
                                    launchKeyboardVoiceInput()
                                    // Fallback to direct mic if keyboard doesn't work
                                    scope.launch {
                                        kotlinx.coroutines.delay(1000)
                                        if (content.isBlank()) {
                                            speechManager.restartSpeechRecognizer()
                                            speechManager.startListening()
                                        }
                                    }
                                    isVoiceActive = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isVoiceActive) 48.dp else 56.dp), // Smaller when inactive
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isListening) MaterialTheme.colorScheme.error else 
                                    if (isVoiceActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        ) {
                        Icon(
                            if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isListening) "Stop Recording" else "Start Voice Input",
                            modifier = Modifier.size(if (isVoiceActive) 16.dp else 20.dp) // Smaller when active
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = speechToText.ifBlank { "Tap to speak" } ?: speechToText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Plus button for adding to content
                            IconButton(
                                onClick = {
                                    val newText = if (speechToText.isBlank()) content else "$content + $speechToText"
                                    speechToText = newText
                                    content = newText
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add to reminder",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                    ) {
                        Icon(
                            if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isListening) "Stop Recording" else "Start Voice Input",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isListening) "⏹️ Stop Recording" else "🎤 Tap to Speak",
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                    
                    if (isListening) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Listening...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Processing indicator
            if (isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("🧠 Processing Your Request...")
                    }
                }
            }
            
            // Simple Analysis (shows extraction)
            if (content.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🧠 Quick Analysis",
                            style = MaterialTheme.typography.titleSmall
                        )
                        
                        Text(
                            text = "📝 Task: ${content.replace(Regex("(remind me to|remember to)"), "").trim()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Text(
                            text = "📁 Category: ${extractCategory(content)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = "📅 Day: ${if (whenDay.isNotBlank()) whenDay else "Not specified"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        
                        Text(
                            text = "⏰ Time: ${if (whenTime.isNotBlank()) whenTime else "Not specified"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        
                        Text(
                            text = "🔥 Priority: ${extractPriority(content)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = when (extractPriority(content)) {
                                "High" -> MaterialTheme.colorScheme.error
                                "Medium" -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        )
                        
                        if (showTriggerConfig) {
                            val triggerSummary = mutableListOf<String>()
                            if (enableAtDueTime) triggerSummary.add("At due time")
                            if (enableMinutesBefore) triggerSummary.add("${minutesBeforeValue}m before")
                            if (enableHoursBefore) triggerSummary.add("${hoursBeforeValue}h before")
                            if (enableDaysBefore) triggerSummary.add("${daysBeforeValue}d before")
                            
                            if (triggerSummary.isNotEmpty()) {
                                Text(
                                    text = "⏰ Alerts: ${triggerSummary.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }
            
            
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = date
                // Only update whenDay if it's currently blank (preserve user input)
                if (whenDay.isBlank()) {
                    whenDay = when {
                        date == LocalDate.now() -> "Today"
                        date == LocalDate.now().plusDays(1) -> "Tomorrow"
                        else -> date.format(DateTimeFormatter.ofPattern("EEEE"))
                    }
                }
            },
            onDismiss = { showDatePicker = false }
        )
    }
    
    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            selectedTime = selectedTime,
            onTimeSelected = { time ->
                selectedTime = time
                // Only update whenTime if it's currently blank (preserve user input)
                if (whenTime.isBlank()) {
                    whenTime = time.format(DateTimeFormatter.ofPattern("h:mm a"))
                }
            },
            onDismiss = { showTimePicker = false }
        )
    }
}