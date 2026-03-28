package com.example.sleepmonitor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.sleepmonitor.data.local.entities.PasswordResetTokenEntity
import com.example.sleepmonitor.data.local.entities.PhaseEntity
import com.example.sleepmonitor.data.local.entities.RecommendationEntity
import com.example.sleepmonitor.data.local.entities.SensorSampleEntity
import com.example.sleepmonitor.data.local.entities.SensorSummaryEntity
import com.example.sleepmonitor.data.local.entities.SleepSessionEntity
import com.example.sleepmonitor.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("UPDATE users SET passwordHash = :newHash WHERE userId = :userId")
    suspend fun updatePassword(userId: String, newHash: String)

    @Query("UPDATE users SET sleepProfile = :profile, aiCalibrationScore = :score WHERE userId = :userId")
    suspend fun updateAiData(userId: String, profile: String, score: Float)
}

@Dao
interface SleepSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SleepSessionEntity)

    @Update
    suspend fun updateSession(session: SleepSessionEntity)

    @Query("SELECT * FROM sleep_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions WHERE userId = :userId ORDER BY startTime DESC")
    fun getSessionsForUser(userId: String): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_sessions WHERE userId = :userId ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestSession(userId: String): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions WHERE userId = :userId AND status = 'ACTIVE' ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(userId: String): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions WHERE userId = :userId AND endTime IS NOT NULL AND feedbackStatus = 'PENDING' ORDER BY endTime DESC LIMIT 1")
    suspend fun getLatestPendingFeedbackSession(userId: String): SleepSessionEntity?

    @Query(
        """
        UPDATE sleep_sessions
        SET endTime = :endTime,
            realWakeUpTime = :wakeTime,
            processedByAi = 1,
            aiScore = :aiScore,
            status = 'COMPLETED',
            wakeMethod = :wakeMethod,
            aiEngineVersion = :engineVersion,
            sensorFailure = :sensorFailure
        WHERE sessionId = :sessionId
        """
    )
    suspend fun completeSession(
        sessionId: String,
        endTime: Long,
        wakeTime: Long,
        aiScore: Int,
        wakeMethod: String,
        engineVersion: String,
        sensorFailure: Boolean
    )

    @Query(
        """
        UPDATE sleep_sessions
        SET userScore = :score,
            discrepancyScore = :discrepancy,
            feedbackStatus = 'SUBMITTED',
            calibrated = 1
        WHERE sessionId = :sessionId
        """
    )
    suspend fun setUserScore(sessionId: String, score: Int, discrepancy: Int)

    @Query("UPDATE sleep_sessions SET feedbackStatus = 'SKIPPED' WHERE sessionId = :sessionId")
    suspend fun markFeedbackSkipped(sessionId: String)

    @Query("UPDATE sleep_sessions SET sensorFailure = 1 WHERE sessionId = :sessionId")
    suspend fun markSensorFailure(sessionId: String)

    @Query("UPDATE sleep_sessions SET status = 'INTERRUPTED' WHERE sessionId = :sessionId")
    suspend fun markInterrupted(sessionId: String)
}

@Dao
interface PhaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhases(phases: List<PhaseEntity>)

    @Query("SELECT * FROM phases WHERE sessionId = :sessionId ORDER BY start ASC")
    fun getPhasesForSession(sessionId: String): Flow<List<PhaseEntity>>

    @Query("SELECT * FROM phases WHERE sessionId = :sessionId ORDER BY start ASC")
    suspend fun getPhasesForSessionOnce(sessionId: String): List<PhaseEntity>

    @Query("SELECT * FROM phases WHERE sessionId = :sessionId AND type = 'LIGHT' ORDER BY start DESC LIMIT 1")
    suspend fun getLatestLightPhase(sessionId: String): PhaseEntity?

    @Query("DELETE FROM phases WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)
}

@Dao
interface SensorSampleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: SensorSampleEntity)

    @Query("SELECT * FROM sensor_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getSamplesForSession(sessionId: String): List<SensorSampleEntity>

    @Query("DELETE FROM sensor_samples WHERE timestamp < :cutoff")
    suspend fun purgeSamplesOlderThan(cutoff: Long)
}

@Dao
interface SensorSummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: SensorSummaryEntity)

    @Query("SELECT * FROM sensor_summary WHERE sessionId = :sessionId")
    suspend fun getSummaryForSession(sessionId: String): SensorSummaryEntity?
}

@Dao
interface RecommendationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendation(rec: RecommendationEntity)

    @Query("SELECT * FROM recommendations WHERE userId = :userId ORDER BY createdAt DESC")
    fun getRecommendationsForUser(userId: String): Flow<List<RecommendationEntity>>

    @Query("SELECT * FROM recommendations WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    suspend fun getRecommendationsForSession(sessionId: String): List<RecommendationEntity>

    @Query("SELECT COUNT(*) FROM recommendations WHERE userId = :userId")
    suspend fun countForUser(userId: String): Int

    @Query("DELETE FROM recommendations WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)
}

@Dao
interface PasswordResetTokenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToken(token: PasswordResetTokenEntity)

    @Query("SELECT * FROM password_reset_tokens WHERE token = :token AND used = 0 LIMIT 1")
    suspend fun findValidToken(token: String): PasswordResetTokenEntity?

    @Query("UPDATE password_reset_tokens SET used = 1 WHERE tokenId = :tokenId")
    suspend fun markUsed(tokenId: String)

    @Query("DELETE FROM password_reset_tokens WHERE userId = :userId")
    suspend fun deleteTokensForUser(userId: String)

    @Query("DELETE FROM password_reset_tokens WHERE expiresAt < :now")
    suspend fun purgeExpired(now: Long = System.currentTimeMillis())
}
