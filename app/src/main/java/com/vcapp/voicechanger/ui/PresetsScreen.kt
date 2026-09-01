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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vcapp.voicechanger.audio.Presets
import com.vcapp.voicechanger.service.EngineController

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PresetsScreen(modifier: Modifier = Modifier) {
    val settings by EngineController.settings.collectAsState()
    var userPresets by remember { mutableStateOf(EngineController.repository.loadUserPresets()) }
    var showSave by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    ScrollColumn(modifier) {

        SectionCard("Built-in voices") {
            Text(
                "Tap a voice to load it. Fine tune it later in the Effects tab.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Presets.all.chunked(2).forEach { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { preset ->
                        val selected = settings.presetName == preset.presetName
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            onClick = { EngineController.applyPreset(preset) }
                        ) {
                            Text(
                                preset.presetName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        SectionCard("My presets") {
            if (userPresets.isEmpty()) {
                Text(
                    "No saved presets yet. Tune the effects and save your own voice.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                userPresets.forEach { p ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { EngineController.applyPreset(p.settings) },
                            modifier = Modifier.weight(1f)
                        ) { Text(p.name) }
                        IconButton(onClick = {
                            EngineController.repository.deleteUserPreset(p.name)
                            userPresets = EngineController.repository.loadUserPresets()
                        }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = { showSave = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Save, null)
                Spacer(Modifier.size(8.dp))
                Text("Save current settings")
            }
        }
    }

    if (showSave) {
        AlertDialog(
            onDismissRequest = { showSave = false },
            title = { Text("Save preset") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty()) {
                        EngineController.repository.saveUserPreset(trimmed, settings)
                        EngineController.updateSettings { presetName = trimmed }
                        userPresets = EngineController.repository.loadUserPresets()
                        name = ""
                        showSave = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSave = false }) { Text("Cancel") } }
        )
    }
}
