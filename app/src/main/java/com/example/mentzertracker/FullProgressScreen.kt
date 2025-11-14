package com.vincentlarkin.mentzertracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
    initialExerciseId: String? = null,
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
    val exercisesWithHistory = exercises.filter { exercise ->
        logs.any { log -> log.sets.any { it.exerciseId == exercise.id } }
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

    val defaultExerciseId = remember(initialExerciseId, exercisesWithHistory) {
        initialExerciseId?.takeIf { id -> exercisesWithHistory.any { it.id == id } }
            ?: exercisesWithHistory.first().id
    }

    var selectedExerciseId by rememberSaveable(defaultExerciseId) {
        mutableStateOf(defaultExerciseId)
    }

    LaunchedEffect(exercisesWithHistory, selectedExerciseId) {
        if (exercisesWithHistory.isNotEmpty() &&
            exercisesWithHistory.none { it.id == selectedExerciseId }
        ) {
            selectedExerciseId = exercisesWithHistory.first().id
        }
    }

    val selectedExercise = exercisesWithHistory.firstOrNull { it.id == selectedExerciseId }
        ?: exercisesWithHistory.first()

    var deleteMode by remember(selectedExerciseId) { mutableStateOf(false) }
    var pendingDeleteEntry by remember(selectedExerciseId) { mutableStateOf<EditableEntryUi?>(null) }

    val entryStateMap = remember(selectedExerciseId) {
        mutableStateMapOf<Pair<Long, Int>, EditableEntryUi>()
    }

    val logsSignature = logs.fold(0) { acc, log ->
        var result = 31 * acc + log.id.hashCode()
        result = 31 * result + log.date.hashCode()
        result = 31 * result + log.sets.hashCode()
        result
    }
    var logsVersion by remember(selectedExerciseId) { mutableStateOf(logsSignature) }
    val logsChanged = logsVersion != logsSignature
    LaunchedEffect(selectedExerciseId, logsSignature) {
        logsVersion = logsSignature
    }

    val activeKeys = mutableListOf<Pair<Long, Int>>()
    logs.forEach { log ->
        log.sets.forEachIndexed { index, set ->
            if (set.exerciseId == selectedExercise.id) {
                val key = log.id to index
                activeKeys.add(key)
                val existing = entryStateMap[key]
                if (existing == null) {
                    entryStateMap[key] = EditableEntryUi(
                        logId = log.id,
                        setIndex = index,
                        date = mutableStateOf(log.date),
                        weight = mutableStateOf(set.weight.toString()),
                        reps = mutableStateOf(set.reps.toString())
                    )
                } else if (logsChanged) {
                    if (existing.date.value != log.date) {
                        existing.date.value = log.date
                    }
                    val weightString = set.weight.toString()
                    if (existing.weight.value != weightString) {
                        existing.weight.value = weightString
                    }
                    val repsString = set.reps.toString()
                    if (existing.reps.value != repsString) {
                        existing.reps.value = repsString
                    }
                }
            }
        }
    }

    val activeKeySet = activeKeys.toSet()
    val keysToRemove = entryStateMap.keys - activeKeySet
    keysToRemove.forEach { key ->
        if (pendingDeleteEntry?.let { it.logId to it.setIndex } == key) {
            pendingDeleteEntry = null
        }
        entryStateMap.remove(key)
    }

    val editableEntries = activeKeys.mapNotNull { entryStateMap[it] }

    LaunchedEffect(deleteMode, editableEntries.size) {
        if (deleteMode && editableEntries.isEmpty()) {
            deleteMode = false
        }
    }

    val sortedEntriesForGraph = editableEntries.sortedWith(
        compareBy(
            { parseIsoDateMillis(it.date.value) ?: Long.MAX_VALUE },
            { it.logId },
            { it.setIndex }
        )
    )

    val sessionPoints = sortedEntriesForGraph.mapIndexed { idx, entry ->
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
            onSelectedChange = { selectedExerciseId = it.id }
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Entries",
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(
                onClick = { deleteMode = !deleteMode },
                shape = RectangleShape,
                enabled = editableEntries.isNotEmpty()
            ) {
                Text(if (deleteMode) "Done" else "Delete entries")
            }
        }

        // Editable list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sortedEntriesForGraph, key = { it.logId to it.setIndex }) { entry ->
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
                    if (deleteMode) {
                        Spacer(modifier = Modifier.weight(1f, fill = false))
                        IconButton(
                            onClick = { pendingDeleteEntry = entry }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Delete entry"
                            )
                        }
                    }
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
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape
        ) {
            Text("Save changes")
        }
    }

    val entryToDelete = pendingDeleteEntry
    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteEntry = null },
            title = { Text("Delete entry?") },
            text = {
                Text(
                    "Remove the entry on ${entryToDelete.date.value} " +
                            "(${entryToDelete.weight.value} lbs × ${entryToDelete.reps.value} reps)?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteEntry = null
                        val updatedLogs = deleteEntryFromLogs(
                            logs = logs,
                            exerciseId = selectedExercise.id,
                            entry = entryToDelete
                        )
                        onUpdateLogs(updatedLogs)
                    },
                    shape = RectangleShape
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDeleteEntry = null },
                    shape = RectangleShape
                ) {
                    Text("Cancel")
                }
            }
        )
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
    val showPicker = remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()

    if (showPicker.value) {
        DatePickerDialog(
            onDismissRequest = { showPicker.value = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            dateState.value = formatter.format(millis)
                        }
                        showPicker.value = false
                        },
                        shape = RectangleShape
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                    TextButton(
                    onClick = { showPicker.value = false },
                        shape = RectangleShape
                    ) {
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
                    showPicker.value = true
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

    val updatedLogs = logs.map { log ->
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

    return updatedLogs.sortedWith(
        compareBy(
            { parseIsoDateMillis(it.date) ?: Long.MAX_VALUE },
            { it.id }
        )
    )
}

fun deleteEntryFromLogs(
    logs: List<WorkoutLogEntry>,
    exerciseId: String,
    entry: EditableEntryUi
): List<WorkoutLogEntry> {
    val updated = logs.mapNotNull { log ->
        if (log.id != entry.logId) {
            log
        } else {
            val sets = log.sets.toMutableList()
            if (entry.setIndex !in sets.indices) {
                log
            } else if (sets[entry.setIndex].exerciseId != exerciseId) {
                log
            } else {
                sets.removeAt(entry.setIndex)
                if (sets.isEmpty()) {
                    null
                } else {
                    log.copy(sets = sets.toList())
                }
            }
        }
    }

    return updated.sortedWith(
        compareBy(
            { parseIsoDateMillis(it.date) ?: Long.MAX_VALUE },
            { it.id }
        )
    )
}

private fun parseIsoDateMillis(date: String): Long? {
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)?.time
    } catch (_: Exception) {
        null
    }
}
