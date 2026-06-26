package com.falldetect.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1,
    val isMonitoringEnabled: Boolean = false,
    val sensitivityLevel: Int = 5,
    val customVoiceText: String = "手机掉落，请注意！",
    val alarmEnabled: Boolean = true,
    val voiceEnabled: Boolean = true,
    val alarmVolume: Int = 100
)
