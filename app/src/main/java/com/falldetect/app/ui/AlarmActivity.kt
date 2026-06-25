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
        
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        reminderManager = ReminderManager(this)
        voiceManager = VoiceManager(this).apply {
            initialize()
        }

        setContent {
            AlarmScreen(
                onDismiss = { dismissAlarm() },
                voiceManager = voiceManager!!
            )
        }

        startAlarm()
    }

    private fun startAlarm() {
        reminderManager?.playAlarm()
    }

    private fun dismissAlarm() {
        reminderManager?.stopAlarm()
        voiceManager?.stop()
        FallDetectionService.resetAlarmState()
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
    voiceManager: VoiceManager
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        scope.launch {
            val db = AppDatabase.getDatabase(context)
            val settings = db.settingsDao().getSettings().first()
            
            settings?.let {
                if (it.voiceEnabled) {
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
                text = "⚠️",
                fontSize = 80.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "手机掉落！",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "检测到手机掉落，请注意查看",
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
                    text = "关闭警报",
                    fontSize = 20.sp,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}