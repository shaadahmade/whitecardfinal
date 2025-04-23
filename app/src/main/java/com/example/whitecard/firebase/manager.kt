package com.example.whitecard.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import com.google.firebase.firestore.SetOptions

object FirestoreManager {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private const val USERS_COLLECTION = "users"

    fun createUserProfile(userData: HashMap<String, Any>, callback: (Boolean) -> Unit) {

        val userId = auth.currentUser?.uid ?: return callback(false)


        db.collection(USERS_COLLECTION).document(userId)
            .set(userData)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }

    }

    fun updateUserData(userData: HashMap<String, Any>, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return callback(false)


        db.collection(USERS_COLLECTION).document(userId)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun getUserData(callback: (Map<String, Any>?) -> Unit) {
        val userId = auth.currentUser?.uid ?: return callback(null)

        db.collection(USERS_COLLECTION).document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    callback(document.data)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { callback(null) }
    }
    fun deleteUserData(callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return callback(false)
        db.collection(USERS_COLLECTION).document(userId)
            .delete()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }

    }
    fun updateUserData(
        aadhaarNumber: String,
        panNumber: String,
        licenseNumber: String,
        expiryDate: String,
        callback: (Boolean) -> Unit
        ) {
        val userId = auth.currentUser?.uid ?: return callback(false)
        val userData = hashMapOf(
            "aadhaarNumber" to aadhaarNumber,
            "panNumber" to panNumber,
            "licenseNumber" to licenseNumber,
            "expiryDate" to expiryDate


        )
        db.collection(USERS_COLLECTION).document(userId)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener { callback(true) }

    }





}