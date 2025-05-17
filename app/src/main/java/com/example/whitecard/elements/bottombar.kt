package com.example.whitecard.elements

import android.R
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AddCircle

import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun BottomNavigation(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp).padding(bottom = 16.dp),


    ) {



        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)

                .fillMaxWidth(0.9f)
                .height(86.dp)
                .shadow(

                    elevation = 20.dp,
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                    ambientColor = Color.White,
                    spotColor =  Color(0xFF1A56E8)
                )
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))





        ) {

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {


                // First two buttons
                BottomNavItem(
                    icon = Icons.Default.Edit,
                    label = "Update",
                    onClick = { navController.navigate("update_card") }
                )

                BottomNavItem(
                    icon = Icons.Default.Person,
                    label = "Templates",
                    onClick = { navController.navigate("card_templates") }
                )

                // Empty space for the center button
                Spacer(modifier = Modifier.width(64.dp))

                // Last two buttons
                BottomNavItem(
                    icon = Icons.Default.AddCircle,
                    label = "pro",
                    onClick = { navController.navigate("order_card") }
                )

                BottomNavItem(
                    icon = Icons.Default.Star,
                    label = "Physical",
                    onClick = { navController.navigate("order_card") }
                )

            }
        }

        // Centered scan button floating above
        FloatingActionButton(
            onClick = { navController.navigate("scan_qr") },
            modifier = Modifier
                .size(88.dp)
                .align(Alignment.TopCenter)
                .offset(y = -16.dp,x=8.dp)
                .shadow(

                    elevation = 20.dp,
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                    ambientColor = Color.Blue,

                ),
            shape = CircleShape,
            containerColor = Color(0xFF1A56E8),
            contentColor = Color.White

        ) {
            Icon(
                imageVector = Icons.Filled.AccountBox,
                contentDescription = "Scan QR Code",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f)

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding( 5.dp,)



            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = if (isPressed) 4.dp.toPx() else 0f
                translationY = if (isPressed) 4.dp.toPx() else 0f




            }
    ) {

        IconButton(
            onClick = {
                isPressed = true
                onClick()
                // Reset after animation completes
                kotlinx.coroutines.GlobalScope.launch {
                    kotlinx.coroutines.delay(100)
                    isPressed = false
                }
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFFFFFFFF),
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = label,
            color = Color(0xFFFFFFFF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )



    }
}