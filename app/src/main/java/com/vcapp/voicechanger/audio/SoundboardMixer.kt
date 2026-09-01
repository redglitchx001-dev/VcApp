package com.vcapp.voicechanger.audio

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Mixes decoded MP3 / audio clips into the outgoing stream, so the person on
 * the other side of the call hears them together with (or instead of) your voice.
 */
class SoundboardMixer {

    private class Voice(
        val id: String,
        val pcm: FloatArray,
        val loop: Boolean,
        var position: Int = 0,
        @Volatile var volume: Float = 1f,
        @Volatile var stopping: Boolean = false,
        var fade: Float = 0f
    )

    private val voices = CopyOnWriteArrayList<Voice>()

    /** Global soundboard volume, 0..2. */
    @Volatile var masterVolume: Float = 1f

    /** When true the microphone is muted while a clip is playing. */
    @Volatile var duckVoiceWhilePlaying: Boolean = false

    /** How much the mic is lowered when ducking (0 = silent). */
    @Volatile var duckLevel: Float = 0.15f

    val isPlaying: Boolean get() = voices.isNotEmpty()

    fun play(id: String, pcm: FloatArray, loop: Boolean, volume: Float = 1f) {
        voices.removeAll { it.id == id }
        voices.add(Voice(id, pcm, loop, volume = volume))
    }

    fun stop(id: String) {
        voices.filter { it.id == id }.forEach { it.stopping = true }
    }

    fun stopAll() {
        voices.forEach { it.stopping = true }
    }

    fun clear() = voices.clear()

    fun isPlaying(id: String) = voices.any { it.id == id && !it.stopping }

    /**
     * Adds the currently playing clips into [buffer] and returns the mic
     * attenuation the caller should apply (1 = untouched).
     */
    fun mixInto(buffer: FloatArray, length: Int): Float {
        if (voices.isEmpty()) return 1f

        val master = masterVolume.coerceIn(0f, 2f)
        val finished = ArrayList<Voice>()

        for (v in voices) {
            var pos = v.position
            for (i in 0 until length) {
                if (pos >= v.pcm.size) {
                    if (v.loop && !v.stopping) pos = 0 else break
                }
                // 10 ms fade in / out to avoid clicks.
                val step = 1f / 480f
                v.fade = if (v.stopping) (v.fade - step).coerceAtLeast(0f)
                else (v.fade + step).coerceAtMost(1f)

                buffer[i] += v.pcm[pos] * v.volume * master * v.fade
                pos++
            }
            v.position = pos
            if ((pos >= v.pcm.size && !v.loop) || (v.stopping && v.fade <= 0f)) finished.add(v)
        }
        voices.removeAll(finished.toSet())

        return if (duckVoiceWhilePlaying && voices.isNotEmpty()) duckLevel.coerceIn(0f, 1f) else 1f
    }
}
