package com.falldetect.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.falldetect.app.service.FallDetectionService
import com.falldetect.app.service.PermissionManager
import com.falldetect.app.ui.screens.HomeScreen
import com.falldetect.app.ui.screens.SettingsScreen
import com.falldetect.app.ui.theme.FallDetectTheme
import com.falldetect.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val permissionManager = PermissionManager(this)
        if (!permissionManager.hasAllPermissions()) {
            permissionManager.requestPermissions(this)
        }
        
        setContent {
            FallDetectTheme {
                MainNavigation()
            }
        }
    }

    private fun startMonitoringService() {
        val intent = Intent(this, FallDetectionService::class.java).apply {
            action = "ACTION_START_MONITORING"
        }
        startForegroundService(intent)
    }

    private fun stopMonitoringService() {
        val intent = Intent(this, FallDetectionService::class.java).apply {
            action = "ACTION_STOP_MONITORING"
        }
        startService(intent)
    }

    @Composable
    fun MainNavigation() {
        val navController = rememberNavController()
        val viewModel: MainViewModel = viewModel()
        val settings by viewModel.settings.collectAsState()
        val todayDetectionCount by viewModel.todayDetectionCount.collectAsState()
        val recentEvents by viewModel.recentEvents.collectAsState()

        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    settings = settings,
                    todayDetectionCount = todayDetectionCount,
                    recentEvents = recentEvents,
                    onToggleMonitoring = { enable ->
                        viewModel.toggleMonitoring(enable)
                        if (enable) startMonitoringService()
                        else stopMonitoringService()
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }
            
            composable("settings") {
                SettingsScreen(
                    settings = settings,
                    onBack = { navController.popBackStack() },
                    onUpdateSensitivity = viewModel::updateSensitivity,
                    onUpdateVoiceText = viewModel::updateVoiceText,
                    onToggleAlarm = viewModel::toggleAlarm,
                    onToggleVoice = viewModel::toggleVoice
                )
            }
        }
    }
}
