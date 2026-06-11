package com.bb3.bb3chat.core.platform

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AndroidVoiceRecorder(private val context: Context) : VoiceRecorder {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startMs = 0L

    override fun startRecording(): Boolean = runCatching {
        cancelRecording()
        val file = File(context.cacheDir, "bb3_voice_${System.currentTimeMillis()}.m4a")
        outputFile = file
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44_100)
            setMaxDuration(30_000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mediaRecorder
        startMs = System.currentTimeMillis()
        true
    }.getOrDefault(false)

    override fun stopRecording(): VoiceClip? {
        val file = outputFile ?: return null
        runCatching {
            recorder?.stop()
            recorder?.release()
        }
        recorder = null
        outputFile = null
        val duration = (System.currentTimeMillis() - startMs).coerceAtMost(30_000L)
        if (!file.exists() || file.length() == 0L) {
            file.delete()
            return null
        }
        val bytes = file.readBytes()
        file.delete()
        return VoiceClip(bytes = bytes, durationMs = duration.coerceAtLeast(500L))
    }

    override fun cancelRecording() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        outputFile?.delete()
        outputFile = null
        startMs = 0L
    }

    override fun isRecording(): Boolean = recorder != null

    override fun elapsedMs(): Long =
        if (startMs == 0L) 0L else (System.currentTimeMillis() - startMs).coerceAtMost(30_000L)
}

class AndroidVoicePlayer(private val context: Context) : VoicePlayer {

    private var player: MediaPlayer? = null

    override fun play(bytes: ByteArray, mimeType: String) {
        stop()
        val file = File(context.cacheDir, "bb3_play_${System.currentTimeMillis()}.m4a")
        file.writeBytes(bytes)
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener {
                file.delete()
                release()
            }
            prepare()
            start()
        }
    }

    override fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }
}
