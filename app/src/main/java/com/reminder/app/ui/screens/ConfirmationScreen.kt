package com.reminder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reminder.app.utils.SpeechManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmationScreen(
    initialText: String,
    speechManager: SpeechManager,
    onConfirm: (String) -> Unit,
    onBack: () -> Unit,
    onCalendarClick: () -> Unit = {}
) {
    var confirmedText by remember { mutableStateOf(initialText) }
    var isEditing by remember { mutableStateOf(false) }
    val isListening by speechManager.isListening.collectAsState()
    val speechResult by speechManager.speechResult.collectAsState()

    LaunchedEffect(speechResult) {
        speechResult?.let { result ->
            confirmedText = result
            speechManager.clearSpeechResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirm Reminder") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onCalendarClick) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar")
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
            Text(
                text = "Please confirm your reminder text:",
                style = MaterialTheme.typography.titleMedium
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (isEditing) {
                        OutlinedTextField(
                            value = confirmedText,
                            onValueChange = { confirmedText = it },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 6
                        )
                    } else {
                        Text(
                            text = confirmedText,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { isEditing = !isEditing },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEditing) "Done" else "Edit")
                }

                OutlinedButton(
                    onClick = {
                        if (isListening) {
                            speechManager.stopListening()
                        } else {
                            speechManager.startListening()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isListening) Icons.Default.Mic else Icons.Default.Mic,
                        contentDescription = if (isListening) "Stop Recording" else "Start Recording",
                        tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isListening) "Stop" else "Re-record")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onConfirm(confirmedText)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm")
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}