package com.example.sleepmonitor.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun DeleteAccountScreen(
    viewModel: DeleteAccountViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var password by remember { mutableStateOf("") }
    val state by viewModel.state.observeAsState(DeleteState.Idle)

    LaunchedEffect(state) {
        if (state is DeleteState.Success) onDeleted()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(20.dp)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Eliminar cuenta", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Esta accion es irreversible y elimina el acceso y los datos guardados en el dispositivo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (step == 1) {
                    Button(
                        onClick = { step = 2 },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continuar con la eliminacion")
                    }
                } else {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Confirma tu contrasena") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { viewModel.deleteAccount(password) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state !is DeleteState.Loading
                    ) {
                        Text("Confirmar eliminacion")
                    }
                }

                when (val current = state) {
                    DeleteState.Loading -> CircularProgressIndicator()
                    is DeleteState.Error -> Text(current.message, color = MaterialTheme.colorScheme.error)
                    else -> Unit
                }

                TextButton(onClick = onBack) {
                    Text("Cancelar")
                }
            }
        }
    }
}
