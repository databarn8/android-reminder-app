package com.reminder.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.reminder.app.data.ReminderDatabase
import com.reminder.app.repository.ReminderRepository
import com.reminder.app.ui.screens.CalendarScreen
import com.reminder.app.ui.screens.ConfirmationScreen
import com.reminder.app.ui.screens.InputScreen
import com.reminder.app.ui.screens.ReminderListScreen
import com.reminder.app.ui.theme.ReminderAppTheme
import com.reminder.app.utils.EnhancedEmailService
import com.reminder.app.utils.NotificationScheduler
import com.reminder.app.utils.ScreenFlashManager
import com.reminder.app.utils.ScreenFlashOverlay
import com.reminder.app.utils.SpeechManager
import com.reminder.app.viewmodel.ReminderViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    private lateinit var speechManager: SpeechManager
    private lateinit var emailService: EnhancedEmailService
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        speechManager.onPermissionResult(isGranted)
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle notification permission result
        if (isGranted) {
            android.util.Log.d("MainActivity", "Notification permission GRANTED")
            // Test notification system immediately
            NotificationScheduler.testAlarm(this)
        } else {
            android.util.Log.w("MainActivity", "Notification permission DENIED - reminders may not work properly")
            // Show toast to user about importance of notifications
            android.widget.Toast.makeText(this, "Notifications are required for reminders to work properly", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    private val alarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle alarm permission result
        if (!isGranted) {
            android.util.Log.d("MainActivity", "Exact alarm permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        speechManager = SpeechManager(this)
        speechManager.setActivity(this)
        emailService = EnhancedEmailService()
        
        // Check and request necessary permissions
        if (!speechManager.hasAudioPermission()) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        
        // Request notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (!checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                android.util.Log.d("MainActivity", "Requesting POST_NOTIFICATIONS permission")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                android.util.Log.d("MainActivity", "POST_NOTIFICATIONS permission already granted")
            }
        }
        
        // Request exact alarm permission for Android 12+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            alarmPermissionLauncher.launch(Manifest.permission.SCHEDULE_EXACT_ALARM)
        }
        
        // Handle voice actions from Google Assistant and keyboard input
        handleIntent(intent)
        
        setContent {
            ReminderAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Screen flash overlay for reminder notifications
                    ScreenFlashOverlay()
                    val navController = rememberNavController()
                    val database = ReminderDatabase.getDatabase(this)
                    val repository = ReminderRepository(database.reminderDao())
                    val viewModel: ReminderViewModel = viewModel(
                        factory = ReminderViewModelFactory(repository, application)
                    )
                    
                    NavHost(
                        navController = navController,
                        startDestination = "reminder_list"
                    ) {
                        composable("reminder_list") {
                            ReminderListScreen(
                                viewModel = viewModel,
                                onAddReminder = { navController.navigate("input_screen") },
                                onReminderClick = { reminder ->
                                    navController.navigate("input_screen?reminderId=${reminder.id}")
                                },
                                onEditClick = { reminder ->
                                    navController.navigate("input_screen?reminderId=${reminder.id}")
                                },
                                onCalendarClick = { 
                                    android.util.Log.d("CalendarTest", "Calendar button clicked!")
                                    navController.navigate("calendar") 
                                },
                                onEmailClick = { reminder ->
                                    emailService.sendReminderEmail(this@MainActivity, reminder)
                                }
                            )
                        }
                        
                        composable("calendar") {
                            android.util.Log.d("CalendarTest", "Calendar screen navigated!")
                            CalendarScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onAddReminder = { navController.navigate("input_screen") },
                                onReminderClick = { reminder ->
                                    navController.navigate("input_screen?reminderId=${reminder.id}")
                                },
                                onAddReminderWithDate = { date ->
                                    navController.navigate("input_screen?selectedDate=${date.toString()}")
                                }
                            )
                        }
                        
                        composable("input_screen?reminderId={reminderId}&selectedDate={selectedDate}") { backStackEntry ->
                            val reminderId = backStackEntry.arguments?.getString("reminderId")?.toIntOrNull()
                            val selectedDateString = backStackEntry.arguments?.getString("selectedDate")
                            val selectedDate = selectedDateString?.let { 
                                try {
                                    java.time.LocalDate.parse(it)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            InputScreen(
                                viewModel = viewModel,
                                speechManager = speechManager,
                                reminderId = reminderId,
                                onBack = { navController.popBackStack() },
                                onConfirm = { text, reminderTime ->
                                    // Save reminder directly
                                    val reminder = com.reminder.app.data.Reminder(
                                        content = text,
                                        category = "Personal",
                                        importance = 5,
                                        reminderTime = reminderTime
                                    )
                                    viewModel.addReminder(reminder)
                                    navController.popBackStack()
                                },
                                onCalendarClick = { navController.navigate("calendar") }
                            )
                        }
                        
                        composable("confirmation_screen/{text}") { backStackEntry ->
                            val text = backStackEntry.arguments?.getString("text") ?: ""
                            ConfirmationScreen(
                                initialText = text,
                                speechManager = speechManager,
                                onConfirm = { confirmedText ->
                                    // Save the reminder to database
                                    val reminder = com.reminder.app.data.Reminder(
                                        content = confirmedText,
                                        category = "Personal",
                                        importance = 5,
                                        reminderTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000, // Tomorrow
                                        repeatType = "none",
                                        repeatInterval = 1
                                    )
                                    viewModel.addReminder(reminder)
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() },
                                onCalendarClick = { navController.navigate("calendar") }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        intent?.let { 
            when (it.action) {
                "android.intent.action.CREATE_NOTE",
                "com.google.android.gms.actions.CREATE_NOTE",
                "com.reminder.app.CREATE_REMINDER",
                "android.intent.action.SEND",
                "android.intent.action.SENDTO" -> {
                    val text = it.getStringExtra(Intent.EXTRA_TEXT) ?: 
                               it.getStringExtra("android.intent.extra.TEXT") ?: return
                    if (text.isNotBlank()) {
                        createReminderFromVoice(text)
                        // Show confirmation that reminder was created
                        showReminderCreatedConfirmation()
                    }
                }
                "com.reminder.app.VOICE_INPUT" -> {
                    val voiceInput = it.getStringExtra("voice_input")
                    if (!voiceInput.isNullOrBlank()) {
                        // Navigate to input screen with the voice input pre-filled
                        // This will be handled by the navigation system
                        // For now, create the reminder directly
                        createReminderFromVoice(voiceInput)
                        showReminderCreatedConfirmation()
                    }
                }
            }
        }
    }
    
    private fun showReminderCreatedConfirmation() {
        // You could show a Toast or Snackbar here
        // For now, the reminder appearing in the list is confirmation enough
    }
    
    private fun createReminderFromVoice(text: String) {
        val reminder = com.reminder.app.data.Reminder(
            content = text,
            category = "Personal",
            importance = 5,
            reminderTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000, // Tomorrow
            repeatType = "none",
            repeatInterval = 1
        )
        
        // Get ViewModel and save reminder
        val database = ReminderDatabase.getDatabase(this)
        val repository = ReminderRepository(database.reminderDao())
        val viewModel = ReminderViewModel(repository, application)
        viewModel.addReminder(reminder)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechManager.destroy()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // Handle speech manager results
        speechManager.handleActivityResult(requestCode, resultCode, data)
        
        // Handle keyboard voice input results
        if (requestCode == 1002 && resultCode == Activity.RESULT_OK && data != null) {
            val matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                // Navigate to input screen with the voice result
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("voice_input", matches[0])
                    action = "com.reminder.app.VOICE_INPUT"
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        }
    }
}