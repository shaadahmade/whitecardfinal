package com.example.whitecard.verifyscreen


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.whitecard.qrutils.isValidPAN

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanVerificationScreen(navController: NavController) {
    var panNumber by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Get aadhaar number from previous screen
    val aadhaarNumber = navController.previousBackStackEntry?.savedStateHandle?.get<String>("aadhaarNumber") ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = 0.66f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Text(
            text = "PAN Card Verification",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                errorMessage = ""
            },
            label = { Text("Full Name (as on PAN)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage.isNotEmpty()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = panNumber,
            onValueChange = {
                if (it.length <= 10) {
                    panNumber = it.uppercase()
                    errorMessage = ""
                }
            },
            label = { Text("PAN Number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage.isNotEmpty()
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
                if (name.isNotEmpty() && isValidPAN(panNumber)) {
                    isLoading = true

                    // Save data to pass to next screen
                    navController.currentBackStackEntry?.savedStateHandle?.set("name", name)
                    navController.currentBackStackEntry?.savedStateHandle?.set("panNumber", panNumber)
                    navController.currentBackStackEntry?.savedStateHandle?.set("aadhaarNumber", aadhaarNumber)

                    navController.navigate("license_verification")
                    isLoading = false
                } else if (name.isEmpty()) {
                    errorMessage = "Please enter your name"
                } else {
                    errorMessage = "Please enter a valid 10-character PAN number"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && name.isNotEmpty() && panNumber.isNotEmpty()
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Next")
            }
        }
    }
}
