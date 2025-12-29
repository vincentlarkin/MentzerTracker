package com.vincentlarkin.mentzertracker.legacy

import com.vincentlarkin.mentzertracker.ThemeMode
import com.vincentlarkin.mentzertracker.importBackupFromJson

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SplashScreen(
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
    onImportBackup: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val showImportConfirm = remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()
                            ?.use { it.readText() }
                    } ?: error("Unable to read backup file.")
                    importBackupFromJson(context, json)
                }
                val message = if (result.isSuccess) {
                    "Import complete. Loading backup..."
                } else {
                    "Import failed: ${result.exceptionOrNull()?.localizedMessage ?: "unknown error"}"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                if (result.isSuccess) {
                    onImportBackup(result.getOrThrow())
                }
            }
        } else {
            Toast.makeText(context, "Import canceled", Toast.LENGTH_SHORT).show()
        }
    }

    if (showImportConfirm.value) {
        AlertDialog(
            onDismissRequest = { showImportConfirm.value = false },
            title = { Text("Import backup?") },
            text = {
                Text(
                    "Importing a backup replaces all workouts, logs, and preferences on this device. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportConfirm.value = false
                        importLauncher.launch(arrayOf("application/json"))
                    },
                    shape = RectangleShape
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImportConfirm.value = false },
                    shape = RectangleShape
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
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
            OutlinedButton(
                onClick = { showImportConfirm.value = true },
                shape = RectangleShape
            ) {
                Text("Import backup")
            }
        }
    }
}

