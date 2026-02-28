package com.shubham.todojetpackcompose.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shubham.todojetpackcompose.common.TODO_TABLE_NAME
import com.shubham.todojetpackcompose.data.models.TodoItemEntity
import com.shubham.todojetpackcompose.data.repo.TodoDataSource
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoRoomDao : TodoDataSource {

    @Query("SELECT * FROM $TODO_TABLE_NAME ORDER BY id DESC")
    override fun fetchAllTodoItems(): Flow<List<TodoItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun addTodoItem(todoItemEntity: TodoItemEntity): Long

    @Query("DELETE FROM $TODO_TABLE_NAME WHERE id = :todoId")
    override suspend fun deleteTodoItem(todoId: Long): Int

    @Update
    override suspend fun updateTodoItem(todoItemEntity: TodoItemEntity): Int

    @Query("SELECT * FROM $TODO_TABLE_NAME WHERE id = :todoId")
    override suspend fun fetchIdTodoItem(todoId: Long): TodoItemEntity?
}
