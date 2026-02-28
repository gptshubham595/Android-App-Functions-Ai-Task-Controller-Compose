package com.shubham.todoAgent.presentation

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.shubham.todoAgent.core.appfunctions.toFunctionDeclarations
import com.shubham.todoAgent.data.executor.GenericFunctionExecutor
import com.shubham.todoAgent.domain.model.DataType
import com.shubham.todoAgent.domain.model.FunctionDeclaration
import com.shubham.todoAgent.domain.model.Schema
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AgentViewModel"
private const val TOOL_PACKAGE = "com.shubham.todojetpackcompose"
private const val EXECUTE_APP_FUNCTIONS_PERMISSION = "android.permission.EXECUTE_APP_FUNCTIONS"
private const val EXECUTE_APP_ACTION_PERMISSION = "android.permission.EXECUTE_APP_ACTION"

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val manager: AppFunctionManager,
    private val gson: Gson,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val executor = GenericFunctionExecutor(manager, gson)

    private val functionMetadataMap =
        MutableStateFlow<Map<FunctionDeclaration, AppFunctionMetadata>>(emptyMap())

    private val _functions = MutableStateFlow<List<FunctionDeclaration>>(emptyList())
    val functions: StateFlow<List<FunctionDeclaration>> = _functions.asStateFlow()

    private val _result = MutableStateFlow("Tap a function to execute")
    val result: StateFlow<String> = _result.asStateFlow()

    private val _executionNotice = MutableStateFlow(buildExecutionNotice())
    val executionNotice: StateFlow<String> = _executionNotice.asStateFlow()

    init {
        observeToolFunctionsWithManager()
    }

    private fun observeToolFunctionsWithManager() {
        val spec = AppFunctionSearchSpec(packageNames = setOf(TOOL_PACKAGE))
        manager.observeAppFunctions(spec)
            .catch { e ->
                Log.e(TAG, "observeAppFunctions failed", e)
                _result.value = "Manager discovery failed: ${e.message}"
                _functions.value = emptyList()
            }
            .onEach { packages ->
                val pkg = packages.firstOrNull()
                if (pkg == null) {
                    _result.value = "No AppFunctions discovered for $TOOL_PACKAGE via manager."
                    _functions.value = emptyList()
                } else {
                    val map = pkg.appFunctions.toFunctionDeclarations()
                    functionMetadataMap.value = map
                    _functions.value = map.keys.toList().sortedBy { it.shortName }
                    _result.value = "Discovered ${map.size} functions (via manager)"
                }
            }
            .launchIn(viewModelScope)
    }

    fun execute(
        function: FunctionDeclaration,
        rawArguments: Map<String, String> = emptyMap(),
    ) {
        val metadata = functionMetadataMap.value[function]
            ?: run {
                _result.value = "Missing metadata for ${function.shortName}"
                return
            }

        val args = runCatching { buildArguments(function, rawArguments) }
            .getOrElse { error ->
                _result.value = "Error: ${error.message ?: error}"
                return
            }

        val fixedFunction = function.copy(name = metadata.id)
        Log.i(TAG, "Executing: short=${function.shortName}, name=${function.name}")
        Log.i(TAG, "Executing: short=${fixedFunction.shortName}, name=${fixedFunction.name}")

        viewModelScope.launch {
            val res = executor.executeAppFunction(
                targetPackageName = TOOL_PACKAGE,
                appFunctionMetadata = metadata,
                functionDeclaration = fixedFunction,
                arguments = args,
            )
            _result.value = res.fold(
                onSuccess = { gson.toJson(it) },
                onFailure = { error ->
                    listOfNotNull(
                        "Error: ${error.message ?: error}",
                        buildExecutionHint(error),
                    ).joinToString("\n\n")
                },
            )
        }
    }

    private fun buildArguments(
        function: FunctionDeclaration,
        rawArguments: Map<String, String>,
    ): Map<String, JsonElement> {
        val schema = function.parameters ?: return emptyMap()
        return schema.properties.mapNotNull { (name, parameterSchema) ->
            val rawValue = rawArguments[name]?.trim().orEmpty()
            when {
                rawValue.isBlank() && name in schema.required && !parameterSchema.nullable -> {
                    throw IllegalArgumentException("Missing required parameter: $name")
                }

                rawValue.isBlank() -> null
                else -> name to parseArgument(rawValue, parameterSchema)
            }
        }.toMap()
    }

    private fun parseArgument(rawValue: String, schema: Schema): JsonElement = when (schema.type) {
        DataType.STRING -> JsonPrimitive(rawValue)
        DataType.INT -> JsonPrimitive(
            rawValue.toIntOrNull()
                ?: throw IllegalArgumentException("Expected an integer value.")
        )

        DataType.LONG -> JsonPrimitive(
            rawValue.toLongOrNull()
                ?: throw IllegalArgumentException("Expected a long value.")
        )

        DataType.FLOAT -> JsonPrimitive(
            rawValue.toFloatOrNull()
                ?: throw IllegalArgumentException("Expected a float value.")
        )

        DataType.DOUBLE -> JsonPrimitive(
            rawValue.toDoubleOrNull()
                ?: throw IllegalArgumentException("Expected a double value.")
        )

        DataType.BOOLEAN -> when (rawValue.lowercase()) {
            "true" -> JsonPrimitive(true)
            "false" -> JsonPrimitive(false)
            else -> throw IllegalArgumentException("Expected 'true' or 'false'.")
        }

        DataType.OBJECT,
        DataType.ARRAY,
        -> JsonParser.parseString(rawValue)

        DataType.UNIT,
        DataType.UNSPECIFIED,
        -> JsonNull.INSTANCE
    }

    private fun buildExecutionHint(error: Throwable): String? {
        if (hasPermission(EXECUTE_APP_FUNCTIONS_PERMISSION)) {
            return null
        }

        return when {
            error.message?.contains("not available under", ignoreCase = true) == true ->
                "The caller app still does not hold android.permission.EXECUTE_APP_FUNCTIONS. On Android 16 that permission is not granted to a normal APK, so discovery can work while cross-app execution still fails."

            else ->
                "Permission status: ${buildExecutionNotice()}. Requesting these permissions in the manifest is not enough on a normal install."
        }
    }

    private fun buildExecutionNotice(): String =
        "EXECUTE_APP_FUNCTIONS=${permissionStatus(EXECUTE_APP_FUNCTIONS_PERMISSION)}, " +
            "EXECUTE_APP_ACTION=${permissionStatus(EXECUTE_APP_ACTION_PERMISSION)}"

    private fun permissionStatus(permission: String): String =
        if (hasPermission(permission)) "granted" else "missing"

    private fun hasPermission(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}
