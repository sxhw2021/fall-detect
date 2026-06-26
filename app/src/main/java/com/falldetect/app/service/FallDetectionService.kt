package com.falldetect.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.falldetect.app.AlarmHelper
import com.falldetect.app.FallDetectApp
import com.falldetect.app.MainActivity
import com.falldetect.app.R
import com.falldetect.app.data.AppDatabase
import com.falldetect.app.data.DetectionEvent
import com.falldetect.app.ui.AlarmActivity
import kotlinx.coroutines.*

class FallDetectionService : Service() {
    companion object {
        var isAlarmActive = false
            private set
        
        fun resetAlarmState() {
            isAlarmActive = false
        }
    }
    
    private lateinit var sensorManager: SensorManager
    private lateinit var sensorDataProcessor: SensorDataProcessor
    private lateinit var fallDetector: FallDetector
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var monitoringJob: Job? = null
    private var lastAlarmTime = 0L
    private val alarmCooldown = 5000L
    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorDataProcessor = SensorDataProcessor()
        fallDetector = FallDetector()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_START_MONITORING" -> startMonitoring()
            "ACTION_STOP_MONITORING" -> stopMonitoring()
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        accelerometer?.let {
            sensorManager.registerListener(
                sensorDataProcessor,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        gyroscope?.let {
            sensorManager.registerListener(
                sensorDataProcessor,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        startForeground(FallDetectApp.NOTIFICATION_ID, createNotification())
        startFallDetection()
    }

    private fun startFallDetection() {
        monitoringJob = serviceScope.launch {
            while (isActive) {
                val acceleration = sensorDataProcessor.acceleration.value
                val isFreeFall = sensorDataProcessor.isFreeFall.value
                val gyroscope = sensorDataProcessor.gyroscope.value

                fallDetector.processData(acceleration, isFreeFall, gyroscope)

                if (fallDetector.fallDetected.value) {
                    onFallDetected()
                }

                delay(100)
            }
        }
    }

    private suspend fun onFallDetected() {
        val currentTime = System.currentTimeMillis()
        if (isAlarmActive || currentTime - lastAlarmTime < alarmCooldown) {
            return
        }
        
        val db = AppDatabase.getDatabase(applicationContext)
        val confidence = fallDetector.calculateConfidence()
        val event = DetectionEvent(confidence = confidence)
        db.detectionEventDao().insertEvent(event)

        isAlarmActive = true
        lastAlarmTime = currentTime
        
        triggerVibration()
        triggerSound()
        triggerAlarmNotification()
        
        AlarmHelper.scheduleAlarm(applicationContext)
        
        delay(3000)
        stopSound()
    }

    private fun triggerAlarmNotification() {
        val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        }
        
        try {
            startActivity(alarmIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, FallDetectApp.ALARM_CHANNEL_ID)
            .setContentTitle("⚠️ 手机掉落！")
            .setContentText("检测到手机掉落，请注意查看")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(FallDetectApp.ALARM_NOTIFICATION_ID, notification)
    }

    private fun triggerVibration() {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        
        val pattern = longArrayOf(0, 500, 200, 500, 200, 500, 200, 500)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun triggerSound() {
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
            
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, alarmUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopSound() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FallDetectApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("掉落检测已启动")
            .setContentText("正在监测手机状态...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        sensorManager.unregisterListener(sensorDataProcessor)
        stopSound()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSound()
        serviceScope.cancel()
    }
}
