package com.reminder.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.reminder.app.data.EmailClientPreference
import com.reminder.app.data.EmailPreferencesManager
import com.reminder.app.utils.EnhancedEmailService

/**
 * Email Client Selector Component
 * 
 * This component provides a UI for selecting and managing email client preferences.
 * It shows the current preferred email client and allows users to change it.
 */
@Composable
fun EmailClientSelector(
    modifier: Modifier = Modifier,
    onEmailClientChanged: (EmailClientPreference) -> Unit = {}
) {
    val context = LocalContext.current
    val emailService = remember { EnhancedEmailService() }
    val emailPreferencesManager = remember { EmailPreferencesManager(context) }
    
    var showSelectorDialog by remember { mutableStateOf(false) }
    var availableEmailClients by remember { mutableStateOf<List<EmailClientPreference>>(emptyList()) }
    var currentPreferredClient by remember { mutableStateOf(emailPreferencesManager.getPreferredEmailClient()) }
    
    // Load available email clients when dialog is shown
    LaunchedEffect(showSelectorDialog) {
        if (showSelectorDialog) {
            availableEmailClients = emailService.getAvailableEmailClients(context)
        }
    }
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Current email client display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSelectorDialog = true },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Email Client",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (currentPreferredClient.packageName.isNotEmpty()) {
                            currentPreferredClient.appName
                        } else {
                            "No preferred email client"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Text(
                    text = "Change",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { showSelectorDialog = true }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Info text
        if (currentPreferredClient.packageName.isNotEmpty()) {
            Text(
                text = "Emails will be sent using ${currentPreferredClient.appName} by default. Tap to change.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "No email client preference set. Tap to select your preferred email client.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    // Email client selection dialog
    if (showSelectorDialog) {
        Dialog(onDismissRequest = { showSelectorDialog = false }) {
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
                        text = "Select Email Client",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (availableEmailClients.isEmpty()) {
                        Text(
                            text = "No email clients found on your device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableEmailClients) { emailClient ->
                                EmailClientItem(
                                    emailClient = emailClient,
                                    isSelected = emailClient.packageName == currentPreferredClient.packageName,
                                    onSelected = {
                                        emailPreferencesManager.savePreferredEmailClient(
                                            emailClient.packageName,
                                            emailClient.appName
                                        )
                                        currentPreferredClient = emailClient
                                        onEmailClientChanged(emailClient)
                                        showSelectorDialog = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Clear preference button
                        if (currentPreferredClient.packageName.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    emailPreferencesManager.clearPreferredEmailClient()
                                    currentPreferredClient = EmailClientPreference()
                                    onEmailClientChanged(EmailClientPreference())
                                    showSelectorDialog = false
                                }
                            ) {
                                Text("Clear Preference")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                        
                        // Cancel button
                        TextButton(
                            onClick = { showSelectorDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual email client item for the selection list
 */
@Composable
private fun EmailClientItem(
    emailClient: EmailClientPreference,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = emailClient.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                Text(
                    text = emailClient.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Simple email client info display for settings screens
 */
@Composable
fun EmailClientInfo(
    modifier: Modifier = Modifier,
    showChangeButton: Boolean = true,
    onChangeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val emailService = remember { EnhancedEmailService() }
    val emailPreferencesManager = remember { EmailPreferencesManager(context) }
    
    val currentPreferredClient by remember {
        derivedStateOf { emailPreferencesManager.getPreferredEmailClient() }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Current Email Client",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (currentPreferredClient.packageName.isNotEmpty()) {
                Text(
                    text = currentPreferredClient.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "Package: ${currentPreferredClient.packageName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (currentPreferredClient.lastUsedTime > 0) {
                    Text(
                        text = "Last used: ${java.text.SimpleDateFormat(
                            "MMM dd, yyyy HH:mm",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date(currentPreferredClient.lastUsedTime))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "No preferred email client set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (showChangeButton) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onChangeClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change Email Client")
                }
            }
        }
    }
}