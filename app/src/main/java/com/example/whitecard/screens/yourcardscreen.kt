package com.example.whitecard.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.whitecard.elements.BottomNavigation
import com.example.whitecard.elements.WhiteCardTopBar
import com.example.whitecard.utils.MLKitCardProcessor
import com.example.whitecard.viewmodels.mainscreenviewmlodel
import com.example.whitecard.firebase.FirestoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// Data class for CardItem
data class CardItem(
    val id: Long,
    val imageUri: String,
    val name: String,
    val details: String,
    val isVerified: Boolean = false,
    val firebaseImageUrl: String = "" // Add Firebase URL field
)

@Composable
fun YourCardsScreen(navController: NavController) {
    // Max number of card holders
    val maxCards = 10

    // State for Firebase operations
    var isSavingToFirebase by remember { mutableStateOf(false) }
    var firebaseError by remember { mutableStateOf<String?>(null) }

    // State for showing loading indicator
    var isProcessing by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("Processing image...") }

    // Cards state - dynamic list that can grow up to maxCards
    var cards by remember { mutableStateOf(mutableListOf<CardItem>()) }

    // Index of selected card holder or -1 for none
    var selectedCardIndex by remember { mutableStateOf(-1) }

    // State for showing add/edit dialog
    var showCardDialog by remember { mutableStateOf(false) }

    // State for showing card details dialog
    var showCardDetails by remember { mutableStateOf(false) }

    // State for showing edit card name dialog
    var showEditNameDialog by remember { mutableStateOf(false) }

    // Card being edited or null for add new
    var currentEditingIndex by remember { mutableStateOf<Int?>(null) }

    // For camera/gallery image selection
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val mlKitCardProcessor = remember { MLKitCardProcessor(context) }

    // Load cards from Firebase when screen starts
    LaunchedEffect(Unit) {
        loadCardsFromFirebase(
            coroutineScope = coroutineScope,
            onCardsLoaded = { firebaseCards ->
                cards = firebaseCards.toMutableList()
            },
            onError = { error ->
                firebaseError = error
            }
        )
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && imageUri != null) {
            processImageWithFirebase(
                uri = imageUri!!,
                context = context,
                mlKitCardProcessor = mlKitCardProcessor,
                coroutineScope = coroutineScope,
                currentEditingIndex = currentEditingIndex,
                cards = cards,
                onCardsUpdate = { newCards -> cards = newCards },
                onProcessingStart = {
                    isProcessing = true
                    processingMessage = "Processing your card image..."
                },
                onProcessingEnd = {
                    isProcessing = false
                    showCardDialog = false
                    currentEditingIndex = null
                },
                onFirebaseStart = {
                    isSavingToFirebase = true
                    processingMessage = "Saving to cloud..."
                },
                onFirebaseEnd = {
                    isSavingToFirebase = false
                },
                onError = { error ->
                    firebaseError = error
                }
            )
        }
    }

    // Permission launcher for camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val tempImageFile = File(context.getExternalFilesDir(null), "card_image_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                tempImageFile
            )
            imageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            processImageWithFirebase(
                uri = it,
                context = context,
                mlKitCardProcessor = mlKitCardProcessor,
                coroutineScope = coroutineScope,
                currentEditingIndex = currentEditingIndex,
                cards = cards,
                onCardsUpdate = { newCards -> cards = newCards },
                onProcessingStart = {
                    isProcessing = true
                    processingMessage = "Processing your card image..."
                },
                onProcessingEnd = {
                    isProcessing = false
                    showCardDialog = false
                    currentEditingIndex = null
                },
                onFirebaseStart = {
                    isSavingToFirebase = true
                    processingMessage = "Saving to cloud..."
                },
                onFirebaseEnd = {
                    isSavingToFirebase = false
                },
                onError = { error ->
                    firebaseError = error
                }
            )
        }
    }

    Scaffold(
        topBar = {
            WhiteCardTopBar(navController = navController, viewModel = mainscreenviewmlodel())
        },
        floatingActionButton = {
            if (cards.size < maxCards) {
                FloatingActionButton(
                    onClick = {
                        currentEditingIndex = null
                        showCardDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Card")
                }
            }
        },
        bottomBar = {
            BottomNavigation(navController = navController)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF000000),
                            Color(0xFF464646)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Header section with sync status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Your Cards",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // Sync status indicator
                        if (isSavingToFirebase) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Syncing...",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${cards.size}/$maxCards",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Firebase error display
                firebaseError?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { firebaseError = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Cards list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    if (cards.isEmpty()) {
                        item {
                            EmptyStateCard {
                                showCardDialog = true
                            }
                        }
                    } else {
                        itemsIndexed(cards) { index, card ->
                            EnhancedCard(
                                card = card,
                                isSelected = selectedCardIndex == index,
                                onClick = {
                                    selectedCardIndex = if (selectedCardIndex == index) -1 else index
                                    if (selectedCardIndex != -1) {
                                        showCardDetails = true
                                    }
                                },
                                onEdit = {
                                    currentEditingIndex = index
                                    showCardDialog = true
                                },
                                onDelete = {
                                    deleteCardFromFirebase(
                                        card = card,
                                        coroutineScope = coroutineScope,
                                        onSuccess = {
                                            cards = cards.toMutableList().also { list ->
                                                list.removeAt(index)
                                            }
                                            if (selectedCardIndex == index) {
                                                selectedCardIndex = -1
                                            } else if (selectedCardIndex > index) {
                                                selectedCardIndex--
                                            }
                                        },
                                        onError = { error ->
                                            firebaseError = error
                                        }
                                    )
                                },
                                onEditName = {
                                    currentEditingIndex = index
                                    showEditNameDialog = true
                                }
                            )
                        }

                        if (cards.size < maxCards) {
                            item {
                                EmptyCardHolder {
                                    currentEditingIndex = null
                                    showCardDialog = true
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialogs
        if (showCardDialog) {
            CardActionDialog(
                card = currentEditingIndex?.let { cards[it] },
                onDismiss = {
                    showCardDialog = false
                    currentEditingIndex = null
                },
                onCameraCapture = {
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                            val tempImageFile = File(context.getExternalFilesDir(null), "card_image_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                tempImageFile
                            )
                            imageUri = uri
                            cameraLauncher.launch(uri)
                        }
                        else -> {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                },
                onGalleryPick = {
                    galleryLauncher.launch("image/*")
                }
            )
        }

        if (showCardDetails && selectedCardIndex != -1 && selectedCardIndex < cards.size) {
            CardDetailsDialog(
                card = cards[selectedCardIndex],
                onDismiss = {
                    showCardDetails = false
                    selectedCardIndex = -1
                },
                onEdit = {
                    showCardDetails = false
                    currentEditingIndex = selectedCardIndex
                    showCardDialog = true
                },
                onEditName = {
                    showCardDetails = false
                    currentEditingIndex = selectedCardIndex
                    showEditNameDialog = true
                }
            )
        }

        if (showEditNameDialog && currentEditingIndex != null) {
            EditCardNameDialog(
                currentName = cards[currentEditingIndex!!].name,
                onDismiss = {
                    showEditNameDialog = false

                    showEditNameDialog = false
                    currentEditingIndex = null
                },
                onSave = { newName ->
                    val index = currentEditingIndex!!
                    val updatedCard = cards[index].copy(name = newName)

                    // Update Firebase
                    updateCardInFirebase(
                        card = updatedCard,
                        coroutineScope = coroutineScope,
                        onSuccess = {
                            cards = cards.toMutableList().also { list ->
                                list[index] = updatedCard
                            }
                        },
                        onError = { error ->
                            firebaseError = error
                        }
                    )

                    showEditNameDialog = false
                    currentEditingIndex = null
                }
            )
        }

        // Processing overlay
        if (isProcessing || isSavingToFirebase) {
            ProcessingOverlay(message = processingMessage)
        }
    }
}

@Composable
fun EnhancedCard(
    card: CardItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEditName: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = tween(300)
    )

    val animatedElevation by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 4.dp,
        animationSpec = tween(300)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                transformOrigin = TransformOrigin.Center
            }
            .shadow(animatedElevation, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card image
            Card(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Gray.copy(alpha = 0.1f)
                )
            ) {
                if (card.imageUri.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(card.imageUri),
                        contentDescription = "Card Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CreditCard,
                            contentDescription = "Card Placeholder",
                            modifier = Modifier.size(40.dp),
                            tint = Color.Gray
                        )
                    }
                }
            }

            // Card details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = card.name.ifEmpty { "Unnamed Card" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (card.isVerified) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = card.details.ifEmpty { "No details extracted" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onEditName,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Edit Image",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            Color.White.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Card",
                modifier = Modifier.size(48.dp),
                tint = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Add Your First Card",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Tap to capture or upload a card image",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun EmptyCardHolder(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.03f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.1f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Card",
                    modifier = Modifier.size(24.dp),
                    tint = Color.White.copy(alpha = 0.4f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Add New Card",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun CardActionDialog(
    card: CardItem?,
    onDismiss: () -> Unit,
    onCameraCapture: () -> Unit,
    onGalleryPick: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (card != null) "Update Card Image" else "Add New Card",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Camera option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCameraCapture() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {

                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Gallery option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGalleryPick() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Choose from Gallery",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Select existing photo",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    }


@Composable
fun CardDetailsDialog(
    card: CardItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onEditName: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Card Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card image
                if (card.imageUri.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(card.imageUri),
                            contentDescription = "Card Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Card name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Card Name",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = card.name.ifEmpty { "Unnamed Card" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(onClick = onEditName) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Name")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card details
                Text(
                    text = "Extracted Details",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = card.details.ifEmpty { "No details extracted from this card" },
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Image")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun EditCardNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Edit Card Name",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Card Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { onSave(name.trim()) },
                        modifier = Modifier.weight(1f),
                        enabled = name.trim().isNotEmpty()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessingOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Image processing function (simplified version)
private fun processImage(
    uri: Uri,
    context: Context,
    mlKitCardProcessor: MLKitCardProcessor,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    currentEditingIndex: Int?,
    cards: MutableList<CardItem>,
    onCardsUpdate: (cards: MutableList<CardItem>) -> Unit,
    onProcessingStart: () -> Unit,
    onProcessingEnd: () -> Unit,
    onError: (error: String) -> Unit
) {
    coroutineScope.launch {
        try {
            onProcessingStart()

            // Process image with ML Kit
            val extractedText: String = withContext(Dispatchers.IO) {
                // Replace with the correct method name from your MLKitCardProcessor
                mlKitCardProcessor.processCard(uri) // or whatever your method is called
            }

            // Create or update card
            val cardItem: CardItem = if (currentEditingIndex != null) {
                cards[currentEditingIndex].copy(
                    imageUri = uri.toString(),
                    details = extractedText
                )
            } else {
                CardItem(
                    id = System.currentTimeMillis(),
                    imageUri = uri.toString(),
                    name = "Card ${cards.size + 1}",
                    details = extractedText,
                    isVerified = true
                )
            }

            // Update cards list
            val updatedCards: MutableList<CardItem> = cards.toMutableList()
            if (currentEditingIndex != null) {
                updatedCards[currentEditingIndex] = cardItem
            } else {
                updatedCards.add(cardItem)
            }
            onCardsUpdate(updatedCards)

        } catch (e: Exception) {
            onError("Failed to process image: ${e.message}")
        } finally {
            onProcessingEnd()
        }
    }
}

// Firebase helper functions
private fun loadCardsFromFirebase(
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onCardsLoaded: (List<CardItem>) -> Unit,
    onError: (String) -> Unit
) {
    coroutineScope.launch {
        try {
            val firestoreManager = FirestoreManager
            // Implement your Firebase loading logic here
            // This is a placeholder - replace with actual Firebase implementation
            onCardsLoaded(emptyList())
        } catch (e: Exception) {
            onError("Failed to load cards: ${e.message}")
        }
    }
}

private fun processImageWithFirebase(
    uri: Uri,
    context: Context,
    mlKitCardProcessor: MLKitCardProcessor,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    currentEditingIndex: Int?,
    cards: MutableList<CardItem>,
    onCardsUpdate: (cards: MutableList<CardItem>) -> Unit,
    onProcessingStart: () -> Unit,
    onProcessingEnd: () -> Unit,
    onFirebaseStart: () -> Unit,
    onFirebaseEnd: () -> Unit,
    onError: (error: String) -> Unit
) {
    coroutineScope.launch {
        try {
            onProcessingStart()

            // Process image with ML Kit
            val extractedText: String = withContext(Dispatchers.IO) {
                // Replace with the correct method name from your MLKitCardProcessor
                mlKitCardProcessor.processCard(uri) // or whatever your method is called
            }

            onFirebaseStart()

            // Upload to Firebase and create card
            val cardItem: CardItem = if (currentEditingIndex != null) {
                cards[currentEditingIndex].copy(
                    imageUri = uri.toString(),
                    details = extractedText
                )
            } else {
                CardItem(
                    id = System.currentTimeMillis(),
                    imageUri = uri.toString(),
                    name = "Card ${cards.size + 1}",
                    details = extractedText,
                    isVerified = true
                )
            }

            // Save to Firebase
            saveCardToFirebase(
                card = cardItem,
                onSuccess = {
                    val updatedCards: MutableList<CardItem> = cards.toMutableList()
                    if (currentEditingIndex != null) {
                        updatedCards[currentEditingIndex] = cardItem
                    } else {
                        updatedCards.add(cardItem)
                    }
                    onCardsUpdate(updatedCards)
                },
                onError = onError
            )

        } catch (e: Exception) {
            onError("Failed to process image: ${e.message}")
        } finally {
            onFirebaseEnd()
            onProcessingEnd()
        }
    }
}

private fun MLKitCardProcessor.processCard(uri: Uri): String {
    // Implement ML Kit processing logic here
    // This is a placeholder - replace with actual ML Kit implementation
    return "Placeholder text"
    // Replace with the actual extracted text

}

private fun saveCardToFirebase(
    card: CardItem,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    // Implement Firebase save logic here
    // This is a placeholder - replace with actual Firebase implementation
    onSuccess()
}

private fun updateCardInFirebase(
    card: CardItem,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    coroutineScope.launch {
        try {
            // Implement Firebase update logic here
            onSuccess()
        } catch (e: Exception) {
            onError("Failed to update card: ${e.message}")
        }
    }
}

private fun deleteCardFromFirebase(
    card: CardItem,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    coroutineScope.launch {
        try {
            // Implement Firebase delete logic here
            onSuccess()
        } catch (e: Exception) {
            onError("Failed to delete card: ${e.message}")
        }
    }
}