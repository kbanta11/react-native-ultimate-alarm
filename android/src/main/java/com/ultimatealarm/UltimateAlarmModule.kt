package com.ultimatealarm

import com.facebook.react.bridge.*
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.content.pm.PackageManager
import java.lang.ref.WeakReference

class UltimateAlarmModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        private const val TAG = "UltimateAlarmModule"

        // Weak reference to avoid memory leaks - used by AlarmReceiver to emit events
        private var reactContextRef: WeakReference<ReactApplicationContext>? = null

        fun getReactContext(): ReactApplicationContext? = reactContextRef?.get()
    }

    private val storage: AlarmStorage = AlarmStorage(reactContext)

    init {
        // Update the reference whenever the module is instantiated
        reactContextRef = WeakReference(reactContext)
    }

    override fun getName(): String = "UltimateAlarm"

    /**
     * Check if AlarmKit is available (always false on Android)
     */
    @ReactMethod
    fun hasAlarmKit(promise: Promise) {
        promise.resolve(false)
    }

    /**
     * Get platform-specific capabilities
     */
    @ReactMethod
    fun getCapabilities(implementation: String, promise: Promise) {
        val capabilities = Arguments.createMap()
        capabilities.putString("platform", "android")
        capabilities.putString("implementation", "alarmmanager")

        val features = Arguments.createMap()
        features.putBoolean("truePersistentAlarm", true)
        features.putBoolean("autoLaunchApp", true)
        features.putBoolean("bypassSilentMode", true)
        features.putBoolean("persistentSound", true)
        features.putBoolean("customSound", false) // Not yet implemented
        features.putBoolean("repeatAlarms", false) // Not yet implemented
        capabilities.putMap("features", features)

        val limitations = Arguments.createArray()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            limitations.pushString("Requires SCHEDULE_EXACT_ALARM permission on Android 12+")
        }
        capabilities.putArray("limitations", limitations)

        promise.resolve(capabilities)
    }

    /**
     * Request necessary permissions
     */
    @ReactMethod
    fun requestPermissions(implementation: String, promise: Promise) {
        try {
            // Check notification permission (Android 13+)
            val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                reactApplicationContext.checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            // Check exact alarm permission (Android 12+)
            val alarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = reactApplicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            val allGranted = notificationGranted && alarmGranted

            if (!allGranted) {
                Log.w(TAG, "Permissions not granted. Notification: $notificationGranted, Exact alarm: $alarmGranted")
            }

            promise.resolve(allGranted)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check permissions: ${e.message}")
            promise.reject("PERMISSION_ERROR", e)
        }
    }

    /**
     * Check if permissions are granted
     */
    @ReactMethod
    fun hasPermissions(implementation: String, promise: Promise) {
        requestPermissions(implementation, promise)
    }

    /**
     * Schedule a new alarm
     */
    @ReactMethod
    fun scheduleAlarm(implementation: String, config: ReadableMap, promise: Promise) {
        try {
            val id = config.getString("id") ?: throw Exception("Alarm ID required")
            val timeMs = if (config.hasKey("time")) config.getDouble("time").toLong() else throw Exception("time required")
            val title = config.getString("title") ?: "Alarm"
            val message = config.getString("message") ?: ""

            // Parse snooze config
            val snoozeConfig = if (config.hasKey("snooze")) config.getMap("snooze") else null
            val snoozeEnabled = snoozeConfig?.getBoolean("enabled") ?: false
            val snoozeDuration = snoozeConfig?.getInt("duration") ?: 300 // 5 minutes default

            // Parse custom data
            val data = if (config.hasKey("data")) config.getMap("data") else null

            val now = System.currentTimeMillis()
            if (timeMs < now) {
                promise.reject("INVALID_TIME", "Alarm time must be in the future")
                return
            }

            val alarmManager = reactApplicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(reactApplicationContext, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM
                putExtra("id", id)
                putExtra("title", title)
                putExtra("message", message)
                putExtra("snoozeEnabled", snoozeEnabled)
                putExtra("snoozeDuration", snoozeDuration)
                if (data != null) {
                    putExtra("data", data.toHashMap().toString())
                }
            }

            val pendingIntent = PendingIntent.getBroadcast(
                reactApplicationContext,
                id.hashCode(),
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Use exact alarm scheduling
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
            }

            storage.saveAlarm(id, config)
            Log.d(TAG, "Scheduled alarm id=$id at=$timeMs")
            promise.resolve(null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm: ${e.message}")
            promise.reject("SCHEDULE_ERROR", e)
        }
    }

    /**
     * Cancel an alarm by ID
     */
    @ReactMethod
    fun cancelAlarm(implementation: String, alarmId: String, promise: Promise) {
        try {
            val intent = Intent(reactApplicationContext, AlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                reactApplicationContext,
                alarmId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = reactApplicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pi)
            pi.cancel()

            storage.deleteAlarm(alarmId)
            Log.d(TAG, "Cancelled alarm id=$alarmId")
            promise.resolve(null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel alarm: ${e.message}")
            promise.reject("CANCEL_ERROR", e)
        }
    }

    /**
     * Cancel all alarms
     */
    @ReactMethod
    fun cancelAllAlarms(implementation: String, promise: Promise) {
        try {
            val allAlarms = storage.getAllAlarms()
            val alarmManager = reactApplicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            for (alarmId in allAlarms) {
                val intent = Intent(reactApplicationContext, AlarmReceiver::class.java)
                val pi = PendingIntent.getBroadcast(
                    reactApplicationContext,
                    alarmId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pi)
                pi.cancel()
            }

            storage.clearAllAlarms()
            Log.d(TAG, "Cancelled all alarms (${allAlarms.size})")
            promise.resolve(null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel all alarms: ${e.message}")
            promise.reject("CANCEL_ALL_ERROR", e)
        }
    }

    /**
     * Get all scheduled alarms
     */
    @ReactMethod
    fun getAllAlarms(implementation: String, promise: Promise) {
        try {
            val configs = storage.getAllAlarmConfigs()
            val arr = Arguments.createArray()

            for (config in configs) {
                arr.pushMap(config)
            }

            promise.resolve(arr)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all alarms: ${e.message}")
            promise.reject("GET_ALARMS_ERROR", e)
        }
    }

    /**
     * Check if specific alarm is scheduled
     */
    @ReactMethod
    fun isAlarmScheduled(implementation: String, alarmId: String, promise: Promise) {
        try {
            val exists = storage.hasAlarm(alarmId)
            promise.resolve(exists)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check alarm: ${e.message}")
            promise.reject("CHECK_ALARM_ERROR", e)
        }
    }

    /**
     * Snooze an alarm
     */
    @ReactMethod
    fun snoozeAlarm(implementation: String, alarmId: String, minutes: Int, promise: Promise) {
        try {
            val alarm = storage.getAlarm(alarmId)
            if (alarm == null) {
                promise.reject("ALARM_NOT_FOUND", "Alarm $alarmId not found")
                return
            }

            val title = alarm.getString("title")
            val message = alarm.getString("message") + " (Snoozed)"
            val snoozeId = "$alarmId-snooze-${System.currentTimeMillis()}"

            val alarmIntent = Intent(reactApplicationContext, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM
                putExtra("id", snoozeId)
                putExtra("title", title)
                putExtra("message", message)
                putExtra("originalId", alarmId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                reactApplicationContext,
                snoozeId.hashCode(),
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = reactApplicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerAt = System.currentTimeMillis() + minutes * 60_000L

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }

            Log.d(TAG, "Snoozed alarm $alarmId for $minutes minutes")
            promise.resolve(null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to snooze alarm: ${e.message}")
            promise.reject("SNOOZE_ERROR", e)
        }
    }

    /**
     * Get launch payload if app was launched by alarm
     */
    @ReactMethod
    fun getLaunchPayload(implementation: String, promise: Promise) {
        try {
            val payload = storage.getLaunchPayload()
            if (payload != null) {
                storage.clearLaunchPayload()
                promise.resolve(payload)
            } else {
                promise.resolve(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get launch payload: ${e.message}")
            promise.reject("GET_LAUNCH_PAYLOAD_ERROR", e)
        }
    }

    @ReactMethod
    fun addListener(eventName: String) {
        // Required for NativeEventEmitter
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        // Required for NativeEventEmitter
    }
}
