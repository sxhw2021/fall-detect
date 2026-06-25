package com.falldetect.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detection_events")
data class DetectionEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float,
    val wasFalsePositive: Boolean = false
)
