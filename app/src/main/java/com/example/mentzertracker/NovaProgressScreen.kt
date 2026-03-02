package com.vincentlarkin.mentzertracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

// Represents an editable session entry
private data class EditableSession(
    val logId: Long,
    val exerciseId: String,
    val setIndex: Int,
    val originalDate: String,
    val originalWeight: Float,
    val originalReps: Int
)

private enum class ProgressViewMode {
    SINGLE_EXERCISE,
    ALL_EXERCISES,
    BY_DATE
}

private data class DailyExerciseSummary(
    val exerciseName: String,
    val setCount: Int,
    val topWeight: Float,
    val bestReps: Int
)

private data class DailyWorkoutSummary(
    val date: String,
    val focusLabel: String,
    val exercises: List<DailyExerciseSummary>
)

private data class StatsSample(
    val date: String,
    val workoutId: Long,
    val weight: Float,
    val reps: Int
)

private data class WeeklyProgressStat(
    val weekStartMillis: Long,
    val weekLabel: String,
    val workoutCount: Int,
    val setCount: Int,
    val averageWeight: Float,
    val topWeight: Float,
    val totalVolume: Float,
    val topWeightDeltaFromPrevious: Float?
)

@Composable
fun NovaProgressScreen(
    logs: List<WorkoutLogEntry>,
    exercises: List<Exercise>,
    onUpdateLogs: (List<WorkoutLogEntry>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var showExercisePicker by remember { mutableStateOf(false) }
    var progressViewMode by remember { mutableStateOf(ProgressViewMode.SINGLE_EXERCISE) }
    var showDetailedStats by remember { mutableStateOf(true) }
    
    // Edit state
    var editingSession by remember { mutableStateOf<EditableSession?>(null) }
    var editDate by remember { mutableStateOf("") }
    var editWeight by remember { mutableStateOf("") }
    var editReps by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Get theme colors
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error
    
    // Build history map with log reference for editing
    data class SessionWithLog(
        val point: SessionPoint,
        val logId: Long,
        val setIndex: Int,
        val exerciseId: String
    )
    
    val historiesWithLogs: Map<Exercise, List<SessionWithLog>> = remember(logs, exercises) {
        val sessionsByExerciseId = mutableMapOf<String, MutableList<SessionWithLog>>()

        logs.forEachIndexed { index, log ->
            log.sets.forEachIndexed { setIdx, setEntry ->
                val bucket = sessionsByExerciseId.getOrPut(setEntry.exerciseId) { mutableListOf() }
                bucket.add(
                    SessionWithLog(
                        point = SessionPoint(
                            sessionIndex = index + 1,
                            date = log.date,
                            weight = setEntry.weight,
                            reps = setEntry.reps,
                            notes = log.notes
                        ),
                        logId = log.id,
                        setIndex = setIdx,
                        exerciseId = setEntry.exerciseId
                    )
                )
            }
        }

        buildMap {
            exercises.forEach { exercise ->
                val history = sessionsByExerciseId[exercise.id]
                if (!history.isNullOrEmpty()) {
                    put(exercise, history.toList())
                }
            }
        }
    }
    
    // Also keep simple history map for compatibility
    val histories: Map<Exercise, List<SessionPoint>> = remember(historiesWithLogs) {
        historiesWithLogs.mapValues { (_, sessions) -> sessions.map { it.point } }
    }

    val dailyWorkoutSummaries = remember(logs, exercises) {
        buildDailyWorkoutSummaries(logs, exercises)
    }

    val overallWeeklyStats = remember(logs) {
        val samples = logs.flatMap { log ->
            log.sets.map { set ->
                StatsSample(
                    date = log.date,
                    workoutId = log.id,
                    weight = set.weight,
                    reps = set.reps
                )
            }
        }
        buildWeeklyProgressStats(samples)
    }
    
    // Auto-select first exercise with history
    LaunchedEffect(histories) {
        if (selectedExercise == null || !histories.containsKey(selectedExercise)) {
            selectedExercise = histories.keys.firstOrNull()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        if (histories.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(surfaceColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "No progress yet",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = onBackgroundColor
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Log workouts to see your progress",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = onSurfaceVariant
                        )
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp)
            ) {
                // Header
                Text(
                    "Progress",
                    style = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        color = onBackgroundColor
                    ),
                    modifier = Modifier.padding(top = 16.dp, bottom = 20.dp)
                )
                
                // Exercise picker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(surfaceColor)
                        .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable { showExercisePicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            when (progressViewMode) {
                                ProgressViewMode.ALL_EXERCISES -> "📊 All Exercises"
                                ProgressViewMode.BY_DATE -> "🗓 See By Date"
                                ProgressViewMode.SINGLE_EXERCISE -> selectedExercise?.name ?: "Select exercise"
                            },
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (progressViewMode == ProgressViewMode.SINGLE_EXERCISE) {
                                    onSurfaceColor
                                } else {
                                    primaryColor
                                }
                            )
                        )
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = "Select",
                            tint = onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                DetailedStatsToggleRow(
                    checked = showDetailedStats,
                    onCheckedChange = { showDetailedStats = it },
                    surfaceColor = surfaceColor,
                    outlineColor = outlineColor,
                    textColor = onSurfaceColor,
                    secondaryColor = onSurfaceVariant,
                    accentColor = primaryColor
                )

                Spacer(Modifier.height(20.dp))
                
                // Graph card and list mode
                when (progressViewMode) {
                    ProgressViewMode.ALL_EXERCISES -> {
                        val latestByExercise = remember(historiesWithLogs) {
                            historiesWithLogs.mapNotNull { (exercise, sessions) ->
                                val latestSession = sessions.maxWithOrNull(
                                    compareBy<SessionWithLog> {
                                        parseIsoDateMillis(it.point.date) ?: Long.MIN_VALUE
                                    }.thenBy { it.point.sessionIndex }
                                )
                                latestSession?.let { exercise to it.point }
                            }.sortedBy { it.first.name }
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(surfaceColor)
                                        .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Text(
                                            "Overall Progress",
                                            style = TextStyle(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = onSurfaceVariant
                                            ),
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        MultiExerciseLineChart(
                                            exerciseData = histories,
                                            modifier = Modifier.fillMaxSize(),
                                            primaryColor = primaryColor,
                                            backgroundColor = backgroundColor,
                                            textColor = onSurfaceVariant,
                                            gridColor = outlineColor
                                        )
                                    }
                                }
                            }
                            item {
                                ExerciseLegend(
                                    exercises = histories.keys.toList(),
                                    surfaceColor = surfaceVariant,
                                    textColor = onSurfaceColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (showDetailedStats) {
                                item {
                                    WeeklyPeriodStatsSection(
                                        title = "Weekly Trend (All Exercises)",
                                        subtitle = "Top-weight change week over week",
                                        stats = overallWeeklyStats,
                                        surfaceColor = surfaceColor,
                                        surfaceVariant = surfaceVariant,
                                        outlineColor = outlineColor,
                                        textColor = onSurfaceColor,
                                        secondaryColor = onSurfaceVariant,
                                        positiveColor = tertiaryColor,
                                        negativeColor = errorColor
                                    )
                                }
                            }
                            item {
                                Text(
                                    "Latest by Exercise",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = onBackgroundColor
                                    )
                                )
                            }
                            items(
                                items = latestByExercise,
                                key = { (exercise, _) -> exercise.id }
                            ) { (exercise, latestPoint) ->
                                LatestExerciseRow(
                                    exerciseName = exercise.name,
                                    point = latestPoint,
                                    surfaceColor = surfaceVariant,
                                    primaryColor = primaryColor,
                                    textColor = onSurfaceColor,
                                    secondaryColor = onSurfaceVariant
                                )
                            }
                            item { Spacer(Modifier.height(8.dp)) }
                        }
                    }

                    ProgressViewMode.BY_DATE -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(surfaceColor)
                                        .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                        .padding(16.dp)
                                ) {
                                    Column {
                                        Text(
                                            "Overall Progress",
                                            style = TextStyle(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = onSurfaceVariant
                                            ),
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )

                                        MultiExerciseLineChart(
                                            exerciseData = histories,
                                            modifier = Modifier.fillMaxSize(),
                                            primaryColor = primaryColor,
                                            backgroundColor = backgroundColor,
                                            textColor = onSurfaceVariant,
                                            gridColor = outlineColor
                                        )
                                    }
                                }
                            }
                            item {
                                ExerciseLegend(
                                    exercises = histories.keys.toList(),
                                    surfaceColor = surfaceVariant,
                                    textColor = onSurfaceColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (showDetailedStats) {
                                item {
                                    WeeklyPeriodStatsSection(
                                        title = "Weekly Period Stats",
                                        subtitle = "Period-by-period training load",
                                        stats = overallWeeklyStats,
                                        surfaceColor = surfaceColor,
                                        surfaceVariant = surfaceVariant,
                                        outlineColor = outlineColor,
                                        textColor = onSurfaceColor,
                                        secondaryColor = onSurfaceVariant,
                                        positiveColor = tertiaryColor,
                                        negativeColor = errorColor
                                    )
                                }
                            }
                            item {
                                Text(
                                    "Workouts by Date",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = onBackgroundColor
                                    )
                                )
                            }
                            items(
                                items = dailyWorkoutSummaries,
                                key = { summary -> summary.date }
                            ) { summary ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(surfaceVariant)
                                        .border(1.dp, outlineColor.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(1.dp)
                                                .background(primaryColor.copy(alpha = 0.45f))
                                        )
                                        Text(
                                            text = formatWorkoutDate(summary.date),
                                            style = TextStyle(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = primaryColor
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(1.dp)
                                                .background(primaryColor.copy(alpha = 0.45f))
                                        )
                                    }
                                    Text(
                                        text = summary.focusLabel,
                                        style = TextStyle(
                                            fontSize = 12.sp,
                                            color = onSurfaceVariant
                                        )
                                    )
                                    summary.exercises.forEach { dayExercise ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(surfaceColor)
                                                .border(
                                                    1.dp,
                                                    outlineColor.copy(alpha = 0.2f),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = dayExercise.exerciseName,
                                                style = TextStyle(
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = onSurfaceColor
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(999.dp))
                                                        .background(primaryColor.copy(alpha = 0.12f))
                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = "${dayExercise.setCount} set" + if (dayExercise.setCount == 1) "" else "s",
                                                        style = TextStyle(
                                                            fontSize = 11.sp,
                                                            color = primaryColor
                                                        )
                                                    )
                                                }
                                                Text(
                                                    text = "${formatCompactWeight(dayExercise.topWeight)} × ${dayExercise.bestReps}",
                                                    style = TextStyle(
                                                        fontSize = 12.sp,
                                                        color = onSurfaceVariant
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(8.dp)) }
                        }
                    }

                    ProgressViewMode.SINGLE_EXERCISE -> {
                        selectedExercise?.let { exercise ->
                            val history = histories[exercise] ?: emptyList()
                            val sessionsWithLogs = historiesWithLogs[exercise] ?: emptyList()
                            val recentSessions = remember(sessionsWithLogs) {
                                sessionsWithLogs.sortedWith(
                                    compareByDescending<SessionWithLog> {
                                        parseIsoDateMillis(it.point.date) ?: Long.MIN_VALUE
                                    }.thenByDescending { it.point.sessionIndex }
                                )
                            }
                            val weeklyStats = remember(exercise, sessionsWithLogs) {
                                buildWeeklyProgressStats(
                                    sessionsWithLogs.map { session ->
                                        StatsSample(
                                            date = session.point.date,
                                            workoutId = session.logId,
                                            weight = session.point.weight,
                                            reps = session.point.reps
                                        )
                                    }
                                )
                            }

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(240.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(surfaceColor)
                                            .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                            .padding(16.dp)
                                    ) {
                                        if (history.isEmpty()) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "No data yet",
                                                    style = TextStyle(
                                                        fontSize = 15.sp,
                                                        color = onSurfaceVariant
                                                    )
                                                )
                                            }
                                        } else {
                                            NovaLineChart(
                                                history = history,
                                                modifier = Modifier.fillMaxSize(),
                                                primaryColor = primaryColor,
                                                backgroundColor = backgroundColor,
                                                surfaceColor = surfaceColor,
                                                textColor = onSurfaceVariant,
                                                gridColor = outlineColor
                                            )
                                        }
                                    }
                                }

                                if (showDetailedStats) {
                                    item {
                                        SingleExerciseStatBoard(
                                            exerciseName = exercise.name,
                                            stats = weeklyStats,
                                            totalSessions = sessionsWithLogs.size,
                                            surfaceColor = surfaceColor,
                                            surfaceVariant = surfaceVariant,
                                            outlineColor = outlineColor,
                                            textColor = onSurfaceColor,
                                            secondaryColor = onSurfaceVariant,
                                            positiveColor = tertiaryColor,
                                            negativeColor = errorColor
                                        )
                                    }
                                }

                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Recent",
                                            style = TextStyle(
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = onBackgroundColor
                                            )
                                        )
                                        Text(
                                            "Tap to edit",
                                            style = TextStyle(
                                                fontSize = 12.sp,
                                                color = onSurfaceVariant
                                            )
                                        )
                                    }
                                }

                                if (recentSessions.isEmpty()) {
                                    item {
                                        Text(
                                            "No recent sets yet.",
                                            style = TextStyle(
                                                fontSize = 13.sp,
                                                color = onSurfaceVariant
                                            )
                                        )
                                    }
                                } else {
                                    items(
                                        items = recentSessions,
                                        key = { session -> "${session.logId}_${session.setIndex}" }
                                    ) { sessionWithLog ->
                                        SessionRow(
                                            point = sessionWithLog.point,
                                            surfaceColor = surfaceVariant,
                                            primaryColor = primaryColor,
                                            textColor = onSurfaceColor,
                                            secondaryColor = onSurfaceVariant,
                                            onClick = {
                                                editingSession = EditableSession(
                                                    logId = sessionWithLog.logId,
                                                    exerciseId = sessionWithLog.exerciseId,
                                                    setIndex = sessionWithLog.setIndex,
                                                    originalDate = sessionWithLog.point.date,
                                                    originalWeight = sessionWithLog.point.weight,
                                                    originalReps = sessionWithLog.point.reps
                                                )
                                                editDate = sessionWithLog.point.date
                                                editWeight = sessionWithLog.point.weight.toString()
                                                editReps = sessionWithLog.point.reps.toString()
                                            }
                                        )
                                    }
                                }

                                item { Spacer(Modifier.height(8.dp)) }
                            }
                        }
                    }
                }
            }
        }
        
        // Exercise picker dialog
        if (showExercisePicker) {
            ExercisePickerDialog(
                exercises = histories.keys.toList(),
                selected = selectedExercise,
                onSelect = {
                    selectedExercise = it
                    progressViewMode = ProgressViewMode.SINGLE_EXERCISE
                    showExercisePicker = false
                },
                onDismiss = { showExercisePicker = false },
                surfaceColor = surfaceColor,
                primaryColor = primaryColor,
                textColor = onSurfaceColor,
                outlineColor = outlineColor,
                surfaceVariant = surfaceVariant,
                showAllOption = true,
                isAllSelected = progressViewMode == ProgressViewMode.ALL_EXERCISES,
                onAllSelected = {
                    progressViewMode = ProgressViewMode.ALL_EXERCISES
                    showExercisePicker = false
                },
                showByDateOption = true,
                isByDateSelected = progressViewMode == ProgressViewMode.BY_DATE,
                onByDateSelected = {
                    progressViewMode = ProgressViewMode.BY_DATE
                    showExercisePicker = false
                }
            )
        }
        
        // Edit session dialog
        editingSession?.let { session ->
            EditSessionDialog(
                date = editDate,
                weight = editWeight,
                reps = editReps,
                onDateChange = { editDate = it },
                onWeightChange = { editWeight = it },
                onRepsChange = { editReps = it },
                onShowDatePicker = { showDatePicker = true },
                onDismiss = { editingSession = null },
                onSave = {
                    val newWeight = editWeight.toFloatOrNull()
                    val newReps = editReps.toIntOrNull()
                    
                    if (newWeight != null && newReps != null) {
                        val updatedLogs = logs.map { log ->
                            if (log.id == session.logId) {
                                val updatedSets = log.sets.mapIndexed { idx, set ->
                                    if (idx == session.setIndex && set.exerciseId == session.exerciseId) {
                                        set.copy(weight = newWeight, reps = newReps)
                                    } else set
                                }
                                log.copy(date = editDate, sets = updatedSets)
                            } else log
                        }
                        onUpdateLogs(updatedLogs)
                    }
                    editingSession = null
                },
                surfaceColor = surfaceColor,
                primaryColor = primaryColor,
                textColor = onSurfaceColor,
                secondaryColor = onSurfaceVariant,
                outlineColor = outlineColor
            )
        }
        
        // Date picker dialog
        if (showDatePicker) {
            DatePickerDialogWrapper(
                initialDate = editDate,
                onDateSelected = { editDate = it },
                onDismiss = { showDatePicker = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogWrapper(
    initialDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val initialMillis = try {
        formatter.parse(initialDate)?.time
    } catch (e: Exception) {
        null
    }
    
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        onDateSelected(formatter.format(Date(millis)))
                    }
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun EditSessionDialog(
    date: String,
    weight: String,
    reps: String,
    onDateChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onShowDatePicker: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    surfaceColor: Color,
    primaryColor: Color,
    textColor: Color,
    secondaryColor: Color,
    outlineColor: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        title = {
            Text(
                "Edit Entry",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date field (read-only, tap to open picker)
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowDatePicker() },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Change date",
                            modifier = Modifier.clickable { onShowDatePicker() }
                        )
                    }
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { value ->
                            val filtered = value.filter { it.isDigit() || it == '.' }
                            onWeightChange(filtered)
                        },
                        label = { Text("Weight (lbs)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { value ->
                            val filtered = value.filter { it.isDigit() }
                            onRepsChange(filtered)
                        },
                        label = { Text("Reps") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = weight.toFloatOrNull() != null && reps.toIntOrNull() != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = secondaryColor)
            }
        }
    )
}

@Composable
private fun SessionRow(
    point: SessionPoint,
    surfaceColor: Color,
    primaryColor: Color,
    textColor: Color,
    secondaryColor: Color,
    onClick: (() -> Unit)? = null
) {
    val dateFormatter = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val inputFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    
    val formattedDate = try {
        val date = inputFormatter.parse(point.date)
        date?.let { dateFormatter.format(it) } ?: point.date
    } catch (e: Exception) {
        point.date
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                formattedDate,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            )
        }
        
        Text(
            "${point.weight.toInt()}lbs",
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor
            )
        )
        
        Spacer(Modifier.width(12.dp))
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(secondaryColor.copy(alpha = 0.1f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                "${point.reps} reps",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = secondaryColor
                )
            )
        }
        
        if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit",
                tint = secondaryColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun LatestExerciseRow(
    exerciseName: String,
    point: SessionPoint,
    surfaceColor: Color,
    primaryColor: Color,
    textColor: Color,
    secondaryColor: Color
) {
    val dateFormatter = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val inputFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val formattedDate = try {
        val date = inputFormatter.parse(point.date)
        date?.let { dateFormatter.format(it) } ?: point.date
    } catch (e: Exception) {
        point.date
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                exerciseName,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            )
            Text(
                formattedDate,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = secondaryColor
                )
            )
        }

        Text(
            "${point.weight.toInt()} lbs",
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor
            )
        )

        Spacer(Modifier.width(10.dp))

        Text(
            "${point.reps} reps",
            style = TextStyle(
                fontSize = 13.sp,
                color = secondaryColor
            )
        )
    }
}

@Composable
private fun DetailedStatsToggleRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    surfaceColor: Color,
    outlineColor: Color,
    textColor: Color,
    secondaryColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(surfaceColor)
            .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Detailed stats",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            )
            Text(
                if (checked) "Week-over-week gain/loss is visible"
                else "Turn on to view weekly period analytics",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = secondaryColor
                )
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        if (checked) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "ON",
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                )
            }
        }
    }
}

@Composable
private fun WeeklyPeriodStatsSection(
    title: String,
    subtitle: String,
    stats: List<WeeklyProgressStat>,
    surfaceColor: Color,
    surfaceVariant: Color,
    outlineColor: Color,
    textColor: Color,
    secondaryColor: Color,
    positiveColor: Color,
    negativeColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            title,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        )
        Text(
            subtitle,
            style = TextStyle(
                fontSize = 12.sp,
                color = secondaryColor
            )
        )

        if (stats.isEmpty()) {
            Text(
                "Need more workout history to calculate period stats.",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = secondaryColor
                )
            )
        } else {
            val latestDelta = stats.firstOrNull()?.topWeightDeltaFromPrevious
            val deltaText = latestDelta?.let { "${formatSignedWeightDelta(it)} vs previous week" }
                ?: "Baseline set. Add another week to compare."
            val deltaColor = when {
                latestDelta == null -> secondaryColor
                latestDelta > 0f -> positiveColor
                latestDelta < 0f -> negativeColor
                else -> secondaryColor
            }

            Text(
                deltaText,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = deltaColor
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                stats.take(4).forEach { stat ->
                    WeeklyPeriodStatCard(
                        stat = stat,
                        surfaceVariant = surfaceVariant,
                        outlineColor = outlineColor,
                        textColor = textColor,
                        secondaryColor = secondaryColor,
                        positiveColor = positiveColor,
                        negativeColor = negativeColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (stats.size > 4) {
                Text(
                    "Showing latest 4 weeks",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = secondaryColor
                    )
                )
            }
        }
    }
}

@Composable
private fun WeeklyPeriodStatCard(
    stat: WeeklyProgressStat,
    surfaceVariant: Color,
    outlineColor: Color,
    textColor: Color,
    secondaryColor: Color,
    positiveColor: Color,
    negativeColor: Color,
    modifier: Modifier = Modifier
) {
    val delta = stat.topWeightDeltaFromPrevious
    val deltaText = delta?.let { formatSignedWeightDelta(it) } ?: "Baseline"
    val deltaColor = when {
        delta == null -> secondaryColor
        delta > 0f -> positiveColor
        delta < 0f -> negativeColor
        else -> secondaryColor
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(surfaceVariant)
            .border(1.dp, outlineColor.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            stat.weekLabel,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        )
        Text(
            "Top ${formatCompactWeight(stat.topWeight)}",
            style = TextStyle(
                fontSize = 13.sp,
                color = textColor
            )
        )
        Text(
            "Avg ${formatCompactWeight(stat.averageWeight)}",
            style = TextStyle(
                fontSize = 12.sp,
                color = secondaryColor
            )
        )
        Text(
            "${stat.workoutCount} workouts | ${stat.setCount} sets",
            style = TextStyle(
                fontSize = 11.sp,
                color = secondaryColor
            )
        )
        Text(
            "Volume ${formatCompactWeight(stat.totalVolume)}",
            style = TextStyle(
                fontSize = 11.sp,
                color = secondaryColor
            )
        )
        Text(
            deltaText,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = deltaColor
            )
        )
    }
}

@Composable
private fun SingleExerciseStatBoard(
    exerciseName: String,
    stats: List<WeeklyProgressStat>,
    totalSessions: Int,
    surfaceColor: Color,
    surfaceVariant: Color,
    outlineColor: Color,
    textColor: Color,
    secondaryColor: Color,
    positiveColor: Color,
    negativeColor: Color
) {
    val latestWeek = stats.firstOrNull()
    val previousWeek = stats.getOrNull(1)
    val fourWeekBase = stats.getOrNull(3)
    val wowDelta = latestWeek?.topWeightDeltaFromPrevious
    val fourWeekDelta = if (latestWeek != null && fourWeekBase != null) {
        latestWeek.topWeight - fourWeekBase.topWeight
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "$exerciseName stat board",
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        )
        Text(
            "Top-weight trends based on weekly periods",
            style = TextStyle(
                fontSize = 12.sp,
                color = secondaryColor
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBoardCell(
                title = "This week top",
                value = latestWeek?.let { formatCompactWeight(it.topWeight) } ?: "N/A",
                valueColor = textColor,
                surfaceVariant = surfaceVariant,
                outlineColor = outlineColor,
                secondaryColor = secondaryColor,
                modifier = Modifier.weight(1f)
            )
            StatBoardCell(
                title = "Last week top",
                value = previousWeek?.let { formatCompactWeight(it.topWeight) } ?: "N/A",
                valueColor = textColor,
                surfaceVariant = surfaceVariant,
                outlineColor = outlineColor,
                secondaryColor = secondaryColor,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBoardCell(
                title = "WoW change",
                value = wowDelta?.let { formatSignedWeightDelta(it) } ?: "N/A",
                valueColor = when {
                    wowDelta == null -> secondaryColor
                    wowDelta > 0f -> positiveColor
                    wowDelta < 0f -> negativeColor
                    else -> secondaryColor
                },
                surfaceVariant = surfaceVariant,
                outlineColor = outlineColor,
                secondaryColor = secondaryColor,
                modifier = Modifier.weight(1f)
            )
            StatBoardCell(
                title = "4-week change",
                value = fourWeekDelta?.let { formatSignedWeightDelta(it) } ?: "Need 4 weeks",
                valueColor = when {
                    fourWeekDelta == null -> secondaryColor
                    fourWeekDelta > 0f -> positiveColor
                    fourWeekDelta < 0f -> negativeColor
                    else -> secondaryColor
                },
                surfaceVariant = surfaceVariant,
                outlineColor = outlineColor,
                secondaryColor = secondaryColor,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            "Tracked sessions: $totalSessions",
            style = TextStyle(
                fontSize = 12.sp,
                color = secondaryColor
            )
        )
    }
}

@Composable
private fun StatBoardCell(
    title: String,
    value: String,
    valueColor: Color,
    surfaceVariant: Color,
    outlineColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceVariant)
            .border(1.dp, outlineColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            title,
            style = TextStyle(
                fontSize = 11.sp,
                color = secondaryColor
            )
        )
        Text(
            value,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        )
    }
}

@Composable
private fun NovaLineChart(
    history: List<SessionPoint>,
    modifier: Modifier = Modifier,
    primaryColor: Color,
    backgroundColor: Color,
    surfaceColor: Color,
    textColor: Color,
    gridColor: Color
) {
    val density = LocalDensity.current
    var highlightedIndex by remember(history) { mutableStateOf<Int?>(null) }
    var canvasSize by remember(history) { mutableStateOf(Size.Zero) }
    
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
    val ticks = (0..4).map { i -> yMax * i / 4f }
    val pointPositions = remember(orderedHistory, canvasSize, yMax, density) {
        if (canvasSize == Size.Zero) {
            emptyList()
        } else {
            val leftPadding = with(density) { 8.dp.toPx() }
            val rightPadding = with(density) { 8.dp.toPx() }
            val topPadding = with(density) { 16.dp.toPx() }
            val bottomPadding = with(density) { 8.dp.toPx() }

            val usableWidth = (canvasSize.width - leftPadding - rightPadding).coerceAtLeast(0f)
            val usableHeight = (canvasSize.height - topPadding - bottomPadding).coerceAtLeast(0f)
            val stepX = if (orderedHistory.size <= 1 || usableWidth == 0f) 0f else usableWidth / (orderedHistory.size - 1)

            fun yForWeight(weight: Float): Float {
                val normalized = (weight / yMax).coerceIn(0f, 1f)
                return topPadding + (1f - normalized) * usableHeight
            }

            orderedHistory.mapIndexed { index, point ->
                Offset(
                    x = leftPadding + stepX * index,
                    y = yForWeight(point.weight)
                )
            }
        }
    }
    
    Row(modifier = modifier) {
        // Y-axis labels
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            for (value in ticks.reversed()) {
                Text(
                    text = value.toInt().toString(),
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = textColor
                    )
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
                    .onSizeChanged { size ->
                        canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                    }
                    .pointerInput(pointPositions) {
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
                val width = size.width
                val height = size.height
                
                val leftPadding = 8.dp.toPx()
                val rightPadding = 8.dp.toPx()
                val topPadding = 16.dp.toPx()
                val bottomPadding = 8.dp.toPx()
                
                val usableHeight = height - topPadding - bottomPadding

                fun yForWeight(w: Float): Float {
                    val normalized = (w / yMax).coerceIn(0f, 1f)
                    return topPadding + (1f - normalized) * usableHeight
                }

                val points = pointPositions
                
                // Grid lines
                ticks.forEach { tickValue ->
                    val y = yForWeight(tickValue)
                    drawLine(
                        color = gridColor.copy(alpha = 0.2f),
                        start = Offset(leftPadding, y),
                        end = Offset(width - rightPadding, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                // Line + fill
                if (points.size >= 2) {
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                    
                    val axisY = yForWeight(0f)
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
                        color = primaryColor.copy(alpha = 0.15f)
                    )
                    
                    drawPath(
                        path = linePath,
                        color = primaryColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                
                // Points
                val selectedIndex = highlightedIndex
                points.forEachIndexed { index, pt ->
                    val isSelected = selectedIndex == index
                    val radius = if (isSelected) 8.dp else 5.dp
                    drawCircle(
                        color = primaryColor,
                        radius = radius.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = if (isSelected) backgroundColor else surfaceColor,
                        radius = (radius - 2.dp).toPx(),
                        center = pt
                    )
                }
            }
            
            // Tooltip
            val selectedIndex = highlightedIndex
            if (selectedIndex != null) {
                val point = pointPositions.getOrNull(selectedIndex)
                val session = orderedHistory.getOrNull(selectedIndex)
                if (point != null && session != null && canvasSize != Size.Zero) {
                    val tooltipWidth = 120.dp
                    val tooltipHeight = 60.dp
                    val tooltipWidthPx = with(density) { tooltipWidth.toPx() }
                    val tooltipHeightPx = with(density) { tooltipHeight.toPx() }
                    val verticalGapPx = with(density) { 12.dp.toPx() }
                    
                    val xPx = (point.x - tooltipWidthPx / 2f).coerceIn(
                        0f,
                        canvasSize.width - tooltipWidthPx
                    )
                    val yPx = (point.y - tooltipHeightPx - verticalGapPx).coerceAtLeast(0f)
                    
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(xPx.roundToInt(), yPx.roundToInt())
                            }
                            .width(tooltipWidth)
                            .clip(RoundedCornerShape(10.dp))
                            .background(surfaceColor)
                            .border(1.dp, gridColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                "${session.weight.toInt()} lbs",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryColor
                                )
                            )
                            Text(
                                "${session.reps} reps",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = textColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExercisePickerDialog(
    exercises: List<Exercise>,
    selected: Exercise?,
    onSelect: (Exercise) -> Unit,
    onDismiss: () -> Unit,
    surfaceColor: Color,
    primaryColor: Color,
    textColor: Color,
    outlineColor: Color,
    surfaceVariant: Color,
    showAllOption: Boolean = false,
    isAllSelected: Boolean = false,
    onAllSelected: (() -> Unit)? = null,
    showByDateOption: Boolean = false,
    isByDateSelected: Boolean = false,
    onByDateSelected: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(surfaceColor)
                .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(20.dp)
        ) {
            Text(
                "Select View",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(300.dp)
            ) {
                // "All Exercises" option
                if (showAllOption && onAllSelected != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isAllSelected) primaryColor.copy(alpha = 0.15f)
                                    else surfaceVariant
                                )
                                .clickable { onAllSelected() }
                                .padding(14.dp)
                        ) {
                            Text(
                                "📊 All Exercises",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = if (isAllSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isAllSelected) primaryColor else textColor
                                )
                            )
                        }
                    }
                }

                if (showByDateOption && onByDateSelected != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isByDateSelected) primaryColor.copy(alpha = 0.15f)
                                    else surfaceVariant
                                )
                                .clickable { onByDateSelected() }
                                .padding(14.dp)
                        ) {
                            Text(
                                "🗓 See By Date",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = if (isByDateSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isByDateSelected) primaryColor else textColor
                                )
                            )
                        }
                    }
                }
                
                items(exercises) { exercise ->
                    val isSelected = exercise == selected && !isAllSelected && !isByDateSelected
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) primaryColor.copy(alpha = 0.15f)
                                else surfaceVariant
                            )
                            .clickable { onSelect(exercise) }
                            .padding(14.dp)
                    ) {
                        Text(
                            exercise.name,
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) primaryColor else textColor
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun paddedMaxWeight(rawMax: Float): Float {
    if (rawMax <= 0f) return 50f
    val rounded = (ceil(rawMax / 50f) * 50f)
    return maxOf(150f, rounded)
}

private fun parseIsoDateMillis(dateStr: String): Long? {
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)?.time
    } catch (_: Exception) {
        null
    }
}

private fun buildWeeklyProgressStats(samples: List<StatsSample>): List<WeeklyProgressStat> {
    if (samples.isEmpty()) return emptyList()

    val groupedByWeek = samples.mapNotNull { sample ->
        val millis = parseIsoDateMillis(sample.date) ?: return@mapNotNull null
        weekStartMillis(millis) to sample
    }.groupBy({ it.first }, { it.second })

    val ascending = groupedByWeek.map { (weekStart, weekSamples) ->
        val weekEnd = weekStart + (6L * 24L * 60L * 60L * 1000L)
        val topWeight = weekSamples.maxOfOrNull { it.weight } ?: 0f
        val avgWeight = weekSamples.map { it.weight }.average().toFloat()
        val totalVolume = weekSamples.sumOf { sample -> (sample.weight * sample.reps).toDouble() }.toFloat()

        WeeklyProgressStat(
            weekStartMillis = weekStart,
            weekLabel = formatWeekLabel(weekStart, weekEnd),
            workoutCount = weekSamples.map { it.workoutId }.distinct().size,
            setCount = weekSamples.size,
            averageWeight = avgWeight,
            topWeight = topWeight,
            totalVolume = totalVolume,
            topWeightDeltaFromPrevious = null
        )
    }.sortedBy { it.weekStartMillis }

    val withDelta = ascending.mapIndexed { index, current ->
        val previous = ascending.getOrNull(index - 1)
        current.copy(
            topWeightDeltaFromPrevious = previous?.let { current.topWeight - it.topWeight }
        )
    }

    return withDelta.sortedByDescending { it.weekStartMillis }
}

private fun weekStartMillis(epochMillis: Long): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = epochMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val firstDayOfWeek = calendar.firstDayOfWeek
    while (calendar.get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) {
        calendar.add(Calendar.DAY_OF_MONTH, -1)
    }
    return calendar.timeInMillis
}

private fun formatWeekLabel(weekStartMillis: Long, weekEndMillis: Long): String {
    val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
    return "${formatter.format(Date(weekStartMillis))} - ${formatter.format(Date(weekEndMillis))}"
}

private fun buildDailyWorkoutSummaries(
    logs: List<WorkoutLogEntry>,
    exercises: List<Exercise>
): List<DailyWorkoutSummary> {
    val exerciseById = exercises.associateBy { it.id }

    return logs
        .groupBy { it.date }
        .map { (date, logsForDate) ->
            val setsForDay = logsForDate.flatMap { it.sets }
            val setsByExercise = setsForDay.groupBy { it.exerciseId }

            val summaries = setsByExercise.map { (exerciseId, sets) ->
                DailyExerciseSummary(
                    exerciseName = exerciseById[exerciseId]?.name ?: readableExerciseId(exerciseId),
                    setCount = sets.size,
                    topWeight = sets.maxOfOrNull { it.weight } ?: 0f,
                    bestReps = sets.maxOfOrNull { it.reps } ?: 0
                )
            }.sortedBy { it.exerciseName }

            val focusCounts = mutableMapOf<String, Int>()
            setsForDay.forEach { set ->
                val exercise = exerciseById[set.exerciseId]
                val group = inferMuscleGroup(
                    exerciseId = set.exerciseId,
                    exerciseName = exercise?.name ?: ""
                )
                focusCounts[group] = focusCounts.getOrDefault(group, 0) + 1
            }

            DailyWorkoutSummary(
                date = date,
                focusLabel = buildFocusLabel(focusCounts),
                exercises = summaries
            )
        }
        .sortedWith(compareByDescending<DailyWorkoutSummary> { parseIsoDateMillis(it.date) ?: Long.MIN_VALUE })
}

private fun buildFocusLabel(groupCounts: Map<String, Int>): String {
    if (groupCounts.isEmpty()) return "Focus: Mixed"

    val maxCount = groupCounts.maxOf { it.value }
    val topGroups = groupCounts
        .filterValues { it == maxCount }
        .keys
        .sorted()

    return if (topGroups.size == 1) {
        "Focus: ${topGroups.first()}"
    } else {
        "Focus: Mixed (${topGroups.joinToString("/")})"
    }
}

private fun inferMuscleGroup(exerciseId: String, exerciseName: String): String {
    val text = "$exerciseId $exerciseName".lowercase(Locale.getDefault())

    val legsKeywords = listOf("squat", "leg", "lunge", "calf", "hip thrust", "glute", "bulgarian", "romanian", "stiff")
    val chestKeywords = listOf("bench", "incline", "decline", "chest", "fly", "pec", "crossover", "push up", "dips")
    val shoulderKeywords = listOf("shoulder", "overhead", "arnold", "lateral", "rear delt", "front raise", "upright row", "ohp")
    val armKeywords = listOf("bicep", "tricep", "curl", "skull", "pushdown", "kickback", "extension")
    val backKeywords = listOf("row", "pulldown", "pull up", "chin up", "lat", "deadlift", "shrug", "face pull", "t-bar")
    val coreKeywords = listOf("crunch", "plank", "ab", "sit up", "twist", "leg raise")

    return when {
        text.containsAny(legsKeywords) -> "Legs"
        text.containsAny(chestKeywords) -> "Chest"
        text.containsAny(shoulderKeywords) -> "Shoulders"
        text.containsAny(armKeywords) -> "Arms"
        text.containsAny(backKeywords) -> "Back"
        text.containsAny(coreKeywords) -> "Core"
        else -> "Mixed"
    }
}

private fun String.containsAny(keywords: List<String>): Boolean {
    return keywords.any { keyword -> this.contains(keyword) }
}

private fun readableExerciseId(exerciseId: String): String {
    return exerciseId
        .split("_")
        .joinToString(" ") { token ->
            token.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            }
        }
}

private fun formatWorkoutDate(dateStr: String): String {
    return try {
        val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
        if (parsedDate != null) {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(parsedDate)
        } else {
            dateStr
        }
    } catch (_: Exception) {
        dateStr
    }
}

private fun formatCompactWeight(weight: Float): String {
    return if (weight % 1f == 0f) {
        "${weight.toInt()} lbs"
    } else {
        "${String.format(Locale.getDefault(), "%.1f", weight)} lbs"
    }
}

private fun formatSignedWeightDelta(delta: Float): String {
    val absolute = abs(delta)
    val formattedValue = if (absolute % 1f == 0f) {
        absolute.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", absolute)
    }
    val sign = when {
        delta > 0f -> "+"
        delta < 0f -> "-"
        else -> "0"
    }
    return if (sign == "0") {
        "0 lbs"
    } else {
        "$sign$formattedValue lbs"
    }
}

// Colors for different exercises in multi-line chart
private val exerciseColors = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFFEC4899), // Pink
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber
    Color(0xFF8B5CF6), // Violet
    Color(0xFF06B6D4), // Cyan
    Color(0xFFF97316), // Orange
    Color(0xFF84CC16), // Lime
)

@Composable
private fun ExerciseLegend(
    exercises: List<Exercise>,
    surfaceColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(exercises) { index, exercise ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(surfaceColor)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(exerciseColors[index % exerciseColors.size])
                )
                Text(
                    exercise.name.split(" ").first(),
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = textColor
                    )
                )
            }
        }
    }
}

@Composable
private fun MultiExerciseLineChart(
    exerciseData: Map<Exercise, List<SessionPoint>>,
    modifier: Modifier = Modifier,
    primaryColor: Color,
    backgroundColor: Color,
    textColor: Color,
    gridColor: Color
) {
    if (exerciseData.isEmpty() || exerciseData.values.all { it.isEmpty() }) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No data yet",
                style = TextStyle(
                    fontSize = 15.sp,
                    color = textColor
                )
            )
        }
        return
    }

    // Get all weights across all exercises to determine Y scale
    val allWeights = exerciseData.values.flatten().map { it.weight }
    val rawMax = allWeights.maxOrNull() ?: 0f
    val yMax = paddedMaxWeight(rawMax)
    val ticks = (0..4).map { i -> yMax * i / 4f }

    val density = LocalDensity.current

    Row(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            for (value in ticks.reversed()) {
                Text(
                    text = value.toInt().toString(),
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = textColor
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Canvas(
                modifier = Modifier.matchParentSize()
            ) {
                val width = size.width
                val height = size.height

                val leftPadding = 8.dp.toPx()
                val rightPadding = 8.dp.toPx()
                val topPadding = 16.dp.toPx()
                val bottomPadding = 8.dp.toPx()

                val usableWidth = width - leftPadding - rightPadding
                val usableHeight = height - topPadding - bottomPadding

                fun yForWeight(w: Float): Float {
                    val normalized = (w / yMax).coerceIn(0f, 1f)
                    return topPadding + (1f - normalized) * usableHeight
                }

                // Draw grid lines
                ticks.forEach { tickValue ->
                    val y = yForWeight(tickValue)
                    drawLine(
                        color = gridColor.copy(alpha = 0.2f),
                        start = Offset(leftPadding, y),
                        end = Offset(width - rightPadding, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw each exercise line
                exerciseData.entries.forEachIndexed { index, (_, history) ->
                    if (history.isEmpty()) return@forEachIndexed

                    val orderedHistory = history.sortedWith(
                        compareBy(
                            { parseIsoDateMillis(it.date) ?: Long.MAX_VALUE },
                            { it.sessionIndex }
                        )
                    )

                    val color = exerciseColors[index % exerciseColors.size]
                    val stepX = if (orderedHistory.size == 1) 0f else usableWidth / (orderedHistory.size - 1)

                    val points = orderedHistory.mapIndexed { pointIndex, point ->
                        val x = leftPadding + stepX * pointIndex
                        val y = yForWeight(point.weight)
                        Offset(x, y)
                    }

                    // Draw line
                    if (points.size >= 2) {
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }

                        drawPath(
                            path = linePath,
                            color = color,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    // Draw points
                    points.forEach { pt ->
                        drawCircle(
                            color = color,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = backgroundColor,
                            radius = 2.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }
    }
}
