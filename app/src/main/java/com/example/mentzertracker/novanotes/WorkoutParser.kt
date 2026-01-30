package com.vincentlarkin.mentzertracker.novanotes

import com.vincentlarkin.mentzertracker.Exercise
import com.vincentlarkin.mentzertracker.ExerciseSetEntry
import com.vincentlarkin.mentzertracker.allExercises
import java.util.Locale

/**
 * Parsed result from a single line of workout text.
 * Can represent multiple sets of the same exercise.
 */
data class ParsedExercise(
    val exercise: Exercise,
    val sets: List<ParsedSet>,
    val rawText: String
)

data class ParsedSet(
    val weight: Float,
    val reps: Int
)

data class ParseResult(
    val parsedExercises: List<ParsedExercise>,
    val unrecognizedLines: List<String>
)

/**
 * Intelligent workout parser that converts natural language input
 * into structured workout data.
 * 
 * Supported formats:
 * - "bench 225 - 8, 8, 6" → Bench press: 225lbs x 8, x8, x6 reps
 * - "squat 3x5 @ 315" → Squat: 3 sets of 5 reps at 315lbs
 * - "deadlift 405 x 3" → Deadlift: 405lbs x 3 reps
 * - "ohp 135 8" → Overhead press: 135lbs x 8 reps
 * - "pulldown 150 12, 10, 8" → Pulldown: 150lbs x 12, x10, x8
 * - "row 185 4x8" → Row: 4 sets of 8 reps at 185lbs
 */
object WorkoutParser {
    
    // Comprehensive aliases for exercise name variations
    // Maps shorthand/common terms to exercise IDs defined in WorkoutData.kt
    private val exerciseAliases = mapOf(
        // === CHEST ===
        "bench" to "bench_press",
        "bench press" to "bench_press",
        "flat bench" to "bench_press",
        "flat" to "bench_press",
        "barbell bench" to "bench_press",
        "bb bench" to "bench_press",
        
        "incline" to "incline_press",
        "incline press" to "incline_press",
        "incline bench" to "incline_press",
        "incline bb" to "incline_press",
        
        "decline" to "decline_press",
        "decline press" to "decline_press",
        "decline bench" to "decline_press",
        
        "db press" to "dumbbell_press",
        "dumbbell press" to "dumbbell_press",
        "dumbbell bench" to "dumbbell_press",
        
        "incline db" to "incline_db_press",
        "incline dumbbell" to "incline_db_press",
        "incline db press" to "incline_db_press",
        
        "fly" to "chest_fly",
        "flys" to "chest_fly",
        "flies" to "chest_fly",
        "chest fly" to "chest_fly",
        "db fly" to "chest_fly",
        "dumbbell fly" to "chest_fly",
        
        "cable fly" to "cable_fly",
        "cable flys" to "cable_fly",
        "cable flies" to "cable_fly",
        
        "pec deck" to "pec_deck",
        "pec" to "pec_deck",
        "peck deck" to "pec_deck",
        
        "push up" to "push_ups",
        "push ups" to "push_ups",
        "pushup" to "push_ups",
        "pushups" to "push_ups",
        
        "dips" to "dips",
        "dip" to "dips",
        "chest dips" to "dips",
        "tricep dips" to "dips",
        
        // === BACK ===
        "deadlift" to "deadlift",
        "dl" to "deadlift",
        "dead" to "deadlift",
        "deads" to "deadlift",
        "conventional" to "deadlift",
        
        "row" to "row",
        "rows" to "row",
        "barbell row" to "row",
        "bb row" to "row",
        "bent over row" to "row",
        "bent row" to "row",
        "pendlay" to "row",
        "pendlay row" to "row",
        
        "db row" to "dumbbell_row",
        "dumbbell row" to "dumbbell_row",
        "one arm row" to "dumbbell_row",
        "single arm row" to "dumbbell_row",
        
        "cable row" to "cable_row",
        
        "seated row" to "seated_row",
        "seated cable row" to "seated_row",
        "machine row" to "seated_row",
        
        "pulldown" to "pulldown",
        "pull down" to "pulldown",
        "lat pulldown" to "pulldown",
        "pulldowns" to "pulldown",
        "lat" to "pulldown",
        "lats" to "pulldown",
        "wide grip pulldown" to "pulldown",
        
        "close grip pulldown" to "close_grip_pulldown",
        "close pulldown" to "close_grip_pulldown",
        "close grip" to "close_grip_pulldown",
        "close-grip" to "close_grip_pulldown",
        "underhand pulldown" to "close_grip_pulldown",

        
        "pull up" to "pull_ups",
        "pull ups" to "pull_ups",
        "pullup" to "pull_ups",
        "pullups" to "pull_ups",
        
        "chin up" to "chin_ups",
        "chin ups" to "chin_ups",
        "chinup" to "chin_ups",
        "chinups" to "chin_ups",
        "chins" to "chin_ups",
        
        "t bar" to "t_bar_row",
        "t bar row" to "t_bar_row",
        "tbar" to "t_bar_row",
        
        "shrug" to "shrugs",
        "shrugs" to "shrugs",
        "barbell shrug" to "shrugs",
        "db shrug" to "shrugs",
        "trap" to "shrugs",
        "traps" to "shrugs",
        
        "face pull" to "face_pulls",
        "face pulls" to "face_pulls",
        "facepull" to "face_pulls",
        "facepulls" to "face_pulls",
        
        "lat prayer" to "lat_prayer",
        "prayer" to "lat_prayer",
        
        "straight arm" to "straight_arm_pulldown",
        "straight arm pulldown" to "straight_arm_pulldown",
        
        // === SHOULDERS ===
        "ohp" to "ohp",
        "overhead press" to "ohp",
        "overhead" to "ohp",
        "military press" to "ohp",
        "military" to "ohp",
        "press" to "ohp",
        "standing press" to "ohp",
        "barbell press" to "ohp",
        
        "shoulder press" to "dumbbell_shoulder_press",
        "db shoulder press" to "dumbbell_shoulder_press",
        "seated shoulder press" to "dumbbell_shoulder_press",
        "db ohp" to "dumbbell_shoulder_press",
        
        "arnold" to "arnold_press",
        "arnold press" to "arnold_press",
        "arnolds" to "arnold_press",
        
        "lateral raise" to "lateral_raise",
        "lateral" to "lateral_raise",
        "lat raise" to "lateral_raise",
        "side raise" to "lateral_raise",
        "side lateral" to "lateral_raise",
        "laterals" to "lateral_raise",
        
        "front raise" to "front_raise",
        "front" to "front_raise",
        
        "rear delt" to "rear_delt_fly",
        "rear delt fly" to "rear_delt_fly",
        "reverse fly" to "rear_delt_fly",
        "rear fly" to "rear_delt_fly",
        "rear delts" to "rear_delt_fly",
        
        "upright row" to "upright_row",
        "upright" to "upright_row",
        
        // === LEGS ===
        "squat" to "squat",
        "squats" to "squat",
        "back squat" to "squat",
        "barbell squat" to "squat",
        "bb squat" to "squat",
        
        "front squat" to "front_squat",
        "front squats" to "front_squat",
        
        "hack squat" to "hack_squat",
        "hack" to "hack_squat",
        
        "leg press" to "leg_press",
        "legpress" to "leg_press",
        
        "lunge" to "lunges",
        "lunges" to "lunges",
        "walking lunge" to "lunges",
        "walking lunges" to "lunges",
        "db lunge" to "lunges",
        
        "leg extension" to "leg_extension",
        "leg ext" to "leg_extension",
        "extensions" to "leg_extension",
        "quad extension" to "leg_extension",
        
        "leg curl" to "leg_curl",
        "hamstring curl" to "leg_curl",
        "lying leg curl" to "leg_curl",
        "seated leg curl" to "leg_curl",
        
        "rdl" to "romanian_deadlift",
        "romanian" to "romanian_deadlift",
        "romanian deadlift" to "romanian_deadlift",
        
        "sldl" to "stiff_leg_deadlift",
        "stiff leg" to "stiff_leg_deadlift",
        "stiff leg deadlift" to "stiff_leg_deadlift",
        
        "hip thrust" to "hip_thrust",
        "thrust" to "hip_thrust",
        "barbell hip thrust" to "hip_thrust",
        
        "glute bridge" to "glute_bridge",
        "bridge" to "glute_bridge",
        
        "calf raise" to "calf_raise",
        "calf raises" to "calf_raise",
        "calves" to "calf_raise",
        "calf" to "calf_raise",
        "standing calf" to "calf_raise",
        "standing calf raise" to "calf_raise",
        
        "seated calf" to "seated_calf",
        "seated calf raise" to "seated_calf",
        
        "goblet squat" to "goblet_squat",
        "goblet" to "goblet_squat",
        
        "bulgarian" to "bulgarian_split_squat",
        "bulgarian split squat" to "bulgarian_split_squat",
        "bss" to "bulgarian_split_squat",
        "split squat" to "bulgarian_split_squat",
        
        // === BICEPS ===
        "curl" to "bicep_curl",
        "curls" to "bicep_curl",
        "bicep curl" to "bicep_curl",
        "bicep curls" to "bicep_curl",
        "barbell curl" to "bicep_curl",
        "bb curl" to "bicep_curl",
        "db curl" to "bicep_curl",
        "dumbbell curl" to "bicep_curl",
        
        "hammer" to "hammer_curl",
        "hammer curl" to "hammer_curl",
        "hammer curls" to "hammer_curl",
        "hammers" to "hammer_curl",
        
        "preacher" to "preacher_curl",
        "preacher curl" to "preacher_curl",
        "preacher curls" to "preacher_curl",
        
        "concentration" to "concentration_curl",
        "concentration curl" to "concentration_curl",
        
        "cable curl" to "cable_curl",
        "cable curls" to "cable_curl",
        
        "incline curl" to "incline_curl",
        "incline curls" to "incline_curl",
        "incline db curl" to "incline_curl",
        
        "ez curl" to "ez_bar_curl",
        "ez bar curl" to "ez_bar_curl",
        "ez bar" to "ez_bar_curl",
        
        // === TRICEPS ===
        "pushdown" to "tricep_pushdown",
        "pushdowns" to "tricep_pushdown",
        "tricep pushdown" to "tricep_pushdown",
        "triceps pushdown" to "tricep_pushdown",
        "tri pushdown" to "tricep_pushdown",
        "cable pushdown" to "tricep_pushdown",
        
        "rope pushdown" to "rope_pushdown",
        "rope" to "rope_pushdown",
        "rope extension" to "rope_pushdown",
        
        "skull crusher" to "skull_crushers",
        "skull crushers" to "skull_crushers",
        "skulls" to "skull_crushers",
        "skullcrushers" to "skull_crushers",
        "lying tricep extension" to "skull_crushers",
        
        "overhead extension" to "overhead_extension",
        "overhead tricep" to "overhead_extension",
        "tricep overhead" to "overhead_extension",
        "french press" to "overhead_extension",
        
        "close grip bench" to "close_grip_bench",
        "cgbp" to "close_grip_bench",
        "close grip" to "close_grip_bench",
        
        "kickback" to "tricep_kickback",
        "kickbacks" to "tricep_kickback",
        "tricep kickback" to "tricep_kickback",
        
        // === CORE ===
        "crunch" to "crunches",
        "crunches" to "crunches",
        "ab crunch" to "crunches",
        
        "leg raise" to "leg_raises",
        "leg raises" to "leg_raises",
        "lying leg raise" to "leg_raises",
        
        "hanging leg raise" to "hanging_leg_raise",
        "hanging raise" to "hanging_leg_raise",
        "hanging" to "hanging_leg_raise",
        
        "plank" to "plank",
        "planks" to "plank",
        
        "cable crunch" to "cable_crunch",
        "cable crunches" to "cable_crunch",
        
        "russian twist" to "russian_twist",
        "russian twists" to "russian_twist",
        
        "ab wheel" to "ab_wheel",
        "wheel" to "ab_wheel",
        "rollout" to "ab_wheel",
        "rollouts" to "ab_wheel",
        
        "sit up" to "sit_ups",
        "sit ups" to "sit_ups",
        "situp" to "sit_ups",
        "situps" to "sit_ups",
        
        // === MACHINES ===
        "chest press" to "chest_press_machine",
        "chest press machine" to "chest_press_machine",
        "machine chest press" to "chest_press_machine",
        
        "shoulder press machine" to "shoulder_press_machine",
        "machine shoulder press" to "shoulder_press_machine",
        
        "smith" to "smith_squat",
        "smith squat" to "smith_squat",
        "smith machine squat" to "smith_squat",
        
        "smith bench" to "smith_bench",
        "smith machine bench" to "smith_bench",
        
        "crossover" to "cable_crossover",
        "cable crossover" to "cable_crossover",
        "crossovers" to "cable_crossover"
    )
    
    /**
     * Get a display-friendly mapping of aliases to exercise names.
     * Useful for showing users what shortcuts are available.
     */
    fun getAliasMap(availableExercises: List<Exercise>): Map<String, String> {
        val exercisesById = availableExercises.associateBy { it.id }
        val aliasLookup = buildAliasLookup(availableExercises)
        return aliasLookup.mapNotNull { (alias, id) ->
            exercisesById[id]?.let { exercise ->
                alias to exercise.name
            }
        }.toMap()
    }

    private fun normalizeAlias(alias: String): String {
        return alias.trim().lowercase(Locale.ROOT)
    }

    private fun buildAliasLookup(availableExercises: List<Exercise>): Map<String, String> {
        val exercisesById = availableExercises.associateBy { it.id }
        val combined = mutableMapOf<String, String>()
        exerciseAliases.forEach { (alias, id) ->
            if (exercisesById.containsKey(id)) {
                combined[alias] = id
            }
        }
        availableExercises.forEach { exercise ->
            exercise.aliases.orEmpty().forEach { alias ->
                val normalized = normalizeAlias(alias)
                if (normalized.isNotBlank() && !combined.containsKey(normalized)) {
                    combined[normalized] = exercise.id
                }
            }
        }
        return combined
    }
    
    /**
     * Parse multiline workout text into structured data.
     */
    fun parse(
        input: String,
        customExercises: List<Exercise> = emptyList()
    ): ParseResult {
        val lines = input.trim().lines().filter { it.isNotBlank() }
        val parsedExercises = mutableListOf<ParsedExercise>()
        val unrecognizedLines = mutableListOf<String>()
        
        val allAvailableExercises = allExercises + customExercises
        
        for (line in lines) {
            val parsed = parseLine(line.trim(), allAvailableExercises)
            if (parsed != null) {
                parsedExercises.add(parsed)
            } else {
                unrecognizedLines.add(line)
            }
        }
        
        return ParseResult(parsedExercises, unrecognizedLines)
    }
    
    /**
     * Parse a single line of workout text.
     */
    private fun parseLine(
        line: String,
        availableExercises: List<Exercise>
    ): ParsedExercise? {
        val normalizedLine = line.lowercase().trim()
        
        // Try to find exercise match
        var matchedExercise: Exercise? = null
        var remainingText = normalizedLine
        
        // First check aliases (longer matches first)
        val aliasLookup = buildAliasLookup(availableExercises)
        val sortedAliases = aliasLookup.keys.sortedByDescending { it.length }
        for (alias in sortedAliases) {
            if (normalizedLine.startsWith(alias)) {
                val exerciseId = aliasLookup[alias]
                matchedExercise = availableExercises.find { it.id == exerciseId }
                if (matchedExercise != null) {
                    remainingText = normalizedLine.removePrefix(alias).trim()
                    break
                }
            }
        }
        
        // Then check exercise names directly
        if (matchedExercise == null) {
            for (exercise in availableExercises.sortedByDescending { it.name.length }) {
                val lowerName = exercise.name.lowercase()
                if (normalizedLine.startsWith(lowerName)) {
                    matchedExercise = exercise
                    remainingText = normalizedLine.removePrefix(lowerName).trim()
                    break
                }
            }
        }
        
        // Check custom exercise aliases from name parts
        if (matchedExercise == null) {
            for (exercise in availableExercises) {
                val nameParts = exercise.name.lowercase().split(" ")
                for (part in nameParts) {
                    if (part.length >= 3 && normalizedLine.startsWith(part)) {
                        matchedExercise = exercise
                        remainingText = normalizedLine.removePrefix(part).trim()
                        break
                    }
                }
                if (matchedExercise != null) break
            }
        }
        
        if (matchedExercise == null) return null
        
        // Parse the numbers portion
        val sets = parseNumbers(remainingText)
        if (sets.isEmpty()) return null
        
        return ParsedExercise(
            exercise = matchedExercise,
            sets = sets,
            rawText = line
        )
    }
    
    /**
     * Parse the numbers portion of the workout text.
     * Supports various formats:
     * - "225 - 8, 8, 6" → weight 225, reps [8, 8, 6]
     * - "3x5 @ 315" → 3 sets of 5 at 315
     * - "405 x 3" → 405 x 3
     * - "135 8" → 135 x 8
     * - "150 12, 10, 8" → 150 x [12, 10, 8]
     * - "185 4x8" → 4 sets of 8 at 185
     */
    private fun parseNumbers(text: String): List<ParsedSet> {
        val cleaned = text
            .replace("-", " ")
            .replace("@", " ")
            .replace("×", "x")
            .replace("X", "x")
            .replace("lbs", "")
            .replace("lb", "")
            .replace("kg", "")
            .replace("reps", "")
            .replace("rep", "")
            .trim()
        
        if (cleaned.isEmpty()) return emptyList()
        
        // Try "NxM @ W" or "NxM W" format (sets x reps @ weight)
        val setsRepsWeightPattern = Regex("""(\d+)\s*x\s*(\d+)\s+(\d+(?:\.\d+)?)""")
        setsRepsWeightPattern.find(cleaned)?.let { match ->
            val numSets = match.groupValues[1].toIntOrNull() ?: return emptyList()
            val reps = match.groupValues[2].toIntOrNull() ?: return emptyList()
            val weight = match.groupValues[3].toFloatOrNull() ?: return emptyList()
            return List(numSets) { ParsedSet(weight, reps) }
        }
        
        // Try "W NxM" format (weight sets x reps)
        val weightSetsRepsPattern = Regex("""(\d+(?:\.\d+)?)\s+(\d+)\s*x\s*(\d+)""")
        weightSetsRepsPattern.find(cleaned)?.let { match ->
            val weight = match.groupValues[1].toFloatOrNull() ?: return emptyList()
            val numSets = match.groupValues[2].toIntOrNull() ?: return emptyList()
            val reps = match.groupValues[3].toIntOrNull() ?: return emptyList()
            return List(numSets) { ParsedSet(weight, reps) }
        }
        
        // Try "W x R" format (weight x reps, single set)
        val weightRepsPattern = Regex("""(\d+(?:\.\d+)?)\s*x\s*(\d+)""")
        weightRepsPattern.find(cleaned)?.let { match ->
            val weight = match.groupValues[1].toFloatOrNull() ?: return emptyList()
            val reps = match.groupValues[2].toIntOrNull() ?: return emptyList()
            return listOf(ParsedSet(weight, reps))
        }
        
        // Try "W R, R, R" format (weight followed by comma-separated reps)
        val weightMultiRepsPattern = Regex("""(\d+(?:\.\d+)?)\s+([\d\s,]+)""")
        weightMultiRepsPattern.find(cleaned)?.let { match ->
            val weight = match.groupValues[1].toFloatOrNull() ?: return emptyList()
            val repsStr = match.groupValues[2]
            val repsList = repsStr.split(Regex("[,\\s]+"))
                .mapNotNull { it.trim().toIntOrNull() }
            if (repsList.isNotEmpty()) {
                return repsList.map { ParsedSet(weight, it) }
            }
        }
        
        // Try simple "W R" format (weight space reps)
        val simplePattern = Regex("""(\d+(?:\.\d+)?)\s+(\d+)""")
        simplePattern.find(cleaned)?.let { match ->
            val weight = match.groupValues[1].toFloatOrNull() ?: return emptyList()
            val reps = match.groupValues[2].toIntOrNull() ?: return emptyList()
            return listOf(ParsedSet(weight, reps))
        }
        
        return emptyList()
    }
    
    /**
     * Convert parsed exercises to ExerciseSetEntry list for saving.
     */
    fun toSetEntries(parsedExercises: List<ParsedExercise>): List<ExerciseSetEntry> {
        return parsedExercises.flatMap { parsed ->
            parsed.sets.map { set ->
                ExerciseSetEntry(
                    exerciseId = parsed.exercise.id,
                    weight = set.weight,
                    reps = set.reps
                )
            }
        }
    }
}

