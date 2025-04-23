package com.example.whitecard.verifyscreen



import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseVerificationScreen(navController: NavController) {
    var licenseNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = 1f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Text(
            text = "Driving License Verification",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        OutlinedTextField(
            value = licenseNumber,
            onValueChange = {
                licenseNumber = it
                errorMessage = ""
            },
            label = { Text("License Number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage.isNotEmpty()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = expiryDate,
            onValueChange = {
                if (it.length <= 10) {
                    expiryDate = it
                }
            },
            label = { Text("Expiry Date (DD/MM/YYYY)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (licenseNumber.isNotEmpty() && expiryDate.isNotEmpty()) {
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
                                errorMessage = e.message ?: "Failed to save data"
                            }
                    } else {
                        isLoading = false
                        errorMessage = "User not authenticated"
                    }
                } else {
                    errorMessage = "Please fill in all fields"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && licenseNumber.isNotEmpty() && expiryDate.isNotEmpty()
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Complete")
            }
        }
    }
}