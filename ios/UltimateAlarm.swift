import ExpoModulesCore
import Foundation

public class UltimateAlarmModule: Module {
    public func definition() -> ModuleDefinition {
        Name("UltimateAlarm")

        // Events that this module can send to JS
        Events("UltimateAlarm.dismiss", "UltimateAlarm.snooze")

        // MARK: - Check AlarmKit Availability
        AsyncFunction("hasAlarmKit") { () -> Bool in
            if #available(iOS 16.0, *) {
                return true
            }
            return false
        }

        // MARK: - Get Capabilities
        AsyncFunction("getCapabilities") { (implementation: String) -> [String: Any] in
            if implementation == "alarmkit" {
                if #available(iOS 16.0, *) {
                    return try await AlarmKitManager.shared.getCapabilities()
                }
            }

            // Fallback to notification manager
            return NotificationManager.shared.getCapabilities()
        }

        // MARK: - Request Permissions
        AsyncFunction("requestPermissions") { (implementation: String) -> Bool in
            if implementation == "alarmkit" {
                if #available(iOS 16.0, *) {
                    return try await AlarmKitManager.shared.requestPermissions()
                }
            }

            return try await NotificationManager.shared.requestPermissions()
        }

        // MARK: - Check Permissions
        AsyncFunction("hasPermissions") { (implementation: String) -> Bool in
            if implementation == "alarmkit" {
                if #available(iOS 16.0, *) {
                    return await AlarmKitManager.shared.hasPermissions()
                }
            }

            return await NotificationManager.shared.hasPermissions()
        }

        // MARK: - Schedule Alarm
        AsyncFunction("scheduleAlarm") { (implementation: String, config: [String: Any]) async throws in
            if implementation == "alarmkit" {
                if #available(iOS 16.0, *) {
                    try await AlarmKitManager.shared.scheduleAlarm(config: config)
                    return
                }
            }

            try await NotificationManager.shared.scheduleAlarm(config: config)
        }

        // MARK: - Cancel Alarm
        AsyncFunction("cancelAlarm") { (implementation: String, alarmId: String) async throws in
            if implementation == "alarmkit" {
                if #available(iOS 16.0, *) {
                    try await AlarmKitManager.shared.cancelAlarm(alarmId: alarmId)
                    return
                }
            }

            try await NotificationManager.shared.cancelAlarm(alarmId: alarmId)
        }

        // MARK: - Cancel All Alarms
        AsyncFunction("cancelAllAlarms") { (implementation: String) async throws in
            if implementation == "alarmkit" {
                if #available(iOS 16.0, *) {
                    try await AlarmKitManager.shared.cancelAllAlarms()
                    return
                }
            }

            try await NotificationManager.shared.cancelAllAlarms()
        }

        // MARK: - Get All Alarms
        AsyncFunction("getAllAlarms") { (implementation: String) -> [[String: Any]] in
            if implementation == "alarmkit" {
                if #available(iOS 16.0, *) {
                    return try await AlarmKitManager.shared.getAllAlarms()
                }
            }

            return try await NotificationManager.shared.getAllAlarms()
        }

        // MARK: - Is Alarm Scheduled
        AsyncFunction("isAlarmScheduled") { (implementation: String, alarmId: String) -> Bool in
            if implementation == "alarmkit" {
                if #available(iOS 16.0, *) {
                    return try await AlarmKitManager.shared.isAlarmScheduled(alarmId: alarmId)
                }
            }

            return try await NotificationManager.shared.isAlarmScheduled(alarmId: alarmId)
        }

        // MARK: - Snooze Alarm
        AsyncFunction("snoozeAlarm") { (implementation: String, alarmId: String, minutes: Int) async throws in
            if implementation == "alarmkit" {
                if #available(iOS 16.0, *) {
                    try await AlarmKitManager.shared.snoozeAlarm(alarmId: alarmId, minutes: minutes)
                    return
                }
            }

            try await NotificationManager.shared.snoozeAlarm(alarmId: alarmId, minutes: minutes)
        }

        // MARK: - Get Launch Payload
        AsyncFunction("getLaunchPayload") { (implementation: String) -> [String: Any]? in
            if implementation == "alarmkit" {
                if #available(iOS 16.0, *) {
                    return try await AlarmKitManager.shared.getLaunchPayload()
                }
            }

            return try await NotificationManager.shared.getLaunchPayload()
        }
    }
}
