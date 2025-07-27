package com.nicholostyler.momentum.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

object LocalSyncStore {
    private val Context.dataStore by preferencesDataStore(name = "sync_store")
    private val LAST_FETCHED_KEY = longPreferencesKey("last_fetched_at")

    suspend fun setLastFetched(context: Context, timestamp: Long) {
        context.dataStore.edit { it[LAST_FETCHED_KEY] = timestamp }
    }

    suspend fun getLastFetched(context: Context): Long {
        return context.dataStore.data.first()[LAST_FETCHED_KEY] ?: 0L
    }
}
