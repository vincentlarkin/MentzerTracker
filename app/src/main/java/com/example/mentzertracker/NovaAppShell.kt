package com.vincentlarkin.mentzertracker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincentlarkin.mentzertracker.novanotes.NovaHomeScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class NovaTab {
    LOG, PROGRESS, SETTINGS
}

@Composable
fun NovaAppShell(
    config: UserWorkoutConfig,
    themeMode: ThemeMode,
    isPartialSessionsAllowed: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAllowPartialSessionsChange: (Boolean) -> Unit,
    onImportBackup: (ThemeMode) -> Unit,
    onResetData: () -> Unit,
    onEditWorkouts: () -> Unit
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(NovaTab.LOG) }
    var logDraftText by remember { mutableStateOf("") }
    
    val combinedExercises = remember(config) {
        (allExercises + config.customExercises).distinctBy { it.id }
    }
    
    val logEntries = remember {
        mutableStateListOf<WorkoutLogEntry>().apply {
            addAll(loadWorkoutLogs(context))
        }
    }
    
    // Use MaterialTheme colors
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Main content
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(spring(stiffness = Spring.StiffnessMedium)) togetherWith
                    fadeOut(spring(stiffness = Spring.StiffnessMedium))
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "tabContent"
            ) { tab ->
                when (tab) {
                    NovaTab.LOG -> {
                        NovaHomeScreen(
                            customExercises = config.customExercises,
                            recentLogs = logEntries.toList(),
                            inputText = logDraftText,
                            onInputTextChange = { logDraftText = it },
                            onSave = { sets, notes, templateId ->
                                val entry = WorkoutLogEntry(
                                    id = System.currentTimeMillis(),
                                    templateId = templateId,
                                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                        .format(Date()),
                                    sets = sets,
                                    notes = notes?.takeIf { it.isNotBlank() }
                                )
                                logEntries.add(entry)
                                saveWorkoutLogs(context, logEntries)
                            }
                        )
                    }
                    NovaTab.PROGRESS -> {
                        NovaProgressScreen(
                            logs = logEntries,
                            exercises = combinedExercises,
                            onUpdateLogs = { newLogs ->
                                logEntries.clear()
                                logEntries.addAll(newLogs)
                                saveWorkoutLogs(context, logEntries)
                            }
                        )
                    }
                    NovaTab.SETTINGS -> {
                        NovaSettingsScreen(
                            themeMode = themeMode,
                            isPartialSessionsAllowed = isPartialSessionsAllowed,
                            onThemeModeChange = onThemeModeChange,
                            onAllowPartialSessionsChange = onAllowPartialSessionsChange,
                            onImportBackup = onImportBackup,
                            onResetData = onResetData,
                            onEditWorkouts = onEditWorkouts
                        )
                    }
                }
            }

            // Bottom navigation
            NovaBottomNav(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                surfaceColor = surfaceColor,
                primaryColor = primaryColor,
                secondaryColor = onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NovaBottomNav(
    currentTab: NovaTab,
    onTabSelected: (NovaTab) -> Unit,
    modifier: Modifier = Modifier,
    surfaceColor: Color,
    primaryColor: Color,
    secondaryColor: Color
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceColor.copy(alpha = 0.95f))
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NovaNavItem(
                icon = Icons.Outlined.Edit,
                selectedIcon = Icons.Filled.Edit,
                label = "Log",
                isSelected = currentTab == NovaTab.LOG,
                onClick = { onTabSelected(NovaTab.LOG) },
                primaryColor = primaryColor,
                secondaryColor = secondaryColor
            )
            NovaNavItem(
                icon = Icons.Outlined.BarChart,
                selectedIcon = Icons.Filled.BarChart,
                label = "Progress",
                isSelected = currentTab == NovaTab.PROGRESS,
                onClick = { onTabSelected(NovaTab.PROGRESS) },
                primaryColor = primaryColor,
                secondaryColor = secondaryColor
            )
            NovaNavItem(
                icon = Icons.Outlined.Settings,
                selectedIcon = Icons.Filled.Settings,
                label = "Settings",
                isSelected = currentTab == NovaTab.SETTINGS,
                onClick = { onTabSelected(NovaTab.SETTINGS) },
                primaryColor = primaryColor,
                secondaryColor = secondaryColor
            )
        }
    }
}

@Composable
private fun NovaNavItem(
    icon: ImageVector,
    selectedIcon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    secondaryColor: Color
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.9f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "navScale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .scale(scale)
    ) {
        Box(
            modifier = Modifier
                .size(if (isSelected) 44.dp else 40.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) primaryColor.copy(alpha = 0.15f) 
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) selectedIcon else icon,
                contentDescription = label,
                tint = if (isSelected) primaryColor else secondaryColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) primaryColor else secondaryColor
            )
        )
    }
}
