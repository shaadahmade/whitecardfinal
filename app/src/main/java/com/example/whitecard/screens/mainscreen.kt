package com.example.whitecard.screens



import android.graphics.Bitmap


import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.createBitmap
import androidx.navigation.NavController

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.tasks.await
import kotlin.math.*
import androidx.core.graphics.set


@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun MainScreen(navController: NavController) {
    var userData by remember { mutableStateOf(mapOf<String, String>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var showQRCode by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Fetch user data from Firestore
    LaunchedEffect(key1 = true) {
        try {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val document = db.collection("users").document(userId).get().await()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.inversePrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                actions = {
                    IconButton(onClick = {
                        auth.signOut()
                        navController.navigate("welcome") {
                            popUpTo("main") { inclusive = true }
                        }
                    }) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = backgroundGradient)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading your credentials...",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else if (errorMessage.isNotEmpty()) {
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
                    Text(
                        text = "Welcome, ${userData["name"]?.split(" ")?.firstOrNull() ?: "User"}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    InteractiveVirtualIdCard(
                        userData = userData,
                        showQR = showQRCode,
                        onShowQRChange = { showQRCode = it },
                        navController = navController
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Add the new circular buttons row
                    CircularButtonsRow(navController = navController)

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { navController.navigate("scan_qr") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(8.dp, RoundedCornerShape(28.dp)),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        )
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR Code",
                            tint = Color(0xFF1A56E8),
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Scan ID",
                            color = Color(0xFF1A56E8),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun generateQRCode(content: String, width: Int, height: Int): Bitmap? {
    try {
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height)
        val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap[x, y] =
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE




            }
        }

        return bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

@Composable
fun InteractiveVirtualIdCard(
    userData: Map<String, String>,
    showQR: Boolean = false,
    onShowQRChange: (Boolean) -> Unit,
    navController: NavController
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
                        value = userData["aadhaarNumber"] ?: "N/A",
                        textColor = cardTemplate.textColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    IdDetailField(
                        label = "PAN",
                        value = userData["panNumber"] ?: "N/A",
                        textColor = cardTemplate.textColor
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    IdDetailField(
                        label = "DL NUMBER",
                        value = userData["licenseNumber"] ?: "N/A",
                        textColor = cardTemplate.textColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    IdDetailField(
                        label = "VALID TILL",
                        value = userData["expiryDate"] ?: "N/A",
                        textColor = cardTemplate.textColor
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

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
                    "SHOW QR",
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
                        generateQRCode(currentUserId, 300, 300)
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
fun CircularButtonsRow(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Update Card Button
        CircularActionButton(
            icon = Icons.Default.Edit,
            label = "Update Card",
            onClick = { navController.navigate("update_card") }
        )

        // Change Template Button
        CircularActionButton(
            icon = Icons.Default.Style,
            label = "Templates",
            onClick = { navController.navigate("card_templates") }
        )

        // Order Card Button
        CircularActionButton(
            icon = Icons.Default.AddCard,
            label = "Order Card",
            onClick = { navController.navigate("order_card") }
        )

        // Order Physical Card Button
        CircularActionButton(
            icon = Icons.Default.CreditCard,
            label = "Physical Card",
            onClick = { navController.navigate("physical_card") }
        )
    }
}

@Composable
fun CircularActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .shadow(4.dp, CircleShape),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF1A56E8),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(64.dp)
        )
    }
}