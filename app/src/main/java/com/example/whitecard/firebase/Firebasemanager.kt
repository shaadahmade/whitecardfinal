
package com.example.whitecard.firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.whitecard.screens.CardItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

object FirestoreManager {
    private const val TAG = "FirestoreManager"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var appContext: Context

    // Collection references
    private const val USERS_COLLECTION = "users"
    private const val CARDS_COLLECTION = "cards"
    private const val TRANSACTIONS_COLLECTION = "transactions"

    // Initialize with context
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

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
            .addOnFailureListener { callback(false) }
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

    // COROUTINE-BASED METHODS WITH IMAGE SUPPORT

    /**
     * Updates the user's profile image and other data
     */
    suspend fun updateUserProfile(imageUri: Uri?, displayName: String? = null): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!::appContext.isInitialized) {
                    return@withContext Result.failure(Exception("FirestoreManager not initialized with context"))
                }

                val currentUser = auth.currentUser ?: throw Exception("User not logged in")
                var photoUrl: String? = null

                // Upload image if provided
                if (imageUri != null) {
                    try {
                        val bytes = appContext.contentResolver.openInputStream(imageUri)?.use {
                            it.readBytes()
                        } ?: throw IOException("Could not read from URI: $imageUri")

                        val imageRef = storage.reference.child("profile_images/${currentUser.uid}/${UUID.randomUUID()}")
                        imageRef.putBytes(bytes).await()
                        photoUrl = imageRef.downloadUrl.await().toString()

                        // Update the user's profile in Firebase Auth
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setPhotoUri(Uri.parse(photoUrl))
                            .apply {
                                if (displayName != null) setDisplayName(displayName)
                            }
                            .build()

                        currentUser.updateProfile(profileUpdates).await()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error uploading profile image", e)
                        return@withContext Result.failure(e)
                    }
                } else if (displayName != null) {
                    // If only updating display name
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()

                    currentUser.updateProfile(profileUpdates).await()
                }

                // Update user document in Firestore if needed
                val userUpdates = hashMapOf<String, Any>()
                if (photoUrl != null) {
                    userUpdates["profileImageUrl"] = photoUrl
                }
                if (displayName != null) {
                    userUpdates["name"] = displayName
                }

                if (userUpdates.isNotEmpty()) {
                    db.collection(USERS_COLLECTION).document(currentUser.uid)
                        .set(userUpdates, SetOptions.merge())
                        .await()
                }

                Result.success(photoUrl ?: currentUser.photoUrl?.toString() ?: "")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user profile", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Save a card to Firestore and upload the image to Storage
     */
    suspend fun saveCard(card: CardItem, imageUri: Uri): Result<CardItem> {
        return withContext(Dispatchers.IO) {
            try {
                if (!::appContext.isInitialized) {
                    return@withContext Result.failure(Exception("FirestoreManager not initialized with context"))
                }

                val currentUser = auth.currentUser ?:
                return@withContext Result.failure(Exception("User not logged in"))

                Log.d(TAG, "Starting card save process for user: ${currentUser.uid}")
                Log.d(TAG, "Image URI: $imageUri")

                try {
                    // Check if the image URI can be opened using appContext
                    val bytes = appContext.contentResolver.openInputStream(imageUri)?.use {
                        it.readBytes()
                    } ?: return@withContext Result.failure(IOException("Could not read from URI: $imageUri"))

                    // Upload as bytes instead of file
                    val imageRef = storage.reference.child("cards/${currentUser.uid}/${UUID.randomUUID()}")
                    Log.d(TAG, "Uploading to storage reference: ${imageRef.path}")

                    imageRef.putBytes(bytes).await()
                    Log.d(TAG, "Image upload successful")

                    val downloadUrl = imageRef.downloadUrl.await().toString()
                    Log.d(TAG, "Download URL obtained: $downloadUrl")

                    // Create the card with Firebase URL
                    val updatedCard = card.copy(firebaseImageUrl = downloadUrl)

                    // Save to Firestore
                    val cardData = hashMapOf(
                        "id" to updatedCard.id,
                        "name" to updatedCard.name,
                        "details" to updatedCard.details,
                        "isVerified" to updatedCard.isVerified,
                        "imageUrl" to downloadUrl,
                        "userId" to currentUser.uid,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )

                    Log.d(TAG, "Saving card data to Firestore")
                    db.collection(USERS_COLLECTION)
                        .document(currentUser.uid)
                        .collection(CARDS_COLLECTION)
                        .document(updatedCard.id.toString())
                        .set(cardData)
                        .await()

                    Log.d(TAG, "Card saved successfully")
                    return@withContext Result.success(updatedCard)
                } catch (e: IOException) {
                    Log.e(TAG, "IO Error reading from URI", e)
                    return@withContext Result.failure(e)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in image upload", e)
                    return@withContext Result.failure(e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving card", e)
                return@withContext Result.failure(e)
            }
        }
    }

    /**
     * Update an existing card in Firestore
     */
    suspend fun updateCard(card: CardItem, newImageUri: Uri? = null): Result<CardItem> {
        return withContext(Dispatchers.IO) {
            try {
                if (!::appContext.isInitialized && newImageUri != null) {
                    return@withContext Result.failure(Exception("FirestoreManager not initialized with context"))
                }

                val currentUser = auth.currentUser ?: throw Exception("User not logged in")
                var updatedCard = card

                // If a new image is provided, upload it
                if (newImageUri != null) {
                    try {
                        val bytes = appContext.contentResolver.openInputStream(newImageUri)?.use {
                            it.readBytes()
                        } ?: throw IOException("Could not read from URI: $newImageUri")

                        val imageRef = storage.reference.child("cards/${currentUser.uid}/${UUID.randomUUID()}")
                        imageRef.putBytes(bytes).await()
                        val downloadUrl = imageRef.downloadUrl.await().toString()
                        updatedCard = card.copy(firebaseImageUrl = downloadUrl)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error uploading new image for card", e)
                        return@withContext Result.failure(e)
                    }
                }

                // Update in Firestore
                val cardData = hashMapOf(
                    "name" to updatedCard.name,
                    "details" to updatedCard.details,
                    "isVerified" to updatedCard.isVerified,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )

                if (newImageUri != null) {
                    cardData["imageUrl"] = updatedCard.firebaseImageUrl
                }

                db.collection(USERS_COLLECTION)
                    .document(currentUser.uid)
                    .collection(CARDS_COLLECTION)
                    .document(updatedCard.id.toString())
                    .set(cardData, SetOptions.merge())
                    .await()

                Result.success(updatedCard)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating card", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Delete a card from Firestore and Storage
     */
    suspend fun deleteCard(card: CardItem): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser ?: throw Exception("User not logged in")

                // Delete from Firestore
                db.collection(USERS_COLLECTION)
                    .document(currentUser.uid)
                    .collection(CARDS_COLLECTION)
                    .document(card.id.toString())
                    .delete()
                    .await()

                // Delete image from Storage if URL exists
                if (card.firebaseImageUrl.isNotEmpty()) {
                    try {
                        val imageRef = storage.getReferenceFromUrl(card.firebaseImageUrl)
                        imageRef.delete().await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error deleting image from storage, continuing anyway", e)
                        // Continue even if image deletion fails
                    }
                }

                Result.success(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting card", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Load all cards for the current user
     */
    suspend fun loadCards(): Result<List<CardItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser ?: throw Exception("User not logged in")

                val snapshot = db.collection(USERS_COLLECTION)
                    .document(currentUser.uid)
                    .collection(CARDS_COLLECTION)
                    .get()
                    .await()

                val cards = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return@mapNotNull null
                    val name = doc.getString("name") ?: ""
                    val details = doc.getString("details") ?: ""
                    val isVerified = doc.getBoolean("isVerified") ?: false
                    val imageUrl = doc.getString("imageUrl") ?: ""

                    CardItem(
                        id = id,
                        imageUri = imageUrl, // Use Firebase URL as the imageUri
                        name = name,
                        details = details,
                        isVerified = isVerified,
                        firebaseImageUrl = imageUrl
                    )
                }

                Result.success(cards)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading cards", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Save user profile data during onboarding
     */
    suspend fun saveUserProfile(
        name: String,
        aadhaarNumber: String,
        panNumber: String,
        licenseNumber: String,
        expiryDate: String
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser ?: throw Exception("User not logged in")

                // Update profile in Firebase Auth
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                currentUser.updateProfile(profileUpdates).await()

                // Create user document
                val userData = hashMapOf(
                    "name" to name,
                    "aadhaarNumber" to aadhaarNumber,
                    "panNumber" to panNumber,
                    "licenseNumber" to licenseNumber,
                    "expiryDate" to expiryDate,
                    "onboardingComplete" to true,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )

                // Save to Firestore - use set with merge to create if not exists
                db.collection(USERS_COLLECTION)
                    .document(currentUser.uid)
                    .set(userData, SetOptions.merge())
                    .await()

                Result.success(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving user profile", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get user data with coroutines
     */
    suspend fun getUserDataAsync(): Result<Map<String, Any>?> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser ?: throw Exception("User not logged in")

                val documentSnapshot = db.collection(USERS_COLLECTION)
                    .document(currentUser.uid)
                    .get()
                    .await()

                if (documentSnapshot.exists()) {
                    Result.success(documentSnapshot.data)
                } else {
                    Result.success(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting user data", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Update user data with coroutines
     */
    suspend fun updateUserDataAsync(userData: HashMap<String, Any>): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser ?: throw Exception("User not logged in")

                db.collection(USERS_COLLECTION)
                    .document(currentUser.uid)
                    .set(userData, SetOptions.merge())
                    .await()

                Result.success(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user data", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Update user verification data with coroutines
     */
    suspend fun updateUserVerificationAsync(
        aadhaarNumber: String,
        panNumber: String,
        licenseNumber: String,
        expiryDate: String
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser ?: throw Exception("User not logged in")

                val userData = hashMapOf(
                    "aadhaarNumber" to aadhaarNumber,
                    "panNumber" to panNumber,
                    "licenseNumber" to licenseNumber,
                    "expiryDate" to expiryDate,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )

                db.collection(USERS_COLLECTION)
                    .document(currentUser.uid)
                    .set(userData, SetOptions.merge())
                    .await()

                Result.success(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user verification data", e)
                Result.failure(e)
            }
        }
    }
}
