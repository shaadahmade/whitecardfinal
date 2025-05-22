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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
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
import com.example.whitecard.qrutils.isValidAadhaar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AadhaarVerificationScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // States
    var hasCameraPermission by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var aadhaarNumber by remember { mutableStateOf("") }
    var detectionState by remember { mutableStateOf<DetectionState>(DetectionState.Scanning) }
    var processingFrame by remember { mutableStateOf(false) }
    var scanTimeoutActive by remember { mutableStateOf(false) }

    // Camera references
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val scannerLineY = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanner line"
    )

    // Permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            detectionState = DetectionState.Error("Camera permission required")
        }
    }

    // Check camera permission
    LaunchedEffect(key1 = true) {
        val permission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        )
        if (permission == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Reset scanner after timeout
    LaunchedEffect(scanTimeoutActive) {
        if (scanTimeoutActive) {
            delay(20000) // 20 seconds timeout
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
                        "Scan Aadhaar Card",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                progress = 0.33f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Text(
                text = when (detectionState) {
                    is DetectionState.Scanning -> "Position your Aadhaar Card within the frame"
                    is DetectionState.Success -> "Aadhaar Card detected"
                    is DetectionState.Error -> (detectionState as DetectionState.Error).message
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
                        CameraPermissionRequired(
                            onRequestPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                        )
                    }
                    detectionState is DetectionState.Success -> {
                        AadhaarResultView(aadhaarNumber = aadhaarNumber)
                    }
                    else -> {
                        CameraPreview(
                            hasCameraPermission = hasCameraPermission,
                            flashEnabled = flashEnabled,
                            onCameraReady = { newCamera -> camera = newCamera },
                            processImageForAadhaar = { imageProxy ->
                                if (!processingFrame) {
                                    processingFrame = true
                                    processImageForAadhaar(
                                        imageProxy = imageProxy,
                                        textRecognizer = textRecognizer
                                    ) { result ->
                                        if (result.isNotEmpty() && isValidAadhaar(result)) {
                                            aadhaarNumber = result
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

                        ScannerOverlay(
                            scannerLineY = scannerLineY.value,
                            scanningActive = detectionState is DetectionState.Scanning
                        )

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
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = "Toggle Flash",
                                    tint = if (flashEnabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
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
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Scanning...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    is DetectionState.Success -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Aadhaar card detected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    is DetectionState.Error -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = (detectionState as DetectionState.Error).message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        aadhaarNumber = ""
                        detectionState = DetectionState.Scanning
                        scanTimeoutActive = true
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry Scan")
                }

                Button(
                    onClick = {
                        // Save Aadhaar number to the savedStateHandle
                        navController.currentBackStackEntry?.savedStateHandle?.set("aadhaarNumber", aadhaarNumber)
                        // Navigate to the next screen
                        coroutineScope.launch {
                            navController.navigate("pan_verification")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    enabled = detectionState is DetectionState.Success,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionRequired(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "To scan your Aadhaar card, we need access to your camera",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Grant Permission")
        }
    }
}

@Composable
private fun AadhaarResultView(aadhaarNumber: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Aadhaar Card Detected",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "XXXX XXXX " + aadhaarNumber.takeLast(4),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Last 4 digits of your Aadhaar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(
    hasCameraPermission: Boolean,
    flashEnabled: Boolean,
    onCameraReady: (Camera) -> Unit,
    processImageForAadhaar: (ImageProxy) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    if (hasCameraPermission) {
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
                            .also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setTargetRotation(previewView.display.rotation)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        // Set analyzer with frame skipping
                        setupImageAnalysis(imageAnalyzer, processImageForAadhaar)

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
}

private fun setupImageAnalysis(
    imageAnalyzer: ImageAnalysis,
    processImageForAadhaar: (ImageProxy) -> Unit
) {
    var frameCount = 0
    val frameInterval = 3 // Process every 3rd frame

    imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
        frameCount++
        if (frameCount % frameInterval == 0) {
            processImageForAadhaar(imageProxy)
        } else {
            imageProxy.close()
        }
    }
}

@Composable
internal fun ScannerOverlay(
    scannerLineY: Float,
    scanningActive: Boolean
) {
    val cardStrokeWidth = with(LocalDensity.current) { 2.dp.toPx() }
    val cornerRadius = with(LocalDensity.current) { 16.dp.toPx() }
    val scannerLineColor = MaterialTheme.colorScheme.primary

    AnimatedVisibility(
        visible = scanningActive,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cardWidth = size.width * 0.85f
            val cardHeight = cardWidth * 0.63f
            val cardLeft = (size.width - cardWidth) / 2
            val cardTop = (size.height - cardHeight) / 2

            // Semi-transparent overlay
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = size
            )

            // Clear card scanning area
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(cardLeft, cardTop),
                size = Size(cardWidth, cardHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )

            // Draw card border
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(cardLeft, cardTop),
                size = Size(cardWidth, cardHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = cardStrokeWidth)
            )

            // Draw scanner line
            val scannerY = cardTop + (cardHeight * scannerLineY)
            drawLine(
                color = scannerLineColor,
                start = Offset(cardLeft, scannerY),
                end = Offset(cardLeft + cardWidth, scannerY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            // Draw corner guides


            val cornerLength = size.width * 0.05f
            val path = Path().apply {
                // Top-left corner
                moveTo(cardLeft, cardTop + cornerLength)
                lineTo(cardLeft, cardTop)
                lineTo(cardLeft + cornerLength, cardTop)

                // Top-right corner
                moveTo(cardLeft + cardWidth - cornerLength, cardTop)
                lineTo(cardLeft + cardWidth, cardTop)
                lineTo(cardLeft + cardWidth, cardTop + cornerLength)

                // Bottom-right corner
                moveTo(cardLeft + cardWidth, cardTop + cardHeight - cornerLength)
                lineTo(cardLeft + cardWidth, cardTop + cardHeight)
                lineTo(cardLeft + cardWidth - cornerLength, cardTop + cardHeight)

                // Bottom-left corner
                moveTo(cardLeft + cornerLength, cardTop + cardHeight)
                lineTo(cardLeft, cardTop + cardHeight)
                lineTo(cardLeft, cardTop + cardHeight - cornerLength)
            }

            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageForAadhaar(
    imageProxy: ImageProxy,
    textRecognizer: TextRecognizer,
    onResult: (String) -> Unit
) {
    val mediaImage = imageProxy.image ?: run {
        imageProxy.close()
        onResult("")
        return
    }

    val inputImage = InputImage.fromMediaImage(
        mediaImage,
        imageProxy.imageInfo.rotationDegrees
    )

    textRecognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            val extractedText = visionText.text

            // Optimized pattern to match 12-digit Aadhaar number (with or without spaces)
            val aadhaarPattern = Pattern.compile("\\b[2-9][0-9]{3}\\s?[0-9]{4}\\s?[0-9]{4}\\b")
            val matcher = aadhaarPattern.matcher(extractedText)

            if (matcher.find()) {
                val aadhaarNumber = matcher.group().replace("\\s".toRegex(), "")
                onResult(aadhaarNumber)
            } else {
                onResult("")
            }
        }
        .addOnFailureListener {
            Log.e("MLKit", "Text recognition failed", it)
            onResult("")
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

