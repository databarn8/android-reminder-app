package com.reminder.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.time.LocalDate

// Enhanced alert configuration data structures
@Serializable
data class AlertConfig(
    val alertType: AlertType = AlertType.NOTIFICATION_ONLY,
    val vibration: VibrationConfig = VibrationConfig(),
    val sound: SoundConfig = SoundConfig(),
    val series: AlertSeries = AlertSeries()
)

@Serializable
enum class AlertType {
    NOTIFICATION_ONLY,
    NOTIFICATION_VIBRATION,
    NOTIFICATION_SOUND,
    FULL_ALERT
}

@Serializable
data class VibrationConfig(
    val enabled: Boolean = true,
    val pattern: VibrationPattern = VibrationPattern.SINGLE,
    val intensity: VibrationIntensity = VibrationIntensity.MEDIUM,
    val seriesCount: Int = 1,
    val seriesInterval: Int = 1000 // ms between series
)

@Serializable
enum class VibrationPattern {
    SINGLE,      // One short vibration
    DOUBLE,       // Two short vibrations
    TRIPLE,       // Three short vibrations
    LONG,         // One long vibration
    PULSE,        // Pulsing pattern
    CUSTOM        // User-defined pattern
}

@Serializable
enum class VibrationIntensity {
    LIGHT, MEDIUM, STRONG
}

@Serializable
data class SoundConfig(
    val enabled: Boolean = true,
    val type: SoundType = SoundType.DEFAULT,
    val volume: Float = 0.8f,
    val seriesCount: Int = 1,
    val seriesInterval: Int = 2000, // ms between series
    val customSoundUri: String? = null // Future feature
)

@Serializable
enum class SoundType {
    DEFAULT,      // System default notification
    ALARM,        // System alarm sound
    GENTLE,       // Soft notification sound
    URGENT,       // Loud attention sound
    CUSTOM         // User-selected sound (future)
}

@Serializable
data class AlertSeries(
    val enabled: Boolean = false,
    val maxAttempts: Int = 3,
    val intervalMinutes: Int = 5,
    val escalationEnabled: Boolean = true,
    val stopOnAcknowledge: Boolean = true
)

// Repeat pattern data structure
@Serializable
data class RepeatPattern(
    val type: RepeatType = RepeatType.NONE,
    val interval: Int = 1, // Every X days/weeks/months
    val daysOfWeek: List<java.time.DayOfWeek>? = null, // For weekly repeats
    val dayOfMonth: Int? = null, // For monthly repeats
    @Serializable(with = LocalDateSerializer::class)
    val endDate: java.time.LocalDate? = null // Optional end date
)

@Serializable
enum class RepeatType {
    NONE, MINUTELY, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM
}

// LocalDate serializer for kotlinx.serialization
object LocalDateSerializer : kotlinx.serialization.KSerializer<LocalDate> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("LocalDate", kotlinx.serialization.descriptors.PrimitiveKind.STRING)
    
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: LocalDate) {
        encoder.encodeString(value.toString())
    }
    
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString())
    }
}

// Helper functions for serialization
fun AlertConfig.toJson(): String {
    return kotlinx.serialization.json.Json.encodeToString(
        AlertConfig.serializer(),
        this
    )
}

fun AlertConfig.Companion.fromJson(json: String): AlertConfig {
    return kotlinx.serialization.json.Json.decodeFromString(
        AlertConfig.serializer(),
        json
    )
}

fun RepeatPattern.toJson(): String {
    return kotlinx.serialization.json.Json.encodeToString(
        RepeatPattern.serializer(),
        this
    )
}

fun RepeatPattern.Companion.fromJson(json: String): RepeatPattern {
    return kotlinx.serialization.json.Json.decodeFromString(
        RepeatPattern.serializer(),
        json
    )
}