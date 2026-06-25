package com.falldetect.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<Settings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: Settings)

    @Query("UPDATE settings SET sensitivityLevel = :level WHERE id = 1")
    suspend fun updateSensitivity(level: Int)

    @Query("UPDATE settings SET customVoiceText = :text WHERE id = 1")
    suspend fun updateVoiceText(text: String)
}
