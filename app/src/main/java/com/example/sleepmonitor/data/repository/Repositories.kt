package com.example.sleepmonitor.data.repository

import com.example.sleepmonitor.data.local.SleepDatabase
import com.example.sleepmonitor.data.local.entities.PasswordResetTokenEntity
import com.example.sleepmonitor.data.local.entities.PhaseEntity
import com.example.sleepmonitor.data.local.entities.RecommendationEntity
import com.example.sleepmonitor.data.local.entities.SensorSampleEntity
import com.example.sleepmonitor.data.local.entities.SensorSummaryEntity
import com.example.sleepmonitor.data.local.entities.SleepSessionEntity
import com.example.sleepmonitor.data.local.entities.UserEntity
import com.example.sleepmonitor.domain.sleep.RuleBasedSleepInsightsEngine
import com.example.sleepmonitor.ui.utils.IdUtils
import com.example.sleepmonitor.ui.utils.PasswordHasher
import com.example.sleepmonitor.ui.utils.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

class AuthRepository(private val db: SleepDatabase) {

    suspend fun register(
        email: String,
        username: String,
        password: String,
        peso: Int? = null,
        altura: Int? = null,
        sexo: String? = null,
        pais: String? = null,
        fechaNacimiento: Long? = null
    ): Result<UserEntity> {
        if (db.userDao().getUserByEmail(email) != null) {
            return Result.Error("El correo ya esta registrado")
        }
        if (db.userDao().getUserByUsername(username) != null) {
            return Result.Error("El nombre de usuario ya esta en uso")
        }

        val user = UserEntity(
            userId = IdUtils.newId(),
            email = email,
            username = username,
            passwordHash = PasswordHasher.hash(password),
            peso = peso,
            altura = altura,
            sexo = sexo,
            pais = pais,
            fechaNacimiento = fechaNacimiento
        )
        db.userDao().insertUser(user)
        return Result.Success(user)
    }

    suspend fun login(emailOrUsername: String, password: String): Result<UserEntity> {
        val user = db.userDao().getUserByEmail(emailOrUsername)
            ?: db.userDao().getUserByUsername(emailOrUsername)
            ?: return Result.Error("Usuario o contrasena incorrectos")

        if (!PasswordHasher.verify(password, user.passwordHash)) {
            return Result.Error("Usuario o contrasena incorrectos")
        }

        return Result.Success(user)
    }

    suspend fun requestPasswordReset(email: String): Result<String> {
        val user = db.userDao().getUserByEmail(email) ?: return Result.Success("demo-hidden")

        db.passwordResetTokenDao().purgeExpired()

        val token = IdUtils.newId()
        db.passwordResetTokenDao().insertToken(
            PasswordResetTokenEntity(
                tokenId = IdUtils.newId(),
                userId = user.userId,
                token = token,
                expiresAt = TimeUtils.in24Hours()
            )
        )
        return Result.Success(token)
    }

    suspend fun resetPassword(token: String, newPassword: String): Result<Unit> {
        db.passwordResetTokenDao().purgeExpired()
        val tokenEntity = db.passwordResetTokenDao().findValidToken(token)
            ?: return Result.Error("Este enlace ha caducado o no es valido")

        db.userDao().updatePassword(tokenEntity.userId, PasswordHasher.hash(newPassword))
        db.passwordResetTokenDao().markUsed(tokenEntity.tokenId)
        return Result.Success(Unit)
    }

    suspend fun deleteAccount(userId: String, password: String): Result<Unit> {
        val user = db.userDao().getUserById(userId) ?: return Result.Error("Usuario no encontrado")
        if (!PasswordHasher.verify(password, user.passwordHash)) {
            return Result.Error("La contrasena es incorrecta")
        }
        db.userDao().deleteUser(user)
        return Result.Success(Unit)
    }

    suspend fun getUserById(userId: String): UserEntity? = db.userDao().getUserById(userId)
}

data class SleepSessionReport(
    val session: SleepSessionEntity,
    val phases: List<PhaseEntity>,
    val summary: SensorSummaryEntity?,
    val recommendations: List<RecommendationEntity>,
    val qualityHeadline: String,
    val qualityDescription: String,
    val precisionNotice: String
)

class SleepRepository(private val db: SleepDatabase) {

    suspend fun startSession(
        userId: String,
        alarmWindowStart: String,
        alarmWindowEnd: String,
        sampleIntervalMs: Long
    ): SleepSessionEntity {
        val session = SleepSessionEntity(
            sessionId = IdUtils.newId(),
            userId = userId,
            startTime = TimeUtils.nowMillis(),
            alarmWindowStart = alarmWindowStart,
            alarmWindowEnd = alarmWindowEnd,
            sampleIntervalMs = sampleIntervalMs
        )
        db.sleepSessionDao().insertSession(session)
        return session
    }

    suspend fun finalizeSession(
        sessionId: String,
        wakeTime: Long = TimeUtils.nowMillis(),
        wakeMethod: String
    ): SleepSessionReport {
        val session = db.sleepSessionDao().getSessionById(sessionId)
            ?: error("Sesion no encontrada")
        val samples = db.sensorSampleDao().getSamplesForSession(sessionId)
        val analysis = RuleBasedSleepInsightsEngine.analyze(
            sessionId = sessionId,
            samples = samples,
            startedAt = session.startTime,
            endedAt = wakeTime
        )

        db.phaseDao().deleteForSession(sessionId)
        db.phaseDao().insertPhases(analysis.phases)
        db.sensorSummaryDao().insertSummary(analysis.summary)
        db.recommendationDao().deleteForSession(sessionId)
        analysis.recommendations.forEach { recommendation ->
            db.recommendationDao().insertRecommendation(
                RecommendationEntity(
                    recId = IdUtils.newId(),
                    userId = session.userId,
                    sessionId = sessionId,
                    title = recommendation.title,
                    description = recommendation.description
                )
            )
        }
        db.sleepSessionDao().completeSession(
            sessionId = sessionId,
            endTime = wakeTime,
            wakeTime = wakeTime,
            aiScore = analysis.aiScore,
            wakeMethod = wakeMethod,
            engineVersion = RuleBasedSleepInsightsEngine.version,
            sensorFailure = analysis.sensorFailure
        )
        purgeOldSamples()
        return getReport(sessionId) ?: error("No se pudo construir el reporte")
    }

    suspend fun setUserFeedback(sessionId: String, score: Int) {
        val session = db.sleepSessionDao().getSessionById(sessionId) ?: return
        val discrepancy = session.aiScore?.let { abs(it - score) } ?: 0
        db.sleepSessionDao().setUserScore(sessionId, score, discrepancy)
    }

    suspend fun skipUserFeedback(sessionId: String) {
        db.sleepSessionDao().markFeedbackSkipped(sessionId)
    }

    suspend fun recordSensorFailure(sessionId: String) {
        db.sleepSessionDao().markSensorFailure(sessionId)
    }

    fun getSessionsFlow(userId: String): Flow<List<SleepSessionEntity>> =
        db.sleepSessionDao().getSessionsForUser(userId)

    suspend fun getLatestSession(userId: String): SleepSessionEntity? =
        db.sleepSessionDao().getLatestSession(userId)

    suspend fun getSessionById(sessionId: String): SleepSessionEntity? =
        db.sleepSessionDao().getSessionById(sessionId)

    suspend fun getActiveSession(userId: String): SleepSessionEntity? =
        db.sleepSessionDao().getActiveSession(userId)

    suspend fun getLatestPendingFeedback(userId: String): SleepSessionEntity? =
        db.sleepSessionDao().getLatestPendingFeedbackSession(userId)

    suspend fun insertSample(sample: SensorSampleEntity) {
        db.sensorSampleDao().insertSample(sample)
    }

    suspend fun getSamples(sessionId: String): List<SensorSampleEntity> =
        db.sensorSampleDao().getSamplesForSession(sessionId)

    suspend fun getLatestLightPhase(sessionId: String): PhaseEntity? =
        db.phaseDao().getLatestLightPhase(sessionId)

    suspend fun getReport(sessionId: String): SleepSessionReport? {
        val session = db.sleepSessionDao().getSessionById(sessionId) ?: return null
        val phases = db.phaseDao().getPhasesForSessionOnce(sessionId)
        val summary = db.sensorSummaryDao().getSummaryForSession(sessionId)
        val recommendations = db.recommendationDao().getRecommendationsForSession(sessionId)
        return SleepSessionReport(
            session = session,
            phases = phases,
            summary = summary,
            recommendations = recommendations,
            qualityHeadline = RuleBasedSleepInsightsEngine.qualityHeadline(session.aiScore ?: 0),
            qualityDescription = RuleBasedSleepInsightsEngine.qualityDescription(session.aiScore ?: 0),
            precisionNotice = RuleBasedSleepInsightsEngine.precisionNotice
        )
    }

    suspend fun purgeOldSamples() {
        val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000L
        db.sensorSampleDao().purgeSamplesOlderThan(cutoff)
    }
}

class RecommendationRepository(private val db: SleepDatabase) {

    fun getRecommendationsFlow(userId: String): Flow<List<RecommendationEntity>> =
        db.recommendationDao().getRecommendationsForUser(userId)

    suspend fun hasRecommendations(userId: String): Boolean =
        db.recommendationDao().countForUser(userId) > 0

    fun getGenericRecommendations(): List<Pair<String, String>> = listOf(
        "Manten una rutina estable" to "Intenta acostarte y levantarte a una hora parecida incluso los fines de semana.",
        "Reduce la luz azul" to "Evita pantallas al menos 30 minutos antes de dormir para facilitar la conciliacion del sueno.",
        "Cuida el entorno" to "Oscuridad, silencio y una temperatura fresca ayudan a reducir despertares nocturnos.",
        "Evita estimulantes tarde" to "Reduce cafeina o bebidas energeticas durante la tarde si notas sueno fragmentado.",
        "Prepara la medicion" to "Deja el telefono cargando y en una posicion estable para mejorar la consistencia del analisis."
    )
}
