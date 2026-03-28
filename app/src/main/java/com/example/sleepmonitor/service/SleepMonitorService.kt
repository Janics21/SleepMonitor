package com.example.sleepmonitor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaRecorder
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.sleepmonitor.R
import com.example.sleepmonitor.data.local.SleepDatabase
import com.example.sleepmonitor.data.local.entities.SensorSampleEntity
import com.example.sleepmonitor.data.repository.SleepRepository
import com.example.sleepmonitor.domain.sleep.RuleBasedSleepInsightsEngine
import com.example.sleepmonitor.ui.utils.SessionManager
import com.example.sleepmonitor.ui.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

class SleepMonitorService : Service(), SensorEventListener {

    companion object {
        const val CHANNEL_ID = "sleep_monitor_channel"
        private const val ALARM_CHANNEL_ID = "sleep_alarm_channel"
        private const val NOTIFICATION_ID = 1001
        private const val ALARM_NOTIFICATION_ID = 1002

        const val ACTION_START = "com.example.sleepmonitor.START_SESSION"
        const val ACTION_STOP = "com.example.sleepmonitor.STOP_SESSION"

        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_ALARM_START = "alarm_start"
        const val EXTRA_ALARM_END = "alarm_end"
        const val EXTRA_STARTED_AT = "started_at"
        const val EXTRA_SAMPLE_INTERVAL_MS = "sample_interval_ms"
        const val EXTRA_STOP_REASON = "stop_reason"

        const val STOP_REASON_MANUAL = "MANUAL_STOP"
        const val STOP_REASON_SMART = "SMART_ALARM"
        const val STOP_REASON_WINDOW_END = "WINDOW_END"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: SleepRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var sensorManager: SensorManager

    private var accelerometer: Sensor? = null
    private var recorder: MediaRecorder? = null
    private var recorderOutputPath: String? = null
    private var samplingJob: Job? = null

    private var sessionId: String = ""
    private var userId: String = ""
    private var startedAt: Long = 0L
    private var alarmStart: String = ""
    private var alarmEnd: String = ""
    private var sampleIntervalMs: Long = 10_000L
    private var finalizing = false

    private var currentX = 0f
    private var currentY = 0f
    private var currentZ = 0f

    override fun onCreate() {
        super.onCreate()
        repository = SleepRepository(SleepDatabase.getInstance(applicationContext))
        sessionManager = SessionManager(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_STOP -> handleStop(intent.getStringExtra(EXTRA_STOP_REASON) ?: STOP_REASON_MANUAL)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopCapture()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            currentX = event.values[0]
            currentY = event.values[1]
            currentZ = event.values[2]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun handleStart(intent: Intent) {
        sessionId = intent.getStringExtra(EXTRA_SESSION_ID).orEmpty()
        userId = intent.getStringExtra(EXTRA_USER_ID).orEmpty()
        alarmStart = intent.getStringExtra(EXTRA_ALARM_START).orEmpty()
        alarmEnd = intent.getStringExtra(EXTRA_ALARM_END).orEmpty()
        startedAt = intent.getLongExtra(EXTRA_STARTED_AT, System.currentTimeMillis())
        sampleIntervalMs = intent.getLongExtra(EXTRA_SAMPLE_INTERVAL_MS, 10_000L)

        if (sessionId.isBlank() || userId.isBlank()) {
            stopSelf()
            return
        }

        startForeground(NOTIFICATION_ID, buildMonitoringNotification())
        startCapture()
    }

    private fun handleStop(stopReason: String) {
        if (sessionId.isBlank()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        serviceScope.launch {
            finalizeAndStop(stopReason)
        }
    }

    private fun startCapture() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        startNoiseRecorder()
        samplingJob?.cancel()
        samplingJob = serviceScope.launch {
            while (isActive) {
                val sample = SensorSampleEntity(
                    sessionId = sessionId,
                    timestamp = System.currentTimeMillis(),
                    accelerometerX = currentX,
                    accelerometerY = currentY,
                    accelerometerZ = currentZ,
                    movementMagnitude = computeMovementMagnitude(currentX, currentY, currentZ),
                    noiseDecibels = readNoiseDb()
                )
                repository.insertSample(sample)
                checkWakeWindow(sample.timestamp)
                delay(sampleIntervalMs)
            }
        }
    }

    private fun stopCapture() {
        samplingJob?.cancel()
        samplingJob = null
        sensorManager.unregisterListener(this)
        stopNoiseRecorder()
    }

    private fun computeMovementMagnitude(x: Float, y: Float, z: Float): Float {
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        return magnitude - SensorManager.STANDARD_GRAVITY
    }

    private fun startNoiseRecorder() {
        stopNoiseRecorder()
        runCatching {
            val outputFile = java.io.File(cacheDir, "sleep_monitor_noise.3gp")
            recorderOutputPath = outputFile.absolutePath
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
        }.onFailure {
            serviceScope.launch { repository.recordSensorFailure(sessionId) }
        }
    }

    private fun stopNoiseRecorder() {
        runCatching {
            recorder?.stop()
        }
        recorder?.release()
        recorder = null
        recorderOutputPath?.let { path ->
            runCatching { java.io.File(path).delete() }
        }
        recorderOutputPath = null
    }

    private fun readNoiseDb(): Float {
        val amplitude = recorder?.maxAmplitude?.toFloat() ?: return 0f
        return if (amplitude > 0f) 20f * log10(amplitude) else 0f
    }

    private suspend fun checkWakeWindow(now: Long) {
        val windowStartMillis = TimeUtils.nextOccurrenceFrom(startedAt, alarmStart)
        val windowEndMillis = TimeUtils.nextOccurrenceFrom(windowStartMillis - 60_000L, alarmEnd)

        if (now < windowStartMillis) return

        if (now >= windowEndMillis) {
            finalizeAndStop(STOP_REASON_WINDOW_END)
            return
        }

        val recentSamples = repository.getSamples(sessionId)
        if (RuleBasedSleepInsightsEngine.isGoodWakeWindow(recentSamples)) {
            finalizeAndStop(STOP_REASON_SMART)
        }
    }

    private suspend fun finalizeAndStop(stopReason: String) {
        if (finalizing) return
        finalizing = true
        stopCapture()

        val wakeTime = System.currentTimeMillis()
        runCatching {
            repository.finalizeSession(
                sessionId = sessionId,
                wakeTime = wakeTime,
                wakeMethod = stopReason
            )
            sessionManager.clearActiveSleepSession()
            if (stopReason != STOP_REASON_MANUAL) {
                fireAlarmNotification(stopReason)
            }
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun fireAlarmNotification(stopReason: String) {
        vibrate()
        val text = when (stopReason) {
            STOP_REASON_SMART -> "Hemos detectado una ventana suave para despertar."
            STOP_REASON_WINDOW_END -> "Se alcanzo el final de tu ventana y se cerro la sesion."
            else -> "Tu sesion de sueno ha terminado."
        }
        val notification = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sleep)
            .setContentTitle("Despertador inteligente")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .build()
        notificationManager.notify(ALARM_NOTIFICATION_ID, notification)
    }

    private fun buildMonitoringNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sleep)
            .setContentTitle("Monitor de sueno activo")
            .setContentText("Registrando movimiento y ruido de forma local.")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Monitoreo de sueno",
                NotificationManager.IMPORTANCE_LOW
            )
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                ALARM_CHANNEL_ID,
                "Despertador inteligente",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1_500L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(1_500L)
        }
    }
}
