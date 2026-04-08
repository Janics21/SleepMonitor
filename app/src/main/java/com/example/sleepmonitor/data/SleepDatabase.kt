package com.example.sleepmonitor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.sleepmonitor.data.local.dao.PasswordResetTokenDao
import com.example.sleepmonitor.data.local.dao.PhaseDao
import com.example.sleepmonitor.data.local.dao.RecommendationDao
import com.example.sleepmonitor.data.local.dao.SensorSampleDao
import com.example.sleepmonitor.data.local.dao.SensorSummaryDao
import com.example.sleepmonitor.data.local.dao.SleepSessionDao
import com.example.sleepmonitor.data.local.dao.SyncTaskDao
import com.example.sleepmonitor.data.local.dao.UserDao
import com.example.sleepmonitor.data.local.entities.PasswordResetTokenEntity
import com.example.sleepmonitor.data.local.entities.PhaseEntity
import com.example.sleepmonitor.data.local.entities.RecommendationEntity
import com.example.sleepmonitor.data.local.entities.SensorSampleEntity
import com.example.sleepmonitor.data.local.entities.SensorSummaryEntity
import com.example.sleepmonitor.data.local.entities.SleepSessionEntity
import com.example.sleepmonitor.data.local.entities.SyncTaskEntity
import com.example.sleepmonitor.data.local.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        SleepSessionEntity::class,
        PhaseEntity::class,
        SensorSampleEntity::class,
        SensorSummaryEntity::class,
        RecommendationEntity::class,
        PasswordResetTokenEntity::class,
        SyncTaskEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SleepDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun phaseDao(): PhaseDao
    abstract fun sensorSampleDao(): SensorSampleDao
    abstract fun sensorSummaryDao(): SensorSummaryDao
    abstract fun recommendationDao(): RecommendationDao
    abstract fun passwordResetTokenDao(): PasswordResetTokenDao
    abstract fun syncTaskDao(): SyncTaskDao

    companion object {
        @Volatile
        private var instance: SleepDatabase? = null

        fun getInstance(context: Context): SleepDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SleepDatabase::class.java,
                    "sleep_monitor.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
