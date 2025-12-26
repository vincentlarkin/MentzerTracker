package com.vincentlarkin.mentzertracker.novanotes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincentlarkin.mentzertracker.Exercise
import com.vincentlarkin.mentzertracker.ExerciseSetEntry
import kotlinx.coroutines.delay

// Nova color palette - deeper, more refined
private val NovaBackground = Color(0xFF0A0A0F)
private val NovaSurface = Color(0xFF12121A)
private val NovaAccent = Color(0xFFFF3366)
private val NovaAccentSoft = Color(0xFF1A1A2E)
private val NovaTextPrimary = Color(0xFFF5F5F7)
private val NovaTextSecondary = Color(0xFF8E8E93)
private val NovaSuccess = Color(0xFF30D158)
private val NovaWarning = Color(0xFFFFD60A)
private val NovaBorder = Color(0xFF2C2C3A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaNotesScreen(
    customExercises: List<Exercise>,
    onSave: (List<ExerciseSetEntry>, String?) -> Unit,
    onBack: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    val parseResult by remember(inputText) {
        derivedStateOf {
            if (inputText.isBlank()) null
            else WorkoutParser.parse(inputText, customExercises)
        }
    }
    
    val hasValidSets = parseResult?.parsedExercises?.isNotEmpty() == true
    
    // Auto-focus on mount
    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }
    
    // Success animation reset
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(1500)
            showSuccess = false
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaBackground)
    ) {
        // Subtle gradient overlay at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NovaAccent.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "nova",
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp,
                                    color = NovaTextPrimary
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.clip(RectangleShape)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = NovaTextPrimary
                            )
                        }
                    },
                    actions = {
                        // Save button with animation
                        val saveScale by animateFloatAsState(
                            targetValue = if (hasValidSets) 1f else 0.9f,
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
                                .scale(saveScale)
                                .alpha(saveAlpha)
                                .clip(RectangleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (hasValidSets) NovaAccent else NovaBorder
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Save",
                                    tint = if (hasValidSets) Color.White else NovaTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
            ) {
                // Main input area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    if (inputText.isEmpty()) {
                        Text(
                            text = "bench 225 - 8, 8, 6\nsquat 315 x 5\ndl 405 3",
                            style = TextStyle(
                                fontSize = 20.sp,
                                lineHeight = 32.sp,
                                color = NovaTextSecondary.copy(alpha = 0.5f),
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
                            fontSize = 20.sp,
                            lineHeight = 32.sp,
                            color = NovaTextPrimary,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(NovaAccent)
                    )
                }
                
                // Parsed results preview
                AnimatedVisibility(
                    visible = parseResult != null,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(),
                    exit = fadeOut()
                ) {
                    parseResult?.let { result ->
                        ParsedResultsCard(
                            result = result,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp)
                        )
                    }
                }
            }
        }
        
        // Success overlay
        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(400)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NovaSuccess.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(NovaSuccess),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Logged",
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NovaTextPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ParsedResultsCard(
    result: ParseResult,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(NovaSurface)
            .border(1.dp, NovaBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(NovaAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = NovaAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Preview",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NovaTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                )
                Spacer(Modifier.weight(1f))
                if (result.parsedExercises.isNotEmpty()) {
                    Text(
                        "${result.parsedExercises.sumOf { it.sets.size }} sets",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = NovaAccent,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            
            // Parsed exercises
            LazyColumn(
                modifier = Modifier.height(
                    minOf(200.dp, (result.parsedExercises.size * 60).dp + 20.dp)
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = result.parsedExercises,
                    key = { idx, item -> "${item.exercise.id}_$idx" }
                ) { index, parsed ->
                    ParsedExerciseRow(
                        parsed = parsed,
                        index = index
                    )
                }
            }
            
            // Unrecognized lines warning
            if (result.unrecognizedLines.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NovaWarning.copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    Text(
                        "⚠",
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${result.unrecognizedLines.size} line(s) not recognized",
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = NovaWarning,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ParsedExerciseRow(
    parsed: ParsedExercise,
    index: Int
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(parsed) {
        delay(index * 50L)
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { 20 },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NovaAccentSoft)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    parsed.exercise.name,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NovaTextPrimary
                    )
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    parsed.sets.joinToString(" · ") { 
                        "${it.weight.toInt()}×${it.reps}"
                    },
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = NovaTextSecondary
                    )
                )
            }
            
            // Set count badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(NovaAccent.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "${parsed.sets.size}",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NovaAccent
                    )
                )
            }
        }
    }
}


