package com.example.whitecard.verifyscreen
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceLandmark

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.Rect
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.example.whitecard.R

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.*

enum class FaceDetectionStage {
    AADHAAR_CARD,
    AADHAAR_FACE_DETECTION,
    SELFIE,
    LIVENESS_CHECK,
    FACE_COMPARISON,
    COMPLETE
}

data class FacialData(
    val landmarks: Map<Int, PointF>,
    val contours: Map<Int, List<PointF>>,
    val boundingBox: Rect,
    val headEulerAngleX: Float,
    val headEulerAngleY: Float,
    val headEulerAngleZ: Float,
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?,
    val smilingProbability: Float?
)

sealed class VerificationError {
    object NoFaceDetected : VerificationError()
    object MultipleFacesDetected : VerificationError()
    object FaceTooSmall : VerificationError()
    object FaceNotCentered : VerificationError()
    object PoorLighting : VerificationError()
    object SpoofingDetected : VerificationError()
    object NoMatchFound : VerificationError()
    object InvalidAadhaarNumber : VerificationError()
    object LivenessCheckFailed : VerificationError()
    data class SystemError(val message: String) : VerificationError()
}

enum class LivenessChallenge {
    BLINK, SMILE, TURN_LEFT, TURN_RIGHT, NOD
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun AadhaarVerificationScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Camera and executor resources
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    // State variables
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var aadhaarNumber by remember { mutableStateOf("") }
    var currentError by remember { mutableStateOf<VerificationError?>(null) }
    var isCameraActive by remember { mutableStateOf(true) }
    var capturedAadhaarImage by remember { mutableStateOf<Bitmap?>(null) }
    var capturedFaceImage by remember { mutableStateOf<Bitmap?>(null) }
    var faceMatchResult by remember { mutableStateOf<Boolean?>(null) }
    var faceMatchScore by remember { mutableStateOf<Double>(0.0) }
    var aadhaarFacialData by remember { mutableStateOf<FacialData?>(null) }
    var selfieFacialData by remember { mutableStateOf<FacialData?>(null) }
    var faceDetectionStage by remember { mutableStateOf(FaceDetectionStage.AADHAAR_CARD) }
    var isLoading by remember { mutableStateOf(false) }
    var previousFaces by remember { mutableStateOf<List<Face>>(emptyList()) }
    var livenessChallenge by remember { mutableStateOf<LivenessChallenge?>(null) }
    var livenessVerified by remember { mutableStateOf(false) }
    var challengeStartTime by remember { mutableStateOf(0L) }

    // Timeout tracking
    var operationTimeoutJob by remember { mutableStateOf<Job?>(null) }

    // Request camera permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            currentError = VerificationError.SystemError("Camera permission is required for verification")
        }
    }

    // Initialize ML Kit instances with proper options
    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    val faceDetectorOptions = remember {
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f) // Increased minimum face size for better detection
            .enableTracking()
            .build()
    }

    val faceDetector = remember {
        FaceDetection.getClient(faceDetectorOptions)
    }

    // Request camera permission on first launch
    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Reset timeout whenever stage changes
    LaunchedEffect(faceDetectionStage) {
        operationTimeoutJob?.cancel()
        operationTimeoutJob = scope.launch {
            delay(30000) // 30 second timeout
            if (isActive) {
                currentError = VerificationError.SystemError("The operation timed out. Please try again.")
                faceDetectionStage = FaceDetectionStage.AADHAAR_CARD
                isCameraActive = true
                isLoading = false
            }
        }
    }

    // Set up liveness challenge when reaching that stage
    LaunchedEffect(faceDetectionStage) {
        if (faceDetectionStage == FaceDetectionStage.LIVENESS_CHECK) {
            livenessChallenge = LivenessChallenge.values().random()
            challengeStartTime = System.currentTimeMillis()
            livenessVerified = false
        }
    }

    // Clean up resources when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            operationTimeoutJob?.cancel()
            cameraExecutor.shutdown()
            camera?.cameraControl?.enableTorch(false)
        }
    }

    val validInput = aadhaarNumber.length == 12 && isValidAadhaar(aadhaarNumber) && faceMatchResult == true
    val gradientColors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)

    Box(modifier = Modifier
        .fillMaxSize()
        .background(brush = Brush.verticalGradient(colors = gradientColors, startY = 0f, endY = 2000f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header card with progress
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WHITE CARD",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val progressValue = when(faceDetectionStage) {
                        FaceDetectionStage.AADHAAR_CARD -> 0.1f
                        FaceDetectionStage.AADHAAR_FACE_DETECTION -> 0.3f
                        FaceDetectionStage.SELFIE -> 0.5f
                        FaceDetectionStage.LIVENESS_CHECK -> 0.7f
                        FaceDetectionStage.FACE_COMPARISON -> 0.9f
                        FaceDetectionStage.COMPLETE -> 1.0f
                    }

                    LinearProgressIndicator(
                        progress = progressValue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Step ${getStepNumber(faceDetectionStage)} of 5",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${(progressValue * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title and instruction
            Text(
                text = "Aadhaar Verification",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = getInstructionText(faceDetectionStage, livenessChallenge),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Camera preview or loading or verification result
            if (isCameraActive && hasCameraPermission &&
                (faceDetectionStage == FaceDetectionStage.AADHAAR_CARD ||
                        faceDetectionStage == FaceDetectionStage.SELFIE ||
                        faceDetectionStage == FaceDetectionStage.LIVENESS_CHECK)) {

                // Camera preview with scan overlay
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CameraPreview(
                            faceDetectionStage = faceDetectionStage,
                            onAadhaarDetected = { detectedAadhaar, bitmap ->
                                aadhaarNumber = detectedAadhaar
                                capturedAadhaarImage = bitmap
                                faceDetectionStage = FaceDetectionStage.AADHAAR_FACE_DETECTION
                                isLoading = true

                                scope.launch {
                                    try {
                                        val faceData = detectFaceOnAadhaarCard(
                                            capturedAadhaarImage!!,
                                            faceDetector
                                        )

                                        if (faceData != null) {
                                            aadhaarFacialData = faceData
                                            faceDetectionStage = FaceDetectionStage.SELFIE
                                            isCameraActive = true
                                        } else {
                                            currentError = VerificationError.NoFaceDetected
                                            faceDetectionStage = FaceDetectionStage.AADHAAR_CARD
                                        }
                                    } catch (e: Exception) {
                                        currentError = VerificationError.SystemError(
                                            "Error during face detection: ${e.localizedMessage}"
                                        )
                                        faceDetectionStage = FaceDetectionStage.AADHAAR_CARD
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            onSelfieCaptured = { bitmap, faceData ->
                                capturedFaceImage = bitmap
                                selfieFacialData = faceData
                                faceDetectionStage = FaceDetectionStage.LIVENESS_CHECK
                            },
                            onLivenessVerified = { verified ->
                                if (verified) {
                                    livenessVerified = true
                                    faceDetectionStage = FaceDetectionStage.FACE_COMPARISON
                                    isCameraActive = false
                                    isLoading = true

                                    scope.launch {
                                        try {
                                            val matchScore = compareFaces(aadhaarFacialData!!, selfieFacialData!!)
                                            faceMatchScore = matchScore
                                            faceMatchResult = matchScore >= 0.85
                                            faceDetectionStage = FaceDetectionStage.COMPLETE
                                        } catch (e: Exception) {
                                            currentError = VerificationError.SystemError(
                                                "Error comparing faces: ${e.localizedMessage}"
                                            )
                                            faceDetectionStage = FaceDetectionStage.SELFIE
                                            isCameraActive = true
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                } else {
                                    currentError = VerificationError.LivenessCheckFailed
                                    // Give another chance for liveness verification
                                    livenessChallenge = LivenessChallenge.values().random()
                                    challengeStartTime = System.currentTimeMillis()
                                }
                            },
                            onFaceDetectionUpdate = { faces ->
                                // For liveness check, we need to track faces across frames
                                if (faceDetectionStage == FaceDetectionStage.LIVENESS_CHECK && faces.isNotEmpty()) {
                                    val currentFace = faces[0]
                                    handleLivenessChallenge(
                                        currentFace,
                                        previousFaces.firstOrNull(),
                                        livenessChallenge,
                                        challengeStartTime
                                    ) { result ->
                                        if (result) {
                                            livenessVerified = true
                                            faceDetectionStage = FaceDetectionStage.FACE_COMPARISON
                                            isCameraActive = false
                                            isLoading = true

                                            scope.launch {
                                                try {
                                                    val matchScore = compareFaces(aadhaarFacialData!!, selfieFacialData!!)
                                                    faceMatchScore = matchScore
                                                    faceMatchResult = matchScore >= 0.85
                                                    faceDetectionStage = FaceDetectionStage.COMPLETE
                                                } catch (e: Exception) {
                                                    currentError = VerificationError.SystemError(
                                                        "Error comparing faces: ${e.localizedMessage}"
                                                    )
                                                    faceDetectionStage = FaceDetectionStage.SELFIE
                                                    isCameraActive = true
                                                } finally {
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    }
                                    previousFaces = faces
                                }
                            },
                            onError = { error ->
                                currentError = error
                            },
                            textRecognizer = textRecognizer,
                            faceDetector = faceDetector,
                            cameraExecutor = cameraExecutor,
                            onCameraInitialized = { cam ->
                                camera = cam
                            }
                        )

                        // Scanner overlay
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            when (faceDetectionStage) {
                                FaceDetectionStage.AADHAAR_CARD -> {
                                    // Aadhaar card frame
                                    Box(modifier = Modifier.align(Alignment.Center).size(280.dp, 170.dp)) {
                                        Box(modifier = Modifier
                                            .fillMaxSize()
                                            .border(
                                                width = 2.dp,
                                                color = Color.White,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        )

                                        // Corner markers
                                        val cornerSize = 20.dp
                                        Box(modifier = Modifier
                                            .size(cornerSize)
                                            .align(Alignment.TopStart)
                                            .border(
                                                width = 2.dp,
                                                color = Color.White,
                                                shape = RoundedCornerShape(topStart = 8.dp)
                                            )
                                        )
                                        Box(modifier = Modifier
                                            .size(cornerSize)
                                            .align(Alignment.TopEnd)
                                            .border(
                                                width = 2.dp,
                                                color = Color.White,
                                                shape = RoundedCornerShape(topEnd = 8.dp)
                                            )
                                        )
                                        Box(modifier = Modifier
                                            .size(cornerSize)
                                            .align(Alignment.BottomStart)
                                            .border(
                                                width = 2.dp,
                                                color = Color.White,
                                                shape = RoundedCornerShape(bottomStart = 8.dp)
                                            )
                                        )
                                        Box(modifier = Modifier
                                            .size(cornerSize)
                                            .align(Alignment.BottomEnd)
                                            .border(
                                                width = 2.dp,
                                                color = Color.White,
                                                shape = RoundedCornerShape(bottomEnd = 8.dp)
                                            )
                                        )
                                    }
                                }
                                FaceDetectionStage.SELFIE, FaceDetectionStage.LIVENESS_CHECK -> {
                                    // Face frame
                                    Box(modifier = Modifier.align(Alignment.Center).size(220.dp)) {
                                        Box(modifier = Modifier
                                            .fillMaxSize()
                                            .border(width = 2.dp, color = Color.White, shape = CircleShape)
                                        )
                                        Box(modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp)
                                            .border(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                        )

                                        // Face icon as position guide
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = "Face Position",
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier
                                                .size(32.dp)
                                                .align(Alignment.TopCenter)
                                                .padding(top = 8.dp)
                                        )
                                    }
                                }
                                else -> {} // No overlay for other stages
                            }

                            // Instruction text
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val instructionText = when (faceDetectionStage) {
                                    FaceDetectionStage.AADHAAR_CARD ->
                                        "Align your Aadhaar card within the frame"
                                    FaceDetectionStage.SELFIE ->
                                        "Position your face within the circle"
                                    FaceDetectionStage.LIVENESS_CHECK ->
                                        getLivenessChallengeInstructionText(livenessChallenge)
                                    else -> ""
                                }

                                if (instructionText.isNotEmpty()) {
                                    Text(
                                        text = instructionText,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier
                                            .background(
                                                Color.Black.copy(alpha = 0.7f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }

                                if (faceDetectionStage == FaceDetectionStage.SELFIE ||
                                    faceDetectionStage == FaceDetectionStage.LIVENESS_CHECK) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Make sure your face is well-lit and centered",
                                        color = Color.White.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier
                                            .background(
                                                Color.Black.copy(alpha = 0.5f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (isLoading) {
                // Loading indicator
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = when(faceDetectionStage) {
                                    FaceDetectionStage.AADHAAR_FACE_DETECTION ->
                                        "Detecting face on Aadhaar card..."
                                    FaceDetectionStage.FACE_COMPARISON ->
                                        "Comparing face with Aadhaar..."
                                    else -> "Processing..."
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else if (faceDetectionStage == FaceDetectionStage.COMPLETE) {
                // Verification result card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Display images side by side
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            if (capturedAadhaarImage != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Aadhaar Photo",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        modifier = Modifier.size(120.dp),
                                        elevation = CardDefaults.cardElevation(4.dp)
                                    ) {
                                        Image(
                                            bitmap = capturedAadhaarImage!!.asImageBitmap(),
                                            contentDescription = "Aadhaar Photo",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }

                            if (capturedFaceImage != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Your Selfie",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        modifier = Modifier.size(120.dp),
                                        elevation = CardDefaults.cardElevation(4.dp)
                                    ) {
                                        Image(
                                            bitmap = capturedFaceImage!!.asImageBitmap(),
                                            contentDescription = "Selfie",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Aadhaar number display
                        if (aadhaarNumber.isNotEmpty()) {
                            val formattedAadhaar = formatAadhaarNumber(aadhaarNumber)

                            Text(
                                text = "Detected Aadhaar Number:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = formattedAadhaar,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (isValidAadhaar(aadhaarNumber))
                                    Color(0xFF00C853)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            if (isValidAadhaar(aadhaarNumber)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF00C853)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Valid Aadhaar Number",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF00C853)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Face verification result
                            if (faceMatchResult != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (faceMatchResult == true)
                                            Color(0xFFE8F5E9)
                                        else
                                            Color(0xFFFFEBEE)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (faceMatchResult == true)
                                                Icons.Default.Check
                                            else
                                                Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (faceMatchResult == true)
                                                Color(0xFF00C853)
                                            else
                                                MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(24.dp)
                                        )

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column {
                                            Text(
                                                text = if (faceMatchResult == true)
                                                    "Face Verification Successful"
                                                else
                                                    "Face Verification Failed",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (faceMatchResult == true)
                                                    Color(0xFF00C853)
                                                else
                                                    MaterialTheme.colorScheme.error
                                            )

                                            Text(
                                                text = if (faceMatchResult == true)
                                                    "Your face matches with the Aadhaar card photo. " +
                                                            "Match confidence: ${(faceMatchScore * 100).toInt()}%"
                                                else
                                                    "Your face does not match with the Aadhaar card. " +
                                                            "Match confidence: ${(faceMatchScore * 100).toInt()}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Error message if any
            if (currentError != null) {
                ErrorMessage(
                    error = currentError!!,
                    onDismiss = { currentError = null }
                )
            }

            // Action button
            if (faceDetectionStage == FaceDetectionStage.COMPLETE) {
                Button(
                    onClick = {
                        if (validInput) {
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                "aadhaarNumber",
                                aadhaarNumber
                            )
                            navController.navigate("pan_verification")
                        } else if (faceMatchResult == false) {
                            // Reset to try again
                            resetVerificationProcess(
                                onReset = {
                                    faceDetectionStage = FaceDetectionStage.AADHAAR_CARD
                                    isCameraActive = true
                                    faceMatchResult = null
                                    capturedAadhaarImage = null
                                    capturedFaceImage = null
                                    aadhaarNumber = ""
                                    currentError = null
                                    isLoading = false
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = isValidAadhaar(aadhaarNumber)
                ) {
                    Icon(
                        imageVector = if (faceMatchResult == true)
                            Icons.Default.CreditCard
                        else
                            Icons.Default.Camera,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (faceMatchResult == true)
                            "Continue to PAN Verification"
                        else
                            "Try Again",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    // Show loading dialog if processing
    if (isLoading) {
        LoadingDialog(
            message = when(faceDetectionStage) {
                FaceDetectionStage.AADHAAR_FACE_DETECTION -> "Detecting face on Aadhaar card..."
                FaceDetectionStage.FACE_COMPARISON -> "Comparing faces..."
                else -> "Processing..."
            }
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(
    faceDetectionStage: FaceDetectionStage,
    onAadhaarDetected: (String, Bitmap) -> Unit,
    onSelfieCaptured: (Bitmap, FacialData) -> Unit,
    onLivenessVerified: (Boolean) -> Unit,
    onFaceDetectionUpdate: (List<Face>) -> Unit,
    onError: (VerificationError) -> Unit,
    textRecognizer: com.google.mlkit.vision.text.TextRecognizer,
    faceDetector: FaceDetector,
    cameraExecutor: ExecutorService,
    onCameraInitialized: (Camera) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetResolution(Size(1080, 1440))
                    .build()

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetResolution(Size(720, 1280))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analyzer ->
                        analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImageProxy(
                                imageProxy = imageProxy,
                                faceDetectionStage = faceDetectionStage,
                                textRecognizer = textRecognizer,
                                faceDetector = faceDetector,
                                onAadhaarDetected = { aadhaarNumber ->
                                    imageCapture.takePicture(
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageCapturedCallback() {
                                            override fun onCaptureSuccess(image: ImageProxy) {
                                                val bitmap = image.toBitmap()
                                                onAadhaarDetected(aadhaarNumber, bitmap)
                                                image.close()
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                onError(
                                                    VerificationError.SystemError(
                                                        "Failed to capture image: ${exception.message}"
                                                    )
                                                )
                                                super.onError(exception)
                                            }
                                        }
                                    )
                                },
                                onFaceDetected = { faces ->
                                    if (faceDetectionStage == FaceDetectionStage.SELFIE && faces.isNotEmpty()) {
                                        val face = faces[0]
                                        val faceBox = face.boundingBox
                                        val imageWidth = imageProxy.width
                                        val imageHeight = imageProxy.height

                                        // Check if face is centered
                                        val faceCenterX = faceBox.centerX().toFloat()
                                        val faceCenterY = faceBox.centerY().toFloat()
                                        val imageCenterX = imageWidth / 2f
                                        val imageCenterY = imageHeight / 2f

                                        val distanceFromCenter = sqrt(
                                            (faceCenterX - imageCenterX).pow(2) +
                                                    (faceCenterY - imageCenterY).pow(2)
                                        )

                                        // Check face size
                                        val faceSize = max(faceBox.width(), faceBox.height())
                                        val minSize = min(imageWidth, imageHeight) * 0.3f

                                        // Only capture when face is centered and properly sized
                                        if (distanceFromCenter < imageWidth * 0.15f && faceSize > minSize) {
                                            imageCapture.takePicture(
                                                ContextCompat.getMainExecutor(context),
                                                object : ImageCapture.OnImageCapturedCallback() {
                                                    override fun onCaptureSuccess(image: ImageProxy) {
                                                        val bitmap = image.toBitmap()

                                                        // Extract facial landmarks
                                                        val facialData = extractFacialData(face)
                                                        onSelfieCaptured(bitmap, facialData)
                                                        image.close()
                                                    }

                                                    override fun onError(exception: ImageCaptureException) {
                                                        onError(
                                                            VerificationError.SystemError(
                                                                "Failed to capture selfie: ${exception.message}"
                                                            )
                                                        )
                                                        super.onError(exception)
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    onFaceDetectionUpdate(faces)
                                },
                                onError = { error ->
                                    onError(error)
                                }
                            )
                        }
                    }

                val cameraSelector = if (faceDetectionStage == FaceDetectionStage.AADHAAR_CARD)
                    CameraSelector.DEFAULT_BACK_CAMERA
                else
                    CameraSelector.DEFAULT_FRONT_CAMERA

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalyzer
                    )

                    // Enable auto-focus
                    camera.cameraControl.enableTorch(false)
                    onCameraInitialized(camera)

                } catch (e: Exception) {
                    Log.e("CameraX", "Use case binding failed", e)
                    onError(VerificationError.SystemError("Camera initialization failed: ${e.message}"))
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@ExperimentalGetImage
private fun processImageProxy(
    imageProxy: ImageProxy,
    faceDetectionStage: FaceDetectionStage,
    textRecognizer: com.google.mlkit.vision.text.TextRecognizer,
    faceDetector: FaceDetector,
    onAadhaarDetected: (String) -> Unit,
    onFaceDetected: (List<Face>) -> Unit,
    onError: (VerificationError) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        when (faceDetectionStage) {
            FaceDetectionStage.AADHAAR_CARD -> {
                // Detect Aadhaar number with OCR
                textRecognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val detectedAadhaar = extractAadhaarNumber(visionText.text)
                        if (detectedAadhaar.isNotEmpty() && isValidAadhaar(detectedAadhaar)) {
                            onAadhaarDetected(detectedAadhaar)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("TextRecognition", "Text recognition failed", e)
                        onError(VerificationError.SystemError("Text recognition failed: ${e.message}"))
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
            FaceDetectionStage.SELFIE, FaceDetectionStage.LIVENESS_CHECK -> {
                // Detect face
                faceDetector.process(image)
                    .addOnSuccessListener { faces ->
                        if (faces.isEmpty()) {
                            onError(VerificationError.NoFaceDetected)
                        } else if (faces.size > 1) {
                            onError(VerificationError.MultipleFacesDetected)
                        } else {
                            onFaceDetected(faces)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FaceDetection", "Face detection failed", e)
                        onError(VerificationError.SystemError("Face detection failed: ${e.message}"))
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
            else -> imageProxy.close()
        }
    } else {
        imageProxy.close()
    }
}

@Composable
private fun ErrorMessage(
    error: VerificationError,
    onDismiss: () -> Unit
) {
    val (icon, title, message) = when(error) {
        is VerificationError.NoFaceDetected -> Triple(
            Icons.Default.Face,
            "No Face Detected",
            "Please make sure your face is clearly visible"
        )
        is VerificationError.MultipleFacesDetected -> Triple(
            Icons.Default.People,
            "Multiple Faces Detected",
            "Please ensure only your face is in the frame"
        )
        is VerificationError.FaceTooSmall -> Triple(
            Icons.Default.ZoomIn,
            "Face Too Small",
            "Please move closer to the camera"
        )
        is VerificationError.FaceNotCentered -> Triple(
            Icons.Default.CenterFocusWeak,
            "Face Not Centered",
            "Please center your face in the frame"
        )
        is VerificationError.PoorLighting -> Triple(
            Icons.Default.WbSunny,
            "Poor Lighting",
            "Please move to a better lit area"
        )
        is VerificationError.SpoofingDetected -> Triple(
            Icons.Default.Security,
            "Verification Issue",
            "Please ensure you're not using a photo or screen"
        )
        is VerificationError.NoMatchFound -> Triple(
            Icons.Default.PersonSearch,
            "No Match Found",
            "Your face doesn't match with the Aadhaar card photo"
        )
        is VerificationError.InvalidAadhaarNumber -> Triple(
            Icons.Default.ErrorOutline,
            "Invalid Aadhaar Number",
            "The detected Aadhaar number is invalid"
        )
        is VerificationError.LivenessCheckFailed -> Triple(
            Icons.Default.Face,
            "Liveness Check Failed",
            "Please complete the challenge to verify you're a real person"
        )
        is VerificationError.SystemError -> Triple(
            Icons.Default.Error,
            "System Error",
            error.message
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

// Helper function to extract Aadhaar number from text
private fun extractAadhaarNumber(text: String): String {
    // Regular expression patterns for various Aadhaar number formats
    val patterns = listOf(
        "\\\\b\\\\d{4}\\\\s\\\\d{4}\\\\s\\\\d{4}\\\\b".toRegex(),  // Format: 1234 5678 9012
        "\\\\b\\\\d{12}\\\\b".toRegex()                    // Format: 123456789012
    )

    for (pattern in patterns) {
        val matchResult = pattern.find(text)
        if (matchResult != null) {
            val matched = matchResult.value
            val digits = matched.replace("\\\\s".toRegex(), "")
            if (digits.length == 12 && isValidAadhaar(digits)) {
                return digits
            }
        }
    }

    return ""
}

// Validates Aadhaar number using Verhoeff algorithm
private fun isValidAadhaar(aadhaar: String): Boolean {
    if (aadhaar.length != 12 || !aadhaar.all { it.isDigit() }) {
        return false
    }

    // Verhoeff algorithm tables
    val d = arrayOf(
        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
        intArrayOf(1, 2, 3, 4, 0, 6, 7, 8, 9, 5),
        intArrayOf(2, 3, 4, 0, 1, 7, 8, 9, 5, 6),
        intArrayOf(3, 4, 0, 1, 2, 8, 9, 5, 6, 7),
        intArrayOf(4, 0, 1, 2, 3, 9, 5, 6, 7, 8),
        intArrayOf(5, 9, 8, 7, 6, 0, 4, 3, 2, 1),
        intArrayOf(6, 5, 9, 8, 7, 1, 0, 4, 3, 2),
        intArrayOf(7, 6, 5, 9, 8, 2, 1, 0, 4, 3),
        intArrayOf(8, 7, 6, 5, 9, 3, 2, 1, 0, 4),
        intArrayOf(9, 8, 7, 6, 5, 4, 3, 2, 1, 0)
    )

    val p = arrayOf(
        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
        intArrayOf(1, 5, 7, 6, 2, 8, 3, 0, 9, 4),
        intArrayOf(5, 8, 0, 3, 7, 9, 6, 1, 4, 2),
        intArrayOf(8, 9, 1, 6, 0, 4, 3, 5, 2, 7),
        intArrayOf(9, 4, 5, 3, 1, 2, 6, 8, 7, 0),
        intArrayOf(4, 2, 8, 6, 5, 7, 3, 9, 0, 1),
        intArrayOf(2, 7, 9, 3, 8, 0, 6, 4, 1, 5),
        intArrayOf(7, 0, 4, 6, 9, 1, 3, 2, 5, 8)
    )

    val inv = intArrayOf(0, 4, 3, 2, 1, 5, 6, 7, 8, 9)

    var c = 0
    val digits = aadhaar.map { it - '0' }.toIntArray()

    for (i in digits.indices) {
        c = d[c][p[(i + 1) % 8][digits[digits.size - i - 1]]]
    }

    return c == 0
}

// Format Aadhaar number for display
private fun formatAadhaarNumber(aadhaar: String): String {
    if (aadhaar.length != 12) return aadhaar

    return "${aadhaar.substring(0, 4)} ${aadhaar.substring(4, 8)} ${aadhaar.substring(8, 12)}"
}

// Helper function to convert ImageProxy to Bitmap
private fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.capacity())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

// Function to detect face on Aadhaar card
private suspend fun detectFaceOnAadhaarCard(
    bitmap: Bitmap,
    faceDetector: FaceDetector
): FacialData? = withContext(Dispatchers.IO) {
    try {
        val image = InputImage.fromBitmap(bitmap, 0)
        val faces = suspendCoroutine<List<Face>> { continuation ->
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    continuation.resume(faces)
                }
                .addOnFailureListener { e ->
                    Log.e("FaceDetection", "Face detection failed", e)
                    continuation.resumeWithException(e)
                }
        }

        if (faces.isEmpty()) {
            return@withContext null
        }

        // Get the largest face in case multiple are detected
        val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            ?: return@withContext null

        extractFacialData(face)
    } catch (e: Exception) {
        Log.e("FaceDetection", "Error in face detection", e)
        null
    }
}

// Extract facial data from Face object
private fun extractFacialData(face: Face): FacialData {
    val facialLandmarks = mutableMapOf<Int, PointF>()
    val facialContours = mutableMapOf<Int, List<PointF>>()

    // Extract facial landmarks
    for (landmark in face.allLandmarks) {
        facialLandmarks[landmark.landmarkType] = landmark.position
    }

    // Extract facial contours
    for (contour in face.allContours) {
        facialContours[contour.faceContourType] = contour.points
    }

    return FacialData(
        landmarks = facialLandmarks,
        contours = facialContours,
        boundingBox = face.boundingBox,
        headEulerAngleX = face.headEulerAngleX,
        headEulerAngleY = face.headEulerAngleY,
        headEulerAngleZ = face.headEulerAngleZ,
        leftEyeOpenProbability = face.leftEyeOpenProbability,
        rightEyeOpenProbability = face.rightEyeOpenProbability,
        smilingProbability = face.smilingProbability
    )
}

// Enhanced face comparison algorithm
private fun compareFaces(aadhaarFace: FacialData, selfieFace: FacialData): Double {
    try {
        var totalScore = 0.0
        var totalWeights = 0.0

        // 1. Compare facial landmarks
        val landmarkScore = compareFacialLandmarks(aadhaarFace.landmarks, selfieFace.landmarks)
        totalScore += landmarkScore * 0.35
        totalWeights += 0.35

        // 2. Compare facial contours
        val contourScore = compareFacialContours(aadhaarFace.contours, selfieFace.contours)
        totalScore += contourScore * 0.25
        totalWeights += 0.25

        // 3. Compare face proportions
        val proportionScore = compareFaceProportions(aadhaarFace, selfieFace)
        totalScore += proportionScore * 0.25
        totalWeights += 0.25

        // 4. Compare head orientation
        val orientationScore = compareHeadOrientation(aadhaarFace, selfieFace)
        totalScore += orientationScore * 0.15
        totalWeights += 0.15

        // Calculate normalized score
        return if (totalWeights > 0) totalScore / totalWeights else 0.0
    } catch (e: Exception) {
        Log.e("FaceComparison", "Error comparing faces", e)
        return 0.0
    }
}

// Compare facial landmarks between two faces
private fun compareFacialLandmarks(
    landmarks1: Map<Int, PointF>,
    landmarks2: Map<Int, PointF>
): Double {
    // Key landmarks to compare
    val keyLandmarks = listOf(
        FaceLandmark.LEFT_EYE,
        FaceLandmark.RIGHT_EYE,
        FaceLandmark.NOSE_BASE,
        FaceLandmark.MOUTH_LEFT,
        FaceLandmark.MOUTH_RIGHT,
        FaceLandmark.LEFT_EAR,
        FaceLandmark.RIGHT_EAR,
        FaceLandmark.LEFT_CHEEK,
        FaceLandmark.RIGHT_CHEEK
    )

    // Check if we have enough landmarks
    val commonLandmarks = keyLandmarks.filter {
        landmarks1.containsKey(it) && landmarks2.containsKey(it)
    }

    if (commonLandmarks.size < 5) {
        return 0.0
    }

    // Normalize coordinates to handle different image sizes
    val normalized1 = normalizeLandmarks(landmarks1.filterKeys { it in commonLandmarks })
    val normalized2 = normalizeLandmarks(landmarks2.filterKeys { it in commonLandmarks })

    // Calculate landmark distances
    var totalDistance = 0.0
    for (landmark in commonLandmarks) {
        val point1 = normalized1[landmark] ?: continue
        val point2 = normalized2[landmark] ?: continue

        // Euclidean distance between normalized points
        val dist = sqrt(
            (point2.x - point1.x).pow(2) + (point2.y - point1.y).pow(2)
        )
        totalDistance += dist
    }

    // Convert to similarity score (0-1, where 1 is perfect match)
    val avgDistance = totalDistance / commonLandmarks.size
    val similarityScore = max(0.0, 1.0 - (avgDistance * 5)) // Scale factor of 5 to make it more sensitive

    return similarityScore
}

// Normalize landmark coordinates relative to eye distance
private fun normalizeLandmarks(landmarks: Map<Int, PointF>): Map<Int, PointF> {
    val leftEye = landmarks[FaceLandmark.LEFT_EYE]
    val rightEye = landmarks[FaceLandmark.RIGHT_EYE]

    // If we don't have both eyes, return the original landmarks
    if (leftEye == null || rightEye == null) {
        return landmarks
    }

    // Calculate reference distance (distance between eyes)
    val referenceDistance = sqrt(
        (rightEye.x - leftEye.x).pow(2) + (rightEye.y - leftEye.y).pow(2)
    )

    if (referenceDistance <= 0) {
        return landmarks
    }

    // Calculate reference center (between eyes)
    val centerX = (leftEye.x + rightEye.x) / 2
    val centerY = (leftEye.y + rightEye.y) / 2

    // Normalize all landmarks
    return landmarks.mapValues { (_, point) ->
        PointF(
            (point.x - centerX) / referenceDistance,
            (point.y - centerY) / referenceDistance
        )
    }
}

// Compare facial contours between two faces
private fun compareFacialContours(
    contours1: Map<Int, List<PointF>>,
    contours2: Map<Int, List<PointF>>
): Double {
    // Key contours to compare
    val keyContours = listOf(
        FaceContour.FACE,
        FaceContour.LEFT_EYEBROW_TOP,
        FaceContour.LEFT_EYEBROW_BOTTOM,
        FaceContour.RIGHT_EYEBROW_TOP,
        FaceContour.RIGHT_EYEBROW_BOTTOM,
        FaceContour.LEFT_EYE,
        FaceContour.RIGHT_EYE,
        FaceContour.UPPER_LIP_TOP,
        FaceContour.UPPER_LIP_BOTTOM,
        FaceContour.LOWER_LIP_TOP,
        FaceContour.LOWER_LIP_BOTTOM,
        FaceContour.NOSE_BRIDGE,
        FaceContour.NOSE_BOTTOM
    )

    // Check which contours we have in both faces
    val commonContours = keyContours.filter {
        contours1.containsKey(it) && contours2.containsKey(it)
    }

    if (commonContours.isEmpty()) {
        return 0.0
    }

    // Normalize contours
    val normalized1 = normalizeContours(contours1)
    val normalized2 = normalizeContours(contours2)

    var totalSimilarity = 0.0
    for (contour in commonContours) {
        val points1 = normalized1[contour] ?: continue
        val points2 = normalized2[contour] ?: continue

        // Handle different numbers of points in contours
        val similarity = comparePointSets(points1, points2)
        totalSimilarity += similarity
    }

    return totalSimilarity / commonContours.size
}

// Normalize contour coordinates
private fun normalizeContours(contours: Map<Int, List<PointF>>): Map<Int, List<PointF>> {
    // Get face contour for normalization
    val faceContour = contours[FaceContour.FACE] ?: return contours

    if (faceContour.isEmpty()) {
        return contours
    }

    // Calculate bounding box of face contour
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE

    for (point in faceContour) {
        minX = min(minX, point.x)
        minY = min(minY, point.y)
        maxX = max(maxX, point.x)
        maxY = max(maxY, point.y)
    }

    val width = maxX - minX
    val height = maxY - minY

    if (width <= 0 || height <= 0) {
        return contours
    }
    val centerX = minX + width / 2

    val centerY = minY + height / 2
    val size = max(width, height)

    // Normalize all contours
    return contours.mapValues { (_, points) ->
        points.map { point ->
            PointF(
                (point.x - centerX) / size,
                (point.y - centerY) / size
            )
        }
    }
}

// Compare two sets of points (contours)
private fun comparePointSets(points1: List<PointF>, points2: List<PointF>): Double {
    if (points1.isEmpty() || points2.isEmpty()) {
        return 0.0
    }

    // Resample to the same number of points if needed
    val sampleSize = min(30, min(points1.size, points2.size))
    val sampled1 = resamplePoints(points1, sampleSize)
    val sampled2 = resamplePoints(points2, sampleSize)

    var totalDistance = 0.0
    for (i in sampled1.indices) {
        val p1 = sampled1[i]
        val p2 = sampled2[i]
        val dist = sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2))
        totalDistance += dist
    }

    val avgDistance = totalDistance / sampleSize
    // Convert to similarity score (0-1)
    return max(0.0, 1.0 - (avgDistance * 5))
}

// Resample a list of points to have a specific number of evenly distributed points
private fun resamplePoints(points: List<PointF>, count: Int): List<PointF> {
    if (points.size <= 1 || count <= 1) return points
    if (points.size == count) return points

    val result = ArrayList<PointF>(count)

    // Calculate total path length
    var totalLength = 0f
    val segmentLengths = ArrayList<Float>(points.size - 1)

    for (i in 0 until points.size - 1) {
        val length = distance(points[i], points[i + 1])
        segmentLengths.add(length)
        totalLength += length
    }

    // Calculate step size
    val stepSize = totalLength / (count - 1)

    // First point is always included
    result.add(points[0])

    // Resample intermediate points
    var currentLength = 0f
    var currentSegment = 0

    for (i in 1 until count - 1) {
        val targetLength = i * stepSize

        // Find the segment containing the target point
        while (currentLength + segmentLengths[currentSegment] < targetLength &&
            currentSegment < segmentLengths.size - 1) {
            currentLength += segmentLengths[currentSegment]
            currentSegment++
        }

        // Interpolate point within segment
        val segmentPos = (targetLength - currentLength) / segmentLengths[currentSegment]
        val p1 = points[currentSegment]
        val p2 = points[currentSegment + 1]

        result.add(PointF(
            p1.x + segmentPos * (p2.x - p1.x),
            p1.y + segmentPos * (p2.y - p1.y)
        ))
    }

    // Last point is always included
    result.add(points.last())

    return result
}

// Compare face proportions
private fun compareFaceProportions(face1: FacialData, face2: FacialData): Double {
    val proportions1 = calculateFaceProportions(face1)
    val proportions2 = calculateFaceProportions(face2)

    if (proportions1.isEmpty() || proportions2.isEmpty()) {
        return 0.0
    }

    var totalSimilarity = 0.0
    var count = 0

    // Compare common proportions
    for ((key, value1) in proportions1) {
        val value2 = proportions2[key] ?: continue

        // Calculate similarity as ratio (smaller/larger)
        val ratio = if (value1 > value2) {
            value2 / value1
        } else {
            value1 / value2
        }

        totalSimilarity += ratio
        count++
    }

    return if (count > 0) totalSimilarity / count else 0.0
}

// Calculate face proportions
private fun calculateFaceProportions(face: FacialData): Map<String, Float> {
    val proportions = mutableMapOf<String, Float>()
    val landmarks = face.landmarks

    // Key points
    val leftEye = landmarks[FaceLandmark.LEFT_EYE]
    val rightEye = landmarks[FaceLandmark.RIGHT_EYE]
    val nose = landmarks[FaceLandmark.NOSE_BASE]
    val mouthLeft = landmarks[FaceLandmark.MOUTH_LEFT]
    val mouthRight = landmarks[FaceLandmark.MOUTH_RIGHT]
    val leftEar = landmarks[FaceLandmark.LEFT_EAR]
    val rightEar = landmarks[FaceLandmark.RIGHT_EAR]

    // Calculate eye distance
    if (leftEye != null && rightEye != null) {
        val eyeDistance = distance(leftEye, rightEye)

        // Eye to nose ratio
        if (nose != null) {
            val leftEyeToNose = distance(leftEye, nose)
            val rightEyeToNose = distance(rightEye, nose)
            proportions["leftEyeToNoseRatio"] = leftEyeToNose / eyeDistance
            proportions["rightEyeToNoseRatio"] = rightEyeToNose / eyeDistance
        }

        // Mouth width ratio
        if (mouthLeft != null && mouthRight != null) {
            val mouthWidth = distance(mouthLeft, mouthRight)
            proportions["mouthWidthRatio"] = mouthWidth / eyeDistance
        }

        // Face width ratio
        if (leftEar != null && rightEar != null) {
            val faceWidth = distance(leftEar, rightEar)
            proportions["faceWidthRatio"] = faceWidth / eyeDistance
        }

        // Vertical proportions
        if (nose != null && mouthLeft != null && mouthRight != null) {
            val mouthCenter = PointF(
                (mouthLeft.x + mouthRight.x) / 2,
                (mouthLeft.y + mouthRight.y) / 2
            )
            val eyeCenter = PointF(
                (leftEye.x + rightEye.x) / 2,
                (leftEye.y + rightEye.y) / 2
            )

            val eyeToNose = distance(eyeCenter, nose)
            val noseToMouth = distance(nose, mouthCenter)

            proportions["eyeToNoseRatio"] = eyeToNose / eyeDistance
            proportions["noseToMouthRatio"] = noseToMouth / eyeDistance
            proportions["verticalRatio"] = eyeToNose / noseToMouth
        }
    }

    return proportions
}

// Compare head orientation
private fun compareHeadOrientation(face1: FacialData, face2: FacialData): Double {
    // Calculate angular difference (accounting for front/selfie camera differences)
    // For Y angle, we might need to invert one of the values due to camera flipping
    val xDiff = abs(face1.headEulerAngleX - face2.headEulerAngleX)
    val yDiff = min(
        abs(face1.headEulerAngleY - face2.headEulerAngleY),
        abs(face1.headEulerAngleY + face2.headEulerAngleY) // Account for possible mirroring
    )
    val zDiff = abs(face1.headEulerAngleZ - face2.headEulerAngleZ)

    // Convert differences to similarities (0-1)
    // Lower angle difference = higher similarity
    val xSim = 1.0 - min(1.0, xDiff / 30.0) // 30 degrees tolerance
    val ySim = 1.0 - min(1.0, yDiff / 30.0)
    val zSim = 1.0 - min(1.0, zDiff / 30.0)

    // Weight the angles (Y angle is most important)
    return (xSim * 0.3) + (ySim * 0.5) + (zSim * 0.2)
}

// Handle liveness detection challenges
private fun handleLivenessChallenge(
    currentFace: Face,
    previousFace: Face?,
    challenge: LivenessChallenge?,
    startTime: Long,
    onComplete: (Boolean) -> Unit
) {
    if (challenge == null || previousFace == null) return

    // Ensure we don't complete too quickly (minimum 1.5 seconds for challenge)
    val elapsedTime = System.currentTimeMillis() - startTime
    if (elapsedTime < 1500) return

    when (challenge) {
        LivenessChallenge.BLINK -> {
            val leftEyeOpen = currentFace.leftEyeOpenProbability ?: 1.0f
            val rightEyeOpen = currentFace.rightEyeOpenProbability ?: 1.0f
            val prevLeftEyeOpen = previousFace.leftEyeOpenProbability ?: 0.0f
            val prevRightEyeOpen = previousFace.rightEyeOpenProbability ?: 0.0f

            // Detect blink sequence (open -> closed -> open)
            val eyesClosed = (leftEyeOpen < 0.3f || rightEyeOpen < 0.3f)
            val eyesWereOpen = (prevLeftEyeOpen > 0.7f || prevRightEyeOpen > 0.7f)

            if (eyesClosed && eyesWereOpen) {
                onComplete(true)
            }
        }

        LivenessChallenge.SMILE -> {
            val smileProb = currentFace.smilingProbability ?: 0.0f

            // Detect smile
            if (smileProb > 0.8f) {
                onComplete(true)
            }
        }

        LivenessChallenge.TURN_LEFT -> {
            val yAngle = currentFace.headEulerAngleY

            // Detect turning left (positive Y angle for front camera)
            if (yAngle > 25f) {
                onComplete(true)
            }
        }

        LivenessChallenge.TURN_RIGHT -> {
            val yAngle = currentFace.headEulerAngleY

            // Detect turning right (negative Y angle for front camera)
            if (yAngle < -25f) {
                onComplete(true)
            }
        }

        LivenessChallenge.NOD -> {
            val xAngle = currentFace.headEulerAngleX
            val prevXAngle = previousFace.headEulerAngleX

            // Detect nodding down
            if (xAngle > 20f && prevXAngle < 10f) {
                onComplete(true)
            }
        }
    }
}

// Get step number based on detection stage
private fun getStepNumber(stage: FaceDetectionStage): Int {
    return when (stage) {
        FaceDetectionStage.AADHAAR_CARD -> 1
        FaceDetectionStage.AADHAAR_FACE_DETECTION -> 2
        FaceDetectionStage.SELFIE -> 3
        FaceDetectionStage.LIVENESS_CHECK -> 4
        FaceDetectionStage.FACE_COMPARISON, FaceDetectionStage.COMPLETE -> 5
    }
}

// Get instruction text for current stage
private fun getInstructionText(
    stage: FaceDetectionStage,
    livenessChallenge: LivenessChallenge?
): String {
    return when(stage) {
        FaceDetectionStage.AADHAAR_CARD ->
            "Scan your Aadhaar card to automatically capture your 12-digit Aadhaar number"
        FaceDetectionStage.AADHAAR_FACE_DETECTION ->
            "Detecting face on your Aadhaar card..."
        FaceDetectionStage.SELFIE ->
            "Now take a clear selfie for face verification with your Aadhaar"
        FaceDetectionStage.LIVENESS_CHECK ->
            getLivenessChallengeInstructionText(livenessChallenge)
        FaceDetectionStage.FACE_COMPARISON ->
            "Comparing your face with the Aadhaar card..."
        FaceDetectionStage.COMPLETE ->
            "Verification complete"
    }
}

// Get instruction text for liveness challenge
private fun getLivenessChallengeInstructionText(challenge: LivenessChallenge?): String {
    return when (challenge) {
        LivenessChallenge.BLINK -> "Please blink your eyes"
        LivenessChallenge.SMILE -> "Please smile for the camera"
        LivenessChallenge.TURN_LEFT -> "Please turn your head slightly to the left"
        LivenessChallenge.TURN_RIGHT -> "Please turn your head slightly to the right"
        LivenessChallenge.NOD -> "Please nod your head down slightly"
        null -> "Preparing liveness check..."
    }
}

// Reset the verification process
private fun resetVerificationProcess(onReset: () -> Unit) {
    onReset()
}

// Helper function to calculate Euclidean distance between two points
private fun distance(p1: PointF, p2: PointF): Float {
    return sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2))
}

@Composable
fun LoadingDialog(message: String) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Processing") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp).padding(bottom = 16.dp)
                )
                Text(message)
            }
        },
        confirmButton = { }
    )
}


