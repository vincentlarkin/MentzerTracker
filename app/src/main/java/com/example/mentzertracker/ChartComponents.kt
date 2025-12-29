package com.vincentlarkin.mentzertracker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Shared chart components used by FullProgressScreen and other parts of the app.
 */

@Composable
fun ExerciseDropdown(
    exercisesWithHistory: List<Exercise>,
    selected: Exercise,
    onSelectedChange: (Exercise) -> Unit,
    showAllOption: Boolean = false,
    isAllSelected: Boolean = false,
    onAllSelected: (() -> Unit)? = null
) {
    val expandedState = remember { mutableStateOf(false) }
    val expanded = expandedState.value

    OutlinedButton(
        onClick = { expandedState.value = true },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape
    ) {
        Text(
            text = if (isAllSelected) "All Exercises" else selected.name,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
        Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = "Choose exercise"
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expandedState.value = false }
    ) {
        if (showAllOption && onAllSelected != null) {
            DropdownMenuItem(
                text = { 
                    Text(
                        "All Exercises",
                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = {
                    onAllSelected()
                    expandedState.value = false
                }
            )
        }
        exercisesWithHistory.forEach { ex ->
            DropdownMenuItem(
                text = { Text(ex.name) },
                onClick = {
                    onSelectedChange(ex)
                    expandedState.value = false
                }
            )
        }
    }
}

@Composable
fun ExerciseLineChart(
    history: List<SessionPoint>,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No data yet for this exercise.")
        }
        return
    }

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

    val primary = MaterialTheme.colorScheme.primary
    val fillColor = primary.copy(alpha = 0.25f)
    val gridColor = Color.Gray.copy(alpha = 0.25f)
    val tooltipContainer = MaterialTheme.colorScheme.surfaceVariant
    val tooltipBorder = MaterialTheme.colorScheme.outline
    val tooltipTextColor = MaterialTheme.colorScheme.onSurface
    val chartBackground = MaterialTheme.colorScheme.background

    val density = LocalDensity.current
    var highlightedIndex by remember(orderedHistory) { mutableStateOf<Int?>(null) }
    var pointPositions by remember(orderedHistory) { mutableStateOf(emptyList<Offset>()) }
    var canvasSize by remember(orderedHistory) { mutableStateOf(Size.Zero) }

    Row(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            for (value in ticks.reversed()) {
                Text(
                    text = value.toInt().toString(),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.End
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

                val leftPadding = 16.dp.toPx()
                val rightPadding = 12.dp.toPx()
                val topPadding = 16.dp.toPx()
                val bottomPadding = 24.dp.toPx()

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

                ticks.forEach { tickValue ->
                    val y = yForWeight(tickValue)
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPadding, y),
                        end = Offset(width - rightPadding, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val axisY = yForWeight(0f)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.6f),
                    start = Offset(leftPadding, axisY),
                    end = Offset(width - rightPadding, axisY),
                    strokeWidth = 2.dp.toPx()
                )

                if (points.size >= 2) {
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }

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
                        color = fillColor
                    )

                    drawPath(
                        path = linePath,
                        color = primary,
                        style = Stroke(
                            width = 4.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                val selectedIndex = highlightedIndex

                points.forEachIndexed { index, pt ->
                    val isSelected = selectedIndex == index
                    val outerRadius = if (isSelected) 7.dp else 6.dp
                    val innerRadius = if (isSelected) 4.dp else 3.dp
                    drawCircle(
                        color = primary,
                        radius = outerRadius.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = if (isSelected) chartBackground else Color.Black,
                        radius = innerRadius.toPx(),
                        center = pt
                    )
                }
            }

            val selectedIndex = highlightedIndex
            if (selectedIndex != null) {
                val point = pointPositions.getOrNull(selectedIndex)
                val session = orderedHistory.getOrNull(selectedIndex)
                if (point != null && session != null && canvasSize != Size.Zero) {
                    val tooltipWidth = 160.dp
                    val tooltipHeight = 72.dp
                    val verticalGap = 12.dp
                    val tooltipWidthPx = with(density) { tooltipWidth.toPx() }
                    val tooltipHeightPx = with(density) { tooltipHeight.toPx() }
                    val verticalGapPx = with(density) { verticalGap.toPx() }

                    val xPx = (point.x - tooltipWidthPx / 2f).coerceIn(
                        0f,
                        canvasSize.width - tooltipWidthPx
                    )
                    val yPx = (point.y - tooltipHeightPx - verticalGapPx).coerceAtLeast(0f)

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    xPx.roundToInt(),
                                    yPx.roundToInt()
                                )
                            }
                            .width(tooltipWidth)
                            .background(tooltipContainer, RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                color = tooltipBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = friendlyDate(
                                    dateStr = session.date,
                                    includeYear = orderedHistory.hasMultipleYears
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = tooltipTextColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = weightLabel(session.weight),
                                style = MaterialTheme.typography.bodySmall,
                                color = tooltipTextColor
                            )
                            Text(
                                text = "${session.reps} rep" + if (session.reps == 1) "" else "s",
                                style = MaterialTheme.typography.bodySmall,
                                color = tooltipTextColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Multi-line chart for showing all exercises on a single graph.
 */
@Composable
fun MultiExerciseLineChart(
    exerciseData: Map<Exercise, List<SessionPoint>>,
    modifier: Modifier = Modifier
) {
    if (exerciseData.isEmpty() || exerciseData.values.all { it.isEmpty() }) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No data yet.")
        }
        return
    }

    // Get all weights across all exercises to determine Y scale
    val allWeights = exerciseData.values.flatten().map { it.weight }
    val rawMax = allWeights.maxOrNull() ?: 0f
    val yMax = paddedMaxWeight(rawMax)
    val ticks = (0..4).map { i -> yMax * i / 4f }

    // Colors for different exercises
    val exerciseColors = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFFEC4899), // Pink
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFF8B5CF6), // Violet
        Color(0xFF06B6D4), // Cyan
        Color(0xFFF97316), // Orange
        Color(0xFF84CC16), // Lime
    )

    val gridColor = Color.Gray.copy(alpha = 0.25f)
    val chartBackground = MaterialTheme.colorScheme.background

    val density = LocalDensity.current
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Row(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            for (value in ticks.reversed()) {
                Text(
                    text = value.toInt().toString(),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.End
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Canvas(
                modifier = Modifier.matchParentSize()
            ) {
                canvasSize = size

                val width = size.width
                val height = size.height

                val leftPadding = 16.dp.toPx()
                val rightPadding = 12.dp.toPx()
                val topPadding = 16.dp.toPx()
                val bottomPadding = 24.dp.toPx()

                val usableWidth = width - leftPadding - rightPadding
                val usableHeight = height - topPadding - bottomPadding

                fun yForWeight(w: Float): Float {
                    val normalized = (w / yMax).coerceIn(0f, 1f)
                    return topPadding + (1f - normalized) * usableHeight
                }

                // Draw grid lines
                ticks.forEach { tickValue ->
                    val y = yForWeight(tickValue)
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPadding, y),
                        end = Offset(width - rightPadding, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw axis
                val axisY = yForWeight(0f)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.6f),
                    start = Offset(leftPadding, axisY),
                    end = Offset(width - rightPadding, axisY),
                    strokeWidth = 2.dp.toPx()
                )

                // Draw each exercise line
                exerciseData.entries.forEachIndexed { index, (_, history) ->
                    if (history.isEmpty()) return@forEachIndexed

                    val orderedHistory = history.sortedWith(
                        compareBy(
                            { parseIsoDateMillis(it.date) ?: Long.MAX_VALUE },
                            { it.sessionIndex }
                        )
                    )

                    val color = exerciseColors[index % exerciseColors.size]
                    val stepX = if (orderedHistory.size == 1) 0f else usableWidth / (orderedHistory.size - 1)

                    val points = orderedHistory.mapIndexed { pointIndex, point ->
                        val x = leftPadding + stepX * pointIndex
                        val y = yForWeight(point.weight)
                        Offset(x, y)
                    }

                    // Draw line
                    if (points.size >= 2) {
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }

                        drawPath(
                            path = linePath,
                            color = color,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    // Draw points
                    points.forEach { pt ->
                        drawCircle(
                            color = color,
                            radius = 5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = chartBackground,
                            radius = 2.5.dp.toPx(),
                            center = pt
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

private fun weightLabel(weight: Float): String {
    return if (weight % 1f == 0f) {
        "${weight.toInt()} lbs"
    } else {
        "${String.format(Locale.getDefault(), "%.1f", weight)} lbs"
    }
}

private fun friendlyDate(dateStr: String, includeYear: Boolean): String {
    return try {
        val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val pattern = if (includeYear) "MMM d, yyyy" else "MMM d"
        val outFmt = SimpleDateFormat(pattern, Locale.getDefault())
        val date = inFmt.parse(dateStr)
        if (date != null) outFmt.format(date) else dateStr
    } catch (_: Exception) {
        dateStr
    }
}

private val List<SessionPoint>.hasMultipleYears: Boolean
    get() {
        if (isEmpty()) return false
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val years = mapNotNull {
            try {
                formatter.parse(it.date)?.let { date ->
                    SimpleDateFormat("yyyy", Locale.getDefault()).format(date)
                }
            } catch (_: Exception) {
                null
            }
        }.toSet()
        return years.size > 1
    }

