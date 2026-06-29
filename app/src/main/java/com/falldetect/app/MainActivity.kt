package com.falldetect.app

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.falldetect.app.data.AppDatabase
import com.falldetect.app.service.FallDetectionService
import com.falldetect.app.ui.screens.HomeScreen
import com.falldetect.app.ui.screens.SettingsScreen
import com.falldetect.app.ui.theme.FallDetectTheme
import com.falldetect.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        requestOverlayPermission()
    }
    
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        requestFullScreenIntentPermission()
    }

    private val fullScreenIntentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        requestIgnoreBatteryOptimizations()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        requestPermissions()
        restoreMonitoringState()
        
        setContent {
            FallDetectTheme {
                MainNavigation()
            }
        }
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        permissions.add(Manifest.permission.VIBRATE)
        
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            requestOverlayPermission()
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
                return
            }
        }
        requestFullScreenIntentPermission()
    }
    
    private fun requestIgnoreBatteryOptimizations() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    private fun restoreMonitoringState() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val settings = db.settingsDao().getSettings().first()
            if (settings?.isMonitoringEnabled == true) {
                startMonitoringService()
            }
        }
    }

    private fun requestFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            if (!notificationManager.canUseFullScreenIntent()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:$packageName")
                }
                fullScreenIntentLauncher.launch(intent)
                return
            }
        }
        requestIgnoreBatteryOptimizations()
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
                    onUpdateAudioUri = viewModel::updateAudioUri,
                    onToggleAlarm = viewModel::toggleAlarm,
                    onToggleVoice = viewModel::toggleVoice
                )
            }
        }
    }
}
