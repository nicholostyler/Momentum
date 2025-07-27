package com.nicholostyler.momentum.viewmodel

import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.nicholostyler.momentum.data.model.TodoItem
import com.nicholostyler.momentum.repository.TodoRepository
import com.nicholostyler.momentum.storage.LocalSyncStore
import com.nicholostyler.momentum.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val repository: TodoRepository,
    private val syncManager: SyncManager
) : ViewModel() {
    val todos = repository.todos.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        Log.d("ViewModel", "Injected repository: $repository hash=${System.identityHashCode(repository)}")
        val userId = Firebase.auth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch {
                repository.loadTodosFromCache(userId)
                syncManager.startSyncListener(userId)
            }
        }
    }

    fun refresh(userId: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            syncManager.checkAndFetchIfStale(userId)
            _isRefreshing.value = false
        }
    }

    fun toggleTodo(userId: String, todo: TodoItem) {
        viewModelScope.launch {
            val updated = todo.copy(completed = !todo.completed, updatedAt = System.currentTimeMillis())
            repository.updateTodo(userId, updated)
        }
    }

    fun addTodo(userId: String, todo: TodoItem) {
        viewModelScope.launch {
            val todo = TodoItem(
                title = todo.title,
                completed = false,
                updatedAt = System.currentTimeMillis(),
                dueDate = todo.dueDate
            )
            repository.addTodo(userId, todo)
        }
    }

    fun removeTodo(userId: String, todo: TodoItem) {
        viewModelScope.launch {
            repository.removeTodo(userId, todo)
        }
    }

}
