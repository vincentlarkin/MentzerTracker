package com.vincentlarkin.mentzertracker

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.math.max
import androidx.core.content.edit

private const val NOTIFICATION_PREFS = "mentzer_prefs"
private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
private const val KEY_NOTIFICATIONS_FREQUENCY = "notifications_frequency"
private const val KEY_NOTIFICATIONS_CUSTOM_DAYS = "notifications_custom_days"
private const val KEY_NOTIFICATIONS_HOUR = "notifications_hour"
private const val KEY_NOTIFICATIONS_MINUTE = "notifications_minute"

private const val NOTIFICATION_CHANNEL_ID = "mentzer_workout_channel"
private const val NOTIFICATION_CHANNEL_NAME = "Workout reminders"
private const val NOTIFICATION_WORK_NAME = "MentzerTrackerNotifications"

enum class ReminderFrequency {
    DAILY,
    WEEKLY,
    CUSTOM
}

data class NotificationPreferences(
    val enabled: Boolean = false,
    val frequency: ReminderFrequency = ReminderFrequency.DAILY,
    val customIntervalDays: Int = 2,
    val hourOfDay: Int = 9,
    val minute: Int = 0
)

object NotificationHelper {
    fun loadPreferences(context: Context): NotificationPreferences {
        val prefs = context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
        val frequency = when (prefs.getString(KEY_NOTIFICATIONS_FREQUENCY, ReminderFrequency.DAILY.name)) {
            ReminderFrequency.WEEKLY.name -> ReminderFrequency.WEEKLY
            ReminderFrequency.CUSTOM.name -> ReminderFrequency.CUSTOM
            else -> ReminderFrequency.DAILY
        }
        val customDays = prefs.getInt(KEY_NOTIFICATIONS_CUSTOM_DAYS, 2).coerceAtLeast(1)
        val hour = prefs.getInt(KEY_NOTIFICATIONS_HOUR, 9).coerceIn(0, 23)
        val minute = prefs.getInt(KEY_NOTIFICATIONS_MINUTE, 0).coerceIn(0, 59)
        return NotificationPreferences(enabled, frequency, customDays, hour, minute)
    }

    fun savePreferences(context: Context, prefs: NotificationPreferences) {
        context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_NOTIFICATIONS_ENABLED, prefs.enabled)
            putString(KEY_NOTIFICATIONS_FREQUENCY, prefs.frequency.name)
            putInt(KEY_NOTIFICATIONS_CUSTOM_DAYS, prefs.customIntervalDays)
            putInt(KEY_NOTIFICATIONS_HOUR, prefs.hourOfDay)
            putInt(KEY_NOTIFICATIONS_MINUTE, prefs.minute)
        }
    }

    fun scheduleNotifications(context: Context, prefs: NotificationPreferences) {
        cancelNotifications(context)
        if (!prefs.enabled) return

        val intervalDays = when (prefs.frequency) {
            ReminderFrequency.DAILY -> 1L
            ReminderFrequency.WEEKLY -> 7L
            ReminderFrequency.CUSTOM -> max(1L, prefs.customIntervalDays.toLong())
        }

        val initialDelayMs = computeInitialDelayMillis(prefs.hourOfDay, prefs.minute)
        val request = PeriodicWorkRequestBuilder<WorkoutReminderWorker>(intervalDays, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .addTag(NOTIFICATION_WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NOTIFICATION_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelNotifications(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(NOTIFICATION_WORK_NAME)
    }

    fun triggerTestNotification(context: Context) {
        if (!hasPermission(context)) {
            if (needsRuntimePermission()) {
                Toast.makeText(
                    context,
                    "Grant notification permission to send reminders.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }
        showReminderNotification(context, isTest = true)
    }

    internal fun showReminderNotification(context: Context, isTest: Boolean = false) {
        if (!hasPermission(context)) return
        ensureChannel(context)
        val title = context.getString(R.string.app_name)
        val content = if (isTest) {
            "This is a test notification from MentzerTracker."
        } else {
            buildReminderMessage(context)
        }
        showNotification(context, title, content)
    }

    fun hasPermission(context: Context): Boolean {
        return !needsRuntimePermission() ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun needsRuntimePermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    private fun computeInitialDelayMillis(hour: Int, minute: Int): Long {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        var nextRun = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1)
        }
        val duration = Duration.between(now, nextRun)
        return max(0L, duration.toMillis())
    }

    private fun buildReminderMessage(context: Context): String {
        return "Weekly check-in: log today’s workout and stay consistent."
    }


    @SuppressLint("MissingPermission")
    private fun showNotification(context: Context, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(message)
            )
            .build()

        NotificationManagerCompat.from(context).notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }
}

