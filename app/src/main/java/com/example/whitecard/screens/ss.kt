package com.example.whitecard.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalTextApi::class)
@Composable
fun CardooSplashScreen(onLoadingComplete: () -> Unit) {
    // Define refined color scheme
    val primaryBlue = Color(0xFF2196F3)
    val secondaryBlue = Color(0xFF42A5F5)
    val accentBlue = Color(0xFF64B5F6)
    val highlightBlue = Color(0xFF90CAF9)
    val cardWhite = Color.White
    val cardGold = Color(0xFFFFFFFF)

    // Refined darker gradient background
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF121212),  // Darker black for more depth
            Color(0xFF1E3A5F)   // Dark blue tint for elegance
        )
    )

    // Text gradient
    val textGradient = Brush.linearGradient(
        colors = listOf(
            cardWhite,
            cardGold
        )
    )

    // Animation states with refined timing
    var isLogoVisible by remember { mutableStateOf(false) }
    var isRingVisible by remember { mutableStateOf(false) }
    var isTextVisible by remember { mutableStateOf(false) }
    var isSubtitleVisible by remember { mutableStateOf(false) }
    var startLoading by remember { mutableStateOf(false) }

    // More sophisticated animation sequence
    LaunchedEffect(key1 = true) {
        isLogoVisible = true
        delay(500)
        isRingVisible = true
        delay(600)
        isTextVisible = true
        delay(400)
        isSubtitleVisible = true
        delay(500)
        startLoading = true
        delay(2500)
        onLoadingComplete()
    }

    // Refined logo animations
    val logoScale = animateFloatAsState(
        targetValue = if (isLogoVisible) 1f else 0.1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    // Subtle floating animation
    val breathingScale = animateFloatAsState(
        targetValue = if (isRingVisible) 1f else 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    // Card floating animation
    val cardFloat = animateFloatAsState(
        targetValue = if (isRingVisible) 3f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cardFloat"
    )

    // Ring animation
    val ringProgress = animateFloatAsState(
        targetValue = if (isRingVisible) 1f else 0f,
        animationSpec = tween(1500, easing = EaseOutQuint),
        label = "ringProgress"
    )

    // Card rotation animation
    val cardRotation = animateFloatAsState(
        targetValue = if (isLogoVisible) 0f else -30f,
        animationSpec = tween(800, easing = EaseOutBack),
        label = "cardRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        // Subtle glow effects in background
        CardGlowEffects()

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo with animated ring
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(logoScale.value * breathingScale.value)
                    .offset(y = cardFloat.value.dp),
                contentAlignment = Alignment.Center
            ) {
                // Animated rings
                CardRings(ringProgress.value, primaryBlue, cardGold)

                // Card-themed logo
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    cardWhite,
                                    Color(0xFFF5F5F5)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Card chip design element
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .align(Alignment.TopCenter)
                            .size(width = 40.dp, height = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        cardGold,
                                        cardGold.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )

                    // Stacked card effect (card in the back)
                    Box(
                        modifier = Modifier
                            .offset(x = (-4).dp, y = 4.dp)
                            .size(90.dp)
                            .rotate(cardRotation.value - 8f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        secondaryBlue.copy(alpha = 0.8f),
                                        primaryBlue
                                    )
                                )
                            )
                    )

                    // Front card with shine effect
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .rotate(cardRotation.value)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        primaryBlue,
                                        accentBlue
                                    )
                                )
                            )
                    ) {
                        // Card shine effect
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.3f)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 80.dp,
                                        topEnd = 8.dp,
                                        bottomStart = 8.dp,
                                        bottomEnd = 80.dp
                                    )
                                )
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.8f),
                                            Color.White.copy(alpha = 0f)
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                    )
                                )
                        )

                        // Card 'C' branding mark
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD700).copy(alpha = 0.9f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .align(Alignment.Center)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.9f))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Refined app name with gradient text
            AnimatedVisibility(
                visible = isTextVisible,
                enter = fadeIn(animationSpec = tween(700)) +
                        scaleIn(initialScale = 0.8f, animationSpec = tween(900, easing = EaseOutQuint))
            ) {
                Text(
                    text = "CARDOO",
                    style = TextStyle(
                        brush = textGradient,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 3.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Refined subtitle
            AnimatedVisibility(
                visible = isSubtitleVisible,
                enter = fadeIn(animationSpec = tween(500)) +
                        expandVertically(animationSpec = tween(700, easing = EaseOutQuint))
            ) {
                Text(
                    text = "Your ID in your pocket.",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Refined loading indicator
        if (startLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp)
            ) {
                RefinedLoadingIndicator(primaryBlue, cardGold)
            }
        }

        // Version text
        Text(
            text = "v1.0",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

@Composable
fun CardRings(progress: Float, primaryColor: Color, accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "ringsTransition")

    // Rotating animation for rings
    val rotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    Canvas(modifier = Modifier.size(180.dp)) {
        // Outer ring with shimmering effect
        drawCircle(
            brush = Brush.sweepGradient(
                0f to primaryColor.copy(alpha = 0.7f),
                0.3f to accentColor.copy(alpha = 0.5f),
                0.6f to primaryColor.copy(alpha = 0.7f),
                1f to accentColor.copy(alpha = 0.5f)
            ),
            radius = size.minDimension / 2 * progress,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        // Middle ring with rotation effect
        val centerX = size.width / 2
        val centerY = size.height / 2
        val rotationRad = rotation.value * (Math.PI / 180f).toFloat()
        val offsetX = cos(rotationRad) * 3f
        val offsetY = sin(rotationRad) * 3f

        drawCircle(
            brush = Brush.sweepGradient(
                0f to accentColor.copy(alpha = 0.6f),
                0.5f to primaryColor.copy(alpha = 0.8f),
                1f to accentColor.copy(alpha = 0.6f)
            ),
            radius = size.minDimension / 2.3f * progress,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
            center = Offset(centerX + offsetX, centerY + offsetY)
        )

        // Inner dotted ring to represent card security elements
        val dotCount = 36
        val radius = size.minDimension / 2.8f * progress
        val dotSize = 2.dp.toPx()

        for (i in 0 until dotCount) {
            val angle = (i * (360f / dotCount) + rotation.value / 2) * (Math.PI / 180f).toFloat()
            val x = centerX + cos(angle) * radius
            val y = centerY + sin(angle) * radius

            drawCircle(
                color = if (i % 3 == 0) accentColor else primaryColor.copy(alpha = 0.7f),
                radius = dotSize / 2,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun RefinedLoadingIndicator(primaryColor: Color, accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingTransition")

    // Loading bar progress animation
    val progress = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loadingProgress"
    )

    // Create gradient for loading bar
    val loadingGradient = Brush.horizontalGradient(
        colors = listOf(
            primaryColor.copy(alpha = 0.6f),
            accentColor,
            primaryColor.copy(alpha = 0.6f)
        )
    )

    Box(
        modifier = Modifier
            .width(120.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(1.5.dp))
            .background(Color.White.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .clip(RoundedCornerShape(1.5.dp))
                .background(loadingGradient)
        )
    }
}

@Composable
fun CardGlowEffects() {
    // Create subtle credit card-like glow effects in the background
    repeat(4) { index ->
        val infiniteTransition = rememberInfiniteTransition(label = "glowTransition$index")

        // Randomize positions and sizes
        val xOffset = remember { (-250..250).random().toFloat() }
        val yOffset = remember { (-350..350).random().toFloat() }
        val width = remember { (180..240).random() }
        val height = remember { (width * 0.63).toInt() } // Credit card proportions

        // Animate opacity for subtle pulsing
        val opacity = infiniteTransition.animateFloat(
            initialValue = 0.05f,
            targetValue = 0.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (4000..7000).random(),
                    easing = EaseInOutQuad
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowOpacity$index"
        )

        // Card rotation
        val rotation = infiniteTransition.animateFloat(
           initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(10000, easing = EaseInOutQuad),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowRotation$index"
        )

        Box(
            modifier = Modifier
                .offset(x = xOffset.dp, y = yOffset.dp)
                .size(width = width.dp, height = height.dp)
                .rotate(rotation.value)
                .alpha(opacity.value)
                .blur(radius = 40.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF2196F3).copy(alpha = 0.5f),
                            Color(0xFF00F7FF).copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
        )
    }
}