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
import com.reminder.app.data.Reminder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Enhanced extraction functions for common reminder patterns

// Helper function to format time for content display
fun formatTimeForContent(time: LocalTime): String {
    return when {
        time.hour == 0 && time.minute == 0 -> "midnight"
        time.hour == 12 && time.minute == 0 -> "noon"
        time.minute == 0 -> "${time.hour} o'clock"
        time.minute < 10 -> "${time.hour}:0${time.minute}"
        else -> "${time.hour}:${time.minute}"
    }
}

// Helper function to format date for content display
fun formatDateForContent(date: LocalDate): String {
    val today = LocalDate.now()
    return when {
        date.isEqual(today) -> "today"
        date.isEqual(today.plusDays(1)) -> "tomorrow"
        date.isEqual(today.minusDays(1)) -> "yesterday"
        date.year == today.year -> date.format(DateTimeFormatter.ofPattern("MMM d"))
        else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
}

// Enhanced day extraction with more patterns
fun extractDay(content: String): String {
    val lowerContent = content.lowercase()
    
    // Direct day mentions
    val directDays = mapOf(
        "today" to "Today",
        "tomorrow" to "Tomorrow", 
        "yesterday" to "Yesterday",
        "monday" to "Monday",
        "tuesday" to "Tuesday",
        "wednesday" to "Wednesday",
        "thursday" to "Thursday",
        "friday" to "Friday",
        "saturday" to "Saturday",
        "sunday" to "Sunday"
    )
    
    for ((pattern, day) in directDays) {
        if (pattern in lowerContent) return day
    }
    
    // Relative day patterns
    when {
        "next week" in lowerContent -> {
            val today = LocalDate.now()
            val nextWeek = today.plusDays(7)
            return nextWeek.format(DateTimeFormatter.ofPattern("EEEE"))
        }
        "next monday" in lowerContent -> "Monday"
        "next tuesday" in lowerContent -> "Tuesday"
        "next wednesday" in lowerContent -> "Wednesday"
        "next thursday" in lowerContent -> "Thursday"
        "next friday" in lowerContent -> "Friday"
        "next saturday" in lowerContent -> "Saturday"
        "next sunday" in lowerContent -> "Sunday"
    }
    
    return ""
}

// Enhanced time extraction with more patterns
fun extractTimeOnly(content: String): String {
    val lowerContent = content.lowercase()
    
    // Direct time patterns
    val timeRegex = Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm|a\.m\.|p\.m\.)?""")
    val matches = timeRegex.findAll(lowerContent)
    
    for (match in matches) {
        val hour = match.groupValues[1].toIntOrNull()
        val minute = match.groupValues[2].toIntOrNull() ?: 0
        val period = match.groupValues[3].lowercase()
        
        if (hour != null && hour in 0..23) {
            val displayHour = when {
                period.contains("p") && hour < 12 -> hour + 12
                period.contains("a") && hour == 12 -> 0
                else -> hour
            }
            
            val time = LocalTime.of(displayHour % 24, minute)
            return time.format(DateTimeFormatter.ofPattern("h:mm a"))
        }
    }
    
    // Common time expressions
    when {
        "noon" in lowerContent -> "12:00 PM"
        "midnight" in lowerContent -> "12:00 AM"
        "morning" in lowerContent -> "9:00 AM"
        "afternoon" in lowerContent -> "2:00 PM"
        "evening" in lowerContent -> "6:00 PM"
        "night" in lowerContent -> "8:00 PM"
    }
    
    return ""
}

// Quick date suggestions
val quickDates = listOf("Today", "Tomorrow", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

// Quick time suggestions
val quickTimes = listOf("9:00 AM", "12:00 PM", "3:00 PM", "6:00 PM", "8:00 PM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Date",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Simple date picker using Column and Row
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.weight(1f)
                ) {
                    // Day headers
                    items(listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")) { day ->
                        Text(
                            text = day,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Date cells (simplified)
                    val daysInMonth = 31
                    items(daysInMonth) { day ->
                        val date = LocalDate.of(selectedDate.year, selectedDate.monthValue, day + 1)
                        val isSelected = date == selectedDate
                        
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .size(40.dp)
                                .clickable { 
                                    selectedDate = date
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (day + 1).toString(),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onDateSelected(selectedDate)
                            onDismiss()
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableStateOf(selectedTime.hour) }
    var minute by remember { mutableStateOf(selectedTime.minute) }
    
    android.util.Log.d("TimePickerDialog", "Dialog opened with selectedTime=$selectedTime, hour=$hour, minute=$minute")
    
    fun formatTime(h: Int, m: Int): String {
        val displayHour = if (h == 0) 12 else if (h > 12) h - 12 else h
        val period = if (h < 12) "AM" else "PM"
        return "$displayHour:${String.format("%02d", m)} $period"
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Time",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = formatTime(hour, minute),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Hour picker
                Text("Hour", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (h in 0..23) {
                        FilterChip(
                            onClick = { hour = h },
                            label = { Text(h.toString()) },
                            selected = hour == h
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Minute picker
                Text("Minute", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (m in listOf(0, 15, 30, 45)) {
                        FilterChip(
                            onClick = { minute = m },
                            label = { Text(String.format("%02d", m)) },
                            selected = minute == m
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val time = LocalTime.of(hour, minute)
                            android.util.Log.d("TimePickerDialog", "Time selected: $time")
                            onTimeSelected(time)
                            onDismiss()
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

// Helper function to parse day string to date
fun parseDayToDate(dayStr: String): LocalDate {
    val today = LocalDate.now()
    return when (dayStr.lowercase()) {
        "today" -> today
        "tomorrow" -> today.plusDays(1)
        "yesterday" -> today.minusDays(1)
        "monday" -> getNextWeekday(today, java.time.DayOfWeek.MONDAY)
        "tuesday" -> getNextWeekday(today, java.time.DayOfWeek.TUESDAY)
        "wednesday" -> getNextWeekday(today, java.time.DayOfWeek.WEDNESDAY)
        "thursday" -> getNextWeekday(today, java.time.DayOfWeek.THURSDAY)
        "friday" -> getNextWeekday(today, java.time.DayOfWeek.FRIDAY)
        "saturday" -> getNextWeekday(today, java.time.DayOfWeek.SATURDAY)
        "sunday" -> getNextWeekday(today, java.time.DayOfWeek.SUNDAY)
        else -> today
    }
}

// Helper function to get next occurrence of a weekday
fun getNextWeekday(fromDate: LocalDate, targetDay: java.time.DayOfWeek): LocalDate {
    var current = fromDate
    while (current.dayOfWeek != targetDay) {
        current = current.plusDays(1)
    }
    return current
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(
    viewModel: ReminderViewModel,
    onBack: () -> Unit,
    reminderId: Int? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State variables
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Personal") }
    var importance by remember { mutableStateOf(5) }
    var whenDay by remember { mutableStateOf("") }
    var whenTime by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    
    // Speech recognition
    val speechManager = remember { SpeechManager(context) }
    val smartVoiceProcessor = remember { SmartVoiceProcessor() }
    
    // Load reminder data if editing
    LaunchedEffect(reminderId) {
        if (reminderId != null) {
            android.util.Log.d("InputScreen", "Loading reminder with ID: $reminderId")
            // Add delay to ensure database operations complete
            delay(100)
            
            viewModel.getReminderById(reminderId)?.let { reminder ->
                android.util.Log.d("InputScreen", "Loaded reminder: $reminder")
                content = reminder.content
                category = reminder.category
                importance = reminder.importance
                whenDay = reminder.whenDay ?: ""
                whenTime = reminder.whenTime ?: ""
                
                // Parse and set selectedDate and selectedTime from whenDay and whenTime
                try {
                    if (!reminder.whenDay.isNullOrBlank()) {
                        selectedDate = parseDayToDate(reminder.whenDay)
                    }
                    if (!reminder.whenTime.isNullOrBlank()) {
                        selectedTime = LocalTime.parse(reminder.whenTime!!, DateTimeFormatter.ofPattern("h:mm a"))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("InputScreen", "Error parsing datetime: ${e.message}")
                }
            }
        }
    }
    
    // Only auto-process content for NEW reminders, not when editing
    LaunchedEffect(content, reminderId) {
        if (content.isNotBlank() && !isProcessing && reminderId == null) {
            isProcessing = true
            scope.launch {
                try {
                    val processed = smartVoiceProcessor.processVoiceInput(content)
                    
                    val extractedDay = extractDay(content)
                    val extractedTime = extractTimeOnly(content)
                    
                    // Only auto-fill for NEW reminders, not when editing existing ones
                    whenDay = extractedDay
                    whenTime = extractedTime
                    
                    android.util.Log.d("InputScreen", "Auto-filled - Day: $extractedDay, Time: $extractedTime")
                    
                } catch (e: Exception) {
                    android.util.Log.e("InputScreen", "Error processing content: ${e.message}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }
    
    // Save reminder
    fun saveReminder() {
        if (content.isBlank()) {
            android.util.Log.w("InputScreen", "Cannot save reminder: content is blank")
            return
        }
        
        scope.launch {
            try {
                // Update whenDay and whenTime from selectedDate and selectedTime
                val updatedWhenDay = if (whenDay.isBlank()) formatDateForContent(selectedDate) else whenDay
                val updatedWhenTime = if (whenTime.isBlank()) formatTimeForContent(selectedTime) else whenTime
                
                android.util.Log.d("InputScreen", "Saving reminder with whenDay: $updatedWhenDay, whenTime: $updatedWhenTime")
                
                // Calculate reminder time from selected date and time
                val reminderDateTime = selectedDate.atTime(selectedTime)
                val reminderTimeMillis = reminderDateTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000
                
                if (reminderId != null) {
                    val updatedReminder = Reminder(
                        id = reminderId,
                        content = content,
                        category = category,
                        importance = importance,
                        reminderTime = reminderTimeMillis,
                        whenDay = updatedWhenDay,
                        whenTime = updatedWhenTime
                    )
                    viewModel.updateReminder(updatedReminder)
                    android.util.Log.d("InputScreen", "Updated reminder $reminderId")
                } else {
                    val newReminder = Reminder(
                        content = content,
                        category = category,
                        importance = importance,
                        reminderTime = reminderTimeMillis,
                        whenDay = updatedWhenDay,
                        whenTime = updatedWhenTime
                    )
                    viewModel.addReminder(newReminder)
                    android.util.Log.d("InputScreen", "Added new reminder")
                }
                
                // Add delay before navigation to ensure update completion
                delay(200)
                onBack()
                
            } catch (e: Exception) {
                android.util.Log.e("InputScreen", "Error saving reminder: ${e.message}")
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (reminderId != null) "Edit Reminder" else "New Reminder") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { saveReminder() }) {
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
            // Content input
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Reminder") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (isRecording) {
                                speechManager.stopListening()
                                isRecording = false
                            } else {
                                speechManager.startListening()
                                isRecording = true
                            }
                        }
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                            tint = if (isRecording) Red else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
            
            // Category selection
            Text("Category", style = MaterialTheme.typography.labelLarge)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(80.dp)
            ) {
                items(listOf("Work", "Family", "Personal")) { cat ->
                    FilterChip(
                        onClick = { category = cat },
                        label = { Text(cat) },
                        selected = category == cat
                    )
                }
            }
            
            // Importance slider
            Text("Importance: $importance", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = importance.toFloat(),
                onValueChange = { importance = it.toInt() },
                valueRange = 1f..10f,
                steps = 8,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Date selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = whenDay,
                    onValueChange = { whenDay = it },
                    label = { Text("Date") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                        }
                    }
                )
            }
            
            // Quick date suggestions
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.height(60.dp)
            ) {
                items(quickDates) { quickDate ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                whenDay = quickDate
                                selectedDate = parseDayToDate(quickDate)
                            },
                        shape = MaterialTheme.shapes.small,
                        color = if (whenDay == quickDate) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.padding(end = 4.dp))
                            Text(
                                text = quickDate,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
            
            // Time selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = whenTime,
                    onValueChange = { whenTime = it },
                    label = { Text("Time") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showTimePicker = true }) {
                            Icon(Icons.Default.AccessTime, contentDescription = "Select Time")
                        }
                    }
                )
            }
            
            // Quick time suggestions
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.height(60.dp)
            ) {
                items(quickTimes) { quickTime ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                whenTime = quickTime
                                selectedTime = LocalTime.parse(quickTime, DateTimeFormatter.ofPattern("h:mm a"))
                            },
                        shape = MaterialTheme.shapes.small,
                        color = if (whenTime == quickTime) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.padding(end = 4.dp))
                            Text(
                                text = quickTime,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
        
        // Date Picker Dialog
        if (showDatePicker) {
            DatePickerDialog(
                initialDate = selectedDate,
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
}