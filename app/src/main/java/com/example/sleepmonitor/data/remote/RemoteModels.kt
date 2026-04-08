package com.example.sleepmonitor.data.remote

import com.example.sleepmonitor.data.local.entities.RecommendationEntity
import com.example.sleepmonitor.data.local.entities.SleepSessionEntity
import com.example.sleepmonitor.data.local.entities.UserEntity

data class UserPayload(
    val userId: String,
    val email: String,
    val username: String,
    val createdAt: Long,
    val peso: Int?,
    val altura: Int?,
    val sexo: String?,
    val pais: String?,
    val fechaNacimiento: Long?
)

data class SleepSessionPayload(
    val sessionId: String,
    val userId: String,
    val startTime: Long,
    val endTime: Long?,
    val alarmWindowStart: String,
    val alarmWindowEnd: String,
    val sampleIntervalMs: Long,
    val aiScore: Int?,
    val userScore: Int?,
    val status: String,
    val wakeMethod: String?
)

data class RecommendationPayload(
    val recId: String,
    val userId: String,
    val sessionId: String?,
    val title: String,
    val description: String,
    val createdAt: Long,
    val applied: Boolean
)

fun UserEntity.toPayload(): UserPayload = UserPayload(
    userId = userId,
    email = email,
    username = username,
    createdAt = createdAt,
    peso = peso,
    altura = altura,
    sexo = sexo,
    pais = pais,
    fechaNacimiento = fechaNacimiento
)

fun SleepSessionEntity.toPayload(): SleepSessionPayload = SleepSessionPayload(
    sessionId = sessionId,
    userId = userId,
    startTime = startTime,
    endTime = endTime,
    alarmWindowStart = alarmWindowStart,
    alarmWindowEnd = alarmWindowEnd,
    sampleIntervalMs = sampleIntervalMs,
    aiScore = aiScore,
    userScore = userScore,
    status = status,
    wakeMethod = wakeMethod
)

fun RecommendationEntity.toPayload(): RecommendationPayload = RecommendationPayload(
    recId = recId,
    userId = userId,
    sessionId = sessionId,
    title = title,
    description = description,
    createdAt = createdAt,
    applied = applied
)

fun RecommendationPayload.toEntity(): RecommendationEntity = RecommendationEntity(
    recId = recId,
    userId = userId,
    sessionId = sessionId,
    title = title,
    description = description,
    createdAt = createdAt,
    applied = applied
)
