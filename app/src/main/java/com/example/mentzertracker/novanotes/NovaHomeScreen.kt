package com.vincentlarkin.mentzertracker.novanotes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincentlarkin.mentzertracker.Exercise
import com.vincentlarkin.mentzertracker.ExerciseSetEntry
import com.vincentlarkin.mentzertracker.NotificationHelper
import com.vincentlarkin.mentzertracker.NotificationSettingsDialog

import com.vincentlarkin.mentzertracker.WorkoutLogEntry
import com.vincentlarkin.mentzertracker.allExercises
import com.vincentlarkin.mentzertracker.loadWorkoutInterval
import com.vincentlarkin.mentzertracker.saveWorkoutInterval
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun NovaHomeScreen(
    customExercises: List<Exercise>,
    recentLogs: List<WorkoutLogEntry>,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSave: (List<ExerciseSetEntry>, String?, String) -> Unit, // Added templateId parameter
    modifier: Modifier = Modifier
) {
    // Build exercise lookup
    val allAvailableExercises = remember(customExercises) {
        (allExercises + customExercises).distinctBy { it.id }
    }
    val context = LocalContext.current


    var showSuccess by remember { mutableStateOf(false) }
    var showExamplesPopup by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    // Load workout interval preference (separate from notifications)
    var workoutIntervalDays by remember { mutableStateOf(loadWorkoutInterval(context)) }
    
    // Load notification state for bell icon
    val notificationsEnabled = remember { NotificationHelper.loadPreferences(context).enabled }
    
    // Callback to update interval
    val onIntervalChange: (Int) -> Unit = { newInterval ->
        workoutIntervalDays = newInterval
        saveWorkoutInterval(context, newInterval)
    }
    val scrollState = rememberScrollState()
    
    // Get theme colors
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    
    // Track the most recent workout
    val lastWorkout = remember(recentLogs) {
        recentLogs.maxByOrNull { it.id } // Most recent by timestamp
    }

    val todayTemplateId = "TODAY"

    
    val parseResult by remember(inputText) {
        derivedStateOf {
            if (inputText.isBlank()) null
            else WorkoutParser.parse(inputText, customExercises)
        }
    }
    
    val hasUnknownLines = parseResult?.unrecognizedLines?.isNotEmpty() == true
    val hasValidSets = parseResult?.parsedExercises?.isNotEmpty() == true && !hasUnknownLines

    
    // Success animation reset
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(1200)
            showSuccess = false
        }
    }
    
    val exampleHints = remember(customExercises) {
        generateExampleHints(customExercises)
    }
    
    // Dynamic suggestions based on what user is typing
    val typingSuggestions = remember(inputText, allAvailableExercises) {
        generateTypingSuggestions(inputText, allAvailableExercises)
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Gradient accent at top
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
                .imePadding()
                .verticalScroll(scrollState)
        ) {
            // Header

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Today's Workout",
                        style = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            color = onBackgroundColor
                        )
                    )
                    Text(
                        "Type naturally, we'll handle the rest",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = onSurfaceVariant
                        )
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Notification bell button
                    IconButton(
                        onClick = { showNotificationDialog = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(surfaceColor)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = if (notificationsEnabled) primaryColor else onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    // Help button
                    IconButton(
                        onClick = { showExamplesPopup = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(surfaceColor)
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = "Examples",
                            tint = onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    // Save button
                    val saveScale by animateFloatAsState(
                        targetValue = if (hasValidSets) 1f else 0.95f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "saveScale"
                    )
                    val saveAlpha by animateFloatAsState(
                        targetValue = if (hasValidSets) 1f else 0.4f,
                        label = "saveAlpha"
                    )
                    
                    IconButton(
                        onClick = {
                            if (hasValidSets && parseResult != null) {
                                val sets = WorkoutParser.toSetEntries(parseResult!!.parsedExercises)
                                onSave(sets, null, todayTemplateId)
                                showSuccess = true
                                onInputTextChange("")
                            }
                        },
                        enabled = hasValidSets,
                        modifier = Modifier
                            .size(44.dp)
                            .scale(saveScale)
                            .alpha(saveAlpha)
                            .clip(CircleShape)
                            .background(if (hasValidSets) primaryColor else surfaceColor)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save",
                            tint = if (hasValidSets) Color.White else onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Schedule Card - Simple: last workout and next due
            ScheduleCard(
                lastWorkout = lastWorkout,
                totalWorkouts = recentLogs.size,
                workoutIntervalDays = workoutIntervalDays,
                onIntervalChange = onIntervalChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                surfaceColor = surfaceColor,
                primaryColor = primaryColor,
                outlineColor = outlineColor,
                textColor = onSurfaceColor,
                secondaryColor = onSurfaceVariant
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Main input area - smaller height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(surfaceColor)
                    .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { focusRequester.requestFocus() }
                    .padding(16.dp)
            ) {
                if (inputText.isEmpty()) {
                    Text(
                        text = exampleHints.placeholder,
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            color = onSurfaceVariant.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
                
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                        color = if (hasUnknownLines) MaterialTheme.colorScheme.error else onSurfaceColor,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(primaryColor)
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Dynamic typing suggestions
            AnimatedVisibility(
                visible = typingSuggestions.isNotEmpty() && inputText.isNotEmpty(),
                enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
                exit = fadeOut()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(typingSuggestions) { suggestion ->
                        SuggestionChip(
                            text = suggestion,
                            onClick = {
                                val lines = inputText.lines().toMutableList()
                                if (lines.isEmpty()) {
                                    onInputTextChange(suggestion)
                                } else {
                                    lines[lines.lastIndex] = suggestion
                                    onInputTextChange(lines.joinToString("\n"))
                                }
                            },
                            surfaceColor = surfaceVariant,
                            primaryColor = primaryColor,
                            textColor = onSurfaceColor
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Parsed results preview

            AnimatedVisibility(
                visible = parseResult != null && parseResult!!.parsedExercises.isNotEmpty(),
                enter = slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(),
                exit = fadeOut(tween(150))
            ) {
                parseResult?.let { result ->
                    ParsedPreviewCompact(
                        result = result,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        surfaceColor = surfaceColor,
                        primaryColor = primaryColor,
                        outlineColor = outlineColor,
                        textColor = onSurfaceColor,
                        secondaryTextColor = onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.height(100.dp)) // Bottom padding for nav bar
        }
        
        // Examples popup
        AnimatedVisibility(
            visible = showExamplesPopup,
            enter = fadeIn(tween(200)) + scaleIn(
                initialScale = 0.9f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.9f),
            modifier = Modifier.fillMaxSize()
        ) {
            ExamplesPopup(
                hints = exampleHints,
                onDismiss = { showExamplesPopup = false },
                backgroundColor = backgroundColor,
                surfaceColor = surfaceColor,
                primaryColor = primaryColor,
                textColor = onSurfaceColor,
                secondaryTextColor = onSurfaceVariant,
                outlineColor = outlineColor
            )
        }
        
        // Success overlay
        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            val successColor = Color(0xFF30D158)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(successColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(successColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Saved!",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = onBackgroundColor
                        )
                    )
                }
            }
        }
        
        // Notification settings dialog
        if (showNotificationDialog) {
            NotificationSettingsDialog(
                onDismiss = { showNotificationDialog = false }
            )
        }
    }
}

@Composable
private fun ScheduleCard(

    lastWorkout: WorkoutLogEntry?,
    totalWorkouts: Int,
    workoutIntervalDays: Int,
    onIntervalChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    surfaceColor: Color,
    primaryColor: Color,
    outlineColor: Color,
    textColor: Color,
    secondaryColor: Color
) {
    var showIntervalPicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val inputFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    
    fun formatDate(dateStr: String?): String {
        if (dateStr == null) return "No workouts yet"
        return try {
            val date = inputFormatter.parse(dateStr)
            date?.let { dateFormatter.format(it) } ?: dateStr
        } catch (e: Exception) {
            dateStr
        }
    }
    
    fun daysAgo(dateStr: String?): String {
        if (dateStr == null) return ""
        return try {
            val date = inputFormatter.parse(dateStr) ?: return ""
            val now = Calendar.getInstance().time
            val diff = now.time - date.time
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            when {
                days == 0L -> "today"
                days == 1L -> "yesterday"
                days < 7 -> "$days days ago"
                else -> "${days / 7}w ago"
            }
        } catch (e: Exception) {
            ""
        }
    }
    
    // Calculate next workout status
    val nextWorkoutStatus = remember(lastWorkout, workoutIntervalDays) {
        if (lastWorkout == null) {
            "Ready to start!" to "Anytime"
        } else {
            try {
                val lastDate = inputFormatter.parse(lastWorkout.date) ?: return@remember "Time to train" to "Soon"
                val calendar = Calendar.getInstance().apply {
                    time = lastDate
                    add(Calendar.DAY_OF_YEAR, workoutIntervalDays)
                }
                val nextDate = calendar.time
                val now = Calendar.getInstance().time
                val daysUntil = TimeUnit.MILLISECONDS.toDays(nextDate.time - now.time)
                
                val hint = when {
                    daysUntil < 0 -> "Time to train!"
                    daysUntil == 0L -> "Workout day!"
                    daysUntil == 1L -> "Workout tomorrow"
                    daysUntil <= 2 -> "Rest day"
                    else -> "Rest • ${daysUntil}d until next"
                }
                
                val dateLabel = when {
                    daysUntil < 0 -> "Overdue"
                    daysUntil == 0L -> "Today"
                    daysUntil == 1L -> "Tomorrow"
                    else -> dateFormatter.format(nextDate)
                }
                
                hint to dateLabel
            } catch (e: Exception) {
                "Time to train" to "Soon"
            }
        }
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            // Header with workout count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Schedule",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                    )
                }
                // Workout count badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(primaryColor.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "$totalWorkouts logged",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = primaryColor
                        )
                    )
                }
            }
            
            Spacer(Modifier.height(14.dp))
            
            // Last workout row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Last workout",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = secondaryColor
                        )
                    )
                    Text(
                        formatDate(lastWorkout?.date),
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                    )
                    val ago = daysAgo(lastWorkout?.date)
                    if (ago.isNotEmpty()) {
                        Text(
                            ago,
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = secondaryColor
                            )
                        )
                    }
                }
                
                // Next due
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Next due",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = secondaryColor
                        )
                    )
                    Text(
                        nextWorkoutStatus.second,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (nextWorkoutStatus.second == "Overdue" || nextWorkoutStatus.second == "Today")
                                primaryColor else textColor
                        )
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Status hint
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (nextWorkoutStatus.second == "Overdue" || nextWorkoutStatus.second == "Today")
                            primaryColor.copy(alpha = 0.15f)
                        else outlineColor.copy(alpha = 0.08f)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    "→ ${nextWorkoutStatus.first}",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (nextWorkoutStatus.second == "Overdue" || nextWorkoutStatus.second == "Today")
                            primaryColor else textColor.copy(alpha = 0.8f)
                    )
                )
            }
            
            Spacer(Modifier.height(10.dp))
            
            // Interval picker row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(outlineColor.copy(alpha = 0.08f))
                    .clickable { showIntervalPicker = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Workout interval",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = secondaryColor
                    )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        when (workoutIntervalDays) {
                            1 -> "Daily"
                            7 -> "Weekly"
                            14 -> "Every 2 weeks"
                            else -> "Every $workoutIntervalDays days"
                        },
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = primaryColor
                        )
                    )
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = "Change interval",
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
    
    // Interval picker dialog
    if (showIntervalPicker) {
        IntervalPickerDialog(
            currentInterval = workoutIntervalDays,
            onIntervalSelected = { days ->
                onIntervalChange(days)
                showIntervalPicker = false
            },
            onDismiss = { showIntervalPicker = false },
            surfaceColor = surfaceColor,
            primaryColor = primaryColor,
            textColor = textColor,
            outlineColor = outlineColor
        )
    }
}

@Composable
private fun IntervalPickerDialog(
    currentInterval: Int,
    onIntervalSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    surfaceColor: Color,
    primaryColor: Color,
    textColor: Color,
    outlineColor: Color
) {
    val intervalOptions = listOf(
        3 to "Every 3 days",
        4 to "Every 4 days", 
        5 to "Every 5 days",
        7 to "Weekly (7 days)",
        10 to "Every 10 days",
        14 to "Every 2 weeks"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        title = {
            Text(
                "Workout Interval",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "How often do you want to work out?",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = textColor.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                intervalOptions.forEach { (days, label) ->
                    val isSelected = currentInterval == days
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) primaryColor.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) primaryColor else outlineColor.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onIntervalSelected(days) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label,
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) primaryColor else textColor
                            )
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textColor.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun SuggestionChip(
    text: String,
    onClick: () -> Unit,
    surfaceColor: Color,
    primaryColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = primaryColor
            )
        )
    }
}

@Composable
private fun ParsedPreviewCompact(
    result: ParseResult,
    modifier: Modifier = Modifier,
    surfaceColor: Color,
    primaryColor: Color,
    outlineColor: Color,
    textColor: Color,
    secondaryTextColor: Color
) {
    val warningColor = Color(0xFFFFD60A)
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${result.parsedExercises.size} exercise${if (result.parsedExercises.size != 1) "s" else ""}",
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            )
            Text(
                result.parsedExercises.joinToString(", ") { it.exercise.name },
                style = TextStyle(
                    fontSize = 13.sp,
                    color = secondaryTextColor
                ),
                maxLines = 1
            )
        }
        
        if (result.unrecognizedLines.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(warningColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    "${result.unrecognizedLines.size} ?",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = warningColor
                    )
                )
            }
        }
    }
}

data class ExampleHints(
    val placeholder: String,
    val examples: List<ExampleItem>
)

data class ExampleItem(
    val input: String,
    val output: String
)

private fun generateTypingSuggestions(

    currentInput: String,
    exercises: List<Exercise>
): List<String> {
    if (currentInput.isBlank()) return emptyList()
    
    val lastLine = currentInput.lines().lastOrNull()?.trim() ?: return emptyList()
    if (lastLine.isEmpty()) return emptyList()
    
    val lastLineLower = lastLine.lowercase()
    
    // Common aliases to check
    val aliases = mapOf(
        "bench" to "bench_press",
        "dl" to "deadlift",
        "dead" to "deadlift",
        "ohp" to "ohp",
        "squat" to "squat",
        "row" to "row",
        "pull" to "pulldown",
        "incline" to "incline_press",
        "inc" to "incline_press",
        "lat" to "pulldown",
        "calf" to "calf_raise",
        "leg" to "leg_press"
    )
    
    // Check if the line has a weight number - if so, adapt suggestions to use that weight
    val hasWeight = lastLineLower.any { it.isDigit() }
    
    if (hasWeight) {
        // Parse the exercise name and weight from input like "bench 115" or "bench 115 x"
        val parts = lastLineLower.split(Regex("\\s+"))
        if (parts.size >= 2) {
            val exercisePart = parts[0]
            val weightPart = parts.find { it.all { c -> c.isDigit() || c == '.' } }
            
            if (weightPart != null) {
                val weight = weightPart.toIntOrNull() ?: weightPart.toFloatOrNull()?.toInt() ?: 135
                
                // Find which exercise this matches
                var matchedName: String? = null
                
                // Check aliases first
                for ((alias, exerciseId) in aliases) {
                    if (exercisePart.startsWith(alias) || alias.startsWith(exercisePart)) {
                        exercises.find { it.id == exerciseId }?.let {
                            matchedName = alias
                        }
                        break
                    }
                }
                
                // Check exercise names
                if (matchedName == null) {
                    for (exercise in exercises) {
                        val nameParts = exercise.name.lowercase().split(" ")
                        if (nameParts.any { it.startsWith(exercisePart) }) {
                            matchedName = nameParts.first()
                            break
                        }
                    }
                }
                
                matchedName?.let { name ->
                    // Generate suggestions with the user's weight
                    return listOf(
                        "$name $weight x 6",
                        "$name $weight x 8",
                        "$name $weight 3x8",
                        "$name $weight 3x5"
                    )
                }
            }
        }
        // If we couldn't parse it but it has numbers, don't show suggestions
        return emptyList()
    }
    
    // No weight yet - show suggestions with default weights
    val formats = listOf(
        { name: String -> "$name 135 3x8" },        // sets x reps format
        { name: String -> "$name 185 @ 3x5" },      // @ sets x reps
        { name: String -> "$name 225 x 5" },        // weight x reps
        { name: String -> "$name 135 - 12, 10, 8" } // drop set style
    )
    
    val suggestions = mutableListOf<String>()
    
    var matchedName: String? = null
    
    // Check aliases first
    for ((alias, exerciseId) in aliases) {
        if (alias.startsWith(lastLineLower)) {
            exercises.find { it.id == exerciseId }?.let {
                matchedName = alias
            }
            break
        }
    }
    
    // Then check exercise names
    if (matchedName == null) {
        for (exercise in exercises) {
            val nameLower = exercise.name.lowercase()
            val nameWords = nameLower.split(" ")
            
            if (nameWords.any { it.startsWith(lastLineLower) } || nameLower.startsWith(lastLineLower)) {
                matchedName = nameWords.first()
                break
            }
        }
    }
    
    // Generate varied format suggestions for the matched name
    matchedName?.let { name ->
        // Shuffle formats a bit based on input to add variety
        val startIndex = name.length % formats.size
        for (i in 0 until minOf(4, formats.size)) {
            val formatIndex = (startIndex + i) % formats.size
            suggestions.add(formats[formatIndex](name))
        }
    }
    
    return suggestions.distinct().take(4)
}

private fun generateExampleHints(customExercises: List<Exercise>): ExampleHints {
    val available = (allExercises + customExercises).distinctBy { it.id }
    
    // Pick some exercises for examples
    val bench = available.find { it.id == "bench_press" || it.name.contains("bench", true) }
    val squat = available.find { it.id == "squat" || it.name.contains("squat", true) }
    val deadlift = available.find { it.id == "deadlift" || it.name.contains("dead", true) }
    val row = available.find { it.id == "row" || it.name.contains("row", true) }
    val incline = available.find { it.id == "incline_press" || it.name.contains("incline", true) }
    
    // Show varied formats in placeholder
    val placeholderLines = mutableListOf<String>()
    bench?.let { placeholderLines.add("bench 225 3x8") }
    squat?.let { placeholderLines.add("squat 315 @ 3x5") }
    deadlift?.let { placeholderLines.add("dl 405 x 3") }
    
    if (placeholderLines.isEmpty()) {
        available.firstOrNull()?.let {
            val shortName = it.name.split(" ").first().lowercase()
            placeholderLines.add("$shortName 135 3x10")
        }
    }
    
    val examples = mutableListOf<ExampleItem>()
    
    // Show different formats
    incline?.let {
        examples.add(ExampleItem(
            "incline 185 @ 3x5",
            "→ ${it.name}: 3 sets of 5 @ 185lbs"
        ))
    }
    bench?.let {
        examples.add(ExampleItem(
            "bench 225 3x8",
            "→ ${it.name}: 3 sets of 8 @ 225lbs"
        ))
    }
    squat?.let {
        examples.add(ExampleItem(
            "squat 315 x 5",
            "→ ${it.name}: 315lbs × 5 reps"
        ))
    }
    deadlift?.let {
        examples.add(ExampleItem(
            "dl 405 - 5, 3, 1",
            "→ ${it.name}: 405lbs × 5, 3, 1"
        ))
    }
    row?.let {
        examples.add(ExampleItem(
            "row 185 12, 10, 8",
            "→ ${it.name}: 185lbs × 12, 10, 8"
        ))
    }
    
    return ExampleHints(
        placeholder = placeholderLines.joinToString("\n"),
        examples = examples
    )
}

@Composable
private fun ExamplesPopup(
    hints: ExampleHints,
    onDismiss: () -> Unit,
    backgroundColor: Color,
    surfaceColor: Color,
    primaryColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    outlineColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(surfaceColor)
                .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Consume clicks
                )
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "How to Log",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                    Text(
                        "Type naturally, we parse it",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = secondaryTextColor
                        )
                    )
                }
            }
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(280.dp)
            ) {
                itemsIndexed(hints.examples) { index, example ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(index * 60L)
                        visible = true
                    }
                    
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInVertically(
                            initialOffsetY = { 20 },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ) + fadeIn()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(backgroundColor)
                                .padding(14.dp)
                        ) {
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(
                                        color = primaryColor,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp
                                    )) {
                                        append(example.input)
                                    }
                                }
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                example.output,
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    color = secondaryTextColor
                                )
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Aliases hint
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor)
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        "💡 Shortcuts",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "dl → Deadlift • ohp → Overhead Press • bench → Flat Bench Press",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = secondaryTextColor,
                            lineHeight = 18.sp
                        )
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Dismiss button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(primaryColor)
                    .clickable(onClick = onDismiss)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Got it",
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

