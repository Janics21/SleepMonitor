package com.example.sleepmonitor.domain.sleep

import com.example.sleepmonitor.data.local.entities.PhaseEntity
import com.example.sleepmonitor.data.local.entities.SensorSampleEntity
import com.example.sleepmonitor.data.local.entities.SensorSummaryEntity
import com.example.sleepmonitor.ui.utils.IdUtils
import com.example.sleepmonitor.ui.utils.TimeUtils
import kotlin.math.max

object RuleBasedSleepInsightsEngine {
    const val version = "rule-based-v1"
    const val precisionNotice =
        "El analisis es una estimacion basada en movimiento y ruido ambiental. No sustituye a un wearable clinico."

    private const val movementAwakeThreshold = 0.32f
    private const val movementLightThreshold = 0.10f
    private const val noiseAwakeThreshold = 56f
    private const val noiseDisruptiveThreshold = 44f
    private const val wakeWindowSampleCount = 12

    data class RecommendationDraft(
        val title: String,
        val description: String
    )

    data class AnalysisResult(
        val phases: List<PhaseEntity>,
        val summary: SensorSummaryEntity,
        val aiScore: Int,
        val recommendations: List<RecommendationDraft>,
        val sensorFailure: Boolean
    )

    fun analyze(
        sessionId: String,
        samples: List<SensorSampleEntity>,
        startedAt: Long,
        endedAt: Long
    ): AnalysisResult {
        if (samples.isEmpty()) {
            return AnalysisResult(
                phases = emptyList(),
                summary = SensorSummaryEntity(
                    sessionId = sessionId,
                    avgMovement = 0f,
                    maxMovement = 0f,
                    avgNoiseDb = 0f,
                    maxNoiseDb = 0f,
                    noiseEvents = 0,
                    movementEvents = 0,
                    estimatedSleepMinutes = TimeUtils.minutesBetween(startedAt, endedAt),
                    totalSamples = 0
                ),
                aiScore = 45,
                recommendations = listOf(
                    RecommendationDraft(
                        title = "Datos insuficientes",
                        description = "La sesion tuvo muy pocas muestras. Revisa permisos, coloca mejor el movil y dejalo conectado."
                    )
                ),
                sensorFailure = true
            )
        }

        val phases = buildPhases(sessionId, samples, endedAt)
        val movementEvents = samples.count { it.movementMagnitude > movementLightThreshold }
        val noiseEvents = samples.count { it.noiseDecibels > noiseDisruptiveThreshold }
        val summary = SensorSummaryEntity(
            sessionId = sessionId,
            avgMovement = samples.map { it.movementMagnitude }.average().toFloat(),
            maxMovement = samples.maxOf { it.movementMagnitude },
            avgNoiseDb = samples.map { it.noiseDecibels }.average().toFloat(),
            maxNoiseDb = samples.maxOf { it.noiseDecibels },
            noiseEvents = noiseEvents,
            movementEvents = movementEvents,
            estimatedSleepMinutes = TimeUtils.minutesBetween(startedAt, endedAt),
            totalSamples = samples.size
        )
        val aiScore = computeScore(phases, summary)
        return AnalysisResult(
            phases = phases,
            summary = summary,
            aiScore = aiScore,
            recommendations = buildRecommendations(summary, aiScore),
            sensorFailure = samples.size < 4
        )
    }

    fun isGoodWakeWindow(samples: List<SensorSampleEntity>): Boolean {
        if (samples.size < wakeWindowSampleCount) return false
        val recent = samples.takeLast(wakeWindowSampleCount)
        val avgMovement = recent.map { it.movementMagnitude }.average().toFloat()
        val avgNoise = recent.map { it.noiseDecibels }.average().toFloat()
        return avgMovement in movementLightThreshold..movementAwakeThreshold && avgNoise < noiseAwakeThreshold
    }

    fun qualityHeadline(score: Int): String = when {
        score >= 85 -> "Descanso muy estable"
        score >= 70 -> "Buena noche"
        score >= 55 -> "Sueno mejorable"
        else -> "Noche irregular"
    }

    fun qualityDescription(score: Int): String = when {
        score >= 85 -> "El patron detectado fue consistente, con pocas interrupciones y una ventana de descanso uniforme."
        score >= 70 -> "Se detecto una noche razonablemente estable, aunque aun hay margen para afinar entorno y rutina."
        score >= 55 -> "Hubo interrupciones o variaciones suficientes para recomendar pequenos ajustes antes de dormir."
        else -> "La sesion muestra ruido o movimiento frecuentes. Conviene revisar el entorno y repetir la medicion."
    }

    private fun buildPhases(
        sessionId: String,
        samples: List<SensorSampleEntity>,
        endedAt: Long
    ): List<PhaseEntity> {
        val phases = mutableListOf<PhaseEntity>()
        var currentType = classify(samples.first())
        var phaseStart = samples.first().timestamp

        samples.drop(1).forEach { sample ->
            val sampleType = classify(sample)
            if (sampleType != currentType) {
                phases += PhaseEntity(
                    phaseId = IdUtils.newId(),
                    sessionId = sessionId,
                    type = currentType,
                    start = phaseStart,
                    end = sample.timestamp,
                    durationSeconds = max(1, ((sample.timestamp - phaseStart) / 1000L).toInt())
                )
                currentType = sampleType
                phaseStart = sample.timestamp
            }
        }

        phases += PhaseEntity(
            phaseId = IdUtils.newId(),
            sessionId = sessionId,
            type = currentType,
            start = phaseStart,
            end = endedAt,
            durationSeconds = max(1, ((endedAt - phaseStart) / 1000L).toInt())
        )
        return phases
    }

    private fun classify(sample: SensorSampleEntity): String = when {
        sample.movementMagnitude > movementAwakeThreshold || sample.noiseDecibels > noiseAwakeThreshold -> "AWAKE"
        sample.movementMagnitude > movementLightThreshold -> "LIGHT"
        else -> "DEEP"
    }

    private fun computeScore(phases: List<PhaseEntity>, summary: SensorSummaryEntity): Int {
        val totalSeconds = phases.sumOf { it.durationSeconds }.coerceAtLeast(1)
        val deepSeconds = phases.filter { it.type == "DEEP" }.sumOf { it.durationSeconds }
        val awakeSeconds = phases.filter { it.type == "AWAKE" }.sumOf { it.durationSeconds }

        val deepRatio = deepSeconds.toFloat() / totalSeconds
        val awakeRatio = awakeSeconds.toFloat() / totalSeconds
        val noisePenalty = (summary.noiseEvents * 1.8f).toInt()
        val movementPenalty = (summary.movementEvents * 1.2f).toInt()
        val durationBonus = when {
            summary.estimatedSleepMinutes in 420..540 -> 12
            summary.estimatedSleepMinutes in 360..419 -> 8
            summary.estimatedSleepMinutes in 300..359 -> 4
            else -> 0
        }

        val score = 52 +
            (deepRatio * 34).toInt() +
            ((1f - awakeRatio) * 18).toInt() +
            durationBonus -
            noisePenalty -
            movementPenalty

        return score.coerceIn(0, 100)
    }

    private fun buildRecommendations(
        summary: SensorSummaryEntity,
        aiScore: Int
    ): List<RecommendationDraft> {
        val recommendations = mutableListOf<RecommendationDraft>()

        if (summary.avgNoiseDb >= 40f || summary.noiseEvents >= 3) {
            recommendations += RecommendationDraft(
                title = "Reduce el ruido nocturno",
                description = "Se detectaron picos de ruido durante la sesion. Prueba con un entorno mas silencioso o activa modo avion."
            )
        }

        if (summary.movementEvents >= 6) {
            recommendations += RecommendationDraft(
                title = "Estabiliza la colocacion del movil",
                description = "Hubo bastante movimiento registrado. Deja el telefono siempre en una posicion estable y compara varias noches."
            )
        }

        if (summary.estimatedSleepMinutes < 420) {
            recommendations += RecommendationDraft(
                title = "Amplia tu ventana de descanso",
                description = "La duracion estimada fue corta. Intenta adelantar la hora de acostarte o ampliar el rango objetivo de despertar."
            )
        }

        if (recommendations.isEmpty()) {
            recommendations += RecommendationDraft(
                title = if (aiScore >= 75) "Manten tu rutina" else "Ajusta habitos suaves",
                description = if (aiScore >= 75) {
                    "La sesion fue estable. Mantener horarios regulares y dejar el dispositivo cargando ayudara a seguir comparando noches."
                } else {
                    "Prueba una rutina mas constante antes de dormir y limita cafeina o pantallas durante la ultima hora del dia."
                }
            )
        }

        return recommendations.take(3)
    }
}
