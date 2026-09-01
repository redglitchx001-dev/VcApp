package com.vcapp.voicechanger.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

/**
 * The whole effect chain applied to the microphone signal, in order:
 *
 *   high-pass -> noise gate -> pitch -> bass/treble -> distortion
 *   -> ring mod (robot) -> tremolo -> echo -> reverb -> gain (dB) -> limiter
 */
class VoiceProcessor(private val sampleRate: Int) {

    private val settings = VoiceSettings()

    private val highPass = Biquad()
    private val bassShelf = Biquad()
    private val trebleShelf = Biquad()
    private val pitchShifter = PitchShifter(sampleRate)
    private val echo = EchoUnit(sampleRate)
    private val reverb = ReverbUnit(sampleRate)

    private var pitchBuffer = FloatArray(0)
    private var robotPhase = 0f
    private var tremoloPhase = 0f
    private var envelope = 0f
    private var gateGain = 0f
    private var lastHp = -1f
    private var lastBass = Float.NaN
    private var lastTreble = Float.NaN

    /** Peak level of the last processed block, 0..1 — used by the VU meter. */
    @Volatile var outputLevel: Float = 0f
        private set

    @Volatile var inputLevel: Float = 0f
        private set

    @Synchronized
    fun updateSettings(s: VoiceSettings) {
        settings.copyFrom(s)

        if (settings.highPassHz != lastHp) {
            highPass.highPass(sampleRate, settings.highPassHz.coerceIn(20f, 1000f))
            lastHp = settings.highPassHz
        }
        if (settings.bassDb != lastBass) {
            bassShelf.lowShelf(sampleRate, 120f, settings.bassDb)
            lastBass = settings.bassDb
        }
        if (settings.trebleDb != lastTreble) {
            trebleShelf.highShelf(sampleRate, 4000f, settings.trebleDb)
            lastTreble = settings.trebleDb
        }

        pitchShifter.setRatio(2f.pow(settings.pitchSemitones / 12f))
        echo.delayMs = settings.echoDelayMs
        echo.feedback = settings.echoFeedback
        echo.mix = settings.echoMix
        reverb.amount = settings.reverbAmount
        reverb.roomSize = settings.reverbRoom
    }

    fun reset() {
        highPass.reset(); bassShelf.reset(); trebleShelf.reset()
        pitchShifter.reset(); echo.reset(); reverb.reset()
        robotPhase = 0f; tremoloPhase = 0f; envelope = 0f; gateGain = 0f
    }

    /** Processes [length] samples in place. */
    @Synchronized
    fun process(buffer: FloatArray, length: Int) {
        if (pitchBuffer.size < length) pitchBuffer = FloatArray(length)

        var inPeak = 0f
        val gateThreshold = if (settings.noiseGateDb <= -79f) 0f
        else 10f.pow(settings.noiseGateDb / 20f)

        // --- pre stage: high pass + gate -------------------------------
        for (i in 0 until length) {
            var x = buffer[i]
            val a = abs(x)
            if (a > inPeak) inPeak = a

            x = highPass.process(x)

            envelope += (a - envelope) * 0.002f
            val target = if (gateThreshold <= 0f || envelope > gateThreshold) 1f else 0f
            gateGain += (target - gateGain) * 0.01f
            x *= gateGain

            buffer[i] = x
        }
        inputLevel = inPeak

        // --- pitch ------------------------------------------------------
        pitchShifter.process(buffer, pitchBuffer, length)
        System.arraycopy(pitchBuffer, 0, buffer, 0, length)

        // --- tone, colour, space ---------------------------------------
        val makeUp = 10f.pow(settings.gainDb / 20f)
        val drive = settings.distortion.coerceIn(0f, 1f)
        val driveAmount = 1f + drive * 24f
        val robotDepth = settings.robotDepth.coerceIn(0f, 1f)
        val robotInc = (2.0 * PI * settings.robotFreq / sampleRate).toFloat()
        val tremDepth = settings.tremoloDepth.coerceIn(0f, 1f)
        val tremInc = (2.0 * PI * settings.tremoloRate / sampleRate).toFloat()

        var outPeak = 0f
        for (i in 0 until length) {
            var x = buffer[i]

            x = bassShelf.process(x)
            x = trebleShelf.process(x)

            if (drive > 0.001f) {
                x = tanh(x * driveAmount) * (1f - drive * 0.45f)
            }

            if (robotDepth > 0.001f) {
                val carrier = sin(robotPhase.toDouble()).toFloat()
                robotPhase += robotInc
                if (robotPhase > 2f * PI) robotPhase -= (2f * PI).toFloat()
                x = x * (1f - robotDepth) + (x * carrier) * robotDepth
            }

            if (tremDepth > 0.001f) {
                val lfo = (1f - tremDepth) + tremDepth * (0.5f + 0.5f * sin(tremoloPhase.toDouble()).toFloat())
                tremoloPhase += tremInc
                if (tremoloPhase > 2f * PI) tremoloPhase -= (2f * PI).toFloat()
                x *= lfo
            }

            x = echo.process(x)
            x = reverb.process(x)
            x *= makeUp

            // Soft limiter so nothing clips into the call.
            if (x > 0.95f || x < -0.95f) x = tanh(x)
            buffer[i] = x

            val a = abs(x)
            if (a > outPeak) outPeak = a
        }
        outputLevel = outPeak
    }
}
