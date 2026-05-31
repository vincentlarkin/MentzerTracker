package com.vincentlarkin.mentzertracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupContractTest {

    @Test
    fun backupRoundTrip_preservesCardioMetricsAndTrackingMode() {
        val config = UserWorkoutConfig(
            workoutAExerciseIds = listOf("bench_press", "treadmill"),
            workoutBExerciseIds = listOf("crunches", "elliptical"),
            customExercises = listOf(
                Exercise(
                    id = "custom_sled_push",
                    name = "Sled Push",
                    aliases = listOf("sled"),
                    trackingMode = ExerciseTrackingMode.CARDIO
                )
            )
        )

        val logs = listOf(
            WorkoutLogEntry(
                id = 1L,
                templateId = "TODAY",
                date = "2026-03-13",
                sets = listOf(
                    ExerciseSetEntry(
                        exerciseId = "bench_press",
                        weight = 225f,
                        reps = 8
                    ),
                    ExerciseSetEntry(
                        exerciseId = "treadmill",
                        durationMinutes = 20f,
                        distanceMiles = 1.9f,
                        calories = 250,
                        steps = 4100
                    )
                ),
                notes = "Mixed session"
            ),
            WorkoutLogEntry(
                id = 2L,
                templateId = "TODAY",
                date = "2026-03-12",
                sets = listOf(
                    ExerciseSetEntry(
                        exerciseId = "elliptical",
                        durationMinutes = 30f,
                        distanceMiles = 2.5f,
                        steps = 4500
                    ),
                    ExerciseSetEntry(
                        exerciseId = "custom_sled_push",
                        durationMinutes = 10f,
                        calories = 140
                    )
                )
            )
        )

        val json = gson.toJson(
            BackupSnapshot(
                exportedAt = "2026-03-13T09:00:00-05:00",
                appVersion = "2.4Beta",
                themeMode = "dark",
                hasSeenSplash = true,
                allowPartialSessions = true,
                workoutConfig = config,
                workoutLogs = logs
            )
        )

        val parsed = parseBackupSnapshotForTest(json)
        val sanitizedLogs = sanitizeImportLogsForTest(parsed.workoutLogs.orEmpty())

        val parsedConfig = parsed.workoutConfig
        assertNotNull(parsedConfig)
        assertEquals(
            ExerciseTrackingMode.CARDIO,
            parsedConfig!!.customExercises.first().trackingMode
        )

        val treadmillEntry = sanitizedLogs
            .first { it.id == 1L }
            .sets
            .first { it.exerciseId == "treadmill" }
        assertEquals(20f, treadmillEntry.durationMinutes ?: 0f, 0.001f)
        assertEquals(1.9f, treadmillEntry.distanceMiles ?: 0f, 0.001f)
        assertEquals(250, treadmillEntry.calories)
        assertEquals(4100, treadmillEntry.steps)

        val customCardioEntry = sanitizedLogs
            .first { it.id == 2L }
            .sets
            .first { it.exerciseId == "custom_sled_push" }
        assertEquals(10f, customCardioEntry.durationMinutes ?: 0f, 0.001f)
        assertEquals(140, customCardioEntry.calories)
        assertTrue(sanitizedLogs.flatMap { it.sets }.all { it.exerciseId.isNotBlank() })
    }

    @Test
    fun parseBackupSnapshot_acceptsLegacyLogsWithoutNewFields() {
        val legacyJson = """
            {
              "themeMode": "dark",
              "hasSeenSplash": true,
              "allowPartialSessions": false,
              "workoutConfig": {
                "workoutAExerciseIds": ["bench_press"],
                "workoutBExerciseIds": ["squat"],
                "customExercises": []
              },
              "workoutLogs": [
                {
                  "id": 10,
                  "templateId": "TODAY",
                  "date": "2026-03-10",
                  "sets": [
                    {
                      "exerciseId": "bench_press",
                      "weight": 185.0,
                      "reps": 8
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val parsed = parseBackupSnapshotForTest(legacyJson)
        val set = parsed.workoutLogs!!.single().sets.single()

        assertEquals("bench_press", set.exerciseId)
        assertEquals(185f, set.weight, 0.001f)
        assertEquals(8, set.reps)
        assertEquals(null, set.durationMinutes)
        assertEquals(null, set.distanceMiles)
        assertEquals(null, set.calories)
        assertEquals(null, set.steps)
    }

    private fun parseBackupSnapshotForTest(json: String): BackupSnapshot {
        val method = Class.forName("com.vincentlarkin.mentzertracker.MainActivityKt")
            .getDeclaredMethod("parseBackupSnapshot", String::class.java)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(null, json) as BackupSnapshot
    }

    private fun sanitizeImportLogsForTest(logs: List<WorkoutLogEntry>): List<WorkoutLogEntry> {
        val method = Class.forName("com.vincentlarkin.mentzertracker.MainActivityKt")
            .getDeclaredMethod("sanitizeImportLogs", List::class.java)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(null, logs) as List<WorkoutLogEntry>
    }
}
