package com.shubham.todojetpackcompose.data.repo

import com.shubham.todojetpackcompose.common.Utils.Either
import com.shubham.todojetpackcompose.data.mapper.toData
import com.shubham.todojetpackcompose.data.mapper.toDomain
import com.shubham.todojetpackcompose.domain.models.TodoItem
import com.shubham.todojetpackcompose.domain.repo.TodoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TodoRepositoryImpl @Inject constructor(
    private val todoDao: TodoDataSource,
) : TodoRepository {
    override suspend fun getTodoList(): Either<Exception, Flow<List<TodoItem>>> {
        return try {
            Either.Success(
                todoDao.fetchAllTodoItems()
                    .map { entities -> entities.map { entity -> entity.toDomain() } }
                    .flowOn(Dispatchers.IO)
            )
        } catch (e: Exception) {
            Either.Error(e)
        }
    }

    override suspend fun addTodoItem(todoItem: TodoItem): Either<Exception, Flow<Long>> {
        return try {
            Either.Success(
                flow {
                    emit(todoDao.addTodoItem(todoItem.toData()))
                }.flowOn(Dispatchers.IO)
            )
        } catch (e: Exception) {
            Either.Error(e)
        }
    }

    override suspend fun deleteTodoItem(todoId: Long): Either<Exception, Flow<Int>> {
        return try {
            Either.Success(flow { emit(todoDao.deleteTodoItem(todoId)) }.flowOn(Dispatchers.IO))
        } catch (e: Exception) {
            Either.Error(e)
        }
    }

    override suspend fun updateTodoItem(todoItem: TodoItem): Either<Exception, Flow<Int>> {
        return try {
            Either.Success(
                flow { emit(todoDao.updateTodoItem(todoItem.toData())) }.flowOn(
                    Dispatchers.IO
                )
            )
        } catch (e: Exception) {
            Either.Error(e)
        }
    }
}
