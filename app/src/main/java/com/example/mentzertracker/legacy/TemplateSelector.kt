package com.vincentlarkin.mentzertracker.legacy

import com.vincentlarkin.mentzertracker.WorkoutTemplate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.RectangleShape

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

