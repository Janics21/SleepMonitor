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
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val requestState by viewModel.requestState.observeAsState(ForgotPasswordRequestState.Idle)
    val resetState by viewModel.resetState.observeAsState(ResetPasswordState.Idle)

    if (requestState is ForgotPasswordRequestState.EmailPrepared) {
        token = (requestState as ForgotPasswordRequestState.EmailPrepared).localDemoToken ?: token
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Recuperar contrasena", style = MaterialTheme.typography.headlineLarge)
            Text(
                "La arquitectura queda lista para backend por email. En esta demo local puedes usar el codigo temporal generado en el propio dispositivo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(email, { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = { viewModel.requestReset(email) },
                        enabled = requestState !is ForgotPasswordRequestState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Solicitar recuperacion")
                    }

                    when (val current = requestState) {
                        ForgotPasswordRequestState.Loading -> CircularProgressIndicator()
                        is ForgotPasswordRequestState.Error -> Text(current.message, color = MaterialTheme.colorScheme.error)
                        is ForgotPasswordRequestState.EmailPrepared -> {
                            Text(
                                "Si el correo existe, el enlace temporal queda preparado. En modo local puedes usar el codigo de abajo.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            current.localDemoToken?.let {
                                Text("Codigo local: $it", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        else -> Unit
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(token, { token = it }, label = { Text("Codigo temporal") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Nueva contrasena") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Repite la contrasena") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { viewModel.resetPassword(token, newPassword, confirmPassword) },
                        enabled = resetState !is ResetPasswordState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar nueva contrasena")
                    }

                    when (val current = resetState) {
                        ResetPasswordState.Loading -> CircularProgressIndicator()
                        ResetPasswordState.Success -> Text("Contrasena actualizada. Ya puedes volver al login.")
                        is ResetPasswordState.Error -> Text(current.message, color = MaterialTheme.colorScheme.error)
                        else -> Unit
                    }
                }
            }

            TextButton(onClick = onBack) {
                Text("Volver")
            }
        }
    }
}
