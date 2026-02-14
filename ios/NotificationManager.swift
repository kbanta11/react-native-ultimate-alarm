import Foundation
import UserNotifications

class NotificationManager: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationManager()
    private let storage = AlarmStorage()
    private let notificationCenter = UNUserNotificationCenter.current()

    private override init() {
        super.init()
        notificationCenter.delegate = self
    }

    func getCapabilities() -> [String: Any] {
        return [
            "platform": "ios",
            "implementation": "notification",
            "features": [
                "truePersistentAlarm": false,
                "autoLaunchApp": false,
                "bypassSilentMode": false,
                "persistentSound": false,
                "customSound": true,
                "repeatAlarms": true
            ],
            "limitations": [
                "Notification-based alarm (not true alarm)",
                "Sound limited to 30 seconds",
                "May not play if device is in Do Not Disturb mode",
                "Requires user interaction to launch app"
            ]
        ]
    }

    func requestPermissions() async throws -> Bool {
        let settings = await notificationCenter.notificationSettings()

        if settings.authorizationStatus == .notDetermined {
            let granted = try await notificationCenter.requestAuthorization(options: [.alert, .sound, .badge])
            return granted
        }

        return settings.authorizationStatus == .authorized
    }

    func hasPermissions() async -> Bool {
        let settings = await notificationCenter.notificationSettings()
        return settings.authorizationStatus == .authorized
    }

    func scheduleAlarm(config: [String: Any]) async throws {
        guard let id = config["id"] as? String,
              let timeMs = config["time"] as? Double else {
            throw NSError(domain: "NotificationManager", code: 1, userInfo: [NSLocalizedDescriptionKey: "Missing id or time"])
        }

        let title = config["title"] as? String ?? "Alarm"
        let message = config["message"] as? String ?? ""

        // Parse snooze config
        let snoozeConfig = config["snooze"] as? [String: Any]
        let snoozeEnabled = snoozeConfig?["enabled"] as? Bool ?? false

        // Parse repeat config
        let repeatConfig = config["repeat"] as? [String: Any]
        let weekdays = repeatConfig?["weekdays"] as? [Int]

        // Create notification content
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = message
        content.sound = .defaultCritical
        content.categoryIdentifier = snoozeEnabled ? "ALARM_WITH_SNOOZE" : "ALARM"
        content.userInfo = ["alarmId": id]

        // Create trigger
        let trigger: UNNotificationTrigger
        let date = Date(timeIntervalSince1970: timeMs / 1000)

        if let weekdays = weekdays, !weekdays.isEmpty {
            // Repeating alarm - schedule for each weekday
            let calendar = Calendar.current
            let hour = calendar.component(.hour, from: date)
            let minute = calendar.component(.minute, from: date)

            for weekday in weekdays {
                var dateComponents = DateComponents()
                dateComponents.hour = hour
                dateComponents.minute = minute
                // iOS weekday: 1 = Sunday, 2 = Monday, etc.
                dateComponents.weekday = weekday + 1

                let weekdayTrigger = UNCalendarNotificationTrigger(
                    dateMatching: dateComponents,
                    repeats: true
                )

                let weekdayId = "\(id)-\(weekday)"
                let request = UNNotificationRequest(
                    identifier: weekdayId,
                    content: content,
                    trigger: weekdayTrigger
                )

                try await notificationCenter.add(request)
            }

            // Store metadata
            storage.saveAlarm(alarmId: id, config: config)
            return
        } else {
            // One-time alarm
            trigger = UNCalendarNotificationTrigger(
                dateMatching: Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: date),
                repeats: false
            )
        }

        // Create request
        let request = UNNotificationRequest(
            identifier: id,
            content: content,
            trigger: trigger
        )

        // Schedule notification
        try await notificationCenter.add(request)

        // Store metadata
        storage.saveAlarm(alarmId: id, config: config)

        // Register notification categories if snooze is enabled
        if snoozeEnabled {
            setupNotificationCategories()
        }
    }

    func cancelAlarm(alarmId: String) async throws {
        // Cancel the main notification
        notificationCenter.removePendingNotificationRequests(withIdentifiers: [alarmId])

        // Cancel any weekday-specific notifications
        for weekday in 0...6 {
            notificationCenter.removePendingNotificationRequests(withIdentifiers: ["\(alarmId)-\(weekday)"])
        }

        storage.deleteAlarm(alarmId: alarmId)
    }

    func cancelAllAlarms() async throws {
        let allAlarms = storage.getAllAlarms()

        for alarmId in allAlarms {
            notificationCenter.removePendingNotificationRequests(withIdentifiers: [alarmId])

            // Cancel any weekday-specific notifications
            for weekday in 0...6 {
                notificationCenter.removePendingNotificationRequests(withIdentifiers: ["\(alarmId)-\(weekday)"])
            }
        }

        storage.clearAllAlarms()
    }

    func getAllAlarms() async throws -> [[String: Any]] {
        return storage.getAllAlarmConfigs()
    }

    func isAlarmScheduled(alarmId: String) async throws -> Bool {
        return storage.hasAlarm(alarmId: alarmId)
    }

    func snoozeAlarm(alarmId: String, minutes: Int) async throws {
        guard let alarm = storage.getAlarm(alarmId: alarmId) else {
            throw NSError(domain: "NotificationManager", code: 3, userInfo: [NSLocalizedDescriptionKey: "Alarm not found"])
        }

        // Create a new notification for the snoozed time
        var snoozeConfig = alarm
        snoozeConfig["id"] = "\(alarmId)-snooze-\(Int(Date().timeIntervalSince1970))"
        let snoozeTime = Date().addingTimeInterval(TimeInterval(minutes * 60)).timeIntervalSince1970 * 1000
        snoozeConfig["time"] = snoozeTime

        if var title = snoozeConfig["title"] as? String {
            title += " (Snoozed)"
            snoozeConfig["title"] = title
        }

        try await scheduleAlarm(config: snoozeConfig)
    }

    func getLaunchPayload() async throws -> [String: Any]? {
        return storage.getLaunchPayload()
    }

    private func setupNotificationCategories() {
        let dismissAction = UNNotificationAction(
            identifier: "DISMISS_ACTION",
            title: "Dismiss",
            options: []
        )

        let snoozeAction = UNNotificationAction(
            identifier: "SNOOZE_ACTION",
            title: "Snooze",
            options: []
        )

        let category = UNNotificationCategory(
            identifier: "ALARM_WITH_SNOOZE",
            actions: [dismissAction, snoozeAction],
            intentIdentifiers: [],
            options: []
        )

        let dismissOnlyCategory = UNNotificationCategory(
            identifier: "ALARM",
            actions: [dismissAction],
            intentIdentifiers: [],
            options: []
        )

        notificationCenter.setNotificationCategories([category, dismissOnlyCategory])
    }

    // MARK: - UNUserNotificationCenterDelegate

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        guard let alarmId = userInfo["alarmId"] as? String else {
            completionHandler()
            return
        }

        switch response.actionIdentifier {
        case "DISMISS_ACTION", UNNotificationDefaultActionIdentifier:
            // User dismissed or tapped the notification
            storage.saveLaunchPayload([
                "alarmId": alarmId,
                "action": "dismiss",
                "timestamp": Date().timeIntervalSince1970 * 1000
            ])

        case "SNOOZE_ACTION":
            // User snoozed
            storage.saveLaunchPayload([
                "alarmId": alarmId,
                "action": "snooze",
                "timestamp": Date().timeIntervalSince1970 * 1000
            ])

            // Snooze for 5 minutes
            Task {
                try? await snoozeAlarm(alarmId: alarmId, minutes: 5)
            }

        default:
            break
        }

        completionHandler()
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        // Show notification even when app is in foreground
        completionHandler([.banner, .sound, .badge])
    }
}
