package com.redx.codex.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.redx.codex.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var soundSend: Int = 0
    private var soundReplyDone: Int = 0
    private var soundNeedsInput: Int = 0
    private var soundCatchUpDone: Int = 0
    private var soundError: Int = 0

    init {
        try {
            soundSend = soundPool.load(context, R.raw.sound_effect_send, 1)
            soundReplyDone = soundPool.load(context, R.raw.sound_effect_reply_done, 1)
            soundNeedsInput = soundPool.load(context, R.raw.sound_effect_needs_input, 1)
            soundCatchUpDone = soundPool.load(context, R.raw.sound_effect_catch_up_done, 1)
            soundError = soundPool.load(context, R.raw.sound_effect_error, 1)
        } catch (_: Exception) {}
    }

    fun playSend() {
        if (soundSend != 0) soundPool.play(soundSend, 1f, 1f, 1, 0, 1f)
    }

    fun playReplyDone() {
        if (soundReplyDone != 0) soundPool.play(soundReplyDone, 1f, 1f, 1, 0, 1f)
    }

    fun playNeedsInput() {
        if (soundNeedsInput != 0) soundPool.play(soundNeedsInput, 1f, 1f, 1, 0, 1f)
    }

    fun playCatchUpDone() {
        if (soundCatchUpDone != 0) soundPool.play(soundCatchUpDone, 1f, 1f, 1, 0, 1f)
    }

    fun playError() {
        if (soundError != 0) soundPool.play(soundError, 1f, 1f, 1, 0, 1f)
    }
}
