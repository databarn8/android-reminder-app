package com.reminder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pointerInput
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reminder.app.data.Reminder
import com.reminder.app.ui.calendar.CalendarViewType
import com.reminder.app.viewmodel.ReminderViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: ReminderViewModel,
    onBack: () -> Unit,
    onAddReminder: () -> Unit,
    onReminderClick: (Reminder) -> Unit,
    onAddReminderWithDate: (LocalDate) -> Unit = { onAddReminder() }
) {
    android.util.Log.d("CalendarTest", "Enhanced CalendarScreen called!")
    val reminders by viewModel.reminders.collectAsState()
    var currentDate by remember { mutableStateOf(LocalDate.now()) }
    var currentViewType by remember { mutableStateOf(CalendarViewType.MONTHLY) }
    
    // Helper function to get reminders for a specific date
    fun getRemindersForDate(date: LocalDate): List<Reminder> {
        return reminders.filter { reminder ->
            val reminderDate = java.time.Instant.ofEpochMilli(reminder.reminderTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            reminderDate == date
        }
    }
    
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row {
                        // View toggle buttons
                        FilterChip(
                            onClick = { currentViewType = CalendarViewType.DAILY },
                            label = { Text("Day", fontSize = 11.sp) },
                            selected = currentViewType == CalendarViewType.DAILY,
                            modifier = Modifier.height(32.dp).padding(end = 2.dp)
                        )
                        FilterChip(
                            onClick = { currentViewType = CalendarViewType.WEEKLY },
                            label = { Text("Week", fontSize = 11.sp) },
                            selected = currentViewType == CalendarViewType.WEEKLY,
                            modifier = Modifier.height(32.dp).padding(end = 2.dp)
                        )
                        FilterChip(
                            onClick = { currentViewType = CalendarViewType.MONTHLY },
                            label = { Text("Month", fontSize = 11.sp) },
                            selected = currentViewType == CalendarViewType.MONTHLY,
                            modifier = Modifier.height(32.dp).padding(end = 2.dp)
                        )
                        FilterChip(
                            onClick = { currentViewType = CalendarViewType.YEARLY },
                            label = { Text("Year", fontSize = 11.sp) },
                            selected = currentViewType == CalendarViewType.YEARLY,
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddReminder
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
            }
        }
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Navigation controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    currentDate = when (currentViewType) {
                        CalendarViewType.DAILY -> currentDate.minusDays(1)
                        CalendarViewType.WEEKLY -> currentDate.minusWeeks(1)
                        CalendarViewType.MONTHLY -> currentDate.minusMonths(1)
                        CalendarViewType.YEARLY -> currentDate.minusYears(1)
                    }
                }) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous")
                }

                Text(
                    text = "${reminders.size} reminder${if (reminders.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium
                )

                IconButton(onClick = { 
                    currentDate = when (currentViewType) {
                        CalendarViewType.DAILY -> currentDate.plusDays(1)
                        CalendarViewType.WEEKLY -> currentDate.plusWeeks(1)
                        CalendarViewType.MONTHLY -> currentDate.plusMonths(1)
                        CalendarViewType.YEARLY -> currentDate.plusYears(1)
                    }
                }) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next")
                }
            }

            // Today button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    onClick = { currentDate = LocalDate.now() },
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Event,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                            )
                            Text("Today")
                        }
                    },
                    selected = currentDate == LocalDate.now()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Show different views based on selected view type
            when (currentViewType) {
                CalendarViewType.DAILY -> {
                    DailyView(
                        currentDate = currentDate,
                        reminders = getRemindersForDate(currentDate),
                        onReminderClick = onReminderClick,
                        onDateClick = { date -> currentDate = date }
                    )
                }
                CalendarViewType.WEEKLY -> {
                    WeeklyView(
                        currentDate = currentDate,
                        reminders = reminders,
                        onReminderClick = onReminderClick,
                        onDateClick = { date -> currentDate = date }
                    )
                }
                 CalendarViewType.MONTHLY -> {
                    MonthlyView(
                        currentDate = currentDate,
                        reminders = reminders,
                        onReminderClick = onReminderClick,
                        onDateClick = { date -> 
                            android.util.Log.d("CalendarTest", "CalendarScreen MonthlyView onDateClick: $date")
                            currentDate = date
                            val dayReminders = getRemindersForDate(date)
                            android.util.Log.d("CalendarTest", "Day has ${dayReminders.size} reminders")
                            if (dayReminders.isEmpty()) {
                                android.util.Log.d("CalendarTest", "No reminders - calling onAddReminderWithDate")
                                onAddReminderWithDate(date)
                            } else {
                                android.util.Log.d("CalendarTest", "Has reminders - showing first one")
                                onReminderClick(dayReminders.first())
                            }
                        }
                    )
                }
                 CalendarViewType.YEARLY -> {
                    YearlyView(
                        currentDate = currentDate,
                        reminders = reminders,
                        onReminderClick = onReminderClick,
                        onDateClick = { date -> 
                            android.util.Log.d("CalendarTest", "CalendarScreen YearlyView onDateClick: $date - switching to monthly view")
                            currentDate = date
                            currentViewType = CalendarViewType.MONTHLY
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DailyView(
    currentDate: LocalDate,
    reminders: List<Reminder>,
    onReminderClick: (Reminder) -> Unit,
    onDateClick: (LocalDate) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = currentDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "${reminders.size} reminder${if (reminders.size != 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (reminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.EventBusy,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No reminders for this day",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(reminders.size) { index ->
                        val reminder = reminders[index]
                        ReminderItem(
                            reminder = reminder,
                            onClick = { onReminderClick(reminder) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyView(
    currentDate: LocalDate,
    reminders: List<Reminder>,
    onReminderClick: (Reminder) -> Unit,
    onDateClick: (LocalDate) -> Unit
) {
    val weekStart = currentDate.with(java.time.DayOfWeek.MONDAY)
    val dayHeaders = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    
    // Helper function to get reminders for a specific date
    fun getRemindersForDate(date: LocalDate): List<Reminder> {
        return reminders.filter { reminder ->
            val reminderDate = java.time.Instant.ofEpochMilli(reminder.reminderTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            reminderDate == date
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Day headers
                items(dayHeaders) { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                
                // Week days
                items((0..6).toList()) { dayOffset ->
                    val date = weekStart.plusDays(dayOffset.toLong())
                    val dayReminders = getRemindersForDate(date)
                    val isToday = date == LocalDate.now()
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.8f)
                            .clickable(
                                onClick = { 
                                    onDateClick(date)
                                    android.util.Log.d("CalendarTest", "Week view clicked: $date with ${dayReminders.size} reminders")
                                },
                                enabled = true,
                                onClickLabel = "Select date $date"
                            )
                            .pointerInput(date) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        android.util.Log.d("CalendarTest", "Week view TAP DETECTED at offset: $offset for date: $date")
                                        onDateClick(date)
                                    }
                                )
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isToday -> MaterialTheme.colorScheme.primaryContainer
                                dayReminders.isNotEmpty() -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isToday) 4.dp else 1.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            )
                            
                            if (dayReminders.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            CircleShape
                                        )
                                )
                                Text(
                                    text = "${dayReminders.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Selected day details
            val selectedDayReminders = getRemindersForDate(currentDate)
            Text(
                text = "Reminders for ${currentDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (selectedDayReminders.isEmpty()) {
                Text(
                    text = "No reminders selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(selectedDayReminders.size) { index ->
                        val reminder = selectedDayReminders[index]
                        ReminderItem(
                            reminder = reminder,
                            onClick = { onReminderClick(reminder) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyView(
    currentDate: LocalDate,
    reminders: List<Reminder>,
    onReminderClick: (Reminder) -> Unit,
    onDateClick: (LocalDate) -> Unit
) {
    // Helper function to get reminders for a specific date
    fun getRemindersForDate(date: LocalDate): List<Reminder> {
        return reminders.filter { reminder ->
            val reminderDate = java.time.Instant.ofEpochMilli(reminder.reminderTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            reminderDate == date
        }
    }
    
    val yearMonth = YearMonth.from(currentDate)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = currentDate.withDayOfMonth(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    val calendarDays = (0 until firstDayOfWeek).map { null } + (1..daysInMonth).toList()
    val dayHeaders = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Day headers
                items(dayHeaders) { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                
                // Calendar days
                items(calendarDays) { day ->
                    if (day != null) {
                        val date = currentDate.withDayOfMonth(day)
                        val dayReminders = getRemindersForDate(date)
                        val isToday = date == LocalDate.now()
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                    dayReminders.isNotEmpty() -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isToday) 4.dp else 1.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(
                                        onClick = { 
                                            android.util.Log.d("CalendarTest", "BOX CLICKED: $date with ${dayReminders.size} reminders")
                                            android.util.Log.d("CalendarTest", "Click timestamp: ${System.currentTimeMillis()}")
                                            onDateClick(date)
                                            android.util.Log.d("CalendarTest", "onDateClick completed")
                                        },
                                        enabled = true,
                                        onClickLabel = "Select date $date"
                                    )
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = { offset ->
                                                android.util.Log.d("CalendarTest", "TAP DETECTED at offset: $offset for date: $date")
                                                onDateClick(date)
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                    
                                    if (dayReminders.isNotEmpty()) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            repeat(minOf(3, dayReminders.size)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.primary,
                                                            CircleShape
                                                        )
                                                )
                                            }
                                        }
                                        if (dayReminders.size > 3) {
                                            Text(
                                                text = "+${dayReminders.size - 3}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 6.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        
                                        // Show first reminder preview if there's space
                                        if (dayReminders.size == 1) {
                                            Text(
                                                text = dayReminders.first().content.take(8) + if (dayReminders.first().content.length > 8) "..." else "",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty cell for alignment
                        Spacer(modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun YearlyView(
    currentDate: LocalDate,
    reminders: List<Reminder>,
    onReminderClick: (Reminder) -> Unit,
    onDateClick: (LocalDate) -> Unit
) {
    val year = currentDate.year
    val months = (1..12).toList()
    val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(months) { month ->
                    val monthReminders = reminders.filter { reminder ->
                        val reminderDate = java.time.Instant.ofEpochMilli(reminder.reminderTime)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        reminderDate.year == year && reminderDate.monthValue == month
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f)
                            .clickable(
                                onClick = { 
                                    android.util.Log.d("CalendarTest", "Year view CLICKED: month $month with ${monthReminders.size} reminders")
                                    val newDate = currentDate.withMonth(month).withDayOfMonth(1)
                                    android.util.Log.d("CalendarTest", "Year view calling onDateClick with: $newDate")
                                    onDateClick(newDate)
                                    android.util.Log.d("CalendarTest", "Year view onDateClick returned")
                                },
                                enabled = true,
                                onClickLabel = "Select month $month"
                            )
                            .pointerInput(month) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        android.util.Log.d("CalendarTest", "Year view TAP DETECTED at offset: $offset for month: $month")
                                        val newDate = currentDate.withMonth(month).withDayOfMonth(1)
                                        onDateClick(newDate)
                                    }
                                )
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = monthNames[month - 1],
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            if (monthReminders.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${monthReminders.size}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                
                                // Show first reminder preview
                                monthReminders.firstOrNull()?.let { reminder ->
                                    Text(
                                        text = reminder.content.take(12) + if (reminder.content.length > 12) "..." else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                if (monthReminders.size > 1) {
                                    Text(
                                        text = "+${monthReminders.size - 1} more",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "0",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderItem(
    reminder: Reminder,
    onClick: () -> Unit
) {
    val reminderTime = java.time.Instant.ofEpochMilli(reminder.reminderTime)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = reminder.content,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = reminderTime.format(DateTimeFormatter.ofPattern("MMM d, h:mm a")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Priority indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        when (reminder.importance) {
                            in 8..10 -> Color.Red
                            in 6..7 -> Color(0xFFFFA500)
                            in 4..5 -> Color.Blue
                            else -> Color.Green
                        },
                        CircleShape
                    )
            )
        }
    }
}