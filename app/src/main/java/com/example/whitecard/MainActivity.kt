package com.example.whitecard



import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.whitecard.ui.theme.WhitecardTheme

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue


import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.whitecard.screens.CardTemplatesScreen

import com.example.whitecard.screens.ForgotPasswordScreen

import com.example.whitecard.screens.LoginScreen
import com.example.whitecard.screens.MainScreen
import com.example.whitecard.screens.ProfileDetailsScreen
import com.example.whitecard.screens.QRScannerScreen
import com.example.whitecard.screens.SignupScreen
import com.example.whitecard.screens.SplashScreen
import com.example.whitecard.screens.UpdateCardScreen
import com.example.whitecard.screens.WelcomeScreen

import com.example.whitecard.screens.commingsoon

import com.example.whitecard.verifyscreen.AadhaarVerificationScreen
import com.example.whitecard.verifyscreen.LicenseVerificationScreen
import com.example.whitecard.verifyscreen.PanVerificationScreen
import com.google.android.play.core.integrity.n


import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)


        enableEdgeToEdge()


        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        setContent {
            WhitecardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }

                    if (showSplash) {
                        SplashScreen(onLoadingComplete = { showSplash = false })
                    } else {
                        AppNavigation(auth)

                    }

                }
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth) {
    val navController = rememberNavController()
    val startDestination = if (auth.currentUser != null) "main" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(navController = navController)
        }
        composable("signup") {
            SignupScreen(navController = navController)
        }
        composable("welcome") {
            WelcomeScreen(navController = navController)
        }
        composable("aadhaar_verification") {
            AadhaarVerificationScreen(navController = navController)
        }
        composable("pan_verification") {
            PanVerificationScreen(navController = navController)
        }
        composable("license_verification") {
            LicenseVerificationScreen(navController = navController)
        }
        composable("splash") {
            SplashScreen(onLoadingComplete = { navController.navigate("login") })
        }
        composable("forgot_password") {
            ForgotPasswordScreen(navController=navController)


        }

        composable("update_card") {
            UpdateCardScreen(navController)
        }
        composable("card_templates"){
            CardTemplatesScreen(navController = navController)

        }
        composable("order_card") {
            commingsoon(navController = navController)
        }
        composable("physical_card") {
            commingsoon(navController = navController)


        }



        composable("main") {
            MainScreen(navController = navController)
        }
        composable("scan_qr") {
            QRScannerScreen(navController = navController)
        }
        composable("profile_details/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            if (userId != null) {
                ProfileDetailsScreen(navController = navController, userId = userId)
            }
        }
    }
}