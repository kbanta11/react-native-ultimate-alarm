package com.ultimatealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                Log.d(TAG, "Rescheduling alarms after: ${intent.action}")
                rescheduleAlarms(context)
            }
        }
    }

    private fun rescheduleAlarms(context: Context) {
        val storage = AlarmStorage(context)
        val allAlarms = storage.getAllAlarmConfigs()

        if (allAlarms.isEmpty()) {
            Log.d(TAG, "No alarms to reschedule")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()

        var rescheduledCount = 0
        var skippedCount = 0

        for (alarmConfig in allAlarms) {
            try {
                val id = alarmConfig["id"] as? String ?: continue
                val timeMs = (alarmConfig["time"] as? Double)?.toLong() ?: continue
                val title = alarmConfig["title"] as? String ?: "Alarm"
                val message = alarmConfig["message"] as? String ?: ""

                // Skip past alarms
                if (timeMs <= now) {
                    Log.d(TAG, "Skipping past alarm id=$id")
                    // Optionally remove past alarms from storage
                    storage.deleteAlarm(id)
                    skippedCount++
                    continue
                }

                // Parse snooze config
                @Suppress("UNCHECKED_CAST")
                val snoozeConfig = alarmConfig["snooze"] as? Map<String, Any?>
                val snoozeEnabled = snoozeConfig?.get("enabled") as? Boolean ?: false
                val snoozeDuration = (snoozeConfig?.get("duration") as? Double)?.toInt() ?: 300

                // Parse custom data
                @Suppress("UNCHECKED_CAST")
                val data = alarmConfig["data"] as? Map<String, Any?>

                // Create alarm intent
                val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                    action = AlarmReceiver.ACTION_ALARM
                    putExtra("id", id)
                    putExtra("title", title)
                    putExtra("message", message)
                    putExtra("snoozeEnabled", snoozeEnabled)
                    putExtra("snoozeDuration", snoozeDuration)
                    if (data != null) {
                        putExtra("data", data.toString())
                    }
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    id.hashCode(),
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Schedule the alarm
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
                }

                rescheduledCount++
                Log.d(TAG, "Rescheduled alarm id=$id")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule alarm: ${e.message}")
            }
        }

        Log.d(TAG, "Rescheduling complete: $rescheduledCount rescheduled, $skippedCount skipped")
    }
}
