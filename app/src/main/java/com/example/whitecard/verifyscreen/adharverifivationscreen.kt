package com.example.whitecard.verifyscreen


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.whitecard.qrutils.isValidAadhaar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AadhaarVerificationScreen(navController: NavController) {
    var aadhaarNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = 0.33f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Text(
            text = "Aadhaar Verification",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        OutlinedTextField(
            value = aadhaarNumber,
            onValueChange = {
                if (it.length <= 12 && it.all { char -> char.isDigit() }) {
                    aadhaarNumber = it
                    errorMessage = ""
                }
            },
            label = { Text("Aadhaar Number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                if (isValidAadhaar(aadhaarNumber)) {
                    isLoading = true

                    // Save aadhaar number to pass to next screen
                    navController.currentBackStackEntry?.savedStateHandle?.set("aadhaarNumber", aadhaarNumber)

                    navController.navigate("pan_verification")
                    isLoading = false
                } else {
                    errorMessage = "Please enter a valid 12-digit Aadhaar number"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && aadhaarNumber.length == 12
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Verify & Continue")
            }
        }
    }
}
