package com.redx.codex.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.redx.codex.data.local.PreferencesManager
import com.redx.codex.data.network.WebSocketManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var webSocketManager: WebSocketManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = preferencesManager.settings.firstOrNull()
                    settings?.serverConfig?.let { config ->
                        webSocketManager.connect(config)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
