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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincentlarkin.mentzertracker.CUSTOM_EXERCISE_NAME_LIMIT
import com.vincentlarkin.mentzertracker.Exercise
import com.vincentlarkin.mentzertracker.UserWorkoutConfig
import com.vincentlarkin.mentzertracker.allExercises
import java.util.Locale
import java.util.UUID

private data class ReferenceItem(
    val exercise: Exercise,
    val aliases: List<String>,
    val isCustom: Boolean
)

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

    var errorText by remember { mutableStateOf<String?>(null) }
    var customNameInput by remember { mutableStateOf("") }
    var customAliasInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }


    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    fun addCustomExercise(rawName: String, rawAlias: String): String? {
        val trimmedName = rawName.trim()
        if (trimmedName.isEmpty()) return "Enter a name"
        if (trimmedName.length > CUSTOM_EXERCISE_NAME_LIMIT) return "Name too long"

        val lowerName = trimmedName.lowercase(Locale.getDefault())
        val existingNames = (baseExercises + customExercises).map { it.name.lowercase(Locale.getDefault()) }
        if (lowerName in existingNames) return "Already exists"

        val trimmedAlias = rawAlias.trim()
        val lowerAlias = trimmedAlias.lowercase(Locale.getDefault())
        val aliasToStore = if (lowerAlias.isNotBlank() && lowerAlias != lowerName) lowerAlias else ""
        if (aliasToStore.isNotEmpty()) {
            if (aliasToStore.length > CUSTOM_EXERCISE_NAME_LIMIT) return "Alias too long"
            val existingAliases = WorkoutParser.getAliasMap(baseExercises + customExercises.toList())
                .keys
                .map { it.lowercase(Locale.getDefault()) }
            if (lowerAlias in existingAliases || lowerAlias in existingNames) return "Alias already used"
        }

        val existingIds = (baseExercises + customExercises).map { it.id }.toSet()
        val newExercise = Exercise(
            id = generateCustomExerciseId(existingIds),
            name = trimmedName,
            aliases = if (aliasToStore.isNotEmpty()) listOf(aliasToStore) else emptyList()
        )
        customExercises.add(newExercise)
        return null
    }

    fun removeCustomExercise(exercise: Exercise) {
        customExercises.removeAll { it.id == exercise.id }
    }


    val combinedExercises = baseExercises + customExercises.toList()

    val aliasMap = remember(combinedExercises) {
        WorkoutParser.getAliasMap(combinedExercises)
    }
    val aliasesByExerciseName = remember(aliasMap) {
        aliasMap.entries
            .groupBy({ it.value }) { it.key }
            .mapValues { entry -> entry.value.sorted() }
    }

    val referenceItems = remember(combinedExercises, aliasesByExerciseName) {
        combinedExercises.map { exercise ->
            ReferenceItem(
                exercise = exercise,
                aliases = aliasesByExerciseName[exercise.name].orEmpty(),
                isCustom = customExercises.any { it.id == exercise.id }
            )
        }
    }

    // Filter exercises based on search query
    val filteredExercises = remember(referenceItems, searchQuery) {
        if (searchQuery.isBlank()) {
            referenceItems
        } else {
            val query = searchQuery.lowercase().trim()
            referenceItems.filter { item ->
                item.exercise.name.lowercase().contains(query) ||
                item.aliases.any { alias -> alias.lowercase().contains(query) }
            }
        }
    }


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
                        text = "Workout Reference",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = onBackgroundColor,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "See official names and casual inputs",
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
                    "${filteredExercises.size} exercises found"
                } else {
                    "${combinedExercises.size} exercises"
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
                        customAliasInput = customAliasInput,
                        onNameChange = { customNameInput = it },
                        onAliasChange = { customAliasInput = it },
                        onAdd = {
                            val error = addCustomExercise(customNameInput, customAliasInput)
                            if (error == null) {
                                customNameInput = ""
                                customAliasInput = ""
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
                items(filteredExercises, key = { it.exercise.id }) { item ->
                    ReferenceExerciseCard(
                        item = item,
                        onDelete = if (item.isCustom) {{ removeCustomExercise(item.exercise) }} else null,
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

        val canSave = true

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
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
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
                        errorText = null
                        onDone(
                            UserWorkoutConfig(
                                workoutAExerciseIds = initialConfig.workoutAExerciseIds,
                                workoutBExerciseIds = initialConfig.workoutBExerciseIds,
                                customExercises = customExercises.toList()
                            )
                        )
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
                        text = "Save Exercises",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


@Composable
private fun AddExerciseCard(
    customNameInput: String,
    customAliasInput: String,
    onNameChange: (String) -> Unit,
    onAliasChange: (String) -> Unit,
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
        
        Column(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = customNameInput,
                onValueChange = { if (it.length <= CUSTOM_EXERCISE_NAME_LIMIT) onNameChange(it) },
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
                                text = "Proper Name",
                                color = onSurfaceVariantColor,
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            BasicTextField(
                value = customAliasInput,
                onValueChange = { if (it.length <= CUSTOM_EXERCISE_NAME_LIMIT) onAliasChange(it) },
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = onBackgroundColor
                ),
                singleLine = true,
                cursorBrush = SolidColor(primaryColor),
                decorationBox = { innerTextField ->
                    Box {
                        if (customAliasInput.isEmpty()) {
                            Text(
                                text = "Alias (optional)",
                                color = onSurfaceVariantColor,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        
        if (customNameInput.isNotBlank()) {
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
private fun ReferenceExerciseCard(
    item: ReferenceItem,
    onDelete: (() -> Unit)?,
    surfaceColor: Color,
    primaryColor: Color,
    onBackgroundColor: Color,
    onSurfaceVariantColor: Color,
    outlineColor: Color
) {
    val inputHints = if (item.aliases.isNotEmpty()) {
        item.aliases
    } else {
        listOf(item.exercise.name.lowercase(Locale.getDefault()))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
            .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.exercise.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = onBackgroundColor
            )
            Text(
                text = "Type: ${inputHints.joinToString(", ")}",
                fontSize = 12.sp,
                color = onSurfaceVariantColor,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (item.isCustom) {
                Text(
                    text = "Custom exercise",
                    fontSize = 12.sp,
                    color = primaryColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
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



