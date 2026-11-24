package com.redx.codex

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RedXApp : Application() {

    companion object {
        const val CHANNEL_ID_VOICE = "voice_call_channel"
        const val CHANNEL_ID_DICTATION = "dictation_channel"
        const val CHANNEL_ID_MESSAGES = "messages_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val voiceChannel = NotificationChannel(
                CHANNEL_ID_VOICE,
                getString(R.string.notification_channel_voice),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active voice call background service"
            }

            val dictationChannel = NotificationChannel(
                CHANNEL_ID_DICTATION,
                getString(R.string.notification_channel_dictation),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Voice dictation recording service"
            }

            val messagesChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                getString(R.string.notification_channel_messages),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for incoming messages and completions"
            }

            notificationManager.createNotificationChannels(
                listOf(voiceChannel, dictationChannel, messagesChannel)
            )
        }
    }
}
