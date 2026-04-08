package com.example.sleepmonitor.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey val userId: String,
    val email: String,
    val username: String,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis(),
    val peso: Int? = null,
    val altura: Int? = null,
    val sexo: String? = null,
    val pais: String? = null,
    val fechaNacimiento: Long? = null,
    val sleepProfile: String? = null,
    val aiCalibrationScore: Float = 0f
)

@Entity(
    tableName = "sleep_sessions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class SleepSessionEntity(
    @PrimaryKey val sessionId: String,
    val userId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val alarmWindowStart: String,
    val alarmWindowEnd: String,
    val sampleIntervalMs: Long = 10_000L,
    val realWakeUpTime: Long? = null,
    val aiScore: Int? = null,
    val userScore: Int? = null,
    val discrepancyScore: Int? = null,
    val feedbackStatus: String = "PENDING",
    val calibrated: Boolean = false,
    val sensorFailure: Boolean = false,
    val batteryWarning: Boolean = false,
    val processedByAi: Boolean = false,
    val status: String = "ACTIVE",
    val wakeMethod: String? = null,
    val aiEngineVersion: String? = null
)

@Entity(
    tableName = "phases",
    foreignKeys = [
        ForeignKey(
            entity = SleepSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class PhaseEntity(
    @PrimaryKey val phaseId: String,
    val sessionId: String,
    val type: String,
    val start: Long,
    val end: Long,
    val durationSeconds: Int
)

@Entity(
    tableName = "sensor_samples",
    foreignKeys = [
        ForeignKey(
            entity = SleepSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class SensorSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val timestamp: Long,
    val accelerometerX: Float,
    val accelerometerY: Float,
    val accelerometerZ: Float,
    val movementMagnitude: Float,
    val noiseDecibels: Float
)

@Entity(
    tableName = "sensor_summary",
    foreignKeys = [
        ForeignKey(
            entity = SleepSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"], unique = true)]
)
data class SensorSummaryEntity(
    @PrimaryKey val sessionId: String,
    val avgMovement: Float,
    val maxMovement: Float,
    val avgNoiseDb: Float,
    val maxNoiseDb: Float,
    val noiseEvents: Int,
    val movementEvents: Int,
    val estimatedSleepMinutes: Int,
    val totalSamples: Int
)

@Entity(
    tableName = "recommendations",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SleepSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("userId"), Index("sessionId")]
)
data class RecommendationEntity(
    @PrimaryKey val recId: String,
    val userId: String,
    val sessionId: String?,
    val title: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val applied: Boolean = false
)

@Entity(
    tableName = "password_reset_tokens",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class PasswordResetTokenEntity(
    @PrimaryKey val tokenId: String,
    val userId: String,
    val token: String,
    val expiresAt: Long,
    val used: Boolean = false
)

@Entity(
    tableName = "sync_tasks",
    indices = [Index(value = ["entityType", "entityId"])]
)
data class SyncTaskEntity(
    @PrimaryKey val taskId: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
