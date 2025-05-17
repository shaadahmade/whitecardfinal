package com.example.whitecard.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Define custom colors
val SuccessGreen = Color(0xFF00C853) // Keep green for ticks
val BlueAccent = Color(0xFF1A56E8) // New blue accent color for buttons and icons
val DarkBackground = Color(0xFF000000) // Dark background base
val DarkBackgroundLight = Color(0xFF464646) // Dark background secondary

// Background gradient
val backgroundGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF000000),
        Color(0xFF464646)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailsScreen(navController: NavController, userId: String) {
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    // Add state for secure animation flow
    var showSecureDisplay by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()

    // Load user data immediately
    LaunchedEffect(key1 = true) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                isLoading = false
                if (document != null && document.exists()) {
                    userData = document.data
                    // Trigger secure display once data is loaded
                    showSecureDisplay = true
                } else {
                    errorMessage = "User not found"
                }
            }
            .addOnFailureListener { e ->
                isLoading = false
                errorMessage = e.message ?: "Error loading user data"
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Secure Profile",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = BlueAccent
                        )
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = BlueAccent,
                        modifier = Modifier.size(60.dp)
                    )
                    Text(
                        "Loading secure data...",
                        modifier = Modifier.padding(top = 16.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.Center)
                )
            } else if (userData != null && showSecureDisplay) {
                // Convert userData to a Map<String, String> for the SecureDisplayScreen
                val displayData = mapOf(
                    "Full Name" to (userData?.get("name") as? String ?: ""),
                    "Aadhaar Number" to (userData?.get("aadhaarNumber") as? String ?: ""),
                    "PAN Number" to (userData?.get("panNumber") as? String ?: ""),
                    "License Number" to (userData?.get("licenseNumber") as? String ?: ""),
                    "License Valid Till" to (userData?.get("expiryDate") as? String ?: "")
                )

                // Use the updated dark display with accent colors
                DarkThemeSecureDisplay(userData = displayData)
            }
        }
    }
}

@Composable
fun DarkThemeSecureDisplay(userData: Map<String, String>) {
    // State to control animation flow
    var showSuccessAnimation by remember { mutableStateOf(true) }
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        delay(2500) // Show success animation for 2.5 seconds
        showSuccessAnimation = false
        delay(300) // Brief delay for animation transition
        showContent = true
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(backgroundGradient)
    ) {
        // Success Animation
        AnimatedVisibility(
            visible = showSuccessAnimation,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            BlueSuccessAnimation()
        }

        // Main Content
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(animationSpec = tween(800)) +
                    scaleIn(
                        initialScale = 0.9f,
                        animationSpec = tween(800, easing = FastOutSlowInEasing)
                    ),
            modifier = Modifier.fillMaxSize()
        ) {
            DarkThemeContent(userData)
        }
    }
}

@Composable
fun BlueSuccessAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "success_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale_pulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glow_pulse"
    )

    val checkmarkScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ), label = "checkmark_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Outer glow effect
        Box(contentAlignment = Alignment.Center) {
            // Glow
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale * 1.2f)
                    .alpha(glowAlpha)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                BlueAccent.copy(alpha = 0.7f),
                                BlueAccent.copy(alpha = 0f)
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Circle background
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                BlueAccent,
                                BlueAccent.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.8f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Success",
                    tint = SuccessGreen, // Keep checkmark green as requested
                    modifier = Modifier
                        .size(90.dp)
                        .scale(checkmarkScale)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Authentication Successful",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Securing your data...",
            style = MaterialTheme.typography.bodyLarge,
            color = BlueAccent,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DarkThemeContent(userData: Map<String, String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Security header
        DarkSecurityHeader()

        // User data fields - no scrolling, compactly arranged
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            userData.forEach { (label, value) ->
                if (value.isNotEmpty()) {
                    DarkDisplayField(label = label, value = value)
                }
            }
        }

        // Security features indication
        DarkSecurityFeatures()
    }
}

@Composable
fun DarkSecurityHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            BlueAccent,
                            BlueAccent.copy(alpha = 0.7f)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    color = Color.White,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = "Secure",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "SECURE VERIFICATION",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )

        Text(
            text = "End-to-end encrypted • TLS 1.3",
            style = MaterialTheme.typography.bodyMedium,
            color = BlueAccent,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp)
        )

        Divider(
            modifier = Modifier
                .padding(top = 12.dp)
                .alpha(0.6f),
            color = Color.Gray
        )
    }
}

@Composable
fun DarkDisplayField(label: String, value: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkBackgroundLight
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Green checkmark icon as requested
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen, // Keep checkmarks green
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun DarkSecurityFeatures() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkBackgroundLight
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SECURITY VERIFICATION",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SecurityBadge(
                    icon = Icons.Filled.Lock,
                    title = "Encrypted"
                )

                SecurityBadge(
                    icon = Icons.Filled.Security,
                    title = "Verified"
                )

                SecurityBadge(
                    icon = Icons.Filled.Shield,
                    title = "Protected"
                )
            }
        }
    }
}

@Composable
fun SecurityBadge(icon: ImageVector, title: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BlueAccent) // Use blue for badge backgrounds
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Color.LightGray
        )
    }
}

@Preview
@Composable
fun DarkThemeSecureDisplayPreview() {
    val sampleData = mapOf(
        "Full Name" to "John Doe",
        "Account Number" to "•••• •••• •••• 4285",
        "Balance" to "$24,302.75",
        "Last Login" to "Today, 2:15 PM",
        "Security Status" to "Strong"
    )

    MaterialTheme {
        Surface {
            DarkThemeSecureDisplay(userData = sampleData)
        }
    }
}