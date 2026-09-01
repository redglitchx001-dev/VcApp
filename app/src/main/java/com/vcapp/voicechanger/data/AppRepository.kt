package com.vcapp.voicechanger.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vcapp.voicechanger.audio.VoiceSettings
import java.util.UUID

data class SoundClip(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val uri: String,
    val loop: Boolean = false,
    val volume: Float = 1f
)

data class SavedPreset(
    val name: String,
    val settings: VoiceSettings
)

/**
 * Tiny SharedPreferences + Gson store: current knobs, user presets and the
 * MP3 soundboard list.
 */
class AppRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("vcapp", Context.MODE_PRIVATE)
    private val gson = Gson()

    // ---- live settings ------------------------------------------------

    fun loadSettings(): VoiceSettings {
        val json = prefs.getString(KEY_SETTINGS, null) ?: return VoiceSettings()
        return runCatching { gson.fromJson(json, VoiceSettings::class.java) }.getOrNull()
            ?: VoiceSettings()
    }

    fun saveSettings(settings: VoiceSettings) {
        prefs.edit { putString(KEY_SETTINGS, gson.toJson(settings)) }
    }

    // ---- user presets --------------------------------------------------

    fun loadUserPresets(): List<SavedPreset> {
        val json = prefs.getString(KEY_PRESETS, null) ?: return emptyList()
        val type = object : TypeToken<List<SavedPreset>>() {}.type
        return runCatching { gson.fromJson<List<SavedPreset>>(json, type) }.getOrNull() ?: emptyList()
    }

    fun saveUserPreset(name: String, settings: VoiceSettings) {
        val list = loadUserPresets().filter { it.name != name }.toMutableList()
        val copy = VoiceSettings().apply { copyFrom(settings); presetName = name }
        list.add(SavedPreset(name, copy))
        prefs.edit { putString(KEY_PRESETS, gson.toJson(list)) }
    }

    fun deleteUserPreset(name: String) {
        val list = loadUserPresets().filter { it.name != name }
        prefs.edit { putString(KEY_PRESETS, gson.toJson(list)) }
    }

    // ---- soundboard ----------------------------------------------------

    fun loadClips(): List<SoundClip> {
        val json = prefs.getString(KEY_CLIPS, null) ?: return emptyList()
        val type = object : TypeToken<List<SoundClip>>() {}.type
        return runCatching { gson.fromJson<List<SoundClip>>(json, type) }.getOrNull() ?: emptyList()
    }

    fun saveClips(clips: List<SoundClip>) {
        prefs.edit { putString(KEY_CLIPS, gson.toJson(clips)) }
    }

    fun addClip(uri: Uri, name: String): SoundClip {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val clip = SoundClip(name = name, uri = uri.toString())
        saveClips(loadClips() + clip)
        return clip
    }

    fun removeClip(id: String) = saveClips(loadClips().filterNot { it.id == id })

    fun updateClip(clip: SoundClip) =
        saveClips(loadClips().map { if (it.id == clip.id) clip else it })

    // ---- misc flags ----------------------------------------------------

    var duckVoice: Boolean
        get() = prefs.getBoolean(KEY_DUCK, false)
        set(v) = prefs.edit { putBoolean(KEY_DUCK, v) }

    var soundboardVolume: Float
        get() = prefs.getFloat(KEY_SB_VOL, 1f)
        set(v) = prefs.edit { putFloat(KEY_SB_VOL, v) }

    var routeName: String
        get() = prefs.getString(KEY_ROUTE, "SPEAKER") ?: "SPEAKER"
        set(v) = prefs.edit { putString(KEY_ROUTE, v) }

    var bubbleEnabled: Boolean
        get() = prefs.getBoolean(KEY_BUBBLE, true)
        set(v) = prefs.edit { putBoolean(KEY_BUBBLE, v) }

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARD, false)
        set(v) = prefs.edit { putBoolean(KEY_ONBOARD, v) }

    private companion object {
        const val KEY_SETTINGS = "settings"
        const val KEY_PRESETS = "user_presets"
        const val KEY_CLIPS = "clips"
        const val KEY_DUCK = "duck"
        const val KEY_SB_VOL = "sb_volume"
        const val KEY_ROUTE = "route"
        const val KEY_BUBBLE = "bubble"
        const val KEY_ONBOARD = "onboarding"
    }
}
