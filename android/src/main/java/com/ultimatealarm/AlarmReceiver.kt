package com.ultimatealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter

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

                // Emit snooze event
                emitEvent("UltimateAlarm.snooze", id, "snooze")

                // The actual snooze scheduling is handled by UltimateAlarmModule.snoozeAlarm()
                // which will be called by the React Native layer after receiving this event
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

        val reactContext = UltimateAlarmModule.getReactContext()

        if (reactContext != null && reactContext.hasActiveCatalystInstance()) {
            try {
                val eventData = com.facebook.react.bridge.Arguments.createMap().apply {
                    putString("alarmId", alarmId)
                    putString("action", action)
                    putDouble("timestamp", System.currentTimeMillis().toDouble())
                }

                reactContext
                    .getJSModule(RCTDeviceEventEmitter::class.java)
                    .emit(eventName, eventData)

                Log.d(TAG, "Emitted event $eventName for alarm $alarmId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to emit event: ${e.message}")
            }
        } else {
            Log.w(TAG, "Cannot emit event: ReactContext not available")
        }
    }
}
