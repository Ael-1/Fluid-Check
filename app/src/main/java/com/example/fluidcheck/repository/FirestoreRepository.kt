package com.example.fluidcheck.repository

import com.example.fluidcheck.model.FluidLog
import com.example.fluidcheck.model.UserRecord
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val usernamesCollection = db.collection("usernames")

    suspend fun saveUserRecord(userId: String, record: UserRecord, username: String): Result<Unit> {
        return try {
            // Check if username is already taken (only if username is not empty)
            if (username.isNotEmpty()) {
                val usernameDoc = usernamesCollection.document(username).get().await()
                if (usernameDoc.exists() && usernameDoc.getString("uid") != userId) {
                    return Result.failure(Exception("Username already taken"))
                }
            }

            db.runBatch { batch ->
                batch.set(usersCollection.document(userId), record, SetOptions.merge())
                if (username.isNotEmpty()) {
                    batch.update(usersCollection.document(userId), "username", username)
                    batch.set(usernamesCollection.document(username), mapOf("uid" to userId))
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEmailFromUsername(username: String): String? {
        return try {
            val usernameDoc = usernamesCollection.document(username).get().await()
            if (usernameDoc.exists()) {
                val uid = usernameDoc.getString("uid") ?: return null
                val userDoc = usersCollection.document(uid).get().await()
                userDoc.getString("email")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserRecord(userId: String): UserRecord? {
        return try {
            val document = usersCollection.document(userId).get().await()
            if (document.exists()) {
                // Manually map fields to handle potential missing fields in older documents
                UserRecord(
                    weight = document.getString("weight") ?: "",
                    height = document.getString("height") ?: "",
                    age = document.getString("age") ?: "",
                    sex = document.getString("sex") ?: "",
                    activity = document.getString("activity") ?: "",
                    environment = document.getString("environment") ?: "",
                    setupCompleted = document.getBoolean("setupCompleted") ?: false
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveDailyGoal(userId: String, goal: Int): Result<Unit> {
        return try {
            usersCollection.document(userId).update("dailyGoal", goal).await()
            Result.success(Unit)
        } catch (e: Exception) {
            try {
                usersCollection.document(userId).set(mapOf("dailyGoal" to goal), SetOptions.merge()).await()
                Result.success(Unit)
            } catch (innerE: Exception) {
                Result.failure(innerE)
            }
        }
    }

    suspend fun getDailyGoal(userId: String): Int {
        return try {
            val document = usersCollection.document(userId).get().await()
            (document.getLong("dailyGoal") ?: 3000L).toInt()
        } catch (e: Exception) {
            3000
        }
    }

    suspend fun saveFluidLog(userId: String, log: FluidLog): Result<Unit> {
        return try {
            usersCollection.document(userId)
                .collection("fluid_logs")
                .document(log.id.toString())
                .set(log).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFluidLogs(userId: String): List<FluidLog> {
        return try {
            val querySnapshot = usersCollection.document(userId)
                .collection("fluid_logs")
                .get().await()
            querySnapshot.toObjects(FluidLog::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
