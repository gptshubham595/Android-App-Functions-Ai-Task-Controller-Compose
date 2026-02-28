package com.shubham.todojetpackcompose

import android.app.Application
import androidx.appfunctions.service.AppFunctionConfiguration
import com.shubham.todojetpackcompose.appfunctions.TodoAppFunctionConfiguration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TodoApp :
    Application(),
    AppFunctionConfiguration.Provider {   // ← implement this interface

    /**
     * Hilt injects this after [onCreate] runs.
     * It carries all the use cases needed to build the AppFunction factories.
     */
    @Inject
    lateinit var todoAppFunctionConfiguration: TodoAppFunctionConfiguration

    /**
     * Called by the AppFunctions framework (not by you) when it needs to
     * know how to construct your AppFunction impl classes.
     *
     * Important: Hilt injection is complete by the time this is first called
     * because the system calls AppFunctionService AFTER Application.onCreate().
     */
    override val appFunctionConfiguration: AppFunctionConfiguration
        get() = todoAppFunctionConfiguration.build()
}