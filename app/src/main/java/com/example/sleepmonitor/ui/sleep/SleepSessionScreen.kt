package com.example.sleepmonitor.ui.sleep

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.sleepmonitor.data.repository.SleepSessionReport
import com.example.sleepmonitor.ui.utils.TimeUtils

private data class PendingStart(
    val start: String,
    val end: String,
    val sampleIntervalMs: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepSessionScreen(
    viewModel: SleepSessionViewModel,
    onGoBackHome: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.observeAsState(SleepSessionState.Initializing)

    val startPickerState = rememberTimePickerState(initialHour = 7, initialMinute = 0, is24Hour = true)
    val endPickerState = rememberTimePickerState(initialHour = 7, initialMinute = 30, is24Hour = true)
    var sampleIntervalSeconds by remember { mutableFloatStateOf(10f) }
    var feedback by remember { mutableFloatStateOf(70f) }
    var pendingStart by remember { mutableStateOf<PendingStart?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        if (allGranted) {
            pendingStart?.let {
                viewModel.startSession(
                    context = context,
                    alarmWindowStart = it.start,
                    alarmWindowEnd = it.end,
                    sampleIntervalMs = it.sampleIntervalMs
                )
                pendingStart = null
            }
        } else {
            Toast.makeText(
                context,
                "Necesitas permiso de microfono y notificaciones para una sesion completa.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(state) {
        when (val current = state) {
            SleepSessionState.FeedbackSaved -> onGoBackHome()
            is SleepSessionState.Error -> Toast.makeText(context, current.message, Toast.LENGTH_LONG).show()
            else -> Unit
        }
    }

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
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Sesion nocturna", style = MaterialTheme.typography.headlineLarge)

            when (val current = state) {
                SleepSessionState.Initializing -> {
                    Text("Recuperando estado de la sesion...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                is SleepSessionState.Ready -> {
                    if (current.showOnboarding) {
                        FeatureCard(
                            title = "Antes de empezar",
                            body = "Coloca el movil cargando, deja el microfono activo y ten en cuenta que las fases son una estimacion basada en movimiento y ruido."
                        )
                        TextButton(onClick = { viewModel.dismissOnboarding() }) {
                            Text("Entendido")
                        }
                    }

                    FeatureCard(
                        title = "Configura el despertar inteligente",
                        body = "La sesion se registrara con un Foreground Service y el reporte final quedara preparado incluso si la app se cierra."
                    )

                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Inicio de ventana", style = MaterialTheme.typography.titleMedium)
                            TimeInput(state = startPickerState)
                            Text("Fin de ventana", style = MaterialTheme.typography.titleMedium)
                            TimeInput(state = endPickerState)
                            Text(
                                "Intervalo de muestreo: ${sampleIntervalSeconds.toInt()} s",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Slider(
                                value = sampleIntervalSeconds,
                                onValueChange = { sampleIntervalSeconds = listOf(5f, 10f, 15f).minBy { option -> kotlin.math.abs(option - it) } },
                                valueRange = 5f..15f,
                                steps = 1
                            )
                            Button(
                                onClick = {
                                    val request = PendingStart(
                                        start = TimeUtils.hhmm(startPickerState.hour, startPickerState.minute),
                                        end = TimeUtils.hhmm(endPickerState.hour, endPickerState.minute),
                                        sampleIntervalMs = sampleIntervalSeconds.toLong() * 1000L
                                    )
                                    pendingStart = request
                                    permissionLauncher.launch(requiredPermissions())
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Empezar sesion de sueno")
                            }
                        }
                    }
                }

                is SleepSessionState.Monitoring -> {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Sesion en marcha", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Ventana objetivo: ${current.alarmWindowStart} - ${current.alarmWindowEnd}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "La captura sigue activa con el movil bloqueado para mantener la sesion toda la noche.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { viewModel.stopSession(context) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Detener y generar reporte")
                            }
                        }
                    }
                }

                is SleepSessionState.Report -> {
                    SleepReportContent(
                        report = current.report,
                        feedback = feedback,
                        onFeedbackChange = { feedback = it },
                        onSubmitFeedback = { viewModel.submitUserFeedback(feedback.toInt()) },
                        onSkip = {
                            viewModel.skipFeedback()
                            onGoBackHome()
                        }
                    )
                }

                SleepSessionState.FeedbackSaved -> {
                    Text("Guardando feedback...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                is SleepSessionState.Error -> {
                    OutlinedButton(
                        onClick = {
                            viewModel.resetToReady()
                            onGoBackHome()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Volver al inicio")
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepReportContent(
    report: SleepSessionReport,
    feedback: Float,
    onFeedbackChange: (Float) -> Unit,
    onSubmitFeedback: () -> Unit,
    onSkip: () -> Unit
) {
    val wakeMethod = when (report.session.wakeMethod) {
        "SMART_ALARM" -> "Despertador inteligente"
        "WINDOW_END" -> "Fin de ventana"
        "MANUAL_STOP" -> "Fin manual"
        else -> "Sesion finalizada"
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(report.qualityHeadline, style = MaterialTheme.typography.headlineSmall)
            Text(report.qualityDescription, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Puntuacion estimada: ${report.session.aiScore ?: 0}/100", style = MaterialTheme.typography.titleLarge)
            Text("Cierre de sesion: $wakeMethod")
            report.summary?.let { summary ->
                Text("Duracion estimada: ${TimeUtils.formatDurationMinutes(summary.estimatedSleepMinutes)}")
                Text("Ruido disruptivo detectado: ${summary.noiseEvents} eventos")
                Text("Movimiento relevante detectado: ${summary.movementEvents} eventos")
            }
            Text(report.precisionNotice, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (report.phases.isNotEmpty()) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Fases detectadas", style = MaterialTheme.typography.titleLarge)
                report.phases.forEach { phase ->
                    val label = TimeUtils.phaseLabel(phase.type)
                    Text("$label - ${phase.durationSeconds / 60} min")
                }
            }
        }
    }

    if (report.recommendations.isNotEmpty()) {
        report.recommendations.forEach { recommendation ->
            FeatureCard(
                title = recommendation.title,
                body = recommendation.description
            )
        }
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Tu valoracion", style = MaterialTheme.typography.titleLarge)
            Text("Como te has levantado hoy: ${feedback.toInt()}/100")
            Slider(
                value = feedback,
                onValueChange = onFeedbackChange,
                valueRange = 0f..100f
            )
            Button(onClick = onSubmitFeedback, modifier = Modifier.fillMaxWidth()) {
                Text("Guardar feedback")
            }
            TextButton(onClick = onSkip) {
                Text("Omitir por ahora")
            }
        }
    }
}

@Composable
private fun FeatureCard(title: String, body: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun requiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(Manifest.permission.RECORD_AUDIO)
    }
}
