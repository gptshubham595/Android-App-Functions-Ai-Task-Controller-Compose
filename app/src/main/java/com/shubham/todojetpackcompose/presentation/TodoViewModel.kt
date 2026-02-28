package com.shubham.todojetpackcompose.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shubham.todojetpackcompose.common.Utils
import com.shubham.todojetpackcompose.domain.models.TodoItem
import com.shubham.todojetpackcompose.domain.usecases.AddTodoItemUseCase
import com.shubham.todojetpackcompose.domain.usecases.DeleteTodoItemUseCase
import com.shubham.todojetpackcompose.domain.usecases.GetTodoListUseCase
import com.shubham.todojetpackcompose.domain.usecases.UpdateTodoItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodoUiState(
    val todoList: List<TodoItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val getTodoListUseCase: GetTodoListUseCase,
    private val addTodoItemUseCase: AddTodoItemUseCase,
    private val deleteTodoItemUseCase: DeleteTodoItemUseCase,
    private val updateTodoItemUseCase: UpdateTodoItemUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodoUiState())
    val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow()

    init {
        observeTodoList()
    }

    private fun observeTodoList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getTodoListUseCase()) {
                is Utils.Either.Success -> {
                    result.data.collectLatest { list ->
                        _uiState.update { it.copy(todoList = list, isLoading = false) }
                    }
                }

                is Utils.Either.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.exception.message)
                    }
                }
            }
        }
    }

    fun addTodoItem(task: String) {
        if (task.isBlank()) return
        val newItem = TodoItem(
            id = System.currentTimeMillis(),
            task = task.trim(),
            status = Utils.TodoStatus.PENDING.name,
        )
        viewModelScope.launch {
            when (val result = addTodoItemUseCase(newItem)) {
                is Utils.Either.Success -> {
                    result.data.collect {
                        _uiState.update { state -> state.copy(successMessage = "Task added!") }
                    }
                }

                is Utils.Either.Error -> {
                    _uiState.update { it.copy(error = result.exception.message) }
                }
            }
        }
    }

    fun deleteTodoItem(todoId: Long) {
        viewModelScope.launch {
            when (val result = deleteTodoItemUseCase(todoId)) {
                is Utils.Either.Success -> {
                    result.data.collect {
                        _uiState.update { it.copy(successMessage = "Task deleted.") }
                    }
                }

                is Utils.Either.Error -> {
                    _uiState.update { it.copy(error = result.exception.message) }
                }
            }
        }
    }

    fun toggleTodoStatus(todoItem: TodoItem) {
        val updatedStatus = if (todoItem.status == Utils.TodoStatus.PENDING.name)
            Utils.TodoStatus.COMPLETED.name
        else
            Utils.TodoStatus.PENDING.name

        val updatedItem = todoItem.copy(status = updatedStatus)
        viewModelScope.launch {
            when (val result = updateTodoItemUseCase(updatedItem)) {
                is Utils.Either.Success -> {
                    result.data.collect {
                        val message = if (updatedStatus == Utils.TodoStatus.COMPLETED.name) {
                            "Task completed."
                        } else {
                            "Task moved back to pending."
                        }
                        _uiState.update { it.copy(successMessage = message) }
                    }
                }

                is Utils.Either.Error -> {
                    _uiState.update { it.copy(error = result.exception.message) }
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(successMessage = null, error = null) }
    }
}
