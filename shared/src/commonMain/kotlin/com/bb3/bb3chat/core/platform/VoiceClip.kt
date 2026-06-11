package com.bb3.bb3chat.core.platform

data class VoiceClip(
    val bytes: ByteArray,
    val durationMs: Long,
    val mimeType: String = "audio/mp4"
)

interface VoiceRecorder {
    fun startRecording(): Boolean
    fun stopRecording(): VoiceClip?
    fun cancelRecording()
    fun isRecording(): Boolean
    fun elapsedMs(): Long
}

interface VoicePlayer {
    fun play(bytes: ByteArray, mimeType: String = "audio/mp4")
    fun stop()
}
