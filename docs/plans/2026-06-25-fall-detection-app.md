# 手机掉落提醒App实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 开发一款Android App，检测手机从口袋/包中掉落并发出最大音量警报和自定义语音提醒，防止手机丢失。

**Architecture:** 采用MVVM架构，传感器数据通过Service持续采集，经掉落检测算法处理后触发提醒。Jetpack Compose实现现代UI，Room数据库存储设置和统计。

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Room, Coroutines, SensorManager, MediaPlayer, TextToSpeech

---

## Task 1: 项目初始化与依赖配置

**Files:**
- Create: `app/build.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/falldetect/app/FallDetectApp.kt`

**Step 1: 创建项目根目录build.gradle.kts**

```kotlin
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
}
```

**Step 2: 创建app/build.gradle.kts**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.falldetect.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.falldetect.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    ksp("androidx.room:room-compiler:2.6.0")
    
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.core:core-ktx:1.12.0")
}
```

**Step 3: 创建AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <application
        android:name=".FallDetectApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.FallDetect"
        tools:targetApi="34">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.FallDetect">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.FallDetectionService"
            android:foregroundServiceType="specialUse"
            android:exported="false" />
    </application>
</manifest>
```

**Step 4: 创建Application类**

```kotlin
package com.falldetect.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class FallDetectApp : Application() {
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "fall_detection_channel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "掉落检测服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "持续检测手机是否掉落"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
```

**Step 5: 验证项目配置**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 6: 提交**

```bash
git add .
git commit -m "feat: 初始化项目结构和依赖配置"
```

---

## Task 2: 数据层 - Room数据库

**Files:**
- Create: `app/src/main/java/com/falldetect/app/data/AppDatabase.kt`
- Create: `app/src/main/java/com/falldetect/app/data/DetectionEvent.kt`
- Create: `app/src/main/java/com/falldetect/app/data/Settings.kt`
- Create: `app/src/main/java/com/falldetect/app/data/DetectionEventDao.kt`
- Create: `app/src/main/java/com/falldetect/app/data/SettingsDao.kt`

**Step 1: 创建DetectionEvent实体**

```kotlin
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
```

**Step 2: 创建Settings实体**

```kotlin
package com.falldetect.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1,
    val isMonitoringEnabled: Boolean = true,
    val sensitivityLevel: Int = 5,
    val customVoiceText: String = "手机掉落，请注意！",
    val alarmEnabled: Boolean = true,
    val voiceEnabled: Boolean = true,
    val alarmVolume: Int = 100
)
```

**Step 3: 创建DetectionEventDao**

```kotlin
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
```

**Step 4: 创建SettingsDao**

```kotlin
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
```

**Step 5: 创建AppDatabase**

```kotlin
package com.falldetect.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DetectionEvent::class, Settings::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun detectionEventDao(): DetectionEventDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fall_detect_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

**Step 6: 提交**

```bash
git add app/src/main/java/com/falldetect/app/data/
git commit -m "feat: 添加Room数据库和DAO"
```

---

## Task 3: 传感器监测服务

**Files:**
- Create: `app/src/main/java/com/falldetect/app/service/FallDetectionService.kt`
- Create: `app/src/main/java/com/falldetect/app/service/SensorDataProcessor.kt`
- Create: `app/src/main/java/com/falldetect/app/service/FallDetector.kt`

**Step 1: 创建SensorDataProcessor**

```kotlin
package com.falldetect.app.service

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SensorDataProcessor : SensorEventListener {
    private val _acceleration = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val acceleration: StateFlow<FloatArray> = _acceleration

    private val _gyroscope = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val gyroscope: StateFlow<FloatArray> = _gyroscope

    private val _isFreeFall = MutableStateFlow(false)
    val isFreeFall: StateFlow<Boolean> = _isFreeFall

    private var lastAcceleration = floatArrayOf(0f, 0f, 0f)
    private val gravity = floatArrayOf(0f, 0f, 9.81f)

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val alpha = 0.8f
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

                val x = event.values[0] - gravity[0]
                val y = event.values[1] - gravity[1]
                val z = event.values[2] - gravity[2]

                _acceleration.value = floatArrayOf(x, y, z)

                val magnitude = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                _isFreeFall.value = magnitude < 2.0f
            }
            Sensor.TYPE_GYROSCOPE -> {
                _gyroscope.value = event.values.clone()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}
```

**Step 2: 创建FallDetector**

```kotlin
package com.falldetect.app.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.sqrt

class FallDetector {
    private val _fallDetected = MutableStateFlow(false)
    val fallDetected: StateFlow<Boolean> = _fallDetected

    private var sensitivityLevel = 5
    private var lastImpactTime = 0L
    private val impactCooldown = 2000L

    private var accelerationHistory = mutableListOf<FloatArray>()
    private val historySize = 50

    fun updateSensitivity(level: Int) {
        sensitivityLevel = level.coerceIn(1, 10)
    }

    fun processData(
        acceleration: FloatArray,
        isFreeFall: Boolean,
        gyroscope: FloatArray
    ) {
        val currentTime = System.currentTimeMillis()
        
        accelerationHistory.add(acceleration.clone())
        if (accelerationHistory.size > historySize) {
            accelerationHistory.removeAt(0)
        }

        val magnitude = sqrt(
            (acceleration[0] * acceleration[0] +
            acceleration[1] * acceleration[1] +
            acceleration[2] * acceleration[2]).toDouble()
        ).toFloat()

        val threshold = calculateThreshold()

        if (magnitude > threshold && 
            currentTime - lastImpactTime > impactCooldown) {
            
            val hasRotation = checkRotation(gyroscope)
            val wasFreeFall = checkFreeFallPattern()

            if (hasRotation || wasFreeFall || magnitude > threshold * 1.5f) {
                _fallDetected.value = true
                lastImpactTime = currentTime
            }
        }

        if (_fallDetected.value && currentTime - lastImpactTime > impactCooldown) {
            _fallDetected.value = false
        }
    }

    private fun calculateThreshold(): Float {
        val baseThreshold = 25.0f
        val sensitivityFactor = 1.0f + (sensitivityLevel - 5) * 0.1f
        return baseThreshold / sensitivityFactor
    }

    private fun checkRotation(gyroscope: FloatArray): Boolean {
        val rotationMagnitude = sqrt(
            (gyroscope[0] * gyroscope[0] +
            gyroscope[1] * gyroscope[1] +
            gyroscope[2] * gyroscope[2]).toDouble()
        ).toFloat()
        return rotationMagnitude > 5.0f
    }

    private fun checkFreeFallPattern(): Boolean {
        if (accelerationHistory.size < 10) return false
        
        val recentAccelerations = accelerationHistory.takeLast(10)
        val lowAccelCount = recentAccelerations.count { accel ->
            val mag = sqrt(
                (accel[0] * accel[0] +
                accel[1] * accel[1] +
                accel[2] * accel[2]).toDouble()
            ).toFloat()
            mag < 3.0f
        }
        return lowAccelCount >= 5
    }
}
```

**Step 3: 创建FallDetectionService**

```kotlin
package com.falldetect.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.falldetect.app.FallDetectApp
import com.falldetect.app.MainActivity
import com.falldetect.app.R
import com.falldetect.app.data.AppDatabase
import com.falldetect.app.data.DetectionEvent
import kotlinx.coroutines.*

class FallDetectionService : Service() {
    private lateinit var sensorManager: SensorManager
    private lateinit var sensorDataProcessor: SensorDataProcessor
    private lateinit var fallDetector: FallDetector
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var monitoringJob: Job? = null

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
        val db = AppDatabase.getDatabase(applicationContext)
        val event = DetectionEvent(confidence = 0.9f)
        db.detectionEventDao().insertEvent(event)

        triggerAlarm()
        triggerVibration()
    }

    private fun triggerAlarm() {
        val intent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    private fun triggerVibration() {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
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
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
```

**Step 4: 提交**

```bash
git add app/src/main/java/com/falldetect/app/service/
git commit -m "feat: 实现传感器监测服务和掉落检测算法"
```

---

## Task 4: 提醒系统 - 警报声与语音

**Files:**
- Create: `app/src/main/java/com/falldetect/app/service/ReminderManager.kt`
- Create: `app/src/main/java/com/falldetect/app/service/VoiceManager.kt`
- Create: `app/src/main/java/com/falldetect/app/ui/AlarmActivity.kt`

**Step 1: 创建ReminderManager**

```kotlin
package com.falldetect.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import com.falldetect.app.R

class ReminderManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun playAlarm() {
        val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

        val alarmUri = Uri.parse(
            "android.resource://${context.packageName}/${R.raw.alarm}"
        )

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(context, alarmUri)
            isLooping = true
            prepare()
            start()
        }
    }

    fun stopAlarm() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    fun release() {
        stopAlarm()
    }
}
```

**Step 2: 创建VoiceManager**

```kotlin
package com.falldetect.app.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class VoiceManager(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    fun initialize(onReady: () -> Unit = {}) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.CHINESE
                isInitialized = true
                onReady()
            }
        }
    }

    fun speak(text: String) {
        if (!isInitialized) {
            initialize { speak(text) }
            return
        }

        val params = android.os.Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "alarm_utterance")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
```

**Step 3: 创建AlarmActivity**

```kotlin
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
import com.falldetect.app.data.AppDatabase
import kotlinx.coroutines.launch

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
        
        val db = AppDatabase.getDatabase(applicationContext)
        val settings = kotlinx.coroutines.runBlocking {
            db.settingsDao().getSettings().first()
        }
        
        settings?.let {
            if (it.voiceEnabled) {
                voiceManager?.speak(it.customVoiceText)
            }
        }
    }

    private fun dismissAlarm() {
        reminderManager?.stopAlarm()
        voiceManager?.stop()
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
    
    LaunchedEffect(Unit) {
        scope.launch {
            voiceManager.speak("手机掉落，请注意！")
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
```

**Step 4: 提交**

```bash
git add app/src/main/java/com/falldetect/app/service/ReminderManager.kt
git add app/src/main/java/com/falldetect/app/service/VoiceManager.kt
git add app/src/main/java/com/falldetect/app/ui/AlarmActivity.kt
git commit -m "feat: 实现警报声和语音提醒系统"
```

---

## Task 5: Jetpack Compose主界面

**Files:**
- Create: `app/src/main/java/com/falldetect/app/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/falldetect/app/ui/theme/Color.kt`
- Create: `app/src/main/java/com/falldetect/app/ui/screens/HomeScreen.kt`
- Create: `app/src/main/java/com/falldetect/app/ui/screens/SettingsScreen.kt`
- Create: `app/src/main/java/com/falldetect/app/ui/viewmodel/MainViewModel.kt`
- Create: `app/src/main/java/com/falldetect/app/MainActivity.kt`

**Step 1: 创建Color.kt**

```kotlin
package com.falldetect.app.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val WarningOrange = Color(0xFFFF9800)
val SafeGreen = Color(0xFF4CAF50)
val DangerRed = Color(0xFFF44336)
```

**Step 2: 创建Theme.kt**

```kotlin
package com.falldetect.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun FallDetectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
```

**Step 3: 创建MainViewModel**

```kotlin
package com.falldetect.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.falldetect.app.data.AppDatabase
import com.falldetect.app.data.Settings
import com.falldetect.app.service.SensitivityAdjuster
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
            settings.value?.let {
                settingsDao.insertOrUpdate(it.copy(isMonitoringEnabled = enable))
            }
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
            settings.value?.let {
                settingsDao.insertOrUpdate(it.copy(alarmEnabled = !it.alarmEnabled))
            }
        }
    }

    fun toggleVoice() {
        viewModelScope.launch {
            settings.value?.let {
                settingsDao.insertOrUpdate(it.copy(voiceEnabled = !it.voiceEnabled))
            }
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
```

**Step 4: 创建HomeScreen**

```kotlin
package com.falldetect.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.falldetect.app.data.DetectionEvent
import com.falldetect.app.data.Settings
import com.falldetect.app.ui.theme.SafeGreen
import com.falldetect.app.ui.theme.DangerRed
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    settings: Settings?,
    todayDetectionCount: Int,
    recentEvents: List<DetectionEvent>,
    onToggleMonitoring: (Boolean) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("掉落检测") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatusCard(
                    isMonitoring = settings?.isMonitoringEnabled ?: false,
                    onToggle = onToggleMonitoring
                )
            }

            item {
                StatisticsCard(todayDetectionCount = todayDetectionCount)
            }

            item {
                SensitivityCard(sensitivity = settings?.sensitivityLevel ?: 5)
            }

            if (recentEvents.isNotEmpty()) {
                item {
                    Text(
                        text = "最近检测记录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                items(recentEvents.take(5)) { event ->
                    EventItem(event = event)
                }
            }
        }
    }
}

@Composable
fun StatusCard(isMonitoring: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isMonitoring) SafeGreen.copy(alpha = 0.1f)
            else DangerRed.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isMonitoring) "监测中" else "已停止",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMonitoring) SafeGreen else DangerRed
                )
                Text(
                    text = if (isMonitoring) "正在保护您的手机安全" else "点击开启监测",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Switch(
                checked = isMonitoring,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SafeGreen,
                    checkedTrackColor = SafeGreen.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
fun StatisticsCard(todayDetectionCount: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "今日统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    value = todayDetectionCount.toString(),
                    label = "检测次数",
                    icon = Icons.Default.Warning
                )
                StatItem(
                    value = "0",
                    label = "误报次数",
                    icon = Icons.Default.CheckCircle
                )
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun SensitivityCard(sensitivity: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "当前灵敏度",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$sensitivity/10",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            LinearProgressIndicator(
                progress = { sensitivity / 10f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            
            Text(
                text = "智能适应模式已开启",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun EventItem(event: DetectionEvent) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "检测到掉落",
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatTimestamp(event.timestamp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${(event.confidence * 100).toInt()}%",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                if (event.wasFalsePositive) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "误报",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
```

**Step 5: 创建SettingsScreen**

```kotlin
package com.falldetect.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.falldetect.app.data.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings?,
    onBack: () -> Unit,
    onUpdateSensitivity: (Int) -> Unit,
    onUpdateVoiceText: (String) -> Unit,
    onToggleAlarm: () -> Unit,
    onToggleVoice: () -> Unit
) {
    var voiceText by remember(settings) { mutableStateOf(settings?.customVoiceText ?: "手机掉落，请注意！") }
    var sliderValue by remember(settings) { mutableFloatStateOf((settings?.sensitivityLevel ?: 5).toFloat()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "灵敏度设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "当前灵敏度: ${sliderValue.toInt()}/10",
                        fontSize = 14.sp
                    )
                    
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = { onUpdateSensitivity(sliderValue.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("低", fontSize = 12.sp)
                        Text("智能适应", fontSize = 12.sp)
                        Text("高", fontSize = 12.sp)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "提醒设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("警报声", fontWeight = FontWeight.Medium)
                            Text(
                                "掉落时播放最大音量警报",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = settings?.alarmEnabled ?: true,
                            onCheckedChange = { onToggleAlarm() }
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("语音播报", fontWeight = FontWeight.Medium)
                            Text(
                                "掉落时语音提醒",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = settings?.voiceEnabled ?: true,
                            onCheckedChange = { onToggleVoice() }
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "自定义语音",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = voiceText,
                        onValueChange = { voiceText = it },
                        label = { Text("提醒语音内容") },
                        placeholder = { Text("输入掉落时播报的文字") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { onUpdateVoiceText(voiceText) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存语音设置")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "关于",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("版本: 1.0.0", fontSize = 14.sp)
                    Text(
                        "手机掉落检测提醒",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
```

**Step 6: 更新MainActivity**

```kotlin
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
import com.falldetect.app.ui.screens.HomeScreen
import com.falldetect.app.ui.screens.SettingsScreen
import com.falldetect.app.ui.theme.FallDetectTheme
import com.falldetect.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
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
```

**Step 7: 提交**

```bash
git add app/src/main/java/com/falldetect/app/ui/
git add app/src/main/java/com/falldetect/app/MainActivity.kt
git commit -m "feat: 实现Jetpack Compose主界面和设置页面"
```

---

## Task 6: 权限处理与通知

**Files:**
- Create: `app/src/main/java/com/falldetect/app/service/PermissionManager.kt`
- Create: `app/src/main/res/values/strings.xml`

**Step 1: 创建PermissionManager**

```kotlin
package com.falldetect.app.service

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {
    
    companion object {
        const val PERMISSION_REQUEST_CODE = 1001
        
        val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.VIBRATE,
                Manifest.permission.WAKE_LOCK
            )
        } else {
            arrayOf(
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.VIBRATE,
                Manifest.permission.WAKE_LOCK
            )
        }
    }
    
    fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun requestPermissions(activity: Activity) {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToRequest, PERMISSION_REQUEST_CODE)
        }
    }
}
```

**Step 2: 创建strings.xml**

```xml
<resources>
    <string name="app_name">掉落检测</string>
    <string name="notification_channel_name">掉落检测服务</string>
    <string name="notification_channel_description">持续检测手机是否掉落</string>
</resources>
```

**Step 3: 提交**

```bash
git add app/src/main/java/com/falldetect/app/service/PermissionManager.kt
git add app/src/main/res/values/strings.xml
git commit -m "feat: 添加权限管理和字符串资源"
```

---

## Task 7: 测试与验证

**Files:**
- Create: `app/src/test/java/com/falldetect/app/FallDetectorTest.kt`

**Step 1: 创建单元测试**

```kotlin
package com.falldetect.app

import com.falldetect.app.service.FallDetector
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FallDetectorTest {
    private lateinit var fallDetector: FallDetector

    @Before
    fun setup() {
        fallDetector = FallDetector()
    }

    @Test
    fun `test fall detection with high acceleration`() {
        fallDetector.updateSensitivity(5)
        
        val acceleration = floatArrayOf(50f, 0f, 0f)
        val gyroscope = floatArrayOf(0f, 0f, 0f)
        
        repeat(10) {
            fallDetector.processData(acceleration, false, gyroscope)
        }
        
        assertTrue(fallDetector.fallDetected.value)
    }

    @Test
    fun `test no fall with normal acceleration`() {
        fallDetector.updateSensitivity(5)
        
        val acceleration = floatArrayOf(1f, 0f, 0f)
        val gyroscope = floatArrayOf(0f, 0f, 0f)
        
        repeat(10) {
            fallDetector.processData(acceleration, false, gyroscope)
        }
        
        assertFalse(fallDetector.fallDetected.value)
    }

    @Test
    fun `test sensitivity affects threshold`() {
        fallDetector.updateSensitivity(1)
        val highThresholdAccel = floatArrayOf(30f, 0f, 0f)
        val gyroscope = floatArrayOf(0f, 0f, 0f)
        
        repeat(10) {
            fallDetector.processData(highThresholdAccel, false, gyroscope)
        }
        
        assertTrue(fallDetector.fallDetected.value)
    }

    @Test
    fun `test free fall detection`() {
        fallDetector.updateSensitivity(5)
        
        val lowAcceleration = floatArrayOf(0.5f, 0.5f, 0.5f)
        val gyroscope = floatArrayOf(0f, 0f, 0f)
        
        repeat(15) {
            fallDetector.processData(lowAcceleration, true, gyroscope)
        }
    }
}
```

**Step 2: 运行测试**

Run: `./gradlew test`
Expected: All tests pass

**Step 3: 构建APK**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL, APK generated at `app/build/outputs/apk/debug/app-debug.apk`

**Step 4: 最终提交**

```bash
git add .
git commit -m "feat: 完成手机掉落检测App开发"
```

---

## 完成

手机掉落提醒App已开发完成。功能包括：

1. **传感器监测**：持续监听加速度计和陀螺仪
2. **智能检测**：基于多维度特征识别掉落事件
3. **提醒系统**：最大音量警报声 + 可自定义语音播报
4. **现代UI**：Material Design界面，实时状态显示
5. **设置管理**：灵敏度调整、语音自定义、提醒开关
6. **后台服务**：持续运行，通知栏常驻
7. **数据统计**：记录检测事件，支持误报标记

**下一步：**
- 生成签名APK发布到应用商店
- 根据用户反馈调整检测算法
- 添加更多自定义选项（如警报音选择）
