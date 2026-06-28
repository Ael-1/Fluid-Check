package com.example.fluidcheck.repository

import com.example.fluidcheck.model.FluidLog
import com.example.fluidcheck.model.QuickAddConfig
import com.example.fluidcheck.model.UserRecord
import com.example.fluidcheck.model.MissionPool
import com.example.fluidcheck.model.ActiveMission
import com.example.fluidcheck.model.MissionDifficulty
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
                // Write username mapping (Case-insensitive)
                if (record.username.isNotEmpty()) {
                    val usernameRef = usernamesCollection.document(record.username.lowercase())
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

            // Check if new username is available (allow if already assigned to this UID)
            val lowerNew = newUsername.lowercase()
            val lowerOld = oldUsername.lowercase()
            
            val newUsernameDoc = usernamesCollection.document(lowerNew).get().await()
            if (newUsernameDoc.exists() && newUsernameDoc.getString("uid") != uid) {
                return Result.failure(Exception("Username already taken"))
            }

            db.runBatch { batch ->
                // Delete old username mapping (Case-insensitive)
                if (lowerOld.isNotEmpty()) {
                    batch.delete(usernamesCollection.document(lowerOld))
                }
                // Add new username mapping (Case-insensitive)
                batch.set(usernamesCollection.document(lowerNew), mapOf("uid" to uid))
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
        if (username.equals("Guest", ignoreCase = true)) return null
        return try {
            val usernameDoc = usernamesCollection.document(username.lowercase()).get().await()
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
            role = document.getString("role") ?: "FREE USER",
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
            emailVerified = document.getBoolean("emailVerified") ?: false,
            
            // Gamification
            streakShields = document.getLong("streakShields")?.toInt() ?: 0,
            shieldFragments = document.getLong("shieldFragments")?.toInt() ?: 0,
            autoShieldEnabled = document.getBoolean("autoShieldEnabled") ?: false,
            missionBoardDate = document.getString("missionBoardDate") ?: "",
            boardMissionIds = document.get("boardMissionIds") as? List<String> ?: emptyList(),
            activeMissions = document.get("activeMissions") as? List<Map<String, Any>> ?: emptyList(),
            completedMissionsToday = document.get("completedMissionsToday") as? List<String> ?: emptyList(),
            abortedMissionsToday = document.get("abortedMissionsToday") as? List<String> ?: emptyList(),
            earnedBadges = (document.get("earnedBadges") as? Map<String, Number>)?.mapValues { it.value.toInt() } ?: emptyMap(),
            earnedMilestones = document.get("earnedMilestones") as? List<String> ?: emptyList()
        )
    }

    suspend fun getUserRecord(uid: String): UserRecord? {
        if (uid == "GUEST") {
            return guestRepository?.guestUserRecord
        }
        return try {
            val document = usersCollection.document(uid).get().await()
            if (document.exists()) {
                val record = mapDocumentToUserRecord(document)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("GMT+8") }
                val todayStr = sdf.format(Date())
                if (record.missionBoardDate != todayStr && record.role != "FREE USER") {
                    generateDailyMissionBoard(uid)
                    return mapDocumentToUserRecord(usersCollection.document(uid).get().await())
                }
                record
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

    suspend fun setAutoShieldEnabled(uid: String, enabled: Boolean): Result<Unit> {
        if (uid == "GUEST") return Result.success(Unit)
        return try {
            usersCollection.document(uid).update("autoShieldEnabled", enabled).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun commitBatch(batch: com.google.firebase.firestore.WriteBatch) {
        val task = batch.commit()
        val isOnline = context?.let { ctx ->
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val activeNet = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNet)
            caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } ?: true
        if (isOnline) {
            task.await()
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
            
            commitBatch(batch)
            
            // GAMIFICATION: Update active mission progress after log is saved
            checkMissionProgress(uid)
            
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
            
            commitBatch(batch)
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
            
            commitBatch(batch)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFluidLogs(uid: String, logs: List<FluidLog>): Result<Unit> {
        if (logs.isEmpty()) return Result.success(Unit)
        if (uid == "GUEST") {
            logs.forEach { guestRepository?.deleteFluidLog(it) }
            return Result.success(Unit)
        }
        return try {
            val batch = db.batch()
            val userRef = usersCollection.document(uid)
            val totalAmount = logs.sumOf { it.amount.toLong() }
            
            batch.update(userRef, "totalFluidDrankAllTime", FieldValue.increment(-totalAmount))
            logs.forEach { log ->
                val logRef = userRef.collection("fluid_logs").document(log.id.toString())
                batch.delete(logRef)
            }
            commitBatch(batch)
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

    suspend fun syncFluidLogsFromServer(uid: String): Result<Unit> {
        if (uid.isEmpty() || uid == "GUEST") return Result.success(Unit)
        return try {
            usersCollection.document(uid)
                .collection("fluid_logs")
                .get(com.google.firebase.firestore.Source.SERVER)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getTodayFluidLogsFlow(uid: String, specificDate: String? = null): Flow<List<FluidLog>> {
        if (uid == "GUEST") {
            return guestRepository?.getTodayFluidLogsFlow(specificDate) ?: kotlinx.coroutines.flow.flowOf(emptyList())
        }
        val dateToQuery = specificDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())
        
        return callbackFlow {
        val registration = usersCollection.document(uid)
            .collection("fluid_logs")
            .whereEqualTo("date", dateToQuery)
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

    /**
     * Resets streak to 0 if the user failed to close their ring yesterday.
     */
    suspend fun evaluateStreak(uid: String): Result<Unit> {
        if (uid == "GUEST") {
             guestRepository?.evaluateStreak()
             return Result.success(Unit)
        }
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }
        val todayStr = sdf.format(Date())
        
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"))
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(calendar.time)

        return try {
            val userRef = usersCollection.document(uid)
            val userDoc = userRef.get().await()
            if (!userDoc.exists()) return Result.success(Unit)
            
            val lastRingDate = userDoc.getString("lastRingClosedDate") ?: ""
            val dailyGoal = userDoc.getLong("dailyGoal")?.toInt() ?: 3000
            val databaseStreak = userDoc.getLong("streak")?.toInt() ?: 0
            val highestStreak = userDoc.getLong("highestStreak")?.toInt() ?: 0
            val totalRingsClosed = userDoc.getLong("totalRingsClosed")?.toInt() ?: 0
            val autoShieldEnabled = userDoc.getBoolean("autoShieldEnabled") ?: false
            val streakShields = userDoc.getLong("streakShields")?.toInt() ?: 0

            val lastEvaluatedDate = userDoc.getString("lastEvaluatedDate") ?: ""

            // If the last evaluated date was already today, we don't need to re-evaluate
            if (lastEvaluatedDate == todayStr) {
                return Result.success(Unit)
            }

            // Get yesterday's logs to check if the goal was met
            val yesterdayLogs = getLogsForDate(uid, yesterdayStr)
            val yesterdayIntake = yesterdayLogs.sumOf { it.amount }

            if (yesterdayIntake >= dailyGoal && dailyGoal > 0) {
                // Yesterday's goal was met! Increment/update streak
                val calendar2 = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"))
                calendar2.time = calendar.time
                calendar2.add(Calendar.DAY_OF_YEAR, -1)
                val dayBeforeYesterdayStr = sdf.format(calendar2.time)

                val newStreak = if (lastRingDate == dayBeforeYesterdayStr) {
                    databaseStreak + 1
                } else {
                    1
                }

                val updates = mutableMapOf<String, Any>(
                    "streak" to newStreak,
                    "lastRingClosedDate" to yesterdayStr,
                    "totalRingsClosed" to totalRingsClosed + 1
                )
                if (newStreak > highestStreak) {
                    updates["highestStreak"] = newStreak
                }
                userRef.update(updates).await()
            } else {
                // Yesterday's goal was not met
                if (autoShieldEnabled && streakShields > 0) {
                    // Auto-use a streak shield to protect streak
                    val updates = mapOf(
                        "streakShields" to streakShields - 1,
                        "lastRingClosedDate" to yesterdayStr // Fake closing the ring to prevent streak drop tomorrow
                    )
                    userRef.update(updates).await()
                } else {
                    // Reset streak to 0
                    userRef.update("streak", 0).await()
                }
            }
            // Gamification Daily Reset: Retain multi-day missions, clear single-day or expired
            val activeData = userDoc.get("activeMissions") as? List<Map<String, Any>> ?: emptyList()
            val currentTime = System.currentTimeMillis()
            val DAY_IN_MS = 24L * 60 * 60 * 1000
            
            val retainedMissions = activeData.filter { data ->
                val missionId = data["missionId"] as? String ?: return@filter false
                val completed = data["completed"] as? Boolean ?: false
                val aborted = data["aborted"] as? Boolean ?: false
                val acceptedAt = (data["acceptedAt"] as? Number)?.toLong() ?: currentTime
                
                val missionDef = com.example.fluidcheck.model.MissionPool.getMission(missionId)
                if (missionDef == null || completed || aborted) {
                    false
                } else {
                    val expirationTime = com.example.fluidcheck.model.getMissionExpirationTime(acceptedAt, missionDef)
                    currentTime <= expirationTime
                }
            }
            
            userRef.update(
                mapOf(
                    "activeMissions" to retainedMissions,
                    "completedMissionsToday" to emptyList<String>(),
                    "abortedMissionsToday" to emptyList<String>(),
                    "lastEvaluatedDate" to todayStr
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLogsForDate(uid: String, dateStr: String): List<FluidLog> {
        return try {
            val querySnapshot = usersCollection.document(uid)
                .collection("fluid_logs")
                .whereEqualTo("date", dateStr)
                .get().await()
            querySnapshot.toObjects(FluidLog::class.java)
        } catch (e: Exception) {
            emptyList()
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
            val userDoc = usersCollection.document(uid).get().await()
            val username = userDoc.getString("username")
            
            db.runBatch { batch ->
                batch.update(usersCollection.document(uid), "deleted", true) // Changed "isDeleted" to "deleted" for consistency
                // Remove username mapping so it can be reused if desired, or at least clean up
                if (!username.isNullOrEmpty()) {
                    batch.delete(usernamesCollection.document(username.lowercase()))
                }
            }.await()
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
            val doc = usernamesCollection.document(username.lowercase()).get().await()
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

    suspend fun resetStreak(uid: String): Result<Unit> {
        if (uid == "GUEST") {
            guestRepository?.resetStreak()
            return Result.success(Unit)
        }
        return try {
            usersCollection.document(uid).update("streak", 0).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPredictiveReminders(uid: String): Result<List<Map<String, Any>>> {
        if (uid.isEmpty() || uid == "GUEST") return Result.success(emptyList())
        return try {
            val snapshot = usersCollection.document(uid)
                .collection("predictive_reminders").get().await()
            val list = snapshot.documents.map { doc ->
                val map = doc.data?.toMutableMap() ?: mutableMapOf()
                map["id"] = doc.id
                map
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePredictiveReminders(uid: String, reminders: List<Map<String, Any>>): Result<Unit> {
        if (uid.isEmpty() || uid == "GUEST") return Result.success(Unit)
        return try {
            val subRef = usersCollection.document(uid).collection("predictive_reminders")
            val existing = subRef.get().await()
            db.runBatch { batch ->
                existing.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                reminders.forEach { reminder ->
                    val id = reminder["id"]?.toString() ?: UUID.randomUUID().toString()
                    val docRef = subRef.document(id)
                    batch.set(docRef, reminder)
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── GAMIFICATION: Mission Board Management ──
    suspend fun generateDailyMissionBoard(userId: String): Result<Unit> {
        if (userId == "GUEST" || userId.isEmpty()) return Result.success(Unit)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("GMT+8") }
        val todayStr = sdf.format(Date())
        return try {
            // Randomly select missions based on probabilities
            val selectedMissions = mutableListOf<String>()
            val pool = MissionPool.missions.toMutableList()
            pool.shuffle()
            for (i in 0 until 10) {
                if (pool.isEmpty()) break
                selectedMissions.add(pool.removeAt(0).id) // Simplified for now, should use weighted selection ideally
            }
            usersCollection.document(userId).update(
                mapOf(
                    "missionBoardDate" to todayStr,
                    "boardMissionIds" to selectedMissions,
                    // Keep multi-day active missions
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptMission(userId: String, missionId: String): Result<Unit> {
        if (userId == "GUEST" || userId.isEmpty()) return Result.success(Unit)
        return try {
            val userRef = usersCollection.document(userId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val activeData = snapshot.get("activeMissions") as? List<Map<String, Any>> ?: emptyList()
                if (activeData.size >= 5) throw Exception("Max active missions reached")
                
                val newMission = mapOf(
                    "missionId" to missionId,
                    "progress" to 0,
                    "completed" to false,
                    "aborted" to false,
                    "acceptedAt" to System.currentTimeMillis()
                )
                val newList = activeData + newMission
                transaction.update(userRef, "activeMissions", newList)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun abortMission(userId: String, missionId: String): Result<Unit> {
        if (userId == "GUEST" || userId.isEmpty()) return Result.success(Unit)
        return try {
            val userRef = usersCollection.document(userId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val activeData = snapshot.get("activeMissions") as? List<Map<String, Any>> ?: emptyList()
                val abortedList = snapshot.get("abortedMissionsToday") as? List<String> ?: emptyList()
                
                val newList = activeData.filter { it["missionId"] != missionId }
                
                transaction.update(userRef, mapOf(
                    "activeMissions" to newList,
                    "abortedMissionsToday" to abortedList + missionId
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun completeMission(userId: String, missionId: String): Result<Unit> {
        if (userId == "GUEST" || userId.isEmpty()) return Result.success(Unit)
        return try {
            val missionDef = MissionPool.getMission(missionId) ?: return Result.failure(Exception("Mission not found"))
            val userRef = usersCollection.document(userId)
            
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val activeData = snapshot.get("activeMissions") as? List<Map<String, Any>> ?: emptyList()
                val completedList = snapshot.get("completedMissionsToday") as? List<String> ?: emptyList()
                
                val newList = activeData.filter { it["missionId"] != missionId }
                
                val updates = mutableMapOf<String, Any>(
                    "activeMissions" to newList,
                    "completedMissionsToday" to completedList + missionId
                )
                
                // Add badge
                val earnedBadges = (snapshot.get("earnedBadges") as? Map<String, Number>)?.toMutableMap() ?: mutableMapOf()
                val currentCount = earnedBadges[missionDef.badgeId]?.toInt() ?: 0
                earnedBadges[missionDef.badgeId] = currentCount + 1
                updates["earnedBadges"] = earnedBadges
                
                // Roll for shield fragments
                if (Math.random() <= missionDef.difficulty.shieldFragmentChance) {
                    val currentFragments = (snapshot.getLong("shieldFragments") ?: 0).toInt()
                    val currentShields = (snapshot.getLong("streakShields") ?: 0).toInt()
                    if (currentFragments + 1 >= 10) {
                        updates["shieldFragments"] = currentFragments + 1 - 10
                        updates["streakShields"] = currentShields + 1
                    } else {
                        updates["shieldFragments"] = currentFragments + 1
                    }
                }
                
                transaction.update(userRef, updates)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkMissionProgress(userId: String) {
        if (userId == "GUEST" || userId.isEmpty()) return
        try {
            val userRef = usersCollection.document(userId)
            val userDoc = userRef.get().await()
            val activeData = userDoc.get("activeMissions") as? List<Map<String, Any>> ?: return
            if (activeData.isEmpty()) return
            
            val currentTime = System.currentTimeMillis()
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8")).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfToday = calendar.timeInMillis
            
            // Check if we have any active single-day missions
            val hasSingleDayMission = activeData.any { data ->
                val mId = data["missionId"] as? String ?: ""
                val mDef = MissionPool.getMission(mId)
                mDef != null && !mDef.isMultiDay
            }
            
            val earliestAcceptedAt = activeData.minOfOrNull { (it["acceptedAt"] as? Number)?.toLong() ?: currentTime } ?: currentTime
            val fetchStartTime = if (hasSingleDayMission) Math.min(earliestAcceptedAt, startOfToday) else earliestAcceptedAt

            // Fetch all logs since fetchStartTime
            val querySnapshot = userRef.collection("fluid_logs")
                .whereGreaterThanOrEqualTo("id", fetchStartTime)
                .get().await()
            val recentLogs = querySnapshot.toObjects(FluidLog::class.java)
            
            val dailyGoal = userDoc.getLong("dailyGoal")?.toInt() ?: 3000
            
            val updatedMissions = activeData.map { data ->
                val missionId = data["missionId"] as? String ?: return@map data
                val missionDef = MissionPool.getMission(missionId) ?: return@map data
                val acceptedAt = (data["acceptedAt"] as? Number)?.toLong() ?: currentTime
                
                // For daily/single-day missions, count logs from start of today. Otherwise, count since acceptedAt.
                val sinceTime = if (missionDef.isMultiDay) acceptedAt else startOfToday
                val missionLogs = recentLogs.filter { it.id >= sinceTime }
                
                val totalVolume = missionLogs.sumOf { it.amount }
                val logCount = missionLogs.size
                var progress = 0
                
                when (missionDef.targetType) {
                    com.example.fluidcheck.model.MissionTargetType.TOTAL_VOLUME -> progress = totalVolume
                    com.example.fluidcheck.model.MissionTargetType.LOG_COUNT -> progress = logCount
                    com.example.fluidcheck.model.MissionTargetType.GOAL_PERCENTAGE -> {
                        progress = if (dailyGoal > 0) (totalVolume * 100 / dailyGoal) else 0
                    }
                    com.example.fluidcheck.model.MissionTargetType.FLUID_TYPE_VOLUME -> {
                        progress = missionLogs.filter { it.type == missionDef.targetFluidType }.sumOf { it.amount }
                    }
                    com.example.fluidcheck.model.MissionTargetType.FLUID_VARIETY -> {
                        progress = missionLogs.map { it.type }.distinct().size
                    }
                    com.example.fluidcheck.model.MissionTargetType.SINGLE_LOG_AMOUNT -> {
                        progress = missionLogs.maxOfOrNull { it.amount } ?: 0
                    }
                    com.example.fluidcheck.model.MissionTargetType.TIME_WINDOW -> {
                        // Simplified check - just counting logs that match the requirement
                        progress = missionLogs.filter {
                            val hour = it.time.split(":")[0].toIntOrNull() ?: 12
                            val isAm = it.time.contains("AM")
                            val hr24 = if (isAm) (if (hour == 12) 0 else hour) else (if (hour == 12) 12 else hour + 12)
                            if (missionDef.isBeforeHour) hr24 < (missionDef.targetHour ?: 24) else hr24 >= (missionDef.targetHour ?: 0)
                        }.sumOf { it.amount }
                    }
                }
                
                val expiration = com.example.fluidcheck.model.getMissionExpirationTime(acceptedAt, missionDef)
                val isExpired = currentTime > expiration
                val failed = isExpired && progress < missionDef.targetValue
                
                data.toMutableMap().apply { 
                    put("progress", Math.min(progress, missionDef.targetValue))
                    put("failed", failed)
                }
            }
            
            userRef.update("activeMissions", updatedMissions).await()
        } catch (e: Exception) {
            // Ignore for now
        }
    }

    // ── GAMIFICATION: Inventory & Milestone Management ──
    suspend fun toggleAutoShield(userId: String, enabled: Boolean): Result<Unit> {
        if (userId == "GUEST" || userId.isEmpty()) return Result.success(Unit)
        return try {
            usersCollection.document(userId).update("autoShieldEnabled", enabled).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun awardMilestoneBadge(userId: String, badgeId: String): Result<Unit> {
        if (userId == "GUEST" || userId.isEmpty()) return Result.success(Unit)
        return try {
            db.runTransaction { transaction ->
                val userRef = usersCollection.document(userId)
                val snapshot = transaction.get(userRef)
                val milestones = snapshot.get("earnedMilestones") as? List<String> ?: emptyList()
                
                if (!milestones.contains(badgeId)) {
                    transaction.update(userRef, "earnedMilestones", milestones + badgeId)
                    val earnedBadges = (snapshot.get("earnedBadges") as? Map<String, Number>)?.toMutableMap() ?: mutableMapOf()
                    earnedBadges[badgeId] = 1
                    transaction.update(userRef, "earnedBadges", earnedBadges)
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upgradeToPremium(userId: String, duration: String): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (userId == "GUEST") {
            return@withContext Result.failure(Exception("Guest users cannot subscribe."))
        }
        try {
            val now = System.currentTimeMillis()
            val endDate = if (duration.equals("Yearly", ignoreCase = true)) {
                now + 31536000000L // 1 year
            } else {
                now + 2592000000L // 30 days
            }
            usersCollection.document(userId).update(
                mapOf(
                    "role" to "PREMIUM USER",
                    "premiumEndDate" to endDate
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun demoteFromPremium(userId: String): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (userId == "GUEST") return@withContext Result.success(Unit)
        try {
            usersCollection.document(userId).update(
                mapOf(
                    "role" to "FREE USER",
                    "premiumEndDate" to com.google.firebase.firestore.FieldValue.delete()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
