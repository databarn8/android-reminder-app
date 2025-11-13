package com.reminder.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.reminder.app.data.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class DataExportImportManager(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    suspend fun exportToCSV(reminders: List<Reminder>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileName = "reminders_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
            val downloadsDir = File(context.getExternalFilesDir(null), "Downloads")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { fos ->
                val csvContent = buildString {
                    appendLine("ID,Content,Category,Importance,Reminder Time,Created At")
                    reminders.forEach { reminder ->
                        appendLine("${reminder.id},\"${reminder.content}\",\"${reminder.category}\",${reminder.importance},${reminder.reminderTime},${reminder.createdAt}")
                    }
                }
                fos.write(csvContent.toByteArray())
            }
            
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun exportToJSON(reminders: List<Reminder>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileName = "reminders_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
            val downloadsDir = File(context.getExternalFilesDir(null), "Downloads")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { fos ->
                val jsonArray = JSONArray()
                reminders.forEach { reminder ->
                    val jsonObject = JSONObject().apply {
                        put("id", reminder.id)
                        put("content", reminder.content)
                        put("category", reminder.category)
                        put("importance", reminder.importance)
                        put("reminderTime", reminder.reminderTime)
                        put("repeatType", reminder.repeatType)
                        put("repeatInterval", reminder.repeatInterval)
                        put("isActive", reminder.isActive)
                        put("voiceInput", reminder.voiceInput)
                        put("isProcessed", reminder.isProcessed)
                        put("createdAt", reminder.createdAt)
                    }
                    jsonArray.put(jsonObject)
                }
                
                val jsonContent = jsonArray.toString(2)
                fos.write(jsonContent.toByteArray())
            }
            
            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun importFromCSV(uri: Uri): Result<List<Reminder>> = withContext(Dispatchers.IO) {
        try {
            val reminders = mutableListOf<Reminder>()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLine() // Skip header
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        line?.let {
                             val parts = it.split(",", limit = 6)
                            if (parts.size >= 6) {
                                val reminder = Reminder(
                                    id = parts[0].toIntOrNull() ?: 0,
                                    content = parts[1].trim('"'),
                                    category = parts[2].trim('"'),
                                    importance = parts[3].toIntOrNull() ?: 5,
                                    reminderTime = parts[4].toLongOrNull() ?: System.currentTimeMillis(),
                                    createdAt = parts[5].toLongOrNull() ?: System.currentTimeMillis()
                                )
                                reminders.add(reminder)
                            }
                        }
                    }
                }
            }
            Result.success(reminders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun importFromJSON(uri: Uri): Result<List<Reminder>> = withContext(Dispatchers.IO) {
        try {
            val reminders = mutableListOf<Reminder>()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val jsonContent = inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(jsonContent)
                
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                     val reminder = Reminder(
                        id = jsonObject.optInt("id", 0),
                        content = jsonObject.optString("content", jsonObject.optString("request", jsonObject.optString("title", ""))),
                        category = jsonObject.optString("category", "Personal"),
                        importance = jsonObject.optInt("importance", 5),
                        reminderTime = jsonObject.optLong("reminderTime", System.currentTimeMillis()),
                        repeatType = jsonObject.optString("repeatType", "none"),
                        repeatInterval = jsonObject.optInt("repeatInterval", 1),
                        isActive = jsonObject.optBoolean("isActive", true),
                        voiceInput = if (jsonObject.has("voiceInput")) jsonObject.optString("voiceInput") else null,
                        isProcessed = jsonObject.optBoolean("isProcessed", false),
                        createdAt = jsonObject.optLong("createdAt", System.currentTimeMillis())
                    )
                    reminders.add(reminder)
                }
            }
            Result.success(reminders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun launchFilePicker(launcher: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "application/json"))
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        launcher.launch(Intent.createChooser(intent, "Select file to import"))
    }
}