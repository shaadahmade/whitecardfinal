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
import com.example.whitecard.qrutils.isValidPAN
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

// Detection state sealed class
sealed class DetectionState {
    object Scanning : DetectionState()
    object Success : DetectionState()
    data class Error(val message: String) : DetectionState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanVerificationScreen(navController: NavController) {
    val aadhaarNumber = navController.previousBackStackEntry?.savedStateHandle?.get<String>("aadhaarNumber") ?: ""
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var hasCameraPermission by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var panNumber by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var detectionState by remember { mutableStateOf<DetectionState>(DetectionState.Scanning) }
    var processingFrame by remember { mutableStateOf(false) }
    var scanTimeoutActive by remember { mutableStateOf(false) }

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
                        "Scan PAN Card",
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
                progress = 0.66f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Text(
                text = when (detectionState) {
                    is DetectionState.Scanning -> "Position your PAN Card within the frame"
                    is DetectionState.Success -> "PAN Card detected"
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
                        PanResultView(panNumber = panNumber, name = name)
                    }
                    else -> {
                        CameraPreview(
                            flashEnabled = flashEnabled,
                            onCameraReady = { cam -> camera = cam },
                            processImageForPAN = { imageProxy ->
                                if (!processingFrame) {
                                    processingFrame = true
                                    processImageForPAN(
                                        imageProxy = imageProxy,
                                        textRecognizer = textRecognizer
                                    ) { detectedName, detectedPAN ->
                                        if (detectedPAN.isNotEmpty() && isValidPAN(detectedPAN)) {
                                            panNumber = detectedPAN
                                            name = detectedName
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
                            Text("PAN card detected", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
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
                        panNumber = ""
                        name = ""
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
                        coroutineScope.launch {
                            navController.currentBackStackEntry?.savedStateHandle?.set("name", name)
                            navController.currentBackStackEntry?.savedStateHandle?.set("panNumber", panNumber)
                            navController.currentBackStackEntry?.savedStateHandle?.set("aadhaarNumber", aadhaarNumber)
                            navController.navigate("license_verification")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    enabled = detectionState is DetectionState.Success,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Camera Permission Required", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("To scan your PAN card, we need access to your camera", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text("Grant Permission")
        }
    }
}

@Composable
private fun PanResultView(panNumber: String, name: String) {
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
                Text("PAN Card Detected", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(24.dp))
                Text(
                    text = panNumber,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = name.ifBlank { "Name not detected" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}



@Composable
private fun CameraPreview(
    flashEnabled: Boolean,
    onCameraReady: (Camera) -> Unit,
    processImageForPAN: (ImageProxy) -> Unit
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

                    setupImageAnalysis(imageAnalyzer, processImageForPAN)

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
    processImageForPAN: (ImageProxy) -> Unit
) {
    var frameCount = 0
    val frameInterval = 3 // process every third frame

    imageAnalyzer.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
        frameCount++
        if (frameCount % frameInterval == 0) {
            processImageForPAN(imageProxy)
        } else {
            imageProxy.close()
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalGetImage::class)
private fun processImageForPAN(
    imageProxy: ImageProxy,
    textRecognizer: TextRecognizer,
    onResult: (String, String) -> Unit
) {
    val mediaImage = imageProxy.image ?: run {
        imageProxy.close()
        onResult("", "")
        return
    }

    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

    textRecognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            val allText = visionText.text
            Log.d("PANScan", "Detected text: $allText")

            // Extract PAN number
            val panPattern = Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]")
            val panMatcher = panPattern.matcher(allText)
            val detectedPAN = if (panMatcher.find()) panMatcher.group() else ""

            // Extract name using the new robust algorithm
            val detectedName = extractNameFromPANCard(visionText)
            Log.d("PANScan", "Detected PAN: $detectedPAN, Name: $detectedName")

            onResult(detectedName, detectedPAN)
        }
        .addOnFailureListener { e ->
            Log.e("PANScan", "Text recognition failed", e)
            onResult("", "")
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

private fun extractNameFromPANCard(visionText: com.google.mlkit.vision.text.Text): String {
    // Get all detected text blocks and lines
    val textBlocks = visionText.textBlocks
    val allLines = textBlocks.flatMap { it.lines }
    val allLineText = allLines.map { it.text.trim() }

    // Log all detected lines for debugging
    for ((index, line) in allLineText.withIndex()) {
        Log.d("PANScan", "Line $index: $line")
    }

    // 1. First try to find name by explicit label
    val nameByLabel = findNameByExplicitLabel(allLines)
    if (nameByLabel.isNotBlank()) {
        Log.d("PANScan", "Found name by label: $nameByLabel")
        return cleanupName(nameByLabel)
    }

    // 2. Try to find name by position relative to "Income Tax Department"
    val nameByHeader = findNameAfterHeader(allLines)
    if (nameByHeader.isNotBlank()) {
        Log.d("PANScan", "Found name by header position: $nameByHeader")
        return cleanupName(nameByHeader)
    }

    // 3. Look for name near the PAN number
    val nameByPAN = findNameNearPAN(allLines)
    if (nameByPAN.isNotBlank()) {
        Log.d("PANScan", "Found name near PAN: $nameByPAN")
        return cleanupName(nameByPAN)
    }

    // 4. Fallback: Look for the most name-like text
    val nameByHeuristics = findMostLikelyName(allLines)
    Log.d("PANScan", "Fallback name by heuristics: $nameByHeuristics")
    return cleanupName(nameByHeuristics)
}

private fun findNameByExplicitLabel(lines: List<com.google.mlkit.vision.text.Text.Line>): String {
    // Look for lines containing explicit name labels
    val nameLabels = listOf("Name", "NAME", "Name:", "NAME:")

    for (i in lines.indices) {
        val line = lines[i].text.trim()

        // Check if this line contains a name label
        for (label in nameLabels) {
            if (line.contains(label, ignoreCase = true)) {
                // Check if name is on the same line after the label
                val afterLabel = line.substringAfter(label, "").trim()
                if (afterLabel.isNotBlank() && isValidNameCandidate(afterLabel)) {
                    return afterLabel
                }

                // Check if name is on the next line
                if (i < lines.size - 1) {
                    val nextLine = lines[i + 1].text.trim()
                    if (isValidNameCandidate(nextLine)) {
                        return nextLine
                    }
                }
            }
        }
    }

    return ""
}

private fun findNameAfterHeader(lines: List<com.google.mlkit.vision.text.Text.Line>): String {
    // In many PAN cards, name appears right after "INCOME TAX DEPARTMENT" or "GOVT OF INDIA"
    val headerTexts = listOf(
        "INCOME TAX DEPARTMENT",
        "GOVT OF INDIA",
        "GOVERNMENT OF INDIA"
    )

    for (i in lines.indices) {
        val line = lines[i].text.trim()

        // Check if this line is a header
        if (headerTexts.any { header -> line.contains(header, ignoreCase = true) }) {
            // Name might be 1-2 lines after the header, depending on the card format
            for (j in 1..3) {
                if (i + j < lines.size) {
                    val candidateLine = lines[i + j].text.trim()

                    // Skip lines about "Permanent Account Number"
                    if (candidateLine.contains("Permanent", ignoreCase = true) ||
                        candidateLine.contains("Account", ignoreCase = true) ||
                        candidateLine.contains("Number", ignoreCase = true) ||
                        candidateLine.contains("Card", ignoreCase = true)) {
                        continue
                    }

                    // Check if this line looks like a name
                    if (isValidNameCandidate(candidateLine)) {
                        return candidateLine
                    }
                }
            }
        }
    }

    return ""
}

private fun findNameNearPAN(lines: List<com.google.mlkit.vision.text.Text.Line>): String {
    // Find the PAN number first
    val panPattern = Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]")
    var panLineIndex = -1

    for (i in lines.indices) {
        if (panPattern.matcher(lines[i].text).find()) {
            panLineIndex = i
            break
        }
    }

    if (panLineIndex == -1) return ""

    // In many card formats, name is 1-3 lines before or after the PAN
    val rangeToCheck = listOf(-3, -2, -1, 1, 2, 3)

    for (offset in rangeToCheck) {
        val targetIndex = panLineIndex + offset
        if (targetIndex in lines.indices) {
            val candidateLine = lines[targetIndex].text.trim()

            // Skip lines with common PAN card texts
            if (containsCommonPANText(candidateLine)) continue

            // Skip lines with father's name (often found near PAN)
            if (candidateLine.contains("Father", ignoreCase = true)) continue

            // Check if this line looks like a name
            if (isValidNameCandidate(candidateLine)) {
                return candidateLine
            }
        }
    }

    return ""
}

private fun findMostLikelyName(lines: List<com.google.mlkit.vision.text.Text.Line>): String {
    // Score each line by how likely it is to be a name
    val candidates = mutableListOf<Pair<String, Int>>()

    for (line in lines) {
        val text = line.text.trim()

        // Skip empty or very short lines
        if (text.isBlank() || text.length < 3) continue

        // Skip lines with common PAN card texts
        if (containsCommonPANText(text)) continue

        // Score this line based on name-like characteristics
        var score = 0

        // Check if text consists mostly of letters
        val letterRatio = text.count { it.isLetter() }.toFloat() / text.length
        if (letterRatio > 0.8) score += 10

        // Check for absence of digits (names don't have numbers)
        if (!text.any { it.isDigit() }) score += 8

        // Prefer names with 2-4 words
        val wordCount = text.split(Regex("\\\\s+")).filter { it.isNotBlank() }.size
        if (wordCount in 2..4) score += 6

        // Prefer appropriate name length
        if (text.length in 5..30) score += 4

        // Check for common Indian name formats (all caps or properly capitalized)
        if (text == text.uppercase() || hasProperCapitalization(text)) score += 5

        // Add to candidates if it meets minimum criteria
        if (score >= 15) {
            candidates.add(Pair(text, score))
        }
    }

    // Return the highest scoring candidate, or empty string if none found
    return candidates.maxByOrNull { it.second }?.first ?: ""
}

private fun hasProperCapitalization(text: String): Boolean {
    val words = text.split(Regex("\\\\s+")).filter { it.isNotBlank() }

    // Check if each word starts with uppercase and continues with lowercase
    return words.all { word ->
        word.isNotBlank() && word.first().isUpperCase() &&
                word.drop(1).all { it.isLowerCase() || !it.isLetter() }
    }
}

private fun containsCommonPANText(text: String): Boolean {
    val commonTexts = listOf(
        "INCOME", "TAX", "DEPARTMENT", "GOVT", "GOVERNMENT", "INDIA",
        "PERMANENT", "ACCOUNT", "NUMBER", "CARD", "PAN", "SIGNATURE",
        "DATE", "BIRTH", "DOB", "FATHER", "SIGN"
    )

    // Check if the text contains any common PAN card text
    return commonTexts.any { commonText ->
        text.contains(commonText, ignoreCase = true)
    }
}

private fun isValidNameCandidate(text: String): Boolean {
    val cleanText = text.trim()

    // Basic validation
    if (cleanText.length < 3 || cleanText.length > 40) return false

    // Should be mostly letters with possible spaces and punctuation
    val letterCount = cleanText.count { it.isLetter() }
    if (letterCount < cleanText.length * 0.7) return false

    // Should not contain digits
    if (cleanText.any { it.isDigit() }) return false

    // Should not contain common PAN card texts
    if (containsCommonPANText(cleanText)) return false

    // Allow letters, spaces, dots, apostrophes, and hyphens (common in names)
    val nameRegex = Regex("[^A-Za-z\\\\s.'-]")
    if (nameRegex.find(cleanText) != null) return false

    return true
}

private fun cleanupName(name: String): String {
    // Initial cleanup - remove excess whitespace
    var cleanedName = name.trim().replace(Regex("\\\\s+"), " ")

    // Fix common OCR errors in names
    val ocrCorrections = mapOf(
        "0" to "O",
        "1" to "I",
        "5" to "S",
        "8" to "B",
        "6" to "G",
        "!" to "I"
    )

    // Apply OCR corrections
    for ((error, correction) in ocrCorrections) {
        cleanedName = cleanedName.replace(error, correction)
    }

    // Convert to proper case (first letter of each word capitalized)
    return cleanedName.split(" ").joinToString(" ") { word ->
        if (word.isNotBlank()) {
            // If all uppercase, convert to title case
            if (word == word.uppercase()) {
                word.lowercase().replaceFirstChar { it.uppercase() }
            } else {
                word
            }
        } else {
            word
        }
    }
}
