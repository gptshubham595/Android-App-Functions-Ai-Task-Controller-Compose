package com.shubham.todoAgent.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shubham.todoAgent.domain.model.DataType
import com.shubham.todoAgent.domain.model.FunctionDeclaration
import com.shubham.todoAgent.domain.model.Schema
import com.shubham.todoAgent.presentation.AgentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val vm: AgentViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val functions by vm.functions.collectAsStateWithLifecycle()
            val result by vm.result.collectAsStateWithLifecycle()
            val executionNotice by vm.executionNotice.collectAsStateWithLifecycle()
            var selectedFunction by remember { mutableStateOf<FunctionDeclaration?>(null) }

            MaterialTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("ComposeTodo Agent") },
                        )
                    },
                ) { paddingValues ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            StatusCard(
                                title = "Permission status",
                                body = executionNotice,
                            )
                        }

                        item {
                            StatusCard(
                                title = "Last result",
                                body = result,
                            )
                        }

                        items(functions, key = { it.name }) { function ->
                            FunctionCard(
                                function = function,
                                onRun = {
                                    if (function.parameters?.properties.orEmpty().isEmpty()) {
                                        vm.execute(function)
                                    } else {
                                        selectedFunction = function
                                    }
                                },
                            )
                        }
                    }

                    selectedFunction?.let { function ->
                        ParameterDialog(
                            function = function,
                            onDismiss = { selectedFunction = null },
                            onExecute = { arguments ->
                                selectedFunction = null
                                vm.execute(function, arguments)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    body: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            SelectionContainer {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun FunctionCard(
    function: FunctionDeclaration,
    onRun: () -> Unit,
) {
    val parameterNames = function.parameters?.properties?.keys.orEmpty().toList()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = function.shortName,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = function.description,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (parameterNames.isNotEmpty()) {
                Text(
                    text = "Parameters: ${parameterNames.joinToString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onRun,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (parameterNames.isEmpty()) "Run" else "Enter parameters")
            }
        }
    }
}

@Composable
private fun ParameterDialog(
    function: FunctionDeclaration,
    onDismiss: () -> Unit,
    onExecute: (Map<String, String>) -> Unit,
) {
    val properties = function.parameters?.properties.orEmpty()
    val required = function.parameters?.required.orEmpty().toSet()
    val values = remember(function.name) {
        SnapshotStateMap<String, String>().apply {
            properties.keys.forEach { key -> this[key] = "" }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(function.shortName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = function.description,
                    style = MaterialTheme.typography.bodyMedium,
                )
                properties.forEach { (name, schema) ->
                    ParameterField(
                        name = name,
                        schema = schema,
                        value = values[name] ?: "",
                        values = values,
                        required = name in required,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onExecute(values.toMap()) },
                enabled = required.all { requiredName -> !(values[requiredName] ?: "").isBlank() },
            ) {
                Text("Run")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ParameterField(
    name: String,
    schema: Schema,
    value: String,
    values: SnapshotStateMap<String, String>,
    required: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { values[name] = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(if (required) "$name *" else name) },
        supportingText = {
            Text(parameterHelp(name, schema))
        },
        singleLine = schema.type != DataType.ARRAY && schema.type != DataType.OBJECT,
        keyboardOptions = KeyboardOptions(
            keyboardType = schema.keyboardType(),
            imeAction = ImeAction.Done,
        ),
    )
}

private fun parameterHelp(
    name: String,
    schema: Schema,
): String = buildList {
    schema.description.takeIf { it.isNotBlank() }?.let(::add)
    if (schema.enum.isNotEmpty()) {
        add("Allowed values: ${schema.enum.joinToString()}")
    }
    if (name == "todoId") {
        add("Tip: run getAllTodos or getPendingTodos first to copy the todo id.")
    }
}.joinToString(" ")

private fun Schema.keyboardType(): KeyboardType = when (type) {
    DataType.INT,
    DataType.LONG,
    DataType.FLOAT,
    DataType.DOUBLE,
    -> KeyboardType.Number

    DataType.BOOLEAN -> KeyboardType.Ascii
    else -> KeyboardType.Text
}
