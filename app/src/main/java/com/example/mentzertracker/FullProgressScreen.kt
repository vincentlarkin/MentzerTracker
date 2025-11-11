package com.example.mentzertracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTextField(
    dateState: MutableState<String>,
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var showPicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            dateState.value = formatter.format(millis)
                        }
                        showPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(modifier = modifier) {
        // Let the text field pick its own height
        OutlinedTextField(
            value = dateState.value,
            onValueChange = { },      // read-only
            readOnly = true,
            singleLine = true,
            label = { Text("Date") },
            modifier = Modifier.fillMaxWidth()
        )

        // Transparent overlay that actually handles taps
        Box(
            // The 'matchParentSize' modifier is an extension on BoxScope,
            // which is the context provided by the parent Box.
            // This should now resolve correctly.
            modifier = Modifier
                .matchParentSize() // This line was causing the error
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    showPicker = true
                }
        )
    }
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
