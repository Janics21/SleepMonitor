package com.example.sleepmonitor.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onBackToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }
    var altura by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf("") }
    var pais by remember { mutableStateOf("") }
    var fechaNacimiento by remember { mutableStateOf("") }
    val state by viewModel.state.observeAsState(RegisterState.Idle)

    LaunchedEffect(state) {
        if (state is RegisterState.Success) onRegisterSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Crear cuenta", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Email, usuario y contrasena son obligatorios. El resto prepara datos utiles para recomendaciones e IA futura.",
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
                    OutlinedTextField(username, { username = it }, label = { Text("Usuario") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contrasena") },
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
                    OutlinedTextField(peso, { peso = it }, label = { Text("Peso en kg (opcional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(altura, { altura = it }, label = { Text("Altura en cm (opcional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(sexo, { sexo = it }, label = { Text("Sexo (opcional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(pais, { pais = it }, label = { Text("Pais (opcional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = fechaNacimiento,
                        onValueChange = { fechaNacimiento = it },
                        label = { Text("Fecha nacimiento AAAA-MM-DD") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.register(
                                email = email,
                                username = username,
                                password = password,
                                confirmPassword = confirmPassword,
                                peso = peso.toIntOrNull(),
                                altura = altura.toIntOrNull(),
                                sexo = sexo.takeIf { it.isNotBlank() },
                                pais = pais.takeIf { it.isNotBlank() },
                                fechaNacimiento = fechaNacimiento.toEpochMillisOrNull()
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state !is RegisterState.Loading
                    ) {
                        Text("Completar registro")
                    }

                    when (val current = state) {
                        RegisterState.Loading -> CircularProgressIndicator()
                        is RegisterState.Error -> Text(current.message, color = MaterialTheme.colorScheme.error)
                        else -> Unit
                    }
                }
            }

            TextButton(onClick = onBackToLogin) {
                Text("Volver al login")
            }
        }
    }
}

private fun String.toEpochMillisOrNull(): Long? = runCatching {
    if (isBlank()) return null
    LocalDate.parse(this)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrNull()
