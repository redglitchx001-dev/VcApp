package com.vcapp.voicechanger.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vcapp.voicechanger.service.EngineController
import kotlin.math.roundToInt

@Composable
private fun Knob(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    onChange: (Float) -> Unit
) {
    LabeledRow(label, display)
    Slider(
        value = value.coerceIn(range.start, range.endInclusive),
        onValueChange = onChange,
        valueRange = range
    )
}

@Composable
fun EffectsScreen(modifier: Modifier = Modifier) {
    val s by EngineController.settings.collectAsState()

    ScrollColumn(modifier) {

        SectionCard("Level") {
            Knob("Volume", s.gainDb, -20f..20f, "${s.gainDb.roundToInt()} dB") { v ->
                EngineController.updateSettings { gainDb = v; presetName = "Custom" }
            }
            Knob(
                "Noise gate", s.noiseGateDb, -80f..-20f,
                if (s.noiseGateDb <= -79f) "Off" else "${s.noiseGateDb.roundToInt()} dB"
            ) { v -> EngineController.updateSettings { noiseGateDb = v } }
        }

        SectionCard("Voice") {
            Knob(
                "Pitch", s.pitchSemitones, -12f..12f,
                "${if (s.pitchSemitones > 0) "+" else ""}${s.pitchSemitones.roundToInt()} st"
            ) { v -> EngineController.updateSettings { pitchSemitones = v; presetName = "Custom" } }
            Text(
                "Negative = deeper / male, positive = higher / chipmunk.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionCard("Equalizer") {
            Knob("Bass (120 Hz)", s.bassDb, -12f..18f, "${s.bassDb.roundToInt()} dB") { v ->
                EngineController.updateSettings { bassDb = v; presetName = "Custom" }
            }
            Knob("Treble (4 kHz)", s.trebleDb, -12f..18f, "${s.trebleDb.roundToInt()} dB") { v ->
                EngineController.updateSettings { trebleDb = v; presetName = "Custom" }
            }
            Knob(
                "Low cut", s.highPassHz, 20f..800f, "${s.highPassHz.roundToInt()} Hz"
            ) { v -> EngineController.updateSettings { highPassHz = v } }
        }

        SectionCard("Echo") {
            Knob("Amount", s.echoMix, 0f..1f, "${(s.echoMix * 100).roundToInt()} %") { v ->
                EngineController.updateSettings { echoMix = v; presetName = "Custom" }
            }
            Knob(
                "Delay", s.echoDelayMs.toFloat(), 20f..1500f, "${s.echoDelayMs} ms"
            ) { v -> EngineController.updateSettings { echoDelayMs = v.roundToInt() } }
            Knob(
                "Feedback", s.echoFeedback, 0f..0.9f, "${(s.echoFeedback * 100).roundToInt()} %"
            ) { v -> EngineController.updateSettings { echoFeedback = v } }
        }

        SectionCard("Reverb") {
            Knob("Amount", s.reverbAmount, 0f..1f, "${(s.reverbAmount * 100).roundToInt()} %") { v ->
                EngineController.updateSettings { reverbAmount = v; presetName = "Custom" }
            }
            Knob("Room size", s.reverbRoom, 0f..1f, "${(s.reverbRoom * 100).roundToInt()} %") { v ->
                EngineController.updateSettings { reverbRoom = v }
            }
        }

        SectionCard("Character") {
            Knob("Distortion", s.distortion, 0f..1f, "${(s.distortion * 100).roundToInt()} %") { v ->
                EngineController.updateSettings { distortion = v; presetName = "Custom" }
            }
            Knob("Robot depth", s.robotDepth, 0f..1f, "${(s.robotDepth * 100).roundToInt()} %") { v ->
                EngineController.updateSettings { robotDepth = v; presetName = "Custom" }
            }
            Knob("Robot frequency", s.robotFreq, 20f..300f, "${s.robotFreq.roundToInt()} Hz") { v ->
                EngineController.updateSettings { robotFreq = v }
            }
            Knob("Tremolo depth", s.tremoloDepth, 0f..1f, "${(s.tremoloDepth * 100).roundToInt()} %") { v ->
                EngineController.updateSettings { tremoloDepth = v; presetName = "Custom" }
            }
            Knob("Tremolo rate", s.tremoloRate, 0.5f..20f, "%.1f Hz".format(s.tremoloRate)) { v ->
                EngineController.updateSettings { tremoloRate = v }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Tip: changes are applied instantly, even during a call.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
