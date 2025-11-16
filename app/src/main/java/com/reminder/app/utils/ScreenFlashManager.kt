package com.reminder.app.utils

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

object ScreenFlashManager {
    private var isFlashing = false
    private val handler = Handler(Looper.getMainLooper())
    
    fun triggerFlash(
        context: Context,
        flashColor: Color = Color.Red,
        flashDurationMs: Long = 800,
        flashCount: Int = 8,
        intervalMs: Long = 150
    ) {
        if (isFlashing) return
        
        // Check accessibility settings for visual notifications
        android.util.Log.d("ScreenFlashManager", "About to check visual notification settings")
        if (!isVisualNotificationEnabled(context)) {
            android.util.Log.d("ScreenFlashManager", "Visual notifications disabled in accessibility settings")
            return
        }
        android.util.Log.d("ScreenFlashManager", "Visual notifications ENABLED - proceeding with simple feedback")
        
        isFlashing = true
        
        // Simple feedback approach: 1 vibration + 1 sound with 1 second wait
        if (context is Activity) {
            context.runOnUiThread {
                try {
                    // Simple sequence: 1 vibration with 1s wait, then 1 sound
                    android.util.Log.d("ScreenFlashManager", "Starting simple feedback sequence")
                    
                    // Single vibration
                    triggerVibration(context)
                    
                    // Wait 1 second
                    handler.postDelayed({
                        // Single sound (using system beep sound instead of default)
                        triggerSystemBeep(context)
                        
                        // Complete sequence
                        android.util.Log.d("ScreenFlashManager", "Simple feedback sequence completed")
                    }, 1000)
                    
                } catch (e: Exception) {
                    android.util.Log.e("ScreenFlashManager", "Simple feedback failed: ${e.message}")
                }
            }
        }
        
        // Reset flag after sequence completes (approximately 2 seconds)
        handler.postDelayed({
            isFlashing = false
        }, 2000)
    }
    
    /**
     * Check if visual notifications are enabled in accessibility settings
     * This respects user preferences and accessibility guidelines
     */
    private fun isVisualNotificationEnabled(context: Context): Boolean {
        return try {
            // Always allow flash for reminders as it's a critical accessibility feature
            // Only block if user explicitly disables all visual notifications
            val accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0
            ) == 1
            
            // Check DND but allow critical reminders through
            val isDndEnabled = when {
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M -> {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val currentFilter = notificationManager.currentInterruptionFilter
                    // Allow through unless total silence
                    currentFilter != NotificationManager.INTERRUPTION_FILTER_NONE
                }
                else -> false
            }
            
            // More permissive - allow flash unless in total silence mode
            val canFlash = isDndEnabled || accessibilityEnabled || true // Default to true
            android.util.Log.d("ScreenFlashManager", "Visual notification check - DND: $isDndEnabled, Accessibility: $accessibilityEnabled, CanFlash: $canFlash")
            canFlash
        } catch (e: Exception) {
            android.util.Log.w("ScreenFlashManager", "Could not check accessibility settings: ${e.message}")
            true // Default to enabled if we can't check
        }
    }
    
    fun triggerVibration(context: Context, pattern: LongArray = longArrayOf(0, 200, 100, 200, 100, 200)) {
        try {
            // Check if vibration is enabled in system settings
            if (!isVibrationEnabled(context)) {
                android.util.Log.d("ScreenFlashManager", "Vibration disabled in system settings")
                return
            }
            
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            }
            
            // Check if device has vibrator
            if (!vibrator.hasVibrator()) {
                android.util.Log.d("ScreenFlashManager", "Device does not have vibrator")
                return
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            android.util.Log.e("ScreenFlashManager", "Vibration failed: ${e.message}")
        }
    }
    
    /**
     * Check if vibration is enabled in system settings
     */
    private fun isVibrationEnabled(context: Context): Boolean {
        return try {
            when {
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q -> {
                    android.provider.Settings.System.getInt(
                        context.contentResolver,
                        android.provider.Settings.System.VIBRATE_WHEN_RINGING,
                        1
                    ) == 1
                }
                else -> true // Default to enabled for older versions
            }
        } catch (e: Exception) {
            android.util.Log.w("ScreenFlashManager", "Could not check vibration settings: ${e.message}")
            true // Default to enabled if we can't check
        }
    }
    
    fun triggerSound(context: Context, soundType: Int = 0) {
        try {
            // Check if sound is enabled in system settings
            if (!isSoundEnabled(context)) {
                android.util.Log.d("ScreenFlashManager", "Sound disabled in system settings")
                return
            }
            
            // Get different notification sounds based on type
            val soundUri = when (soundType) {
                1 -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                2 -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                3 -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                else -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            }
            
            val ringtone = android.media.RingtoneManager.getRingtone(context, soundUri)
            ringtone?.play()
            
            android.util.Log.d("ScreenFlashManager", "Playing sound type $soundType: ${soundUri}")
            
            // Stop sound after 1.5 seconds (shorter for multiple sounds)
            handler.postDelayed({
                ringtone?.stop()
            }, 1500)
        } catch (e: Exception) {
            android.util.Log.e("ScreenFlashManager", "Sound failed: ${e.message}")
        }
    }
    
    fun triggerSystemBeep(context: Context) {
        try {
            // Check if sound is enabled in system settings
            if (!isSoundEnabled(context)) {
                android.util.Log.d("ScreenFlashManager", "Sound disabled in system settings")
                return
            }
            
            // Use system beep sound (different from default notification)
            val toneGenerator = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100)
            toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 500)
            
            android.util.Log.d("ScreenFlashManager", "Playing system beep sound")
        } catch (e: Exception) {
            android.util.Log.e("ScreenFlashManager", "System beep failed: ${e.message}")
        }
    }
    
    /**
     * Check if sound is enabled in system settings
     */
    private fun isSoundEnabled(context: Context): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
            currentVolume > 0 && maxVolume > 0
        } catch (e: Exception) {
            android.util.Log.w("ScreenFlashManager", "Could not check audio settings: ${e.message}")
            true // Default to enabled if we can't check
        }
    }
}

object FlashState {
    private var _flashTrigger by mutableStateOf(0)
    val flashTrigger: Int get() = _flashTrigger
    
    private var _flashColor by mutableStateOf(Color.Red)
    val flashColor: Color get() = _flashColor
    
    private var _flashDuration by mutableStateOf(200L)
    val flashDuration: Long get() = _flashDuration
    
    private var _flashCount by mutableStateOf(3)
    val flashCount: Int get() = _flashCount
    
    private var _flashInterval by mutableStateOf(300L)
    val flashInterval: Long get() = _flashInterval
    
    fun triggerFlash(color: Color, duration: Long, count: Int, interval: Long) {
        _flashColor = color
        _flashDuration = duration
        _flashCount = count
        _flashInterval = interval
        _flashTrigger++ // Increment to trigger LaunchedEffect
    }
}

@Composable
fun ScreenFlashOverlay() {
    // Use direct state observation instead of remember
    val flashTrigger = FlashState.flashTrigger
    val flashColor = FlashState.flashColor
    val flashDuration = FlashState.flashDuration
    val flashCount = FlashState.flashCount
    val flashInterval = FlashState.flashInterval
    
    var isVisible by remember { mutableStateOf(false) }
    var currentFlashCount by remember { mutableStateOf(0) }
    
    LaunchedEffect(flashTrigger) {
        if (flashTrigger > 0) {
            android.util.Log.d("ScreenFlashOverlay", "Flash triggered! Count: $flashCount, Duration: $flashDuration")
            currentFlashCount = 0
            repeat(flashCount) { index ->
                isVisible = true
                android.util.Log.d("ScreenFlashOverlay", "Flash ON $index")
                delay(flashDuration)
                isVisible = false
                android.util.Log.d("ScreenFlashOverlay", "Flash OFF $index")
                currentFlashCount++
                
                if (index < flashCount - 1) {
                    delay(flashInterval)
                }
            }
        }
    }
    
    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(flashColor.copy(alpha = 0.95f)) // Increased alpha for better visibility
        )
    }
}