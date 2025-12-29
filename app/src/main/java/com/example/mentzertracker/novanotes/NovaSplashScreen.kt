package com.vincentlarkin.mentzertracker.novanotes

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincentlarkin.mentzertracker.ThemeMode
import com.vincentlarkin.mentzertracker.importBackupFromJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NovaSplashScreen(
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
    onImportBackup: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showImportConfirm by remember { mutableStateOf(false) }
    
    // Animation states
    var showContent by remember { mutableStateOf(false) }
    val iconScale = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
        iconScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
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
                    "Import complete. Loading backup..."
                } else {
                    "Import failed: ${result.exceptionOrNull()?.localizedMessage ?: "unknown error"}"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                if (result.isSuccess) {
                    onImportBackup(result.getOrThrow())
                }
            }
        } else {
            Toast.makeText(context, "Import canceled", Toast.LENGTH_SHORT).show()
        }
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            containerColor = surfaceColor,
            title = {
                Text(
                    "Import Backup",
                    color = onBackgroundColor,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    "This will replace all your current data including workouts, logs, and preferences. This action cannot be undone.",
                    color = onSurfaceVariantColor
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportConfirm = false
                        importLauncher.launch(arrayOf("application/json"))
                    }
                ) {
                    Text("Import", color = primaryColor, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) {
                    Text("Cancel", color = onSurfaceVariantColor)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(24.dp)
    ) {
        // Settings icon - top right
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(surfaceColor.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = onSurfaceVariantColor
            )
        }

        // Main content - centered
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated icon
            Box(
                modifier = Modifier
                    .scale(iconScale.value)
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.2f),
                                primaryColor.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.5f),
                                primaryColor.copy(alpha = 0.2f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title with animation
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(600, delayMillis = 200)) +
                        slideInVertically(
                            initialOffsetY = { 30 },
                            animationSpec = tween(600, delayMillis = 200, easing = FastOutSlowInEasing)
                        )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NovaNotes",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = onBackgroundColor,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "by MentzerTracker",
                        fontSize = 14.sp,
                        color = onSurfaceVariantColor,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tagline
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(600, delayMillis = 400)) +
                        slideInVertically(
                            initialOffsetY = { 20 },
                            animationSpec = tween(600, delayMillis = 400, easing = FastOutSlowInEasing)
                        )
            ) {
                Text(
                    text = "Track your A/B workouts\nwith natural language",
                    fontSize = 16.sp,
                    color = onSurfaceVariantColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Start button
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(600, delayMillis = 600)) +
                        slideInVertically(
                            initialOffsetY = { 20 },
                            animationSpec = tween(600, delayMillis = 600, easing = FastOutSlowInEasing)
                        )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Primary Start button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(primaryColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onStart
                            )
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Get Started",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Import backup option
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { showImportConfirm = true }
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = onSurfaceVariantColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Import existing backup",
                            color = onSurfaceVariantColor,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Version info at bottom
        AnimatedVisibility(
            visible = showContent,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(600, delayMillis = 800))
        ) {
            Text(
                text = "v2.0.1-beta",
                fontSize = 12.sp,
                color = onSurfaceVariantColor.copy(alpha = 0.5f)
            )
        }
    }
}



