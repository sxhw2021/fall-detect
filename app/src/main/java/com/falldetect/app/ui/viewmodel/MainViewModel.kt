package com.falldetect.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.falldetect.app.data.AppDatabase
import com.falldetect.app.data.Settings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val settingsDao = db.settingsDao()
    private val eventDao = db.detectionEventDao()

    val settings: StateFlow<Settings?> = settingsDao.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayDetectionCount: StateFlow<Int> = eventDao.getEventCountSince(
        getStartOfDay()
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentEvents = eventDao.getRecentEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleMonitoring(enable: Boolean) {
        viewModelScope.launch {
            settingsDao.updateMonitoring(enable)
        }
    }

    fun updateSensitivity(level: Int) {
        viewModelScope.launch {
            settingsDao.updateSensitivity(level)
        }
    }

    fun updateVoiceText(text: String) {
        viewModelScope.launch {
            settingsDao.updateVoiceText(text)
        }
    }

    fun toggleAlarm() {
        viewModelScope.launch {
            val current = settings.value?.alarmEnabled ?: true
            settingsDao.updateAlarm(!current)
        }
    }

    fun toggleVoice() {
        viewModelScope.launch {
            val current = settings.value?.voiceEnabled ?: true
            settingsDao.updateVoice(!current)
        }
    }

    private fun getStartOfDay(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
