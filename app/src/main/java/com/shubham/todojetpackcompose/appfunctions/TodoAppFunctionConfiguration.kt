package com.shubham.todojetpackcompose.appfunctions

import androidx.appfunctions.service.AppFunctionConfiguration
import javax.inject.Inject
import javax.inject.Provider

class TodoAppFunctionConfiguration @Inject constructor(
    private val todoAppFunctionsProvider: Provider<TodoAppFunctions>,
) {
    fun build(): AppFunctionConfiguration =
        AppFunctionConfiguration.Builder()
            .addEnclosingClassFactory(TodoAppFunctions::class.java) {
                todoAppFunctionsProvider.get()
            }
            .build()
}
