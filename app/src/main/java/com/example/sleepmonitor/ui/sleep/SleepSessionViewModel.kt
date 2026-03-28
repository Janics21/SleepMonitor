package com.example.sleepmonitor.ui.sleep

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleepmonitor.data.repository.SleepRepository
import com.example.sleepmonitor.data.repository.SleepSessionReport
import com.example.sleepmonitor.service.SleepMonitorService
import com.example.sleepmonitor.ui.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class SleepSessionState {
    data object Initializing : SleepSessionState()
    data class Ready(val showOnboarding: Boolean) : SleepSessionState()
    data class Monitoring(val sessionId: String, val alarmWindowStart: String, val alarmWindowEnd: String) : SleepSessionState()
    data class Report(val report: SleepSessionReport) : SleepSessionState()
    data object FeedbackSaved : SleepSessionState()
    data class Error(val message: String) : SleepSessionState()
}

class SleepSessionViewModel(
    private val sleepRepository: SleepRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableLiveData<SleepSessionState>(SleepSessionState.Initializing)
    val state: LiveData<SleepSessionState> = _state

    init {
        recoverState()
    }

    fun recoverState() {
        val userId = sessionManager.getUserId()
        if (userId.isNullOrBlank()) {
            _state.value = SleepSessionState.Error("Necesitas iniciar sesion para registrar una noche")
            return
        }

        viewModelScope.launch {
            val activeSession = sessionManager.getActiveSleepSession()
            if (activeSession != null) {
                val session = sleepRepository.getSessionById(activeSession.sessionId)
                if (session != null && session.endTime == null) {
                    _state.postValue(
                        SleepSessionState.Monitoring(
                            sessionId = session.sessionId,
                            alarmWindowStart = session.alarmWindowStart,
                            alarmWindowEnd = session.alarmWindowEnd
                        )
                    )
                    return@launch
                }
                sessionManager.clearActiveSleepSession()
            }

            val pendingFeedback = sleepRepository.getLatestPendingFeedback(userId)
            if (pendingFeedback != null) {
                val report = sleepRepository.getReport(pendingFeedback.sessionId)
                if (report != null) {
                    _state.postValue(SleepSessionState.Report(report))
                    return@launch
                }
            }

            _state.postValue(
                SleepSessionState.Ready(
                    showOnboarding = !sessionManager.hasSeenOnboarding()
                )
            )
        }
    }

    fun dismissOnboarding() {
        sessionManager.setOnboardingSeen()
        _state.value = SleepSessionState.Ready(showOnboarding = false)
    }

    fun startSession(
        context: Context,
        alarmWindowStart: String,
        alarmWindowEnd: String,
        sampleIntervalMs: Long
    ) {
        val userId = sessionManager.getUserId()
        if (userId.isNullOrBlank()) {
            _state.value = SleepSessionState.Error("No hay una sesion de usuario activa")
            return
        }

        viewModelScope.launch {
            runCatching {
                val session = sleepRepository.startSession(
                    userId = userId,
                    alarmWindowStart = alarmWindowStart,
                    alarmWindowEnd = alarmWindowEnd,
                    sampleIntervalMs = sampleIntervalMs
                )

                sessionManager.saveActiveSleepSession(
                    sessionId = session.sessionId,
                    userId = userId,
                    startedAt = session.startTime,
                    alarmWindowStart = alarmWindowStart,
                    alarmWindowEnd = alarmWindowEnd,
                    sampleIntervalMs = sampleIntervalMs
                )

                val intent = Intent(context, SleepMonitorService::class.java).apply {
                    action = SleepMonitorService.ACTION_START
                    putExtra(SleepMonitorService.EXTRA_SESSION_ID, session.sessionId)
                    putExtra(SleepMonitorService.EXTRA_USER_ID, userId)
                    putExtra(SleepMonitorService.EXTRA_ALARM_START, alarmWindowStart)
                    putExtra(SleepMonitorService.EXTRA_ALARM_END, alarmWindowEnd)
                    putExtra(SleepMonitorService.EXTRA_STARTED_AT, session.startTime)
                    putExtra(SleepMonitorService.EXTRA_SAMPLE_INTERVAL_MS, sampleIntervalMs)
                }
                context.startForegroundService(intent)
                _state.postValue(
                    SleepSessionState.Monitoring(
                        sessionId = session.sessionId,
                        alarmWindowStart = alarmWindowStart,
                        alarmWindowEnd = alarmWindowEnd
                    )
                )
            }.onFailure {
                sessionManager.clearActiveSleepSession()
                _state.postValue(
                    SleepSessionState.Error(it.message ?: "No se pudo iniciar la sesion de sueno")
                )
            }
        }
    }

    fun stopSession(context: Context) {
        val current = state.value as? SleepSessionState.Monitoring
        if (current == null) {
            _state.value = SleepSessionState.Error("No hay una sesion activa")
            return
        }

        viewModelScope.launch {
            val intent = Intent(context, SleepMonitorService::class.java).apply {
                action = SleepMonitorService.ACTION_STOP
                putExtra(SleepMonitorService.EXTRA_SESSION_ID, current.sessionId)
                putExtra(SleepMonitorService.EXTRA_STOP_REASON, SleepMonitorService.STOP_REASON_MANUAL)
            }
            context.startService(intent)
            awaitFinalReport(current.sessionId)
        }
    }

    fun submitUserFeedback(score: Int) {
        val report = (state.value as? SleepSessionState.Report)?.report ?: return
        viewModelScope.launch {
            sleepRepository.setUserFeedback(report.session.sessionId, score)
            _state.postValue(SleepSessionState.FeedbackSaved)
        }
    }

    fun skipFeedback() {
        val report = (state.value as? SleepSessionState.Report)?.report ?: return
        viewModelScope.launch {
            sleepRepository.skipUserFeedback(report.session.sessionId)
            _state.postValue(SleepSessionState.Ready(showOnboarding = false))
        }
    }

    fun resetToReady() {
        _state.value = SleepSessionState.Ready(showOnboarding = false)
    }

    private suspend fun awaitFinalReport(sessionId: String) {
        repeat(20) {
            val report = sleepRepository.getReport(sessionId)
            if (report?.session?.endTime != null) {
                sessionManager.clearActiveSleepSession()
                _state.postValue(SleepSessionState.Report(report))
                return
            }
            delay(500)
        }
        sessionManager.clearActiveSleepSession()
        _state.postValue(
            SleepSessionState.Error("La sesion se detuvo, pero el reporte aun no esta listo. Abre la pantalla de nuevo en unos segundos.")
        )
    }
}
