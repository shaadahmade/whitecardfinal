package com.example.whitecard.qrutils

fun isValidAadhaar(aadhaar: String): Boolean {
    return aadhaar.length == 12 && aadhaar.all { it.isDigit() }
}

fun isValidPAN(pan: String): Boolean {
    val regex = Regex("[A-Z]{5}[0-9]{4}[A-Z]{1}")
    return pan.length == 10 && regex.matches(pan)
}

fun isValidDrivingLicense(license: String): Boolean {
    val regex = """^[A-Z]{2}[-\s]?[0-9]{2,14}$""".toRegex()
    return regex.matches(license)
}
