package com.shubham.todojetpackcompose.data.repo

import com.shubham.todojetpackcompose.data.models.TodoItemEntity
import kotlinx.coroutines.flow.Flow

interface TodoDataSource {
    fun fetchAllTodoItems(): Flow<List<TodoItemEntity>>
    suspend fun addTodoItem(todoItemEntity: TodoItemEntity): Long

    suspend fun deleteTodoItem(todoId: Long): Int

    suspend fun updateTodoItem(todoItemEntity: TodoItemEntity): Int
    suspend fun fetchIdTodoItem(todoId: Long): TodoItemEntity?
}
