package com.vcapp.voicechanger.audio

/** Feedback delay line = the "Echo" control. */
class EchoUnit(private val sampleRate: Int) {

    private val maxDelay = sampleRate * 2                 // up to 2000 ms
    private val line = FloatArray(maxDelay)
    private var index = 0

    var delayMs: Int = 250
    var feedback: Float = 0.35f
    var mix: Float = 0f                                   // 0 = dry, 1 = full echo

    fun reset() {
        java.util.Arrays.fill(line, 0f)
        index = 0
    }

    fun process(x: Float): Float {
        if (mix <= 0.001f) return x
        val delaySamples = ((delayMs.coerceIn(20, 2000)) * sampleRate / 1000).coerceIn(1, maxDelay - 1)
        var readIndex = index - delaySamples
        if (readIndex < 0) readIndex += maxDelay
        val delayed = line[readIndex]
        line[index] = x + delayed * feedback.coerceIn(0f, 0.95f)
        index = (index + 1) % maxDelay
        return x * (1f - mix) + delayed * mix
    }
}

/** Classic Schroeder reverb: 4 parallel combs into 2 series all-passes. */
class ReverbUnit(sampleRate: Int) {

    private class Comb(size: Int) {
        val buf = FloatArray(size)
        var idx = 0
        var feedback = 0.84f
        var damp = 0.2f
        var store = 0f
        fun process(x: Float): Float {
            val out = buf[idx]
            store = out * (1f - damp) + store * damp
            buf[idx] = x + store * feedback
            idx = (idx + 1) % buf.size
            return out
        }
        fun reset() { java.util.Arrays.fill(buf, 0f); idx = 0; store = 0f }
    }

    private class AllPass(size: Int) {
        val buf = FloatArray(size)
        var idx = 0
        fun process(x: Float): Float {
            val buffered = buf[idx]
            val out = -x + buffered
            buf[idx] = x + buffered * 0.5f
            idx = (idx + 1) % buf.size
            return out
        }
        fun reset() { java.util.Arrays.fill(buf, 0f); idx = 0 }
    }

    private val scale = sampleRate / 44100f
    private val combs = intArrayOf(1116, 1188, 1277, 1356).map { Comb((it * scale).toInt().coerceAtLeast(8)) }
    private val allPasses = intArrayOf(556, 441).map { AllPass((it * scale).toInt().coerceAtLeast(8)) }

    /** 0 = off, 1 = cathedral. */
    var amount: Float = 0f
    var roomSize: Float = 0.7f

    fun reset() {
        combs.forEach { it.reset() }
        allPasses.forEach { it.reset() }
    }

    fun process(x: Float): Float {
        if (amount <= 0.001f) return x
        var wet = 0f
        for (c in combs) {
            c.feedback = 0.7f + roomSize.coerceIn(0f, 1f) * 0.28f
            wet += c.process(x)
        }
        wet /= combs.size
        for (a in allPasses) wet = a.process(wet)
        return x * (1f - amount * 0.6f) + wet * amount
    }
}
