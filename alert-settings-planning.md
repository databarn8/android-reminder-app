# Alert Settings Enhancement Planning

## Overview
Enhance the reminder app's alert configuration to provide users with comprehensive control over how reminders are notified.

## Current State
- Basic trigger configuration exists in InputScreen.kt (lines 1612-1753)
- Simple "At due time", "Minutes before", "Hours before", "Days before" checkboxes
- Limited to basic notification scheduling

## Proposed Enhancements

### 1. Alert Settings UI Improvements
**Goal**: Make "Alert Settings" section more intuitive and expandable

#### Current Issues:
- Configuration section is always expanded, taking up screen space
- Limited visual hierarchy
- No clear indication of what each setting does

#### Proposed Solution:
- Collapsible/expandable section with smooth animation
- Better visual indicators (icons, colors)
- Clear descriptions for each setting
- Natural scrolling behavior

### 2. Repeating Reminders
**Goal**: Allow users to set recurring reminders

#### Implementation Plan:
```kotlin
// Data structure for repeating patterns
data class RepeatPattern(
    val type: RepeatType, // DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM
    val interval: Int, // Every X days/weeks/months
    val daysOfWeek: List<DayOfWeek>? = null, // For weekly repeats
    val dayOfMonth: Int? = null, // For monthly repeats
    val endDate: LocalDate? = null // Optional end date
)

enum class RepeatType {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM
}
```

#### UI Components:
- Repeat type selector (None, Daily, Weekly, Monthly, Yearly, Custom)
- Interval selector (Every X days/weeks/months)
- Day of week selector (for weekly repeats)
- End date picker (optional)
- Preview text showing next few occurrences

### 3. Enhanced Alert Types
**Goal**: Provide multiple notification methods beyond basic notifications

#### Basic Alert Types (Phase 1):
1. **Notification Only**
   - Standard Android notification
   - Default sound
   - No vibration or flash

2. **Notification + Vibration**
   - Standard notification
   - Configurable vibration pattern
   - Single series by default

3. **Notification + Sound**
   - Standard notification
   - Configurable sound volume
   - Default notification sound

4. **Full Alert** (Current enhanced version)
   - Notification
   - Vibration pattern
   - Sound
   - Screen flash (if enabled)

#### Advanced Alert Types (Phase 2 - Future):
1. **Custom Sound Selection**
   - Allow users to pick from phone sounds
   - Record custom sounds
   - Different sounds for different priorities

2. **Escalating Alerts**
   - Start gentle, increase intensity
   - Multiple notification attempts
   - User acknowledgment required

### 4. Vibration Configuration
**Goal**: Fine-tuned control over vibration patterns

#### Implementation Plan:
```kotlin
data class VibrationConfig(
    val enabled: Boolean = true,
    val pattern: VibrationPattern = VibrationPattern.SINGLE,
    val intensity: VibrationIntensity = VibrationIntensity.MEDIUM,
    val seriesCount: Int = 1,
    val seriesInterval: Int = 1000 // ms between series
)

enum class VibrationPattern {
    SINGLE,      // One short vibration
    DOUBLE,       // Two short vibrations
    TRIPLE,       // Three short vibrations
    LONG,         // One long vibration
    PULSE,        // Pulsing pattern
    CUSTOM        // User-defined pattern
}

enum class VibrationIntensity {
    LIGHT, MEDIUM, STRONG
}
```

#### UI Components:
- Enable/disable vibration toggle
- Pattern selector with preview
- Intensity slider
- Series count selector (1-5)
- Series interval selector (0.5-5 seconds)

### 5. Sound Configuration
**Goal**: Control over notification sounds

#### Implementation Plan:
```kotlin
data class SoundConfig(
    val enabled: Boolean = true,
    val type: SoundType = SoundType.DEFAULT,
    val volume: Float = 0.8f,
    val seriesCount: Int = 1,
    val seriesInterval: Int = 2000, // ms between series
    val customSoundUri: String? = null // Future feature
)

enum class SoundType {
    DEFAULT,      // System default notification
    ALARM,        // System alarm sound
    GENTLE,       // Soft notification sound
    URGENT,       // Loud attention sound
    CUSTOM         // User-selected sound (future)
}
```

#### UI Components:
- Enable/disable sound toggle
- Sound type selector with preview
- Volume slider
- Series count selector (1-3)
- Series interval selector (1-10 seconds)

### 6. Alert Series Configuration
**Goal**: Control how alerts repeat over time

#### Implementation Plan:
```kotlin
data class AlertSeries(
    val enabled: Boolean = false,
    val maxAttempts: Int = 3,
    val intervalMinutes: Int = 5,
    val escalationEnabled: Boolean = true,
    val stopOnAcknowledge: Boolean = true
)
```

#### UI Components:
- Enable/disable series toggle
- Max attempts selector (1-10)
- Interval between attempts (1-60 minutes)
- Escalation toggle (increase intensity over time)
- Stop on acknowledge toggle

## Implementation Phases

### Phase 1: Core Enhancements (Current Sprint)
1. **Alert Settings UI Improvements**
   - Collapsible section with smooth animation
   - Better visual hierarchy
   - Natural scrolling

2. **Basic Repeating Reminders**
   - Daily, Weekly, Monthly, Yearly options
   - Interval configuration
   - End date option

3. **Enhanced Alert Types**
   - Basic 4 alert types
   - Simple configuration UI

4. **Vibration Configuration**
   - Pattern selection
   - Intensity control
   - Series configuration

5. **Sound Configuration**
   - Type selection
   - Volume control
   - Series configuration

### Phase 2: Advanced Features (Future Sprint)
1. **Custom Sound Selection**
   - Browse phone sounds
   - Sound preview
   - Per-reminder sound assignment

2. **Escalating Alerts**
   - Progressive intensity increase
   - Multiple notification types
   - Smart escalation logic

3. **Smart Alert Profiles**
   - Pre-configured alert sets
   - Time-based profiles
   - Location-based profiles

## Technical Implementation Details

### Database Schema Changes
```sql
-- Add to reminders table
ALTER TABLE reminders ADD COLUMN repeat_pattern TEXT; -- JSON for RepeatPattern
ALTER TABLE reminders ADD COLUMN alert_config TEXT; -- JSON for AlertConfig

-- New tables for future features
CREATE TABLE alert_profiles (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    config TEXT NOT NULL, -- JSON for AlertConfig
    created_at INTEGER NOT NULL
);
```

### Data Structures
```kotlin
data class AlertConfig(
    val alertType: AlertType = AlertType.NOTIFICATION_ONLY,
    val vibration: VibrationConfig = VibrationConfig(),
    val sound: SoundConfig = SoundConfig(),
    val series: AlertSeries = AlertSeries()
)

enum class AlertType {
    NOTIFICATION_ONLY,
    NOTIFICATION_VIBRATION,
    NOTIFICATION_SOUND,
    FULL_ALERT
}
```

### UI Components Structure
```
InputScreen
├── Alert Settings Section (Collapsible)
│   ├── Alert Type Selector
│   ├── Repeat Configuration
│   │   ├── Repeat Type
│   │   ├── Interval
│   │   └── End Date
│   ├── Vibration Configuration
│   │   ├── Enable Toggle
│   │   ├── Pattern Selector
│   │   ├── Intensity Slider
│   │   └── Series Configuration
│   ├── Sound Configuration
│   │   ├── Enable Toggle
│   │   ├── Type Selector
│   │   ├── Volume Slider
│   │   └── Series Configuration
│   └── Series Configuration
│       ├── Max Attempts
│       ├── Interval
│       └── Escalation Settings
```

## User Experience Considerations

### Simplicity First
- Default settings work well for most users
- Advanced options hidden behind expandable sections
- Clear preview of what each setting does

### Progressive Disclosure
- Show basic options first
- Reveal advanced options as needed
- Provide helpful tooltips and descriptions

### Visual Feedback
- Real-time preview of vibration patterns
- Sound preview for different alert types
- Visual indicators for active settings

### Accessibility
- High contrast support
- Screen reader compatibility
- Alternative input methods

## Testing Strategy

### Unit Tests
- Data structure serialization/deserialization
- Repeat pattern calculations
- Alert configuration validation

### Integration Tests
- Database migration
- Notification scheduling
- Alert series execution

### UI Tests
- Expandable section behavior
- Slider interactions
- Configuration persistence

### User Testing
- Ease of use for basic configurations
- Discoverability of advanced features
- Overall satisfaction with alert options

## Success Metrics

### Technical Metrics
- Zero crashes related to alert configuration
- Reliable notification delivery
- Accurate repeat scheduling

### User Experience Metrics
- Time to configure basic reminder (< 30 seconds)
- Discovery rate of advanced features (> 20%)
- User satisfaction with alert options (> 4/5)

### Performance Metrics
- App startup time impact (< 100ms)
- Memory usage increase (< 5MB)
- Battery impact minimal (< 2% per day)

## Risks and Mitigations

### Technical Risks
1. **Complexity Creep**
   - Mitigation: Phase-based implementation, regular reviews
   
2. **Performance Impact**
   - Mitigation: Efficient data structures, background processing
   
3. **Compatibility Issues**
   - Mitigation: Broad device testing, fallback options

### User Experience Risks
1. **Option Overwhelm**
   - Mitigation: Smart defaults, progressive disclosure
   
2. **Discovery Issues**
   - Mitigation: Clear visual cues, helpful tooltips
   
3. **Configuration Errors**
   - Mitigation: Input validation, clear error messages

## Timeline

### Phase 1 (Current Sprint - 2 weeks)
- Week 1: UI improvements and basic repeating
- Week 2: Alert types and vibration/sound config

### Phase 2 (Future Sprint - 3 weeks)
- Week 1: Custom sound selection
- Week 2: Escalating alerts
- Week 3: Smart profiles and polish

## Conclusion

This enhancement plan provides a comprehensive approach to improving the reminder app's alert system while maintaining simplicity for basic users and offering powerful options for advanced users. The phased implementation ensures we can deliver value quickly while building toward a more feature-rich solution.