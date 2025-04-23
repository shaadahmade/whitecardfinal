package com.example.whitecard.screens
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardTemplatesScreen(navController: NavController) {
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF000000),
            Color(0xFF464646)
        )
    )

    // Define card templates
    val cardTemplates = remember {
        listOf(
            CardTemplate(
                id = "default",
                name = "Default Black",
                backgroundGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF000000),
                        Color(0xFF464646)
                    )
                ),
                primaryBackgroundColor = Color(0xFF000000), // First color of gradient
                textColor = Color.White,
                accentColor = Color(0xFF1A56E8)
            ),
            CardTemplate(
                id = "blue_gradient",
                name = "Blue Gradient",
                backgroundGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A56E8),
                        Color(0xFF0E2F7C)
                    )
                ),
                primaryBackgroundColor = Color(0xFF1A56E8), // First color of gradient
                textColor = Color.White,
                accentColor = Color(0xFFFFD700)
            ),
            CardTemplate(
                id = "purple_dark",
                name = "Purple Dark",
                backgroundGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF4B0082),
                        Color(0xFF8A2BE2)
                    )
                ),
                primaryBackgroundColor = Color(0xFF4B0082), // First color of gradient
                textColor = Color.White,
                accentColor = Color(0xFFE6E6FA)
            ),
            CardTemplate(
                id = "carbon_fiber",
                name = "Carbon Fiber",
                backgroundGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2C2C2C),
                        Color(0xFF1A1A1A)
                    )
                ),
                primaryBackgroundColor = Color(0xFF2C2C2C), // First color of gradient
                textColor = Color.White,
                accentColor = Color(0xFFFF4500)
            ),
            CardTemplate(
                id = "rose_gold",
                name = "Rose Gold",
                backgroundGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFB76E79),
                        Color(0xFFE8C5B6)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                primaryBackgroundColor = Color(0xFFB76E79), // First color of gradient
                textColor = Color(0xFF4A2932),
                accentColor = Color.White
            ),
            CardTemplate(
                id = "emerald",
                name = "Emerald",
                backgroundGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF046307),
                        Color(0xFF2E8B57)
                    )
                ),
                primaryBackgroundColor = Color(0xFF046307), // First color of gradient
                textColor = Color.White,
                accentColor = Color(0xFFFFD700)
            )
        )
    }

    // Current selected template
    var selectedTemplateId by remember { mutableStateOf(DataStoreManager.getSelectedTemplateId()) }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Card Templates",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Choose a template for your card",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Success message
                AnimatedVisibility(visible = showSuccessMessage) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Template applied successfully!",
                                color = Color.White
                            )
                        }
                    }

                    LaunchedEffect(showSuccessMessage) {
                        delay(3000)
                        showSuccessMessage = false
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Template grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(cardTemplates) { template ->
                        TemplateCard(
                            template = template,
                            isSelected = template.id == selectedTemplateId,
                            onTemplateSelected = { templateId ->
                                isLoading = true
                                // Save the selected template to DataStore
                                DataStoreManager.saveSelectedTemplateId(templateId)
                                selectedTemplateId = templateId
                                isLoading = false
                                showSuccessMessage = true
                            }
                        )
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
fun TemplateCard(
    template: CardTemplate,
    isSelected: Boolean,
    onTemplateSelected: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFF1A56E8) else Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onTemplateSelected(template.id) }
    ) {
        // Card preview
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(template.backgroundGradient)
        ) {
            // Card content preview
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Top row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Cardoo ID",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = template.textColor.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "JOHN DOE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = template.textColor
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(template.accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ID",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (template.accentColor == Color.White) template.primaryBackgroundColor else Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom text
                Text(
                    text = template.name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = template.textColor,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        // Selection indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp)
                    .background(Color(0xFF1A56E8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// Updated data class for card templates with primaryBackgroundColor
data class CardTemplate(
    val id: String,
    val name: String,
    val backgroundGradient: Brush,
    val primaryBackgroundColor: Color, // Added field to store first color of gradient
    val textColor: Color,
    val accentColor: Color
)

// DataStore Manager for saving template selection
object DataStoreManager {
    private var selectedTemplateId = "default"

    fun saveSelectedTemplateId(templateId: String) {
        selectedTemplateId = templateId
    }

    fun getSelectedTemplateId(): String {
        return selectedTemplateId
    }

    // In a real app, this would use Android's DataStore or SharedPreferences
    // to persist the selected template ID
}