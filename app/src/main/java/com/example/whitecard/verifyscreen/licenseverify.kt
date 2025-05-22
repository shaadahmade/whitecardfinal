package com.example.whitecard.verifyscreen

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.regex.Pattern
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseVerificationScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var licenseNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var holderName by remember { mutableStateOf("") }
    var detectionState by remember { mutableStateOf<DetectionState>(DetectionState.Scanning) }
    var processingFrame by remember { mutableStateOf(false) }
    var scanTimeoutActive by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Firebase
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Camera
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    // Animation for scanning line
    val infiniteTransition = rememberInfiniteTransition()
    val scannerLineY = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Camera Permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            detectionState = DetectionState.Error("Camera permission required")
        }
    }

    // Check camera permission once
    LaunchedEffect(key1 = true) {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Reset scan after timeout
    LaunchedEffect(scanTimeoutActive) {
        if (scanTimeoutActive) {
            delay(20000)
            if (detectionState is DetectionState.Scanning) {
                detectionState = DetectionState.Error("Scanning timeout. Please try again.")
                scanTimeoutActive = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Scan Driving License",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = 1f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Text(
                text = when (detectionState) {
                    is DetectionState.Scanning -> "Position your Driving License within the frame"
                    is DetectionState.Success -> "Driving License detected"
                    is DetectionState.Error -> (detectionState as DetectionState.Error).message
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                when {
                    !hasCameraPermission -> {
                        CameraPermissionRequired {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                    detectionState is DetectionState.Success -> {
                        LicenseResultView(
                            licenseNumber = licenseNumber,
                            expiryDate = expiryDate,
                            holderName = holderName
                        )
                    }
                    else -> {
                        CameraPreview(
                            flashEnabled = flashEnabled,
                            onCameraReady = { cam -> camera = cam },
                            processImageForLicense = { imageProxy ->
                                if (!processingFrame) {
                                    processingFrame = true
                                    processImageForLicense(
                                        imageProxy = imageProxy,
                                        textRecognizer = textRecognizer
                                    ) { detectedLicense, detectedExpiry, detectedName ->
                                        if (detectedLicense.isNotEmpty()) {
                                            licenseNumber = detectedLicense
                                            expiryDate = detectedExpiry
                                            holderName = detectedName
                                            detectionState = DetectionState.Success
                                            scanTimeoutActive = false
                                        }
                                        processingFrame = false
                                    }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        )
                        ScannerOverlay(scannerLineY = scannerLineY.value, scanningActive = detectionState is DetectionState.Scanning)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            IconButton(
                                onClick = {
                                    flashEnabled = !flashEnabled
                                    camera?.cameraControl?.enableTorch(flashEnabled)
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FlashOn,
                                    contentDescription = "Toggle Flash",
                                    tint = if (flashEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (detectionState) {
                    is DetectionState.Scanning -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Scanning...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    is DetectionState.Success -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("License detected", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                    is DetectionState.Error -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text((detectionState as DetectionState.Error).message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        licenseNumber = ""
                        expiryDate = ""
                        holderName = ""
                        detectionState = DetectionState.Scanning
                        scanTimeoutActive = true
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Retry Scan")
                }

                Button(
                    onClick = {
                        if (licenseNumber.isNotEmpty()) {
                            isLoading = true

                            // Get user ID
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                // Get previous data from navigation/argument state
                                val aadhaarNumber = navController.previousBackStackEntry?.savedStateHandle?.get<String>("aadhaarNumber") ?: ""
                                val panNumber = navController.previousBackStackEntry?.savedStateHandle?.get<String>("panNumber") ?: ""
                                val name = navController.previousBackStackEntry?.savedStateHandle?.get<String>("name") ?: ""

                                // Create user document
                                val userData = hashMapOf(
                                    "name" to name,
                                    "aadhaarNumber" to aadhaarNumber,
                                    "panNumber" to panNumber,
                                    "licenseNumber" to licenseNumber,
                                    "expiryDate" to expiryDate,
                                    "onboardingComplete" to true,
                                    "updatedAt" to Timestamp.now()
                                )

                                // Save to Firestore
                                db.collection("users").document(userId)
                                    .update(userData as Map<String, Any>)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        navController.navigate("main") {
                                            popUpTo("welcome") { inclusive = true }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        detectionState = DetectionState.Error(e.message ?: "Failed to save data")
                                    }
                            } else {
                                isLoading = false
                                detectionState = DetectionState.Error("User not authenticated")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    enabled = !isLoading && detectionState is DetectionState.Success,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Complete")
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionRequired(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Camera Permission Required", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("To scan your driving license, we need access to your camera", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text("Grant Permission")
        }
    }
}

@Composable
private fun LicenseResultView(licenseNumber: String, expiryDate: String, holderName: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("License Detected", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(24.dp))
                Text(
                    text = licenseNumber,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary
                )
                if (expiryDate.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Expires: $expiryDate",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (holderName.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = holderName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    flashEnabled: Boolean,
    onCameraReady: (Camera) -> Unit,
    processImageForLicense: (ImageProxy) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder()
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    setupImageAnalysis(imageAnalyzer, processImageForLicense)

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalyzer
                    )

                    onCameraReady(camera)
                    camera.cameraControl.enableTorch(flashEnabled)

                } catch (e: Exception) {
                    Log.e("Camera", "Binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun setupImageAnalysis(
    imageAnalyzer: ImageAnalysis,
    processImageForLicense: (ImageProxy) -> Unit
) {
    var frameCount = 0
    val frameInterval = 3 // process every third frame

    imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
        frameCount++
        if (frameCount % frameInterval == 0) {
            processImageForLicense(imageProxy)
        } else {
            imageProxy.close()
        }
    }
}



@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalGetImage::class)
private fun processImageForLicense(
    imageProxy: ImageProxy,
    textRecognizer: TextRecognizer,
    onResult: (String, String, String) -> Unit
) {
    val mediaImage = imageProxy.image ?: run {
        imageProxy.close()
        onResult("", "", "")
        return
    }

    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

    textRecognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            val text = visionText.text ?: ""

            // License number patterns (adjust based on your region)
            // Example patterns for Indian DL: StateCode-RTO Code-Year-Serial Number
            val licensePatterns = listOf(
                Pattern.compile("[A-Z]{2}[0-9]{2}[0-9]{11}"), // Standard Indian DL format
                Pattern.compile("[A-Z]{2}-[0-9]{13}"), // Alternative format
                Pattern.compile("[A-Z]{2}[0-9]{14}") // Compact format
            )

            var detectedLicense = ""
            for (pattern in licensePatterns) {
                val matcher = pattern.matcher(text.replace("\\s".toRegex(), ""))
                if (matcher.find()) {
                    detectedLicense = matcher.group()
                    break
                }
            }

            // Date pattern for expiry (DD/MM/YYYY or DD-MM-YYYY)
            val datePattern = Pattern.compile("\\b(0[1-9]|[12][0-9]|3[01])[-/](0[1-9]|1[0-2])[-/](19|20)\\d{2}\\b")
            val dateMatcher = datePattern.matcher(text)
            var detectedExpiry = ""
            if (dateMatcher.find()) {
                detectedExpiry = dateMatcher.group()
            }

            // Extract name (similar logic to PAN card)
            val lines = text.lines()
            var detectedName = ""

            for (line in lines) {
                if (line.contains("Name:", ignoreCase = true) ||
                    line.contains("Holder:", ignoreCase = true) ||
                    line.contains("Driver:", ignoreCase = true)) {
                    detectedName = line.substringAfter(":").trim()
                    break
                } else if (line.isNotBlank() &&
                    !licensePatterns.any { it.matcher(line).find() } &&
                    !datePattern.matcher(line).find() &&
                    line.length > 3) {
                    detectedName = line.trim()
                    break
                }
            }

            onResult(detectedLicense, detectedExpiry, detectedName)
        }
        .addOnFailureListener {
            Log.e("MLKit", "Text recognition failed", it)
            onResult("", "", "")
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}