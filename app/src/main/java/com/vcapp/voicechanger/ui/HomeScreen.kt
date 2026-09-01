package com.vcapp.voicechanger.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.vcapp.voicechanger.audio.OutputRoute
import com.vcapp.voicechanger.service.BubbleService
import com.vcapp.voicechanger.service.EngineController
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(modifier: Modifier = Modifier, onStartRequested: () -> Unit) {
    val context = LocalContext.current
    val running by EngineController.isRunning.collectAsState()
    val settings by EngineController.settings.collectAsState()
    val route by EngineController.route.collectAsState()
    val micOn by EngineController.micEnabled.collectAsState()
    val recording by EngineController.isRecording.collectAsState()

    var outLevel by remember { mutableFloatStateOf(0f) }
    var inLevel by remember { mutableFloatStateOf(0f) }
    var bubble by remember { mutableStateOf(EngineController.repository.bubbleEnabled) }

    LaunchedEffect(running) {
        while (running) {
            outLevel = EngineController.outputLevel
            inLevel = EngineController.inputLevel
            delay(60)
        }
        outLevel = 0f; inLevel = 0f
    }

    ScrollColumn(modifier) {

        SectionCard("Engine") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(running)
                Spacer(Modifier.size(8.dp))
                Text(
                    if (running) "Running · ${settings.presetName}" else "Stopped",
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    if (running) EngineController.requestStop(context) else onStartRequested()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (running) Color(0xFFB3261E)
                    else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Filled.PowerSettingsNew, null)
                Spacer(Modifier.size(8.dp))
                Text(if (running) "Stop voice changer" else "Start voice changer", fontSize = 16.sp)
            }
            Spacer(Modifier.height(14.dp))
            LevelMeter(inLevel, "Microphone input")
            Spacer(Modifier.height(10.dp))
            LevelMeter(outLevel, "Processed output")
        }

        SectionCard("Output") {
            Text(
                "Choose where the changed voice is played. Speaker is what you use " +
                    "for calls in WhatsApp, Discord or Messenger: turn on the call's " +
                    "loudspeaker and VcApp feeds your new voice through it.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutputRoute.entries.forEach { r ->
                    FilterChip(
                        selected = route == r,
                        onClick = { EngineController.setRoute(r) },
                        label = {
                            Text(
                                when (r) {
                                    OutputRoute.SPEAKER -> "Speaker"
                                    OutputRoute.EARPIECE -> "Earpiece"
                                    OutputRoute.BLUETOOTH -> "Bluetooth"
                                }
                            )
                        }
                    )
                }
            }
        }

        SectionCard("Quick controls") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { EngineController.setMicEnabled(!micOn) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(if (micOn) Icons.Filled.Mic else Icons.Filled.MicOff, null)
                    Spacer(Modifier.size(6.dp))
                    Text(if (micOn) "Mic on" else "Mic off")
                }
                OutlinedButton(
                    onClick = { EngineController.stopAllClips() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Stop, null)
                    Spacer(Modifier.size(6.dp))
                    Text("Stop sounds")
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Floating bubble", fontWeight = FontWeight.Medium)
                    Text(
                        "Control VcApp on top of other apps",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = bubble,
                    onCheckedChange = { checked ->
                        bubble = checked
                        EngineController.repository.bubbleEnabled = checked
                        if (checked && !BubbleService.canDrawOverlay(context)) {
                            context.startActivity(
                                BubbleService.overlayPermissionIntent(context)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                )
            }
        }

        SectionCard("Recorder") {
            Text(
                "Record your processed voice and share the file to WhatsApp, " +
                    "Discord or Messenger as a voice message.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { EngineController.toggleRecording() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (recording) Color(0xFFB3261E)
                    else MaterialTheme.colorScheme.secondary,
                    contentColor = Color(0xFF07131A)
                )
            ) {
                Icon(Icons.Filled.FiberManualRecord, null)
                Spacer(Modifier.size(8.dp))
                Text(if (recording) "Stop recording" else "Record")
            }

            val files = EngineController.recordings()
            if (files.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                files.take(5).forEach { file ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(file.name, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = {
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file
                            )
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "audio/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(share, "Share with"))
                        }) {
                            Icon(Icons.Filled.Share, null)
                            Spacer(Modifier.size(6.dp))
                            Text("Share")
                        }
                    }
                }
            }
        }
    }
}
