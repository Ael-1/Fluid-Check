package com.example.fluidcheck.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.fluidcheck.model.QuickAddConfig
import com.example.fluidcheck.model.UserRecord
import com.example.fluidcheck.util.DataResult
import com.example.fluidcheck.util.ImageUploadService
import com.example.fluidcheck.util.ImageUtils
import com.example.fluidcheck.util.ProfilePhotoManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository handling user-related data operations in Firestore and local guest storage.
 * Manages profile information, goals, streaks, and photo uploads.
 */
class UserRepository(private val context: Context? = null) {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val usernamesCollection = db.collection("usernames")
    
    private val guestRepository: GuestRepository? = context?.let { GuestRepository(it) }

    /**
     * Saves or updates a user record in Firestore or local storage.
     * 
     * If the user is a guest, data is persisted locally. For authenticated users, 
     * updates are sent to Firestore. The operation ensures that the username 
     * mapping is established within an atomic batch.
     *
     * @param uid The unique identifier of the user.
     * @param record The [UserRecord] object containing updated profile data.
     * @return A [DataResult] indicating success or containing an exception.
     */
    suspend fun saveUserRecord(uid: String, record: UserRecord): DataResult<Unit> {
        return try {
            if (uid.isEmpty()) return DataResult.Error(Exception("UID cannot be empty"))
            if (uid == "GUEST") {
                return guestRepository?.saveUserRecord(record) ?: DataResult.Error(Exception("GuestRepository not initialized"))
            }
            
            db.runBatch { batch ->
                if (record.username.isNotEmpty()) {
                    val usernameRef = usernamesCollection.document(record.username.lowercase())
                    batch.set(usernameRef, mapOf("uid" to uid))
                }
                
                val userRef = usersCollection.document(uid)
                batch.set(userRef, record, SetOptions.merge())
            }.await()
            
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Updates a user's username, handling the removal of the old mapping and creation of the new one.
     * Performs a check for username availability during the process.
     */
    suspend fun updateUsername(uid: String, oldUsername: String, newUsername: String): DataResult<Unit> {
        return try {
            if (oldUsername == newUsername) return DataResult.Success(Unit)
            
            if (uid == "GUEST") {
                val currentRecordRes = getUserRecord(uid)
                val currentRecord = (currentRecordRes as? DataResult.Success)?.data
                if (currentRecord != null) {
                    saveUserRecord(uid, currentRecord.copy(username = newUsername))
                }
                return DataResult.Success(Unit)
            }

            val lowerNew = newUsername.lowercase()
            val lowerOld = oldUsername.lowercase()
            
            val newUsernameDoc = usernamesCollection.document(lowerNew).get().await()
            if (newUsernameDoc.exists() && newUsernameDoc.getString("uid") != uid) {
                return DataResult.Error(Exception("Username already taken"))
            }

            db.runBatch { batch ->
                if (lowerOld.isNotEmpty()) {
                    batch.delete(usernamesCollection.document(lowerOld))
                }
                batch.set(usernamesCollection.document(lowerNew), mapOf("uid" to uid))
                batch.update(usersCollection.document(uid), "username", newUsername)
            }.await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Retrieves the email associated with a given username.
     * 
     * Queries the global `usernames` collection mapping to find the UID, 
     * then fetches the corresponding user document to extract the email.
     *
     * @param username The username to look up.
     * @return A [DataResult] containing the email address, or null if not found.
     */
    suspend fun getEmailFromUsername(username: String): DataResult<String?> {
        if (username.equals("Guest", ignoreCase = true)) return DataResult.Success(null)
        return try {
            val usernameDoc = usernamesCollection.document(username.lowercase()).get().await()
            if (!usernameDoc.exists()) return DataResult.Success(null)
            val uid = usernameDoc.getString("uid") ?: return DataResult.Success(null)
            val userDoc = usersCollection.document(uid).get().await()
            DataResult.Success(userDoc.getString("email"))
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Retrieves the username for a specific UID.
     */
    suspend fun getUsernameFromUid(uid: String): DataResult<String?> {
        return try {
            val userDoc = usersCollection.document(uid).get().await()
            DataResult.Success(if (userDoc.exists()) userDoc.getString("username") else null)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Returns a real-time Flow of the user record from Firestore or Guest storage.
     */
    fun getUserRecordFlow(uid: String): Flow<UserRecord?> {
        if (uid == "GUEST") {
            return guestRepository?.guestUserRecordFlow ?: kotlinx.coroutines.flow.flowOf(null)
        }
        return callbackFlow {
            val docRef = usersCollection.document(uid)
            val registration = docRef.addSnapshotListener { document, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (document != null && document.exists()) {
                    trySend(mapDocumentToUserRecord(document))
                } else {
                    trySend(null)
                }
            }
            awaitClose { registration.remove() }
        }
    }

    /**
     * Maps a Firestore [DocumentSnapshot] to a [UserRecord] data model.
     * 
     * Handles type safety for nested objects such as the Quick Add configurations 
     * and maps legacy fields for backward compatibility.
     *
     * @param document The Firestore document snapshot to map.
     * @return A fully populated [UserRecord] instance.
     */
    internal fun mapDocumentToUserRecord(document: com.google.firebase.firestore.DocumentSnapshot): UserRecord {
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
            deleted = (document.getBoolean("deleted") ?: document.getBoolean("isDeleted")) ?: false,
            fcmToken = document.getString("fcmToken") ?: "",
            quickAddConfig = quickAddConfig,
            notificationsEnabled = document.getBoolean("notificationsEnabled"),
            reminderFrequency = document.getString("reminderFrequency") ?: "60",
            lastRingClosedDate = document.getString("lastRingClosedDate") ?: "",
            streak = (document.getLong("streak") ?: 0L).toInt(),
            highestStreak = (document.getLong("highestStreak") ?: 0L).toInt(),
            totalFluidDrankAllTime = (document.getLong("totalFluidDrankAllTime") ?: 0L).toInt(),
            totalRingsClosed = (document.getLong("totalRingsClosed") ?: 0L).toInt(),
            createdAt = document.getTimestamp("createdAt"),
            dailyGoal = document.getLong("dailyGoal")?.toInt(),
            emailVerified = document.getBoolean("emailVerified") ?: false
        )
    }

    /**
     * Performs a one-time fetch of a user record.
     */
    suspend fun getUserRecord(uid: String): DataResult<UserRecord?> {
        if (uid == "GUEST") return guestRepository?.guestUserRecord?.let { DataResult.Success(it) } ?: DataResult.Error(Exception("Guest record missing"))
        return try {
            val document = usersCollection.document(uid).get().await()
            DataResult.Success(if (document.exists()) mapDocumentToUserRecord(document) else null)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Saves the user's daily hydration goal.
     */
    suspend fun saveDailyGoal(uid: String, goal: Int?): DataResult<Unit> {
        if (uid == "GUEST") {
            return guestRepository?.saveDailyGoal(goal) ?: DataResult.Error(Exception("GuestRepository not initialized"))
        }
        return try {
            usersCollection.document(uid).update("dailyGoal", goal).await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            try {
                usersCollection.document(uid).set(mapOf("dailyGoal" to goal), SetOptions.merge()).await()
                DataResult.Success(Unit)
            } catch (innerE: Exception) {
                DataResult.Error(innerE)
            }
        }
    }

    /**
     * Fetches the user's daily hydration goal.
     */
    suspend fun getDailyGoal(uid: String): DataResult<Int?> {
        return try {
            val document = usersCollection.document(uid).get().await()
            DataResult.Success(document.getLong("dailyGoal")?.toInt())
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Updates the Quick Add settings (preset fluid amounts) for a user.
     */
    suspend fun updateQuickAddConfig(uid: String, config: List<QuickAddConfig>): DataResult<Unit> {
        if (uid == "GUEST") {
            return guestRepository?.updateQuickAddConfig(config) ?: DataResult.Error(Exception("GuestRepository not initialized"))
        }
        return try {
            usersCollection.document(uid).update("quickAddConfig", config).await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Records that the user has achieved their daily goal, updating streaks and ring counts.
     */
    suspend fun markGoalAchievedToday(uid: String, todayDate: String, yesterdayDate: String): DataResult<Unit> {
        if (uid == "GUEST") {
            return guestRepository?.markGoalAchievedToday(todayDate, yesterdayDate) ?: DataResult.Error(Exception("GuestRepository not initialized"))
        }

        return try {
            val userRef = usersCollection.document(uid)
            val userDoc = userRef.get().await()
            
            val currentLastRingDate = userDoc.getString("lastRingClosedDate")
            if (currentLastRingDate == todayDate) return DataResult.Success(Unit)
            
            val currentStreak = (userDoc.getLong("streak") ?: 0L).toInt()
            val highestStreak = (userDoc.getLong("highestStreak") ?: 0L).toInt()
            
            val newStreak = if (currentLastRingDate == yesterdayDate) currentStreak + 1 else 1
            
            val updates = mutableMapOf<String, Any>(
                "streak" to newStreak,
                "lastRingClosedDate" to todayDate,
                "totalRingsClosed" to FieldValue.increment(1)
            )
            
            if (newStreak > highestStreak) {
                updates["highestStreak"] = newStreak
            }
            
            userRef.update(updates).await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Evaluates the current streak, resetting it to zero if the user missed a day.
     */
    suspend fun evaluateStreak(uid: String): DataResult<Unit> {
        if (uid == "GUEST") {
             return guestRepository?.evaluateStreak() ?: DataResult.Error(Exception("GuestRepository not initialized"))
        }
        
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())
        
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"))
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(calendar.time)

        return try {
            val userRef = usersCollection.document(uid)
            val userDoc = userRef.get().await()
            if (!userDoc.exists()) return DataResult.Success(Unit)
            
            val lastRingDate = userDoc.getString("lastRingClosedDate") ?: ""
            if (lastRingDate != yesterdayStr && lastRingDate != todayStr) {
                 userRef.update("streak", 0).await()
            }
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Updates the FCM token for push notifications.
     */
    suspend fun updateFcmToken(uid: String, token: String): DataResult<Unit> {
        return try {
            usersCollection.document(uid).update("fcmToken", token).await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Updates notification preferences in the cloud.
     */
    suspend fun updateNotificationsEnabled(uid: String, enabled: Boolean): DataResult<Unit> {
        if (uid == "GUEST") {
            return guestRepository?.updateNotificationsEnabled(enabled) ?: DataResult.Error(Exception("GuestRepository not initialized"))
        }
        return try {
            usersCollection.document(uid).update("notificationsEnabled", enabled).await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Updates the reminder frequency (interval) in the cloud.
     */
    suspend fun updateReminderFrequency(uid: String, frequency: String): DataResult<Unit> {
        if (uid == "GUEST") {
            return guestRepository?.updateReminderFrequency(frequency) ?: DataResult.Error(Exception("GuestRepository not initialized"))
        }
        return try {
            usersCollection.document(uid).update("reminderFrequency", frequency).await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Checks if a username is available. Returns true if unique.
     */
    suspend fun isUsernameAvailable(username: String): DataResult<Boolean> {
        return try {
            val doc = usernamesCollection.document(username.lowercase()).get().await()
            DataResult.Success(!doc.exists())
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Transfers local guest data (logs and stats) to a newly created authenticated account.
     */
    suspend fun createAccountWithGuestData(newUid: String, newRecord: UserRecord): DataResult<Unit> {
        return try {
            if (guestRepository == null) return DataResult.Error(Exception("Guest repository not initialized"))
            
            val guestLogsRes = guestRepository.getFluidLogs()
            if (guestLogsRes is DataResult.Error) return DataResult.Error(guestLogsRes.exception)
            val guestLogs = (guestLogsRes as DataResult.Success).data
            
            val guestRecord = guestRepository.guestUserRecord
            
            val batch = db.batch()
            val userRef = usersCollection.document(newUid)
            
            val finalRecord = newRecord.copy(
                streak = guestRecord?.streak ?: 0,
                highestStreak = guestRecord?.highestStreak ?: 0,
                totalRingsClosed = guestRecord?.totalRingsClosed ?: 0,
                lastRingClosedDate = guestRecord?.lastRingClosedDate ?: "",
                totalFluidDrankAllTime = guestLogs.sumOf { it.amount }
            )
            
            batch.set(userRef, finalRecord)
            guestLogs.forEach { log ->
                val logRef = userRef.collection("fluid_logs").document(log.id.toString())
                batch.set(logRef, log)
            }
            batch.commit().await()
            guestRepository.clearGuestData()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Tracks if there are unsynced writes to Firestore for the given user.
     */
    fun hasPendingWritesFlow(uid: String): Flow<Boolean> {
        if (uid == "GUEST") return kotlinx.coroutines.flow.flowOf(false)
        return callbackFlow {
            val registration = usersCollection.document(uid)
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) {
                        trySend(true)
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.metadata?.hasPendingWrites() == true)
                }
            awaitClose { registration.remove() }
        }
    }

    /**
     * Uploads the user's profile picture. Supports local saving for offline mode and ImgBB sync for online mode.
     */
    suspend fun uploadProfilePicture(uid: String, uri: Uri, isOnline: Boolean = true): DataResult<String> {
        return try {
            if (context == null) return DataResult.Error(Exception("Context is required for image processing"))

            val imageBytes = if (uri.scheme == "file") {
                val file = java.io.File(uri.path!!)
                if (file.exists()) file.readBytes() else null
            } else {
                ImageUtils.uriToCompressedByteArray(context, uri, 256)
            } ?: return DataResult.Error(Exception("Failed to process image"))

            val localFile = ProfilePhotoManager.savePhotoLocally(context, imageBytes)
                ?: return DataResult.Error(Exception("Failed to save photo locally"))

            if (!isOnline) {
                return DataResult.Success(localFile.absolutePath)
            }

            val base64ForApi = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val uploadResult = ImageUploadService.uploadToImgBB(base64ForApi)

            if (uploadResult.isFailure) {
                return DataResult.Success(localFile.absolutePath)
            }

            val imageUrl = uploadResult.getOrThrow()

            if (uid != "GUEST") {
                usersCollection.document(uid).update("profilePictureUrl", imageUrl).await()
            }

            ProfilePhotoManager.deleteLocalPhoto(context)
            DataResult.Success(imageUrl)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Syncs a locally saved profile photo to the cloud once connectivity is restored.
     */
    suspend fun syncPendingProfilePhoto(uid: String): DataResult<String> {
        return try {
            if (context == null) return DataResult.Error(Exception("Context is required"))

            val localFile = ProfilePhotoManager.getLocalPhotoFile(context)
                ?: return DataResult.Error(Exception("No local photo found to sync"))

            val imageBytes = localFile.readBytes()
            val base64ForApi = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            val uploadResult = ImageUploadService.uploadToImgBB(base64ForApi)
            if (uploadResult.isFailure) {
                return DataResult.Error(uploadResult.exceptionOrNull() as? Exception ?: Exception("Upload failed"))
            }

            val imageUrl = uploadResult.getOrThrow()
            if (uid != "GUEST") {
                usersCollection.document(uid).update("profilePictureUrl", imageUrl).await()
            }
            ProfilePhotoManager.deleteLocalPhoto(context)
            DataResult.Success(imageUrl)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Queues user profile updates to be sent to Firestore. 
     * Optimistically relies on Firestore's offline persistence.
     */
    fun queueOfflineUserRecordUpdate(uid: String, record: UserRecord) {
        if (uid == "GUEST" || uid.isEmpty()) return
        
        val userRef = usersCollection.document(uid)
        val updates = mutableMapOf<String, Any>(
            "weight" to record.weight,
            "height" to record.height,
            "age" to record.age,
            "sex" to record.sex,
            "activity" to record.activity,
            "environment" to record.environment
        )
        
        if (record.profilePictureUrl.isEmpty()) {
            updates["profilePictureUrl"] = ""
        }
        userRef.update(updates)
    }

    /**
     * Removes the profile picture from both local storage and the cloud.
     */
    suspend fun removeProfilePicture(uid: String): DataResult<Unit> {
        return try {
            context?.let { ProfilePhotoManager.deleteLocalPhoto(it) }

            if (uid == "GUEST") return DataResult.Success(Unit)
            
            usersCollection.document(uid).update("profilePictureUrl", "").await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Manually resets the user's streak to zero.
     */
    suspend fun resetStreak(uid: String): DataResult<Unit> {
        if (uid == "GUEST") {
            return guestRepository?.resetStreak() ?: DataResult.Error(Exception("GuestRepository not initialized"))
        }
        return try {
            usersCollection.document(uid).update("streak", 0).await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }
}
