package com.example.whitecard.firebase

import com.google.firebase.firestore.FirebaseFirestore

private fun checkUserVerificationStatus(userId: String?, callback: (Boolean) -> Unit) {
    if (userId == null) {
        callback(false)
        return
    }

    val db = FirebaseFirestore.getInstance()
    db.collection("users").document(userId).get()
        .addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val isVerified = document.getBoolean("isVerified") ?: false
                callback(isVerified)
            } else {
                callback(false)
            }
        }
        .addOnFailureListener {
            callback(false)
        }
}