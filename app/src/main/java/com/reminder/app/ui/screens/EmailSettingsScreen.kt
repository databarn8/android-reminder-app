package com.reminder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.reminder.app.ui.components.EmailClientSelector
import com.reminder.app.ui.components.EmailClientInfo
import com.reminder.app.utils.EnhancedEmailService

/**
 * Email Settings Screen
 * 
 * This screen allows users to manage their email client preferences
 * and other email-related settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailSettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val emailService = remember { EnhancedEmailService() }
    
    var showEmailClientSelector by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Email Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Email Client Selection Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Email Client Preference",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Choose your preferred email client for sending reminders. This will be used as the default option when you share reminders via email.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    EmailClientSelector(
                        onEmailClientChanged = { emailClient ->
                            // Show confirmation that preference was saved
                            android.util.Log.d("EmailSettings", "Email client preference changed to: ${emailClient.appName}")
                        }
                    )
                }
            }
            
            // Current Email Client Info
            EmailClientInfo(
                showChangeButton = false,
                onChangeClick = { showEmailClientSelector = true }
            )
            
            // Email Settings Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "How Email Preferences Work",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val infoPoints = listOf(
                        "Your preferred email client will be used automatically when sending reminders",
                        "You can always choose a different email client when sending",
                        "If your preferred email client is uninstalled, the chooser will appear",
                        "Email preferences are saved locally on your device"
                    )
                    
                    infoPoints.forEach { point ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            // Test Email Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Test Email Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Send a test email to verify your email client preferences are working correctly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            // Create a test reminder and send it
                            val testReminder = com.reminder.app.data.Reminder(
                                id = 0,
                                content = "This is a test reminder from your Reminder App",
                                category = "Test",
                                importance = 3,
                                reminderTime = System.currentTimeMillis(),
                                whenDay = null,
                                whenTime = null,
                                repeatType = "none",
                                repeatInterval = 1,
                                createdAt = System.currentTimeMillis()
                            )
                            
                            try {
                                emailService.sendReminderEmail(context, testReminder)
                            } catch (e: Exception) {
                                // Handle error - could show a toast or snackbar
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send Test Email")
                    }
                }
            }
            
            // Advanced Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Advanced Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Always Show Email Chooser",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Ignore preferences and always show all email options",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Switch(
                            checked = false, // This could be a preference setting
                            onCheckedChange = { /* Handle switch change */ }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            emailService.clearEmailPreference(context)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear Email Preference")
                    }
                }
            }
        }
    }
}