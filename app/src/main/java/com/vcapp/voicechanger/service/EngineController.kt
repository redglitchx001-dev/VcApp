package com.vcapp.voicechanger.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.vcapp.voicechanger.audio.AudioDecoder
import com.vcapp.voicechanger.audio.OutputRoute
import com.vcapp.voicechanger.audio.VoiceEngine
import com.vcapp.voicechanger.audio.VoiceSettings
import com.vcapp.voicechanger.data.AppRepository
import com.vcapp.voicechanger.data.SoundClip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single source of truth shared by the UI, the foreground service and the
 * floating bubble.
 */
object EngineController {

    private lateinit var appContext: Context
    lateinit var repository: AppRepository
        private set

    private var engine: VoiceEngine? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    /** Decoded PCM cache, keyed by clip id. */
    private val pcmCache = HashMap<String, FloatArray>()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _settings = MutableStateFlow(VoiceSettings())
    val settings: StateFlow<VoiceSettings> = _settings.asStateFlow()

    private val _clips = MutableStateFlow<List<SoundClip>>(emptyList())
    val clips: StateFlow<List<SoundClip>> = _clips.asStateFlow()

    private val _playingClipIds = MutableStateFlow<Set<String>>(emptySet())
    val playingClipIds: StateFlow<Set<String>> = _playingClipIds.asStateFlow()

    private val _route = MutableStateFlow(OutputRoute.SPEAKER)
    val route: StateFlow<OutputRoute> = _route.asStateFlow()

    private val _micEnabled = MutableStateFlow(true)
    val micEnabled: StateFlow<Boolean> = _micEnabled.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _loadingClipId = MutableStateFlow<String?>(null)
    val loadingClipId: StateFlow<String?> = _loadingClipId.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        repository = AppRepository(appContext)
        _settings.value = repository.loadSettings()
        _clips.value = repository.loadClips()
        _route.value = runCatching { OutputRoute.valueOf(repository.routeName) }
            .getOrDefault(OutputRoute.SPEAKER)
    }

    fun consumeMessage() { _message.value = null }
    fun postMessage(text: String) { _message.value = text }

    val outputLevel: Float get() = engine?.processor?.outputLevel ?: 0f
    val inputLevel: Float get() = engine?.processor?.inputLevel ?: 0f

    // ------------------------------------------------------------------
    // Service lifecycle
    // ------------------------------------------------------------------

    fun requestStart(context: Context) {
        val intent = Intent(context, VoiceChangerService::class.java)
            .setAction(VoiceChangerService.ACTION_START)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun requestStop(context: Context) {
        context.startService(
            Intent(context, VoiceChangerService::class.java)
                .setAction(VoiceChangerService.ACTION_STOP)
        )
    }

    /** Called by the service — do not call from the UI. */
    internal fun startEngine(): Boolean {
        if (engine?.isRunning == true) return true
        val e = VoiceEngine(appContext)
        e.onError = { postMessage(it) }
        e.processor.updateSettings(_settings.value)
        e.soundboard.masterVolume = repository.soundboardVolume
        e.soundboard.duckVoiceWhilePlaying = repository.duckVoice
        e.route = _route.value
        e.micEnabled = _micEnabled.value
        engine = e
        val ok = e.start()
        _isRunning.value = ok
        if (!ok) engine = null
        return ok
    }

    internal fun stopEngine() {
        engine?.stop()
        engine = null
        _isRunning.value = false
        _isRecording.value = false
        _playingClipIds.value = emptySet()
    }

    // ------------------------------------------------------------------
    // Settings
    // ------------------------------------------------------------------

    fun updateSettings(block: VoiceSettings.() -> Unit) {
        val next = VoiceSettings().apply { copyFrom(_settings.value); block() }
        _settings.value = next
        engine?.processor?.updateSettings(next)
        repository.saveSettings(next)
    }

    fun applyPreset(preset: VoiceSettings) {
        val next = VoiceSettings().apply { copyFrom(preset) }
        _settings.value = next
        engine?.processor?.updateSettings(next)
        repository.saveSettings(next)
    }

    fun setRoute(route: OutputRoute) {
        _route.value = route
        repository.routeName = route.name
        engine?.route = route
    }

    fun setMicEnabled(enabled: Boolean) {
        _micEnabled.value = enabled
        engine?.micEnabled = enabled
    }

    fun setSoundboardVolume(v: Float) {
        repository.soundboardVolume = v
        engine?.soundboard?.masterVolume = v
    }

    fun setDuck(enabled: Boolean) {
        repository.duckVoice = enabled
        engine?.soundboard?.duckVoiceWhilePlaying = enabled
    }

    // ------------------------------------------------------------------
    // Soundboard
    // ------------------------------------------------------------------

    fun addClip(uri: Uri, name: String) {
        repository.addClip(uri, name)
        _clips.value = repository.loadClips()
    }

    fun removeClip(id: String) {
        pcmCache.remove(id)
        repository.removeClip(id)
        _clips.value = repository.loadClips()
    }

    fun updateClip(clip: SoundClip) {
        repository.updateClip(clip)
        _clips.value = repository.loadClips()
    }

    fun toggleClip(clip: SoundClip) {
        val e = engine
        if (e == null || !e.isRunning) {
            postMessage("Start the voice changer first.")
            return
        }
        if (e.soundboard.isPlaying(clip.id)) {
            e.soundboard.stop(clip.id)
            _playingClipIds.value = _playingClipIds.value - clip.id
            return
        }
        val cached = pcmCache[clip.id]
        if (cached != null) {
            e.soundboard.play(clip.id, cached, clip.loop, clip.volume)
            _playingClipIds.value = _playingClipIds.value + clip.id
            watchClip(clip.id)
            return
        }
        _loadingClipId.value = clip.id
        scope.launch {
            val pcm = withContext(Dispatchers.IO) {
                AudioDecoder.decodeToMonoFloat(
                    appContext, Uri.parse(clip.uri), VoiceEngine.SAMPLE_RATE
                )
            }
            _loadingClipId.value = null
            if (pcm == null) {
                postMessage("Could not read \"${clip.name}\". The file may have been moved.")
                return@launch
            }
            pcmCache[clip.id] = pcm
            engine?.soundboard?.play(clip.id, pcm, clip.loop, clip.volume)
            _playingClipIds.value = _playingClipIds.value + clip.id
            watchClip(clip.id)
        }
    }

    private fun watchClip(id: String) {
        scope.launch {
            while (engine?.soundboard?.isPlaying(id) == true) {
                kotlinx.coroutines.delay(200)
            }
            _playingClipIds.value = _playingClipIds.value - id
        }
    }

    fun stopAllClips() {
        engine?.soundboard?.stopAll()
        _playingClipIds.value = emptySet()
    }

    // ------------------------------------------------------------------
    // Recording
    // ------------------------------------------------------------------

    fun toggleRecording(): File? {
        val e = engine ?: run {
            postMessage("Start the voice changer first.")
            return null
        }
        return if (e.isRecording) {
            e.stopRecording()
            _isRecording.value = false
            lastRecording
        } else {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(appContext.filesDir, "recordings/VcApp_$stamp.wav")
            if (e.startRecording(file)) {
                lastRecording = file
                _isRecording.value = true
            }
            null
        }
    }

    var lastRecording: File? = null
        private set

    fun recordings(): List<File> =
        File(appContext.filesDir, "recordings").listFiles()
            ?.filter { it.extension.equals("wav", true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
}
