package com.vincentlarkin.mentzertracker.novanotes

import com.vincentlarkin.mentzertracker.Exercise
import com.vincentlarkin.mentzertracker.ExerciseSetEntry
import com.vincentlarkin.mentzertracker.allExercises

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
    
    // Aliases for common exercise name variations
    private val exerciseAliases = mapOf(
        // Bench variations
        "bench" to "bench_press",
        "bench press" to "bench_press",
        "flat bench" to "bench_press",
        "barbell bench" to "bench_press",
        "bb bench" to "bench_press",
        
        // Incline
        "incline" to "incline_press",
        "incline press" to "incline_press",
        "incline bench" to "incline_press",
        
        // Squat
        "squat" to "squat",
        "squats" to "squat",
        "smith squat" to "squat",
        "smith" to "squat",
        
        // Deadlift
        "deadlift" to "deadlift",
        "dl" to "deadlift",
        "dead" to "deadlift",
        "deads" to "deadlift",
        
        // Pulldown
        "pulldown" to "pulldown",
        "pull down" to "pulldown",
        "lat pulldown" to "pulldown",
        "pulldowns" to "pulldown",
        
        // Dips
        "dips" to "dips",
        "dip" to "dips",
        "chest dips" to "dips",
        "tricep dips" to "dips",
        
        // OHP
        "ohp" to "ohp",
        "overhead press" to "ohp",
        "overhead" to "ohp",
        "shoulder press" to "ohp",
        "military press" to "ohp",
        "military" to "ohp",
        
        // Row
        "row" to "row",
        "rows" to "row",
        "barbell row" to "row",
        "bb row" to "row",
        "bent over row" to "row",
        
        // Leg press
        "leg press" to "leg_press",
        "legpress" to "leg_press",
        
        // Calf raise
        "calf raise" to "calf_raise",
        "calf raises" to "calf_raise",
        "calves" to "calf_raise",
        "calf" to "calf_raise",
        "standing calf" to "calf_raise"
    )
    
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
        val sortedAliases = exerciseAliases.keys.sortedByDescending { it.length }
        for (alias in sortedAliases) {
            if (normalizedLine.startsWith(alias)) {
                val exerciseId = exerciseAliases[alias]
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

