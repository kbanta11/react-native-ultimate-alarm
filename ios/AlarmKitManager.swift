import Foundation
import AlarmKit
import AppIntents

@available(iOS 16.0, *)
class AlarmKitManager {
    static let shared = AlarmKitManager()
    private let storage = AlarmStorage()

    private init() {}

    func getCapabilities() async throws -> [String: Any] {
        return [
            "platform": "ios",
            "implementation": "alarmkit",
            "features": [
                "truePersistentAlarm": true,
                "autoLaunchApp": true,
                "bypassSilentMode": true,
                "persistentSound": true,
                "customSound": true,
                "repeatAlarms": true
            ],
            "limitations": []
        ]
    }

    func requestPermissions() async throws -> Bool {
        let status = try await AlarmManager.shared.requestAuthorization()
        return status == .authorized
    }

    func hasPermissions() async -> Bool {
        return AlarmManager.shared.authorizationState == .authorized
    }

    func scheduleAlarm(config: [String: Any]) async throws {
        guard let id = config["id"] as? String,
              let timeMs = config["time"] as? Double else {
            throw NSError(domain: "AlarmKitManager", code: 1, userInfo: [NSLocalizedDescriptionKey: "Missing id or time"])
        }

        let title = config["title"] as? String ?? "Alarm"
        let message = config["message"] as? String ?? ""

        // Parse snooze config
        let snoozeConfig = config["snooze"] as? [String: Any]
        let snoozeEnabled = snoozeConfig?["enabled"] as? Bool ?? false
        let snoozeDuration = snoozeConfig?["duration"] as? Int ?? 300

        // Parse repeat config
        let repeatConfig = config["repeat"] as? [String: Any]
        let weekdays = repeatConfig?["weekdays"] as? [Int]

        struct Meta: AlarmMetadata {}

        guard let uuid = UUID(uuidString: id) else {
            throw NSError(domain: "AlarmKitManager", code: 2, userInfo: [NSLocalizedDescriptionKey: "Invalid UUID"])
        }

        // Create buttons
        let stopButton = AlarmButton(
            text: LocalizedStringResource(stringLiteral: "Dismiss"),
            textColor: .white,
            systemImageName: "stop.circle"
        )

        let snoozeButton = AlarmButton(
            text: LocalizedStringResource(stringLiteral: "Snooze"),
            textColor: .white,
            systemImageName: "clock.badge.checkmark"
        )

        // Create alert presentation
        let alertPresentation = AlarmPresentation.Alert(
            title: LocalizedStringResource(stringLiteral: title),
            stopButton: stopButton,
            secondaryButton: snoozeEnabled ? snoozeButton : nil,
            secondaryButtonBehavior: .countdown
        )

        let presentation = AlarmPresentation(alert: alertPresentation)

        // Create countdown duration for snooze
        let countdownDuration = Alarm.CountdownDuration(
            preAlert: nil,
            postAlert: snoozeEnabled ? TimeInterval(snoozeDuration) : nil
        )

        // Create attributes
        let attributes = AlarmAttributes<Meta>(
            presentation: presentation,
            metadata: Meta(),
            tintColor: .blue
        )

        // Create schedule
        let schedule: Alarm.Schedule
        if let weekdays = weekdays, !weekdays.isEmpty {
            // Repeating alarm
            let weekdayArray: [Locale.Weekday] = weekdays.compactMap { day -> Locale.Weekday? in
                switch day {
                case 0: return .sunday
                case 1: return .monday
                case 2: return .tuesday
                case 3: return .wednesday
                case 4: return .thursday
                case 5: return .friday
                case 6: return .saturday
                default: return nil
                }
            }

            let date = Date(timeIntervalSince1970: timeMs / 1000)
            let calendar = Calendar.current
            let hour = calendar.component(.hour, from: date)
            let minute = calendar.component(.minute, from: date)

            let time = Alarm.Schedule.Relative.Time(hour: hour, minute: minute)
            let recurrence = Alarm.Schedule.Relative.Recurrence.weekly(weekdayArray)
            schedule = .relative(Alarm.Schedule.Relative(time: time, repeats: recurrence))
        } else {
            // One-time alarm
            let date = Date(timeIntervalSince1970: timeMs / 1000)
            schedule = .fixed(date)
        }

        // Create dismiss intent (launches app)
        let stopIntent = UltimateAlarmDismissIntent(alarmId: id)

        // Create snooze intent if enabled
        let secondaryIntent: (any LiveActivityIntent)? = snoozeEnabled
            ? UltimateAlarmSnoozeIntent(alarmId: id)
            : nil

        // Create configuration
        let alarmConfig = AlarmManager.AlarmConfiguration<Meta>(
            countdownDuration: countdownDuration,
            schedule: schedule,
            attributes: attributes,
            stopIntent: stopIntent,
            secondaryIntent: secondaryIntent,
            sound: .default
        )

        // Schedule the alarm
        try await AlarmManager.shared.schedule(id: uuid, configuration: alarmConfig)

        // Store alarm metadata
        storage.saveAlarm(alarmId: id, config: config)
    }

    func cancelAlarm(alarmId: String) async throws {
        guard let uuid = UUID(uuidString: alarmId) else {
            throw NSError(domain: "AlarmKitManager", code: 2, userInfo: [NSLocalizedDescriptionKey: "Invalid UUID"])
        }

        try AlarmManager.shared.cancel(id: uuid)
        storage.deleteAlarm(alarmId: alarmId)
    }

    func cancelAllAlarms() async throws {
        let allAlarms = storage.getAllAlarms()

        for alarmId in allAlarms {
            if let uuid = UUID(uuidString: alarmId) {
                try? AlarmManager.shared.cancel(id: uuid)
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
            throw NSError(domain: "AlarmKitManager", code: 3, userInfo: [NSLocalizedDescriptionKey: "Alarm not found"])
        }

        // Create a new alarm for the snoozed time
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
}

// MARK: - App Intents

@available(iOS 16.0, *)
public struct UltimateAlarmDismissIntent: LiveActivityIntent {
    public static var title: LocalizedStringResource = "Dismiss Alarm"
    public static var description = IntentDescription("Dismiss the alarm and open app")
    public static var openAppWhenRun: Bool = true

    @Parameter(title: "alarmId")
    public var alarmId: String

    public init() {
        self.alarmId = ""
    }

    public init(alarmId: String) {
        self.alarmId = alarmId
    }

    public func perform() async throws -> some IntentResult {
        let storage = AlarmStorage()
        storage.saveLaunchPayload([
            "alarmId": alarmId,
            "action": "dismiss",
            "timestamp": Date().timeIntervalSince1970 * 1000
        ])
        storage.deleteAlarm(alarmId: alarmId)
        return .result()
    }
}

@available(iOS 16.0, *)
public struct UltimateAlarmSnoozeIntent: LiveActivityIntent {
    public static var title: LocalizedStringResource = "Snooze Alarm"
    public static var description = IntentDescription("Snooze the alarm")
    public static var openAppWhenRun: Bool = false

    @Parameter(title: "alarmId")
    public var alarmId: String

    public init() {
        self.alarmId = ""
    }

    public init(alarmId: String) {
        self.alarmId = alarmId
    }

    public func perform() async throws -> some IntentResult {
        let storage = AlarmStorage()
        storage.saveLaunchPayload([
            "alarmId": alarmId,
            "action": "snooze",
            "timestamp": Date().timeIntervalSince1970 * 1000
        ])
        return .result()
    }
}
