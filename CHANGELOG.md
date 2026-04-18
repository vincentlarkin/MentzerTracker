# Changelog

All notable changes to this project are documented in this file.

## 2.5.1 - 2026-04-17

- Fixed workout date editing so selected dates stay on the exact day the user picked.
- Fixed schedule freshness so overdue status follows the latest logged workout date, not insertion order.
- Added backlog date selection to the main logging screen, defaulting to today.
- Cleaned up corrupted UI text in progress, logging, and shortcut surfaces.
- Improved log refresh behavior after edits and sorted backfilled saves more consistently.
- Bumped app version:
  - `versionCode`: 11
  - `versionName`: `2.5.1`

## 2.5 - 2026-03-13

- Added a much larger built-in exercise catalog, with expanded ab/core coverage and more machine options.
- Added first-class cardio logging for treadmill, elliptical, stair, and bike-style entries.
- Added flexible cardio parsing for time, distance, calories, and steps.
- Improved core parsing for reps-only movements and timed holds like planks.
- Updated progress history so cardio entries show cleanly in by-date summaries without polluting strength charts.
- Cleaned up deprecated Android Gradle configuration flags and bumped app version:
  - `versionCode`: 10
  - `versionName`: `2.5`

## 2.3 - 2026-02-23

- Added a new Progress dropdown mode, `See By Date`, to group workouts by date.
- Added daily focus labels (for example, Legs/Back/Mixed) inferred from logged exercises.
- Improved by-date UI with centered date dividers, focus labels, and cleaner exercise cards.
- Kept the all-exercises line chart available while browsing by-date history.
- Bumped app version to support upgrade detection:
  - `versionCode`: 9
  - `versionName`: `2.3`
