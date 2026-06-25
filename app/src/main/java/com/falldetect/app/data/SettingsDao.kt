package com.falldetect.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<Settings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: Settings)

    @Query("UPDATE settings SET isMonitoringEnabled = :enabled WHERE id = 1")
    suspend fun updateMonitoring(enabled: Boolean)

    @Query("UPDATE settings SET sensitivityLevel = :level WHERE id = 1")
    suspend fun updateSensitivity(level: Int)

    @Query("UPDATE settings SET customVoiceText = :text WHERE id = 1")
    suspend fun updateVoiceText(text: String)

    @Query("UPDATE settings SET alarmEnabled = :enabled WHERE id = 1")
    suspend fun updateAlarm(enabled: Boolean)

    @Query("UPDATE settings SET voiceEnabled = :enabled WHERE id = 1")
    suspend fun updateVoice(enabled: Boolean)
}
