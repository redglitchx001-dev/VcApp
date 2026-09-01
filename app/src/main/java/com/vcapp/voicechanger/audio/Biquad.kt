package com.vcapp.voicechanger.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Simple Direct-Form-I biquad filter used for the bass / treble shelves
 * and for the low-cut of the microphone signal.
 */
class Biquad {

    private var b0 = 1f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f

    private var x1 = 0f
    private var x2 = 0f
    private var y1 = 0f
    private var y2 = 0f

    fun reset() {
        x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
    }

    fun lowShelf(sampleRate: Int, freq: Float, gainDb: Float, slope: Float = 0.9f) {
        val a = Math.pow(10.0, (gainDb / 40.0)).toFloat()
        val w0 = (2.0 * PI * freq / sampleRate).toFloat()
        val cosW = cos(w0.toDouble()).toFloat()
        val sinW = sin(w0.toDouble()).toFloat()
        val alpha = sinW / 2f * sqrt((a + 1f / a) * (1f / slope - 1f) + 2f)
        val twoSqrtAAlpha = 2f * sqrt(a) * alpha

        val a0 = (a + 1f) + (a - 1f) * cosW + twoSqrtAAlpha
        b0 = a * ((a + 1f) - (a - 1f) * cosW + twoSqrtAAlpha) / a0
        b1 = 2f * a * ((a - 1f) - (a + 1f) * cosW) / a0
        b2 = a * ((a + 1f) - (a - 1f) * cosW - twoSqrtAAlpha) / a0
        a1 = -2f * ((a - 1f) + (a + 1f) * cosW) / a0
        a2 = ((a + 1f) + (a - 1f) * cosW - twoSqrtAAlpha) / a0
    }

    fun highShelf(sampleRate: Int, freq: Float, gainDb: Float, slope: Float = 0.9f) {
        val a = Math.pow(10.0, (gainDb / 40.0)).toFloat()
        val w0 = (2.0 * PI * freq / sampleRate).toFloat()
        val cosW = cos(w0.toDouble()).toFloat()
        val sinW = sin(w0.toDouble()).toFloat()
        val alpha = sinW / 2f * sqrt((a + 1f / a) * (1f / slope - 1f) + 2f)
        val twoSqrtAAlpha = 2f * sqrt(a) * alpha

        val a0 = (a + 1f) - (a - 1f) * cosW + twoSqrtAAlpha
        b0 = a * ((a + 1f) + (a - 1f) * cosW + twoSqrtAAlpha) / a0
        b1 = -2f * a * ((a - 1f) + (a + 1f) * cosW) / a0
        b2 = a * ((a + 1f) + (a - 1f) * cosW - twoSqrtAAlpha) / a0
        a1 = 2f * ((a - 1f) - (a + 1f) * cosW) / a0
        a2 = ((a + 1f) - (a - 1f) * cosW - twoSqrtAAlpha) / a0
    }

    fun highPass(sampleRate: Int, freq: Float, q: Float = 0.707f) {
        val w0 = (2.0 * PI * freq / sampleRate).toFloat()
        val cosW = cos(w0.toDouble()).toFloat()
        val sinW = sin(w0.toDouble()).toFloat()
        val alpha = sinW / (2f * q)

        val a0 = 1f + alpha
        b0 = ((1f + cosW) / 2f) / a0
        b1 = (-(1f + cosW)) / a0
        b2 = ((1f + cosW) / 2f) / a0
        a1 = (-2f * cosW) / a0
        a2 = (1f - alpha) / a0
    }

    fun process(x: Float): Float {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1; x1 = x
        y2 = y1; y1 = y
        return y
    }
}
