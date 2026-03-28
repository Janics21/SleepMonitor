package com.example.sleepmonitor.ui.profile

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onDeleteAccount: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.state.observeAsState(ProfileState.Loading)

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
        when (val current = state) {
            ProfileState.Loading -> {
                Text("Cargando perfil...", style = MaterialTheme.typography.bodyLarge)
            }

            is ProfileState.Ready -> {
                val user = current.user
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Perfil y seguridad", style = MaterialTheme.typography.headlineLarge)
                    ProfileCard(
                        title = user?.username ?: "Usuario",
                        lines = listOfNotNull(
                            user?.email,
                            user?.pais?.let { "Pais: $it" },
                            user?.sexo?.let { "Sexo: $it" },
                            user?.peso?.let { "Peso: ${it} kg" },
                            user?.altura?.let { "Altura: ${it} cm" },
                            user?.fechaNacimiento?.let { "Datos utiles para IA local preparados" }
                        ).ifEmpty {
                            listOf("Completa el registro para enriquecer futuras recomendaciones y la futura capa IA.")
                        }
                    )
                    ProfileCard(
                        title = "Privacidad y precision",
                        lines = listOf(
                            "El audio no sale del dispositivo.",
                            "La sesion se guarda localmente con hash seguro para credenciales.",
                            "Las fases de sueno son una estimacion basada en ruido y movimiento."
                        )
                    )
                    ProfileCard(
                        title = "Preparado para integrar IA",
                        lines = listOf(
                            "El motor de analisis esta desacoplado del servicio.",
                            "Los datos quedan estructurados para conectar un modelo TFLite despues.",
                            "La app ya registra feedback del usuario para futuras calibraciones."
                        )
                    )
                    OutlinedButton(
                        onClick = onDeleteAccount,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Eliminar cuenta")
                    }
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar sesion")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(title: String, lines: List<String>) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            lines.forEach {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
