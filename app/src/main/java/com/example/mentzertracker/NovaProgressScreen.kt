package com.vincentlarkin.mentzertracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun NovaProgressScreen(
    logs: List<WorkoutLogEntry>,
    exercises: List<Exercise>,
    onUpdateLogs: (List<WorkoutLogEntry>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var showExercisePicker by remember { mutableStateOf(false) }
    
    // Get theme colors
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    
    // Build history map
    val histories: Map<Exercise, List<SessionPoint>> = remember(logs, exercises) {
        exercises.associateWith { ex ->
            logs.flatMapIndexed { index, log ->
                log.sets
                    .filter { it.exerciseId == ex.id }
                    .map { setEntry ->
                        SessionPoint(
                            sessionIndex = index + 1,
                            date = log.date,
                            weight = setEntry.weight,
                            reps = setEntry.reps,
                            notes = log.notes
                        )
                    }
            }
        }.filterValues { it.isNotEmpty() }
    }
    
    // Auto-select first exercise with history
    LaunchedEffect(histories) {
        if (selectedExercise == null || !histories.containsKey(selectedExercise)) {
            selectedExercise = histories.keys.firstOrNull()
        }
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
        
        if (histories.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(surfaceColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "No progress yet",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = onBackgroundColor
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Log workouts to see your progress",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = onSurfaceVariant
                        )
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp)
            ) {
                // Header
                Text(
                    "Progress",
                    style = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        color = onBackgroundColor
                    ),
                    modifier = Modifier.padding(top = 16.dp, bottom = 20.dp)
                )
                
                // Exercise picker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(surfaceColor)
                        .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable { showExercisePicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            selectedExercise?.name ?: "Select exercise",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = onSurfaceColor
                            )
                        )
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = "Select",
                            tint = onSurfaceVariant
                        )
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
                // Graph card
                selectedExercise?.let { exercise ->
                    val history = histories[exercise] ?: emptyList()
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(surfaceColor)
                            .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        if (history.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No data yet",
                                    style = TextStyle(
                                        fontSize = 15.sp,
                                        color = onSurfaceVariant
                                    )
                                )
                            }
                        } else {
                            NovaLineChart(
                                history = history,
                                modifier = Modifier.fillMaxSize(),
                                primaryColor = primaryColor,
                                backgroundColor = backgroundColor,
                                surfaceColor = surfaceColor,
                                textColor = onSurfaceVariant,
                                gridColor = outlineColor
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    
                    // Recent sessions
                    if (history.isNotEmpty()) {
                        Text(
                            "Recent",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = onBackgroundColor
                            ),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(history.reversed().take(10)) { index, point ->
                                var visible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) {
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
                                    SessionRow(
                                        point = point,
                                        surfaceColor = surfaceVariant,
                                        primaryColor = primaryColor,
                                        textColor = onSurfaceColor,
                                        secondaryColor = onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Exercise picker dialog
        if (showExercisePicker) {
            ExercisePickerDialog(
                exercises = histories.keys.toList(),
                selected = selectedExercise,
                onSelect = {
                    selectedExercise = it
                    showExercisePicker = false
                },
                onDismiss = { showExercisePicker = false },
                surfaceColor = surfaceColor,
                primaryColor = primaryColor,
                textColor = onSurfaceColor,
                outlineColor = outlineColor,
                surfaceVariant = surfaceVariant
            )
        }
    }
}

@Composable
private fun SessionRow(
    point: SessionPoint,
    surfaceColor: Color,
    primaryColor: Color,
    textColor: Color,
    secondaryColor: Color
) {
    val dateFormatter = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val inputFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    
    val formattedDate = try {
        val date = inputFormatter.parse(point.date)
        date?.let { dateFormatter.format(it) } ?: point.date
    } catch (e: Exception) {
        point.date
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                formattedDate,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            )
        }
        
        Text(
            "${point.weight.toInt()}lbs",
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor
            )
        )
        
        Spacer(Modifier.width(12.dp))
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(secondaryColor.copy(alpha = 0.1f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                "${point.reps} reps",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = secondaryColor
                )
            )
        }
    }
}

@Composable
private fun NovaLineChart(
    history: List<SessionPoint>,
    modifier: Modifier = Modifier,
    primaryColor: Color,
    backgroundColor: Color,
    surfaceColor: Color,
    textColor: Color,
    gridColor: Color
) {
    val density = LocalDensity.current
    var highlightedIndex by remember(history) { mutableStateOf<Int?>(null) }
    var pointPositions by remember(history) { mutableStateOf(emptyList<Offset>()) }
    var canvasSize by remember(history) { mutableStateOf(Size.Zero) }
    
    val orderedHistory = remember(history) {
        history.sortedWith(
            compareBy(
                { parseIsoDateMillis(it.date) ?: Long.MAX_VALUE },
                { it.sessionIndex }
            )
        )
    }
    
    val weights = orderedHistory.map { it.weight }
    val rawMax = weights.maxOrNull() ?: 0f
    val yMax = paddedMaxWeight(rawMax)
    val ticks = (0..4).map { i -> yMax * i / 4f }
    
    Row(modifier = modifier) {
        // Y-axis labels
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            for (value in ticks.reversed()) {
                Text(
                    text = value.toInt().toString(),
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = textColor
                    )
                )
            }
        }
        
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(orderedHistory) {
                        detectTapGestures { offset ->
                            val points = pointPositions
                            if (points.isEmpty()) {
                                highlightedIndex = null
                                return@detectTapGestures
                            }
                            val threshold = with(density) { 32.dp.toPx() }
                            val nearest = points
                                .mapIndexed { index, pt ->
                                    val dx = pt.x - offset.x
                                    val dy = pt.y - offset.y
                                    index to (dx * dx + dy * dy)
                                }
                                .minByOrNull { it.second }
                            if (nearest != null && nearest.second <= threshold * threshold) {
                                highlightedIndex = nearest.first
                            } else {
                                highlightedIndex = null
                            }
                        }
                    }
            ) {
                canvasSize = size
                
                val width = size.width
                val height = size.height
                
                val leftPadding = 8.dp.toPx()
                val rightPadding = 8.dp.toPx()
                val topPadding = 16.dp.toPx()
                val bottomPadding = 8.dp.toPx()
                
                val usableWidth = width - leftPadding - rightPadding
                val usableHeight = height - topPadding - bottomPadding
                
                val stepX = if (orderedHistory.size == 1) 0f else usableWidth / (orderedHistory.size - 1)
                
                fun yForWeight(w: Float): Float {
                    val normalized = (w / yMax).coerceIn(0f, 1f)
                    return topPadding + (1f - normalized) * usableHeight
                }
                
                val points = orderedHistory.mapIndexed { index, point ->
                    val x = leftPadding + stepX * index
                    val y = yForWeight(point.weight)
                    Offset(x, y)
                }
                pointPositions = points
                
                // Grid lines
                ticks.forEach { tickValue ->
                    val y = yForWeight(tickValue)
                    drawLine(
                        color = gridColor.copy(alpha = 0.2f),
                        start = Offset(leftPadding, y),
                        end = Offset(width - rightPadding, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                // Line + fill
                if (points.size >= 2) {
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                    
                    val axisY = yForWeight(0f)
                    val fillPath = Path().apply {
                        moveTo(points.first().x, axisY)
                        for (pt in points) {
                            lineTo(pt.x, pt.y)
                        }
                        lineTo(points.last().x, axisY)
                        close()
                    }
                    
                    drawPath(
                        path = fillPath,
                        color = primaryColor.copy(alpha = 0.15f)
                    )
                    
                    drawPath(
                        path = linePath,
                        color = primaryColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                
                // Points
                val selectedIndex = highlightedIndex
                points.forEachIndexed { index, pt ->
                    val isSelected = selectedIndex == index
                    val radius = if (isSelected) 8.dp else 5.dp
                    drawCircle(
                        color = primaryColor,
                        radius = radius.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = if (isSelected) backgroundColor else surfaceColor,
                        radius = (radius - 2.dp).toPx(),
                        center = pt
                    )
                }
            }
            
            // Tooltip
            val selectedIndex = highlightedIndex
            if (selectedIndex != null) {
                val point = pointPositions.getOrNull(selectedIndex)
                val session = orderedHistory.getOrNull(selectedIndex)
                if (point != null && session != null && canvasSize != Size.Zero) {
                    val tooltipWidth = 120.dp
                    val tooltipHeight = 60.dp
                    val tooltipWidthPx = with(density) { tooltipWidth.toPx() }
                    val tooltipHeightPx = with(density) { tooltipHeight.toPx() }
                    val verticalGapPx = with(density) { 12.dp.toPx() }
                    
                    val xPx = (point.x - tooltipWidthPx / 2f).coerceIn(
                        0f,
                        canvasSize.width - tooltipWidthPx
                    )
                    val yPx = (point.y - tooltipHeightPx - verticalGapPx).coerceAtLeast(0f)
                    
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(xPx.roundToInt(), yPx.roundToInt())
                            }
                            .width(tooltipWidth)
                            .clip(RoundedCornerShape(10.dp))
                            .background(surfaceColor)
                            .border(1.dp, gridColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                "${session.weight.toInt()} lbs",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryColor
                                )
                            )
                            Text(
                                "${session.reps} reps",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = textColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExercisePickerDialog(
    exercises: List<Exercise>,
    selected: Exercise?,
    onSelect: (Exercise) -> Unit,
    onDismiss: () -> Unit,
    surfaceColor: Color,
    primaryColor: Color,
    textColor: Color,
    outlineColor: Color,
    surfaceVariant: Color
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
                .padding(32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(surfaceColor)
                .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(20.dp)
        ) {
            Text(
                "Select Exercise",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(300.dp)
            ) {
                items(exercises) { exercise ->
                    val isSelected = exercise == selected
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) primaryColor.copy(alpha = 0.15f)
                                else surfaceVariant
                            )
                            .clickable { onSelect(exercise) }
                            .padding(14.dp)
                    ) {
                        Text(
                            exercise.name,
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) primaryColor else textColor
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun paddedMaxWeight(rawMax: Float): Float {
    if (rawMax <= 0f) return 50f
    val rounded = (ceil(rawMax / 50f) * 50f)
    return maxOf(150f, rounded)
}

private fun parseIsoDateMillis(dateStr: String): Long? {
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)?.time
    } catch (_: Exception) {
        null
    }
}
