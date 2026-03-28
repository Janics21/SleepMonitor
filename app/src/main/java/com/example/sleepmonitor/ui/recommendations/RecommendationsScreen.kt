package com.example.sleepmonitor.ui.recommendations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecommendationsScreen(
    viewModel: RecommendationsViewModel,
    onBack: () -> Unit
) {
    val recommendations by viewModel.recommendations.observeAsState(emptyList())
    val showGeneric by viewModel.showGeneric.observeAsState(true)
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Recomendaciones", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Aqui queda el historial de consejos generados por cada sesion. Si aun no hay historial, veras sugerencias base.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onBack) {
                Text("Volver al inicio")
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (showGeneric) {
                    items(viewModel.getGenericRecommendations()) { pair ->
                        RecommendationCard(
                            title = pair.first,
                            description = pair.second,
                            footer = "Sugerencia general"
                        )
                    }
                } else {
                    items(recommendations) { recommendation ->
                        RecommendationCard(
                            title = recommendation.title,
                            description = recommendation.description,
                            footer = dateFormatter.format(Date(recommendation.createdAt))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    title: String,
    description: String,
    footer: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(footer, style = MaterialTheme.typography.labelLarge)
        }
    }
}
