# It Really Worked — Android APK Test

This repository is a minimal Android app used to verify that we can build an installable **.apk entirely through GitHub Actions**, without needing Android Studio or a PC.

## What the app does

The app intentionally does only one thing:

**Displays:** It Really Worked

## Build process

1. Android source code is stored in this repository.
2. GitHub Actions runs the Android/Gradle build in the cloud.
3. A debug APK is generated at app/build/outputs/apk/debug/app-debug.apk.
4. The workflow uploads the APK as a GitHub Actions artifact.
5. The APK can then be downloaded to an Android phone and installed for testing.

## Project

- **Application ID:** com.example.itreallyworked
- **Version:** 1.0
- **Minimum Android:** API 23
- **Target Android:** API 35
- **Build:** Debug APK
- **Build system:** Gradle + Android Gradle Plugin
- **Automation:** GitHub Actions

## Purpose

This is a proof-of-concept for a **phone-only Android development workflow**: write/edit code on a phone, let GitHub's cloud runner compile it, and obtain an installable APK.

The next step is to verify the workflow and download/install the generated APK.
