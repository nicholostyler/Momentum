package com.nicholostyler.momentum.sync

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.nicholostyler.momentum.repository.TodoRepository
import com.nicholostyler.momentum.storage.LocalSyncStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SyncManager @Inject constructor(
    private val context: Context,
    private val repository: TodoRepository
) {
    private var listener: ListenerRegistration? = null

    init {
        Log.d("SyncManager", "Injected repository: $repository hash=${System.identityHashCode(repository)}")

    }
    fun startSyncListener(userId: String) {
        if (userId.isBlank()) {
            Log.w("SyncManager", "User ID is blank — skipping listener")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            ensureSyncStatusExists(userId)

            val syncRef = Firebase.firestore.document("users/$userId/syncStatus/main")
            Log.d("SyncManager", "Attaching listener to: ${syncRef.path}")

            withContext(Dispatchers.Main) {
                listener = syncRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("SyncManager", "Snapshot listener error:", error)
                        return@addSnapshotListener
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        Log.w("SyncManager", "Snapshot is null or does not exist")
                        return@addSnapshotListener
                    }

                    val serverLastUpdated = snapshot.getLong("lastUpdatedAt")
                    if (serverLastUpdated == null) {
                        Log.w("SyncManager", "Missing 'lastUpdatedAt' field in snapshot")
                        return@addSnapshotListener
                    }

                    Log.d("SyncManager", "Detected update: lastUpdatedAt = $serverLastUpdated")

                    CoroutineScope(Dispatchers.IO).launch {
                        val localLastFetched = LocalSyncStore.getLastFetched(context)
                        if (serverLastUpdated > localLastFetched) {
                            Log.d("SyncManager", "Data is stale — fetching todos")
                            repository.fetchTodos(userId)
                            LocalSyncStore.setLastFetched(context, System.currentTimeMillis())
                        } else {
                            Log.d("SyncManager", "Local data is up to date")
                            repository.loadTodosFromCache(userId)
                        }
                    }
                }
            }
        }
    }


    suspend fun ensureSyncStatusExists(userId: String) {
        val docRef = Firebase.firestore.document("users/$userId/syncStatus/main")
        val snapshot = docRef.get().await()

        if (!snapshot.exists()) {
            docRef.set(
                mapOf("lastUpdatedAt" to System.currentTimeMillis())
            )
        }
    }

    suspend fun checkAndFetchIfStale(userId: String) {
        val syncRef = Firebase.firestore.document("users/$userId/syncStatus/main")
        val snapshot = syncRef.get().await()
        val serverLastUpdated = snapshot.getLong("lastUpdatedAt") ?: return

        val localLastFetched = LocalSyncStore.getLastFetched(context)
        if (serverLastUpdated > localLastFetched) {
            Log.d("checkandFetchIfStale", "Data is stale — fetching todos")

            repository.fetchTodos(userId)
            LocalSyncStore.setLastFetched(context, System.currentTimeMillis())
        } else {
            Log.d("checkandFetchIfStale", "Local data up to date.")

            repository.loadTodosFromCache(userId)
        }
    }






    fun stop() = listener?.remove()
}
