package com.example.whitecard.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailsScreen(navController: NavController, userId: String) {
    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var showPermissionDialog by remember { mutableStateOf(true) }
    var permissionGranted by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()

    // Function to load user data if permission is granted
    fun loadUserData() {
        isLoading = true
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                isLoading = false
                if (document != null && document.exists()) {
                    userData = document.data
                } else {
                    errorMessage = "User not found"
                }
            }
            .addOnFailureListener { e ->
                isLoading = false
                errorMessage = e.message ?: "Error loading user data"
            }
    }

    // Prompt for permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                // If dismissed without granting permission, go back
                if (!permissionGranted) {
                    navController.popBackStack()
                }
                showPermissionDialog = false
            },
            title = { Text("Permission Request") },
            text = {
                Text("Someone is requesting to view your Virtual ID details. Do you want to grant permission?")
            },
            confirmButton = {
                Button(onClick = {
                    permissionGranted = true
                    showPermissionDialog = false
                    loadUserData()
                }) {
                    Text("Allow")
                }
            },
            dismissButton = {
                Button(onClick = {
                    navController.popBackStack()
                    showPermissionDialog = false
                }) {
                    Text("Deny")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!permissionGranted) {
                // Don't show anything until permission is granted
                Box(modifier = Modifier.fillMaxSize())
            } else if (isLoading) {
                CircularProgressIndicator()
                Text("Loading user details...", modifier = Modifier.padding(top = 16.dp))
            } else if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (userData != null) {
                Text(
                    text = userData?.get("name") as? String ?: "",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                DisplayField("Aadhaar Number", userData?.get("aadhaarNumber") as? String ?: "")
                DisplayField("PAN Number", userData?.get("panNumber") as? String ?: "")
                DisplayField("License Number", userData?.get("licenseNumber") as? String ?: "")
                DisplayField("License Valid Till", userData?.get("expiryDate") as? String ?: "")
            }
        }
    }
}
@Composable
fun DisplayField(label: String, value: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "$label: $value",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)

            )
        }
    }




}
