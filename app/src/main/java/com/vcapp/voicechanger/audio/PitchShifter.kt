package com.vcapp.voicechanger.audio

import kotlin.math.abs
import kotlin.math.max

/**
 * Overlap-add granular pitch shifter.
 *
 * It is intentionally lightweight (no FFT) so it can run in real time on the
 * audio thread of low/mid range phones. Quality is "voice-changer grade",
 * which is exactly what we want here.
 *
 * ratio 1.0  = unchanged
 * ratio 2.0  = one octave up   (chipmunk)
 * ratio 0.5  = one octave down (deep / demon)
 */
class PitchShifter(sampleRate: Int) {

    private val grain = max(256, sampleRate / 20)      // ~50 ms grain
    private val bufferSize = grain * 4
    private val buffer = FloatArray(bufferSize)

    private var writeIndex = 0
    private var readPos = 0f
    private var ratio = 1f
    private var crossfade = grain / 4

    fun setRatio(r: Float) {
        ratio = r.coerceIn(0.35f, 3.0f)
    }

    fun reset() {
        java.util.Arrays.fill(buffer, 0f)
        writeIndex = 0
        readPos = 0f
    }

    fun process(input: FloatArray, output: FloatArray, length: Int) {
        if (abs(ratio - 1f) < 0.001f) {
            System.arraycopy(input, 0, output, 0, length)
            return
        }

        for (i in 0 until length) {
            buffer[writeIndex] = input[i]
            writeIndex = (writeIndex + 1) % bufferSize

            // Distance between the write head and the (fractional) read head.
            var distance = writeIndex - readPos
            if (distance < 0) distance += bufferSize

            // Keep the read head inside a safe window, cross-fading when it
            // would run into the write head.
            if (distance < crossfade || distance > bufferSize - crossfade) {
                readPos = (writeIndex - grain.toFloat() + bufferSize) % bufferSize
                distance = grain.toFloat()
            }

            val idx = readPos.toInt()
            val frac = readPos - idx
            val s0 = buffer[idx % bufferSize]
            val s1 = buffer[(idx + 1) % bufferSize]
            var sample = s0 + (s1 - s0) * frac

            // Fade the grain edges so the seams are less audible.
            val edge = if (distance < crossfade * 2f) distance / (crossfade * 2f) else 1f
            sample *= edge.coerceIn(0f, 1f)

            output[i] = sample

            readPos += ratio
            if (readPos >= bufferSize) readPos -= bufferSize
            if (readPos < 0) readPos += bufferSize
        }
    }
}
