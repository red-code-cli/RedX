package com.redx.codex.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.redx.codex.MainActivity
import com.redx.codex.R
import com.redx.codex.RedXApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VoiceCallService : Service() {

    companion object {
        const val NOTIFICATION_ID = 2002
        const val ACTION_START_CALL = "com.redx.codex.action.START_CALL"
        const val ACTION_END_CALL = "com.redx.codex.action.END_CALL"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_END_CALL -> stopSelf()
            else -> startForegroundNotification()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundNotification() {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val endIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, VoiceCallService::class.java).apply { action = ACTION_END_CALL },
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, RedXApp.CHANNEL_ID_VOICE)
            .setContentTitle(getString(R.string.notification_voice_title))
            .setContentText("Voice call session active")
            .setSmallIcon(R.drawable.ic_splash_logo)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "End", endIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }
}
