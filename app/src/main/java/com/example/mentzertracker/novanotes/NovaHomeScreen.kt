package com.vincentlarkin.mentzertracker.novanotes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.vincentlarkin.mentzertracker.WorkoutLogEntry
import com.vincentlarkin.mentzertracker.allExercises
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NovaHomeScreen(
    customExercises: List<Exercise>,
    recentLogs: List<WorkoutLogEntry>,
    onSave: (List<ExerciseSetEntry>, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    var showExamplesPopup by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
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
    
    val parseResult by remember(inputText) {
        derivedStateOf {
            if (inputText.isBlank()) null
            else WorkoutParser.parse(inputText, customExercises)
        }
    }
    
    val hasValidSets = parseResult?.parsedExercises?.isNotEmpty() == true
    
    // Success animation reset
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(1200)
            showSuccess = false
        }
    }
    
    // Generate dynamic examples based on actual exercises
    val allAvailableExercises = remember(customExercises) {
        (allExercises + customExercises).distinctBy { it.id }
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
                        "Log Workout",
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
                                onSave(sets, null)
                                showSuccess = true
                                inputText = ""
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
            
            // Main input area - smaller height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
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
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                        color = onSurfaceColor,
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
                                // Append the suggestion to the current line
                                val lines = inputText.lines().toMutableList()
                                if (lines.isEmpty()) {
                                    inputText = suggestion
                                } else {
                                    val lastLine = lines.last()
                                    // If the line has content, replace the partial match
                                    lines[lines.lastIndex] = suggestion
                                    inputText = lines.joinToString("\n")
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
            
            Spacer(Modifier.height(12.dp))
            
            // Last workout hint (when input is empty)
            AnimatedVisibility(
                visible = inputText.isEmpty() && recentLogs.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LastWorkoutHint(
                    logs = recentLogs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    surfaceColor = surfaceVariant,
                    textColor = onSurfaceColor,
                    secondaryTextColor = onSurfaceVariant
                )
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
    }
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

@Composable
private fun LastWorkoutHint(
    logs: List<WorkoutLogEntry>,
    modifier: Modifier = Modifier,
    surfaceColor: Color,
    textColor: Color,
    secondaryTextColor: Color
) {
    val lastLog = logs.lastOrNull() ?: return
    val dateFormatter = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val inputFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    
    val formattedDate = try {
        val date = inputFormatter.parse(lastLog.date)
        date?.let { dateFormatter.format(it) } ?: lastLog.date
    } catch (e: Exception) {
        lastLog.date
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "📝",
                fontSize = 16.sp
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Last: $formattedDate",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                )
                Text(
                    "${lastLog.sets.size} sets logged",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = secondaryTextColor
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
    
    val lastLine = currentInput.lines().lastOrNull()?.trim()?.lowercase() ?: return emptyList()
    if (lastLine.isEmpty()) return emptyList()
    
    // Check if the line already looks complete (has numbers)
    if (lastLine.any { it.isDigit() }) return emptyList()
    
    // Find matching exercises
    val suggestions = mutableListOf<String>()
    
    // Common aliases to check
    val aliases = mapOf(
        "bench" to "bench_press",
        "dl" to "deadlift",
        "dead" to "deadlift",
        "ohp" to "ohp",
        "squat" to "squat",
        "row" to "row",
        "pull" to "pulldown"
    )
    
    for (exercise in exercises) {
        val nameLower = exercise.name.lowercase()
        val nameWords = nameLower.split(" ")
        
        // Check if any word starts with what user typed
        if (nameWords.any { it.startsWith(lastLine) } || nameLower.startsWith(lastLine)) {
            // Suggest the shorthand + example format
            val shortName = nameWords.first()
            suggestions.add("$shortName 135 - 10, 8, 6")
            if (suggestions.size >= 3) break
        }
    }
    
    // Also suggest based on aliases
    for ((alias, exerciseId) in aliases) {
        if (alias.startsWith(lastLine) && suggestions.size < 4) {
            val exercise = exercises.find { it.id == exerciseId }
            if (exercise != null) {
                suggestions.add("$alias 135 - 10, 8, 6")
            }
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
    
    val placeholderLines = mutableListOf<String>()
    bench?.let { placeholderLines.add("bench 225 - 8, 8, 6") }
    squat?.let { placeholderLines.add("squat 315 x 5") }
    deadlift?.let { placeholderLines.add("dl 405 3") }
    
    if (placeholderLines.isEmpty()) {
        // Fallback with first available
        available.firstOrNull()?.let {
            val shortName = it.name.split(" ").first().lowercase()
            placeholderLines.add("$shortName 135 - 10, 10, 8")
        }
    }
    
    val examples = mutableListOf<ExampleItem>()
    
    bench?.let {
        examples.add(ExampleItem(
            "bench 225 - 8, 8, 6",
            "→ ${it.name}: 225lbs × 8, 8, 6"
        ))
    }
    squat?.let {
        examples.add(ExampleItem(
            "squat 3x5 @ 315",
            "→ ${it.name}: 3 sets of 5 @ 315lbs"
        ))
    }
    deadlift?.let {
        examples.add(ExampleItem(
            "dl 405 x 3",
            "→ ${it.name}: 405lbs × 3"
        ))
    }
    row?.let {
        examples.add(ExampleItem(
            "row 185 12, 10, 8",
            "→ ${it.name}: 185lbs × 12, 10, 8"
        ))
    }
    
    // Add format examples
    examples.add(ExampleItem(
        "ohp 135 8",
        "→ Overhead Press: 135lbs × 8"
    ))
    
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
