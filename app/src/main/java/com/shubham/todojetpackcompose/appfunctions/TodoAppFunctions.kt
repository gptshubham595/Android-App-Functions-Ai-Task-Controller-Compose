package com.shubham.todojetpackcompose.appfunctions

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.service.AppFunction
import com.shubham.todojetpackcompose.appfunctions.schemas.AppFunctionTodoItem
import com.shubham.todojetpackcompose.appfunctions.schemas.TodoMutationResult
import com.shubham.todojetpackcompose.appfunctions.schemas.TodoStats
import com.shubham.todojetpackcompose.common.Utils
import com.shubham.todojetpackcompose.domain.models.TodoItem
import com.shubham.todojetpackcompose.domain.usecases.AddTodoItemUseCase
import com.shubham.todojetpackcompose.domain.usecases.DeleteTodoItemUseCase
import com.shubham.todojetpackcompose.domain.usecases.GetTodoListUseCase
import com.shubham.todojetpackcompose.domain.usecases.UpdateTodoItemUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private suspend fun GetTodoListUseCase.fetchAll(): List<TodoItem> =
    when (val result = invoke()) {
        is Utils.Either.Success -> result.data.first()
        is Utils.Either.Error -> throw result.exception
    }

private fun TodoItem.toAppFunctionItem(): AppFunctionTodoItem =
    AppFunctionTodoItem(
        id = id.toString(),
        task = task,
        status = status,
    )

class TodoAppFunctions @Inject constructor(
    private val getTodoListUseCase: GetTodoListUseCase,
    private val addTodoItemUseCase: AddTodoItemUseCase,
    private val deleteTodoItemUseCase: DeleteTodoItemUseCase,
    private val updateTodoItemUseCase: UpdateTodoItemUseCase,
) {
    /**
     * Creates and saves a new todo item with the given task description.
     * Use this when the user wants to add, create, or remember a new task or todo item.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @param task The task description to save. Must not be blank.
     * @return A [TodoMutationResult] indicating whether the task was saved successfully.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun addTodo(
        appFunctionContext: AppFunctionContext,
        task: String,
    ): TodoMutationResult {
        if (task.isBlank()) {
            throw AppFunctionInvalidArgumentException("Task description must not be blank.")
        }

        val newItem = TodoItem(
            id = System.currentTimeMillis(),
            task = task.trim(),
            status = Utils.TodoStatus.PENDING.name,
        )

        return when (val result = addTodoItemUseCase(newItem)) {
            is Utils.Either.Success -> {
                result.data.first()
                TodoMutationResult(
                    success = true,
                    message = "Task '${newItem.task}' added successfully.",
                )
            }

            is Utils.Either.Error -> {
                TodoMutationResult(
                    success = false,
                    message = result.exception.message ?: "Failed to add task.",
                )
            }
        }
    }

    /**
     * Returns every todo item currently stored in the app, both pending and completed.
     * Use this when the user wants to see, list, or review all their tasks.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @return A list of all [AppFunctionTodoItem] objects.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getAllTodos(appFunctionContext: AppFunctionContext): List<AppFunctionTodoItem> =
        getTodoListUseCase.fetchAll().map { it.toAppFunctionItem() }

    /**
     * Returns all todo items that are still pending.
     * Use this when the user asks what tasks are remaining, outstanding, or still to do.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @return A list of pending [AppFunctionTodoItem] objects.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getPendingTodos(appFunctionContext: AppFunctionContext): List<AppFunctionTodoItem> =
        getTodoListUseCase.fetchAll()
            .filter { it.status == Utils.TodoStatus.PENDING.name }
            .map { it.toAppFunctionItem() }

    /**
     * Returns all todo items that are already completed.
     * Use this when the user asks to review finished tasks.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @return A list of completed [AppFunctionTodoItem] objects.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getCompletedTodos(appFunctionContext: AppFunctionContext): List<AppFunctionTodoItem> =
        getTodoListUseCase.fetchAll()
            .filter { it.status == Utils.TodoStatus.COMPLETED.name }
            .map { it.toAppFunctionItem() }

    /**
     * Marks the todo with the given ID as completed.
     * Use this when the user says they finished or completed a specific task.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @param todoId The unique ID of the todo to complete.
     * @return A [TodoMutationResult] indicating whether the todo was updated.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun completeTodo(
        appFunctionContext: AppFunctionContext,
        todoId: String,
    ): TodoMutationResult = updateTodoStatus(
        todoId = todoId,
        expectedCurrentStatus = Utils.TodoStatus.PENDING.name,
        newStatus = Utils.TodoStatus.COMPLETED.name,
        alreadyInStateMessage = "Todo is already completed.",
        successMessage = { task -> "Todo '$task' marked as completed." },
    )

    /**
     * Moves a completed todo back to pending.
     * Use this when the user wants to reopen or uncomplete a task.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @param todoId The unique ID of the todo to reopen.
     * @return A [TodoMutationResult] indicating whether the todo was updated.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun reopenTodo(
        appFunctionContext: AppFunctionContext,
        todoId: String,
    ): TodoMutationResult = updateTodoStatus(
        todoId = todoId,
        expectedCurrentStatus = Utils.TodoStatus.COMPLETED.name,
        newStatus = Utils.TodoStatus.PENDING.name,
        alreadyInStateMessage = "Todo is already pending.",
        successMessage = { task -> "Todo '$task' moved back to pending." },
    )

    /**
     * Permanently removes the todo with the given ID from the list.
     * Use this when the user wants to delete, remove, or discard a specific task.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @param todoId The unique ID of the todo to delete.
     * @return A [TodoMutationResult] indicating whether the todo was deleted.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun deleteTodo(
        appFunctionContext: AppFunctionContext,
        todoId: String,
    ): TodoMutationResult {
        val existing = findTodo(todoId)
            ?: return TodoMutationResult(
                success = false,
                message = "No todo found with id '$todoId'.",
            )

        return when (val result = deleteTodoItemUseCase(existing.id)) {
            is Utils.Either.Success -> {
                result.data.first()
                TodoMutationResult(
                    success = true,
                    message = "Todo '${existing.task}' deleted successfully.",
                )
            }

            is Utils.Either.Error -> {
                TodoMutationResult(
                    success = false,
                    message = result.exception.message ?: "Failed to delete todo.",
                )
            }
        }
    }

    /**
     * Returns a summary of the current todo list: total count, completed count, and pending count.
     * Use this when the user asks how many tasks they have, how many are done, or wants a summary.
     *
     * @param appFunctionContext Execution context provided by the Android system. Do not pass manually.
     * @return A [TodoStats] object with counts for total, completed, and pending todos.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getTodoStats(appFunctionContext: AppFunctionContext): TodoStats {
        val todos = getTodoListUseCase.fetchAll()
        val completed = todos.count { it.status == Utils.TodoStatus.COMPLETED.name }
        return TodoStats(
            total = todos.size,
            completed = completed,
            pending = todos.size - completed,
        )
    }

    private suspend fun updateTodoStatus(
        todoId: String,
        expectedCurrentStatus: String,
        newStatus: String,
        alreadyInStateMessage: String,
        successMessage: (String) -> String,
    ): TodoMutationResult {
        val existing = findTodo(todoId)
            ?: return TodoMutationResult(
                success = false,
                message = "No todo found with id '$todoId'.",
            )

        if (existing.status != expectedCurrentStatus) {
            return TodoMutationResult(
                success = false,
                message = alreadyInStateMessage,
            )
        }

        return when (val result = updateTodoItemUseCase(existing.copy(status = newStatus))) {
            is Utils.Either.Success -> {
                result.data.first()
                TodoMutationResult(
                    success = true,
                    message = successMessage(existing.task),
                )
            }

            is Utils.Either.Error -> {
                TodoMutationResult(
                    success = false,
                    message = result.exception.message ?: "Failed to update todo.",
                )
            }
        }
    }

    private suspend fun findTodo(todoId: String): TodoItem? {
        val id = todoId.toLongOrNull()
            ?: throw AppFunctionInvalidArgumentException(
                "Invalid todoId '$todoId'. Use the numeric id returned by getAllTodos or getPendingTodos.",
            )

        return getTodoListUseCase.fetchAll().find { it.id == id }
    }
}
