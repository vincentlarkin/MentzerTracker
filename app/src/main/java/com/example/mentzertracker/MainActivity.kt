package com.vincentlarkin.mentzertracker

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.vincentlarkin.mentzertracker.novanotes.NovaBuilderScreen
import com.vincentlarkin.mentzertracker.novanotes.NovaNotesScreen
import com.vincentlarkin.mentzertracker.novanotes.NovaSplashScreen
import com.vincentlarkin.mentzertracker.ui.theme.MentzerTrackerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Transparent system bars; icon contrast auto-handled by the system.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )

        setContent {
            MentzerApp()
        }
    }
}


// ---------- UI-ONLY MODELS ----------
enum class ThemeMode { DARK, LIGHT }
private val ScreenPadding = 16.dp
private const val UI_SCALE_FACTOR = 0.94f

internal const val CUSTOM_EXERCISE_NAME_LIMIT = 40
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_ALLOW_PARTIAL_SESSIONS = "allow_partial_sessions"

// Point used for graphs + text list
data class SessionPoint(
    val sessionIndex: Int,
    val date: String,
    val weight: Float,
    val reps: Int,
    val notes: String? = null
)

// ---------- SHARED PREFS KEYS / GSON ----------

private const val PREFS_NAME = "mentzer_prefs"
private const val KEY_HAS_SEEN_SPLASH = "has_seen_splash"
private const val KEY_WORKOUT_LOGS = "workout_logs"
private const val KEY_WORKOUT_CONFIG = "workout_config"
private const val KEY_WORKOUT_INTERVAL = "workout_interval_days"
private const val DEFAULT_WORKOUT_INTERVAL = 7 // Weekly by default

internal data class BackupSnapshot(
    val exportedAt: String = "",
    val appVersion: String = "",
    val themeMode: String = "dark",
    val hasSeenSplash: Boolean = false,
    val allowPartialSessions: Boolean = false,
    val workoutConfig: UserWorkoutConfig? = null,
    val workoutLogs: List<WorkoutLogEntry>? = emptyList()
)

internal val gson = Gson()

internal fun hasSeenSplash(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_HAS_SEEN_SPLASH, false)
}

private fun saveHasSeenSplash(context: Context, seen: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit {
        if (seen) {
            putBoolean(KEY_HAS_SEEN_SPLASH, true)
        } else {
            remove(KEY_HAS_SEEN_SPLASH)
        }
    }
}

private fun setHasSeenSplash(context: Context) {
    saveHasSeenSplash(context, true)
}

internal fun loadWorkoutLogs(context: Context): List<WorkoutLogEntry> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_WORKOUT_LOGS, null) ?: return emptyList()
    return try {
        val type = object : TypeToken<List<WorkoutLogEntry>>() {}.type
        gson.fromJson<List<WorkoutLogEntry>>(json, type) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}

internal fun saveWorkoutLogs(context: Context, logs: List<WorkoutLogEntry>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = gson.toJson(logs)
    prefs.edit { putString(KEY_WORKOUT_LOGS, json) }
}

private fun hasWorkoutConfig(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_WORKOUT_CONFIG, null) != null
}

internal fun loadWorkoutConfig(context: Context): UserWorkoutConfig {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_WORKOUT_CONFIG, null) ?: return defaultWorkoutConfig
    return try {
        val jsonElement = JsonParser.parseString(json)
        if (!jsonElement.isJsonObject) {
            return defaultWorkoutConfig
        }
        val jsonObject = jsonElement.asJsonObject
        if (!jsonObject.has("customExercises") || jsonObject.get("customExercises").isJsonNull) {
            jsonObject.add("customExercises", JsonArray())
        }
        val parsed = gson.fromJson(jsonObject, UserWorkoutConfig::class.java)
        parsed?.sanitized() ?: defaultWorkoutConfig
    } catch (_: Exception) {
        defaultWorkoutConfig
    }
}

private fun UserWorkoutConfig.sanitized(): UserWorkoutConfig {
    val filtered = customExercises
        .filter { it.id.isNotBlank() && it.name.isNotBlank() }
        .distinctBy { it.id }
    return copy(customExercises = filtered)
}

private fun saveWorkoutConfig(context: Context, config: UserWorkoutConfig) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = gson.toJson(config)
    prefs.edit { putString(KEY_WORKOUT_CONFIG, json) }
}
private fun loadThemeMode(context: Context): ThemeMode {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return when (prefs.getString(KEY_THEME_MODE, "dark")) {
        "light" -> ThemeMode.LIGHT
        else -> ThemeMode.DARK
    }
}

private fun saveThemeMode(context: Context, mode: ThemeMode) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit {
        putString(KEY_THEME_MODE, if (mode == ThemeMode.DARK) "dark" else "light")
    }
}

internal fun allowPartialSessions(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_ALLOW_PARTIAL_SESSIONS, false)
}

private fun saveAllowPartialSessions(context: Context, allowed: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit { putBoolean(KEY_ALLOW_PARTIAL_SESSIONS, allowed) }
}

fun loadWorkoutInterval(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_WORKOUT_INTERVAL, DEFAULT_WORKOUT_INTERVAL)
}

fun saveWorkoutInterval(context: Context, days: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit { putInt(KEY_WORKOUT_INTERVAL, days.coerceIn(1, 30)) }
}

private fun resetAppData(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit {
        remove(KEY_HAS_SEEN_SPLASH)
        remove(KEY_WORKOUT_LOGS)
        remove(KEY_WORKOUT_CONFIG)
        remove(KEY_THEME_MODE)
        remove(KEY_ALLOW_PARTIAL_SESSIONS)
    }
}

internal fun importBackupFromJson(
    context: Context,
    json: String
): ThemeMode {
    val snapshot = parseBackupSnapshot(json)
    val themeMode = when (snapshot.themeMode.lowercase(Locale.ROOT)) {
        "light" -> ThemeMode.LIGHT
        else -> ThemeMode.DARK
    }
    val config = snapshot.workoutConfig?.sanitized() ?: defaultWorkoutConfig
    val logs = sanitizeImportLogs(snapshot.workoutLogs.orEmpty())

    resetAppData(context)
    saveThemeMode(context, themeMode)
    saveWorkoutConfig(context, config)
    saveAllowPartialSessions(context, snapshot.allowPartialSessions)
    saveWorkoutLogs(context, logs)
    saveHasSeenSplash(context, snapshot.hasSeenSplash)

    return themeMode
}

private fun parseBackupSnapshot(json: String): BackupSnapshot {
    val element = JsonParser.parseString(json)
    if (!element.isJsonObject) {
        throw IllegalArgumentException("Backup file must be a JSON object.")
    }
    val root = element.asJsonObject

    val configObject = if (root.has("workoutConfig") && root.get("workoutConfig").isJsonObject) {
        root.getAsJsonObject("workoutConfig")
    } else {
        val defaultConfig = gson.toJsonTree(defaultWorkoutConfig).asJsonObject
        root.add("workoutConfig", defaultConfig)
        defaultConfig
    }
    ensureJsonArray(configObject, "workoutAExerciseIds")
    ensureJsonArray(configObject, "workoutBExerciseIds")
    ensureJsonArray(configObject, "customExercises")

    val logsArray = when {
        root.has("workoutLogs") && root.get("workoutLogs").isJsonArray -> {
            root.getAsJsonArray("workoutLogs")
        }
        root.has("workoutLog") && root.get("workoutLog").isJsonObject -> {
            JsonArray().also {
                it.add(root.getAsJsonObject("workoutLog"))
                root.add("workoutLogs", it)
            }
        }
        else -> {
            JsonArray().also { root.add("workoutLogs", it) }
        }
    }

    logsArray.forEach { logElement ->
        if (logElement.isJsonObject) {
            val logObject = logElement.asJsonObject
            ensureJsonArray(logObject, "sets")
            if (!logObject.has("templateId") || logObject.get("templateId").isJsonNull) {
                logObject.addProperty("templateId", "TODAY")
            }
            if (!logObject.has("date") || logObject.get("date").isJsonNull) {
                logObject.addProperty("date", "")
            }
        }
    }

    return gson.fromJson(root, BackupSnapshot::class.java)
        ?: throw IllegalArgumentException("Unable to parse backup.")
}


private fun ensureJsonArray(obj: com.google.gson.JsonObject, key: String) {
    if (!obj.has(key) || !obj.get(key).isJsonArray) {
        obj.add(key, JsonArray())
    }
}

private fun sanitizeImportLogs(rawLogs: List<WorkoutLogEntry>): List<WorkoutLogEntry> {
    return rawLogs.mapNotNull { log ->
        val sanitizedSets = log.sets.filter { it.exerciseId.isNotBlank() }
        if (sanitizedSets.isEmpty()) {
            null
        } else {
            val templateId = log.templateId.ifBlank { "TODAY" }
            log.copy(sets = sanitizedSets, templateId = templateId)
        }
    }
}


// ---------- ROOT / FLOW CONTROL ----------
@Composable
fun MentzerApp() {
    val context = LocalContext.current

    val parentDensity = LocalDensity.current
    val scaledDensity = remember(parentDensity.density, parentDensity.fontScale) {
        parentDensity.scaled(UI_SCALE_FACTOR)
    }

    val themeModeState = remember { mutableStateOf(loadThemeMode(context)) }
    val allowPartialSessionsState = remember { mutableStateOf(allowPartialSessions(context)) }
    val resetKeyState = remember { mutableStateOf(0) }

    val themeMode = themeModeState.value
    val allowPartialSessions = allowPartialSessionsState.value
    val resetKey = resetKeyState.value

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MentzerTrackerTheme(darkTheme = themeMode == ThemeMode.DARK) {
            ApplySystemBarStyle(themeMode = themeMode)

        val handleImport: (ThemeMode) -> Unit = { importedMode ->
            themeModeState.value = importedMode
            allowPartialSessionsState.value = allowPartialSessions(context)
            resetKeyState.value = resetKeyState.value + 1
        }

        // Nova shell handles settings internally via bottom nav
        key(resetKey) {
            AppRoot(
                themeMode = themeMode,
                allowPartialSessions = allowPartialSessions,
                onThemeModeChange = { newMode ->
                    themeModeState.value = newMode
                    saveThemeMode(context, newMode)
                },
                onAllowPartialSessionsChange = { enabled ->
                    allowPartialSessionsState.value = enabled
                    saveAllowPartialSessions(context, enabled)
                },
                onImportBackup = handleImport,
                onResetData = {
                    resetAppData(context)
                    themeModeState.value = loadThemeMode(context)
                    allowPartialSessionsState.value = allowPartialSessions(context)
                    resetKeyState.value = resetKeyState.value + 1
                }
            )
        }
        }
    }
}

@Composable
private fun ApplySystemBarStyle(themeMode: ThemeMode) {
    val view = LocalView.current
    val lightIcons = themeMode == ThemeMode.LIGHT
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        @Suppress("DEPRECATION")
        run {
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
        }
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = lightIcons
        controller.isAppearanceLightNavigationBars = lightIcons
    }
}


@Composable
fun AppRoot(
    themeMode: ThemeMode,
    allowPartialSessions: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAllowPartialSessionsChange: (Boolean) -> Unit,
    onImportBackup: (ThemeMode) -> Unit,
    onResetData: () -> Unit
) {
    val context = LocalContext.current

    val showSplashState = remember { mutableStateOf(!hasSeenSplash(context)) }
    val workoutConfigState = remember { mutableStateOf(loadWorkoutConfig(context)) }
    val hasConfigState = remember { mutableStateOf(hasWorkoutConfig(context)) }
    val editingConfigState = remember { mutableStateOf(false) }

    val showSplash = showSplashState.value
    val workoutConfig = workoutConfigState.value
    val hasConfig = hasConfigState.value
    val editingConfig = editingConfigState.value

    // If we're editing an existing config, back should return to main screen
    if (editingConfig && hasConfig) {
        BackHandler {
            editingConfigState.value = false
        }
    }

    when {
        showSplash -> {
            NovaSplashScreen(
                onStart = {
                    // Don't persist yet - only mark as seen when they complete workout setup
                    showSplashState.value = false
                },
                onOpenSettings = { /* handled internally now */ },
                onImportBackup = onImportBackup
            )
        }

        editingConfig || !hasConfig -> {
            NovaBuilderScreen(
                initialConfig = workoutConfig,
                onDone = { newConfig ->
                    workoutConfigState.value = newConfig
                    saveWorkoutConfig(context, newConfig)
                    hasConfigState.value = true
                    editingConfigState.value = false
                    // Only now mark splash as seen - user completed full setup
                    setHasSeenSplash(context)
                },
                showBack = editingConfig && hasConfig,
                onBack = { editingConfigState.value = false },
                onOpenSettings = null
            )
        }

        else -> {
            // Use the new Nova shell with bottom navigation
            NovaAppShell(
                config = workoutConfig,
                themeMode = themeMode,
                isPartialSessionsAllowed = allowPartialSessions,
                onThemeModeChange = onThemeModeChange,
                onAllowPartialSessionsChange = onAllowPartialSessionsChange,
                onImportBackup = onImportBackup,
                onResetData = onResetData,
                onEditWorkouts = { editingConfigState.value = true }
            )
        }
    }
}

private fun Density.scaled(scaleFactor: Float): Density {
    val baseDensity = density
    val baseFontScale = fontScale
    return object : Density {
        override val density: Float = baseDensity * scaleFactor
        override val fontScale: Float = baseFontScale * scaleFactor
    }
}
