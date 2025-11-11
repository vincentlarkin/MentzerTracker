package com.example.mentzertracker

// Core domain models for workouts

data class Exercise(
    val id: String,
    val name: String
)

data class WorkoutTemplate(
    val id: String,        // e.g. "A" or "B"
    val name: String,      // e.g. "Workout A"
    val exerciseIds: List<String>
)

data class ExerciseSetEntry(
    val exerciseId: String,
    val weight: Float,
    val reps: Int
)

data class WorkoutLogEntry(
    val id: Long,              // timestamp
    val templateId: String,    // "A" or "B"
    val date: String,
    val sets: List<ExerciseSetEntry>
)

// User-configurable A/B setup, stored in SharedPreferences
data class UserWorkoutConfig(
    val workoutAExerciseIds: List<String>,
    val workoutBExerciseIds: List<String>
)

// All exercises your app knows about.
// Add/remove/edit here without touching the UI code.
val allExercises = listOf(
    Exercise("squat", "Smith Squat"),
    Exercise("deadlift", "Deadlift"),
    Exercise("pulldown", "Close-Grip Palm-Up Pulldown"),
    Exercise("incline_press", "Incline Press"),
    Exercise("dips", "Dips"),
    Exercise("bench_press", "Flat Bench Press"),
    Exercise("ohp", "Overhead Press"),
    Exercise("row", "Barbell Row"),
    Exercise("leg_press", "Leg Press"),
    Exercise("calf_raise", "Standing Calf Raise")
)

// Default A/B config used on first launch
val defaultWorkoutConfig = UserWorkoutConfig(
    workoutAExerciseIds = listOf(
        "squat",
        "pulldown",
        "incline_press"
    ),
    workoutBExerciseIds = listOf(
        "deadlift",
        "row",
        "dips",
        "calf_raise"
    )
)
