package com.example.mentzertracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.ExperimentalFoundationApi
import com.example.mentzertracker.ui.theme.MentzerTrackerTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MentzerTrackerTheme {
                AppRoot()
            }
        }
    }
}

// ---------- UI-ONLY MODELS ----------

// Point used for graph + text list
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

private val gson = Gson()

private fun hasSeenSplash(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_HAS_SEEN_SPLASH, false)
}

private fun setHasSeenSplash(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_HAS_SEEN_SPLASH, true).apply()
}

private fun loadWorkoutLogs(context: Context): List<WorkoutLogEntry> {
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
    prefs.edit().putString(KEY_WORKOUT_LOGS, json).apply()
}

private fun hasWorkoutConfig(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_WORKOUT_CONFIG, null) != null
}

private fun loadWorkoutConfig(context: Context): UserWorkoutConfig {
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
    prefs.edit().putString(KEY_WORKOUT_CONFIG, json).apply()
}

// ---------- ROOT / FLOW CONTROL ----------

@Composable
fun AppRoot() {
    val context = LocalContext.current

    var showSplash by remember { mutableStateOf(!hasSeenSplash(context)) }
    var workoutConfig by remember { mutableStateOf(loadWorkoutConfig(context)) }
    var hasConfig by remember { mutableStateOf(hasWorkoutConfig(context)) }

    when {
        showSplash -> {
            SplashScreen(
                onStart = {
                    setHasSeenSplash(context)
                    showSplash = false
                }
            )
        }

        !hasConfig -> {
            WorkoutBuilderScreen(
                initialConfig = workoutConfig,
                onDone = { newConfig ->
                    workoutConfig = newConfig
                    saveWorkoutConfig(context, newConfig)
                    hasConfig = true
                }
            )
        }

        else -> {
            WorkoutTrackerApp(
                config = workoutConfig,
                onEditWorkouts = {
                    hasConfig = false
                }
            )
        }
    }
}

// ---------- SCREENS ----------

@Composable
fun SplashScreen(onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
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
                textAlign = TextAlign.Center
            )
            Button(onClick = onStart) {
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
@Composable
fun WorkoutBuilderScreen(
    initialConfig: UserWorkoutConfig,
    onDone: (UserWorkoutConfig) -> Unit
) {
    val scrollState = rememberScrollState()

    // Start from whatever config we have (default or saved)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Build your A / B workouts",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "Pick at least 2 exercises for each workout. " +
                    "You can reuse an exercise in both A and B if you want.",
            style = MaterialTheme.typography.bodyMedium
        )

        // Workout A
        Text("Workout A", style = MaterialTheme.typography.titleMedium)
        allExercises.forEach { ex ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = aSelections[ex.id] == true,
                    onCheckedChange = { checked -> aSelections[ex.id] = checked }
                )
                Text(ex.name)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Workout B
        Text("Workout B", style = MaterialTheme.typography.titleMedium)
        allExercises.forEach { ex ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = bSelections[ex.id] == true,
                    onCheckedChange = { checked -> bSelections[ex.id] = checked }
                )
                Text(ex.name)
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
                .padding(top = 8.dp)
        ) {
            Text("Save workouts")
        }
    }
}

// ---------- MAIN TRACKER APP ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTrackerApp(
    config: UserWorkoutConfig,
    onEditWorkouts: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
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

    // Load saved logs once, then keep in memory
    val logEntries = remember {
        mutableStateListOf<WorkoutLogEntry>().apply {
            addAll(loadWorkoutLogs(context))
        }
    }

    var showAbout by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mentzer A/B Tracker") },
                actions = {
                    IconButton(onClick = { showAbout = true }) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "About MentzerTracker"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TemplateSelector(
                selectedTemplateId = selectedTemplateId,
                templates = templates,
                onTemplateSelected = { selectedTemplateId = it },
                onEditWorkouts = onEditWorkouts
            )

            val currentTemplate = templates.first { it.id == selectedTemplateId }

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
                exercises = allExercises
            )
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("MentzerTracker") },
            text = { Text("Vincent L · 2025") },
            confirmButton = {
                TextButton(onClick = {
                    uriHandler.openUri("https://github.com/VincentW2/MentzerTracker")
                    showAbout = false
                }) {
                    Text("GitHub")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("Close")
                }
            }
        )
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
                    Button(onClick = { onTemplateSelected(template.id) }) {
                        Text(template.name)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onTemplateSelected(template.id) },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text(template.name)
                    }
                }
            }
        }

        TextButton(onClick = onEditWorkouts) {
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
    val weightState = remember { mutableStateMapOf<String, String>() }
    val repsState = remember { mutableStateMapOf<String, String>() }

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
                    onValueChange = { weightState[exerciseId] = it },
                    label = { Text("lbs") },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = repsState[exerciseId] ?: "",
                    onValueChange = { repsState[exerciseId] = it },
                    label = { Text("reps") },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        Button(
            onClick = {
                val sets = template.exerciseIds.mapNotNull { id ->
                    val w = weightState[id]?.toFloatOrNull()
                    val r = repsState[id]?.toIntOrNull()
                    if (w != null && r != null) {
                        ExerciseSetEntry(
                            exerciseId = id,
                            weight = w,
                            reps = r
                        )
                    } else null
                }
                if (sets.isNotEmpty()) {
                    onSave(sets)
                    template.exerciseIds.forEach { id ->
                        weightState[id] = ""
                        repsState[id] = ""
                    }
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Save session")
        }
    }
}

// ---------- PROGRESS: DROPDOWN + SWIPEABLE GRAPH/LIST ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProgressSection(
    logs: List<WorkoutLogEntry>,
    exercises: List<Exercise>
) {
    Text("Progress", style = MaterialTheme.typography.titleMedium)

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
                            }
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
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.List,
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

@Composable
fun ExerciseDropdown(
    exercisesWithHistory: List<Exercise>,
    selected: Exercise,
    onSelectedChange: (Exercise) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { expanded = true },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
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
        onDismissRequest = { expanded = false }
    ) {
        exercisesWithHistory.forEach { ex ->
            DropdownMenuItem(
                text = { Text(ex.name) },
                onClick = {
                    onSelectedChange(ex)
                    expanded = false
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

    val primary = MaterialTheme.colorScheme.primary
    val fillColor = primary.copy(alpha = 0.25f)

    val weights = history.map { it.weight }
    val minWeight = weights.minOrNull() ?: 0f
    val maxWeight = weights.maxOrNull() ?: 0f
    val range = (maxWeight - minWeight).takeIf { it > 0f } ?: 1f

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
    ) {
        val width = size.width
        val height = size.height

        val leftPadding = 16.dp.toPx()
        val rightPadding = 16.dp.toPx()
        val topPadding = 16.dp.toPx()
        val bottomPadding = 24.dp.toPx()

        val usableWidth = width - leftPadding - rightPadding
        val usableHeight = height - topPadding - bottomPadding

        val stepX = if (history.size == 1) 0f else usableWidth / (history.size - 1)

        val points = history.mapIndexed { index, point ->
            val weight = point.weight
            val x = leftPadding + stepX * index
            val normalized = (weight - minWeight) / range
            val y = topPadding + (1f - normalized) * usableHeight
            Offset(x, y)
        }

        val axisY = topPadding + usableHeight
        drawLine(
            color = Color.Gray.copy(alpha = 0.6f),
            start = Offset(leftPadding, axisY),
            end = Offset(width - rightPadding, axisY),
            strokeWidth = 2.dp.toPx()
        )

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
