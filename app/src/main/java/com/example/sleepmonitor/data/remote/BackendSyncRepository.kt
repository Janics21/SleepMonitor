package com.example.sleepmonitor.data.remote

import android.content.Context
import com.example.sleepmonitor.data.local.SleepDatabase
import com.example.sleepmonitor.data.local.entities.RecommendationEntity
import com.example.sleepmonitor.data.local.entities.SleepSessionEntity
import com.example.sleepmonitor.data.local.entities.UserEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BackendSyncRepository(
    private val db: SleepDatabase,
    context: Context,
    private val api: BackendApi,
    private val gson: Gson = Gson()
) {
    private val prefs = context.getSharedPreferences("backend_sync_queue", Context.MODE_PRIVATE)

    suspend fun pushUser(user: UserEntity) {
        val payload = user.toPayload()
        runCatching { api.createUser(payload) }
            .onFailure { enqueueTask(SyncTask("user", user.userId, "upsert", gson.toJson(payload))) }
    }

    suspend fun deleteUser(userId: String) {
        runCatching { api.deleteUser(userId) }
            .onFailure { enqueueTask(SyncTask("user", userId, "delete", "{}")) }
    }

    suspend fun pushSession(session: SleepSessionEntity) {
        val payload = session.toPayload()
        runCatching {
            if (session.endTime == null) {
                api.createSession(payload)
            } else {
                api.updateSession(session.sessionId, payload)
            }
        }.onFailure { enqueueTask(SyncTask("session", session.sessionId, "upsert", gson.toJson(payload))) }
    }

    suspend fun pushRecommendation(recommendation: RecommendationEntity) {
        val payload = recommendation.toPayload()
        runCatching { api.createRecommendation(payload) }
            .onFailure { enqueueTask(SyncTask("recommendation", recommendation.recId, "upsert", gson.toJson(payload))) }
    }

    suspend fun pullRecommendations(userId: String) {
        runCatching { api.getRecommendations(userId) }
            .onSuccess { recommendations ->
                recommendations.forEach { db.recommendationDao().insertRecommendation(it.toEntity()) }
            }
    }

    suspend fun flushPendingTasks() {
        val pending = getTasks()
        pending.forEach { task ->
            val sent = when (task.entityType) {
                "user" -> sendUserTask(task)
                "session" -> sendSessionTask(task)
                "recommendation" -> sendRecommendationTask(task)
                else -> false
            }
            if (sent) {
                removeTask(task)
            }
        }
    }

    private suspend fun sendUserTask(task: SyncTask): Boolean = runCatching {
        if (task.action == "delete") {
            api.deleteUser(task.entityId)
        } else {
            api.updateUser(task.entityId, gson.fromJson(task.payloadJson, UserPayload::class.java))
        }
    }.isSuccess

    private suspend fun sendSessionTask(task: SyncTask): Boolean = runCatching {
        api.updateSession(task.entityId, gson.fromJson(task.payloadJson, SleepSessionPayload::class.java))
    }.isSuccess

    private suspend fun sendRecommendationTask(task: SyncTask): Boolean = runCatching {
        api.createRecommendation(gson.fromJson(task.payloadJson, RecommendationPayload::class.java))
    }.isSuccess

    private fun enqueueTask(task: SyncTask) {
        val updated = getTasks().toMutableList().apply { add(task) }
        prefs.edit().putString(QUEUE_KEY, gson.toJson(updated)).apply()
    }

    private fun removeTask(task: SyncTask) {
        val updated = getTasks().toMutableList().apply { remove(task) }
        prefs.edit().putString(QUEUE_KEY, gson.toJson(updated)).apply()
    }

    private fun getTasks(): List<SyncTask> {
        val raw = prefs.getString(QUEUE_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<SyncTask>>() {}.type
        return gson.fromJson(raw, type) ?: emptyList()
    }

    companion object {
        private const val QUEUE_KEY = "pending_tasks"
    }
}

data class SyncTask(
    val entityType: String,
    val entityId: String,
    val action: String,
    val payloadJson: String
)
