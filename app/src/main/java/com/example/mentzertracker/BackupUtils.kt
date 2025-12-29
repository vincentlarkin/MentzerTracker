package com.vincentlarkin.mentzertracker

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Backup/Export utilities shared across the app.
 */

fun buildExportJson(
    context: Context,
    themeMode: ThemeMode
): String {
    val snapshot = BackupSnapshot(
        exportedAt = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.getDefault()
        ).format(Date()),
        appVersion = resolveAppVersion(context),
        themeMode = themeMode.name.lowercase(Locale.ROOT),
        hasSeenSplash = hasSeenSplash(context),
        allowPartialSessions = allowPartialSessions(context),
        workoutConfig = loadWorkoutConfig(context),
        workoutLogs = loadWorkoutLogs(context)
    )
    return gson.toJson(snapshot)
}

fun resolveAppVersion(context: Context): String {
    return try {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            ).versionName ?: "unknown"
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        }
    } catch (e: Exception) {
        "unknown"
    }
}

