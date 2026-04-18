package com.vincentlarkin.mentzertracker

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val workoutDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun currentWorkoutDate(): String = LocalDate.now().format(workoutDateFormatter)

fun parseWorkoutLocalDate(dateStr: String): LocalDate? {
    if (dateStr.isBlank()) return null
    return try {
        LocalDate.parse(dateStr, workoutDateFormatter)
    } catch (_: DateTimeParseException) {
        null
    }
}

fun formatWorkoutDateLabel(
    dateStr: String,
    pattern: String,
    locale: Locale = Locale.getDefault()
): String {
    val date = parseWorkoutLocalDate(dateStr) ?: return dateStr
    return date.format(DateTimeFormatter.ofPattern(pattern, locale))
}

fun workoutDatePickerInitialMillis(dateStr: String): Long? {
    return parseWorkoutLocalDate(dateStr)
        ?.atStartOfDay(ZoneOffset.UTC)
        ?.toInstant()
        ?.toEpochMilli()
}

fun workoutDateFromPickerMillis(millis: Long): String {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .format(workoutDateFormatter)
}

fun sortWorkoutLogs(logs: List<WorkoutLogEntry>): List<WorkoutLogEntry> {
    return logs.sortedWith(
        compareBy<WorkoutLogEntry> { parseWorkoutLocalDate(it.date) ?: LocalDate.MIN }
            .thenBy { it.id }
    )
}
