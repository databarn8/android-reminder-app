package com.reminder.app.data

import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.content.pm.ResolveInfo

/**
 * Data class to hold email client preferences
 */
data class EmailClientPreference(
    val packageName: String = "",
    val appName: String = "",
    val lastUsedTime: Long = 0L
)

/**
 * Manager class to handle email client preferences
 */
class EmailPreferencesManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "email_preferences"
        private const val KEY_PREFERRED_PACKAGE = "preferred_email_package"
        private const val KEY_PREFERRED_APP_NAME = "preferred_email_app_name"
        private const val KEY_LAST_USED_TIME = "preferred_email_last_used"
    }
    
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Save the preferred email client
     */
    fun savePreferredEmailClient(packageName: String, appName: String) {
        sharedPreferences.edit().apply {
            putString(KEY_PREFERRED_PACKAGE, packageName)
            putString(KEY_PREFERRED_APP_NAME, appName)
            putLong(KEY_LAST_USED_TIME, System.currentTimeMillis())
            apply()
        }
    }
    
    /**
     * Get the preferred email client
     */
    fun getPreferredEmailClient(): EmailClientPreference {
        val packageName = sharedPreferences.getString(KEY_PREFERRED_PACKAGE, "") ?: ""
        val appName = sharedPreferences.getString(KEY_PREFERRED_APP_NAME, "") ?: ""
        val lastUsedTime = sharedPreferences.getLong(KEY_LAST_USED_TIME, 0L)
        
        return EmailClientPreference(packageName, appName, lastUsedTime)
    }
    
    /**
     * Check if a preferred email client exists and is still installed
     */
    fun hasPreferredEmailClient(): Boolean {
        val preference = getPreferredEmailClient()
        return preference.packageName.isNotEmpty() && isAppInstalled(preference.packageName)
    }
    
    /**
     * Clear the preferred email client
     */
    fun clearPreferredEmailClient() {
        sharedPreferences.edit().apply {
            remove(KEY_PREFERRED_PACKAGE)
            remove(KEY_PREFERRED_APP_NAME)
            remove(KEY_LAST_USED_TIME)
            apply()
        }
    }
    
    /**
     * Check if an app is installed
     */
    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Get list of available email clients
     */
    fun getAvailableEmailClients(): List<EmailClientPreference> {
        val emailClients = mutableListOf<EmailClientPreference>()
        
        // Create email intent to find email apps
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
        }
        
        val resolveInfos = context.packageManager.queryIntentActivities(
            emailIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        
        for (resolveInfo in resolveInfos) {
            val packageName = resolveInfo.activityInfo.packageName
            val appName = resolveInfo.loadLabel(context.packageManager).toString()
            
            // Skip system apps that aren't really email clients
            if (isEmailClient(packageName)) {
                emailClients.add(EmailClientPreference(packageName, appName))
            }
        }
        
        return emailClients.distinctBy { it.packageName }
    }
    
    /**
     * Check if a package is likely an email client
     */
    private fun isEmailClient(packageName: String): Boolean {
        val knownEmailClients = setOf(
            "com.google.android.gm", // Gmail
            "com.microsoft.office.outlook", // Outlook
            "com.yahoo.mobile.client.android.mail", // Yahoo Mail
            "com.fsck.k9", // K-9 Mail
            "com.android.email", // Android Email
            "com.samsung.android.email.provider", // Samsung Email
            "com.htc.android.mail", // HTC Mail
            "com.tmobile.us.mail", // T-Mobile Mail
            "com.aol.mobile.mail", // AOL Mail
            "com.google.android.apps.gmail", // Gmail (alternative package)
            "com.google.android.gm.lite", // Gmail Go
            "com.bluepointe.foxmail", // Foxmail
            "com.my.mail.android" // myMail
        )
        
        // Check if it's a known email client
        if (knownEmailClients.contains(packageName)) {
            return true
        }
        
        // Check if package name contains email-related keywords
        val emailKeywords = listOf("mail", "email", "gmail", "outlook", "yahoo")
        return emailKeywords.any { keyword ->
            packageName.lowercase().contains(keyword)
        }
    }
    
    /**
     * Create an email intent targeting the preferred client if available
     */
    fun createEmailIntent(subject: String, body: String, forceChooser: Boolean = false): Intent {
        val baseIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        
        // If forceChooser is true or no preferred client, show chooser
        if (forceChooser || !hasPreferredEmailClient()) {
            return Intent.createChooser(baseIntent, "Send reminder via email")
        }
        
        // Target the preferred email client
        val preference = getPreferredEmailClient()
        baseIntent.setPackage(preference.packageName)
        
        // Verify the app can handle this intent
        val resolveInfo = context.packageManager.resolveActivity(
            baseIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        
        return if (resolveInfo != null) {
            baseIntent
        } else {
            // Fallback to chooser if preferred client can't handle the intent
            Intent.createChooser(baseIntent, "Send reminder via email")
        }
    }
    
    /**
     * Update the preferred email client after successful email sending
     */
    fun updatePreferredEmailClient(intent: Intent?) {
        intent?.let {
            // Try to extract package info from the intent
            val packageName = it.`package` ?: return@let
            
            // Get app name from package manager
            val appName = try {
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                context.packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                packageName
            }
            
            savePreferredEmailClient(packageName, appName)
        }
    }
}