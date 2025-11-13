package com.reminder.app.utils

import android.content.Context
import android.content.Intent
import com.reminder.app.data.Reminder

/**
 * Simple email service without attachments for maximum compatibility
 */
class SimpleEmailService {
    
    companion object {
        private const val EMAIL_SUBJECT = "Reminder: %s"
        private val EMAIL_BODY = """
            Hello,
            
            This is your reminder:
            
            %s
            
            Time: %s
            Date: %s
            
            Best regards,
            Reminder App
        """.trimIndent()
    }
    
    /**
     * Send simple email without attachments
     */
    fun sendReminderEmail(context: Context, reminder: Reminder) {
        try {
            // Create simple share intent that works with most apps
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, String.format(EMAIL_SUBJECT, reminder.content.take(50)))
                putExtra(Intent.EXTRA_TEXT, String.format(
                    EMAIL_BODY,
                    reminder.content,
                    formatReminderTime(reminder.reminderTime),
                    formatReminderDate(reminder.whenDay)
                ))
            }
            
            // Show chooser with all available apps
            context.startActivity(Intent.createChooser(shareIntent, "Share reminder via email"))
            
        } catch (e: Exception) {
            // Ultimate fallback - just copy to clipboard
            val text = "${reminder.content}\n\nTime: ${formatReminderTime(reminder.reminderTime)}\nDate: ${formatReminderDate(reminder.whenDay)}"
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Reminder", text)
            clipboard.setPrimaryClip(clip)
            
            // Show toast (would need to import Toast)
            android.widget.Toast.makeText(context, "Reminder copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Format reminder time for display
     */
    private fun formatReminderTime(timestamp: Long): String {
        return java.text.SimpleDateFormat("h:mm a, MMM d, yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    }
    
    /**
     * Format reminder date for display
     */
    private fun formatReminderDate(dateString: String?): String {
        return dateString ?: "No date"
    }
}