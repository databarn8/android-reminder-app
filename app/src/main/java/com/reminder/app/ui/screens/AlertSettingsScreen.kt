 package com.reminder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reminder.app.ui.components.*
import com.reminder.app.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertSettingsScreen(
    alertConfig: AlertConfig = AlertConfig(),
    repeatPattern: RepeatPattern = RepeatPattern(),
    onAlertConfigChange: (AlertConfig) -> Unit,
    onRepeatPatternChange: (RepeatPattern) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alert Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
            // Alert Type Selection
            AlertTypeSelector(
                selectedType = alertConfig.alertType,
                onTypeSelected = { onAlertConfigChange(alertConfig.copy(alertType = it)) }
            )
            
            // Repeat Configuration
            RepeatConfigurationSection(
                repeatPattern = repeatPattern,
                onRepeatPatternChange = onRepeatPatternChange
            )
            
            // Vibration Configuration
            VibrationConfigurationSection(
                vibrationConfig = alertConfig.vibration,
                onVibrationConfigChange = { onAlertConfigChange(alertConfig.copy(vibration = it)) }
            )
            
            // Sound Configuration
            SoundConfigurationSection(
                soundConfig = alertConfig.sound,
                onSoundConfigChange = { onAlertConfigChange(alertConfig.copy(sound = it)) }
            )
            
            // Alert Series Configuration
            AlertSeriesSection(
                alertSeries = alertConfig.series,
                onAlertSeriesChange = { onAlertConfigChange(alertConfig.copy(series = it)) }
            )
        }
    }
}