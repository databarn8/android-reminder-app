package com.reminder.app.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import com.reminder.app.data.Reminder
import com.reminder.app.data.EmailPreferencesManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced Email Service with improved reminder content and formatting
 */
class EnhancedEmailService {
    
    private var emailPreferencesManager: EmailPreferencesManager? = null
    
    companion object {
        private const val EMAIL_SUBJECT = "🔔 Reminder: %s"
        private val EMAIL_BODY = """
            📅 REMINDER NOTIFICATION
            
            Your reminder is now due:
            
            📝 Task: %s
            
            ⏰ Scheduled Time: %s
            📅 Due Date: %s
            🔄 Repeats: %s
            🏷️ Category: %s
            ⭐ Priority: %s
            
            ---
            This is an automated reminder from your Reminder App.
            Please complete this task at your earliest convenience.
            
            📱 Reminder App
            Your personal task assistant
        """.trimIndent()
    }
    
    /**
     * Send email with enhanced reminder details
     */
    fun sendReminderEmail(context: Context, reminder: Reminder, forceChooser: Boolean = false) {
        try {
            // Initialize preferences manager if not already done
            if (emailPreferencesManager == null) {
                emailPreferencesManager = EmailPreferencesManager(context)
            }
            
            // Create email intent with proper email-specific configuration
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("")) // User can add recipient
                putExtra(Intent.EXTRA_SUBJECT, String.format(EMAIL_SUBJECT, reminder.content.take(30)))
                putExtra(Intent.EXTRA_TEXT, String.format(
                    EMAIL_BODY,
                    reminder.content,
                    formatReminderTime(reminder.reminderTime),
                    formatReminderDate(reminder.whenDay),
                    formatRepeatInfo(reminder.repeatType, reminder.repeatInterval),
                    reminder.category ?: "General",
                    formatPriority(reminder.importance)
                ))
            }
            
            // Check if we have a preferred email client and forceChooser is false
            if (!forceChooser && emailPreferencesManager?.hasPreferredEmailClient() == true) {
                val preference = emailPreferencesManager?.getPreferredEmailClient()
                emailIntent.setPackage(preference?.packageName)
                context.startActivity(emailIntent)
            } else {
                // Show chooser
                context.startActivity(Intent.createChooser(emailIntent, "Send reminder via email"))
            }
            
        } catch (e: Exception) {
            // Fallback to basic sharing if email fails
            sendSimpleReminder(context, reminder)
        }
    }
    
    /**
     * Send email with reminder details and update preferences
     */
    fun sendReminderEmailAndUpdatePreference(context: Context, reminder: Reminder, forceChooser: Boolean = false) {
        sendReminderEmail(context, reminder, forceChooser)
        
        // Note: We can't directly update preferences here since we don't know which email client
        // user chose. This will be handled by the activity result callback.
    }
    
    /**
     * Send email with activity result launcher to capture user's email client choice
     */
    fun sendReminderEmailWithLauncher(
        context: Context,
        reminder: Reminder,
        launcher: ActivityResultLauncher<Intent>
    ) {
        try {
            // Initialize preferences manager if not already done
            if (emailPreferencesManager == null) {
                emailPreferencesManager = EmailPreferencesManager(context)
            }
            
            // Create Gmail intent directly - since Gmail is pre-installed on most Android devices
            val gmailIntent = Intent(Intent.ACTION_SEND).apply {
                setPackage("com.google.android.gm") // Target Gmail specifically
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("")) // User can add recipient
                putExtra(Intent.EXTRA_SUBJECT, String.format(EMAIL_SUBJECT, reminder.content.take(30)))
                putExtra(Intent.EXTRA_TEXT, String.format(
                    EMAIL_BODY,
                    reminder.content,
                    formatReminderTime(reminder.reminderTime),
                    formatReminderDate(reminder.whenDay),
                    formatRepeatInfo(reminder.repeatType, reminder.repeatInterval),
                    reminder.category ?: "General",
                    formatPriority(reminder.importance)
                ))
            }
            
            // Try to create and attach file with reminder details
            try {
                val reminderFile = createReminderFile(context, reminder)
                val fileUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    reminderFile
                )
                
                // Grant temporary permission to file
                gmailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                gmailIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
            } catch (fileException: Exception) {
                // Continue without attachment if file creation fails
                println("Could not create attachment: ${fileException.message}")
            }
            
            // Launch Gmail directly
            launcher.launch(gmailIntent)
            
        } catch (e: Exception) {
            // Fallback to basic sharing if email fails
            sendSimpleReminder(context, reminder)
        }
    }
    
    /**
     * Send email directly to a specific email client
     * This bypasses the chooser and goes straight to the specified app
     */
    fun sendReminderEmailToSpecificClient(
        context: Context,
        reminder: Reminder,
        packageName: String,
        launcher: ActivityResultLauncher<Intent>? = null
    ) {
        try {
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("")) // User can add recipient
                putExtra(Intent.EXTRA_SUBJECT, String.format(EMAIL_SUBJECT, reminder.content.take(30)))
                putExtra(Intent.EXTRA_TEXT, String.format(
                    EMAIL_BODY,
                    reminder.content,
                    formatReminderTime(reminder.reminderTime),
                    formatReminderDate(reminder.whenDay),
                    formatRepeatInfo(reminder.repeatType, reminder.repeatInterval),
                    reminder.category ?: "General",
                    formatPriority(reminder.importance)
                ))
                setPackage(packageName) // Directly target specific email app
            }
            
            // Try to create and attach file with reminder details
            try {
                val reminderFile = createReminderFile(context, reminder)
                val fileUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    reminderFile
                )
                
                // Grant temporary permission to file
                emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
            } catch (fileException: Exception) {
                // Continue without attachment if file creation fails
                println("Could not create attachment: ${fileException.message}")
            }
            
            // Launch directly to the specified email client
            if (launcher != null) {
                launcher.launch(emailIntent)
            } else {
                context.startActivity(emailIntent)
            }
            
        } catch (e: Exception) {
            // Fallback to basic sharing if email fails
            sendSimpleReminder(context, reminder)
        }
    }
    
    /**
     * Update email client preference after successful email sending
     */
    fun updateEmailPreference(context: Context, chosenIntent: Intent?) {
        if (emailPreferencesManager == null) {
            emailPreferencesManager = EmailPreferencesManager(context)
        }
        
        android.util.Log.d("EmailService", "Updating email preference from intent: $chosenIntent")
        
        // Try to extract package name from the chosen intent
        chosenIntent?.let { intent ->
            // First try to get package from the intent itself
            var packageName = intent.`package`
            android.util.Log.d("EmailService", "Package from intent.`package`: $packageName")
            
            // If not found, try to get from component
            if (packageName == null) {
                packageName = intent.component?.packageName
                android.util.Log.d("EmailService", "Package from component: $packageName")
            }
            
            // If still not found, try to get from selector
            if (packageName == null) {
                val selector = intent.selector
                if (selector != null) {
                    packageName = selector.`package`
                    android.util.Log.d("EmailService", "Package from selector: $packageName")
                }
            }
            
            // If we found a package name, save it as preference
            if (packageName != null && packageName.isNotEmpty()) {
                try {
                    val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                    val appName = context.packageManager.getApplicationLabel(appInfo).toString()
                    android.util.Log.d("EmailService", "Saving email preference: $appName ($packageName)")
                    emailPreferencesManager?.savePreferredEmailClient(packageName, appName)
                } catch (e: PackageManager.NameNotFoundException) {
                    android.util.Log.w("EmailService", "Package not found: $packageName", e)
                }
            } else {
                android.util.Log.w("EmailService", "Could not extract package name from email intent")
            }
        }
    }
    
    /**
     * Get current preferred email client
     */
    fun getPreferredEmailClient(context: Context): String {
        if (emailPreferencesManager == null) {
            emailPreferencesManager = EmailPreferencesManager(context)
        }
        val preference = emailPreferencesManager?.getPreferredEmailClient()
        return if (preference?.packageName?.isNotEmpty() == true) {
            preference.appName
        } else {
            "No preferred email client set"
        }
    }
    
    /**
     * Get available email clients
     */
    fun getAvailableEmailClients(context: Context) = EmailPreferencesManager(context).getAvailableEmailClients()
    
    /**
     * Clear email client preference
     */
    fun clearEmailPreference(context: Context) {
        if (emailPreferencesManager == null) {
            emailPreferencesManager = EmailPreferencesManager(context)
        }
        emailPreferencesManager?.clearPreferredEmailClient()
    }
    
    /**
     * Fallback to simple reminder sharing
     */
    private fun sendSimpleReminder(context: Context, reminder: Reminder) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Reminder: ${reminder.content.take(30)}")
                putExtra(Intent.EXTRA_TEXT, createSimpleReminderText(reminder))
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share reminder"))
        } catch (e: Exception) {
            // Ultimate fallback - copy to clipboard
            val text = createSimpleReminderText(reminder)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Reminder", text)
            clipboard.setPrimaryClip(clip)
            
            android.widget.Toast.makeText(context, "Reminder copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Create simple reminder text for fallback
     */
    private fun createSimpleReminderText(reminder: Reminder): String {
        return """
            📅 REMINDER DUE NOW!
            
            Task: ${reminder.content}
            Time: ${formatReminderTime(reminder.reminderTime)}
            Date: ${formatReminderDate(reminder.whenDay)}
            Priority: ${formatPriority(reminder.importance)}
            
            - Reminder App
        """.trimIndent()
    }
    
    /**
     * Create a file with reminder details for email attachment
     */
    private fun createReminderFile(context: Context, reminder: Reminder): File {
        val fileName = "reminder_${reminder.id}_${System.currentTimeMillis()}.txt"
        val content = """
            ========================================
            📅 REMINDER DETAILS
            ========================================
            
            TASK INFORMATION
            ----------------
            Content: ${reminder.content}
            Category: ${reminder.category ?: "General"}
            Priority Level: ${formatPriority(reminder.importance)}
            
            SCHEDULING
            ----------
            Reminder Time: ${formatReminderTime(reminder.reminderTime)}
            Due Date: ${formatReminderDate(reminder.whenDay)}
            Repeat Type: ${reminder.repeatType}
            Repeat Interval: Every ${reminder.repeatInterval} ${getIntervalUnit(reminder.repeatType, reminder.repeatInterval)}
            
            SYSTEM INFORMATION
            ------------------
            Reminder ID: ${reminder.id}
            Created: ${Date(reminder.createdAt)}
            Status: ${if (reminder.isActive) "Active" else "Inactive"}
            
            ========================================
            Generated by Reminder App
            Generated on: ${Date()}
            ========================================
        """.trimIndent()
        
        // Create file in app's internal storage
        val file = File(context.filesDir, fileName)
        file.writeText(content)
        return file
    }
    
    /**
     * Format reminder time for display
     */
    private fun formatReminderTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("h:mm a, MMMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    /**
     * Format reminder date for display
     */
    private fun formatReminderDate(dateString: String?): String {
        return dateString ?: "No specific date"
    }
    
    /**
     * Format repeat information
     */
    private fun formatRepeatInfo(repeatType: String, interval: Int): String {
        return if (repeatType == "none" || repeatType.isBlank()) {
            "Does not repeat"
        } else {
            "Every $interval ${getIntervalUnit(repeatType, interval)}"
        }
    }
    
    /**
     * Get interval unit based on repeat type and interval
     */
    private fun getIntervalUnit(repeatType: String, interval: Int): String {
        val unit = when (repeatType.lowercase()) {
            "daily" -> "day"
            "weekly" -> "week"
            "monthly" -> "month"
            "yearly" -> "year"
            else -> "period"
        }
        return if (interval == 1) unit else "${unit}s"
    }
    
    /**
     * Format priority level with emojis
     */
    private fun formatPriority(importance: Int): String {
        return when (importance) {
            1, 2 -> "🔴 High Priority"
            3 -> "🟡 Medium Priority"
            4, 5 -> "🟢 Low Priority"
            else -> "⚪ Normal Priority"
        }
    }
    
    /**
     * Check if email apps are available
     */
    fun isEmailAvailable(context: Context): Boolean {
        val packageManager = context.packageManager
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
        }
        val activities = packageManager.queryIntentActivities(emailIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return activities.isNotEmpty()
    }
}