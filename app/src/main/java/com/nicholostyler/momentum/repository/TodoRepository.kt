package com.nicholostyler.momentum.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import com.nicholostyler.momentum.data.model.TodoItem
import com.nicholostyler.momentum.storage.LocalSyncStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.java

@Singleton

class TodoRepository @Inject constructor() {
    private val db = Firebase.firestore

    val todos = MutableStateFlow<List<TodoItem>>(emptyList())

    init {
        Log.d("Repository", "Created TodoRepository: $this hash=${System.identityHashCode(this)}")

    }

    suspend fun fetchTodos(userId: String) {
        val snapshot = db.collection("users/$userId/todos").get().await()
        Log.d("Firestore", "Fetched ${snapshot.size()} todos")
        val parsed = snapshot.documents.mapNotNull { doc ->
            doc.toObject(TodoItem::class.java)?.copy(id = doc.id)
        }
        Log.d("Firestore", "Successfully parsed ${todos.value.size} todos")

        withContext (Dispatchers.Main){
            todos.value = parsed

        }
    }

    suspend fun updateTodo(userId: String, todo: TodoItem) {
        db.collection("users/$userId/todos").document(todo.id).set(todo).await()

        todos.value = todos.value.map { if (it.id == todo.id) todo else it }
        updateSyncStatus(userId)
    }

    private suspend fun updateSyncStatus(userId: String) {
        db.document("users/$userId/syncStatus/main")
            .set(mapOf("lastUpdatedAt" to System.currentTimeMillis()), SetOptions.merge())
            .await()
    }

    suspend fun addTodo(userId: String, todo: TodoItem) {
        val docRef = db.collection("users/$userId/todos")
            .add(todo)
            .await()

        val savedTodo = todo.copy(id = docRef.id)

        todos.value = todos.value + savedTodo

        updateSyncStatus(userId)
    }

    suspend fun removeTodo(userId: String, todo: TodoItem ) {
        db.collection("users/$userId/todos")
            .document(todo.id)
            .delete()
            .await()
        todos.value = todos.value.filterNot { it.id == todo.id }

        updateSyncStatus(userId)
    }

    suspend fun loadTodosFromCache(userId: String) {
        val snapshot = db
            .collection("users/$userId/todos")
            .get(Source.CACHE) //  only pull from local cache
            .await()

        todos.value = snapshot.documents.mapNotNull { doc ->
            doc.toObject(TodoItem::class.java)?.copy(id = doc.id)
        }
    }



}
