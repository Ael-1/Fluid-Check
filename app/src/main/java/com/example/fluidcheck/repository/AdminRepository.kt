package com.example.fluidcheck.repository

import android.content.Context
import com.example.fluidcheck.model.UserRecord
import com.example.fluidcheck.util.DataResult
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository providing administrative capabilities for managing user accounts.
 * Allows roles updates, soft deletions, and monitoring all user records.
 */
class AdminRepository(private val context: Context? = null) {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val usernamesCollection = db.collection("usernames")
    
    // We instantiate UserRepository internally just to reuse the mapping function
    private val userRepository = UserRepository(context)

    /**
     * Updates the role (e.g., USER, MODERATOR, ADMIN) for a specific user.
     * 
     * @param uid The unique identifier of the user to update.
     * @param newRole The string identifier for the new role.
     * @return A [DataResult] indicating the outcome of the update.
     */
    suspend fun updateRole(uid: String, newRole: String): DataResult<Unit> {
        return try {
            usersCollection.document(uid).update("role", newRole).await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Returns a real-time Flow of all user records stored in Firestore.
     * Wrapped in a [DataResult] to handle snapshot errors.
     */
    fun getAllUsersFlow(): Flow<DataResult<List<UserRecord>>> = callbackFlow {
        val registration = usersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(DataResult.Error(error))
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val users = snapshot.documents.map { userRepository.mapDocumentToUserRecord(it) }
                trySend(DataResult.Success(users))
            }
        }
        awaitClose { registration.remove() }
    }

    /**
     * Performs a soft delete on a user account, marking it as deleted and removing its username mapping.
     */
    suspend fun softDeleteUser(uid: String): DataResult<Unit> {
        return try {
            val userDoc = usersCollection.document(uid).get().await()
            val username = userDoc.getString("username")
            
            db.runBatch { batch ->
                batch.update(usersCollection.document(uid), "deleted", true)
                if (!username.isNullOrEmpty()) {
                    batch.delete(usernamesCollection.document(username.lowercase()))
                }
            }.await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Performs a bulk soft delete for multiple user accounts.
     */
    suspend fun softDeleteUsers(uids: Set<String>): DataResult<Unit> {
        if (uids.isEmpty()) return DataResult.Success(Unit)
        return try {
            val batch = db.batch()
            uids.forEach { uid ->
                val userRef = usersCollection.document(uid)
                batch.update(userRef, "deleted", true)
            }
            batch.commit().await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }
}
