package com.example.mentzertracker

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// UI model used only on the full-screen screen
data class EditableEntryUi(
    val logId: Long,
    val setIndex: Int,
    val date: MutableState<String>,
    val weight: MutableState<String>,
    val reps: MutableState<String>
)

@Composable
fun FullProgressScreen(
    logs: List<WorkoutLogEntry>,
    exercises: List<Exercise>,
    modifier: Modifier = Modifier,
    onUpdateLogs: (List<WorkoutLogEntry>) -> Unit
) {
    if (logs.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No sessions logged yet.")
        }
        return
    }

    // Only exercises that actually have history
    val exercisesWithHistory = remember(logs) {
        val ids = logs.flatMap { it.sets.map { s -> s.exerciseId } }.toSet()
        exercises.filter { it.id in ids }
    }

    if (exercisesWithHistory.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No data for any exercises yet.")
        }
        return
    }

    var selectedExercise by remember { mutableStateOf(exercisesWithHistory.first()) }

    // Editable entries for this exercise
    val editableEntries = remember(selectedExercise, logs) {
        logs.flatMap { log ->
            log.sets.mapIndexedNotNull { index, set ->
                if (set.exerciseId == selectedExercise.id) {
                    EditableEntryUi(
                        logId = log.id,
                        setIndex = index,
                        date = mutableStateOf(log.date),
                        weight = mutableStateOf(set.weight.toString()),
                        reps = mutableStateOf(set.reps.toString())
                    )
                } else null
            }
        }.toMutableList()
    }

    val sessionPoints = editableEntries.mapIndexed { idx, entry ->
        val w = entry.weight.value.toFloatOrNull() ?: 0f
        val r = entry.reps.value.toIntOrNull() ?: 0
        SessionPoint(
            sessionIndex = idx + 1,
            date = entry.date.value,
            weight = w,
            reps = r
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Reuse the same dropdown style as main screen
        ExerciseDropdown(
            exercisesWithHistory = exercisesWithHistory,
            selected = selectedExercise,
            onSelectedChange = { selectedExercise = it }
        )

        // Graph
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            ExerciseLineChart(
                history = sessionPoints,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )
        }

        Text(
            text = "Entries",
            style = MaterialTheme.typography.titleMedium
        )

        // Editable list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(editableEntries, key = { it.logId to it.setIndex }) { entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DateTextField(
                        dateState = entry.date,
                        modifier = Modifier.width(110.dp)
                    )

                    OutlinedTextField(
                        value = entry.weight.value,
                        onValueChange = { entry.weight.value = it },
                        modifier = Modifier.width(80.dp),
                        singleLine = true,
                        label = { Text("lbs") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = entry.reps.value,
                        onValueChange = { entry.reps.value = it },
                        modifier = Modifier.width(80.dp),
                        singleLine = true,
                        label = { Text("reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }

        Button(
            onClick = {
                val newLogs = applyEditsToLogs(
                    logs = logs,
                    exerciseId = selectedExercise.id,
                    edits = editableEntries
                )
                onUpdateLogs(newLogs)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save changes")
        }
    }
}

/**
 * Read-only text field that opens a native DatePicker dialog.
 * No keyboard, just tap → pick date.
 */
@Composable
fun DateTextField(
    dateState: MutableState<String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val calendar = remember { Calendar.getInstance() }

    fun openPicker() {
        try {
            val parsed = formatter.parse(dateState.value)
            if (parsed != null) calendar.time = parsed
        } catch (_: Exception) { /* ignore */ }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            context,
            { _, y, m, d ->
                calendar.set(y, m, d)
                dateState.value = formatter.format(calendar.time)
            },
            year,
            month,
            day
        ).show()
    }

    OutlinedTextField(
        value = dateState.value,
        onValueChange = {}, // readOnly
        modifier = modifier.clickable { openPicker() },
        label = { Text("Date") },
        singleLine = true,
        readOnly = true
    )
}

/**
 * Apply edits from the full-screen editor back into the logs list.
 */
fun applyEditsToLogs(
    logs: List<WorkoutLogEntry>,
    exerciseId: String,
    edits: List<EditableEntryUi>
): List<WorkoutLogEntry> {
    val editsByLog = edits.groupBy { it.logId }

    return logs.map { log ->
        val logEdits = editsByLog[log.id] ?: emptyList()
        if (logEdits.isEmpty()) {
            log
        } else {
            val updatedSets = log.sets.toMutableList()
            var newDate = log.date
            logEdits.forEach { e ->
                if (e.setIndex in updatedSets.indices) {
                    val old = updatedSets[e.setIndex]
                    if (old.exerciseId == exerciseId) {
                        val newWeight = e.weight.value.toFloatOrNull() ?: old.weight
                        val newReps = e.reps.value.toIntOrNull() ?: old.reps
                        updatedSets[e.setIndex] = old.copy(
                            weight = newWeight,
                            reps = newReps
                        )
                        newDate = e.date.value
                    }
                }
            }
            log.copy(
                date = newDate,
                sets = updatedSets.toList()
            )
        }
    }
}
