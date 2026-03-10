package com.example.fluidcheck.repository

import android.content.Context
import com.example.fluidcheck.model.FluidLog
import com.example.fluidcheck.util.DataResult
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository in charge of managing hydration logs.
 * Supports both authenticated cloud storage (Firestore) and local guest storage.
 */
class FluidLogRepository(private val context: Context? = null) {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    
    private val guestRepository: GuestRepository? = context?.let { GuestRepository(it) }

    /**
     * Persists a new hydration log entry.
     * 
     * Uses a Firestore batch to atomically update the log collection and 
     * increment the user's `totalFluidDrankAllTime` counter.
     *
     * @param uid The UID of the authenticated user or "GUEST".
     * @param log The [FluidLog] object to be saved.
     * @return [DataResult] indicating successful storage or failure reason.
     */
    suspend fun saveFluidLog(uid: String, log: FluidLog): DataResult<Unit> {
        if (uid == "GUEST") {
            return guestRepository?.saveFluidLog(log) ?: DataResult.Error(Exception("GuestRepository not initialized"))
        }
        return try {
            val batch = db.batch()
            val userRef = usersCollection.document(uid)
            val logRef = userRef.collection("fluid_logs").document(log.id.toString())
            
            batch.update(userRef, "totalFluidDrankAllTime", FieldValue.increment(log.amount.toLong()))
            batch.set(logRef, log)
            
            batch.commit().await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Updates an existing hydration log and adjusts the total intake count accordingly.
     */
    suspend fun updateFluidLog(uid: String, oldLog: FluidLog, newLog: FluidLog): DataResult<Unit> {
        if (uid == "GUEST") {
            return guestRepository?.updateFluidLog(oldLog, newLog) ?: DataResult.Error(Exception("GuestRepository not initialized"))
        }
        return try {
            val amountDiff = (newLog.amount - oldLog.amount).toLong()
            val batch = db.batch()
            val userRef = usersCollection.document(uid)
            val logRef = userRef.collection("fluid_logs").document(newLog.id.toString())
            
            batch.update(userRef, "totalFluidDrankAllTime", FieldValue.increment(amountDiff))
            batch.set(logRef, newLog)
            
            batch.commit().await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Deletes a hydration log and decrements the total intake count.
     */
    suspend fun deleteFluidLog(uid: String, log: FluidLog): DataResult<Unit> {
        if (uid == "GUEST") {
            return guestRepository?.deleteFluidLog(log) ?: DataResult.Error(Exception("GuestRepository not initialized"))
        }
        return try {
            val batch = db.batch()
            val userRef = usersCollection.document(uid)
            val logRef = userRef.collection("fluid_logs").document(log.id.toString())
            
            batch.update(userRef, "totalFluidDrankAllTime", FieldValue.increment(-log.amount.toLong()))
            batch.delete(logRef)
            
            batch.commit().await()
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Returns a real-time Flow of all hydration logs for the specified user.
     */
    fun getFluidLogsFlow(uid: String): Flow<DataResult<List<FluidLog>>> {
        if (uid == "GUEST") {
            return guestRepository?.guestLogsFlow?.map { DataResult.Success(it) } 
                ?: kotlinx.coroutines.flow.flowOf(DataResult.Error(Exception("GuestRepository not initialized")))
        }
        return callbackFlow {
            val registration = usersCollection.document(uid)
                .collection("fluid_logs")
                .orderBy("id")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(DataResult.Error(error))
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        trySend(DataResult.Success(snapshot.toObjects(FluidLog::class.java)))
                    }
                }
            awaitClose { registration.remove() }
        }
    }

    /**
     * Returns a real-time Flow of hydration logs specifically for the current day.
     */
    fun getTodayFluidLogsFlow(uid: String, specificDate: String? = null): Flow<DataResult<List<FluidLog>>> {
        val dateToQuery = specificDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())

        if (uid == "GUEST") {
            return guestRepository?.getTodayFluidLogsFlow(dateToQuery)?.map { DataResult.Success(it) }
                ?: kotlinx.coroutines.flow.flowOf(DataResult.Error(Exception("GuestRepository not initialized")))
        }
        
        return callbackFlow {
            val registration = usersCollection.document(uid)
                .collection("fluid_logs")
                .whereEqualTo("date", dateToQuery)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(DataResult.Error(error))
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        trySend(DataResult.Success(snapshot.toObjects(FluidLog::class.java).sortedBy { it.id }))
                    }
                }
            awaitClose { registration.remove() }
        }
    }

    /**
     * Performs a one-time fetch of all fluid logs for the specified user.
     */
    suspend fun getFluidLogs(uid: String): DataResult<List<FluidLog>> {
        if (uid == "GUEST") {
            return guestRepository?.getFluidLogs() ?: DataResult.Error(Exception("GuestRepository not initialized"))
        }
        return try {
            val querySnapshot = usersCollection.document(uid)
                .collection("fluid_logs")
                .orderBy("id")
                .get().await()
            DataResult.Success(querySnapshot.toObjects(FluidLog::class.java))
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }

    /**
     * Performs a one-time fetch of today's fluid logs.
     */
    suspend fun getTodayFluidLogs(uid: String): DataResult<List<FluidLog>> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())
        
        if (uid == "GUEST") {
            val res = guestRepository?.getFluidLogs()
            return if (res is DataResult.Success) {
                DataResult.Success(res.data.filter { it.date == today }.sortedBy { it.id })
            } else {
                res ?: DataResult.Error(Exception("GuestRepository not initialized"))
            }
        }

        return try {
            val querySnapshot = usersCollection.document(uid)
                .collection("fluid_logs")
                .whereEqualTo("date", today)
                .get().await()
            DataResult.Success(querySnapshot.toObjects(FluidLog::class.java).sortedBy { it.id })
        } catch (e: Exception) {
            DataResult.Error(e)
        }
    }
}
