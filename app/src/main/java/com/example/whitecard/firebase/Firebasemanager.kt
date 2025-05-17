package com.example.whitecard.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot

object FirestoreManager {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private const val USERS_COLLECTION = "users"
    private const val CARDS_COLLECTION = "cards"
    private const val TRANSACTIONS_COLLECTION = "transactions"

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

    // Query users by specific field
    fun queryUsersByField(field: String, value: Any, callback: (List<Map<String, Any>>) -> Unit) {
        db.collection(USERS_COLLECTION)
            .whereEqualTo(field, value)
            .get()
            .addOnSuccessListener { documents ->
                val users = documents.map { it.data }
                callback(users)
            }
            .addOnFailureListener { callback(emptyList()) }
    }

    // Add a subcollection to user document
    fun addCardToUser(cardData: HashMap<String, Any>, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return callback(false)
        
        db.collection(USERS_COLLECTION)
            .document(userId)
            .collection(CARDS_COLLECTION)
            .add(cardData)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    // Batch operations
    fun batchUpdateUserData(updates: List<HashMap<String, Any>>, callback: (Boolean) -> Unit) {
        val batch = db.batch()
        val userId = auth.currentUser?.uid ?: return callback(false)

        updates.forEach { update ->
            val docRef = db.collection(USERS_COLLECTION).document(userId)
            batch.update(docRef, update)
        }

        batch.commit()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    // Real-time updates
    fun listenToUserData(callback: (Map<String, Any>?) -> Unit): ListenerRegistration {
        val userId = auth.currentUser?.uid ?: return object : ListenerRegistration {
            override fun remove() {}
        }

        return db.collection(USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    callback(null)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    callback(snapshot.data)
                } else {
                    callback(null)
                }
            }
    }

    // Transaction operation
    fun updateUserBalance(amount: Double, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return callback(false)

        db.runTransaction { transaction ->
            val userRef = db.collection(USERS_COLLECTION).document(userId)
            val userDoc = transaction.get(userRef)
            
            val currentBalance = userDoc.getDouble("balance") ?: 0.0
            val newBalance = currentBalance + amount
            
            transaction.update(userRef, "balance", newBalance)
            newBalance
        }
        .addOnSuccessListener { callback(true) }
        .addOnFailureListener { callback(false) }
    }

    // Get user's cards
    fun getUserCards(callback: (List<Map<String, Any>>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return callback(emptyList())

        db.collection(USERS_COLLECTION)
            .document(userId)
            .collection(CARDS_COLLECTION)
            .get()
            .addOnSuccessListener { documents ->
                val cards = documents.map { it.data }
                callback(cards)
            }
            .addOnFailureListener { callback(emptyList()) }
    }

    // Add transaction
    fun addTransaction(transactionData: HashMap<String, Any>, callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return callback(false)

        db.collection(USERS_COLLECTION)
            .document(userId)
            .collection(TRANSACTIONS_COLLECTION)
            .add(transactionData)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }
}