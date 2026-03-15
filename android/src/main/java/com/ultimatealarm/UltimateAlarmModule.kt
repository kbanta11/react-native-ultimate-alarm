package com.ultimatealarm

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.content.pm.PackageManager
import java.lang.ref.WeakReference
import org.json.JSONObject

class UltimateAlarmModule : Module() {

    companion object {
        private const val TAG = "UltimateAlarmModule"

        // Weak reference to avoid memory leaks - used by AlarmReceiver to emit events
        private var moduleRef: WeakReference<UltimateAlarmModule>? = null

        fun getModule(): UltimateAlarmModule? = moduleRef?.get()
    }

    private val storage: AlarmStorage by lazy {
        AlarmStorage(appContext.reactContext ?: throw Exception("React context not available"))
    }

    init {
        // Update the reference whenever the module is instantiated
        moduleRef = WeakReference(this)
    }

    override fun definition() = ModuleDefinition {
        // Module name - accessed as NativeModules.UltimateAlarm
        Name("UltimateAlarm")

        // Events that this module can send (must match names used in AlarmReceiver.emitEvent)
        Events("UltimateAlarm.dismiss", "UltimateAlarm.snooze")

        // Handle new intents when the activity is already running
        // (e.g. notification dismiss button with launchOnDismiss=true)
        OnNewIntent { intent ->
            val alarmAction = intent.getStringExtra("alarm_action")
            val alarmId = intent.getStringExtra("alarm_id")

            if (alarmAction != null && alarmId != null) {
                val context = appContext.reactContext
                if (context != null && alarmAction == "dismiss") {
                    val serviceIntent = Intent(context, AlarmService::class.java)
                    context.stopService(serviceIntent)
                    Log.d(TAG, "Stopped AlarmService via onNewIntent dismiss")
                }

                // Emit the event so JS listeners can handle navigation
                try {
                    val eventData = mapOf(
                        "alarmId" to alarmId,
                        "action" to alarmAction,
                        "timestamp" to System.currentTimeMillis().toDouble()
                    )
                    sendEvent("UltimateAlarm.$alarmAction", eventData)
                    Log.d(TAG, "Emitted UltimateAlarm.$alarmAction from onNewIntent")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to emit event from onNewIntent: ${e.message}")
                }
            }
        }

        /**
         * Check if AlarmKit is available (always false on Android)
         */
        AsyncFunction("hasAlarmKit") {
            return@AsyncFunction false
        }

        /**
         * Get platform-specific capabilities
         */
        AsyncFunction("getCapabilities") { implementation: String ->
            val capabilities = mapOf(
                "platform" to "android",
                "implementation" to "alarmmanager",
                "features" to mapOf(
                    "truePersistentAlarm" to true,
                    "autoLaunchApp" to true,
                    "bypassSilentMode" to true,
                    "persistentSound" to true,
                    "customSound" to false,
                    "repeatAlarms" to false
                ),
                "limitations" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    listOf("Requires SCHEDULE_EXACT_ALARM permission on Android 12+")
                } else {
                    emptyList()
                }
            )
            return@AsyncFunction capabilities
        }

        /**
         * Request necessary permissions
         */
        AsyncFunction("requestPermissions") { implementation: String ->
            val context = appContext.reactContext ?: throw Exception("React context not available")

            // Check notification permission (Android 13+)
            val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            // Check exact alarm permission (Android 12+)
            val alarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            val allGranted = notificationGranted && alarmGranted

            if (!allGranted) {
                Log.w(TAG, "Permissions not granted. Notification: $notificationGranted, Exact alarm: $alarmGranted")
            }

            return@AsyncFunction allGranted
        }

        /**
         * Check if permissions are granted
         */
        AsyncFunction("hasPermissions") { implementation: String ->
            val context = appContext.reactContext ?: throw Exception("React context not available")

            val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            val alarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            return@AsyncFunction notificationGranted && alarmGranted
        }

        /**
         * Schedule a new alarm
         */
        AsyncFunction("scheduleAlarm") { implementation: String, config: Map<String, Any?> ->
            val context = appContext.reactContext ?: throw Exception("React context not available")

            val id = config["id"] as? String ?: throw Exception("Alarm ID required")
            val timeMs = (config["time"] as? Double)?.toLong() ?: throw Exception("time required")
            val title = config["title"] as? String ?: "Alarm"
            val message = config["message"] as? String ?: ""

            // Parse snooze config
            @Suppress("UNCHECKED_CAST")
            val snoozeConfig = config["snooze"] as? Map<String, Any?>
            val snoozeEnabled = snoozeConfig?.get("enabled") as? Boolean ?: false
            val snoozeDuration = (snoozeConfig?.get("duration") as? Double)?.toInt() ?: 300

            // Parse custom data
            @Suppress("UNCHECKED_CAST")
            val data = config["data"] as? Map<String, Any?>

            // Parse launch-on-dismiss option
            val launchOnDismiss = config["launchOnDismiss"] as? Boolean ?: false

            val now = System.currentTimeMillis()
            if (timeMs < now) {
                throw Exception("Alarm time must be in the future")
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM
                putExtra("id", id)
                putExtra("title", title)
                putExtra("message", message)
                putExtra("snoozeEnabled", snoozeEnabled)
                putExtra("snoozeDuration", snoozeDuration)
                putExtra("launchOnDismiss", launchOnDismiss)
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

            // Use exact alarm scheduling
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
            }

            storage.saveAlarm(id, config)
            Log.d(TAG, "Scheduled alarm id=$id at=$timeMs")
        }

        /**
         * Dismiss a currently ringing alarm (stops sound and service)
         */
        AsyncFunction("dismissAlarm") { implementation: String, alarmId: String ->
            val context = appContext.reactContext ?: throw Exception("React context not available")

            // Stop the AlarmService (stops sound, vibration, removes notification)
            val serviceIntent = Intent(context, AlarmService::class.java)
            context.stopService(serviceIntent)

            Log.d(TAG, "Dismissed ringing alarm id=$alarmId")
        }

        /**
         * Cancel an alarm by ID
         */
        AsyncFunction("cancelAlarm") { implementation: String, alarmId: String ->
            val context = appContext.reactContext ?: throw Exception("React context not available")

            val intent = Intent(context, AlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                alarmId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pi)
            pi.cancel()

            storage.deleteAlarm(alarmId)
            Log.d(TAG, "Cancelled alarm id=$alarmId")
        }

        /**
         * Cancel all alarms
         */
        AsyncFunction("cancelAllAlarms") { implementation: String ->
            val context = appContext.reactContext ?: throw Exception("React context not available")

            val allAlarms = storage.getAllAlarms()
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            for (alarmId in allAlarms) {
                val intent = Intent(context, AlarmReceiver::class.java)
                val pi = PendingIntent.getBroadcast(
                    context,
                    alarmId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pi)
                pi.cancel()
            }

            storage.clearAllAlarms()
            Log.d(TAG, "Cancelled all alarms (${allAlarms.size})")
        }

        /**
         * Get all scheduled alarms
         */
        AsyncFunction("getAllAlarms") { implementation: String ->
            val configs = storage.getAllAlarmConfigs()
            return@AsyncFunction configs
        }

        /**
         * Check if specific alarm is scheduled
         */
        AsyncFunction("isAlarmScheduled") { implementation: String, alarmId: String ->
            return@AsyncFunction storage.hasAlarm(alarmId)
        }

        /**
         * Snooze an alarm
         */
        AsyncFunction("snoozeAlarm") { implementation: String, alarmId: String, minutes: Int ->
            val context = appContext.reactContext ?: throw Exception("React context not available")

            val alarm = storage.getAlarm(alarmId) ?: throw Exception("Alarm $alarmId not found")

            val title = alarm["title"] as? String ?: "Alarm"
            val message = (alarm["message"] as? String ?: "") + " (Snoozed)"
            val snoozeId = "$alarmId-snooze-${System.currentTimeMillis()}"

            // Retrieve snooze/launch config from the stored alarm
            @Suppress("UNCHECKED_CAST")
            val snoozeConfig = alarm["snooze"] as? Map<String, Any?>
            val snoozeEnabled = snoozeConfig?.get("enabled") as? Boolean ?: true
            val snoozeDuration = (snoozeConfig?.get("duration") as? Double)?.toInt() ?: (minutes * 60)
            val launchOnDismiss = alarm["launchOnDismiss"] as? Boolean ?: false

            val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM
                putExtra("id", snoozeId)
                putExtra("title", title)
                putExtra("message", message)
                putExtra("originalId", alarmId)
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
            val triggerAt = System.currentTimeMillis() + minutes * 60_000L

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }

            Log.d(TAG, "Snoozed alarm $alarmId for $minutes minutes")
        }

        /**
         * Get launch payload if app was launched by alarm
         */
        AsyncFunction("getLaunchPayload") { implementation: String ->
            val context = appContext.reactContext ?: throw Exception("React context not available")

            // First check if the activity was launched with alarm_action extras
            // (e.g. from notification dismiss/snooze buttons that use PendingIntent.getActivity)
            val activity = appContext.currentActivity
            val activityIntent = activity?.intent
            val alarmAction = activityIntent?.getStringExtra("alarm_action")
            val alarmId = activityIntent?.getStringExtra("alarm_id")

            if (alarmAction != null && alarmId != null) {
                // Clear the extras so they don't fire again
                activityIntent.removeExtra("alarm_action")
                activityIntent.removeExtra("alarm_id")

                // If dismiss, stop the alarm service
                if (alarmAction == "dismiss") {
                    val serviceIntent = Intent(context, AlarmService::class.java)
                    context.stopService(serviceIntent)
                    Log.d(TAG, "Stopped AlarmService via activity dismiss intent")
                }

                // Build payload from activity extras
                val payload = mapOf(
                    "alarmId" to alarmId,
                    "action" to alarmAction,
                    "timestamp" to System.currentTimeMillis()
                )
                // Also clear any stored payload to avoid double-handling
                storage.clearLaunchPayload()
                return@AsyncFunction payload
            }

            // Fall back to stored payload (from AlarmReceiver)
            val payload = storage.getLaunchPayload()
            if (payload != null) {
                storage.clearLaunchPayload()
                return@AsyncFunction payload
            } else {
                return@AsyncFunction null
            }
        }
    }
}
