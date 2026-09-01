package com.vcapp.voicechanger.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.Process
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Where the processed audio is sent.
 */
enum class OutputRoute {
    /** Loudspeaker – the phone's speaker plays your changed voice so the mic of
     *  the call (or of the person next to you) picks it up. Works with every app. */
    SPEAKER,

    /** Earpiece / headset – private monitoring, nobody else hears it. */
    EARPIECE,

    /** Bluetooth SCO headset. */
    BLUETOOTH
}

/**
 * Real-time engine: microphone -> effects -> speaker, with an MP3 soundboard
 * mixed into the same stream and optional recording of the result.
 */
class VoiceEngine(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 44100
        private const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }

    val processor = VoiceProcessor(SAMPLE_RATE)
    val soundboard = SoundboardMixer()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var record: AudioRecord? = null
    private var track: AudioTrack? = null
    private var thread: Thread? = null

    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null

    @Volatile private var running = false
    @Volatile var route: OutputRoute = OutputRoute.SPEAKER
        set(value) { field = value; applyRoute() }

    /** When false only the soundboard is heard, the mic is muted. */
    @Volatile var micEnabled: Boolean = true

    /** Live monitoring on/off (engine keeps running but output is silenced). */
    @Volatile var outputMuted: Boolean = false

    private var recordFile: RandomAccessFile? = null
    private var recordedBytes = 0
    @Volatile var isRecording = false
        private set

    var onError: ((String) -> Unit)? = null

    val isRunning: Boolean get() = running

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (running) return true

        val inMin = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING)
        val outMin = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT, ENCODING)
        if (inMin <= 0 || outMin <= 0) {
            onError?.invoke("This device does not support 44.1 kHz mono capture.")
            return false
        }

        val frames = 1024
        val inBufferBytes = maxOf(inMin, frames * 2 * 4)
        val outBufferBytes = maxOf(outMin, frames * 2 * 4)

        try {
            val rec = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE, CHANNEL_IN, ENCODING, inBufferBytes
            )
            if (rec.state != AudioRecord.STATE_INITIALIZED) {
                rec.release()
                onError?.invoke("Microphone is busy or permission was denied.")
                return false
            }

            val trk = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(ENCODING)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_OUT)
                        .build()
                )
                .setBufferSizeInBytes(outBufferBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            attachSystemEffects(rec.audioSessionId)

            record = rec
            track = trk

            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            applyRoute()

            rec.startRecording()
            trk.play()
            running = true

            thread = Thread({ loop(frames) }, "VcApp-Audio").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
            return true
        } catch (t: Throwable) {
            onError?.invoke(t.message ?: "Could not start the audio engine.")
            stop()
            return false
        }
    }

    private fun loop(frames: Int) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

        val shorts = ShortArray(frames)
        val floats = FloatArray(frames)
        val outShorts = ShortArray(frames)
        val rec = record
        val trk = track

        while (running && rec != null && trk != null) {
            val read = rec.read(shorts, 0, frames)
            if (read <= 0) continue

            val micGain = if (!micEnabled) 0f else {
                if (soundboard.duckVoiceWhilePlaying && soundboard.isPlaying)
                    soundboard.duckLevel else 1f
            }

            for (i in 0 until read) floats[i] = shorts[i] / 32768f * micGain

            processor.process(floats, read)
            soundboard.mixInto(floats, read)

            val mute = outputMuted
            for (i in 0 until read) {
                var v = floats[i]
                if (mute) v = 0f
                if (v > 1f) v = 1f
                if (v < -1f) v = -1f
                outShorts[i] = (v * 32767f).toInt().toShort()
            }

            trk.write(outShorts, 0, read)
            if (isRecording) writePcm(outShorts, read)
        }
    }

    fun stop() {
        running = false
        try { thread?.join(400) } catch (_: InterruptedException) {}
        thread = null

        stopRecording()

        try { record?.stop() } catch (_: Throwable) {}
        try { record?.release() } catch (_: Throwable) {}
        try { track?.stop() } catch (_: Throwable) {}
        try { track?.release() } catch (_: Throwable) {}
        record = null
        track = null

        releaseSystemEffects()
        soundboard.clear()
        processor.reset()

        try {
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (_: Throwable) {}
    }

    // ------------------------------------------------------------------
    // Routing
    // ------------------------------------------------------------------

    private fun applyRoute() {
        try {
            when (route) {
                OutputRoute.SPEAKER -> {
                    stopSco()
                    setSpeaker(true)
                }
                OutputRoute.EARPIECE -> {
                    stopSco()
                    setSpeaker(false)
                }
                OutputRoute.BLUETOOTH -> {
                    setSpeaker(false)
                    @Suppress("DEPRECATION")
                    audioManager.startBluetoothSco()
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothScoOn = true
                }
            }
        } catch (_: Throwable) {}
    }

    private fun setSpeaker(on: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val type = if (on) AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            else AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            val device = audioManager.availableCommunicationDevices.firstOrNull { it.type == type }
            if (device != null) {
                audioManager.setCommunicationDevice(device)
                return
            }
        }
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = on
    }

    private fun stopSco() {
        try {
            @Suppress("DEPRECATION")
            if (audioManager.isBluetoothScoOn) {
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn = false
                @Suppress("DEPRECATION")
                audioManager.stopBluetoothSco()
            }
        } catch (_: Throwable) {}
    }

    // ------------------------------------------------------------------
    // System DSP (helps a lot against speaker feedback)
    // ------------------------------------------------------------------

    var systemAecEnabled = true
        set(value) { field = value; aec?.enabled = value }

    var systemNsEnabled = true
        set(value) { field = value; ns?.enabled = value }

    private fun attachSystemEffects(sessionId: Int) {
        try {
            if (AcousticEchoCanceler.isAvailable()) {
                aec = AcousticEchoCanceler.create(sessionId)?.apply { enabled = systemAecEnabled }
            }
            if (NoiseSuppressor.isAvailable()) {
                ns = NoiseSuppressor.create(sessionId)?.apply { enabled = systemNsEnabled }
            }
            if (AutomaticGainControl.isAvailable()) {
                agc = AutomaticGainControl.create(sessionId)?.apply { enabled = false }
            }
        } catch (_: Throwable) {}
    }

    private fun releaseSystemEffects() {
        try { aec?.release() } catch (_: Throwable) {}
        try { ns?.release() } catch (_: Throwable) {}
        try { agc?.release() } catch (_: Throwable) {}
        aec = null; ns = null; agc = null
    }

    // ------------------------------------------------------------------
    // Recording the processed output to a WAV file
    // ------------------------------------------------------------------

    fun startRecording(target: File): Boolean {
        if (isRecording) return false
        return try {
            target.parentFile?.mkdirs()
            val raf = RandomAccessFile(target, "rw")
            raf.setLength(0)
            raf.write(ByteArray(44)) // placeholder header
            recordFile = raf
            recordedBytes = 0
            isRecording = true
            true
        } catch (t: Throwable) {
            onError?.invoke("Could not start recording: ${t.message}")
            false
        }
    }

    private fun writePcm(data: ShortArray, length: Int) {
        val raf = recordFile ?: return
        try {
            val bb = ByteBuffer.allocate(length * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until length) bb.putShort(data[i])
            raf.write(bb.array())
            recordedBytes += length * 2
        } catch (_: Throwable) {}
    }

    fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        val raf = recordFile ?: return
        recordFile = null
        try {
            writeWavHeader(raf, recordedBytes)
            raf.close()
        } catch (_: Throwable) {}
    }

    private fun writeWavHeader(raf: RandomAccessFile, dataBytes: Int) {
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(36 + dataBytes)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1)              // PCM
        header.putShort(1)              // mono
        header.putInt(SAMPLE_RATE)
        header.putInt(SAMPLE_RATE * 2)  // byte rate
        header.putShort(2)              // block align
        header.putShort(16)             // bits
        header.put("data".toByteArray())
        header.putInt(dataBytes)
        raf.seek(0)
        raf.write(header.array())
    }
}
