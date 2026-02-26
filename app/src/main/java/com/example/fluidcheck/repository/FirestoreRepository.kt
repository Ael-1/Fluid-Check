package com.example.fluidcheck.repository

import com.example.fluidcheck.model.FluidLog
import com.example.fluidcheck.model.QuickAddConfig
import com.example.fluidcheck.model.UserRecord
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val usernamesCollection = db.collection("usernames")

    suspend fun saveUserRecord(uid: String, record: UserRecord): Result<Unit> {
        return try {
            if (uid.isEmpty()) return Result.failure(Exception("UID cannot be empty"))
            
            db.runBatch { batch ->
                // 1. Write username mapping
                if (record.username.isNotEmpty()) {
                    val usernameRef = usernamesCollection.document(record.username)
                    batch.set(usernameRef, mapOf("uid" to uid))
                }
                
                // 2. Write user record
                val userRef = usersCollection.document(uid)
                batch.set(userRef, record, SetOptions.merge())
            }.await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUsername(uid: String, oldUsername: String, newUsername: String): Result<Unit> {
        return try {
            if (oldUsername == newUsername) return Result.success(Unit)
            
            // Check if new username is available
            val newUsernameDoc = usernamesCollection.document(newUsername).get().await()
            if (newUsernameDoc.exists()) {
                return Result.failure(Exception("Username already taken"))
            }

            db.runBatch { batch ->
                // Delete old username mapping
                if (oldUsername.isNotEmpty()) {
                    batch.delete(usernamesCollection.document(oldUsername))
                }
                // Add new username mapping
                batch.set(usernamesCollection.document(newUsername), mapOf("uid" to uid))
                // Update user record
                batch.update(usersCollection.document(uid), "username", newUsername)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRole(uid: String, newRole: String): Result<Unit> {
        return try {
            usersCollection.document(uid).update("role", newRole).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEmailFromUsername(username: String): String? {
        return try {
            val usernameDoc = usernamesCollection.document(username).get().await()
            val uid = usernameDoc.getString("uid") ?: return null
            
            val userDoc = usersCollection.document(uid).get().await()
            userDoc.getString("email")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUsernameFromUid(uid: String): String? {
        return try {
            val userDoc = usersCollection.document(uid).get().await()
            if (userDoc.exists()) {
                userDoc.getString("username")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getUserRecordFlow(uid: String): Flow<UserRecord?> = callbackFlow {
        val docRef = usersCollection.document(uid)
        val registration = docRef.addSnapshotListener { document, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (document != null && document.exists()) {
                val record = mapDocumentToUserRecord(document)
                trySend(record)
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    fun getAllUsersFlow(): Flow<List<UserRecord>> = callbackFlow {
        val registration = usersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val users = snapshot.documents.map { mapDocumentToUserRecord(it) }
                trySend(users)
            }
        }
        awaitClose { registration.remove() }
    }

    private fun mapDocumentToUserRecord(document: com.google.firebase.firestore.DocumentSnapshot): UserRecord {
        val quickAddData = document.get("quickAddConfig") as? List<Map<String, Any>>
        val quickAddConfig = quickAddData?.map {
            QuickAddConfig(
                amount = (it["amount"] as? Long)?.toInt() ?: 0,
                type = it["type"] as? String ?: "Water"
            )
        }

        return UserRecord(
            uid = document.id,
            username = document.getString("username") ?: "",
            email = document.getString("email") ?: "",
            profilePictureUrl = document.getString("profilePictureUrl") ?: "",
            weight = document.getString("weight") ?: "",
            height = document.getString("height") ?: "",
            age = document.getString("age") ?: "",
            sex = document.getString("sex") ?: "",
            activity = document.getString("activity") ?: "",
            environment = document.getString("environment") ?: "",
            setupCompleted = document.getBoolean("setupCompleted") ?: false,
            role = document.getString("role") ?: "USER",
            isDeleted = document.getBoolean("isDeleted") ?: false,
            fcmToken = document.getString("fcmToken") ?: "",
            quickAddConfig = quickAddConfig,
            notificationsEnabled = document.getBoolean("notificationsEnabled") ?: true,
            lastRingClosedDate = document.getString("lastRingClosedDate") ?: "",
            streak = (document.getLong("streak") ?: 0L).toInt(),
            highestStreak = (document.getLong("highestStreak") ?: 0L).toInt(),
            totalFluidDrankAllTime = (document.getLong("totalFluidDrankAllTime") ?: 0L).toInt(),
            totalRingsClosed = (document.getLong("totalRingsClosed") ?: 0L).toInt(),
            createdAt = document.getTimestamp("createdAt"),
            dailyGoal = document.getLong("dailyGoal")?.toInt()
        )
    }

    suspend fun getUserRecord(uid: String): UserRecord? {
        return try {
            val document = usersCollection.document(uid).get().await()
            if (document.exists()) {
                mapDocumentToUserRecord(document)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveDailyGoal(uid: String, goal: Int?): Result<Unit> {
        return try {
            usersCollection.document(uid).update("dailyGoal", goal).await()
            Result.success(Unit)
        } catch (e: Exception) {
            try {
                usersCollection.document(uid).set(mapOf("dailyGoal" to goal), SetOptions.merge()).await()
                Result.success(Unit)
            } catch (innerE: Exception) {
                Result.failure(innerE)
            }
        }
    }

    suspend fun getDailyGoal(uid: String): Int? {
        return try {
            val document = usersCollection.document(uid).get().await()
            document.getLong("dailyGoal")?.toInt()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveFluidLog(uid: String, log: FluidLog): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val userRef = usersCollection.document(uid)
                val userDoc = transaction.get(userRef)
                val currentTotal = userDoc.getLong("totalFluidDrankAllTime") ?: 0L
                
                transaction.update(userRef, "totalFluidDrankAllTime", currentTotal + log.amount)
                
                val logRef = userRef.collection("fluid_logs").document(log.id.toString())
                transaction.set(logRef, log)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFluidLog(uid: String, oldLog: FluidLog, newLog: FluidLog): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val userRef = usersCollection.document(uid)
                val userDoc = transaction.get(userRef)
                val currentTotal = userDoc.getLong("totalFluidDrankAllTime") ?: 0L
                
                val amountDiff = newLog.amount - oldLog.amount
                transaction.update(userRef, "totalFluidDrankAllTime", currentTotal + amountDiff)
                
                val logRef = userRef.collection("fluid_logs").document(newLog.id.toString())
                transaction.set(logRef, newLog)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFluidLog(uid: String, log: FluidLog): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val userRef = usersCollection.document(uid)
                val userDoc = transaction.get(userRef)
                val currentTotal = userDoc.getLong("totalFluidDrankAllTime") ?: 0L
                
                transaction.update(userRef, "totalFluidDrankAllTime", (currentTotal - log.amount).coerceAtLeast(0))
                
                val logRef = userRef.collection("fluid_logs").document(log.id.toString())
                transaction.delete(logRef)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getFluidLogsFlow(uid: String): Flow<List<FluidLog>> = callbackFlow {
        val registration = usersCollection.document(uid)
            .collection("fluid_logs")
            .orderBy("id")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(FluidLog::class.java))
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun getFluidLogs(uid: String): List<FluidLog> {
        return try {
            val querySnapshot = usersCollection.document(uid)
                .collection("fluid_logs")
                .orderBy("id")
                .get().await()
            querySnapshot.toObjects(FluidLog::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getTodayFluidLogsFlow(uid: String): Flow<List<FluidLog>> = callbackFlow {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())
        val registration = usersCollection.document(uid)
            .collection("fluid_logs")
            .whereEqualTo("date", today)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(FluidLog::class.java).sortedBy { it.id })
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun getTodayFluidLogs(uid: String): List<FluidLog> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())
        return try {
            val querySnapshot = usersCollection.document(uid)
                .collection("fluid_logs")
                .whereEqualTo("date", today)
                .get().await()
            querySnapshot.toObjects(FluidLog::class.java).sortedBy { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateQuickAddConfig(uid: String, config: List<QuickAddConfig>): Result<Unit> {
        return try {
            usersCollection.document(uid).update("quickAddConfig", config).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStreak(uid: String, streak: Int, lastRingClosedDate: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val userRef = usersCollection.document(uid)
                val userDoc = transaction.get(userRef)
                val highestStreak = (userDoc.getLong("highestStreak") ?: 0L).toInt()
                
                val updates = mutableMapOf<String, Any>(
                    "streak" to streak,
                    "lastRingClosedDate" to lastRingClosedDate
                )
                
                if (streak > highestStreak) {
                    updates["highestStreak"] = streak
                }
                
                transaction.update(userRef, updates)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(uid: String, token: String): Result<Unit> {
        return try {
            usersCollection.document(uid).update("fcmToken", token).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNotificationsEnabled(uid: String, enabled: Boolean): Result<Unit> {
        return try {
            usersCollection.document(uid).update("notificationsEnabled", enabled).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun incrementTotalRingsClosed(uid: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val userRef = usersCollection.document(uid)
                val userDoc = transaction.get(userRef)
                val currentTotal = userDoc.getLong("totalRingsClosed") ?: 0L
                transaction.update(userRef, "totalRingsClosed", currentTotal + 1)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun softDeleteUser(uid: String): Result<Unit> {
        return try {
            usersCollection.document(uid).update("isDeleted", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isUsernameAvailable(username: String): Boolean {
        return try {
            val doc = usernamesCollection.document(username).get().await()
            !doc.exists()
        } catch (e: Exception) {
            false
        }
    }
}
