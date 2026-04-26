# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.1] - 2026-04-25

### Fixed
- Tighten `peerDependencies` so npm cannot hoist an incompatible
  `expo-modules-core`. The previous `>=2.0.0` constraint allowed
  much newer versions to be hoisted, causing API mismatches against
  the `expo-modules-core` that ships with `expo` itself (e.g. SDK 54
  ships `expo-modules-core@3.x`, but npm could resolve `55.x` for
  this package and break the Gradle build with `ConstantsService`
  not implementing `val constants` and similar errors).
- Pin to the SDK 54 versions of `expo` (`^54.0.0`) and
  `expo-modules-core` (`^3.0.0`); raise `react` to `>=18.0.0` and
  `react-native` to `>=0.74.0` to match what's actually been tested.

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
