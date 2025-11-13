# Android Reminder App

A complete Android reminder app with voice input, local storage, and notifications.

## Features
- Voice and text input for reminders
- Categories (Work, Family, Personal) 
- Importance levels (1-10)
- Local storage with Room database
- Timely notifications with AlarmManager
- Export/Import functionality (CSV/JSON)
- Material Design 3 UI
- Calendar integration
- Smart voice processing
- Email notifications
- Screen flash notifications

## Requirements
- Android Studio Arctic Fox or later
- Android SDK API 24+
- Java 11+

## Build
```bash
./gradlew build
```

## Project Structure
- `app/src/main/java/com/reminder/app/` - Main source code
- `app/src/main/res/` - Android resources
- `app/build.gradle` - App-level build configuration

## Key Components
- `Reminder.kt` - Data entity
- `ReminderDao.kt` - Database access
- `ReminderViewModel.kt` - Business logic
- `SpeechManager.kt` - Voice input/output
- `NotificationScheduler.kt` - Alarm notifications
- `CalendarScreen.kt` - Calendar interface
- `SmartVoiceProcessor.kt` - Advanced voice processing