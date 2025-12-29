package com.vincentlarkin.mentzertracker

import android.Manifest
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun NotificationSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val initialPrefs = remember { NotificationHelper.loadPreferences(context) }
    var enabled by remember { mutableStateOf(initialPrefs.enabled) }
    var frequency by remember { mutableStateOf(initialPrefs.frequency) }
    var customDaysText by remember { mutableStateOf(initialPrefs.customIntervalDays.toString()) }
    var hour by remember { mutableStateOf(initialPrefs.hourOfDay) }
    var minute by remember { mutableStateOf(initialPrefs.minute) }
    var pendingSave by remember { mutableStateOf<NotificationPreferences?>(null) }

    val latestOnDismiss by rememberUpdatedState(onDismiss)

    fun persistPreferences(prefs: NotificationPreferences) {
        NotificationHelper.savePreferences(context, prefs)
        if (prefs.enabled) {
            NotificationHelper.scheduleNotifications(context, prefs)
            Toast.makeText(context, "Notifications scheduled.", Toast.LENGTH_SHORT).show()
        } else {
            NotificationHelper.cancelNotifications(context)
            Toast.makeText(context, "Notifications disabled.", Toast.LENGTH_SHORT).show()
        }
        latestOnDismiss()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val target = pendingSave
        if (granted && target != null) {
            persistPreferences(target)
        } else if (!granted) {
            Toast.makeText(
                context,
                "Notification permission is required to schedule reminders.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun handleSave() {
        val customDays = customDaysText.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val prefs = NotificationPreferences(
            enabled = enabled,
            frequency = frequency,
            customIntervalDays = customDays,
            hourOfDay = hour,
            minute = minute
        )

        if (prefs.enabled && NotificationHelper.needsRuntimePermission() &&
            !NotificationHelper.hasPermission(context)
        ) {
            pendingSave = prefs
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            persistPreferences(prefs)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Workout reminders") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Toggle row with more visual prominence
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (enabled) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            if (enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { enabled = !enabled }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Enable reminders", 
                            fontWeight = FontWeight.SemiBold,
                            color = if (enabled) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (enabled) "Notifications are active" 
                            else "Tap to enable workout reminders",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Frequency", fontWeight = FontWeight.SemiBold)
                    ReminderFrequency.entries.forEach { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = frequency == option,
                                onClick = { frequency = option },
                                enabled = enabled
                            )
                            Text(
                                text = when (option) {
                                    ReminderFrequency.DAILY -> "Daily"
                                    ReminderFrequency.WEEKLY -> "Weekly"
                                    ReminderFrequency.CUSTOM -> "Custom interval"
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    if (frequency == ReminderFrequency.CUSTOM) {
                        OutlinedTextField(
                            value = customDaysText,
                            onValueChange = { input ->
                                customDaysText = input.filter { it.isDigit() }.take(2)
                            },
                            modifier = Modifier.width(140.dp),
                            label = { Text("Days between") },
                            enabled = enabled,
                            singleLine = true
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Reminder time", fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    hour = h
                                    minute = m
                                },
                                hour,
                                minute,
                                android.text.format.DateFormat.is24HourFormat(context)
                            ).show()
                        },
                        enabled = enabled
                    ) {
                        val timeLabel = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                        Text(timeLabel)
                    }
                }

                if (!enabled) {
                    Text(
                        text = "Reminders are disabled. Turn them on to get notified.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { handleSave() }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

