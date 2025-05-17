package com.example.whitecard.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults

import androidx.compose.material3.TopAppBar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.whitecard.firebase.FirestoreManager
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.input.KeyboardType
import com.example.whitecard.qrutils.isValidAadhaar
import com.example.whitecard.qrutils.isValidPAN

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateCardScreen(navController: NavController) {
    var aadhaarNumber by remember { mutableStateOf("") }
    var panNumber by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    // Fetch current user data to prefill fields
    LaunchedEffect(key1 = true) {
        FirestoreManager.getUserData { userData ->
            userData?.let {
                aadhaarNumber = (it["aadhaarNumber"] as? String) ?: ""
                panNumber = (it["panNumber"] as? String) ?: ""
                licenseNumber = (it["licenseNumber"] as? String) ?: ""
                expiryDate = (it["expiryDate"] as? String) ?: ""
            }
        }
    }

    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF000000),
            Color(0xFF464646)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Update Card",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A56E8),

                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        androidx.compose.material3.Icon(
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
                    .verticalScroll(rememberScrollState())
            ) {
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (successMessage.isNotEmpty()) {
                    Text(
                        text = successMessage,
                        color = Color.Green,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                    isError = errorMessage.isNotEmpty(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                    )
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
                    isError = errorMessage.isNotEmpty(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                    )

                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = licenseNumber,
                    onValueChange = { licenseNumber = it },
                    label = { Text("License Number") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Expiry Date") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(



                    onClick = {
                        if(aadhaarNumber.isNotEmpty() && panNumber.isNotEmpty() && licenseNumber.isNotEmpty() && expiryDate.isNotEmpty()) {
                            if (isValidAadhaar(aadhaarNumber)) {


                                isLoading = true
                                FirestoreManager.updateUserData(
                                    aadhaarNumber,
                                    panNumber,
                                    licenseNumber,
                                    expiryDate
                                ) { success ->
                                    isLoading = false
                                    if (success) {
                                        successMessage = "Card updated successfully"
                                        errorMessage = ""
                                    }
                                }
                            } else {
                                errorMessage = "Invalid Aadhaar Number"
                                successMessage = ""

                            }

                        }
                        else if (panNumber.isNotEmpty() && licenseNumber.isNotEmpty() && expiryDate.isNotEmpty()){
                            if (isValidPAN(panNumber)){
                                isLoading = true
                                FirestoreManager.updateUserData(
                                    aadhaarNumber,
                                    panNumber,
                                    licenseNumber,
                                    expiryDate
                                ) { success ->
                                    isLoading = false
                                    if (success) {
                                        successMessage = "Card updated successfully"
                                        errorMessage = ""
                                    }
                                }
                            }
                            else {
                                errorMessage = "Invalid PAN Number"
                                successMessage = ""
                            }



                        }



                        else {
                                errorMessage = "Failed to update card"
                                successMessage = ""
                            }

                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF1A56E8),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Update Card",
                            color = Color(0xFF1A56E8),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}