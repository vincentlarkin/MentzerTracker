package com.vincentlarkin.mentzertracker

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.vincentlarkin.mentzertracker.ui.settings.SettingsScreen
import com.vincentlarkin.mentzertracker.ui.theme.MentzerTrackerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt


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

private const val CUSTOM_EXERCISE_NAME_LIMIT = 40
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_ALLOW_PARTIAL_SESSIONS = "allow_partial_sessions"

// Point used for graphs + text list
data class SessionPoint(
    val sessionIndex: Int,
    val date: String,
    val weight: Float,
    val reps: Int
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

private fun saveWorkoutLogs(context: Context, logs: List<WorkoutLogEntry>) {
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

    val themeModeState = remember { mutableStateOf(loadThemeMode(context)) }
    val allowPartialSessionsState = remember { mutableStateOf(allowPartialSessions(context)) }
    val showSettingsState = remember { mutableStateOf(false) }
    val resetKeyState = remember { mutableStateOf(0) }

    val themeMode = themeModeState.value
    val allowPartialSessions = allowPartialSessionsState.value
    val showSettings = showSettingsState.value
    val resetKey = resetKeyState.value

    MentzerTrackerTheme(darkTheme = themeMode == ThemeMode.DARK) {
        ApplySystemBarStyle(themeMode = themeMode)

        val handleImport: (ThemeMode) -> Unit = { importedMode ->
            themeModeState.value = importedMode
            allowPartialSessionsState.value = allowPartialSessions(context)
            resetKeyState.value = resetKeyState.value + 1
            showSettingsState.value = false
        }

        if (showSettings) {
            SettingsScreen(
                themeMode = themeMode,
                isPartialSessionsAllowed = allowPartialSessions,
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
                    showSettingsState.value = false
                },
                onBack = { showSettingsState.value = false }
            )
        } else {
            key(resetKey) {
                AppRoot(
                    onOpenSettings = { showSettingsState.value = true },
                    onImportBackup = handleImport,
                    allowPartialSessions = allowPartialSessions
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
    onOpenSettings: () -> Unit,
    onImportBackup: (ThemeMode) -> Unit,
    allowPartialSessions: Boolean
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
            SplashScreen(
                onStart = {
                    setHasSeenSplash(context)
                    showSplashState.value = false
                },
                onOpenSettings = onOpenSettings,
                onImportBackup = onImportBackup
            )
        }

        editingConfig || !hasConfig -> {
            WorkoutBuilderScreen(
                initialConfig = workoutConfig,
                onDone = { newConfig ->
                    workoutConfigState.value = newConfig
                    saveWorkoutConfig(context, newConfig)
                    hasConfigState.value = true
                    editingConfigState.value = false
                },
                showBack = editingConfig && hasConfig,
                onBack = { editingConfigState.value = false },
                onOpenSettings = onOpenSettings
            )
        }

        else -> {
            WorkoutTrackerApp(
                config = workoutConfig,
                onEditWorkouts = { editingConfigState.value = true },
                onOpenSettings = onOpenSettings,
                allowPartialSessions = allowPartialSessions
            )
        }
    }
}




// ---------- SCREENS ----------

@Composable
fun SplashScreen(
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
    onImportBackup: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val showImportConfirm = remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()
                            ?.use { it.readText() }
                    } ?: error("Unable to read backup file.")
                    importBackupFromJson(context, json)
                }
                val message = if (result.isSuccess) {
                    "Import complete. Loading backup..."
                } else {
                    "Import failed: ${result.exceptionOrNull()?.localizedMessage ?: "unknown error"}"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                if (result.isSuccess) {
                    onImportBackup(result.getOrThrow())
                }
            }
        } else {
            Toast.makeText(context, "Import canceled", Toast.LENGTH_SHORT).show()
        }
    }

    if (showImportConfirm.value) {
        AlertDialog(
            onDismissRequest = { showImportConfirm.value = false },
            title = { Text("Import backup?") },
            text = {
                Text(
                    "Importing a backup replaces all workouts, logs, and preferences on this device. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportConfirm.value = false
                        importLauncher.launch(arrayOf("application/json"))
                    },
                    shape = RectangleShape
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImportConfirm.value = false },
                    shape = RectangleShape
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)   // <- force our theme bg
            .padding(24.dp)
    ) {
        // settings top-right
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(RectangleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "MentzerTracker",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Welcome to MentzerTracker.\nTrack your A/B workouts Mentzer-style.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(onClick = onStart, shape = RectangleShape) {
                Text("Start")
            }
            OutlinedButton(
                onClick = { showImportConfirm.value = true },
                shape = RectangleShape
            ) {
                Text("Import backup")
            }
        }
    }
}




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


// ---------- MAIN TRACKER APP (INCLUDING FULL PROGRESS NAV) ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTrackerApp(
    config: UserWorkoutConfig,
    onEditWorkouts: () -> Unit,
    onOpenSettings: () -> Unit,
    allowPartialSessions: Boolean
) {
    val context = LocalContext.current
    var selectedTemplateId by remember { mutableStateOf("A") }

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

    if (showFullProgress) {
        BackHandler {
            showFullProgressState.value = false
            fullScreenExerciseIdState.value = null
        }
    }

    Scaffold(
        topBar = {
            if (!showFullProgress) {
                TopAppBar(
                    title = { Text("Mentzer A/B Tracker") },
                    actions = {
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
                 onSave = { sets ->
                     val entry = WorkoutLogEntry(
                         id = System.currentTimeMillis(),
                         templateId = currentTemplate.id,
                         date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                             .format(Date()),
                         sets = sets
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

// ---------- TEMPLATE SELECTOR (A / B) ----------

@Composable
fun TemplateSelector(
    selectedTemplateId: String,
    templates: List<WorkoutTemplate>,
    onTemplateSelected: (String) -> Unit,
    onEditWorkouts: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            templates.forEach { template ->
                val isSelected = template.id == selectedTemplateId
                if (isSelected) {
                    Button(
                        onClick = { onTemplateSelected(template.id) },
                        shape = RectangleShape
                    ) {
                        Text(template.name)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onTemplateSelected(template.id) },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        shape = RectangleShape
                    ) {
                        Text(template.name)
                    }
                }
            }
        }

        TextButton(onClick = onEditWorkouts, shape = RectangleShape) {
            Text("Edit workouts")
        }
    }
}

// ---------- LOGGING A WORKOUT ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogWorkoutSection(
    template: WorkoutTemplate,
    exercisesById: Map<String, Exercise>,
    allowPartialSessions: Boolean,
    onSave: (List<ExerciseSetEntry>) -> Unit
) {
    Text("Log ${template.name}", style = MaterialTheme.typography.titleMedium)

    val weightState = remember { mutableStateMapOf<String, String>() }
    val repsState = remember { mutableStateMapOf<String, String>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(allowPartialSessions) {
        errorMessage = null
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        template.exerciseIds.forEach { exerciseId ->
            val exercise = exercisesById[exerciseId]
                ?: return@forEach

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    exercise.name,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = weightState[exerciseId] ?: "",
                    onValueChange = { input ->
                        // allow digits + decimal point
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        weightState[exerciseId] = filtered
                    },
                    label = { Text("lbs") },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = repsState[exerciseId] ?: "",
                    onValueChange = { input ->
                        // digits only
                        val filtered = input.filter { it.isDigit() }
                        repsState[exerciseId] = filtered
                    },
                    label = { Text("reps") },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        Button(
            onClick = {
                var hasInvalidEntry = false
                val sets = mutableListOf<ExerciseSetEntry>()

                template.exerciseIds.forEach { id ->
                    val wStr = weightState[id]?.trim().orEmpty()
                    val rStr = repsState[id]?.trim().orEmpty()
                    val w = wStr.toFloatOrNull()
                    val r = rStr.toIntOrNull()

                    if (allowPartialSessions) {
                        when {
                            wStr.isEmpty() && rStr.isEmpty() -> Unit
                            w == null || r == null -> hasInvalidEntry = true
                            else -> sets.add(
                                ExerciseSetEntry(
                                    exerciseId = id,
                                    weight = w,
                                    reps = r
                                )
                            )
                        }
                    } else {
                        if (w == null || r == null) {
                            hasInvalidEntry = true
                        } else {
                            sets.add(
                                ExerciseSetEntry(
                                    exerciseId = id,
                                    weight = w,
                                    reps = r
                                )
                            )
                        }
                    }
                }

                val canSave = when {
                    !allowPartialSessions && (hasInvalidEntry || sets.size != template.exerciseIds.size) -> {
                        errorMessage =
                            "Please enter numeric weight and reps for all exercises before saving."
                        false
                    }

                    allowPartialSessions && hasInvalidEntry -> {
                        errorMessage =
                            "Enter weight and reps together for the exercises you track, or leave them blank."
                        false
                    }

                    allowPartialSessions && sets.isEmpty() -> {
                        errorMessage = "Add at least one exercise entry before saving."
                        false
                    }

                    sets.isEmpty() -> {
                        errorMessage =
                            "Please enter numeric weight and reps for all exercises before saving."
                        false
                    }

                    else -> {
                        errorMessage = null
                        true
                    }
                }

                if (canSave) {
                    onSave(sets)

                    // Clear fields after save
                    template.exerciseIds.forEach { id ->
                        weightState[id] = ""
                        repsState[id] = ""
                    }
                }
            },
            modifier = Modifier.padding(top = 8.dp),
            shape = RectangleShape
        ) {
            Text("Save session")
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ---------- COMPACT PROGRESS SECTION (IN MAIN SCREEN) ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable

fun ProgressSection(
    logs: List<WorkoutLogEntry>,
    exercises: List<Exercise>,
    onOpenFullScreen: (Exercise) -> Unit
) {
    if (logs.isEmpty()) {
        Text("No sessions logged yet.")
        return
    }

    val histories: Map<Exercise, List<SessionPoint>> =
        exercises.associateWith { ex ->
            logs.flatMapIndexed { index, log ->
                log.sets
                    .filter { it.exerciseId == ex.id }
                    .map { setEntry ->
                        SessionPoint(
                            sessionIndex = index + 1,
                            date = log.date,
                            weight = setEntry.weight,
                            reps = setEntry.reps
                        )
                    }
            }
        }.filterValues { it.isNotEmpty() }

    if (histories.isEmpty()) {
        Text("No data for any exercises yet.")
        return
    }

    var selectedExercise by remember { mutableStateOf(histories.keys.first()) }

    if (!histories.containsKey(selectedExercise)) {
        selectedExercise = histories.keys.first()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Progress", style = MaterialTheme.typography.titleMedium)
        TextButton(
            onClick = { onOpenFullScreen(selectedExercise) },
            shape = RectangleShape
        ) {
            Text("Full screen")
        }
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExerciseDropdown(
            exercisesWithHistory = histories.keys.toList(),
            selected = selectedExercise,
            onSelectedChange = { selectedExercise = it }
        )

        Spacer(Modifier.height(4.dp))

        val history = histories[selectedExercise] ?: emptyList()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedExercise.name,
                        style = MaterialTheme.typography.titleSmall
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val isGraph = pagerState.currentPage == 0

                        IconButton(
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(0) }
                            },
                            modifier = Modifier.clip(RectangleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BarChart,
                                contentDescription = "Graph view",
                                tint = if (isGraph)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        IconButton(
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(1) }
                            },
                            modifier = Modifier.clip(RectangleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = "List view",
                                tint = if (!isGraph)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> ExerciseGraphPage(history = history)
                        1 -> ExerciseHistoryList(history = history)
                    }
                }
            }
        }
    }
}

// ---------- SHARED DROPDOWNS / SMALL COMPOSABLES ----------

@Composable
fun ExerciseDropdown(
    exercisesWithHistory: List<Exercise>,
    selected: Exercise,
    onSelectedChange: (Exercise) -> Unit
) {
    val expandedState = remember { mutableStateOf(false) }
    val expanded = expandedState.value

    OutlinedButton(
        onClick = { expandedState.value = true },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape
    ) {
        Text(
            text = selected.name,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
        Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = "Choose exercise"
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expandedState.value = false }
    ) {
        exercisesWithHistory.forEach { ex ->
            DropdownMenuItem(
                text = { Text(ex.name) },
                onClick = {
                    onSelectedChange(ex)
                    expandedState.value = false
                }
            )
        }
    }
}

@Composable
fun ExerciseGraphPage(
    history: List<SessionPoint>
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ExerciseLineChart(
            history = history,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        if (history.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(history) { point ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = friendlyDate(point.date),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "${point.weight.toInt()} lbs",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${point.reps} rep" +
                                    if (point.reps == 1) "" else "s",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// ---------- GRAPH + TEXT LIST IMPLEMENTATIONS ----------
private fun paddedMaxWeight(rawMax: Float): Float {
    if (rawMax <= 0f) return 50f
    val rounded = (kotlin.math.ceil(rawMax / 50f) * 50f)
    return maxOf(150f, rounded)   // baseline “ceiling”
}

@Composable
fun ExerciseLineChart(
    history: List<SessionPoint>,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No data yet for this exercise.")
        }
        return
    }

    val orderedHistory = remember(history) {
        history.sortedWith(
            compareBy(
                { parseIsoDateMillis(it.date) ?: Long.MAX_VALUE },
                { it.sessionIndex }
            )
        )
    }

    val weights = orderedHistory.map { it.weight }
    val rawMax = weights.maxOrNull() ?: 0f
    val yMax = paddedMaxWeight(rawMax)
    val ticks = (0..4).map { i -> yMax * i / 4f }   // 0, 25%, 50%, 75%, 100%

    val primary = MaterialTheme.colorScheme.primary
    val fillColor = primary.copy(alpha = 0.25f)
    val gridColor = Color.Gray.copy(alpha = 0.25f)
    val tooltipContainer = MaterialTheme.colorScheme.surfaceVariant
    val tooltipBorder = MaterialTheme.colorScheme.outline
    val tooltipTextColor = MaterialTheme.colorScheme.onSurface
    val chartBackground = MaterialTheme.colorScheme.background

    val density = LocalDensity.current
    var highlightedIndex by remember(orderedHistory) { mutableStateOf<Int?>(null) }
    var pointPositions by remember(orderedHistory) { mutableStateOf(emptyList<Offset>()) }
    var canvasSize by remember(orderedHistory) { mutableStateOf(Size.Zero) }

    Row(
        modifier = modifier
    ) {
        // Y-axis labels on the left
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            // top to bottom (max → 0)
            for (value in ticks.reversed()) {
                Text(
                    text = value.toInt().toString(),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.End
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(orderedHistory) {
                        detectTapGestures { offset ->
                            val points = pointPositions
                            if (points.isEmpty()) {
                                highlightedIndex = null
                                return@detectTapGestures
                            }
                            val threshold = with(density) { 32.dp.toPx() }
                            val nearest = points
                                .mapIndexed { index, pt ->
                                    val dx = pt.x - offset.x
                                    val dy = pt.y - offset.y
                                    index to (dx * dx + dy * dy)
                                }
                                .minByOrNull { it.second }
                            if (nearest != null && nearest.second <= threshold * threshold) {
                                highlightedIndex = nearest.first
                            } else {
                                highlightedIndex = null
                            }
                        }
                    }
            ) {
                canvasSize = size

                val width = size.width
                val height = size.height

                val leftPadding = 16.dp.toPx()
                val rightPadding = 12.dp.toPx()
                val topPadding = 16.dp.toPx()
                val bottomPadding = 24.dp.toPx()

                val usableWidth = width - leftPadding - rightPadding
                val usableHeight = height - topPadding - bottomPadding

                val stepX = if (orderedHistory.size == 1) 0f else usableWidth / (orderedHistory.size - 1)

                fun yForWeight(w: Float): Float {
                    val normalized = (w / yMax).coerceIn(0f, 1f)
                    return topPadding + (1f - normalized) * usableHeight
                }

                val points = orderedHistory.mapIndexed { index, point ->
                    val x = leftPadding + stepX * index
                    val y = yForWeight(point.weight)
                    Offset(x, y)
                }
                pointPositions = points

                // Horizontal grid lines
                ticks.forEach { tickValue ->
                    val y = yForWeight(tickValue)
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPadding, y),
                        end = Offset(width - rightPadding, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // X-axis
                val axisY = yForWeight(0f)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.6f),
                    start = Offset(leftPadding, axisY),
                    end = Offset(width - rightPadding, axisY),
                    strokeWidth = 2.dp.toPx()
                )

                // Area + line
                if (points.size >= 2) {
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }

                    val fillPath = Path().apply {
                        moveTo(points.first().x, axisY)
                        for (pt in points) {
                            lineTo(pt.x, pt.y)
                        }
                        lineTo(points.last().x, axisY)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        color = fillColor
                    )

                    drawPath(
                        path = linePath,
                        color = primary,
                        style = Stroke(
                            width = 4.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                val selectedIndex = highlightedIndex

                // Points
                points.forEachIndexed { index, pt ->
                    val isSelected = selectedIndex == index
                    val outerRadius = if (isSelected) 7.dp else 6.dp
                    val innerRadius = if (isSelected) 4.dp else 3.dp
                    drawCircle(
                        color = primary,
                        radius = outerRadius.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = if (isSelected) chartBackground else Color.Black,
                        radius = innerRadius.toPx(),
                        center = pt
                    )
                }
            }

            val selectedIndex = highlightedIndex
            if (selectedIndex != null) {
                val point = pointPositions.getOrNull(selectedIndex)
                val session = orderedHistory.getOrNull(selectedIndex)
                if (point != null && session != null && canvasSize != Size.Zero) {
                    val tooltipWidth = 160.dp
                    val tooltipHeight = 72.dp
                    val verticalGap = 12.dp
                    val tooltipWidthPx = with(density) { tooltipWidth.toPx() }
                    val tooltipHeightPx = with(density) { tooltipHeight.toPx() }
                    val verticalGapPx = with(density) { verticalGap.toPx() }

                    val xPx = (point.x - tooltipWidthPx / 2f).coerceIn(
                        0f,
                        canvasSize.width - tooltipWidthPx
                    )
                    val yPx = (point.y - tooltipHeightPx - verticalGapPx).coerceAtLeast(0f)

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    xPx.roundToInt(),
                                    yPx.roundToInt()
                                )
                            }
                            .width(tooltipWidth)
                            .background(tooltipContainer, RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                color = tooltipBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = friendlyDate(
                                    dateStr = session.date,
                                    includeYear = orderedHistory.hasMultipleYears
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = tooltipTextColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = weightLabel(session.weight),
                                style = MaterialTheme.typography.bodySmall,
                                color = tooltipTextColor
                            )
                            Text(
                                text = "${session.reps} rep" + if (session.reps == 1) "" else "s",
                                style = MaterialTheme.typography.bodySmall,
                                color = tooltipTextColor
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun weightLabel(weight: Float): String {
    return if (weight % 1f == 0f) {
        "${weight.toInt()} lbs"
    } else {
        "${String.format(Locale.getDefault(), "%.1f", weight)} lbs"
    }
}


@Composable
fun ExerciseHistoryList(
    history: List<SessionPoint>,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No history yet.")
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(history) { point ->
            Text(
                "Session ${point.sessionIndex} (${point.date}): " +
                        "${point.weight} lbs × ${point.reps} reps"
            )
        }
    }
}

// ---------- DATE HELPER ----------

private fun friendlyDate(dateStr: String): String {
    return friendlyDate(dateStr, includeYear = false)
}

private fun friendlyDate(dateStr: String, includeYear: Boolean): String {
    return try {
        val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val pattern = if (includeYear) "MMM d, yyyy" else "MMM d"
        val outFmt = SimpleDateFormat(pattern, Locale.getDefault())
        val date = inFmt.parse(dateStr)
        if (date != null) outFmt.format(date) else dateStr
    } catch (_: Exception) {
        dateStr
    }
}

private fun parseIsoDateMillis(dateStr: String): Long? {
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)?.time
    } catch (_: Exception) {
        null
    }
}

private val List<SessionPoint>.hasMultipleYears: Boolean
    get() {
        if (isEmpty()) return false
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val years = mapNotNull {
            try {
                formatter.parse(it.date)?.let { date ->
                    SimpleDateFormat("yyyy", Locale.getDefault()).format(date)
                }
            } catch (_: Exception) {
                null
            }
        }.toSet()
        return years.size > 1
    }
