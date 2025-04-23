package com.example.whitecard.qrutils



import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CredentialSharingManager {
    private val db = FirebaseFirestore.getInstance()

    suspend fun requestCredentialAccess(targetUserId: String): String {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: throw Exception("User not logged in")
        val currentUserDoc = db.collection("users").document(currentUser.uid).get().await()
        val requesterName = currentUserDoc.getString("name") ?: "Unknown User"

        // Create a pending request
        val requestMap = hashMapOf(
            "requesterId" to currentUser.uid,
            "requesterName" to requesterName,
            "targetUserId" to targetUserId,
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )

        val requestRef = db.collection("credentialRequests").add(requestMap).await()
        return requestRef.id
    }

    suspend fun checkRequestStatus(requestId: String): String {
        val request = db.collection("credentialRequests").document(requestId).get().await()
        return request.getString("status") ?: "pending"
    }
}