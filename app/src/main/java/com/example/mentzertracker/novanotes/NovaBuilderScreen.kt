package com.vincentlarkin.mentzertracker.novanotes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincentlarkin.mentzertracker.CUSTOM_EXERCISE_NAME_LIMIT
import com.vincentlarkin.mentzertracker.Exercise
import com.vincentlarkin.mentzertracker.UserWorkoutConfig
import com.vincentlarkin.mentzertracker.allExercises
import java.util.Locale
import java.util.UUID

@Composable
fun NovaBuilderScreen(
    initialConfig: UserWorkoutConfig,
    onDone: (UserWorkoutConfig) -> Unit,
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null
) {
    val baseExercises = allExercises
    val customExercises = remember(initialConfig) {
        mutableStateListOf<Exercise>().apply {
            addAll(initialConfig.customExercises)
        }
    }

    val aSelections = remember(initialConfig) {
        val initialA = initialConfig.workoutAExerciseIds.toSet()
        mutableStateMapOf<String, Boolean>().apply {
            (baseExercises + initialConfig.customExercises).forEach { ex ->
                this[ex.id] = ex.id in initialA
            }
        }
    }
    val bSelections = remember(initialConfig) {
        val initialB = initialConfig.workoutBExerciseIds.toSet()
        mutableStateMapOf<String, Boolean>().apply {
            (baseExercises + initialConfig.customExercises).forEach { ex ->
                this[ex.id] = ex.id in initialB
            }
        }
    }

    var errorText by remember { mutableStateOf<String?>(null) }
    var customNameInput by remember { mutableStateOf("") }
    var addingToWorkout by remember { mutableStateOf<String?>(null) } // "A" or "B" or null
    var selectedTab by remember { mutableStateOf(0) } // 0 = Workout A, 1 = Workout B
    var searchQuery by remember { mutableStateOf("") }

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    fun ensureSelectionEntry(id: String) {
        if (aSelections[id] == null) aSelections[id] = false
        if (bSelections[id] == null) bSelections[id] = false
    }

    fun addCustomExercise(rawName: String, forA: Boolean, forB: Boolean): String? {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) return "Enter a name"
        if (trimmed.length > CUSTOM_EXERCISE_NAME_LIMIT) return "Too long"
        
        val lower = trimmed.lowercase(Locale.getDefault())
        val existingNames = (baseExercises + customExercises).map { it.name.lowercase(Locale.getDefault()) }
        if (lower in existingNames) return "Already exists"
        
        val existingIds = (baseExercises + customExercises).map { it.id }.toSet()
        val newExercise = Exercise(
            id = generateCustomExerciseId(existingIds),
            name = trimmed
        )
        customExercises.add(newExercise)
        ensureSelectionEntry(newExercise.id)
        aSelections[newExercise.id] = forA
        bSelections[newExercise.id] = forB
        return null
    }

    fun removeCustomExercise(exercise: Exercise) {
        customExercises.removeAll { it.id == exercise.id }
        aSelections.remove(exercise.id)
        bSelections.remove(exercise.id)
    }

    val combinedExercises = baseExercises + customExercises.toList()
    
    // Filter exercises based on search query
    val filteredExercises = remember(combinedExercises, searchQuery) {
        if (searchQuery.isBlank()) {
            combinedExercises
        } else {
            val query = searchQuery.lowercase().trim()
            combinedExercises.filter { exercise ->
                exercise.name.lowercase().contains(query) ||
                exercise.id.lowercase().contains(query)
            }
        }
    }
    
    val selectedACount = aSelections.count { it.value }
    val selectedBCount = bSelections.count { it.value }
    val canSave = selectedACount >= 2 && selectedBCount >= 2

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .windowInsetsPadding(WindowInsets.statusBars)
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBack && onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(surfaceColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = onBackgroundColor
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Build Your Workouts",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = onBackgroundColor,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Select exercises for each workout",
                        fontSize = 14.sp,
                        color = onSurfaceVariantColor
                    )
                }
                
                if (onOpenSettings != null) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(surfaceColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = onSurfaceVariantColor
                        )
                    }
                }
            }

            // Workout tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkoutTab(
                    title = "Workout A",
                    count = selectedACount,
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    primaryColor = primaryColor,
                    surfaceColor = surfaceColor,
                    onBackgroundColor = onBackgroundColor,
                    onSurfaceVariantColor = onSurfaceVariantColor,
                    modifier = Modifier.weight(1f)
                )
                WorkoutTab(
                    title = "Workout B",
                    count = selectedBCount,
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    primaryColor = primaryColor,
                    surfaceColor = surfaceColor,
                    onBackgroundColor = onBackgroundColor,
                    onSurfaceVariantColor = onSurfaceVariantColor,
                    modifier = Modifier.weight(1f)
                )
            }

            // Search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceColor)
                    .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = onSurfaceVariantColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = onBackgroundColor
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(primaryColor),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            Box {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search exercises...",
                                        style = TextStyle(
                                            fontSize = 15.sp,
                                            color = onSurfaceVariantColor
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = onSurfaceVariantColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            
            // Info text with result count
            Text(
                text = if (searchQuery.isNotEmpty()) {
                    "${filteredExercises.size} exercises found • Select at least 2 per workout"
                } else {
                    "${combinedExercises.size} exercises • Select at least 2 per workout"
                },
                fontSize = 13.sp,
                color = onSurfaceVariantColor,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Exercise list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add custom exercise card
                item {
                    AddExerciseCard(
                        customNameInput = customNameInput,
                        onInputChange = { customNameInput = it },
                        onAdd = {
                            val error = addCustomExercise(
                                rawName = customNameInput,
                                forA = selectedTab == 0,
                                forB = selectedTab == 1
                            )
                            if (error == null) {
                                customNameInput = ""
                            } else {
                                errorText = error
                            }
                        },
                        surfaceColor = surfaceColor,
                        primaryColor = primaryColor,
                        onSurfaceVariantColor = onSurfaceVariantColor,
                        outlineColor = outlineColor,
                        onBackgroundColor = onBackgroundColor
                    )
                }

                // Error message
                if (errorText != null) {
                    item {
                        Text(
                            text = errorText!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                // Exercise items (filtered by search)
                items(filteredExercises, key = { it.id }) { exercise ->
                    val isSelected = if (selectedTab == 0) {
                        aSelections[exercise.id] == true
                    } else {
                        bSelections[exercise.id] == true
                    }
                    val isCustom = customExercises.any { it.id == exercise.id }
                    val isInOtherWorkout = if (selectedTab == 0) {
                        bSelections[exercise.id] == true
                    } else {
                        aSelections[exercise.id] == true
                    }

                    ExerciseCard(
                        exercise = exercise,
                        isSelected = isSelected,
                        isCustom = isCustom,
                        isInOtherWorkout = isInOtherWorkout,
                        onToggle = {
                            val map = if (selectedTab == 0) aSelections else bSelections
                            map[exercise.id] = !(map[exercise.id] ?: false)
                            errorText = null
                        },
                        onDelete = if (isCustom) {{ removeCustomExercise(exercise) }} else null,
                        surfaceColor = surfaceColor,
                        primaryColor = primaryColor,
                        onBackgroundColor = onBackgroundColor,
                        onSurfaceVariantColor = onSurfaceVariantColor,
                        outlineColor = outlineColor
                    )
                }

                // Bottom padding
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Save button - floating at bottom
        AnimatedVisibility(
            visible = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            val scale by animateFloatAsState(
                targetValue = if (canSave) 1f else 0.95f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "save_scale"
            )
            
            Box(
                modifier = Modifier
                    .scale(scale)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (canSave) primaryColor else surfaceColor
                    )
                    .border(
                        width = 1.dp,
                        color = if (canSave) Color.Transparent else outlineColor,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable(enabled = canSave) {
                        val combined = (baseExercises + customExercises.toList()).distinctBy { it.id }
                        val validIds = combined.map { it.id }.toSet()
                        val aIds = aSelections
                            .filter { (id, checked) -> checked && id in validIds }
                            .keys
                            .toList()
                        val bIds = bSelections
                            .filter { (id, checked) -> checked && id in validIds }
                            .keys
                            .toList()

                        when {
                            aIds.size < 2 -> errorText = "Need at least 2 exercises for Workout A"
                            bIds.size < 2 -> errorText = "Need at least 2 exercises for Workout B"
                            else -> {
                                errorText = null
                                onDone(
                                    UserWorkoutConfig(
                                        workoutAExerciseIds = aIds,
                                        workoutBExerciseIds = bIds,
                                        customExercises = customExercises.toList()
                                    )
                                )
                            }
                        }
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (canSave) Color.White else onSurfaceVariantColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (canSave) "Save Workouts" else "Select more exercises",
                        color = if (canSave) Color.White else onSurfaceVariantColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutTab(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    surfaceColor: Color,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.97f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tab_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) primaryColor.copy(alpha = 0.15f) else surfaceColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) primaryColor else surfaceColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) primaryColor else onBackgroundColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$count selected",
                fontSize = 13.sp,
                color = if (isSelected) primaryColor.copy(alpha = 0.8f) else onSurfaceVariantColor
            )
        }
    }
}

@Composable
private fun AddExerciseCard(
    customNameInput: String,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
    surfaceColor: Color,
    primaryColor: Color,
    onSurfaceVariantColor: Color,
    outlineColor: Color,
    onBackgroundColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
            .border(1.dp, outlineColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        BasicTextField(
            value = customNameInput,
            onValueChange = { if (it.length <= CUSTOM_EXERCISE_NAME_LIMIT) onInputChange(it) },
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = onBackgroundColor
            ),
            singleLine = true,
            cursorBrush = SolidColor(primaryColor),
            decorationBox = { innerTextField ->
                Box {
                    if (customNameInput.isEmpty()) {
                        Text(
                            text = "Add custom exercise...",
                            color = onSurfaceVariantColor,
                            fontSize = 15.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
        
        if (customNameInput.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(primaryColor)
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Add",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: Exercise,
    isSelected: Boolean,
    isCustom: Boolean,
    isInOtherWorkout: Boolean,
    onToggle: () -> Unit,
    onDelete: (() -> Unit)?,
    surfaceColor: Color,
    primaryColor: Color,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    outlineColor: Color
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.98f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )

    Row(
        modifier = Modifier
            .scale(scale)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) primaryColor.copy(alpha = 0.1f) else surfaceColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) primaryColor else outlineColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onToggle)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox circle
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) primaryColor else Color.Transparent
                )
                .border(
                    width = 2.dp,
                    color = if (isSelected) primaryColor else outlineColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.name,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) primaryColor else onBackgroundColor
            )
            if (isCustom || isInOtherWorkout) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (isCustom) {
                        Text(
                            text = "Custom",
                            fontSize = 12.sp,
                            color = onSurfaceVariantColor
                        )
                    }
                    if (isInOtherWorkout) {
                        Text(
                            text = "• Also in other workout",
                            fontSize = 12.sp,
                            color = primaryColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
        
        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = onSurfaceVariantColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun generateCustomExerciseId(existingIds: Set<String>): String {
    var candidate: String
    do {
        candidate = "custom_${UUID.randomUUID()}"
    } while (candidate in existingIds)
    return candidate
}



