package com.example.whitecard.qrutils

fun isValidAadhaar(aadhaar: String): Boolean {
    // Basic validation - in a real app, implement actual Aadhaar validation algorithm
    return aadhaar.length == 12 && aadhaar.all { it.isDigit() }
}

fun isValidPAN(pan: String): Boolean {
    // Basic validation - in a real app, implement actual PAN validation algorithm
    val regex = Regex("[A-Z]{5}[0-9]{4}[A-Z]{1}")
    return pan.length == 10 && regex.matches(pan)
}