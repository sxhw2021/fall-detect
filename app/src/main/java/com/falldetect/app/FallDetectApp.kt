package com.falldetect.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class FallDetectApp : Application() {
    companion object {
        const val NOTIFICATION_CHANNEL_ID = "fall_detection_channel"
        const val NOTIFICATION_ID = 1
        const val ALARM_CHANNEL_ID = "alarm_channel"
        const val ALARM_NOTIFICATION_ID = 2
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_description)
            }

            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "掉落警报",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "检测到手机掉落时的警报通知"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setBypassDnd(true)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(alarmChannel)
        }
    }
}
