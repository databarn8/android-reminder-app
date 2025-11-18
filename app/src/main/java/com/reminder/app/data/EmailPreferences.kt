package com.reminder.app.data

import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri

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
        
        // Create email intent to find email apps - use SENDTO with mailto: to target only email clients
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
        }
        
        val resolveInfos = context.packageManager.queryIntentActivities(
            emailIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        
        android.util.Log.d("EmailPreferences", "Found ${resolveInfos.size} apps that can handle email intent")
        
        for (resolveInfo in resolveInfos) {
            val packageName = resolveInfo.activityInfo.packageName
            val appName = resolveInfo.loadLabel(context.packageManager).toString()
            
            android.util.Log.d("EmailPreferences", "Checking app: $appName ($packageName)")
            
            // Add ALL apps first, then filter
            emailClients.add(EmailClientPreference(packageName, appName))
        }
        
        val distinctClients = emailClients.distinctBy { it.packageName }
        android.util.Log.d("EmailPreferences", "Final email client list: ${distinctClients.size} apps")
        distinctClients.forEach {
            android.util.Log.d("EmailPreferences", " - ${it.appName} (${it.packageName})")
        }
        
        return distinctClients
    }
    
    /**
     * Check if a package is definitely NOT an email client
     */
    private fun isNonEmailApp(packageName: String, appName: String): Boolean {
        val knownNonEmailApps = setOf(
            "com.android.bluetooth", // Bluetooth
            "com.android.phone", // Phone
            "com.android.settings", // Settings
            "com.android.systemui", // System UI
            "com.google.android.apps.maps", // Maps
            "com.google.android.youtube", // YouTube
            "com.google.android.apps.photos", // Photos
            "com.google.android.gms", // Google Play Services
            "com.android.vending", // Play Store
            "com.whatsapp", // WhatsApp (though it can share, it's not primarily email)
            "com.facebook.katana", // Facebook
            "com.instagram.android", // Instagram
            "com.twitter.android", // Twitter
            "com.tiktok", // TikTok
            "com.netflix.mediaclient", // Netflix
            "com.spotify.music", // Spotify
            "com.google.android.apps.docs.editors.docs", // Google Docs
            "com.google.android.apps.docs.editors.sheets", // Google Sheets
            "com.google.android.apps.docs.editors.slides", // Google Slides
            "com.google.android.apps.messaging", // Android Messages
            "com.samsung.android.messaging" // Samsung Messages
        )
        
        // Check if it's a known non-email app
        if (knownNonEmailApps.contains(packageName)) {
            return true
        }
        
        // Check if app name suggests it's not an email client
        val nonEmailKeywords = listOf("camera", "gallery", "music", "video", "game", "browser", "file manager")
        return nonEmailKeywords.any { keyword ->
            appName.lowercase().contains(keyword) || packageName.lowercase().contains(keyword)
        }
    }
    
    /**
     * Create an email intent targeting the preferred client if available
     */
    fun createEmailIntent(subject: String, body: String, forceChooser: Boolean = false): Intent {
        val baseIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:reminder@example.com") // Add dummy email to ensure email apps only
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
            // The package name might be in different fields depending on how the chooser was used
            var packageName = it.`package`
            
            // If package is null, try to get it from the component
            if (packageName == null) {
                packageName = it.component?.packageName
            }
            
            // If still null, try to extract from the selector (for newer Android versions)
            if (packageName == null) {
                val selector = it.selector
                if (selector != null) {
                    packageName = selector.`package`
                }
            }
            
            // If we still don't have a package name, we can't update the preference
            if (packageName == null) {
                return@let
            }
            
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