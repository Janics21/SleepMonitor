package com.example.sleepmonitor.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    username: String,
    onGoToSleep: () -> Unit,
    onGoToRecommendations: () -> Unit,
    onGoToProfile: () -> Unit
) {
    val displayName = username.ifBlank { "usuario" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Buenas noches, $displayName",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "La app queda preparada para medir tu descanso de forma local, generar un reporte y dejar el punto de integracion listo para IA futura.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Flujo principal en 2 toques", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "1. Entra en Dormir. 2. Configura tu ventana y empieza.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onGoToSleep,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Empezar sesion de sueno")
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InsightCard(
                    modifier = Modifier.weight(1f),
                    title = "Privacidad",
                    body = "Todo el procesamiento de sensores queda en el dispositivo."
                )
                InsightCard(
                    modifier = Modifier.weight(1f),
                    title = "IA futura",
                    body = "La arquitectura ya deja preparado el motor de analisis para TFLite."
                )
            }

            InsightCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Siguientes espacios de la app",
                body = "Consejos guarda el historial de recomendaciones. Perfil concentra datos del usuario, aviso de precision y acciones de cuenta."
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onGoToRecommendations) {
                    Text("Ver recomendaciones")
                }
                TextButton(onClick = onGoToProfile) {
                    Text("Abrir perfil")
                }
            }
        }
    }
}

@Composable
private fun InsightCard(
    modifier: Modifier = Modifier,
    title: String,
    body: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
