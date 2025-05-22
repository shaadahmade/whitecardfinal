package com.example.whitecard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DriveEta
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.whitecard.firebase.FirestoreManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateCardScreen(navController: NavController) {
    var aadhaarNumber by remember { mutableStateOf("") }
    var panNumber by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    // Fetch current user data to prefill fields
    LaunchedEffect(key1 = true) {
        isLoading = true
        FirestoreManager.getUserData { userData ->
            userData?.let {
                aadhaarNumber = (it["aadhaarNumber"] as? String) ?: ""
                panNumber = (it["panNumber"] as? String) ?: ""
                licenseNumber = (it["licenseNumber"] as? String) ?: ""
                expiryDate = (it["expiryDate"] as? String) ?: ""
            }
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
                        "Your Documents",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A56E8),
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = Color.White
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
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Text(
                        text = "Your Identity Documents",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Update or verify your documents",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    // Status messages
                    if (errorMessage.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color.Red
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = errorMessage,
                                    color = Color.Red
                                )
                            }
                        }
                    }

                    if (successMessage.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE8F5E9)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color.Green
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = successMessage,
                                    color = Color.Green
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Document cards
                    DocumentCard(
                        title = "Aadhaar Card",
                        icon = Icons.Outlined.Fingerprint,
                        documentNumber = aadhaarNumber,
                        isVerified = aadhaarNumber.isNotEmpty(),
                        onVerifyClick = { navController.navigate("aadhaar_verification") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    DocumentCard(
                        title = "PAN Card",
                        icon = Icons.Outlined.Badge,
                        documentNumber = panNumber,
                        isVerified = panNumber.isNotEmpty(),
                        onVerifyClick = { navController.navigate("pan_verification") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    DocumentCard(
                        title = "Driving License",
                        icon = Icons.Outlined.DriveEta,
                        documentNumber = licenseNumber,
                        expiryDate = expiryDate,
                        isVerified = licenseNumber.isNotEmpty(),
                        onVerifyClick = { navController.navigate("license_verification") }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Notes
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Why verify your documents?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Verified documents help us ensure your identity and provide a secure experience. Your data is encrypted and protected.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentCard(
    title: String,
    icon: ImageVector,
    documentNumber: String,
    expiryDate: String = "",
    isVerified: Boolean,
    onVerifyClick: () -> Unit
) {
    val cardColor = if (isVerified) Color(0xFF1A56E8).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
    val borderColor = if (isVerified) Color(0xFF1A56E8) else Color.White.copy(alpha = 0.3f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,



        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isVerified) Color(0xFF1A56E8) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                if (isVerified) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Verified",
                        tint = Color(0xFF1A56E8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (documentNumber.isNotEmpty()) {
                Text(
                    text = "Document Number",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDocumentNumber(documentNumber, title),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                if (expiryDate.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Expiry Date",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = expiryDate,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Text(
                    text = "Not verified yet",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onVerifyClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isVerified) Color(0xFF1A56E8) else Color.White
                )
            ) {
                Text(
                    text = if (isVerified) "Update" else "Verify Now",
                    color = if (isVerified) Color.White else Color(0xFF1A56E8),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Helper function to format document numbers with masking
fun formatDocumentNumber(number: String, documentType: String): String {
    return when {
        documentType.contains("Aadhaar", ignoreCase = true) && number.length == 12 -> {
            "XXXX XXXX ${number.takeLast(4)}"
        }
        documentType.contains("PAN", ignoreCase = true) && number.length == 10 -> {
            "${number.take(5)}XXXXX"
        }
        documentType.contains("License", ignoreCase = true) -> {
            if (number.length > 6) {
                "${number.take(4)}...${number.takeLast(4)}"
            } else {
                number
            }
        }
        else -> number
    }
}
