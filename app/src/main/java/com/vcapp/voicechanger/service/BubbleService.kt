package com.vcapp.voicechanger.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.vcapp.voicechanger.R
import com.vcapp.voicechanger.audio.Presets
import kotlin.math.abs

/**
 * Floating bubble that stays on top of WhatsApp / Discord / Messenger so the
 * user can switch presets and fire MP3s without leaving the call.
 */
class BubbleService : Service() {

    companion object {
        fun canDrawOverlay(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

        fun overlayPermissionIntent(context: Context): Intent =
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
    }

    private lateinit var windowManager: WindowManager
    private var root: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var expanded = false
    private var presetIndex = 0

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    override fun onCreate() {
        super.onCreate()
        EngineController.init(applicationContext)

        if (!canDrawOverlay(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view = LayoutInflater.from(this).inflate(R.layout.bubble, null)
        root = view

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 320
        }
        params = lp

        val handle = view.findViewById<ImageView>(R.id.bubbleHandle)
        val panel = view.findViewById<LinearLayout>(R.id.bubblePanel)
        val presetLabel = view.findViewById<TextView>(R.id.presetLabel)
        val muteButton = view.findViewById<Button>(R.id.btnMute)
        val clipRow = view.findViewById<LinearLayout>(R.id.clipRow)

        fun refresh() {
            presetLabel.text = EngineController.settings.value.presetName
            muteButton.text = getString(
                if (EngineController.micEnabled.value) R.string.mute_mic else R.string.unmute_mic
            )
        }

        handle.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchX = 0f
            private var touchY = 0f
            private var dragged = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = lp.x; startY = lp.y
                        touchX = event.rawX; touchY = event.rawY
                        dragged = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchX).toInt()
                        val dy = (event.rawY - touchY).toInt()
                        if (abs(dx) > 12 || abs(dy) > 12) dragged = true
                        lp.x = startX + dx
                        lp.y = startY + dy
                        runCatching { windowManager.updateViewLayout(view, lp) }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!dragged) {
                            expanded = !expanded
                            panel.visibility = if (expanded) View.VISIBLE else View.GONE
                            if (expanded) refresh()
                        }
                    }
                }
                return true
            }
        })

        view.findViewById<Button>(R.id.btnPreset).setOnClickListener {
            presetIndex = (presetIndex + 1) % Presets.all.size
            EngineController.applyPreset(Presets.all[presetIndex])
            refresh()
        }

        muteButton.setOnClickListener {
            EngineController.setMicEnabled(!EngineController.micEnabled.value)
            refresh()
        }

        view.findViewById<Button>(R.id.btnStopSounds).setOnClickListener {
            EngineController.stopAllClips()
        }

        view.findViewById<Button>(R.id.btnClose).setOnClickListener {
            EngineController.requestStop(this)
        }

        // Quick buttons for the first eight soundboard clips.
        val clips = EngineController.clips.value.take(8)
        for (clip in clips) {
            val b = Button(this).apply {
                text = clip.name.take(14)
                textSize = 12f
                setOnClickListener { EngineController.toggleClip(clip) }
            }
            clipRow.addView(b)
        }
        view.findViewById<HorizontalScrollView>(R.id.clipScroll).visibility =
            if (clips.isEmpty()) View.GONE else View.VISIBLE

        panel.visibility = View.GONE
        refresh()

        runCatching { windowManager.addView(view, lp) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        root?.let { v -> runCatching { windowManager.removeView(v) } }
        root = null
        super.onDestroy()
    }
}
