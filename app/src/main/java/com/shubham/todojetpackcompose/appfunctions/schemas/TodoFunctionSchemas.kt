package com.shubham.todojetpackcompose.appfunctions.schemas

import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionStringValueConstraint

@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppFunctionTodoItem(
    /** Unique identifier for the todo (epoch millis as a numeric string). */
    val id: String,
    /** The task description text. */
    val task: String,
    /** Current status. Always one of "PENDING" or "COMPLETED". */
    @AppFunctionStringValueConstraint(enumValues = ["PENDING", "COMPLETED"])
    val status: String,
)

@AppFunctionSerializable(isDescribedByKDoc = true)
data class TodoMutationResult(
    /** True if the operation completed successfully, false otherwise. */
    val success: Boolean,
    /** Human-readable message describing the outcome. */
    val message: String,
)

@AppFunctionSerializable(isDescribedByKDoc = true)
data class TodoStats(
    /** Total number of todos (pending + completed). */
    val total: Int,
    /** Number of todos marked as COMPLETED. */
    val completed: Int,
    /** Number of todos still marked as PENDING. */
    val pending: Int,
)
