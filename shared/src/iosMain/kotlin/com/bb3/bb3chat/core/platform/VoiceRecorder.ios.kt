package com.bb3.bb3chat.core.platform

import com.bb3.bb3chat.core.bridge.VoiceBridgeHolder

class IosVoiceRecorder : VoiceRecorder {
    override fun startRecording(): Boolean = VoiceBridgeHolder.startRecording()
    override fun stopRecording(): VoiceClip? = VoiceBridgeHolder.stopRecording()
    override fun cancelRecording() = VoiceBridgeHolder.cancelRecording()
    override fun isRecording(): Boolean = VoiceBridgeHolder.isRecording()
    override fun elapsedMs(): Long = VoiceBridgeHolder.elapsedMs()
}

class IosVoicePlayer : VoicePlayer {
    override fun play(bytes: ByteArray, mimeType: String) = VoiceBridgeHolder.play(bytes)
    override fun stop() = VoiceBridgeHolder.stopPlayback()
}
