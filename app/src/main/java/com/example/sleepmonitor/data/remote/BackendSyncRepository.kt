package com.example.sleepmonitor.data.remote

import com.example.sleepmonitor.data.local.SleepDatabase
import com.example.sleepmonitor.data.local.entities.RecommendationEntity
import com.example.sleepmonitor.data.local.entities.SleepSessionEntity
import com.example.sleepmonitor.data.local.entities.SyncTaskEntity
import com.example.sleepmonitor.data.local.entities.UserEntity
import com.example.sleepmonitor.ui.utils.IdUtils
import com.google.gson.Gson

class BackendSyncRepository(
    private val db: SleepDatabase,
    private val api: BackendApi,
    private val gson: Gson = Gson()
) {

    suspend fun pushUser(user: UserEntity) {
        val payload = user.toPayload()
        runCatching { api.createUser(payload) }
            .onFailure { enqueueTask("user", user.userId, "upsert", gson.toJson(payload)) }
    }

    suspend fun deleteUser(userId: String) {
        runCatching { api.deleteUser(userId) }
            .onFailure { enqueueTask("user", userId, "delete", "{}") }
    }

    suspend fun pushSession(session: SleepSessionEntity) {
        val payload = session.toPayload()
        runCatching {
            if (session.endTime == null) {
                api.createSession(payload)
            } else {
                api.updateSession(session.sessionId, payload)
            }
        }.onFailure { enqueueTask("session", session.sessionId, "upsert", gson.toJson(payload)) }
    }

    suspend fun pushRecommendation(recommendation: RecommendationEntity) {
        val payload = recommendation.toPayload()
        runCatching { api.createRecommendation(payload) }
            .onFailure { enqueueTask("recommendation", recommendation.recId, "upsert", gson.toJson(payload)) }
    }

    suspend fun pullRecommendations(userId: String) {
        runCatching { api.getRecommendations(userId) }
            .onSuccess { recommendations ->
                recommendations.forEach { db.recommendationDao().insertRecommendation(it.toEntity()) }
            }
    }

    suspend fun flushPendingTasks() {
        val pending = db.syncTaskDao().getAll()
        pending.forEach { task ->
            val sent = when (task.entityType) {
                "user" -> sendUserTask(task)
                "session" -> sendSessionTask(task)
                "recommendation" -> sendRecommendationTask(task)
                else -> false
            }
            if (sent) {
                db.syncTaskDao().deleteTask(task.taskId)
            }
        }
    }

    private suspend fun sendUserTask(task: SyncTaskEntity): Boolean = runCatching {
        if (task.action == "delete") {
            api.deleteUser(task.entityId)
        } else {
            api.updateUser(task.entityId, gson.fromJson(task.payloadJson, UserPayload::class.java))
        }
    }.isSuccess

    private suspend fun sendSessionTask(task: SyncTaskEntity): Boolean = runCatching {
        api.updateSession(task.entityId, gson.fromJson(task.payloadJson, SleepSessionPayload::class.java))
    }.isSuccess

    private suspend fun sendRecommendationTask(task: SyncTaskEntity): Boolean = runCatching {
        api.createRecommendation(gson.fromJson(task.payloadJson, RecommendationPayload::class.java))
    }.isSuccess

    private suspend fun enqueueTask(entityType: String, entityId: String, action: String, payloadJson: String) {
        db.syncTaskDao().insertTask(
            SyncTaskEntity(
                taskId = IdUtils.newId(),
                entityType = entityType,
                entityId = entityId,
                action = action,
                payloadJson = payloadJson
            )
        )
    }
}
