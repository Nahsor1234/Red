# JeE — JEE 2027 Command Center

A personal, offline-first Android command center for JEE 2027 preparation.

## Current stack

- Kotlin
- Jetpack Compose + Material 3
- Android Gradle Plugin 8.6.1
- Gradle 8.7
- Kotlin 2.0.20
- compileSdk / targetSdk 35
- minSdk 23
- Java 17
- SharedPreferences + JSON for the current local data layer

## Current functionality

- Dashboard driven by stored study/task data
- Chapter progress tracking
- Task creation, completion, deletion, and persistence
- Study timer with presets
- Completed study-session recording
- Daily study-goal tracking
- Local persistence across app restarts
- Haptic interaction feedback
- Bottom navigation

## Build

The app is built through GitHub Actions and produces the debug APK at:
app/build/outputs/apk/debug/app-debug.apk

The current workflow is intentionally simple:

Phone → GitHub → GitHub Actions → Gradle/Android SDK → APK → phone

## Product direction

JeE is intended to become a real JEE preparation command center rather than a generic productivity app.

Planned domain systems include:

- richer syllabus states
- revision scheduling
- mistake bank
- test tracking
- actionable analytics
- justified reminders/notifications

The app remains offline-first unless a strong product reason justifies cloud functionality.

## Important data rule

Dashboard values must come from actual stored data. The application does not intentionally seed fake/demo tasks or study metrics.
