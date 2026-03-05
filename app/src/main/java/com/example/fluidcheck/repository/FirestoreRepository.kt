package com.example.fluidcheck.repository

import com.example.fluidcheck.model.FluidLog
import com.example.fluidcheck.model.QuickAddConfig
import com.example.fluidcheck.model.UserRecord
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.fluidcheck.util.ImageUploadService
import com.example.fluidcheck.util.ImageUtils
import com.example.fluidcheck.util.ProfilePhotoManager
import java.text.SimpleDateFormat
import java.util.*

class FirestoreRepository(private val context: Context? = null) {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val usernamesCollection = db.collection("usernames")
    
    // Delegate for guest operations
    private val guestRepository: GuestRepository? = context?.let { GuestRepository(it) }

    suspend fun saveUserRecord(uid: String, record: UserRecord): Result<Unit> {
        return try {
            if (uid.isEmpty()) return Result.failure(Exception("UID cannot be empty"))
            if (uid == "GUEST") {
                guestRepository?.saveUserRecord(record)
                return Result.success(Unit)
            }
            
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
            
            // Guest username update handled by saveUserRecord delegation if needed, 
            // but for explicitly updating username:
            if (uid == "GUEST") {
                val currentRecord = getUserRecord(uid)
                if (currentRecord != null) {
                    saveUserRecord(uid, currentRecord.copy(username = newUsername))
                }
                return Result.success(Unit)
            }

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
        if (username == "Guest") return null
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
                val record = mapDocumentToUserRecord(document)
                trySend(record)
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
        }
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
            dailyGoal = document.getLong("dailyGoal")?.toInt()
        )
    }

    suspend fun getUserRecord(uid: String): UserRecord? {
        if (uid == "GUEST") {
            return guestRepository?.guestUserRecord
        }
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
        if (uid == "GUEST") {
            guestRepository?.saveDailyGoal(goal)
            return Result.success(Unit)
        }
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
        if (uid == "GUEST") {
            guestRepository?.saveFluidLog(log)
            return Result.success(Unit)
        }
        return try {
            val batch = db.batch()
            val userRef = usersCollection.document(uid)
            val logRef = userRef.collection("fluid_logs").document(log.id.toString())
            
            batch.update(userRef, "totalFluidDrankAllTime", FieldValue.increment(log.amount.toLong()))
            batch.set(logRef, log)
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFluidLog(uid: String, oldLog: FluidLog, newLog: FluidLog): Result<Unit> {
        if (uid == "GUEST") {
            guestRepository?.updateFluidLog(oldLog, newLog)
            return Result.success(Unit)
        }
        return try {
            val amountDiff = (newLog.amount - oldLog.amount).toLong()
            val batch = db.batch()
            val userRef = usersCollection.document(uid)
            val logRef = userRef.collection("fluid_logs").document(newLog.id.toString())
            
            batch.update(userRef, "totalFluidDrankAllTime", FieldValue.increment(amountDiff))
            batch.set(logRef, newLog)
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFluidLog(uid: String, log: FluidLog): Result<Unit> {
        if (uid == "GUEST") {
            guestRepository?.deleteFluidLog(log)
            return Result.success(Unit)
        }
        return try {
            val batch = db.batch()
            val userRef = usersCollection.document(uid)
            val logRef = userRef.collection("fluid_logs").document(log.id.toString())
            
            batch.update(userRef, "totalFluidDrankAllTime", FieldValue.increment(-log.amount.toLong()))
            batch.delete(logRef)
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getFluidLogsFlow(uid: String): Flow<List<FluidLog>> {
        if (uid == "GUEST") {
            return guestRepository?.guestLogsFlow ?: kotlinx.coroutines.flow.flowOf(emptyList())
        }
        return callbackFlow {
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

    fun getTodayFluidLogsFlow(uid: String): Flow<List<FluidLog>> {
        if (uid == "GUEST") {
            return guestRepository?.getTodayFluidLogsFlow() ?: kotlinx.coroutines.flow.flowOf(emptyList())
        }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())
        return callbackFlow {
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
        if (uid == "GUEST") {
            guestRepository?.updateQuickAddConfig(config)
            return Result.success(Unit)
        }
        return try {
            usersCollection.document(uid).update("quickAddConfig", config).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markGoalAchievedToday(uid: String, todayDate: String, yesterdayDate: String): Result<Unit> {
        if (uid == "GUEST") {
            val success = guestRepository?.markGoalAchievedToday(todayDate, yesterdayDate) ?: false
            return if (success) Result.success(Unit) else Result.failure(Exception("Already achieved today or GuestRepository missing"))
        }

        return try {
            // Use a read first to determine streak logic, then a batch write.
            // The read will come from cache if offline, and the write will queue.
            val userRef = usersCollection.document(uid)
            val userDoc = userRef.get().await()
            
            val currentLastRingDate = userDoc.getString("lastRingClosedDate")
            if (currentLastRingDate == todayDate) {
                // Already closed today, avoid redundant write
                return Result.success(Unit)
            }
            
            val currentStreak = (userDoc.getLong("streak") ?: 0L).toInt()
            val highestStreak = (userDoc.getLong("highestStreak") ?: 0L).toInt()
            
            val newStreak = if (currentLastRingDate == yesterdayDate) {
                currentStreak + 1
            } else {
                1
            }
            
            val updates = mutableMapOf<String, Any>(
                "streak" to newStreak,
                "lastRingClosedDate" to todayDate,
                "totalRingsClosed" to FieldValue.increment(1)
            )
            
            if (newStreak > highestStreak) {
                updates["highestStreak"] = newStreak
            }
            
            userRef.update(updates).await()
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
        if (uid == "GUEST") {
            guestRepository?.updateNotificationsEnabled(enabled)
            return Result.success(Unit)
        }
        return try {
            usersCollection.document(uid).update("notificationsEnabled", enabled).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateReminderFrequency(uid: String, frequency: String): Result<Unit> {
        if (uid == "GUEST") {
            guestRepository?.updateReminderFrequency(frequency)
            return Result.success(Unit)
        }
        return try {
            usersCollection.document(uid).update("reminderFrequency", frequency).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Removed incrementTotalRingsClosed: Logic moved to markGoalAchievedToday.

    suspend fun softDeleteUser(uid: String): Result<Unit> {
        return try {
            usersCollection.document(uid).update("deleted", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun softDeleteUsers(uids: Set<String>): Result<Unit> {
        if (uids.isEmpty()) return Result.success(Unit)
        return try {
            val batch = db.batch()
            uids.forEach { uid ->
                val userRef = usersCollection.document(uid)
                batch.update(userRef, "deleted", true)
            }
            batch.commit().await()
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

    suspend fun createAccountWithGuestData(newUid: String, newRecord: UserRecord): Result<Unit> {
        return try {
            if (guestRepository == null) return Result.failure(Exception("Guest repository not initialized"))
            
            val guestLogs = guestRepository.getFluidLogs()
            val guestRecord = guestRepository.guestUserRecord
            
            // Create an atomic batch write
            val batch = db.batch()
            val userRef = usersCollection.document(newUid)
            
            // Merge guest stats into the permanent record
            val finalRecord = newRecord.copy(
                streak = guestRecord?.streak ?: 0,
                highestStreak = guestRecord?.highestStreak ?: 0,
                totalRingsClosed = guestRecord?.totalRingsClosed ?: 0,
                lastRingClosedDate = guestRecord?.lastRingClosedDate ?: "",
                totalFluidDrankAllTime = guestLogs.sumOf { it.amount }
            )
            
            // 1. Save the new user document
            batch.set(userRef, finalRecord)
            
            // 2. Add all their previous logs to the new collection
            guestLogs.forEach { log ->
                val logRef = userRef.collection("fluid_logs").document(log.id.toString())
                batch.set(logRef, log)
            }
            
            // Commit all changes simultaneously
            batch.commit().await()
            
            // Clear local guest cache ONLY after the cloud says success
            guestRepository.clearGuestData()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Provides a Flow that tracks whether Firestore has pending writes for a given user.
     * True = data is waiting to be synced to the cloud.
     * False = all data is fully synced.
     */
    fun hasPendingWritesFlow(uid: String): Flow<Boolean> {
        if (uid == "GUEST") {
            return kotlinx.coroutines.flow.flowOf(false)
        }
        return callbackFlow {
            val registration = usersCollection.document(uid)
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) {
                        // If we can't connect to verify sync status (e.g. quota exceeded),
                        // it is safer to assume sync is pending/stuck rather than successful.
                        trySend(true)
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.metadata?.hasPendingWrites() == true)
                }
            awaitClose { registration.remove() }
        }
    }

    /**
     * Uploads a profile picture: compresses, saves locally, and uploads to ImgBB if online.
     * Returns the display URL (ImgBB HTTPS URL) or the local file path for offline display.
     */
    suspend fun uploadProfilePicture(uid: String, uri: Uri, isOnline: Boolean = true): Result<String> {
        return try {
            if (context == null) return Result.failure(Exception("Context is required for image processing"))

            // 1. Get image bytes — handle both file:// URIs (from uCrop) and content:// URIs
            val imageBytes = if (uri.scheme == "file") {
                // uCrop outputs file:// URIs — already cropped & compressed, just read bytes
                val file = java.io.File(uri.path!!)
                if (file.exists()) file.readBytes() else null
            } else {
                // content:// URI — needs processing
                ImageUtils.uriToCompressedByteArray(context, uri, 256)
            } ?: return Result.failure(Exception("Failed to process image"))

            // 2. Save locally for immediate display
            val localFile = ProfilePhotoManager.savePhotoLocally(context, imageBytes)
                ?: return Result.failure(Exception("Failed to save photo locally"))

            if (!isOnline) {
                // Offline: return local file path for UI display
                Log.d("FirestoreRepository", "Offline — photo saved locally, pending upload")
                return Result.success(localFile.absolutePath)
            }

            // 3. Online: upload to ImgBB
            val base64ForApi = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val uploadResult = ImageUploadService.uploadToImgBB(base64ForApi)

            if (uploadResult.isFailure) {
                // Upload failed but local copy exists — return local path as fallback
                Log.e("FirestoreRepository", "ImgBB upload failed, using local copy")
                return Result.success(localFile.absolutePath)
            }

            val imageUrl = uploadResult.getOrThrow()

            // 4. Store the HTTPS URL in Firestore
            if (uid != "GUEST") {
                usersCollection.document(uid).update("profilePictureUrl", imageUrl).await()
            }

            // 5. Clean up local file since we have the cloud URL now
            ProfilePhotoManager.deleteLocalPhoto(context)

            Log.d("FirestoreRepository", "Profile picture uploaded: $imageUrl")
            Result.success(imageUrl)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error uploading profile picture: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Syncs a pending local profile photo to ImgBB when connectivity is restored.
     * Called by the UI layer when network becomes available and a pending upload exists.
     */
    suspend fun syncPendingProfilePhoto(uid: String): Result<String> {
        return try {
            if (context == null) return Result.failure(Exception("Context is required"))

            val localFile = ProfilePhotoManager.getLocalPhotoFile(context)
                ?: return Result.failure(Exception("No local photo found to sync"))

            val imageBytes = localFile.readBytes()
            val base64ForApi = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            val uploadResult = ImageUploadService.uploadToImgBB(base64ForApi)
            if (uploadResult.isFailure) {
                return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Upload failed"))
            }

            val imageUrl = uploadResult.getOrThrow()

            if (uid != "GUEST") {
                usersCollection.document(uid).update("profilePictureUrl", imageUrl).await()
            }

            // Clean up local file
            ProfilePhotoManager.deleteLocalPhoto(context)

            Log.d("FirestoreRepository", "Pending photo synced: $imageUrl")
            Result.success(imageUrl)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Error syncing pending photo: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Queues an offline update for non-critical user profile fields (including profile picture removal).
     * This uses a fire-and-forget .update() which instantly writes to Firestore's local cache,
     * triggering local UI updates, and pushes to the cloud once connectivity is restored.
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
        
        // If the photo was explicitly removed offline, we should queue the empty URL to Firestore.
        // We DO NOT sync a local file:// path, as that shouldn't go to the cloud.
        if (record.profilePictureUrl.isEmpty()) {
            updates["profilePictureUrl"] = ""
        }
        
        // Fire and forget: Firestore offline persistence handles the queuing and immediate local cache update.
        userRef.update(updates)
    }

    suspend fun removeProfilePicture(uid: String): Result<Unit> {
        return try {
            // Clean up local cached photo
            context?.let { ProfilePhotoManager.deleteLocalPhoto(it) }

            if (uid == "GUEST") {
                return Result.success(Unit)
            }
            usersCollection.document(uid).update("profilePictureUrl", "").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
