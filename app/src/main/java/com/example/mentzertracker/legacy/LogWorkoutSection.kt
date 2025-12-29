package com.vincentlarkin.mentzertracker.legacy

import com.vincentlarkin.mentzertracker.Exercise
import com.vincentlarkin.mentzertracker.ExerciseSetEntry
import com.vincentlarkin.mentzertracker.WorkoutTemplate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private const val NOTE_CHAR_LIMIT = 400

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogWorkoutSection(
    template: WorkoutTemplate,
    exercisesById: Map<String, Exercise>,
    allowPartialSessions: Boolean,
    onSave: (List<ExerciseSetEntry>, String?) -> Unit
) {
    Text("Log ${template.name}", style = MaterialTheme.typography.titleMedium)

    val weightState = remember { mutableStateMapOf<String, String>() }
    val repsState = remember { mutableStateMapOf<String, String>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var showNotesField by remember { mutableStateOf(false) }
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

        if (showNotesField) {
            OutlinedTextField(
                value = notes,
                onValueChange = { input ->
                    notes = input.take(NOTE_CHAR_LIMIT)
                },
                label = { Text("Notes") },
                placeholder = { Text("Any quick context for this session?") },
                supportingText = {
                    Text("${notes.length}/$NOTE_CHAR_LIMIT")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
            TextButton(
                onClick = { showNotesField = false },
                shape = RectangleShape
            ) {
                Text("Hide notes")
            }
        } else {
            TextButton(
                onClick = { showNotesField = true },
                shape = RectangleShape
            ) {
                Text("Add notes")
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
                    val trimmedNotes = notes.trim()
                    onSave(sets, trimmedNotes.takeIf { it.isNotEmpty() })

                    template.exerciseIds.forEach { id ->
                        weightState[id] = ""
                        repsState[id] = ""
                    }
                    notes = ""
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

