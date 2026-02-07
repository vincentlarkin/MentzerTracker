package com.vincentlarkin.mentzertracker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max

class WorkoutReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val prefs = NotificationHelper.loadPreferences(applicationContext)
        if (!prefs.enabled) {
            NotificationHelper.cancelNotifications(applicationContext)
            return Result.success()
        }

        val intervalDays = when (prefs.frequency) {
            ReminderFrequency.DAILY -> 1L
            ReminderFrequency.WEEKLY -> 7L
            ReminderFrequency.CUSTOM -> max(1L, prefs.customIntervalDays.toLong())
        }

        val lastWorkoutDate = loadWorkoutLogs(applicationContext)
            .mapNotNull { log ->
                runCatching {
                    LocalDate.parse(log.date, DateTimeFormatter.ISO_LOCAL_DATE)
                }.getOrNull()
            }
            .maxOrNull()

        if (lastWorkoutDate != null) {
            val daysSinceWorkout = ChronoUnit.DAYS.between(lastWorkoutDate, LocalDate.now())
            if (daysSinceWorkout < intervalDays) {
                return Result.success()
            }
        }

        NotificationHelper.showReminderNotification(applicationContext)
        return Result.success()
    }
}

