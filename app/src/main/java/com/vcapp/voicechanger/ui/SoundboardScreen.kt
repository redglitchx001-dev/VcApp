package com.vcapp.voicechanger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vcapp.voicechanger.service.EngineController
import kotlin.math.roundToInt

@Composable
fun SoundboardScreen(modifier: Modifier = Modifier, onPickAudio: () -> Unit) {
    val clips by EngineController.clips.collectAsState()
    val playing by EngineController.playingClipIds.collectAsState()
    val loading by EngineController.loadingClipId.collectAsState()

    var volume by remember { mutableFloatStateOf(EngineController.repository.soundboardVolume) }
    var duck by remember { mutableStateOf(EngineController.repository.duckVoice) }

    ScrollColumn(modifier) {

        SectionCard("Soundboard") {
            Text(
                "Add MP3, M4A, WAV or OGG files. When the engine is running the clips " +
                    "are mixed into the same output as your voice, so the other side of " +
                    "the call hears them too.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onPickAudio, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.size(8.dp))
                Text("Add audio file")
            }
            Spacer(Modifier.height(14.dp))
            LabeledRow("Soundboard volume", "${(volume * 100).roundToInt()} %")
            Slider(
                value = volume,
                onValueChange = { volume = it; EngineController.setSoundboardVolume(it) },
                valueRange = 0f..2f
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Duck my voice while a clip plays", fontWeight = FontWeight.Medium)
                    Text(
                        "Lowers the microphone so the music stays clean",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = duck, onCheckedChange = {
                    duck = it
                    EngineController.setDuck(it)
                })
            }
        }

        if (clips.isEmpty()) {
            SectionCard("No clips yet") {
                Text(
                    "Tap \"Add audio file\" above, or share an MP3 from any app to VcApp.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            clips.forEach { clip ->
                val isPlaying = clip.id in playing
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPlaying) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                clip.name,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            if (loading == clip.id) {
                                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(onClick = { EngineController.toggleClip(clip) }) {
                                    Icon(
                                        if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                        contentDescription = if (isPlaying) "Stop" else "Play"
                                    )
                                }
                            }
                            IconButton(onClick = {
                                EngineController.updateClip(clip.copy(loop = !clip.loop))
                            }) {
                                Icon(
                                    Icons.Filled.Repeat,
                                    contentDescription = "Loop",
                                    tint = if (clip.loop) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { EngineController.removeClip(clip.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove")
                            }
                        }
                        LabeledRow("Clip volume", "${(clip.volume * 100).roundToInt()} %")
                        Slider(
                            value = clip.volume,
                            onValueChange = { EngineController.updateClip(clip.copy(volume = it)) },
                            valueRange = 0f..2f
                        )
                    }
                }
            }
        }
    }
}
