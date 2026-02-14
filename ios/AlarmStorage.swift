import Foundation

class AlarmStorage {
    private let defaults = UserDefaults.standard
    private let alarmsKey = "UltimateAlarms"
    private let launchPayloadKey = "UltimateAlarmLaunchPayload"

    // MARK: - Alarm Management

    func saveAlarm(alarmId: String, config: [String: Any]) {
        var alarms = getAllAlarmsDict()
        alarms[alarmId] = config
        defaults.set(alarms, forKey: alarmsKey)
    }

    func getAlarm(alarmId: String) -> [String: Any]? {
        let alarms = getAllAlarmsDict()
        return alarms[alarmId] as? [String: Any]
    }

    func deleteAlarm(alarmId: String) {
        var alarms = getAllAlarmsDict()
        alarms.removeValue(forKey: alarmId)
        defaults.set(alarms, forKey: alarmsKey)
    }

    func getAllAlarms() -> [String] {
        let alarms = getAllAlarmsDict()
        return Array(alarms.keys)
    }

    func getAllAlarmConfigs() -> [[String: Any]] {
        let alarms = getAllAlarmsDict()
        return alarms.values.compactMap { $0 as? [String: Any] }
    }

    func hasAlarm(alarmId: String) -> Bool {
        let alarms = getAllAlarmsDict()
        return alarms[alarmId] != nil
    }

    func clearAllAlarms() {
        defaults.removeObject(forKey: alarmsKey)
    }

    // MARK: - Launch Payload Management

    func saveLaunchPayload(_ payload: [String: Any]) {
        defaults.set(payload, forKey: launchPayloadKey)
    }

    func getLaunchPayload() -> [String: Any]? {
        return defaults.dictionary(forKey: launchPayloadKey)
    }

    func clearLaunchPayload() {
        defaults.removeObject(forKey: launchPayloadKey)
    }

    // MARK: - Private Helpers

    private func getAllAlarmsDict() -> [String: Any] {
        return defaults.dictionary(forKey: alarmsKey) ?? [:]
    }
}
