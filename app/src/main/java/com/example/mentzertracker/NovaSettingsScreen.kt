package com.vincentlarkin.mentzertracker

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincentlarkin.mentzertracker.ui.settings.buildExportJson
import com.vincentlarkin.mentzertracker.ui.settings.resolveAppVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NovaSettingsScreen(
    themeMode: ThemeMode,
    isPartialSessionsAllowed: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAllowPartialSessionsChange: (Boolean) -> Unit,
    onImportBackup: (ThemeMode) -> Unit,
    onResetData: () -> Unit,
    onEditWorkouts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val isDarkMode = themeMode == ThemeMode.DARK
    val appVersion = remember { resolveAppVersion(context) }
    
    // Get theme colors
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val errorColor = MaterialTheme.colorScheme.error
    
    var showResetConfirm by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var advancedExpanded by remember { mutableStateOf(false) }
    
    val advancedRotation by animateFloatAsState(
        targetValue = if (advancedExpanded) 180f else 0f,
        label = "advancedRotation"
    )
    
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
                val message = if (result.isSuccess) "Backup exported!" else "Export failed"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
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
                val message = if (result.isSuccess) "Backup imported!" else "Import failed"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                result.getOrNull()?.let { onImportBackup(it) }
            }
        }
    }
    
    // Reset confirmation dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = surfaceColor,
            title = {
                Text(
                    "Reset All Data?",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor
                    )
                )
            },
            text = {
                Text(
                    "This will delete all workouts, logs, and preferences. This cannot be undone.",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = onSurfaceVariant
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    onResetData()
                }) {
                    Text("Reset", color = errorColor, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel", color = onSurfaceVariant)
                }
            }
        )
    }
    
    // Import confirmation dialog
    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            containerColor = surfaceColor,
            title = {
                Text(
                    "Import Backup?",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor
                    )
                )
            },
            text = {
                Text(
                    "This will replace all current data with the backup. This cannot be undone.",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = onSurfaceVariant
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    importLauncher.launch(arrayOf("application/json"))
                }) {
                    Text("Import", color = primaryColor, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) {
                    Text("Cancel", color = onSurfaceVariant)
                }
            }
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 100.dp) // Space for nav bar
        ) {
            // Header
            Text(
                "Settings",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = onBackgroundColor
                ),
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )
            
            // Appearance section
            SectionHeader("Appearance", onSurfaceVariant)
            
            SettingsCard(surfaceColor, outlineColor) {
                SettingsRow(
                    icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                    title = "Theme",
                    subtitle = if (isDarkMode) "Dark mode" else "Light mode",
                    onClick = {
                        val newMode = if (isDarkMode) ThemeMode.LIGHT else ThemeMode.DARK
                        onThemeModeChange(newMode)
                    },
                    surfaceVariant = surfaceVariant,
                    textColor = onSurfaceColor,
                    secondaryColor = onSurfaceVariant,
                    trailing = {
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { dark ->
                                onThemeModeChange(if (dark) ThemeMode.DARK else ThemeMode.LIGHT)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = primaryColor,
                                checkedTrackColor = primaryColor.copy(alpha = 0.3f),
                                uncheckedThumbColor = onSurfaceVariant,
                                uncheckedTrackColor = outlineColor
                            )
                        )
                    }
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Workouts section
            SectionHeader("Workouts", onSurfaceVariant)
            
            SettingsCard(surfaceColor, outlineColor) {
                SettingsRow(
                    icon = Icons.Default.FitnessCenter,
                    title = "Edit Exercises",
                    subtitle = "Configure your A/B workouts",
                    onClick = onEditWorkouts,
                    surfaceVariant = surfaceVariant,
                    textColor = onSurfaceColor,
                    secondaryColor = onSurfaceVariant
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Data section
            SectionHeader("Data", onSurfaceVariant)
            
            SettingsCard(surfaceColor, outlineColor) {
                SettingsRow(
                    icon = Icons.Default.Download,
                    title = "Export Backup",
                    subtitle = "Save your data to a file",
                    onClick = {
                        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
                            .format(Date())
                        exportLauncher.launch("mentzer-backup-$timestamp.json")
                    },
                    surfaceVariant = surfaceVariant,
                    textColor = onSurfaceColor,
                    secondaryColor = onSurfaceVariant
                )
                
                SettingsDivider(outlineColor)
                
                SettingsRow(
                    icon = Icons.Default.Upload,
                    title = "Import Backup",
                    subtitle = "Restore from a backup file",
                    onClick = { showImportConfirm = true },
                    surfaceVariant = surfaceVariant,
                    textColor = onSurfaceColor,
                    secondaryColor = onSurfaceVariant
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Advanced section
            SectionHeader("Advanced", onSurfaceVariant)
            
            SettingsCard(surfaceColor, outlineColor) {
                SettingsRow(
                    icon = Icons.Default.ExpandMore,
                    iconRotation = advancedRotation,
                    title = "Developer Options",
                    subtitle = if (advancedExpanded) "Tap to collapse" else "Tap to expand",
                    onClick = { advancedExpanded = !advancedExpanded },
                    surfaceVariant = surfaceVariant,
                    textColor = onSurfaceColor,
                    secondaryColor = onSurfaceVariant
                )
                
                AnimatedVisibility(visible = advancedExpanded) {
                    Column {
                        SettingsDivider(outlineColor)
                        
                        SettingsRow(
                            icon = null,
                            title = "Allow Partial Sessions",
                            subtitle = "Save workouts with missing exercises",
                            onClick = { onAllowPartialSessionsChange(!isPartialSessionsAllowed) },
                            surfaceVariant = surfaceVariant,
                            textColor = onSurfaceColor,
                            secondaryColor = onSurfaceVariant,
                            trailing = {
                                Switch(
                                    checked = isPartialSessionsAllowed,
                                    onCheckedChange = onAllowPartialSessionsChange,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = primaryColor,
                                        checkedTrackColor = primaryColor.copy(alpha = 0.3f),
                                        uncheckedThumbColor = onSurfaceVariant,
                                        uncheckedTrackColor = outlineColor
                                    )
                                )
                            }
                        )
                        
                        SettingsDivider(outlineColor)
                        
                        SettingsRow(
                            icon = Icons.Default.Notifications,
                            title = "Test Notification",
                            subtitle = "Send a test reminder",
                            onClick = {
                                scope.launch {
                                    NotificationHelper.triggerTestNotification(context)
                                }
                            },
                            surfaceVariant = surfaceVariant,
                            textColor = onSurfaceColor,
                            secondaryColor = onSurfaceVariant
                        )
                        
                        SettingsDivider(outlineColor)
                        
                        SettingsRow(
                            icon = Icons.Default.Refresh,
                            title = "Reset All Data",
                            subtitle = "Delete all workouts and start fresh",
                            titleColor = errorColor,
                            onClick = { showResetConfirm = true },
                            surfaceVariant = surfaceVariant,
                            textColor = onSurfaceColor,
                            secondaryColor = onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // About section
            SectionHeader("About", onSurfaceVariant)
            
            SettingsCard(surfaceColor, outlineColor) {
                SettingsRow(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = appVersion,
                    onClick = null,
                    surfaceVariant = surfaceVariant,
                    textColor = onSurfaceColor,
                    secondaryColor = onSurfaceVariant
                )
                
                SettingsDivider(outlineColor)
                
                SettingsRow(
                    icon = Icons.Default.Code,
                    title = "Source Code",
                    subtitle = "View on GitHub",
                    onClick = {
                        uriHandler.openUri("https://github.com/VincentW2/MentzerTracker")
                    },
                    surfaceVariant = surfaceVariant,
                    textColor = onSurfaceColor,
                    secondaryColor = onSurfaceVariant
                )
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Footer
            Text(
                "Made with ❤️ by Vincent L · 2025",
                style = TextStyle(
                    fontSize = 13.sp,
                    color = onSurfaceVariant,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Text(
        title.uppercase(),
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = color
        ),
        modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsCard(
    surfaceColor: Color,
    outlineColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector?,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    surfaceVariant: Color,
    textColor: Color,
    secondaryColor: Color,
    titleColor: Color = textColor,
    iconRotation: Float = 0f,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = secondaryColor,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(iconRotation)
                )
            }
            Spacer(Modifier.width(14.dp))
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = titleColor
                )
            )
            Text(
                subtitle,
                style = TextStyle(
                    fontSize = 13.sp,
                    color = secondaryColor
                )
            )
        }
        
        trailing?.invoke()
    }
}

@Composable
private fun SettingsDivider(outlineColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(outlineColor.copy(alpha = 0.2f))
    )
}
