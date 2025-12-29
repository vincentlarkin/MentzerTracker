package com.vincentlarkin.mentzertracker

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
    val sets: List<ExerciseSetEntry>,
    val notes: String? = null
)

// User-configurable A/B setup, stored in SharedPreferences
data class UserWorkoutConfig(
    val workoutAExerciseIds: List<String>,
    val workoutBExerciseIds: List<String>,
    val customExercises: List<Exercise> = emptyList()
)

// All exercises your app knows about - comprehensive list of common gym movements
val allExercises = listOf(
    // === CHEST ===
    Exercise("bench_press", "Flat Bench Press"),
    Exercise("incline_press", "Incline Press"),
    Exercise("decline_press", "Decline Press"),
    Exercise("dumbbell_press", "Dumbbell Press"),
    Exercise("incline_db_press", "Incline Dumbbell Press"),
    Exercise("chest_fly", "Chest Fly"),
    Exercise("cable_fly", "Cable Fly"),
    Exercise("pec_deck", "Pec Deck"),
    Exercise("push_ups", "Push Ups"),
    Exercise("dips", "Dips"),
    
    // === BACK ===
    Exercise("deadlift", "Deadlift"),
    Exercise("row", "Barbell Row"),
    Exercise("dumbbell_row", "Dumbbell Row"),
    Exercise("cable_row", "Cable Row"),
    Exercise("seated_row", "Seated Row"),
    Exercise("pulldown", "Lat Pulldown"),
    Exercise("close_grip_pulldown", "Close-Grip Pulldown"),
    Exercise("pull_ups", "Pull Ups"),
    Exercise("chin_ups", "Chin Ups"),
    Exercise("t_bar_row", "T-Bar Row"),
    Exercise("shrugs", "Shrugs"),
    Exercise("face_pulls", "Face Pulls"),
    Exercise("lat_prayer", "Lat Prayer"),
    Exercise("straight_arm_pulldown", "Straight Arm Pulldown"),
    
    // === SHOULDERS ===
    Exercise("ohp", "Overhead Press"),
    Exercise("dumbbell_shoulder_press", "Dumbbell Shoulder Press"),
    Exercise("arnold_press", "Arnold Press"),
    Exercise("lateral_raise", "Lateral Raise"),
    Exercise("front_raise", "Front Raise"),
    Exercise("rear_delt_fly", "Rear Delt Fly"),
    Exercise("upright_row", "Upright Row"),
    
    // === LEGS ===
    Exercise("squat", "Squat"),
    Exercise("front_squat", "Front Squat"),
    Exercise("hack_squat", "Hack Squat"),
    Exercise("leg_press", "Leg Press"),
    Exercise("lunges", "Lunges"),
    Exercise("leg_extension", "Leg Extension"),
    Exercise("leg_curl", "Leg Curl"),
    Exercise("romanian_deadlift", "Romanian Deadlift"),
    Exercise("stiff_leg_deadlift", "Stiff Leg Deadlift"),
    Exercise("hip_thrust", "Hip Thrust"),
    Exercise("glute_bridge", "Glute Bridge"),
    Exercise("calf_raise", "Calf Raise"),
    Exercise("seated_calf", "Seated Calf Raise"),
    Exercise("goblet_squat", "Goblet Squat"),
    Exercise("bulgarian_split_squat", "Bulgarian Split Squat"),
    
    // === ARMS - BICEPS ===
    Exercise("bicep_curl", "Bicep Curl"),
    Exercise("hammer_curl", "Hammer Curl"),
    Exercise("preacher_curl", "Preacher Curl"),
    Exercise("concentration_curl", "Concentration Curl"),
    Exercise("cable_curl", "Cable Curl"),
    Exercise("incline_curl", "Incline Curl"),
    Exercise("ez_bar_curl", "EZ Bar Curl"),
    
    // === ARMS - TRICEPS ===
    Exercise("tricep_pushdown", "Tricep Pushdown"),
    Exercise("rope_pushdown", "Rope Pushdown"),
    Exercise("skull_crushers", "Skull Crushers"),
    Exercise("overhead_extension", "Overhead Extension"),
    Exercise("close_grip_bench", "Close Grip Bench"),
    Exercise("tricep_kickback", "Tricep Kickback"),
    
    // === CORE ===
    Exercise("crunches", "Crunches"),
    Exercise("leg_raises", "Leg Raises"),
    Exercise("hanging_leg_raise", "Hanging Leg Raise"),
    Exercise("plank", "Plank"),
    Exercise("cable_crunch", "Cable Crunch"),
    Exercise("russian_twist", "Russian Twist"),
    Exercise("ab_wheel", "Ab Wheel"),
    Exercise("sit_ups", "Sit Ups"),
    
    // === MACHINES ===
    Exercise("chest_press_machine", "Chest Press Machine"),
    Exercise("shoulder_press_machine", "Shoulder Press Machine"),
    Exercise("smith_squat", "Smith Machine Squat"),
    Exercise("smith_bench", "Smith Machine Bench"),
    Exercise("cable_crossover", "Cable Crossover")
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
