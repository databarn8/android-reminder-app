package com.reminder.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reminder.app.data.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AlertSettingsSection(
    alertConfig: AlertConfig = AlertConfig(),
    repeatPattern: RepeatPattern = RepeatPattern(),
    onAlertConfigChange: (AlertConfig) -> Unit,
    onRepeatPatternChange: (RepeatPattern) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with expand/collapse functionality
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚙️ Alert Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                TextButton(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Text(
                        text = if (isExpanded) "Hide" else "Configure",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            // Animated expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 300, easing = EaseOutQuart)
                ) + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 300, easing = EaseInQuart)
                ) + fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
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
    }
}

@Composable
fun AlertTypeSelector(
    selectedType: AlertType,
    onTypeSelected: (AlertType) -> Unit
) {
    val alertTypes = listOf(
        AlertType.NOTIFICATION_ONLY to "Notification Only",
        AlertType.NOTIFICATION_VIBRATION to "Notification + Vibration",
        AlertType.NOTIFICATION_SOUND to "Notification + Sound",
        AlertType.FULL_ALERT to "Full Alert (All)"
    )
    
    Column {
        Text(
            text = "🔔 Alert Type",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Alert type chips in a grid
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            alertTypes.forEach { (type, description) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedType == type,
                        onClick = { onTypeSelected(type) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = getAlertTypeDescription(type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RepeatConfigurationSection(
    repeatPattern: RepeatPattern,
    onRepeatPatternChange: (RepeatPattern) -> Unit
) {
    Column {
        Text(
            text = "🔄 Repeat",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Repeat type selector
        val repeatTypes = listOf(
            RepeatType.NONE to "None",
            RepeatType.MINUTELY to "Minutely",
            RepeatType.HOURLY to "Hourly",
            RepeatType.DAILY to "Daily",
            RepeatType.WEEKLY to "Weekly",
            RepeatType.MONTHLY to "Monthly",
            RepeatType.YEARLY to "Yearly"
        )
        
        // Repeat type dropdown
        var expanded by remember { mutableStateOf(false) }
        @OptIn(ExperimentalMaterial3Api::class)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = repeatTypes.find { it.first == repeatPattern.type }?.second ?: "None",
                onValueChange = { },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = {
                    @OptIn(ExperimentalMaterial3Api::class)
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select repeat type")
                    }
                }
            )
            
            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                repeatTypes.forEach { (type, description) ->
                    DropdownMenuItem(
                        text = { Text(description) },
                        onClick = {
                            onRepeatPatternChange(repeatPattern.copy(type = type))
                            expanded = false
                        }
                    )
                }
            }
        }
        
        // Interval configuration (only show if not NONE)
        if (repeatPattern.type != RepeatType.NONE) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Every",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                @OptIn(ExperimentalMaterial3Api::class)
                OutlinedTextField(
                    value = repeatPattern.interval.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let {
                            onRepeatPatternChange(repeatPattern.copy(interval = it.coerceAtLeast(1)))
                        }
                    },
                    modifier = Modifier.width(80.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                
                Text(
                    text = getIntervalUnit(repeatPattern.type),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            // End date configuration
            Spacer(modifier = Modifier.height(8.dp))
            
            var showEndDatePicker by remember { mutableStateOf(false) }
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEndDatePicker = true },
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                            text = "📅 End Date (Optional)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = repeatPattern.endDate?.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)) 
                                ?: "No end date",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Select end date",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // End date picker dialog (simplified for now)
            if (showEndDatePicker) {
                AlertDialog(
                    onDismissRequest = { showEndDatePicker = false },
                    title = { Text("Set End Date") },
                    text = {
                        Text("End date picker will be implemented in next phase")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { 
                                showEndDatePicker = false
                                // For now, set end date to 30 days from now
                                onRepeatPatternChange(
                                    repeatPattern.copy(endDate = LocalDate.now().plusDays(30))
                                )
                            }
                        ) {
                            Text("Set 30 Days")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEndDatePicker = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun VibrationConfigurationSection(
    vibrationConfig: VibrationConfig,
    onVibrationConfigChange: (VibrationConfig) -> Unit
) {
    Column {
        Text(
            text = "📳 Vibration",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Enable vibration toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = vibrationConfig.enabled,
                onCheckedChange = { onVibrationConfigChange(vibrationConfig.copy(enabled = it)) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Enable vibration",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        if (vibrationConfig.enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Vibration pattern selector
            Text(
                text = "Pattern",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            val patterns = listOf(
                VibrationPattern.SINGLE to "Single",
                VibrationPattern.DOUBLE to "Double",
                VibrationPattern.TRIPLE to "Triple",
                VibrationPattern.LONG to "Long",
                VibrationPattern.PULSE to "Pulse"
            )
            
            // Pattern selector chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                patterns.forEach { (pattern, name) ->
                    @OptIn(ExperimentalMaterial3Api::class)
                    FilterChip(
                        onClick = { onVibrationConfigChange(vibrationConfig.copy(pattern = pattern)) },
                        label = { Text(name, fontSize = 10.sp) },
                        selected = vibrationConfig.pattern == pattern
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Intensity slider
            Text(
                text = "Intensity: ${vibrationConfig.intensity.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            val intensityValues = listOf(VibrationIntensity.LIGHT, VibrationIntensity.MEDIUM, VibrationIntensity.STRONG)
            Slider(
                value = intensityValues.indexOf(vibrationConfig.intensity).toFloat(),
                onValueChange = { 
                    onVibrationConfigChange(vibrationConfig.copy(intensity = intensityValues[it.toInt()]))
                },
                valueRange = 0f..2f,
                steps = 2,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Series configuration
            Text(
                text = "Series Configuration",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Series count:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                Slider(
                    value = vibrationConfig.seriesCount.toFloat(),
                    onValueChange = { 
                        onVibrationConfigChange(vibrationConfig.copy(seriesCount = it.toInt().coerceIn(1, 5)))
                    },
                    valueRange = 1f..5f,
                    steps = 4,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = "${vibrationConfig.seriesCount}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Interval (ms):",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                Slider(
                    value = vibrationConfig.seriesInterval.toFloat(),
                    onValueChange = { 
                        onVibrationConfigChange(vibrationConfig.copy(seriesInterval = it.toInt().coerceIn(500, 5000)))
                    },
                    valueRange = 500f..5000f,
                    steps = 9,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = "${vibrationConfig.seriesInterval}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(40.dp)
                )
            }
        }
    }
}

@Composable
fun SoundConfigurationSection(
    soundConfig: SoundConfig,
    onSoundConfigChange: (SoundConfig) -> Unit
) {
    Column {
        Text(
            text = "🔊 Sound",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Enable sound toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = soundConfig.enabled,
                onCheckedChange = { onSoundConfigChange(soundConfig.copy(enabled = it)) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Enable sound",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        if (soundConfig.enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Sound type selector
            Text(
                text = "Sound Type",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            val soundTypes = listOf(
                SoundType.DEFAULT to "Default",
                SoundType.ALARM to "Alarm",
                SoundType.GENTLE to "Gentle",
                SoundType.URGENT to "Urgent"
            )
            
            var expanded by remember { mutableStateOf(false) }
            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = soundTypes.find { it.first == soundConfig.type }?.second ?: "Default",
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select sound type")
                        }
                    }
                )
                
                @OptIn(ExperimentalMaterial3Api::class)
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    soundTypes.forEach { (type, description) ->
                        DropdownMenuItem(
                            text = { Text(description) },
                            onClick = {
                                onSoundConfigChange(soundConfig.copy(type = type))
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Volume slider
            Text(
                text = "Volume: ${(soundConfig.volume * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Slider(
                value = soundConfig.volume,
                onValueChange = { onSoundConfigChange(soundConfig.copy(volume = it)) },
                valueRange = 0f..1f,
                steps = 10,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Series configuration
            Text(
                text = "Series Configuration",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Series count:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                Slider(
                    value = soundConfig.seriesCount.toFloat(),
                    onValueChange = { 
                        onSoundConfigChange(soundConfig.copy(seriesCount = it.toInt().coerceIn(1, 3)))
                    },
                    valueRange = 1f..3f,
                    steps = 2,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = "${soundConfig.seriesCount}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Interval (ms):",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                Slider(
                    value = soundConfig.seriesInterval.toFloat(),
                    onValueChange = { 
                        onSoundConfigChange(soundConfig.copy(seriesInterval = it.toInt().coerceIn(1000, 10000)))
                    },
                    valueRange = 1000f..10000f,
                    steps = 9,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = "${soundConfig.seriesInterval}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(40.dp)
                )
            }
        }
    }
}

@Composable
fun AlertSeriesSection(
    alertSeries: AlertSeries,
    onAlertSeriesChange: (AlertSeries) -> Unit
) {
    Column {
        Text(
            text = "🔁 Alert Series",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Enable series toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = alertSeries.enabled,
                onCheckedChange = { onAlertSeriesChange(alertSeries.copy(enabled = it)) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Enable alert series (repeat if not acknowledged)",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        if (alertSeries.enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Max attempts slider
            Text(
                text = "Max attempts: ${alertSeries.maxAttempts}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Slider(
                value = alertSeries.maxAttempts.toFloat(),
                onValueChange = { 
                    onAlertSeriesChange(alertSeries.copy(maxAttempts = it.toInt().coerceIn(1, 10)))
                },
                valueRange = 1f..10f,
                steps = 9,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Interval between attempts
            Text(
                text = "Interval between attempts: ${alertSeries.intervalMinutes} minutes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Slider(
                value = alertSeries.intervalMinutes.toFloat(),
                onValueChange = { 
                    onAlertSeriesChange(alertSeries.copy(intervalMinutes = it.toInt().coerceIn(1, 60)))
                },
                valueRange = 1f..60f,
                steps = 59,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Escalation toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = alertSeries.escalationEnabled,
                    onCheckedChange = { onAlertSeriesChange(alertSeries.copy(escalationEnabled = it)) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Enable escalation (increase intensity over time)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Stop on acknowledge toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = alertSeries.stopOnAcknowledge,
                    onCheckedChange = { onAlertSeriesChange(alertSeries.copy(stopOnAcknowledge = it)) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Stop series when reminder is acknowledged",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// Helper functions
private fun getAlertTypeDescription(type: AlertType): String {
    return when (type) {
        AlertType.NOTIFICATION_ONLY -> "Basic notification only"
        AlertType.NOTIFICATION_VIBRATION -> "Notification with vibration feedback"
        AlertType.NOTIFICATION_SOUND -> "Notification with custom sound"
        AlertType.FULL_ALERT -> "All alert features enabled"
    }
}

private fun getIntervalUnit(type: RepeatType): String {
    return when (type) {
        RepeatType.MINUTELY -> "minute(s)"
        RepeatType.HOURLY -> "hour(s)"
        RepeatType.DAILY -> "day(s)"
        RepeatType.WEEKLY -> "week(s)"
        RepeatType.MONTHLY -> "month(s)"
        RepeatType.YEARLY -> "year(s)"
        else -> ""
    }
}