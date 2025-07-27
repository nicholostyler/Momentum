package com.nicholostyler.momentum.data.model

data class TodoItem(
    val id: String = "",
    val title: String = "",
    val completed: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null
)
