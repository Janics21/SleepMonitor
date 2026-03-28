package com.example.sleepmonitor.ui.utils

import java.security.SecureRandom
import java.util.Base64
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.abs

object PasswordHasher {
    private const val algorithm = "PBKDF2WithHmacSHA256"
    private const val iterations = 120_000
    private const val keyLength = 256
    private const val saltLength = 16

    fun hash(password: String): String {
        val salt = ByteArray(saltLength).also { SecureRandom().nextBytes(it) }
        val hash = derive(password, salt, iterations)
        return listOf(
            "pbkdf2",
            iterations.toString(),
            Base64.getEncoder().encodeToString(salt),
            Base64.getEncoder().encodeToString(hash)
        ).joinToString("$")
    }

    fun verify(password: String, storedValue: String): Boolean {
        val parts = storedValue.split("$")
        if (parts.size != 4 || parts.first() != "pbkdf2") {
            return false
        }

        return runCatching {
            val rounds = parts[1].toInt()
            val salt = Base64.getDecoder().decode(parts[2])
            val currentHash = derive(password, salt, rounds)
            Base64.getEncoder().encodeToString(currentHash) == parts[3]
        }.getOrDefault(false)
    }

    private fun derive(password: String, salt: ByteArray, rounds: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, rounds, keyLength)
        return SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
    }
}

object IdUtils {
    fun newId(): String = UUID.randomUUID().toString()
}

object ValidationUtils {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean = emailRegex.matches(email.trim())

    fun isValidUsername(username: String): Boolean = username.trim().length >= 3

    fun isValidPassword(password: String): Boolean {
        val candidate = password.trim()
        return candidate.length >= 8 && candidate.any(Char::isDigit)
    }

    fun passwordsMatch(password: String, confirmation: String): Boolean = password == confirmation

    fun isPositiveInt(value: String): Boolean = value.toIntOrNull()?.let { it > 0 } == true
}

object TimeUtils {
    fun nowMillis(): Long = System.currentTimeMillis()

    fun in24Hours(): Long = nowMillis() + 24 * 60 * 60 * 1000L

    fun hhmm(hour: Int, minute: Int): String =
        String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

    fun nextOccurrenceFrom(referenceTime: Long, hhmm: String): Long {
        val (hour, minute) = hhmm.split(":").map { it.toInt() }
        val calendar = Calendar.getInstance().apply {
            timeInMillis = referenceTime
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= referenceTime) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    fun minutesBetween(startTime: Long, endTime: Long): Int {
        if (endTime <= startTime) return 0
        return ((endTime - startTime) / 60_000L).toInt()
    }

    fun formatDurationMinutes(totalMinutes: Int): String {
        val safe = abs(totalMinutes)
        val hours = safe / 60
        val minutes = safe % 60
        return if (hours > 0) "${hours} h ${minutes} min" else "$minutes min"
    }

    fun phaseLabel(type: String): String = when (type) {
        "AWAKE" -> "Despierto"
        "LIGHT" -> "Ligero"
        "DEEP" -> "Profundo"
        else -> type
    }
}
