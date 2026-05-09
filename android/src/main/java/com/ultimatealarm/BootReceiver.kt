package com.ultimatealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import android.util.Log
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
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
        var advancedCount = 0
        var droppedCount = 0

        for (alarmConfig in allAlarms) {
            try {
                val id = alarmConfig["id"] as? String ?: continue
                val timeMs = (alarmConfig["time"] as? Double)?.toLong() ?: continue
                val title = alarmConfig["title"] as? String ?: "Alarm"
                val message = alarmConfig["message"] as? String ?: ""

                // Transient snooze alarms are runtime-only — drop them whether
                // they're past or future; the user wouldn't expect a snooze to
                // re-arm itself across a reboot.
                if (id.contains("-snooze-")) {
                    storage.deleteAlarm(id)
                    droppedCount++
                    continue
                }

                @Suppress("UNCHECKED_CAST")
                val snoozeConfig = alarmConfig["snooze"] as? Map<String, Any?>
                val snoozeEnabled = snoozeConfig?.get("enabled") as? Boolean ?: false
                val snoozeDuration = (snoozeConfig?.get("duration") as? Double)?.toInt() ?: 300

                @Suppress("UNCHECKED_CAST")
                val data = alarmConfig["data"] as? Map<String, Any?>

                val launchOnDismiss = alarmConfig["launchOnDismiss"] as? Boolean ?: false

                @Suppress("UNCHECKED_CAST")
                val repeatConfig = alarmConfig["repeat"] as? Map<String, Any?>
                val repeatWeekdays = (repeatConfig?.get("weekdays") as? List<*>)
                    ?.mapNotNull { (it as? Double)?.toInt() }
                    ?: emptyList()

                // If the alarm time has already passed, the device must have
                // been off when it was supposed to fire. For repeating alarms,
                // advance to the next matching weekday. For one-shot alarms,
                // drop them — the user's intent to fire at that specific time
                // can't be honored.
                if (timeMs <= now) {
                    if (repeatWeekdays.isNotEmpty()) {
                        val timeOfDayMs = computeTimeOfDay(timeMs)
                        AlarmReceiver.scheduleNextRepeatAlarm(
                            context, id, repeatWeekdays.toIntArray(),
                            timeOfDayMs, title, message,
                            snoozeEnabled, snoozeDuration, launchOnDismiss
                        )
                        advancedCount++
                        Log.d(TAG, "Advanced past repeating alarm id=$id to next occurrence")
                    } else {
                        storage.deleteAlarm(id)
                        droppedCount++
                        Log.d(TAG, "Dropped past one-shot alarm id=$id")
                    }
                    continue
                }

                // Future alarm — re-arm at the stored time.
                val timeOfDayMs = computeTimeOfDay(timeMs)
                val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                    action = AlarmReceiver.ACTION_ALARM
                    putExtra("id", id)
                    putExtra("title", title)
                    putExtra("message", message)
                    putExtra("snoozeEnabled", snoozeEnabled)
                    putExtra("snoozeDuration", snoozeDuration)
                    putExtra("launchOnDismiss", launchOnDismiss)
                    if (repeatWeekdays.isNotEmpty()) {
                        putExtra("repeatWeekdays", repeatWeekdays.toIntArray())
                    }
                    putExtra("timeOfDayMs", timeOfDayMs)
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

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, timeMs, pendingIntent
                    )
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
                }

                rescheduledCount++
                Log.d(TAG, "Rescheduled alarm id=$id at $timeMs")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule alarm: ${e.message}")
            }
        }

        Log.d(
            TAG,
            "Boot reschedule complete: $rescheduledCount rescheduled, " +
                "$advancedCount advanced, $droppedCount dropped"
        )
    }

    private fun computeTimeOfDay(timeMs: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMs }
        val h = cal.get(Calendar.HOUR_OF_DAY).toLong()
        val m = cal.get(Calendar.MINUTE).toLong()
        val s = cal.get(Calendar.SECOND).toLong()
        return ((h * 3600L) + (m * 60L) + s) * 1000L
    }
}
