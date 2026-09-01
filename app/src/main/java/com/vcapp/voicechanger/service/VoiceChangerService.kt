package com.vcapp.voicechanger.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vcapp.voicechanger.R
import com.vcapp.voicechanger.ui.MainActivity

/**
 * Keeps the audio engine alive while the user is inside WhatsApp, Discord,
 * Messenger or any other call app.
 */
class VoiceChangerService : Service() {

    companion object {
        const val ACTION_START = "com.vcapp.voicechanger.START"
        const val ACTION_STOP = "com.vcapp.voicechanger.STOP"
        const val ACTION_TOGGLE_MIC = "com.vcapp.voicechanger.TOGGLE_MIC"
        const val ACTION_PANIC = "com.vcapp.voicechanger.PANIC"

        private const val CHANNEL_ID = "vcapp_engine"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        EngineController.init(applicationContext)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                EngineController.stopEngine()
                stopBubble()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_MIC -> {
                EngineController.setMicEnabled(!EngineController.micEnabled.value)
                notifyState()
                return START_STICKY
            }
            ACTION_PANIC -> {
                EngineController.stopAllClips()
                EngineController.applyPreset(com.vcapp.voicechanger.audio.Presets.all.first())
                notifyState()
                return START_STICKY
            }
            else -> {
                startForegroundCompat()
                val ok = EngineController.startEngine()
                if (!ok) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (EngineController.repository.bubbleEnabled) startBubble()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        EngineController.stopEngine()
        stopBubble()
        super.onDestroy()
    }

    private fun startBubble() {
        if (!BubbleService.canDrawOverlay(this)) return
        startService(Intent(this, BubbleService::class.java))
    }

    private fun stopBubble() {
        stopService(Intent(this, BubbleService::class.java))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_engine),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_engine_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun notifyState() {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val micOn = EngineController.micEnabled.value
        val preset = EngineController.settings.value.presetName

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mic)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text, preset))
            .setOngoing(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                0,
                getString(if (micOn) R.string.mute_mic else R.string.unmute_mic),
                service(ACTION_TOGGLE_MIC, 1)
            )
            .addAction(0, getString(R.string.reset), service(ACTION_PANIC, 2))
            .addAction(0, getString(R.string.stop), service(ACTION_STOP, 3))
            .build()
    }

    private fun service(action: String, code: Int): PendingIntent =
        PendingIntent.getService(
            this, code,
            Intent(this, VoiceChangerService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
}
