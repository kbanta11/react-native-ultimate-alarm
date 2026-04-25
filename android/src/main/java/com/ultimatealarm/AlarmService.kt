package com.ultimatealarm

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.*
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    companion object {
        private const val TAG = "AlarmService"
        private const val CHANNEL_ID = "ultimate_alarm_channel"
        private const val NOTIFICATION_ID = 1001
        private const val RAW_RES = "alarm_default"
        private const val AUTO_STOP_SECONDS = 60
    }

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var originalVolume: Int = 0
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var currentAlarmId: String? = null
    private var currentTitle: String? = null
    private var currentMessage: String? = null
    private var currentSnoozeEnabled: Boolean = false
    private var currentSnoozeDuration: Int = 300
    private var currentLaunchOnDismiss: Boolean = false
    private var currentRepeatWeekdays: IntArray? = null
    private var currentTimeOfDayMs: Long = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AlarmService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_NOT_STICKY

        val id = intent.getStringExtra("id") ?: return START_NOT_STICKY
        val title = intent.getStringExtra("title") ?: "Alarm"
        val message = intent.getStringExtra("message") ?: ""
        val snoozeEnabled = intent.getBooleanExtra("snoozeEnabled", false)
        val snoozeDuration = intent.getIntExtra("snoozeDuration", 300)
        val launchOnDismiss = intent.getBooleanExtra("launchOnDismiss", false)

        currentAlarmId = id
        currentTitle = title
        currentMessage = message
        currentSnoozeEnabled = snoozeEnabled
        currentSnoozeDuration = snoozeDuration
        currentLaunchOnDismiss = launchOnDismiss
        currentRepeatWeekdays = intent.getIntArrayExtra("repeatWeekdays")
        currentTimeOfDayMs = intent.getLongExtra("timeOfDayMs", 0)

        // Start as foreground service
        val notification = buildNotification(id, title, message, snoozeEnabled, snoozeDuration, launchOnDismiss)
        startForeground(NOTIFICATION_ID, notification)

        // Acquire wake lock
        acquireWakeLock()

        // Setup audio
        setupAudio()

        // Play alarm sound
        playAlarm(id)

        // Auto-stop after configured time
        scheduleAutoStop(id)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AlarmService destroyed")
        stopAlarm()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm notifications"
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        id: String,
        title: String,
        message: String,
        snoozeEnabled: Boolean,
        snoozeDuration: Int,
        launchOnDismiss: Boolean = false
    ): Notification {
        // Dismiss action
        val dismissPendingIntent = if (launchOnDismiss) {
            // Launch the app with dismiss extras so the app can handle navigation
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("alarm_action", "dismiss")
                putExtra("alarm_id", id)
                putExtra("repeatWeekdays", currentRepeatWeekdays)
                putExtra("timeOfDayMs", currentTimeOfDayMs)
                putExtra("title", currentTitle)
                putExtra("message", currentMessage)
                putExtra("snoozeEnabled", currentSnoozeEnabled)
                putExtra("snoozeDuration", currentSnoozeDuration)
                putExtra("launchOnDismiss", currentLaunchOnDismiss)
            }
            if (launchIntent != null) {
                PendingIntent.getActivity(
                    this,
                    id.hashCode() + 1,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            } else {
                // Fallback to broadcast if launch intent unavailable
                buildDismissBroadcast(id)
            }
        } else {
            // Default: just stop the alarm via broadcast, don't open the app
            buildDismissBroadcast(id)
        }

        // Snooze action (if enabled)
        val snoozePendingIntent = if (snoozeEnabled) {
            val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_SNOOZE
                putExtra("id", id)
                putExtra("title", title)
                putExtra("message", message)
                putExtra("snoozeDuration", snoozeDuration)
                putExtra("snoozeEnabled", snoozeEnabled)
                putExtra("launchOnDismiss", launchOnDismiss)
                putExtra("repeatWeekdays", currentRepeatWeekdays)
                putExtra("timeOfDayMs", currentTimeOfDayMs)
            }
            PendingIntent.getBroadcast(
                this,
                id.hashCode() + 2,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        // Full-screen intent to launch app (also used as contentIntent for notification body tap).
        // CLEAR_TOP + SINGLE_TOP: if the activity exists, bring it to front and deliver
        // the intent via onNewIntent (no destroy/recreate). If it doesn't exist, create
        // it normally. Without SINGLE_TOP, CLEAR_TOP destroys and recreates the activity,
        // causing multiple React mounts that consume/lose the alarm payload.
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("alarm_id", id)
            putExtra("alarm_action", "trigger")
        }

        val fullScreenPendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSound(null)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                dismissPendingIntent
            )

        // Add snooze action if enabled
        if (snoozeEnabled && snoozePendingIntent != null) {
            builder.addAction(
                android.R.drawable.ic_menu_recent_history,
                "Snooze",
                snoozePendingIntent
            )
        }

        // Full-screen intent for lock screen display
        if (fullScreenPendingIntent != null) {
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
            builder.setContentIntent(fullScreenPendingIntent)
        }

        return builder.build()
    }

    private fun buildDismissBroadcast(id: String): PendingIntent {
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS
            putExtra("id", id)
            putExtra("title", currentTitle)
            putExtra("message", currentMessage)
            putExtra("snoozeEnabled", currentSnoozeEnabled)
            putExtra("snoozeDuration", currentSnoozeDuration)
            putExtra("launchOnDismiss", currentLaunchOnDismiss)
            putExtra("repeatWeekdays", currentRepeatWeekdays)
            putExtra("timeOfDayMs", currentTimeOfDayMs)
        }
        return PendingIntent.getBroadcast(
            this,
            id.hashCode() + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ultimatealarm:AlarmWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire((AUTO_STOP_SECONDS + 5) * 1000L)
        }

        Log.d(TAG, "WakeLock acquired")
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock: ${e.message}")
        }
        wakeLock = null
    }

    private fun setupAudio() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        audioManager?.let { am ->
            // Respect the system's current alarm volume — don't override it.
            originalVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)

            // Request audio focus
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attributes)
                    .setOnAudioFocusChangeListener { }
                    .build()
                am.requestAudioFocus(audioFocusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }

            Log.d(TAG, "Audio setup complete (max volume, audio focus)")
        }
    }

    private fun playAlarm(id: String) {
        // Release any existing player
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping previous player: ${e.message}")
            }
            it.release()
        }

        mediaPlayer = MediaPlayer().apply {
            try {
                // Try custom sound from host app's res/raw folder
                val resId = resources.getIdentifier(RAW_RES, "raw", packageName)
                if (resId != 0) {
                    setDataSource(
                        this@AlarmService,
                        android.net.Uri.parse("android.resource://$packageName/raw/$RAW_RES")
                    )
                } else {
                    // Fallback to default alarm sound
                    val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setDataSource(this@AlarmService, alarmUri)
                }

                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )

                @Suppress("DEPRECATION")
                setAudioStreamType(AudioManager.STREAM_ALARM)
                isLooping = true
                prepare()
                start()

                Log.d(TAG, "Alarm sound started for id=$id")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play alarm sound: ${e.message}")
            }
        }
    }

    private fun scheduleAutoStop(id: String) {
        Thread {
            try {
                Thread.sleep(AUTO_STOP_SECONDS * 1000L)
                if (currentAlarmId == id) {
                    Log.d(TAG, "Auto-snoozing alarm id=$id after ${AUTO_STOP_SECONDS}s")
                    val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
                        action = AlarmReceiver.ACTION_SNOOZE
                        putExtra("id", id)
                        putExtra("title", currentTitle)
                        putExtra("message", currentMessage)
                        putExtra("snoozeEnabled", currentSnoozeEnabled)
                        putExtra("snoozeDuration", currentSnoozeDuration)
                        putExtra("launchOnDismiss", currentLaunchOnDismiss)
                        putExtra("repeatWeekdays", currentRepeatWeekdays)
                        putExtra("timeOfDayMs", currentTimeOfDayMs)
                    }
                    sendBroadcast(snoozeIntent)
                }
            } catch (e: InterruptedException) {
                Log.d(TAG, "Auto-stop thread interrupted")
            }
        }.start()
    }

    private fun stopAlarm() {
        Log.d(TAG, "Stopping alarm")

        // Stop media player
        mediaPlayer?.apply {
            try {
                if (isPlaying) stop()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping media player: ${e.message}")
            }
            release()
        }
        mediaPlayer = null

        // Restore volume and abandon audio focus
        audioManager?.let { am ->
            try {
                // Volume not modified — nothing to restore
            } catch (_: Exception) {}

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        }

        // Release wake lock
        releaseWakeLock()

        currentAlarmId = null
        currentTitle = null
        currentMessage = null
        currentSnoozeEnabled = false
        currentSnoozeDuration = 300
        currentLaunchOnDismiss = false
        currentRepeatWeekdays = null
        currentTimeOfDayMs = 0
    }
}
