package com.example.whitecard.screens



import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.createBitmap
import androidx.navigation.NavController
import com.example.whitecard.viewmodels.mainscreenviewmlodel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*



import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHost
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.whitecard.elements.BottomNavigation
import com.example.whitecard.elements.WhiteCardTopBar
import kotlinx.coroutines.delay


import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, viewModel: mainscreenviewmlodel) {
    var userData by remember { mutableStateOf(mapOf<String, String>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var showQRCode by remember { mutableStateOf(false) }

    // Fetch user data from Firestore
    LaunchedEffect(key1 = true) {
        try {
            val userId = viewModel.auth.currentUser?.uid
            if (userId != null) {
                val document = viewModel.db.collection("users").document(userId).get().await()
                if (document.exists()) {
                    val data = document.data
                    if (data != null) {
                        userData = mapOf(
                            "name" to (data["name"] as? String ?: ""),
                            "aadhaarNumber" to (data["aadhaarNumber"] as? String ?: ""),
                            "panNumber" to (data["panNumber"] as? String ?: ""),
                            "licenseNumber" to (data["licenseNumber"] as? String ?: ""),
                            "expiryDate" to (data["expiryDate"] as? String ?: "")
                        )
                    }
                }
            }
            // Add a small delay to ensure shimmer is visible even on fast connections
            delay(200)
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load data"
            isLoading = false
        }
    }

    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF000000),
            Color(0xFF464646)
        )
    )

    // Use shimmer loading or actual content based on loading state
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            ShimmerLoadingScreen()
        } else {
            Scaffold(
                topBar = {
                    WhiteCardTopBar(navController = navController, viewModel = viewModel)

                },
                bottomBar = {
                    BottomNavigation(navController = navController)
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = backgroundGradient)
                        .padding(paddingValues)
                ) {
                    if (errorMessage.isNotEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Welcome, ${userData["name"]?.split(" ")?.firstOrNull() ?: "User"}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp,bottom = 12.dp),
                                textAlign = TextAlign.Center
                            )



                            InteractiveVirtualIdCard(
                                userData = userData,
                                showQR = showQRCode,
                                onShowQRChange = { showQRCode = it },
                                navController = navController,
                                mainscreenviewmlodel = viewModel
                            )

                            Spacer(modifier = Modifier.height(24.dp))
                            AppInfoArticlesList(navController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveVirtualIdCard(
    userData: Map<String, String>,
    showQR: Boolean = false,
    onShowQRChange: (Boolean) -> Unit,
    navController: NavController,
    mainscreenviewmlodel: mainscreenviewmlodel
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Get the selected template ID from DataStore
    val selectedTemplateId = remember { DataStoreManager.getSelectedTemplateId() }

    // Get the template based on selected ID
    val cardTemplate = remember(selectedTemplateId) {
        when (selectedTemplateId) {
            "blue_gradient" -> CardTemplate(
                id = "blue_gradient",
                name = "Blue Gradient",
                primaryBackgroundColor = Color(0xFF1A56E8),
                backgroundGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A56E8),
                        Color(0xFF0E2F7C)

                    )
                ),
                textColor = Color.White,
                accentColor = Color(0xFFFFD700)
            )
            "purple_dark" -> CardTemplate(
                id = "purple_dark",
                name = "Purple Dark",
                primaryBackgroundColor = Color(0xFF4B0082),
                backgroundGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF4B0082),

                        Color(0xFF8A2BE2)
                    )
                ),
                textColor = Color.White,
                accentColor = Color(0xFFE6E6FA)
            )
            "carbon_fiber" -> CardTemplate(
                id = "carbon_fiber",
                name = "Carbon Fiber",
                primaryBackgroundColor = Color(0xFF2C2C2C),
                backgroundGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2C2C2C),
                        Color(0xFF1A1A1A)
                    )
                ),
                textColor = Color.White,
                accentColor = Color(0xFFFF4500)
            )
            "rose_gold" -> CardTemplate(
                id = "rose_gold",
                name = "Rose Gold",
                primaryBackgroundColor = Color(0xFFB76E79),
                backgroundGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFB76E79),
                        Color(0xFFE8C5B6)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                textColor = Color(0xFF4A2932),
                accentColor = Color.White
            )
            "emerald" -> CardTemplate(
                id = "emerald",
                name = "Emerald",
                primaryBackgroundColor = Color(0xFF046307),
                backgroundGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF046307),
                        Color(0xFF2E8B57)
                    )
                ),

                textColor = Color.White,
                accentColor = Color(0xFFFFD700)
            )
            "ocean" -> CardTemplate(
                id = "ocean",
                name = "Ocean",
                primaryBackgroundColor = Color(0xFF835959),
                backgroundGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0CE8BB),
                        Color(0xFF174617)
                    )
                )
                ,
                textColor = Color.White,
                accentColor = Color(0xFFEAE8DD)
            )
            else -> CardTemplate(
                id = "default",
                name = "Default Black",
                backgroundGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF000000),
                        Color(0xFF464646)
                    )
                ),
                textColor = Color.White,
                accentColor = Color(0xFF1A56E8),
                primaryBackgroundColor = Color(0xFF000000)
            )
        }
    }

    // These values will control the 3D rotation effect
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val rotationLimitDegrees = 15f

    // Animation for card auto-rotation
    val infiniteTransition = rememberInfiniteTransition()
    val autoRotationX = infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        )
    )

    val autoRotationY = infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Combine manual and auto rotation
    val finalRotationX = if (offsetX != 0f || offsetY != 0f) {
        offsetY / 10f
    } else {
        autoRotationX.value
    }

    val finalRotationY = if (offsetX != 0f || offsetY != 0f) {
        -offsetX / 10f
    } else {
        autoRotationY.value
    }

    // Card shine effect
    val maxShine = 0.4f
    val angleRad = atan2(finalRotationY, finalRotationX)
    val shinePositionX = remember(finalRotationX, finalRotationY) {
        (cos(angleRad) * 0.5f + 0.5f).coerceIn(0f, 1f)
    }
    val shinePositionY = remember(finalRotationX, finalRotationY) {
        (sin(angleRad) * 0.5f + 0.5f).coerceIn(0f, 1f)
    }

    val shineAlpha = remember(finalRotationX, finalRotationY) {
        val rotationMagnitude = (abs(finalRotationX) + abs(finalRotationY)) / (rotationLimitDegrees * 2)
        (rotationMagnitude * maxShine).coerceIn(0f, maxShine)
    }

    val shineGradient = remember(shinePositionX, shinePositionY, shineAlpha) {
        Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = shineAlpha),
                Color.Transparent
            ),
            center = Offset(shinePositionX, shinePositionY),
            radius = 1000f
        )
    }

    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(240.dp)
            .graphicsLayer {
                rotationX = finalRotationX
                rotationY = finalRotationY
                cameraDistance = 12f * density
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        // Reset to center position with animation
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onDragCancel = {
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(-300f, 300f)
                        offsetY = (offsetY + dragAmount.y).coerceIn(-300f, 300f)
                    }
                )
            }
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(cardTemplate.backgroundGradient)
    ) {

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

        // Card shine overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(shineGradient)
        )

        // Content
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
                    Text(
                        text = "Cardoo ID",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = cardTemplate.textColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = userData["name"] ?: "Name Not Available",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = cardTemplate.textColor
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(cardTemplate.accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ID",
                        fontWeight = FontWeight.Bold,
                        color = if (cardTemplate.accentColor == Color.White) cardTemplate.primaryBackgroundColor

                        else
                            Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ID Details
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    IdDetailField(
                        label = "AADHAAR",
                        value = userData["aadhaarNumber"]?.takeIf { it != "N/A" } ?: "Not Verified",
                        textColor = cardTemplate.textColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    IdDetailField(
                        label = "PAN",
                        value = userData["panNumber"]?.takeIf { it != "N/A" } ?: "Not Verified",
                        textColor = cardTemplate.textColor
                    )
                }

                Column(modifier = Modifier.weight(1.1f)) {
                    IdDetailField(
                        label = "DL NUMBER",
                        value = userData["licenseNumber"]?.takeIf { it != "N/A" } ?: "Not Verified",
                        textColor = cardTemplate.textColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    IdDetailField(
                        label = "VALID TILL",
                        value = userData["expiryDate"]?.takeIf { it != "N/A" } ?: "Not Verified",
                        textColor = cardTemplate.textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // QR Button
            Button(
                onClick = { onShowQRChange(true) },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(40.dp)
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cardTemplate.accentColor
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    "",
                    color = if (cardTemplate.accentColor == Color.White) cardTemplate.primaryBackgroundColor



                    else
                        Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }

    if (showQR) {
        Dialog(onDismissRequest = { onShowQRChange(false) }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Create QR code with user ID
                    val qrCodeBitmap = remember {
                        mainscreenviewmlodel.generateQRCode (currentUserId, 300, 300)
                    }

                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrCodeBitmap?.asImageBitmap() ?: createBitmap(1, 1).asImageBitmap(),
                            contentDescription = "",
                            modifier = Modifier.size(220.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Scan to verify identity",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { onShowQRChange(false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A56E8)
                        ),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(
                            "CLOSE",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IdDetailField(label: String, value: String, textColor: Color) {
    Column {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = textColor.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}




@Composable
fun QuickActionsGrid(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionItem(
                icon = Icons.Default.Share,
                label = "Share ID",
                onClick = { /* Handle share action */ }
            )

            QuickActionItem(
                icon = Icons.Default.History,
                label = "History",
                onClick = { navController.navigate("scan_history") }
            )

            QuickActionItem(
                icon = Icons.Default.Lock,
                label = "Lock ID",
                onClick = { /* Handle ID locking */ }
            )

            QuickActionItem(
                icon = Icons.Default.Help,
                label = "Help",
                onClick = { navController.navigate("help_screen") }
            )
        }

        // Optional: Add a second row if you have more actions
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                onClick = { navController.navigate("settings") }
            )

            QuickActionItem(
                icon = Icons.Default.Notifications,
                label = "Alerts",
                onClick = { navController.navigate("notifications") }
            )

            QuickActionItem(
                icon = Icons.Default.Star,
                label = "Premium",
                onClick = { navController.navigate("premium_features") }
            )

            QuickActionItem(
                icon = Icons.Default.Person,
                label = "Profile",
                onClick = { navController.navigate("profile") }
            )
        }
    }
}

@Composable
fun QuickActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    // Animation for button press effect
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1A56E8),
                            Color(0xFF0E2F7C)
                        )
                    )
                )
                .clickable {
                    isPressed = true
                    onClick()
                    // Reset after animation
                    kotlinx.coroutines.GlobalScope.launch {
                        kotlinx.coroutines.delay(100)
                        isPressed = false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}


@Composable
fun AppInfoArticlesList(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Featured Articles",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // List of articles
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            // Sample article data - in production you would fetch this from a repository
            val articles = listOf(
                ArticleData(
                    id = "security_features",
                    title = "Security Features",
                    summary = "How your digital ID keeps your information secure",
                    icon = Icons.Default.Security,
                    color = Color(0xFF1A56E8)
                ),
                ArticleData(
                    id = "id_verification",
                    title = "ID Verification",
                    summary = "Learn how businesses verify your Cardoo ID",
                    icon = Icons.Default.Verified,
                    color = Color(0xFF4B0082)
                ),
                ArticleData(
                    id = "physical_cards",
                    title = "Physical Cards",
                    summary = "Benefits of ordering a physical backup card",
                    icon = Icons.Default.CreditCard,
                    color = Color(0xFF046307)
                ),
                ArticleData(
                    id = "app_updates",
                    title = "What's New",
                    summary = "Latest features and improvements in Cardoo",
                    icon = Icons.Default.NewReleases,
                    color = Color(0xFFB76E79)
                ),
                ArticleData(
                    id = "privacy_policy",
                    title = "Privacy Policy",
                    summary = "How we protect your personal information",
                    icon = Icons.Default.PrivacyTip,
                    color = Color(0xFF0CE8BB)
                )
            )

            items(articles) { article ->
                ArticleCard(article) {
                    // Navigate to article detail screen with article ID
                    navController.navigate("article_detail/${article.id}")
                }
            }
        }




            }
        }



@Composable
fun ArticleCard(article: ArticleData, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222))
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Subtle gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                article.color.copy(alpha = 0.4f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Icon with colored background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(article.color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = article.icon,
                        contentDescription = article.title,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Article title
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Article summary
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                // "Read more" text
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Read more",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = article.color
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Read more",
                        tint = article.color,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(start = 4.dp)
                    )
                }
            }
        }
    }
}



// Data classes to hold article and activity information
data class ArticleData(
    val id: String,
    val title: String,
    val summary: String,
    val icon: ImageVector,
    val color: Color
)

data class ActivityData(
    val title: String,
    val location: String,
    val timestamp: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)


@Composable
private fun LazyListScope.securityFeaturesContent() {
    item {
        ArticleSection(
            title = "Encryption",
            content = "All data is encrypted using industry-standard AES-256 encryption both in transit and at rest."
        )
    }

    item {
        ArticleSection(
            title = "Biometric Authentication",
            content = "Your digital ID can only be accessed after verifying your identity through facial recognition or fingerprint authentication."
        )
    }

    item {
        ArticleSection(
            title = "Secure Element Storage",
            content = "Sensitive information is stored in your device's secure enclave, isolated from the main operating system."
        )
    }

    item {
        ArticleSection(
            title = "Remote Wipe",
            content = "If your device is lost or stolen, you can remotely deactivate your digital ID and wipe any sensitive data."
        )
    }

    item {
        ArticleSection(
            title = "Fraud Detection",
            content = "Our AI-powered systems continuously monitor for suspicious activity and alert you to potential security threats."
        )
    }
}

@Composable
private fun LazyListScope.idVerificationContent() {
    item {
        ArticleSection(
            title = "QR Code Scanning",
            content = "The most common verification method is scanning your ID's QR code, which contains a cryptographically signed attestation of your identity."
        )
    }

    item {
        ArticleSection(
            title = "NFC Tap",
            content = "Compatible terminals can read your digital ID via NFC for contactless verification."
        )
    }

    item {
        ArticleSection(
            title = "Verification API",
            content = "Business partners can integrate with our API for seamless digital verification during online transactions."
        )
    }

    item {
        ArticleSection(
            title = "Identity Attestation",
            content = "Cardoo only shares the minimum information required for each verification, protecting your privacy while still confirming your identity."
        )
    }
}

@Composable
private fun LazyListScope.physicalCardsContent() {
    item {
        ArticleSection(
            title = "Backup Access",
            content = "Physical cards provide a reliable backup when your phone battery dies or in areas with poor connectivity."
        )
    }

    item {
        ArticleSection(
            title = "Universal Acceptance",
            content = "Our physical cards are accepted anywhere traditional ID cards are used, ensuring you're never without identification."
        )
    }

    item {
        ArticleSection(
            title = "Enhanced Security",
            content = "Physical cards include tamper-evident features, holograms, and a chip containing encrypted credentials."
        )
    }

    item {
        ArticleSection(
            title = "Emergency Access",
            content = "Physical cards can be set up with emergency contact information and medical data even when your phone is inaccessible."
        )
    }

    item {
        ArticleSection(
            title = "How to Order",
            content = "You can order a physical Cardoo card directly through the app. Standard shipping is free, and express options are available."
        )
    }
}

@Composable
private fun LazyListScope.appUpdatesContent() {
    item {
        ArticleSection(
            title = "Recent Updates",
            content = "• Enhanced biometric security\n• Faster ID verification process\n• Dark mode support\n• Improved accessibility features\n• Multi-device synchronization"
        )
    }

    item {
        ArticleSection(
            title = "Coming Soon",
            content = "• International ID support\n• Travel document integration\n• Digital signature capabilities\n• Organization ID management\n• Age verification without revealing birth date"
        )
    }

    item {
        ArticleSection(
            title = "Feature Requests",
            content = "We're constantly working to improve Cardoo. Submit your feature requests through the app's feedback option."
        )
    }
}

@Composable
private fun LazyListScope.privacyPolicyContent() {
    item {
        ArticleSection(
            title = "Data Collection",
            content = "We collect only the information necessary to provide our identification services. This includes your name, photo, date of birth, and other ID-related information."
        )
    }

    item {
        ArticleSection(
            title = "Data Usage",
            content = "Your information is used solely for identity verification purposes and is never sold to third parties."
        )
    }

    item {
        ArticleSection(
            title = "Data Control",
            content = "You maintain full control of your data and can revoke access permissions at any time."
        )
    }

    item {
        ArticleSection(
            title = "Data Sharing",
            content = "Information is only shared with businesses during verification processes, and only with your explicit consent."
        )
    }

    item {
        ArticleSection(
            title = "Data Retention",
            content = "You can delete your account at any time, which will permanently remove all your personal information from our systems."
        )
    }
}



// Helper function to get article by ID
fun getArticleById(articleId: String): ArticleData? {
    val articles = listOf(
        ArticleData(
            id = "security_features",
            title = "Security Features",
            summary = "How your digital ID keeps your information secure",
            icon = Icons.Default.Security,
            color = Color(0xFF1A56E8)
        ),
        ArticleData(
            id = "id_verification",
            title = "ID Verification",
            summary = "Learn how businesses verify your Cardoo ID",
            icon = Icons.Default.Verified,
            color = Color(0xFF4B0082)
        ),
        ArticleData(
            id = "physical_cards",
            title = "Physical Cards",
            summary = "Benefits of ordering a physical backup card",
            icon = Icons.Default.CreditCard,
            color = Color(0xFF046307)
        ),
        ArticleData(
            id = "app_updates",
            title = "What's New",
            summary = "Latest features and improvements in Cardoo",
            icon = Icons.Default.NewReleases,
            color = Color(0xFFB76E79)
        ),
        ArticleData(
            id = "privacy_policy",
            title = "Privacy Policy",
            summary = "How we protect your personal information",
            icon = Icons.Default.PrivacyTip,
            color = Color(0xFF0CE8BB)
        )
    )

    return articles.find { it.id == articleId }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    articleId: String,
    navController: NavController
) {
    // Find the article by ID
    val article = getArticleById(articleId)

    if (article != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // TopBar with back navigation
                TopAppBar(
                    title = { Text(text = article.title, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = article.color
                    )
                )

                // Article content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header section with icon
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            // Icon with colored background
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(article.color),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = article.icon,
                                    contentDescription = article.title,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = article.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Text(
                                    text = article.summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Divider
                    item {
                        Divider(
                            color = Color.White.copy(alpha = 0.1f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }

                    // Content sections - dynamically generated based on article content
                    when (article.id) {
                        "security_features" -> {
                            item {
                                ArticleSection(
                                    title = "Encryption",
                                    content = "All data is encrypted using industry-standard AES-256 encryption both in transit and at rest."
                                )
                            }

                            item {
                                ArticleSection(
                                    title = "Biometric Authentication",
                                    content = "Your digital ID can only be accessed after verifying your identity through facial recognition or fingerprint authentication."
                                )
                            }

                            item {
                                ArticleSection(
                                    title = "Secure Element Storage",
                                    content = "Sensitive information is stored in your device's secure enclave, isolated from the main operating system."
                                )
                            }

                            item {
                                ArticleSection(
                                    title = "Remote Wipe",
                                    content = "If your device is lost or stolen, you can remotely deactivate your digital ID and wipe any sensitive data."
                                )
                            }

                            item {
                                ArticleSection(
                                    title = "Fraud Detection",
                                    content = "Our AI-powered systems continuously monitor for suspicious activity and alert you to potential security threats."
                                )
                            }
                        }
                        "id_verification" -> {
                            item {
                                ArticleSection(
                                    title = "QR Code Scanning",
                                    content = "The most common verification method is scanning your ID's QR code, which contains a cryptographically signed attestation of your identity."
                                )
                            }

                            item {
                                ArticleSection(
                                    title = "NFC Tap",
                                    content = "Compatible terminals can read your digital ID via NFC for contactless verification."
                                )
                            }

                            item {
                                ArticleSection(
                                    title = "Verification API",
                                    content = "Business partners can integrate with our API for seamless digital verification during online transactions."
                                )
                            }

                            item {
                                ArticleSection(
                                    title = "Identity Attestation",
                                    content = "Cardoo only shares the minimum information required for each verification, protecting your privacy while still confirming your identity."
                                )
                            }
                        }
                        "physical_cards" -> {
                            item {
                                ArticleSection(
                                    title = "Backup Access",
                                    content = "Physical cards provide a reliable backup when your phone battery dies or in areas with poor connectivity."
                                )
                            }

                            item {
                                ArticleSection(
                                    title = "Universal Acceptance",
                                    content = "Our physical cards are accepted anywhere traditional ID cards are used, ensuring you're never without identification."
                                )
                            }

                            item {
                                ArticleSection(
                                    title = "Enhanced Security",
                                    content = "Physical cards include tamper-evident features, holograms, and a chip containing encrypted credentials."
                                )
                            }

                            item {
                                ArticleSection(
                                    title = "Emergency Access",
                                    content = "Physical cards can be set up with emergency contact information and medical data even when your phone is inaccessible."
                                )
                            }

                            item {
                                ArticleSection(
                                    title = "How to Order",
                                    content = "You can order a physical Cardoo card directly through the app. Standard shipping is free, and express options are available."
                                )
                            }
                        }
                        "app_updates" -> {
                            item {
                                ArticleSection(
                                    title = "Recent Updates",
                                    content = "• Enhanced biometric security\n• Faster ID verification process\n• Dark mode support\n• Improved accessibility features\n• Multi-device synchronization"
                                )
                            }

                            item {
                                ArticleSection(
                                    title = "Coming Soon",
                                    content = "• International ID support\n• Travel document integration\n• Digital signature capabilities\n• Organization ID management\n• Age verification without revealing birth date"
                                )
                            }

                            item {
                                ArticleSection(
                                    title = "Feature Requests",
                                    content = "We're constantly working to improve Cardoo. Submit your feature requests through the app's feedback option."
                                )
                            }
                        }
                        "privacy_policy" -> {
                            item {
                                ArticleSection(
                                    title = "Data Collection",
                                    content = "We collect only the information necessary to provide our identification services. This includes your name, photo, date of birth, and other ID-related information."
                                )
                            }

                            item {
                                ArticleSection(
                                    title = "Data Usage",
                                    content = "Your information is used solely for identity verification purposes and is never sold to third parties."
                                )
                            }

                            item {
                                ArticleSection(
                                    title = "Data Control",
                                    content = "You maintain full control of your data and can revoke access permissions at any time."
                                )
                            }

                            item {
                                ArticleSection(
                                    title = "Data Sharing",
                                    content = "Information is only shared with businesses during verification processes, and only with your explicit consent."
                                )
                            }

                            item {
                                ArticleSection(
                                    title = "Data Retention",
                                    content = "You can delete your account at any time, which will permanently remove all your personal information from our systems."
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Article not found error state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Article not found",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

// Article Section Composable
@Composable
fun ArticleSection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            lineHeight = 24.sp
        )
    }
}

