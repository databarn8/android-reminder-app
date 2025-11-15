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
                        android.util.Log.d("FlashTest", "Test flash alarm triggered!")
                        com.reminder.app.utils.NotificationScheduler.testAlarm(context)
                    }) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⚡", style = MaterialTheme.typography.titleLarge)
                            Text("Test", style = MaterialTheme.typography.labelSmall)
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
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                placeholder = { Text("Search...") },
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
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(4.dp)
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
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    IconButton(onClick = onEmail) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = "Email",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Display day and time fields
            if (reminder.whenDay.isNullOrBlank() && reminder.whenTime.isNullOrBlank()) {
                Text(
                    text = "⚠️ No time",
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
            Spacer(modifier = Modifier.height(2.dp))
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