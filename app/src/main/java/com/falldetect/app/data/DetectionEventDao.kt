package com.falldetect.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionEventDao {
    @Query("SELECT * FROM detection_events ORDER BY timestamp DESC LIMIT 100")
    fun getRecentEvents(): Flow<List<DetectionEvent>>

    @Insert
    suspend fun insertEvent(event: DetectionEvent): Long

    @Update
    suspend fun updateEvent(event: DetectionEvent)

    @Query("SELECT COUNT(*) FROM detection_events WHERE timestamp > :startTime")
    fun getEventCountSince(startTime: Long): Flow<Int>

    @Query("DELETE FROM detection_events")
    suspend fun clearAll()
}
