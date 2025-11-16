package com.reminder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.reminder.app.data.Reminder
import com.reminder.app.utils.EmailService
import com.reminder.app.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.*
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import android.view.WindowManager
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures

// Fresh Button Implementation - Simple flash with vibration and system beep
fun performSafeFreshFlash(context: Context) {
    try {
        android.util.Log.d("FreshButton", "Starting simple flash implementation")
        
        // Use ScreenFlashManager with simplified approach
        com.reminder.app.utils.ScreenFlashManager.triggerFlash(
            context = context,
            flashColor = androidx.compose.ui.graphics.Color.Yellow,
            flashDurationMs = 300, // Short flash duration
            flashCount = 1, // Single flash for simplicity
            intervalMs = 200 // Short interval
        )
        
        android.util.Log.d("FreshButton", "Simple flash completed successfully")
        
    } catch (e: Exception) {
        android.util.Log.e("FreshButton", "Simple flash failed: ${e.message}")
        // Fallback to vibration only if flash fails
        try {
            com.reminder.app.utils.ScreenFlashManager.triggerVibration(context)
        } catch (e2: Exception) {
            android.util.Log.e("FreshButton", "Fallback vibration also failed: ${e2.message}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    viewModel: ReminderViewModel,
    onAddReminder: () -> Unit,
    onReminderClick: (Reminder) -> Unit,
    onEditClick: (Reminder) -> Unit,
    onCalendarClick: () -> Unit = {},
    onEmailClick: (Reminder) -> Unit = {}
) {
    val context = LocalContext.current
    val reminders by viewModel.reminders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter reminders based on search query
    val filteredReminders = remember(searchQuery, reminders) {
        if (searchQuery.isBlank()) {
            reminders
        } else {
            reminders.filter { reminder ->
                reminder.content.contains(searchQuery, ignoreCase = true) ||
                reminder.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Reminders")
                        if (searchQuery.isNotBlank()) {
                            Text(
                                text = "${filteredReminders.size} found",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        android.util.Log.d("CalendarTest", "Calendar button clicked in ReminderListScreen!")
                        onCalendarClick() 
                    }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar")
                    }
                    IconButton(onClick = {
                        android.util.Log.d("FreshButton", "Fresh button triggered - safe flash implementation")
                        performSafeFreshFlash(context)
                    }) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("✨", style = MaterialTheme.typography.titleLarge)
                            Text("Fresh", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(onClick = { /* Search toggle */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
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
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search reminders...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { /* Handle search action */ }
                ),
                singleLine = true
            )
            
            // Reminders list
            Box(modifier = Modifier.weight(1f)) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            errorMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (filteredReminders.isEmpty() && !isLoading) {
                if (searchQuery.isBlank()) {
                    Text(
                        text = "No reminders yet. Tap + to add one!",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Text(
                        text = "No reminders found for \"$searchQuery\"",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredReminders) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onClick = { onReminderClick(reminder) },
                            onDelete = { viewModel.deleteReminder(reminder) },
                            onEdit = { onEditClick(reminder) },
                            onEmail = { onEmailClick(reminder) }
                        )
                    }
            }
            }
        }
    }
    }
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onEmail: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reminder.content,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(onClick = onEmail) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = "Email",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Display day and time fields prominently
            if (reminder.whenDay?.isNotBlank() == true || reminder.whenTime?.isNotBlank() == true) {
                reminder.whenDay?.let { day ->
                    if (day.isNotBlank()) {
                        Text(
                            text = "📅 $day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                reminder.whenTime?.let { time ->
                    if (time.isNotBlank()) {
                        Text(
                            text = "⏰ $time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Text(
                text = reminder.category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            if (reminder.content.length > 50) {
                Text(
                    text = reminder.content.take(50) + "...",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Priority: ${reminder.importance}/10",
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        reminder.importance >= 8 -> MaterialTheme.colorScheme.error
                        reminder.importance >= 5 -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                if (reminder.whenDay.isNullOrBlank() && reminder.whenTime.isNullOrBlank()) {
                    Text(
                        text = "⚠️ Day/Time not specified",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = dateFormat.format(Date(reminder.reminderTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
        }
    }
}