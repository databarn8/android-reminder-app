package com.reminder.app.ui.screens

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reminder.app.ui.theme.ReminderAppTheme
import kotlinx.coroutines.delay

class AlarmActivity : ComponentActivity() {
    
    private var mediaPlayer: MediaPlayer? = null
    private var handler: Handler? = null
    private var isAlarmDismissed = false
    private var alarmCount = 0
    private val maxAlarms = 5
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set flags to show alarm over lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        
        val title = intent.getStringExtra("alarm_title") ?: "Reminder"
        val content = intent.getStringExtra("alarm_content") ?: "Your reminder is due!"
        val reminderId = intent.getIntExtra("reminder_id", -1)
        
        handler = Handler(Looper.getMainLooper())
        
        setContent {
            ReminderAppTheme {
                AlarmScreen(
                    title = title,
                    content = content,
                    onDismiss = ::dismissAlarm,
                    alarmCount = alarmCount,
                    maxAlarms = maxAlarms
                )
            }
        }
        
        // Start the repeating alarm
        startRepeatingAlarm()
    }
    
    private fun startRepeatingAlarm() {
        if (isAlarmDismissed) return
        
        playAlarmSound()
        alarmCount++
        
        // Schedule next alarm if not dismissed and under max
        if (!isAlarmDismissed && alarmCount < maxAlarms) {
            handler?.postDelayed({
                startRepeatingAlarm()
            }, 60000) // 1 minute delay
        }
    }
    
    private fun playAlarmSound() {
        try {
            // Stop any existing sound
            mediaPlayer?.stop()
            mediaPlayer?.release()
            
            // Get default alarm sound
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                isLooping = false
                setAudioStreamType(AudioManager.STREAM_ALARM)
                prepare()
                start()
                
                // Auto-stop after 10 seconds
                handler?.postDelayed({
                    if (!isReleased) {
                        stop()
                        release()
                    }
                }, 10000)
            }
        } catch (e: Exception) {
            // Fallback to system sound
            try {
                val ringtone = RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                ringtone?.play()
                
                handler?.postDelayed({
                    ringtone?.stop()
                }, 10000)
            } catch (e2: Exception) {
                android.util.Log.e("AlarmActivity", "Could not play alarm sound: ${e2.message}")
            }
        }
    }
    
    private fun dismissAlarm() {
        isAlarmDismissed = true
        
        // Stop sound
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        
        // Cancel any pending alarms
        handler?.removeCallbacksAndMessages(null)
        
        // Finish activity
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        dismissAlarm()
    }
    
    override fun onBackPressed() {
        // Handle back button as dismiss
        dismissAlarm()
    }
}

@Composable
fun AlarmScreen(
    title: String,
    content: String,
    onDismiss: () -> Unit,
    alarmCount: Int,
    maxAlarms: Int
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Update time every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }
    
    val timeString = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date(currentTime))
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Current time
                Text(
                    text = timeString,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
                
                // Alarm icon and count
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⏰",
                        fontSize = 48.sp
                    )
                    Text(
                        text = "Alarm $alarmCount of $maxAlarms",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Reminder title
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                
                // Reminder content
                Text(
                    text = content,
                    fontSize = 18.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                // Dismiss button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop Alarm",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DISMISS ALARM",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Instructions
                Text(
                    text = "Tap DISMISS to stop the alarm\nNext alarm in 1 minute if not dismissed",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}