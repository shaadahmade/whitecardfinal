package com.example.whitecard.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
@Preview()

@Composable
fun ShimmerLoadingScreen() {
    val backgroundGradient = Color(0x00FFFFFF)


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background( backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Empty top app bar space
            Spacer(modifier = Modifier.height(200.dp))

            // Welcome text shimmer


            Spacer(modifier = Modifier.height(16.dp))

            // Virtual ID Card shimmer - matches InteractiveVirtualIdCard exactly
            ShimmerVirtualIdCard()

            Spacer(modifier = Modifier.height(24.dp))

            // Featured Articles header
            ShimmerItem(
                modifier = Modifier
                    .width(160.dp)
                    .height(24.dp)
                    .align(Alignment.Start)
                    .padding(start = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal article cards - matches AppInfoArticlesList
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // First article card
                ShimmerArticleCard()

                // Second article card (partially visible)
                ShimmerItem(
                    modifier = Modifier
                        .width(80.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                )
            }

            // Spacer to push bottom nav to bottom
            Spacer(modifier = Modifier.weight(1f))

            // Bottom navigation placeholder

        }
    }
}

/**
 * Shimmer version of the virtual ID card that exactly matches your InteractiveVirtualIdCard
 */
@Composable
fun ShimmerVirtualIdCard() {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(240.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
    ) {
        // Base card shimmer
        ShimmerItem(
            modifier = Modifier.fillMaxSize()
        )

        // White overlay to match the card's shine effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.2f)
                        )
                    )
                )
        )

        // Card content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Top Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    // "Cardoo ID" text shimmer
                    ShimmerItem(
                        modifier = Modifier
                            .width(60.dp)
                            .height(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // User name shimmer
                    ShimmerItem(
                        modifier = Modifier
                            .width(150.dp)
                            .height(20.dp)
                    )
                }

                // ID Circle shimmer
                ShimmerItem(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ID Details
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    // AADHAAR
                    ShimmerItem(
                        modifier = Modifier
                            .width(70.dp)
                            .height(10.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    ShimmerItem(
                        modifier = Modifier
                            .width(120.dp)
                            .height(16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // PAN
                    ShimmerItem(
                        modifier = Modifier
                            .width(70.dp)
                            .height(10.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    ShimmerItem(
                        modifier = Modifier
                            .width(100.dp)
                            .height(16.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    // DL NUMBER
                    ShimmerItem(
                        modifier = Modifier
                            .width(70.dp)
                            .height(10.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    ShimmerItem(
                        modifier = Modifier
                            .width(120.dp)
                            .height(16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // VALID TILL
                    ShimmerItem(
                        modifier = Modifier
                            .width(70.dp)
                            .height(10.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    ShimmerItem(
                        modifier = Modifier
                            .width(90.dp)
                            .height(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // SHOW QR button shimmer
            ShimmerItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
        }
    }
}

/**
 * Shimmer version of your article cards
 */
@Composable
fun ShimmerArticleCard() {
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Base card shimmer
        ShimmerItem(
            modifier = Modifier.fillMaxSize()
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Icon box
            ShimmerItem(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Article title
            ShimmerItem(
                modifier = Modifier
                    .width(160.dp)
                    .height(18.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Article summary (2 lines)
            ShimmerItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            ShimmerItem(
                modifier = Modifier
                    .width(200.dp)
                    .height(14.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // "Read more" text
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerItem(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                )
            }
        }
    }
}

/**
 * Shimmer version of bottom navigation
 */
@Composable
fun ShimmerBottomNavigation() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        // Nav bar background
        ShimmerItem(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(70.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        )

        // Center scan button
        ShimmerItem(
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.TopCenter)
                .offset(y = 8.dp)
                .clip(CircleShape)
        )

        // Bottom nav items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // First two buttons
            ShimmerNavItem()
            ShimmerNavItem()

            // Space for center button
            Spacer(modifier = Modifier.width(64.dp))

            // Last two buttons
            ShimmerNavItem()
            ShimmerNavItem()
        }
    }
}

@Composable
fun ShimmerNavItem() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        ShimmerItem(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(4.dp))

        ShimmerItem(
            modifier = Modifier
                .width(40.dp)
                .height(10.dp)
        )
    }
}

/**
 * Base shimmer animation component
 */
@Composable
fun ShimmerItem(modifier: Modifier = Modifier) {
    val shimmerColors = listOf(
        Color.DarkGray.copy(alpha = 0.6f),
        Color.DarkGray.copy(alpha = 0.2f),
        Color.DarkGray.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(10f, 10f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .background(brush)
    )
}

/**
 * Use this function to integrate the shimmer loading into your MainScreen
 */
@Composable
fun MainScreenWithShimmer(
    isLoading: Boolean,
    content: @Composable () -> Unit
) {
    if (isLoading) {
        ShimmerLoadingScreen()
    } else {
        content()
    }
}