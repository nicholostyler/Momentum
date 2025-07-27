package com.nicholostyler.momentum

import android.content.Context
import com.nicholostyler.momentum.repository.TodoRepository
import com.nicholostyler.momentum.sync.SyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideSyncManager(
        @ApplicationContext context: Context,
        repository: TodoRepository
    ): SyncManager {
        return SyncManager(context, repository)
    }
}