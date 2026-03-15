package com.ultimatealarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_ALARM = "com.ultimatealarm.ACTION_ALARM"
        const val ACTION_DISMISS = "com.ultimatealarm.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.ultimatealarm.ACTION_SNOOZE"

        /**
         * Schedule the next occurrence of a repeating alarm for the next matching weekday.
         * Callable from both AlarmReceiver (broadcast dismiss) and UltimateAlarmModule (activity dismiss).
         */
        fun scheduleNextRepeatAlarm(
            context: Context,
            alarmId: String,
            weekdays: IntArray,
            timeOfDayMs: Long,
            title: String,
            message: String,
            snoozeEnabled: Boolean,
            snoozeDuration: Int,
            launchOnDismiss: Boolean
        ) {
            val calendar = Calendar.getInstance()
            val hours = (timeOfDayMs / 3600000).toInt()
            val minutes = ((timeOfDayMs % 3600000) / 60000).toInt()
            val seconds = ((timeOfDayMs % 60000) / 1000).toInt()

            // Start from tomorrow
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, hours)
            calendar.set(Calendar.MINUTE, minutes)
            calendar.set(Calendar.SECOND, seconds)
            calendar.set(Calendar.MILLISECOND, 0)

            // Find the next matching weekday (Calendar uses 1=Sun..7=Sat, config uses 0=Sun..6=Sat)
            for (i in 0 until 7) {
                val calendarDow = calendar.get(Calendar.DAY_OF_WEEK) // 1-7
                val configDow = calendarDow - 1 // 0-6
                if (configDow in weekdays) {
                    break
                }
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val triggerAt = calendar.timeInMillis

            // Use the ORIGINAL alarm ID (strip any snooze suffix) so cancel still works
            val baseId = if (alarmId.contains("-snooze-")) {
                alarmId.substringBefore("-snooze-")
            } else {
                alarmId
            }

            // Clean message (remove snoozed suffix if present)
            val cleanMessage = message.replace(" (Snoozed)", "")

            val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                this.action = ACTION_ALARM
                putExtra("id", baseId)
                putExtra("title", title)
                putExtra("message", cleanMessage)
                putExtra("snoozeEnabled", snoozeEnabled)
                putExtra("snoozeDuration", snoozeDuration)
                putExtra("launchOnDismiss", launchOnDismiss)
                putExtra("repeatWeekdays", weekdays)
                putExtra("timeOfDayMs", timeOfDayMs)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                baseId.hashCode(),
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }

            // Update storage with next fire time
            val storage = AlarmStorage(context)
            val existingConfig = storage.getAlarm(baseId)
            if (existingConfig != null) {
                val updatedConfig = existingConfig.toMutableMap()
                updatedConfig["time"] = triggerAt.toDouble()
                storage.saveAlarm(baseId, updatedConfig)
            }

            Log.d(TAG, "Repeat alarm $baseId rescheduled for $triggerAt (${calendar.time})")
        }
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

                // Reschedule if this is a repeating alarm
                val repeatWeekdays = intent.getIntArrayExtra("repeatWeekdays")
                val timeOfDayMs = intent.getLongExtra("timeOfDayMs", 0)
                if (id != null && repeatWeekdays != null && repeatWeekdays.isNotEmpty() && timeOfDayMs > 0) {
                    val title = intent.getStringExtra("title") ?: "Alarm"
                    val message = intent.getStringExtra("message") ?: ""
                    val snoozeEnabled = intent.getBooleanExtra("snoozeEnabled", false)
                    val snoozeDuration = intent.getIntExtra("snoozeDuration", 300)
                    val launchOnDismiss = intent.getBooleanExtra("launchOnDismiss", false)
                    scheduleNextRepeatAlarm(context, id, repeatWeekdays, timeOfDayMs, title, message, snoozeEnabled, snoozeDuration, launchOnDismiss)
                }
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

                val snoozeRepeatWeekdays = intent.getIntArrayExtra("repeatWeekdays")
                val snoozeTimeOfDayMs = intent.getLongExtra("timeOfDayMs", 0)

                val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                    this.action = ACTION_ALARM
                    putExtra("id", snoozeId)
                    putExtra("title", title)
                    putExtra("message", "$message (Snoozed)")
                    putExtra("snoozeEnabled", snoozeEnabled)
                    putExtra("snoozeDuration", snoozeDuration)
                    putExtra("launchOnDismiss", launchOnDismiss)
                    putExtra("repeatWeekdays", snoozeRepeatWeekdays)
                    putExtra("timeOfDayMs", snoozeTimeOfDayMs)
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
