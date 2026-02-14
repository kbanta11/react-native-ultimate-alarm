# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-02-13

### Added
- Initial release of `react-native-ultimate-alarm`
- **Android support** via native AlarmManager
  - True persistent alarms that survive force-close
  - Full-screen lock screen display
  - Sound playback with STREAM_ALARM (bypasses silent mode)
  - Boot persistence (alarms survive device reboot)
  - Exact alarm scheduling with AlarmManager.setExactAndAllowWhileIdle()
- **iOS 16+ support** via native AlarmKit
  - Native iOS alarm experience (same as Clock app)
  - Persistent sound until dismissed
  - Auto-launches app on dismiss
  - Live Activity integration for snooze/dismiss
  - App Group storage for launch payloads
- **iOS <16 fallback** via local notifications
  - Notification-based alarms for older iOS versions
  - Snooze and dismiss notification actions
  - UserDefaults storage
- **Unified API** across all platforms
  - Automatic platform detection
  - Single API regardless of implementation
  - Graceful feature degradation
- **Core Features**
  - Schedule one-time alarms
  - Schedule repeating alarms (weekday selection)
  - Snooze functionality
  - Custom alarm sounds
  - Custom data payloads
  - Event listeners for dismiss/snooze
  - App launch detection from alarm
- **TypeScript support**
  - Full type definitions
  - Type-safe API
  - IntelliSense support
- **Comprehensive testing**
  - 42 unit tests
  - 100% test coverage for core functionality
  - Platform detection tests
  - Event emitter tests
- **Documentation**
  - Complete API reference
  - Platform permissions guide
  - Capabilities matrix
  - Usage examples
  - Contributing guidelines

### Implementation Details
- Android: Kotlin implementation with AlarmManager, foreground service, broadcast receivers
- iOS: Swift implementation with AlarmKit and UNUserNotificationCenter
- TypeScript API layer with automatic platform selection
- Jest test suite with ts-jest
- React Native 0.73+ compatible
- Expo compatible

[unreleased]: https://github.com/kylemichaelreaves/react-native-ultimate-alarm/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/kylemichaelreaves/react-native-ultimate-alarm/releases/tag/v0.1.0
