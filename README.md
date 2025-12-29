# MentzerTracker

A flexible workout logging app for Android, with optional A/B workout tracking inspired by Mike Mentzer's High Intensity Training.

## Features
- **Natural language logging** - Type "bench 225 x 8" and it just works
- **70+ built-in exercises** with smart aliases (bench, flat, bb bench all work)
- **Progress tracking** with line charts and history
- **Customizable workout intervals** - Daily, weekly, or custom
- **Light/dark themes**
- **Backup & restore**

## Screenshots

<p align="center">
  <img src="docs/images/home.png" alt="Home screen" width="260">
  <img src="docs/images/progress.png" alt="Progress view" width="260">
  <img src="docs/images/builder.png" alt="Workout builder" width="260">
</p>

## Quick Start

1. Clone and open in Android Studio
2. Sync Gradle & run on Android 11+

## Code Structure
- `novanotes/` - Main UI screens (Home, Builder, Progress)
- `WorkoutParser.kt` - Natural language exercise parsing
- `WorkoutData.kt` - Exercise definitions and data models
- `NotificationHelper.kt` - Reminder scheduling

## License
Personal project - feel free to fork and modify.
