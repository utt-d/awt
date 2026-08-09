package jp.awt.clock.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import jp.awt.clock.R
import jp.awt.clock.ui.AlarmActivity
import kotlin.math.min

class AlarmService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentId = 0L
    private var currentLabel = "アラーム"
    private var currentGradual = true
    private var currentVibrate = true
    private var volume = 1f

    private val volumeRamp = object : Runnable {
        override fun run() {
            volume = min(1f, volume + 0.06f)
            player?.setVolume(volume, volume)
            if (volume < 1f) handler.postDelayed(this, 2_000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopAlarm()
            ACTION_SNOOZE -> {
                AlarmScheduler(this).scheduleSnooze(
                    intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, currentId),
                    intent.getStringExtra(AlarmReceiver.EXTRA_LABEL) ?: currentLabel,
                    intent.getBooleanExtra(AlarmReceiver.EXTRA_GRADUAL, currentGradual),
                    intent.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATE, currentVibrate),
                )
                stopAlarm()
            }
            ACTION_START -> startAlarm(intent)
        }
        return START_NOT_STICKY
    }

    private fun startAlarm(intent: Intent) {
        currentId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, 0L)
        currentLabel = intent.getStringExtra(AlarmReceiver.EXTRA_LABEL).orEmpty().ifBlank { "アラーム" }
        currentGradual = intent.getBooleanExtra(AlarmReceiver.EXTRA_GRADUAL, true)
        currentVibrate = intent.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATE, true)

        AlarmNotifications.createChannel(this)
        startForeground(NOTIFICATION_ID, buildNotification())
        acquireWakeLock()
        startSound()
        if (currentVibrate) startVibration()
    }

    private fun buildNotification(): Notification {
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            currentId.hashCode(),
            Intent(this, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, currentId)
                putExtra(AlarmReceiver.EXTRA_LABEL, currentLabel)
                putExtra(AlarmReceiver.EXTRA_GRADUAL, currentGradual)
                putExtra(AlarmReceiver.EXTRA_VIBRATE, currentVibrate)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = serviceAction(ACTION_STOP, 1)
        val snoozeIntent = serviceAction(ACTION_SNOOZE, 2)

        return Notification.Builder(this, AlarmNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(currentLabel)
            .setContentText("AWT アラーム")
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .addAction(Notification.Action.Builder(null, "10分後", snoozeIntent).build())
            .addAction(Notification.Action.Builder(null, "停止", stopIntent).build())
            .build()
    }

    private fun serviceAction(actionName: String, suffix: Int): PendingIntent = PendingIntent.getService(
        this,
        currentId.hashCode() xor suffix,
        Intent(this, AlarmService::class.java).apply {
            action = actionName
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, currentId)
            putExtra(AlarmReceiver.EXTRA_LABEL, currentLabel)
            putExtra(AlarmReceiver.EXTRA_GRADUAL, currentGradual)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, currentVibrate)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun startSound() {
        player?.release()
        val alarmUri = Settings.System.DEFAULT_ALARM_ALERT_URI
            ?: Settings.System.DEFAULT_NOTIFICATION_URI
        player = createPlayer(alarmUri)?.apply {
            isLooping = true
            volume = if (currentGradual) 0.08f else 1f
            setVolume(volume, volume)
            start()
        }
        if (currentGradual) handler.postDelayed(volumeRamp, 2_000L)
    }

    private fun createPlayer(uri: Uri): MediaPlayer? = try {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            setDataSource(this@AlarmService, uri)
            prepare()
        }
    } catch (_: Exception) {
        null
    }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(android.os.VibratorManager::class.java).defaultVibrator
        } else {
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator?.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 700, 500, 700), 0),
        )
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AWT:AlarmWakeLock",
        ).apply { acquire(30 * 60 * 1000L) }
    }

    private fun stopAlarm() {
        handler.removeCallbacksAndMessages(null)
        player?.runCatching { stop() }
        player?.release()
        player = null
        vibrator?.cancel()
        vibrator = null
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player?.release()
        vibrator?.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "jp.awt.clock.action.START_ALARM"
        const val ACTION_STOP = "jp.awt.clock.action.STOP_ALARM"
        const val ACTION_SNOOZE = "jp.awt.clock.action.SNOOZE_ALARM"
        private const val NOTIFICATION_ID = 8241
    }
}

