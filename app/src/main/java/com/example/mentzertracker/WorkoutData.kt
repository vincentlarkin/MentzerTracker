package com.vincentlarkin.mentzertracker

import java.util.Locale

// Core domain models for workouts

enum class ExerciseTrackingMode {
    STRENGTH,
    CARDIO
}

data class Exercise(
    val id: String,
    val name: String,
    val aliases: List<String>? = null,
    val trackingMode: ExerciseTrackingMode? = null
)

data class ExerciseSetEntry(
    val exerciseId: String,
    val weight: Float = 0f,
    val reps: Int = 0,
    val durationMinutes: Float? = null,
    val distanceMiles: Float? = null,
    val calories: Int? = null,
    val steps: Int? = null
)

data class WorkoutLogEntry(
    val id: Long,              // timestamp
    val templateId: String,    // workout label (e.g., "TODAY", "A", "B")
    val date: String,
    val sets: List<ExerciseSetEntry>,
    val notes: String? = null
)

// User workout setup, stored in SharedPreferences

data class UserWorkoutConfig(
    val workoutAExerciseIds: List<String>,
    val workoutBExerciseIds: List<String>,
    val customExercises: List<Exercise> = emptyList()
)

val Exercise.resolvedTrackingMode: ExerciseTrackingMode
    get() = trackingMode ?: ExerciseTrackingMode.STRENGTH

val Exercise.isCardio: Boolean
    get() = resolvedTrackingMode == ExerciseTrackingMode.CARDIO

fun ExerciseSetEntry.hasCardioMetrics(): Boolean {
    return durationMinutes != null || distanceMiles != null || calories != null || steps != null
}

fun ExerciseSetEntry.hasStrengthMetrics(): Boolean {
    return weight > 0f || reps > 0
}

fun ExerciseSetEntry.isCardioEntry(exercise: Exercise? = null): Boolean {
    return exercise?.isCardio == true || hasCardioMetrics()
}

fun formatExerciseEntrySummary(
    entry: ExerciseSetEntry,
    exercise: Exercise? = null,
    separator: String = " Â· "
): String {
    return if (entry.isCardioEntry(exercise)) {
        formatCardioEntry(entry, separator)
    } else {
        val repsLabel = "${entry.reps} rep" + if (entry.reps == 1) "" else "s"
        "${formatWeightValue(entry.weight)} x $repsLabel"
    }
}

fun formatExerciseEntryCompact(entry: ExerciseSetEntry, exercise: Exercise? = null): String {
    return if (entry.isCardioEntry(exercise)) {
        formatCardioEntry(entry, " Â· ")
    } else {
        "${formatWeightValue(entry.weight, includeUnit = false)}x${entry.reps}"
    }
}

private fun formatCardioEntry(entry: ExerciseSetEntry, separator: String): String {
    val parts = buildList {
        entry.durationMinutes?.let { add(formatDurationMinutes(it)) }
        entry.distanceMiles?.let { add("${formatDecimal(it)} mi") }
        entry.calories?.let { add("${formatWholeNumber(it)} cal") }
        entry.steps?.let {
            add(
                "${formatWholeNumber(it)} step" + if (it == 1) "" else "s"
            )
        }
    }
    return parts.joinToString(separator).ifBlank { "Cardio entry" }
}

fun formatWeightValue(weight: Float, includeUnit: Boolean = true): String {
    val value = if (weight % 1f == 0f) {
        weight.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", weight)
    }
    return if (includeUnit) "$value lbs" else value
}

fun formatDurationMinutes(durationMinutes: Float): String {
    if (durationMinutes <= 0f) return "0 min"
    val wholeMinutes = durationMinutes.toInt()
    val hasFractionalMinute = durationMinutes % 1f != 0f
    if (durationMinutes >= 60f && !hasFractionalMinute) {
        val hours = wholeMinutes / 60
        val minutes = wholeMinutes % 60
        return when {
            minutes == 0 -> "$hours hr"
            else -> "$hours hr $minutes min"
        }
    }
    return "${formatDecimal(durationMinutes)} min"
}

private fun formatDecimal(value: Float): String {
    return if (value % 1f == 0f) {
        value.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
}

private fun formatWholeNumber(value: Int): String {
    return String.format(Locale.getDefault(), "%,d", value)
}

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
    Exercise("machine_fly", "Machine Fly"),
    Exercise("push_ups", "Push Ups"),
    Exercise("dips", "Dips"),
    Exercise("seated_chest_press", "Seated Chest Press"),
    Exercise("incline_chest_press_machine", "Incline Chest Press Machine"),
    Exercise("decline_chest_press_machine", "Decline Chest Press Machine"),
    Exercise("smith_incline_press", "Smith Machine Incline Press"),

    // === BACK ===
    Exercise("deadlift", "Deadlift"),
    Exercise("row", "Barbell Row"),
    Exercise("dumbbell_row", "Dumbbell Row"),
    Exercise("cable_row", "Cable Row"),
    Exercise("seated_row", "Seated Row"),
    Exercise("pulldown", "Lat Pulldown"),
    Exercise("close_grip_pulldown", "Close-Grip Lat Pulldown"),
    Exercise("pull_ups", "Pull Ups"),
    Exercise("chin_ups", "Chin Ups"),
    Exercise("t_bar_row", "T-Bar Row"),
    Exercise("shrugs", "Shrugs"),
    Exercise("face_pulls", "Face Pulls"),
    Exercise("lat_prayer", "Lat Prayer"),
    Exercise("straight_arm_pulldown", "Straight Arm Pulldown"),
    Exercise("machine_row", "Machine Row"),
    Exercise("high_row", "High Row"),
    Exercise("low_row", "Low Row"),
    Exercise("assisted_pull_up", "Assisted Pull Up"),
    Exercise("pullover_machine", "Pullover Machine"),

    // === SHOULDERS ===
    Exercise("ohp", "Overhead Press"),
    Exercise("dumbbell_shoulder_press", "Dumbbell Shoulder Press"),
    Exercise("arnold_press", "Arnold Press"),
    Exercise("lateral_raise", "Lateral Raise"),
    Exercise("front_raise", "Front Raise"),
    Exercise("rear_delt_fly", "Rear Delt Fly"),
    Exercise("upright_row", "Upright Row"),
    Exercise("machine_shoulder_press", "Machine Shoulder Press"),
    Exercise("machine_lateral_raise", "Machine Lateral Raise"),
    Exercise("rear_delt_machine", "Rear Delt Machine"),

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
    Exercise("smith_lunge", "Smith Machine Lunge"),
    Exercise("seated_leg_press", "Seated Leg Press"),
    Exercise("adductor_machine", "Adductor Machine"),
    Exercise("abductor_machine", "Abductor Machine"),
    Exercise("glute_drive", "Glute Drive"),
    Exercise("glute_kickback", "Glute Kickback"),
    Exercise("hack_squat_machine", "Hack Squat Machine"),

    // === ARMS - BICEPS ===
    Exercise("bicep_curl", "Bicep Curl"),
    Exercise("hammer_curl", "Hammer Curl"),
    Exercise("preacher_curl", "Preacher Curl"),
    Exercise("concentration_curl", "Concentration Curl"),
    Exercise("cable_curl", "Cable Curl"),
    Exercise("incline_curl", "Incline Curl"),
    Exercise("ez_bar_curl", "EZ Bar Curl"),
    Exercise("machine_bicep_curl", "Machine Bicep Curl"),

    // === ARMS - TRICEPS ===
    Exercise("tricep_pushdown", "Tricep Pushdown"),
    Exercise("rope_pushdown", "Rope Pushdown"),
    Exercise("skull_crushers", "Skull Crushers"),
    Exercise("overhead_extension", "Overhead Extension"),
    Exercise("close_grip_bench", "Close Grip Bench"),
    Exercise("tricep_kickback", "Tricep Kickback"),
    Exercise("assisted_dip", "Assisted Dip"),
    Exercise("machine_tricep_extension", "Machine Tricep Extension"),

    // === CORE ===
    Exercise("crunches", "Crunches", aliases = listOf("crunch", "crunchs", "ab crunch")),
    Exercise("decline_crunch", "Decline Crunch"),
    Exercise("machine_crunch", "Machine Crunch"),
    Exercise("reverse_crunch", "Reverse Crunch"),
    Exercise("bicycle_crunch", "Bicycle Crunch"),
    Exercise("oblique_crunch", "Oblique Crunch"),
    Exercise("leg_raises", "Leg Raises"),
    Exercise("hanging_leg_raise", "Hanging Leg Raise"),
    Exercise("hanging_knee_raise", "Hanging Knee Raise"),
    Exercise("captains_chair_raise", "Captain's Chair Leg Raise"),
    Exercise("plank", "Plank"),
    Exercise("side_plank", "Side Plank"),
    Exercise("hollow_hold", "Hollow Hold"),
    Exercise("cable_crunch", "Cable Crunch"),
    Exercise("russian_twist", "Russian Twist"),
    Exercise("ab_wheel", "Ab Wheel"),
    Exercise("sit_ups", "Sit Ups"),
    Exercise("v_up", "V Up"),
    Exercise("toe_touch", "Toe Touch"),
    Exercise("flutter_kick", "Flutter Kick"),
    Exercise("scissor_kick", "Scissor Kick"),
    Exercise("dead_bug", "Dead Bug"),
    Exercise("mountain_climber", "Mountain Climber"),
    Exercise("wood_chop", "Wood Chop"),
    Exercise("cable_wood_chop", "Cable Wood Chop"),
    Exercise("pallof_press", "Pallof Press"),
    Exercise("torso_rotation", "Torso Rotation"),
    Exercise("back_extension", "Back Extension"),

    // === CARDIO ===
    Exercise(
        "treadmill",
        "Treadmill",
        aliases = listOf("tread", "run", "jog", "walk"),
        trackingMode = ExerciseTrackingMode.CARDIO
    ),
    Exercise(
        "elliptical",
        "Elliptical",
        aliases = listOf("ellip", "ell", "elliptical machine", "eliptical", "eleptical"),
        trackingMode = ExerciseTrackingMode.CARDIO
    ),
    Exercise(
        "stair_climber",
        "Stair Climber",
        aliases = listOf("stairs", "stairmaster", "stepmill"),
        trackingMode = ExerciseTrackingMode.CARDIO
    ),
    Exercise(
        "upright_bike",
        "Upright Bike",
        aliases = listOf("bike", "stationary bike"),
        trackingMode = ExerciseTrackingMode.CARDIO
    ),
    Exercise(
        "recumbent_bike",
        "Recumbent Bike",
        aliases = listOf("recumbent"),
        trackingMode = ExerciseTrackingMode.CARDIO
    ),

    // === MACHINES ===
    Exercise("chest_press_machine", "Chest Press Machine"),
    Exercise("shoulder_press_machine", "Shoulder Press Machine"),
    Exercise("smith_squat", "Smith Machine Squat"),
    Exercise("smith_bench", "Smith Machine Bench"),
    Exercise("cable_crossover", "Cable Crossover")
)

// Default workout config used on first launch

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
