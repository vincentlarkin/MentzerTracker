package com.example.mentzertracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.mentzertracker.ui.theme.MentzerTrackerTheme
import androidx.compose.foundation.layout.Box
import android.content.Context
import androidx.compose.ui.platform.LocalContext



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

// ---------- DATA MODELS ----------

data class Exercise(
    val id: String,
    val name: String
)

data class WorkoutTemplate(
    val id: String,        // "A" or "B"
    val name: String,      // "Workout A"
    val exerciseIds: List<String>
)

data class ExerciseSetEntry(
    val exerciseId: String,
    val weight: Float,
    val reps: Int
)

data class WorkoutLogEntry(
    val id: Long,              // timestamp
    val templateId: String,    // "A" or "B"
    val date: String,
    val sets: List<ExerciseSetEntry>
)

// ---------- HARD-CODED EXERCISES & TEMPLATES ----------

val allExercises = listOf(
    Exercise("squat", "Smith Squat"),
    Exercise("deadlift", "Deadlift"),
    Exercise("pulldown", "Close-Grip Palm-Up Pulldown"),
    Exercise("incline_press", "Incline Press"),
    Exercise("dips", "Dips")
)

// Edit these however you want, but keep them as A / B
val workoutA = WorkoutTemplate(
    id = "A",
    name = "Workout A",
    exerciseIds = listOf("squat", "pulldown")
)

val workoutB = WorkoutTemplate(
    id = "B",
    name = "Workout B",
    exerciseIds = listOf("deadlift", "incline_press", "dips")
)

val templates = listOf(workoutA, workoutB)

@Composable
fun AppRoot() {
    val context = LocalContext.current

    // Initialize from SharedPreferences once
    var showSplash by remember {
        mutableStateOf(!hasSeenSplash(context))
    }

    if (showSplash) {
        SplashScreen(
            onStart = {
                setHasSeenSplash(context)
                showSplash = false
            }
        )
    } else {
        WorkoutTrackerApp()
    }
}


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
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onStart) {
                Text("Start")
            }
        }
    }
}


// ---------- TOP-LEVEL UI ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTrackerApp() {
    var selectedTemplateId by remember { mutableStateOf("A") }

    // In-memory list of workout logs
    val logEntries = remember { mutableStateListOf<WorkoutLogEntry>() }

    val currentTemplate = templates.first { it.id == selectedTemplateId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mentzer A/B Tracker") }
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
                onTemplateSelected = { selectedTemplateId = it }
            )

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
                }
            )

            ProgressSection(
                logs = logEntries,
                exercises = allExercises
            )
        }
    }
}

// ---------- TEMPLATE SELECTOR (A / B) ----------

@Composable
fun TemplateSelector(
    selectedTemplateId: String,
    onTemplateSelected: (String) -> Unit
) {
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
}

// ---------- LOGGING A WORKOUT ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogWorkoutSection(
    template: WorkoutTemplate,
    onSave: (List<ExerciseSetEntry>) -> Unit
) {
    Text("Log ${template.name}", style = MaterialTheme.typography.titleMedium)

    // Per-exercise fields
    val weightState = remember { mutableStateMapOf<String, String>() }
    val repsState = remember { mutableStateMapOf<String, String>() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        template.exerciseIds.forEach { exerciseId ->
            val exercise = allExercises.first { it.id == exerciseId }

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
                    // Clear fields after save
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

// ---------- SIMPLE PROGRESS VIEW ----------

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

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(exercises) { ex ->
            val history = logs
                .flatMap { log ->
                    log.sets.filter { it.exerciseId == ex.id }
                        .map { log.date to it.weight }
                }
                .sortedBy { it.first }

            if (history.isNotEmpty()) {
                Column {
                    Text(ex.name, fontWeight = FontWeight.Bold)
                    history.forEachIndexed { index, (date, weight) ->
                        Text("Week ${index + 1} ($date): ${weight} lbs")
                    }
                }
            }
        }
    }
}
private const val PREFS_NAME = "mentzer_prefs"
private const val KEY_HAS_SEEN_SPLASH = "has_seen_splash"

private fun hasSeenSplash(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_HAS_SEEN_SPLASH, false)
}

private fun setHasSeenSplash(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_HAS_SEEN_SPLASH, true).apply()
}
