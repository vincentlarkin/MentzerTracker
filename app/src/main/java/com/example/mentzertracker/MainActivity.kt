package com.vincentlarkin.mentzertracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vincentlarkin.mentzertracker.ui.settings.SettingsScreen
import com.vincentlarkin.mentzertracker.ui.theme.MentzerTrackerTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


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

private const val KEY_THEME_MODE = "theme_mode"

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

internal val gson = Gson()

internal fun hasSeenSplash(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_HAS_SEEN_SPLASH, false)
}

private fun setHasSeenSplash(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit { putBoolean(KEY_HAS_SEEN_SPLASH, true) }
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
        gson.fromJson(json, UserWorkoutConfig::class.java) ?: defaultWorkoutConfig
    } catch (_: Exception) {
        defaultWorkoutConfig
    }
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

private fun resetAppData(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit {
        remove(KEY_HAS_SEEN_SPLASH)
        remove(KEY_WORKOUT_LOGS)
        remove(KEY_WORKOUT_CONFIG)
        remove(KEY_THEME_MODE)
    }
}

// ---------- ROOT / FLOW CONTROL ----------
@Composable
fun MentzerApp() {
    val context = LocalContext.current

    val themeModeState = remember { mutableStateOf(loadThemeMode(context)) }
    val showSettingsState = remember { mutableStateOf(false) }
    val resetKeyState = remember { mutableStateOf(0) }

    val themeMode = themeModeState.value
    val showSettings = showSettingsState.value
    val resetKey = resetKeyState.value

    MentzerTrackerTheme(darkTheme = themeMode == ThemeMode.DARK) {
        if (showSettings) {
            SettingsScreen(
                themeMode = themeMode,
                onThemeModeChange = { newMode ->
                    themeModeState.value = newMode
                    saveThemeMode(context, newMode)
                },
                onResetData = {
                    resetAppData(context)
                    themeModeState.value = loadThemeMode(context)
                    resetKeyState.value = resetKeyState.value + 1
                    showSettingsState.value = false
                },
                onBack = { showSettingsState.value = false }
            )
        } else {
            key(resetKey) {
                AppRoot(
                    onOpenSettings = { showSettingsState.value = true }
                )
            }
        }
    }
}


@Composable
fun AppRoot(
    onOpenSettings: () -> Unit
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
                onOpenSettings = onOpenSettings
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
                onOpenSettings = onOpenSettings
            )
        }
    }
}




// ---------- SCREENS ----------

@Composable
fun SplashScreen(
    onStart: () -> Unit,
    onOpenSettings: () -> Unit
) {
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

    val allIds = allExercises.map { it.id }
    val initialA = initialConfig.workoutAExerciseIds.toSet()
    val initialB = initialConfig.workoutBExerciseIds.toSet()

    val aSelections = remember {
        mutableStateMapOf<String, Boolean>().apply {
            allIds.forEach { id -> this[id] = id in initialA }
        }
    }
    val bSelections = remember {
        mutableStateMapOf<String, Boolean>().apply {
            allIds.forEach { id -> this[id] = id in initialB }
        }
    }

    var errorText by remember { mutableStateOf<String?>(null) }

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
            allExercises.forEach { ex ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = aSelections[ex.id] == true,
                        onCheckedChange = { checked -> aSelections[ex.id] = checked },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.outline,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Text(
                        ex.name,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Workout B section
            Text(
                "Workout B",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            allExercises.forEach { ex ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = bSelections[ex.id] == true,
                        onCheckedChange = { checked -> bSelections[ex.id] = checked },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.outline,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Text(
                        ex.name,
                        color = MaterialTheme.colorScheme.onBackground
                    )
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
                    val aIds = aSelections.filterValues { it }.keys.toList()
                    val bIds = bSelections.filterValues { it }.keys.toList()

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
                                    workoutBExerciseIds = bIds
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




// ---------- MAIN TRACKER APP (INCLUDING FULL PROGRESS NAV) ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTrackerApp(
    config: UserWorkoutConfig,
    onEditWorkouts: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var selectedTemplateId by remember { mutableStateOf("A") }

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
    val showFullProgress = showFullProgressState.value

    if (showFullProgress) {
        BackHandler {
            showFullProgressState.value = false
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
                            onClick = { showFullProgressState.value = false },
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
                 exercises = allExercises,
                 onOpenFullScreen = { showFullProgressState.value = true }
             )
         }
        } else {
            FullProgressScreen(
                logs = logEntries,
                exercises = allExercises,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
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
    onSave: (List<ExerciseSetEntry>) -> Unit
) {
    Text("Log ${template.name}", style = MaterialTheme.typography.titleMedium)

    val weightState = remember { mutableStateMapOf<String, String>() }
    val repsState = remember { mutableStateMapOf<String, String>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        template.exerciseIds.forEach { exerciseId ->
            val exercise = allExercises.firstOrNull { it.id == exerciseId }
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
                var hasError = false
                val sets = mutableListOf<ExerciseSetEntry>()

                template.exerciseIds.forEach { id ->
                    val wStr = weightState[id]?.trim().orEmpty()
                    val rStr = repsState[id]?.trim().orEmpty()
                    val w = wStr.toFloatOrNull()
                    val r = rStr.toIntOrNull()

                    if (wStr.isEmpty() || rStr.isEmpty() || w == null || r == null) {
                        hasError = true
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

                if (hasError || sets.isEmpty()) {
                    errorMessage =
                        "Please enter numeric weight and reps for all exercises before saving."
                } else {
                    errorMessage = null
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
    onOpenFullScreen: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Progress", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onOpenFullScreen, shape = RectangleShape) {
            Text("Full screen")
        }
    }

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

    val weights = history.map { it.weight }
    val rawMax = weights.maxOrNull() ?: 0f
    val yMax = paddedMaxWeight(rawMax)
    val ticks = (0..4).map { i -> yMax * i / 4f }   // 0, 25%, 50%, 75%, 100%

    val primary = MaterialTheme.colorScheme.primary
    val fillColor = primary.copy(alpha = 0.25f)
    val gridColor = Color.Gray.copy(alpha = 0.25f)

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

        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
        ) {
            val width = size.width
            val height = size.height

            val leftPadding = 8.dp.toPx()
            val rightPadding = 8.dp.toPx()
            val topPadding = 8.dp.toPx()
            val bottomPadding = 16.dp.toPx()

            val usableWidth = width - leftPadding - rightPadding
            val usableHeight = height - topPadding - bottomPadding

            val stepX = if (history.size == 1) 0f else usableWidth / (history.size - 1)

            fun yForWeight(w: Float): Float {
                val normalized = (w / yMax).coerceIn(0f, 1f)
                return topPadding + (1f - normalized) * usableHeight
            }

            val points = history.mapIndexed { index, point ->
                val x = leftPadding + stepX * index
                val y = yForWeight(point.weight)
                Offset(x, y)
            }

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

            // Points
            points.forEach { pt ->
                drawCircle(
                    color = primary,
                    radius = 6.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = Color.Black,
                    radius = 3.dp.toPx(),
                    center = pt
                )
            }
        }
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
    return try {
        val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outFmt = SimpleDateFormat("MMM d", Locale.getDefault())
        val date = inFmt.parse(dateStr)
        if (date != null) outFmt.format(date) else dateStr
    } catch (_: Exception) {
        dateStr
    }
}
