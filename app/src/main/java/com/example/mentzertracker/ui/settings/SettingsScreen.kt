package com.vincentlarkin.mentzertracker.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vincentlarkin.mentzertracker.ThemeMode
import com.vincentlarkin.mentzertracker.UserWorkoutConfig
import com.vincentlarkin.mentzertracker.WorkoutLogEntry
import com.vincentlarkin.mentzertracker.gson
import com.vincentlarkin.mentzertracker.hasSeenSplash
import com.vincentlarkin.mentzertracker.loadWorkoutConfig
import com.vincentlarkin.mentzertracker.loadWorkoutLogs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.text.Charsets

private val SettingsScreenPadding = 16.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onResetData: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val showResetConfirmState = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isDarkMode = themeMode == ThemeMode.DARK

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = runCatching {
                    val json = buildExportJson(context, themeMode)
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            output.write(json.toByteArray(Charsets.UTF_8))
                            output.flush()
                        } ?: error("Unable to open output stream")
                    }
                }
                val message = if (result.isSuccess) {
                    "Export complete."
                } else {
                    "Export failed: ${result.exceptionOrNull()?.localizedMessage ?: "unknown error"}"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Export canceled", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(onBack = onBack)

    if (showResetConfirmState.value) {
        AlertDialog(
            onDismissRequest = { showResetConfirmState.value = false },
            title = { Text("Reset data?") },
            text = {
                Text(
                    "This will delete your saved workouts, logs, and splash preferences. " +
                            "You can configure everything again after the reset."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmState.value = false
                        onResetData()
                    },
                    shape = RectangleShape
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmState.value = false },
                    shape = RectangleShape
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.clip(RectangleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(SettingsScreenPadding)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "App info"
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Vincent L · 2025", fontWeight = FontWeight.SemiBold)
                    Text(
                        "MentzerTracker is a small app inspired by Mike Mentzer's " +
                                "heavy-duty training principles."
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Theme", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (isDarkMode) "Dark mode" else "Light mode",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(
                    onClick = {
                        val newMode =
                            if (themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                        onThemeModeChange(newMode)
                    },
                    modifier = Modifier.clip(RectangleShape)
                ) {
                    if (isDarkMode) {
                        Icon(
                            imageVector = Icons.Filled.DarkMode,
                            contentDescription = "Dark mode"
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.LightMode,
                            contentDescription = "Light mode"
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val timestamp = SimpleDateFormat(
                        "yyyyMMdd-HHmmss",
                        Locale.getDefault()
                    ).format(Date())
                    val suggestedName = "mentzer-tracker-export-$timestamp.json"
                    exportLauncher.launch(suggestedName)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape
            ) {
                Text("Export data")
            }

            Button(
                onClick = { showResetConfirmState.value = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape
            ) {
                Text("Reset data")
            }

            TextButton(
                onClick = { uriHandler.openUri("https://github.com/VincentW2/MentzerTracker") },
                modifier = Modifier.align(Alignment.End),
                shape = RectangleShape
            ) {
                Text("GitHub")
            }
        }
    }
}

private data class ExportSnapshot(
    val exportedAt: String,
    val appVersion: String,
    val themeMode: String,
    val hasSeenSplash: Boolean,
    val workoutConfig: UserWorkoutConfig,
    val workoutLogs: List<WorkoutLogEntry>
)

internal fun buildExportJson(
    context: Context,
    themeMode: ThemeMode
): String {
    val snapshot = ExportSnapshot(
        exportedAt = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.getDefault()
        ).format(Date()),
        appVersion = resolveAppVersion(context),
        themeMode = themeMode.name.lowercase(Locale.ROOT),
        hasSeenSplash = hasSeenSplash(context),
        workoutConfig = loadWorkoutConfig(context),
        workoutLogs = loadWorkoutLogs(context)
    )
    return gson.toJson(snapshot)
}

internal fun resolveAppVersion(context: Context): String {
    return try {
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            ).versionName ?: "unknown"
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        }
    } catch (e: Exception) {
        "unknown"
    }
}


