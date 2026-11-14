package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ocr.OcrConfigManager
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Full-screen dynamic UI overlay displayed for exactly 3 seconds when an OCR hot-fix payload is applied.
 * Features:
 * 1. Falling binary matrix particles flowing down the screen.
 * 2. Asset-based rotating mechanical gear animation.
 * 3. Smooth progress bar filling from 0% to 100%.
 * 4. Completion state transitioning to a glowing green checkmark stating "App Logic Optimised Successfully!".
 */
@Composable
fun HotFixOptimizationOverlay(
    event: OcrConfigManager.HotFixEvent,
    onDismiss: () -> Unit
) {
    var isCompleted by remember { mutableStateOf(false) }
    val progressAnimatable = remember { Animatable(0f) }
    var currentStepText by remember { mutableStateOf("Intercepting hot_fix_prompt payload...") }

    // Exactly 3000ms duration lifecycle
    LaunchedEffect(event) {
        // Stage 1: 0% -> 35%
        currentStepText = "Intercepting hot_fix_prompt payload..."
        progressAnimatable.animateTo(
            targetValue = 0.35f,
            animationSpec = tween(durationMillis = 700, easing = LinearEasing)
        )

        // Stage 2: 35% -> 70%
        currentStepText = "Patching ocr_config.json & updating Gemini system instructions..."
        progressAnimatable.animateTo(
            targetValue = 0.70f,
            animationSpec = tween(durationMillis = 800, easing = LinearEasing)
        )

        // Stage 3: 70% -> 100%
        currentStepText = "Compiling and validating spatial extraction pipeline..."
        progressAnimatable.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
        )

        // Completion transition at ~2250ms
        isCompleted = true
        currentStepText = "App Logic Optimised Successfully!"

        // Hold success checkmark state until exactly 3000ms, then dismiss
        delay(750)
        onDismiss()
    }

    Dialog(
        onDismissRequest = { /* Modal: auto dismisses after 3 seconds */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF0070B14))
                .testTag("hot_fix_optimization_overlay")
        ) {
            // Layer 1: Falling Binary Matrix Particles
            BinaryMatrixRainBackground(modifier = Modifier.fillMaxSize())

            // Layer 2: Center Cybernetic Modal Card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0F172A).copy(alpha = 0.95f),
                                    Color(0xFF090D16).copy(alpha = 0.98f)
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = if (isCompleted) {
                                    listOf(Color(0xFF00FF66), Color(0xFF00E5FF))
                                } else {
                                    listOf(Color(0xFF00E5FF), Color(0xFF10B981))
                                }
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isCompleted) Color(0xFF00FF66).copy(alpha = 0.15f)
                                else Color(0xFF00E5FF).copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Memory,
                            contentDescription = null,
                            tint = if (isCompleted) Color(0xFF00FF66) else Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCompleted) "HOT-FIX DEPLOYED" else "LIVE OCR RE-CONFIGURATION",
                            color = if (isCompleted) Color(0xFF00FF66) else Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Animated Visual: Rotating Gears vs Green Checkmark
                    Box(
                        modifier = Modifier
                            .size(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isCompleted) {
                            // Asset-based Rotating Gear Animation
                            RotatingGearSystem(modifier = Modifier.fillMaxSize())
                        } else {
                            // Transitioned Glowing Green Checkmark
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00FF66).copy(alpha = 0.18f))
                                    .border(2.dp, Color(0xFF00FF66), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Success",
                                    tint = Color(0xFF00FF66),
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Dynamic Title
                    Text(
                        text = if (isCompleted) {
                            "App Logic Optimised Successfully!"
                        } else {
                            "Applying Live Hot-Fix..."
                        },
                        color = Color.White,
                        fontSize = if (isCompleted) 20.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Subtitle / Status Description
                    Text(
                        text = if (isCompleted) {
                            "Gemini OCR instructions updated in memory & saved to ocr_config.json (v${event.version})."
                        } else {
                            currentStepText
                        },
                        color = if (isCompleted) Color(0xFFD1FAE5) else Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Progress Bar filling 0% to 100%
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pipeline Status",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${(progressAnimatable.value * 100).toInt()}%",
                                color = if (isCompleted) Color(0xFF00FF66) else Color(0xFF00E5FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { progressAnimatable.value },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (isCompleted) Color(0xFF00FF66) else Color(0xFF00E5FF),
                            trackColor = Color(0xFF1E293B)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Config target pill
                    Text(
                        text = "Target: /data/user/0/.../files/ocr_config.json",
                        color = Color(0xFF475569),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Asset-based rotating mechanical gear animation utilizing R.drawable.ic_tech_gear
 * with dual interlocking rotating elements.
 */
@Composable
fun RotatingGearSystem(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "gear_rotation")
    val rotationPrimary by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "primary_gear"
    )
    val rotationSecondary by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "secondary_gear"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Primary Main Tech Gear (Asset-based)
        Icon(
            painter = painterResource(id = R.drawable.ic_tech_gear),
            contentDescription = "Rotating Gear",
            tint = Color(0xFF00E5FF),
            modifier = Modifier
                .size(92.dp)
                .rotate(rotationPrimary)
        )

        // Secondary Interlocking Offset Gear (Asset-based)
        Icon(
            painter = painterResource(id = R.drawable.ic_tech_gear),
            contentDescription = "Interlocking Gear",
            tint = Color(0xFF00FF66),
            modifier = Modifier
                .size(54.dp)
                .align(Alignment.BottomEnd)
                .rotate(rotationSecondary)
        )

        // Glowing center core pulse
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(0xFF00E5FF))
        )
    }
}

/**
 * High-performance falling binary matrix particles background ('0' and '1' code stream).
 */
@Composable
fun BinaryMatrixRainBackground(modifier: Modifier = Modifier) {
    var tick by remember { mutableFloatStateOf(0f) }

    // Continuous tick animation for matrix flow
    LaunchedEffect(Unit) {
        while (true) {
            delay(40)
            tick += 1f
        }
    }

    // Pre-calculate randomized matrix column seeds
    val columnCount = 24
    val columns = remember {
        List(columnCount) { idx ->
            MatrixColumnState(
                speed = Random.nextFloat() * 1.5f + 0.8f,
                offset = Random.nextFloat() * 1000f,
                chars = List(25) { if (Random.nextBoolean()) "1" else "0" }
            )
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val colWidth = width / columnCount

        val textPaint = android.graphics.Paint().apply {
            textSize = 28f
            typeface = android.graphics.Typeface.MONOSPACE
            isAntiAlias = true
        }

        columns.forEachIndexed { colIdx, col ->
            val x = colIdx * colWidth + (colWidth / 4)
            val streamY = ((tick * col.speed * 8f) + col.offset) % (height + 500f)

            col.chars.forEachIndexed { charIdx, char ->
                val y = streamY - (charIdx * 32f)
                if (y in -50f..height + 50f) {
                    val alpha = if (charIdx == 0) 1.0f else (1.0f - (charIdx / 25f)).coerceIn(0.1f, 0.85f)
                    val colorInt = if (charIdx == 0) {
                        android.graphics.Color.WHITE
                    } else if (charIdx < 3) {
                        android.graphics.Color.rgb(180, 255, 200)
                    } else {
                        android.graphics.Color.argb((alpha * 255).toInt(), 0, 255, 102)
                    }
                    textPaint.color = colorInt
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(char, x, y, textPaint)
                    }
                }
            }
        }
    }
}

private data class MatrixColumnState(
    val speed: Float,
    val offset: Float,
    val chars: List<String>
)
