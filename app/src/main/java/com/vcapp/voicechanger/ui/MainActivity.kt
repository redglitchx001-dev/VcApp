package com.vcapp.voicechanger.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.vcapp.voicechanger.service.EngineController

class MainActivity : ComponentActivity() {

    private var pendingStart = false

    private val requestAudio = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.RECORD_AUDIO] ?: false
        if (granted && pendingStart) {
            EngineController.requestStart(this)
        } else if (!granted) {
            EngineController.postMessage("Microphone permission is required.")
        }
        pendingStart = false
    }

    private val pickAudio = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) EngineController.addClip(uri, displayName(uri))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EngineController.init(applicationContext)

        // Audio shared into VcApp from another app.
        if (intent?.action == Intent.ACTION_SEND) {
            val shared = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
            shared?.let { EngineController.addClip(it, displayName(it)) }
        }

        setContent {
            VcAppTheme {
                AppRoot(
                    onStartRequested = { ensurePermissionsAndStart() },
                    onPickAudio = { pickAudio.launch(arrayOf("audio/*")) }
                )
            }
        }
    }

    private fun ensurePermissionsAndStart() {
        val needed = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            needed += Manifest.permission.RECORD_AUDIO
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            needed += Manifest.permission.POST_NOTIFICATIONS
        }
        if (needed.isEmpty()) {
            EngineController.requestStart(this)
        } else {
            pendingStart = true
            requestAudio.launch(needed.toTypedArray())
        }
    }

    private fun displayName(uri: Uri): String {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "Clip"
        runCatching {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) name = c.getString(idx)
            }
        }
        return name.substringBeforeLast('.').take(28)
    }
}

private enum class Tab(val title: String, val icon: ImageVector) {
    HOME("Live", Icons.Filled.Mic),
    EFFECTS("Effects", Icons.Filled.Tune),
    PRESETS("Presets", Icons.Filled.GraphicEq),
    SOUNDS("Sounds", Icons.Filled.LibraryMusic),
    GUIDE("Guide", Icons.Filled.HelpOutline)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(
    onStartRequested: () -> Unit,
    onPickAudio: () -> Unit
) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    val snackbar = remember { SnackbarHostState() }
    val message by EngineController.message.collectAsState()

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            EngineController.consumeMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("VcApp · ${tab.title}") }) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.title) },
                        label = { Text(t.title) }
                    )
                }
            }
        }
    ) { padding ->
        when (tab) {
            Tab.HOME -> HomeScreen(Modifier.padding(padding), onStartRequested)
            Tab.EFFECTS -> EffectsScreen(Modifier.padding(padding))
            Tab.PRESETS -> PresetsScreen(Modifier.padding(padding))
            Tab.SOUNDS -> SoundboardScreen(Modifier.padding(padding), onPickAudio)
            Tab.GUIDE -> GuideScreen(Modifier.padding(padding))
        }
    }
}
