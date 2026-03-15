package com.ultimatealarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_ALARM = "com.ultimatealarm.ACTION_ALARM"
        const val ACTION_DISMISS = "com.ultimatealarm.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.ultimatealarm.ACTION_SNOOZE"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        intent ?: return
        val action = intent.action
        val id = intent.getStringExtra("id")

        Log.d(TAG, "onReceive: action=$action id=$id")

        when (action) {
            ACTION_ALARM -> {
                // Alarm triggered - start the AlarmService
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtras(intent.extras ?: return)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // Save launch payload for app launch detection
                saveLaunchPayload(context, intent)
            }

            ACTION_DISMISS -> {
                // Stop the alarm service
                val serviceIntent = Intent(context, AlarmService::class.java)
                context.stopService(serviceIntent)

                // Emit dismiss event
                emitEvent("UltimateAlarm.dismiss", id, "dismiss")

                // Save launch payload with dismiss action
                saveLaunchPayload(context, intent, "dismiss")
            }

            ACTION_SNOOZE -> {
                // Stop the alarm service
                val serviceIntent = Intent(context, AlarmService::class.java)
                context.stopService(serviceIntent)

                // Schedule a new alarm after the snooze duration
                val snoozeDuration = intent.getIntExtra("snoozeDuration", 300)
                val title = intent.getStringExtra("title") ?: "Alarm"
                val message = intent.getStringExtra("message") ?: ""
                val snoozeEnabled = intent.getBooleanExtra("snoozeEnabled", true)
                val launchOnDismiss = intent.getBooleanExtra("launchOnDismiss", false)
                val snoozeId = "$id-snooze-${System.currentTimeMillis()}"

                val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                    this.action = ACTION_ALARM
                    putExtra("id", snoozeId)
                    putExtra("title", title)
                    putExtra("message", "$message (Snoozed)")
                    putExtra("snoozeEnabled", snoozeEnabled)
                    putExtra("snoozeDuration", snoozeDuration)
                    putExtra("launchOnDismiss", launchOnDismiss)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    snoozeId.hashCode(),
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val triggerAt = System.currentTimeMillis() + snoozeDuration * 1000L

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }

                // Save the snoozed alarm to storage so JS snoozeAlarm() can find it later
                val storage = AlarmStorage(context)
                val snoozeConfig = mapOf<String, Any?>(
                    "id" to snoozeId,
                    "title" to title,
                    "message" to "$message (Snoozed)",
                    "time" to triggerAt.toDouble(),
                    "snooze" to mapOf(
                        "enabled" to snoozeEnabled,
                        "duration" to snoozeDuration.toDouble()
                    ),
                    "launchOnDismiss" to launchOnDismiss
                )
                storage.saveAlarm(snoozeId, snoozeConfig)

                Log.d(TAG, "Snoozed alarm $id for ${snoozeDuration}s, will re-trigger as $snoozeId")

                // Emit snooze event to JS (if app is running)
                emitEvent("UltimateAlarm.snooze", id, "snooze")
            }
        }
    }

    private fun saveLaunchPayload(context: Context, intent: Intent, action: String = "trigger") {
        val id = intent.getStringExtra("id") ?: return
        val title = intent.getStringExtra("title") ?: "Alarm"
        val message = intent.getStringExtra("message") ?: ""
        val data = intent.getStringExtra("data")

        val storage = AlarmStorage(context)
        val payload = mutableMapOf<String, Any>(
            "alarmId" to id,
            "action" to action,
            "title" to title,
            "message" to message,
            "timestamp" to System.currentTimeMillis()
        )

        if (data != null) {
            payload["data"] = data
        }

        storage.saveLaunchPayload(payload)
        Log.d(TAG, "Saved launch payload for alarm $id with action $action")
    }

    private fun emitEvent(eventName: String, alarmId: String?, action: String) {
        alarmId ?: return

        val module = UltimateAlarmModule.getModule()

        if (module != null) {
            try {
                val eventData = mapOf(
                    "alarmId" to alarmId,
                    "action" to action,
                    "timestamp" to System.currentTimeMillis().toDouble()
                )

                module.sendEvent(eventName, eventData)

                Log.d(TAG, "Emitted event $eventName for alarm $alarmId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to emit event: ${e.message}")
            }
        } else {
            Log.w(TAG, "Cannot emit event: Module not available")
        }
    }
}
