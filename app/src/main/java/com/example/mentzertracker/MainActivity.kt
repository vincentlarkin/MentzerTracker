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
import com.vincentlarkin.mentzertracker.ui.settings.SettingsScreen
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

private const val CUSTOM_EXERCISE_NAME_LIMIT = 40
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

    if (!root.has("workoutConfig") || !root.get("workoutConfig").isJsonObject) {
        throw IllegalArgumentException("Backup file missing workoutConfig.")
    }
    val configObject = root.getAsJsonObject("workoutConfig")
    ensureJsonArray(configObject, "workoutAExerciseIds")
    ensureJsonArray(configObject, "workoutBExerciseIds")
    ensureJsonArray(configObject, "customExercises")

    if (!root.has("workoutLogs") || !root.get("workoutLogs").isJsonArray) {
        root.add("workoutLogs", JsonArray())
    } else {
        val logsArray = root.getAsJsonArray("workoutLogs")
        logsArray.forEach { logElement ->
            if (logElement.isJsonObject) {
                val logObject = logElement.asJsonObject
                ensureJsonArray(logObject, "sets")
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
            log.copy(sets = sanitizedSets)
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
                    setHasSeenSplash(context)
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




// ---------- SCREENS ----------

/**
 * Simple one-screen builder:
 * - Scrollable list of checkboxes for Workout A
 * - Scrollable list of checkboxes for Workout B
 * - Must pick at least 2 for each
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutBuilderScreen(
    initialConfig: UserWorkoutConfig,
    onDone: (UserWorkoutConfig) -> Unit,
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()

    val baseExercises = allExercises
    val customExercises = remember(initialConfig) {
        mutableStateListOf<Exercise>().apply {
            addAll(initialConfig.customExercises)
        }
    }

    val aSelections = remember(initialConfig) {
        val initialA = initialConfig.workoutAExerciseIds.toSet()
        mutableStateMapOf<String, Boolean>().apply {
            (baseExercises + initialConfig.customExercises).forEach { ex ->
                this[ex.id] = ex.id in initialA
            }
        }
    }
    val bSelections = remember(initialConfig) {
        val initialB = initialConfig.workoutBExerciseIds.toSet()
        mutableStateMapOf<String, Boolean>().apply {
            (baseExercises + initialConfig.customExercises).forEach { ex ->
                this[ex.id] = ex.id in initialB
            }
        }
    }

    var errorText by remember { mutableStateOf<String?>(null) }
    var customNameA by remember { mutableStateOf("") }
    var customNameB by remember { mutableStateOf("") }
    var customErrorA by remember { mutableStateOf<String?>(null) }
    var customErrorB by remember { mutableStateOf<String?>(null) }

    fun ensureSelectionEntry(id: String) {
        if (aSelections[id] == null) {
            aSelections[id] = false
        }
        if (bSelections[id] == null) {
            bSelections[id] = false
        }
    }

    fun addCustomExercise(
        rawName: String,
        selectForA: Boolean,
        selectForB: Boolean
    ): String? {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) {
            return "Please enter a name."
        }
        if (trimmed.length > CUSTOM_EXERCISE_NAME_LIMIT) {
            return "Limit is $CUSTOM_EXERCISE_NAME_LIMIT characters."
        }
        val lower = trimmed.lowercase(Locale.getDefault())
        val existingNames = (baseExercises + customExercises).map {
            it.name.lowercase(Locale.getDefault())
        }
        if (lower in existingNames) {
            return "That exercise already exists."
        }
        val existingIds = (baseExercises + customExercises).map { it.id }.toSet()
        val newExercise = Exercise(
            id = generateCustomExerciseId(existingIds),
            name = trimmed
        )
        customExercises.add(newExercise)
        ensureSelectionEntry(newExercise.id)
        aSelections[newExercise.id] = selectForA
        bSelections[newExercise.id] = selectForB
        return null
    }

    fun removeCustomExercise(exercise: Exercise) {
        customExercises.removeAll { it.id == exercise.id }
        aSelections.remove(exercise.id)
        bSelections.remove(exercise.id)
    }

    fun toggleSelection(
        map: MutableMap<String, Boolean>,
        id: String,
        newValue: Boolean
    ) {
        map[id] = newValue
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Build your A / B workouts") },
                navigationIcon = {
                    if (showBack && onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.clip(RectangleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (onOpenSettings != null) {
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.clip(RectangleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)          // respects notch / status bar
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val combinedExercises = baseExercises + customExercises.toList()

            Text(
                "Pick at least 2 exercises for each workout. You can reuse an exercise in both A and B if you want.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Workout A section
            Text(
                "Workout A",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = customNameA,
                    onValueChange = { value ->
                        val clipped = value.take(CUSTOM_EXERCISE_NAME_LIMIT)
                        customNameA = clipped
                        if (customErrorA != null) {
                            customErrorA = null
                        }
                    },
                    label = { Text("Custom exercise") },
                    placeholder = { Text("e.g. Cable Fly") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = customErrorA != null,
                    supportingText = {
                        Text("${customNameA.length}/$CUSTOM_EXERCISE_NAME_LIMIT")
                    }
                )
                Button(
                    onClick = {
                        val error = addCustomExercise(
                            rawName = customNameA,
                            selectForA = true,
                            selectForB = false
                        )
                        if (error != null) {
                            customErrorA = error
                        } else {
                            customNameA = ""
                            customErrorA = null
                        }
                    },
                    enabled = customNameA.isNotBlank(),
                    shape = RectangleShape
                ) {
                    Text("Add")
                }
            }
            if (customErrorA != null) {
                Text(
                    text = customErrorA!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            combinedExercises.forEach { ex ->
                val checked = aSelections[ex.id] == true
                val isCustom = customExercises.any { it.id == ex.id }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { toggleSelection(aSelections, ex.id, it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.outline,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Text(
                        ex.name,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { toggleSelection(aSelections, ex.id, !checked) }
                            .padding(end = 8.dp)
                    )
                    if (isCustom) {
                        TextButton(
                            onClick = { removeCustomExercise(ex) },
                            shape = RectangleShape
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Workout B section
            Text(
                "Workout B",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = customNameB,
                    onValueChange = { value ->
                        val clipped = value.take(CUSTOM_EXERCISE_NAME_LIMIT)
                        customNameB = clipped
                        if (customErrorB != null) {
                            customErrorB = null
                        }
                    },
                    label = { Text("Custom exercise") },
                    placeholder = { Text("e.g. Reverse Crunch") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = customErrorB != null,
                    supportingText = {
                        Text("${customNameB.length}/$CUSTOM_EXERCISE_NAME_LIMIT")
                    }
                )
                Button(
                    onClick = {
                        val error = addCustomExercise(
                            rawName = customNameB,
                            selectForA = false,
                            selectForB = true
                        )
                        if (error != null) {
                            customErrorB = error
                        } else {
                            customNameB = ""
                            customErrorB = null
                        }
                    },
                    enabled = customNameB.isNotBlank(),
                    shape = RectangleShape
                ) {
                    Text("Add")
                }
            }
            if (customErrorB != null) {
                Text(
                    text = customErrorB!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            combinedExercises.forEach { ex ->
                val checked = bSelections[ex.id] == true
                val isCustom = customExercises.any { it.id == ex.id }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { toggleSelection(bSelections, ex.id, it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.outline,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Text(
                        ex.name,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { toggleSelection(bSelections, ex.id, !checked) }
                            .padding(end = 8.dp)
                    )
                    if (isCustom) {
                        TextButton(
                            onClick = { removeCustomExercise(ex) },
                            shape = RectangleShape
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }

            if (errorText != null) {
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = {
                    val combined = (baseExercises + customExercises.toList())
                        .distinctBy { it.id }
                    val validIds = combined.map { it.id }.toSet()
                    val aIds = aSelections
                        .filter { (id, checked) -> checked && id in validIds }
                        .keys
                        .toList()
                    val bIds = bSelections
                        .filter { (id, checked) -> checked && id in validIds }
                        .keys
                        .toList()

                    when {
                        aIds.size < 2 ->
                            errorText = "Please pick at least 2 exercises for Workout A."
                        bIds.size < 2 ->
                            errorText = "Please pick at least 2 exercises for Workout B."
                        else -> {
                            errorText = null
                            onDone(
                                UserWorkoutConfig(
                                    workoutAExerciseIds = aIds,
                                    workoutBExerciseIds = bIds,
                                    customExercises = customExercises.toList()
                                )
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RectangleShape
            ) {
                Text("Save workouts")
            }
        }
    }
}




private fun generateCustomExerciseId(existingIds: Set<String>): String {
    var candidate: String
    do {
        candidate = "custom_${UUID.randomUUID()}"
    } while (candidate in existingIds)
    return candidate
}

private fun Density.scaled(scaleFactor: Float): Density {
    val baseDensity = density
    val baseFontScale = fontScale
    return object : Density {
        override val density: Float = baseDensity * scaleFactor
        override val fontScale: Float = baseFontScale * scaleFactor
    }
}


// ---------- MAIN TRACKER APP (INCLUDING FULL PROGRESS NAV) ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTrackerApp(
    config: UserWorkoutConfig,
    onEditWorkouts: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    allowPartialSessions: Boolean
) {
    val context = LocalContext.current
    var selectedTemplateId by remember { mutableStateOf("A") }
    var showNovaNotesState by remember { mutableStateOf(false) }

    val combinedExercises = remember(config) {
        (allExercises + config.customExercises).distinctBy { it.id }
    }
    val exercisesById = remember(config) {
        combinedExercises.associateBy { it.id }
    }

    // Build templates from current config
    val templates = remember(config) {
        listOf(
            WorkoutTemplate(
                id = "A",
                name = "Workout A",
                exerciseIds = config.workoutAExerciseIds
            ),
            WorkoutTemplate(
                id = "B",
                name = "Workout B",
                exerciseIds = config.workoutBExerciseIds
            )
        )
    }

    // Logs
    val logEntries = remember {
        mutableStateListOf<WorkoutLogEntry>().apply {
            addAll(loadWorkoutLogs(context))
        }
    }

    val showFullProgressState = remember { mutableStateOf(false) }
    val fullScreenExerciseIdState = remember { mutableStateOf<String?>(null) }
    val showFullProgress = showFullProgressState.value
    val showNovaNotes = showNovaNotesState

    // Handle back for Nova Notes
    if (showNovaNotes) {
        BackHandler {
            showNovaNotesState = false
        }
    }

    if (showFullProgress) {
        BackHandler {
            showFullProgressState.value = false
            fullScreenExerciseIdState.value = null
        }
    }

    // Nova Notes fullscreen
    if (showNovaNotes) {
        NovaNotesScreen(
            customExercises = config.customExercises,
            onSave = { sets, notes ->
                val currentTemplate = templates.firstOrNull { it.id == selectedTemplateId }
                    ?: templates.firstOrNull()
                val entry = WorkoutLogEntry(
                    id = System.currentTimeMillis(),
                    templateId = currentTemplate?.id ?: "A",
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(Date()),
                    sets = sets,
                    notes = notes?.takeIf { it.isNotBlank() }
                )
                logEntries.add(entry)
                saveWorkoutLogs(context, logEntries)
            },
            onBack = { showNovaNotesState = false }
        )
        return
    }

    Scaffold(
        topBar = {
            if (!showFullProgress) {
                TopAppBar(
                    title = { Text("Mentzer A/B Tracker") },
                    actions = {
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier.clip(RectangleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.clip(RectangleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Progress") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                showFullProgressState.value = false
                                fullScreenExerciseIdState.value = null
                            },
                            modifier = Modifier.clip(RectangleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!showFullProgress) {
                LargeFloatingActionButton(
                    onClick = { showNovaNotesState = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Quick Log",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    )
 { padding ->
     if (!showFullProgress) {
         val scrollState = rememberScrollState()

         Column(
             modifier = Modifier
                 .padding(padding)
                 .fillMaxSize()
                 .verticalScroll(scrollState)
                 .padding(ScreenPadding),
             verticalArrangement = Arrangement.spacedBy(16.dp)
         ) {
             TemplateSelector(
                 selectedTemplateId = selectedTemplateId,
                 templates = templates,
                 onTemplateSelected = { selectedTemplateId = it },
                 onEditWorkouts = onEditWorkouts
             )

             val currentTemplate = templates.firstOrNull { it.id == selectedTemplateId }
                ?: templates.firstOrNull()
                ?: return@Column

             LogWorkoutSection(
                 template = currentTemplate,
                 exercisesById = exercisesById,
                allowPartialSessions = allowPartialSessions,
                onSave = { sets, notes ->
                     val entry = WorkoutLogEntry(
                         id = System.currentTimeMillis(),
                         templateId = currentTemplate.id,
                         date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                             .format(Date()),
                        sets = sets,
                        notes = notes?.takeIf { it.isNotBlank() }
                     )
                     logEntries.add(entry)
                     saveWorkoutLogs(context, logEntries)
                 }
             )

             ProgressSection(
                 logs = logEntries,
                 exercises = combinedExercises,
                onOpenFullScreen = { exercise ->
                    fullScreenExerciseIdState.value = exercise.id
                    showFullProgressState.value = true
                }
             )
         }
        } else {
            FullProgressScreen(
                logs = logEntries,
                exercises = combinedExercises,
                modifier = Modifier
                    .padding(padding)
                .fillMaxSize(),
            initialExerciseId = fullScreenExerciseIdState.value,
                onUpdateLogs = { newLogs ->
                    logEntries.clear()
                    logEntries.addAll(newLogs)
                    saveWorkoutLogs(context, logEntries)
                }
            )
        }
    }
}

