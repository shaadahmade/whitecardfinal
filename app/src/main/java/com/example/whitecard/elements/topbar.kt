package com.example.whitecard.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.whitecard.screens.backgroundGradient
import com.example.whitecard.viewmodels.mainscreenviewmlodel
import com.google.android.gms.auth.api.signin.GoogleSignIn


import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhiteCardTopBar(navController: NavController, viewModel: mainscreenviewmlodel) {
    // State for tracking current screen and drawer state
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "main"
    var showMenu by remember { mutableStateOf(false) }

    // Define the tabs
    val tabs = listOf(
        TopBarTab("main", "Home", Icons.Default.Home),
        TopBarTab("your_cards", "Your Cards", Icons.Default.CreditCard)
    )



    // Top bar with tabs
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shadowElevation = 0.dp
    ) {
        Column {
            Spacer(modifier = Modifier.height(22.dp))

            // Top section with logo and menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App logo
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Replace R.drawable.app_logo with your actual logo resource
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(shape = CircleShape)
                            .background(Color(0xFF1A56E8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = com.example.whitecard.R.drawable.icon),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(24.dp).clip(shape = CircleShape)
                        )


                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Cardoo",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                // Hamburger menu
                Box {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }

                    // Dropdown menu
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },

                        modifier = Modifier.background(Color(0xFF222222)).fillMaxWidth(0.5f) ,

                        shape = RoundedCornerShape(16)



                    ) {







                        MenuItem(
                            icon = Icons.Default.Logout,
                            title = "Logout",
                            tint = Color.Red,

                            onClick = {
                                viewModel.auth.signOut()
                                navController.navigate("welcome") {
                                    popUpTo("main") { inclusive = true }


                                    FirebaseAuth.getInstance().signOut()

                                    // Sign out from Google as well
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken("162214361636-8jbme7gg8kd0fp6ttlh7gem99fk2anfu.apps.googleusercontent.com")
                                        .requestEmail()
                                        .build()

                                    val googleSignInClient = GoogleSignIn.getClient(navController.context, gso)
                                    googleSignInClient.signOut()

                                }
                                showMenu = false
                            }
                        )
                    }
                }
            }

            // Navigation tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                tabs.forEach { tab ->
                    val isSelected = currentRoute == tab.route

                    NavigationTab(
                        tab = tab,
                        isSelected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                navController.navigate(tab.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Divider
            Divider(
                modifier = Modifier.padding(top = 8.dp),
                color = Color.Gray.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun NavigationTab(
    tab: TopBarTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) Color(0xFF1A56E8) else Color.White.copy(alpha = 0.6f)

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.title,
            tint = color,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = tab.title,
            color = color,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Indicator
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(200.dp)
                .background(
                    color = if (isSelected) Color(0xFF1A56E8) else Color.Transparent,
                    shape = MaterialTheme.shapes.small
                )
        )
    }
}

@Composable
fun MenuItem(
    icon: ImageVector,
    title: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF222222) ),
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    color = tint,
                    fontSize = 14.sp
                    ,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        onClick = onClick,
        colors = MenuDefaults.itemColors(
            textColor = Color.White,
            leadingIconColor = Color.White
        )
    )
}

// Data class to represent a tab item
data class TopBarTab(
    val route: String,
    val title: String,
    val icon: ImageVector
)
