package com.reminder.app.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class SpeechManager(private val context: Context) : TextToSpeech.OnInitListener {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isVoiceFeedbackEnabled = false
    private var permissionCallback: ((Boolean) -> Unit)? = null
    private var activity: Activity? = null
    
    fun setActivity(activity: Activity) {
        this.activity = activity
    }
    
    private val _speechResult = MutableStateFlow<String?>(null)
    val speechResult: StateFlow<String?> = _speechResult.asStateFlow()
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private val _ttsStatus = MutableStateFlow(false)
    val ttsStatus: StateFlow<Boolean> = _ttsStatus.asStateFlow()
    
    private val _permissionNeeded = MutableStateFlow(false)
    val permissionNeeded: StateFlow<Boolean> = _permissionNeeded.asStateFlow()

    init {
        initializeSpeechRecognizer()
        initializeTextToSpeech()
    }

    private fun initializeSpeechRecognizer() {
        if (isGoogleSpeechServicesAvailable()) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        Log.d("SpeechManager", "Ready for speech")
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d("SpeechManager", "Beginning of speech")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Can be used for visual feedback
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {
                        // Buffer received
                    }

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        Log.d("SpeechManager", "End of speech")
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected, please try again"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                            else -> "Unknown error ($error)"
                        }
                        Log.e("SpeechManager", "Speech recognition error: $errorMessage")
                        
                        // For Android 11+, try fallback on certain errors
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && 
                            (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_SERVER)) {
                            Log.d("SpeechManager", "Trying fallback to intent-based recognition")
                            startIntentBasedSpeechRecognition()
                        } else {
                            _speechResult.value = errorMessage
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _speechResult.value = matches[0]
                            Log.d("SpeechManager", "Speech result: ${matches[0]}")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            Log.d("SpeechManager", "Partial result: ${matches[0]}")
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {
                        // Events
                    }
                })
                Log.d("SpeechManager", "Speech recognizer initialized successfully")
            } catch (e: Exception) {
                Log.e("SpeechManager", "Error initializing speech recognizer: ${e.message}")
            }
        } else {
            Log.w("SpeechManager", "Google speech services not available")
        }
    }
    
    private fun isGoogleSpeechServicesAvailable(): Boolean {
        return try {
            val pm = context.packageManager
            
            // Check specifically for Google app speech recognition
            val googleIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                setClassName(
                    "com.google.android.googlequicksearchbox",
                    "com.google.android.voicesearch.intentapi.IntentApiActivity"
                )
            }
            
            val googleAvailable = googleIntent.resolveActivity(pm) != null
            Log.d("SpeechManager", "Google speech recognition available: $googleAvailable")
            
            if (googleAvailable) {
                return true
            }
            
            // Fallback to general check
            val packages = pm.queryIntentActivities(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0
            )
            Log.d("SpeechManager", "Found ${packages.size} general speech recognition activities")
            packages.forEach { 
                Log.d("SpeechManager", "Available speech app: ${it.activityInfo.packageName}")
            }
            packages.isNotEmpty()
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error checking speech services availability: ${e.message}")
            false
        }
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context, this)
    }

    fun startListening() {
        Log.d("SpeechManager", "startListening() called")
        Log.d("SpeechManager", "hasAudioPermission: ${hasAudioPermission()}")
        Log.d("SpeechManager", "SpeechRecognizer available: ${SpeechRecognizer.isRecognitionAvailable(context)}")
        Log.d("SpeechManager", "Google speech services available: ${isGoogleSpeechServicesAvailable()}")
        Log.d("SpeechManager", "Android version: ${Build.VERSION.SDK_INT}")
        
        if (!hasAudioPermission()) {
            Log.w("SpeechManager", "Audio permission not granted")
            _permissionNeeded.value = true
            _speechResult.value = "Please grant microphone permission"
            return
        }
        
        // Check if any speech recognition is available at all
        if (!isGoogleSpeechServicesAvailable()) {
            Log.e("SpeechManager", "No speech recognition services available")
            _speechResult.value = "Voice input not available on this device. Try using Google Assistant: 'Hey Google, remind me to...'"
            return
        }
        
        // Since speech recognition doesn't work properly on this device, show helpful message
        _speechResult.value = "Try: 'Hey Google, remind me to [your reminder]'"
    }
    
    private fun startDirectSpeechRecognition(): Boolean {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("SpeechManager", "Speech recognition not available on this device")
            _speechResult.value = "Speech recognition not available on this device"
            return false
        }
        
        if (speechRecognizer == null) {
            initializeSpeechRecognizer()
        }
        
        if (speechRecognizer == null) {
            Log.e("SpeechManager", "Speech recognizer not available")
            _speechResult.value = "Speech recognition not available"
            return false
        }
        
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false) // Prefer online for better accuracy
                
                // Android 11+ specific flags
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    putExtra(RecognizerIntent.EXTRA_SECURE, false)
                    // Add extra flags for Android 11 compatibility
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            
            speechRecognizer?.startListening(intent)
            Log.d("SpeechManager", "Started direct speech recognition successfully")
            return true
        } catch (e: SecurityException) {
            Log.e("SpeechManager", "Security exception in direct speech recognition: ${e.message}")
            _isListening.value = false
            _speechResult.value = "Permission denied. Please grant microphone access."
            return false
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error starting direct speech recognition: ${e.message}")
            _isListening.value = false
            // Don't set error message here, try fallback first
            return false
        }
    }
    
    private fun startIntentBasedSpeechRecognition() {
        Log.d("SpeechManager", "Trying intent-based speech recognition")
        
        try {
            // Use the specific Google app component that we know works
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your reminder...")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                
                // Set the specific component that handles speech recognition
                setClassName(
                    "com.google.android.googlequicksearchbox",
                    "com.google.android.voicesearch.intentapi.IntentApiActivity"
                )
                
                // Android 11+ specific handling
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    // Ensure we're not using secure mode
                    putExtra(RecognizerIntent.EXTRA_SECURE, false)
                }
            }
            
            activity?.let { act ->
                act.startActivityForResult(intent, SPEECH_REQUEST_CODE)
                _isListening.value = true
                Log.d("SpeechManager", "Started speech recognition with Google app")
            } ?: run {
                Log.e("SpeechManager", "Activity not available for speech recognition")
                _speechResult.value = "Speech recognition not available - please restart the app"
            }
        } catch (e: SecurityException) {
            Log.e("SpeechManager", "Security exception in speech recognition: ${e.message}")
            _isListening.value = false
            _speechResult.value = "Permission denied for speech recognition"
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error starting speech recognition: ${e.message}")
            _isListening.value = false
            _speechResult.value = "Speech recognition failed: ${e.message}"
        }
    }
    
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SPEECH_REQUEST_CODE) {
            _isListening.value = false
            if (resultCode == Activity.RESULT_OK && data != null) {
                val matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!matches.isNullOrEmpty()) {
                    _speechResult.value = matches[0]
                    Log.d("SpeechManager", "Intent-based speech result: ${matches[0]}")
                } else {
                    Log.w("SpeechManager", "No speech results from intent")
                    _speechResult.value = null
                }
            } else {
                Log.w("SpeechManager", "Intent-based speech recognition failed or cancelled")
                _speechResult.value = null
            }
        }
    }
    
    companion object {
        private const val SPEECH_REQUEST_CODE = 1001
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
    }

    fun setVoiceFeedbackEnabled(enabled: Boolean) {
        isVoiceFeedbackEnabled = enabled
    }

    fun speak(text: String) {
        if (isVoiceFeedbackEnabled && _ttsStatus.value) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onInit(status: Int) {
        _ttsStatus.value = status == TextToSpeech.SUCCESS
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.getDefault()
            Log.d("SpeechManager", "TTS initialized successfully")
        } else {
            Log.e("SpeechManager", "TTS initialization failed")
        }
    }

    fun clearSpeechResult() {
        _speechResult.value = null
    }
    
    fun restartSpeechRecognizer() {
        Log.d("SpeechManager", "Restarting speech recognizer")
        speechRecognizer?.destroy()
        speechRecognizer = null
        initializeSpeechRecognizer()
    }
    
    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    

    
    fun onPermissionResult(granted: Boolean) {
        _permissionNeeded.value = false
        if (granted) {
            Log.d("SpeechManager", "Audio permission granted")
            // Try to initialize speech recognizer again
            initializeSpeechRecognizer()
        } else {
            Log.w("SpeechManager", "Audio permission denied")
            _speechResult.value = "Microphone permission required for speech recognition"
        }
    }
}