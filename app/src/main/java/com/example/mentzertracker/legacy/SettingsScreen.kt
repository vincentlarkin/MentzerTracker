package com.vincentlarkin.mentzertracker.legacy

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import com.vincentlarkin.mentzertracker.NotificationHelper
import com.vincentlarkin.mentzertracker.BackupSnapshot
import com.vincentlarkin.mentzertracker.ThemeMode
import com.vincentlarkin.mentzertracker.allowPartialSessions
import com.vincentlarkin.mentzertracker.gson
import com.vincentlarkin.mentzertracker.importBackupFromJson
import com.vincentlarkin.mentzertracker.hasSeenSplash
import com.vincentlarkin.mentzertracker.loadWorkoutConfig
import com.vincentlarkin.mentzertracker.loadWorkoutLogs
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.text.Charsets
import kotlinx.coroutines.Dispatchers

private val SettingsScreenPadding = 16.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    isPartialSessionsAllowed: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAllowPartialSessionsChange: (Boolean) -> Unit,
    onImportBackup: (ThemeMode) -> Unit,
    onResetData: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val showResetConfirmState = remember { mutableStateOf(false) }
    val showImportConfirmState = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isDarkMode = themeMode == ThemeMode.DARK
    val appVersion = remember { resolveAppVersion(context) }
    var debugExpanded by remember { mutableStateOf(false) }
    val debugChevronRotation by animateFloatAsState(
        targetValue = if (debugExpanded) 90f else 0f,
        label = "debugChevron"
    )
    val primaryButtonShape = RoundedCornerShape(10.dp)

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
                    "Import complete. Reloading backup..."
                } else {
                    "Import failed: ${result.exceptionOrNull()?.localizedMessage ?: "unknown error"}"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                result.getOrNull()?.let { onImportBackup(it) }
            }
        } else {
            Toast.makeText(context, "Import canceled", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(onBack = onBack)

    if (showImportConfirmState.value) {
        AlertDialog(
            onDismissRequest = { showImportConfirmState.value = false },
            title = { Text("Import backup?") },
            text = {
                Text(
                    "Importing a backup replaces all workouts, logs, and preferences on this device. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportConfirmState.value = false
                        importLauncher.launch(arrayOf("application/json"))
                    },
                    shape = RectangleShape
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImportConfirmState.value = false },
                    shape = RectangleShape
                ) {
                    Text("Cancel")
                }
            }
        )
    }

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
                .padding(horizontal = SettingsScreenPadding)
                .padding(bottom = 24.dp, top = SettingsScreenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
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

            SettingsSectionHeader("Appearance")
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

            SettingsSectionHeader("Backups")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val timestamp = SimpleDateFormat(
                            "yyyyMMdd-HHmmss",
                            Locale.getDefault()
                        ).format(Date())
                        val suggestedName = "mentzer-tracker-backup-$timestamp.json"
                        exportLauncher.launch(suggestedName)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = primaryButtonShape
                ) {
                    Text("Export backup")
                }

                Button(
                    onClick = { showImportConfirmState.value = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = primaryButtonShape
                ) {
                    Text("Import backup")
                }

                Button(
                    onClick = { showResetConfirmState.value = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = primaryButtonShape
                ) {
                    Text("Reset data")
                }
            }

            SettingsSectionHeader("About")
            SettingsInfoRow(
                label = "Version",
                value = appVersion
            )
            Button(
                onClick = { uriHandler.openUri("https://github.com/VincentW2/MentzerTracker") },
                modifier = Modifier.fillMaxWidth(),
                shape = primaryButtonShape
            ) {
                Text("GitHub Repository")
            }

            SettingsSectionHeader("Debug tools")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { debugExpanded = !debugExpanded }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            contentDescription = if (debugExpanded) "Collapse" else "Expand",
                            modifier = Modifier.rotate(debugChevronRotation)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Debug options", fontWeight = FontWeight.SemiBold)
                    }

                    if (debugExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAllowPartialSessionsChange(!isPartialSessionsAllowed)
                                    }
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Allow partial sessions", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Let \"Save session\" skip exercises left blank.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Switch(
                                    checked = isPartialSessionsAllowed,
                                    onCheckedChange = onAllowPartialSessionsChange
                                )
                            }

                            // Test Notification
                            Button(
                                onClick = {
                                    scope.launch {
                                        NotificationHelper.triggerTestNotification(context)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = primaryButtonShape
                            ) {
                                Text("Send Test Notification")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

internal fun buildExportJson(
    context: Context,
    themeMode: ThemeMode
): String {
    val snapshot = BackupSnapshot(
        exportedAt = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.getDefault()
        ).format(Date()),
        appVersion = resolveAppVersion(context),
        themeMode = themeMode.name.lowercase(Locale.ROOT),
        hasSeenSplash = hasSeenSplash(context),
        allowPartialSessions = allowPartialSessions(context),
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


