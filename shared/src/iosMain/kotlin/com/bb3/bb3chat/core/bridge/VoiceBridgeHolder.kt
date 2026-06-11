package com.bb3.bb3chat.core.bridge

import com.bb3.bb3chat.core.platform.VoiceClip

object VoiceBridgeHolder {
    private var startFn: (() -> Boolean)? = null
    private var stopFn: (() -> VoiceClip?)? = null
    private var cancelFn: (() -> Unit)? = null
    private var elapsedFn: (() -> Long)? = null
    private var isRecordingFn: (() -> Boolean)? = null
    private var playFn: ((ByteArray) -> Unit)? = null
    private var stopPlayFn: (() -> Unit)? = null

    fun registerRecorder(
        start: () -> Boolean,
        stop: () -> VoiceClip?,
        cancel: () -> Unit,
        elapsed: () -> Long,
        isRecording: () -> Boolean
    ) {
        startFn = start
        stopFn = stop
        cancelFn = cancel
        elapsedFn = elapsed
        isRecordingFn = isRecording
    }

    fun registerPlayer(play: (ByteArray) -> Unit, stop: () -> Unit) {
        playFn = play
        stopPlayFn = stop
    }

    fun startRecording(): Boolean = startFn?.invoke() ?: false

    fun stopRecording(): VoiceClip? = stopFn?.invoke()

    fun cancelRecording() { cancelFn?.invoke() }

    fun elapsedMs(): Long = elapsedFn?.invoke() ?: 0L

    fun isRecording(): Boolean = isRecordingFn?.invoke() ?: false

    fun play(bytes: ByteArray) { playFn?.invoke(bytes) }

    fun stopPlayback() { stopPlayFn?.invoke() }
}
