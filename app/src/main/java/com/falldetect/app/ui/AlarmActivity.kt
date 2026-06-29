package com.falldetect.app.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.falldetect.app.service.ReminderManager
import com.falldetect.app.service.VoiceManager
import com.falldetect.app.service.FallDetectionService
import com.falldetect.app.data.AppDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class AlarmActivity : ComponentActivity() {
    private var reminderManager: ReminderManager? = null
    private var voiceManager: VoiceManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        reminderManager = ReminderManager(this)
        voiceManager = VoiceManager(this).apply {
            initialize()
        }

        setContent {
            AlarmScreen(
                onDismiss = { dismissAlarm() },
                voiceManager = voiceManager!!,
                onStartAlarm = { audioUri -> startAlarm(audioUri) }
            )
        }
    }

    private fun startAlarm(audioUri: String?) {
        reminderManager?.playAlarm(audioUri)
    }

    private fun dismissAlarm() {
        reminderManager?.stopAlarm()
        voiceManager?.stop()
        FallDetectionService.stopAlarm()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        reminderManager?.release()
        voiceManager?.shutdown()
    }
}

@Composable
fun AlarmScreen(
    onDismiss: () -> Unit,
    voiceManager: VoiceManager,
    onStartAlarm: (String?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        scope.launch {
            val db = AppDatabase.getDatabase(context)
            val settings = db.settingsDao().getSettings().first()
            
            settings?.let {
                onStartAlarm(it.customAudioUri)
                
                if (it.voiceEnabled && it.customAudioUri == null) {
                    voiceManager.speak(it.customVoiceText)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "\u26a0\ufe0f",
                fontSize = 80.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "\u624b\u673a\u6389\u843d\uff01",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "\u68c0\u6d4b\u5230\u624b\u673a\u6389\u843d\uff0c\u8bf7\u6ce8\u610f\u67e5\u770b",
                fontSize = 24.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                modifier = Modifier
                    .width(200.dp)
                    .height(60.dp)
            ) {
                Text(
                    text = "\u5173\u95ed\u8b66\u62a5",
                    fontSize = 20.sp,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
