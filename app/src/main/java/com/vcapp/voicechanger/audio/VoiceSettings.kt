package com.vcapp.voicechanger.audio

/**
 * Every knob the user can touch. Values are kept plain so they can be
 * serialised straight to JSON as presets.
 */
data class VoiceSettings(
    var presetName: String = "Custom",

    /** Output volume in decibels, -20 dB .. +20 dB. */
    var gainDb: Float = 0f,

    /** Pitch in semitones, -12 .. +12. */
    var pitchSemitones: Float = 0f,

    /** Bass shelf gain in dB at 120 Hz, -12 .. +18. */
    var bassDb: Float = 0f,

    /** Treble shelf gain in dB at 4 kHz, -12 .. +18. */
    var trebleDb: Float = 0f,

    /** Echo wet mix 0..1 plus its time / feedback. */
    var echoMix: Float = 0f,
    var echoDelayMs: Int = 250,
    var echoFeedback: Float = 0.35f,

    /** Reverb wet amount 0..1 and room size 0..1. */
    var reverbAmount: Float = 0f,
    var reverbRoom: Float = 0.7f,

    /** Overdrive / distortion 0..1. */
    var distortion: Float = 0f,

    /** Ring-modulator depth 0..1 and frequency in Hz -> robot voice. */
    var robotDepth: Float = 0f,
    var robotFreq: Float = 60f,

    /** Tremolo depth 0..1 and rate in Hz -> alien / wobble. */
    var tremoloDepth: Float = 0f,
    var tremoloRate: Float = 5f,

    /** Noise gate threshold in dB, -80 = off. */
    var noiseGateDb: Float = -55f,

    /** Roll off everything under this frequency (rumble filter). */
    var highPassHz: Float = 90f
) {
    fun copyFrom(other: VoiceSettings) {
        presetName = other.presetName
        gainDb = other.gainDb
        pitchSemitones = other.pitchSemitones
        bassDb = other.bassDb
        trebleDb = other.trebleDb
        echoMix = other.echoMix
        echoDelayMs = other.echoDelayMs
        echoFeedback = other.echoFeedback
        reverbAmount = other.reverbAmount
        reverbRoom = other.reverbRoom
        distortion = other.distortion
        robotDepth = other.robotDepth
        robotFreq = other.robotFreq
        tremoloDepth = other.tremoloDepth
        tremoloRate = other.tremoloRate
        noiseGateDb = other.noiseGateDb
        highPassHz = other.highPassHz
    }
}

object Presets {

    val all: List<VoiceSettings> = listOf(
        VoiceSettings(presetName = "Clean"),
        VoiceSettings(
            presetName = "Deep Voice",
            pitchSemitones = -5f, bassDb = 8f, trebleDb = -2f, gainDb = 2f
        ),
        VoiceSettings(
            presetName = "Chipmunk",
            pitchSemitones = 7f, trebleDb = 4f, bassDb = -4f
        ),
        VoiceSettings(
            presetName = "Robot",
            robotDepth = 0.8f, robotFreq = 70f, distortion = 0.15f, trebleDb = 3f
        ),
        VoiceSettings(
            presetName = "Demon",
            pitchSemitones = -9f, bassDb = 12f, distortion = 0.35f,
            reverbAmount = 0.35f, reverbRoom = 0.9f, gainDb = 3f
        ),
        VoiceSettings(
            presetName = "Cave Echo",
            echoMix = 0.45f, echoDelayMs = 380, echoFeedback = 0.55f,
            reverbAmount = 0.4f, reverbRoom = 0.85f, bassDb = 4f
        ),
        VoiceSettings(
            presetName = "Bass Boost",
            bassDb = 16f, gainDb = 3f, highPassHz = 50f
        ),
        VoiceSettings(
            presetName = "Alien",
            pitchSemitones = 4f, tremoloDepth = 0.7f, tremoloRate = 7f,
            robotDepth = 0.3f, robotFreq = 110f
        ),
        VoiceSettings(
            presetName = "Megaphone",
            highPassHz = 400f, bassDb = -10f, trebleDb = 8f,
            distortion = 0.5f, gainDb = 4f
        ),
        VoiceSettings(
            presetName = "Radio",
            highPassHz = 300f, trebleDb = 6f, bassDb = -8f, distortion = 0.2f
        ),
        VoiceSettings(
            presetName = "Ghost",
            pitchSemitones = -2f, reverbAmount = 0.75f, reverbRoom = 1f,
            echoMix = 0.3f, echoDelayMs = 600, tremoloDepth = 0.25f
        ),
        VoiceSettings(
            presetName = "Girl",
            pitchSemitones = 4f, trebleDb = 3f, bassDb = -3f
        ),
        VoiceSettings(
            presetName = "Man",
            pitchSemitones = -4f, bassDb = 5f
        ),
        VoiceSettings(
            presetName = "Stadium",
            echoMix = 0.35f, echoDelayMs = 500, echoFeedback = 0.6f,
            reverbAmount = 0.6f, reverbRoom = 1f, gainDb = 2f
        )
    )

    fun byName(name: String): VoiceSettings? = all.firstOrNull { it.presetName == name }
}
