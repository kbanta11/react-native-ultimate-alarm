# React Native Ultimate Alarm - Implementation Plan

**Goal:** Create a unified alarm package that combines the best of react-native-alarmageddon and expo-alarm-kit into a single npm package with one API that works on all platforms.

**Status:** Foundation complete, native implementations needed

---

## Current Status

### ✅ Complete
- [x] Package structure created
- [x] TypeScript API layer (`src/index.ts`, `src/types.ts`, `src/NativeUltimateAlarm.ts`)
- [x] package.json with dependencies
- [x] tsconfig.json configured
- [x] README.md with full documentation
- [x] Platform detection logic
- [x] Event emitter setup

### 🔨 In Progress
- [ ] Android native implementation
- [ ] iOS native implementation
- [ ] Example app
- [ ] Testing

---

## Implementation Strategy

We will extract proven code from two source packages:
1. **react-native-alarmageddon** - Android implementation + iOS notifications
2. **expo-alarm-kit** - iOS AlarmKit implementation

**Why extract instead of fork?**
- We need a unified API across all platforms
- We want to combine Android (from alarmageddon) + iOS AlarmKit (from expo-alarm-kit)
- We can improve and customize as needed
- Single source of truth for maintenance

---

## Phase 1: Clone Source Packages (30 min)

**Location:** Clone these alongside the package (NOT inside it)

```bash
cd C:\Users\Kyle\documents

# Clone source packages for reference
git clone https://github.com/joaoGabriel55/react-native-alarmageddon.git
git clone https://github.com/nickdeupree/expo-alarm-kit.git
```

**What we'll extract:**

### From react-native-alarmageddon:
- `android/src/main/java/com/joaogabriel/alarmageddon/`
  - ✅ AlarmModule.kt → UltimateAlarmModule.kt (bridge to React Native)
  - ✅ AlarmReceiver.kt → AlarmReceiver.kt (handles alarm trigger)
  - ✅ AlarmService.kt → AlarmService.kt (plays alarm sound)
  - ✅ BootReceiver.kt → BootReceiver.kt (re-register alarms on boot)
  - ✅ AlarmStorage (SharedPreferences wrapper)

- `ios/` (for notification fallback)
  - ✅ iOS notification scheduling code
  - ✅ UNUserNotificationCenter setup

### From expo-alarm-kit:
- `ios/`
  - ✅ AlarmKit integration code
  - ✅ Live Activity intent handlers
  - ✅ App Group storage

---

## Phase 2: Android Implementation (2-3 days)

### Step 1: Set up Android module structure

Create:
```
android/
├── build.gradle
├── src/main/
│   ├── AndroidManifest.xml
│   └── java/com/ultimatealarm/
│       ├── UltimateAlarmModule.kt
│       ├── UltimateAlarmPackage.kt
│       ├── AlarmReceiver.kt
│       ├── AlarmService.kt
│       ├── BootReceiver.kt
│       └── AlarmStorage.kt
```

### Step 2: Extract AlarmModule

**Source:** `react-native-alarmageddon/android/src/main/java/com/joaogabriel/alarmageddon/AlarmModule.kt`

**Key changes:**
- Rename to `UltimateAlarmModule.kt`
- Change package to `com.ultimatealarm`
- Update module name to `"UltimateAlarm"`
- Add `hasAlarmKit()` method (returns false on Android)
- Ensure all methods match our TypeScript API:
  - `getCapabilities(implementation: String)`
  - `requestPermissions(implementation: String)`
  - `scheduleAlarm(implementation: String, config: ReadableMap)`
  - `cancelAlarm(implementation: String, alarmId: String)`
  - `cancelAllAlarms(implementation: String)`
  - `getAllAlarms(implementation: String)`
  - `isAlarmScheduled(implementation: String, alarmId: String)`
  - `snoozeAlarm(implementation: String, alarmId: String, minutes: Int)`
  - `getLaunchPayload(implementation: String)`

**What to keep:**
- ✅ `AlarmManager.setExactAndAllowWhileIdle()` for reliable scheduling
- ✅ Permission checking for Android 12+ exact alarms
- ✅ Full-screen intent for lock screen display
- ✅ Broadcast receiver pattern

**What to add:**
- ✨ Repeat alarm support (alarmageddon doesn't have this)
- ✨ Better error handling
- ✨ Configurable auto-stop duration

### Step 3: Extract AlarmReceiver

**Source:** `react-native-alarmageddon/android/src/main/java/com/joaogabriel/alarmageddon/AlarmReceiver.kt`

**Key changes:**
- Change package to `com.ultimatealarm`
- Update to start our `AlarmService`
- Pass through alarm metadata (title, message, sound, data)

**What to keep:**
- ✅ BroadcastReceiver pattern
- ✅ Intent handling
- ✅ Foreground service launch (Android 8+)

### Step 4: Extract AlarmService

**Source:** `react-native-alarmageddon/android/src/main/java/com/joaogabriel/alarmageddon/AlarmService.kt`

**Key changes:**
- Change package to `com.ultimatealarm`
- Use our notification builder
- Use our audio playback logic

**What to keep:**
- ✅ MediaPlayer with STREAM_ALARM
- ✅ WakeLock acquisition (65 seconds)
- ✅ AudioFocus request
- ✅ Notification with full-screen intent
- ✅ 60-second auto-stop (make configurable)

**What to improve:**
- ✨ Make auto-stop duration configurable
- ✨ Better resource cleanup
- ✨ Vibration pattern control

### Step 5: Extract BootReceiver

**Source:** `react-native-alarmageddon/android/src/main/java/com/joaogabriel/alarmageddon/BootReceiver.kt`

**Key changes:**
- Change package to `com.ultimatealarm`
- Update to use our storage

**What to keep:**
- ✅ BOOT_COMPLETED intent filter
- ✅ Re-register all alarms from storage
- ✅ Handle QUICKBOOT_POWERON (some devices)

### Step 6: Create AlarmStorage

**Purpose:** Persist alarm metadata for boot recovery and querying

**Implementation:**
```kotlin
class AlarmStorage(private val context: Context) {
    private val prefs = context.getSharedPreferences("UltimateAlarms", Context.MODE_PRIVATE)

    fun saveAlarm(alarmId: String, config: ReadableMap)
    fun getAlarm(alarmId: String): ReadableMap?
    fun deleteAlarm(alarmId: String)
    fun getAllAlarms(): List<String>
    fun getAllAlarmConfigs(): List<ReadableMap>
    fun hasAlarm(alarmId: String): Boolean
    fun clearAllAlarms()

    // For app launch detection
    fun saveLaunchPayload(payload: Map<String, Any>)
    fun getLaunchPayload(): Map<String, Any>?
    fun clearLaunchPayload()
}
```

### Step 7: Create Android Manifest entries

**File:** `android/src/main/AndroidManifest.xml`

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          package="com.ultimatealarm">

    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />

    <application>
        <!-- Alarm Receiver -->
        <receiver
            android:name=".AlarmReceiver"
            android:enabled="true"
            android:exported="false" />

        <!-- Boot Receiver -->
        <receiver
            android:name=".BootReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.QUICKBOOT_POWERON" />
            </intent-filter>
        </receiver>

        <!-- Foreground Service for alarm audio -->
        <service
            android:name=".AlarmService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="mediaPlayback" />
    </application>
</manifest>
```

### Step 8: Create build.gradle

**File:** `android/build.gradle`

```gradle
buildscript {
    ext.kotlin_version = '1.8.0'
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:7.4.2'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}

apply plugin: 'com.android.library'
apply plugin: 'kotlin-android'

android {
    compileSdkVersion 33

    defaultConfig {
        minSdkVersion 21
        targetSdkVersion 33
    }

    sourceSets {
        main {
            manifest.srcFile 'src/main/AndroidManifest.xml'
            java.srcDirs = ['src/main/java']
        }
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation 'com.facebook.react:react-native:+'
    implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
}
```

### Step 9: Create Package class

**File:** `android/src/main/java/com/ultimatealarm/UltimateAlarmPackage.kt`

```kotlin
package com.ultimatealarm

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class UltimateAlarmPackage : ReactPackage {
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        return listOf(UltimateAlarmModule(reactContext))
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList()
    }
}
```

---

## Phase 3: iOS Implementation (2-3 days)

### Step 1: Set up iOS module structure

Create:
```
ios/
├── UltimateAlarm.podspec
├── UltimateAlarm.swift
├── AlarmKitManager.swift
├── NotificationManager.swift
└── AlarmStorage.swift
```

### Step 2: Create main module (Expo Module style)

**File:** `ios/UltimateAlarm.swift`

**Purpose:** Route calls to AlarmKit or Notification manager based on iOS version

```swift
import ExpoModulesCore
import UserNotifications
import AlarmKit

public class UltimateAlarmModule: Module {
  private let alarmKitManager = AlarmKitManager()
  private let notificationManager = NotificationManager()

  public func definition() -> ModuleDefinition {
    Name("UltimateAlarm")

    AsyncFunction("hasAlarmKit") { () -> Bool in
      if #available(iOS 16.0, *) {
        return true
      }
      return false
    }

    AsyncFunction("getCapabilities") { (implementation: String) -> [String: Any] in
      if implementation == "alarmkit" {
        return self.alarmKitManager.getCapabilities()
      } else {
        return self.notificationManager.getCapabilities()
      }
    }

    // ... route all other methods similarly
  }
}
```

### Step 3: Extract AlarmKit manager (iOS 16+)

**Source:** `expo-alarm-kit/ios/ExpoAlarmKitModule.swift`

**File:** `ios/AlarmKitManager.swift`

**Key changes:**
- Rename to `AlarmKitManager`
- Remove Expo-specific module wrapper
- Make it a helper class used by main module
- Update to match our API

**What to extract:**
- ✅ `AlarmManager.shared` usage
- ✅ `scheduleAlarm()` and `scheduleRepeatingAlarm()` methods
- ✅ Live Activity intent handlers (AlarmDismissIntentWithLaunch, AlarmSnoozeIntentWithLaunch)
- ✅ App Group storage for launch payloads
- ✅ Authorization request

**What to keep:**
- ✅ All AlarmKit native features
- ✅ Repeating alarm support
- ✅ Snooze support
- ✅ App launch on dismiss

**Code structure:**
```swift
@available(iOS 16.0, *)
class AlarmKitManager {
  private let storage = AlarmStorage()

  func getCapabilities() -> [String: Any]
  func requestPermissions() async -> Bool
  func scheduleAlarm(config: [String: Any]) async throws
  func cancelAlarm(alarmId: String) async throws
  func cancelAllAlarms() async throws
  func getAllAlarms() async throws -> [[String: Any]]
  func isAlarmScheduled(alarmId: String) async throws -> Bool
  func snoozeAlarm(alarmId: String, minutes: Int) async throws
  func getLaunchPayload() async throws -> [String: Any]?

  private func scheduleOneTimeAlarm(...) async throws
  private func scheduleRepeatingAlarm(...) async throws
}
```

### Step 4: Extract Notification manager (iOS < 16)

**Source:** `react-native-alarmageddon/ios/` notification code

**File:** `ios/NotificationManager.swift`

**Key changes:**
- Create as helper class
- Use `UNUserNotificationCenter`
- Match our API

**What to implement:**
- ✅ UNCalendarNotificationTrigger for scheduling
- ✅ Custom notification sounds (30s limit)
- ✅ Notification categories with Snooze/Dismiss actions
- ✅ UserDefaults storage
- ✅ Delegate methods for handling user actions

**Code structure:**
```swift
class NotificationManager: NSObject, UNUserNotificationCenterDelegate {
  private let storage = AlarmStorage()

  func getCapabilities() -> [String: Any]
  func requestPermissions() async -> Bool
  func scheduleAlarm(config: [String: Any]) async throws
  func cancelAlarm(alarmId: String) async throws
  func cancelAllAlarms() async throws
  func getAllAlarms() async throws -> [[String: Any]]
  func isAlarmScheduled(alarmId: String) async throws -> Bool
  func snoozeAlarm(alarmId: String, minutes: Int) async throws
  func getLaunchPayload() async throws -> [String: Any]?

  // UNUserNotificationCenterDelegate
  func userNotificationCenter(_:didReceive:withCompletionHandler:)
}
```

### Step 5: Create AlarmStorage (iOS)

**File:** `ios/AlarmStorage.swift`

**Purpose:** Persist alarm metadata and launch payloads

```swift
class AlarmStorage {
  private let defaults = UserDefaults.standard
  private let alarmsKey = "UltimateAlarms"
  private let launchPayloadKey = "UltimateAlarmLaunchPayload"

  func saveAlarm(alarmId: String, config: [String: Any])
  func getAlarm(alarmId: String) -> [String: Any]?
  func deleteAlarm(alarmId: String)
  func getAllAlarms() -> [[String: Any]]
  func hasAlarm(alarmId: String) -> Bool
  func clearAllAlarms()

  // For app launch detection
  func saveLaunchPayload(_ payload: [String: Any])
  func getLaunchPayload() -> [String: Any]?
  func clearLaunchPayload()
}
```

### Step 6: Create Podspec

**File:** `ios/UltimateAlarm.podspec`

```ruby
Pod::Spec.new do |s|
  s.name           = 'UltimateAlarm'
  s.version        = '1.0.0'
  s.summary        = 'Unified alarm package for React Native'
  s.description    = 'True alarm support on Android and iOS with graceful fallbacks'
  s.author         = 'Kyle'
  s.homepage       = 'https://github.com/kylemichaelreaves/react-native-ultimate-alarm'
  s.license        = 'MIT'
  s.platform       = :ios, '13.0'
  s.source         = { git: '' }
  s.source_files   = '*.{swift}'

  s.dependency 'ExpoModulesCore'
  s.swift_version  = '5.0'
end
```

---

## Phase 4: Example App (1 day)

### Step 1: Create Expo example app

```bash
cd C:\Users\Kyle\documents\react-native-ultimate-alarm
npx create-expo-app example --template blank-typescript
cd example
```

### Step 2: Link local package

**File:** `example/package.json`

```json
{
  "dependencies": {
    "react-native-ultimate-alarm": "file:..",
    "expo": "~51.0.0",
    "react": "18.2.0",
    "react-native": "0.74.0"
  }
}
```

### Step 3: Create test app

**File:** `example/app/index.tsx`

```typescript
import { useEffect, useState } from 'react';
import { View, Text, Button, Alert, StyleSheet, ScrollView } from 'react-native';
import UltimateAlarm from 'react-native-ultimate-alarm';

export default function AlarmTest() {
  const [capabilities, setCapabilities] = useState(null);
  const [alarms, setAlarms] = useState([]);

  useEffect(() => {
    async function init() {
      // Get capabilities
      const caps = await UltimateAlarm.getCapabilities();
      setCapabilities(caps);

      // Request permissions
      const hasPermissions = await UltimateAlarm.requestPermissions();
      if (!hasPermissions) {
        Alert.alert('Permissions Required', 'Please grant alarm permissions');
      }

      // Check if launched from alarm
      const launchPayload = await UltimateAlarm.getLaunchPayload();
      if (launchPayload) {
        Alert.alert(
          'Alarm Dismissed!',
          `Alarm: ${launchPayload.alarmId}\nAction: ${launchPayload.action}`
        );
      }

      // Load existing alarms
      refreshAlarms();
    }

    init();

    // Listen for alarm events
    const dismissSub = UltimateAlarm.addEventListener('dismiss', (event) => {
      console.log('Alarm dismissed:', event);
      Alert.alert('Dismissed', `Alarm ${event.alarmId} was dismissed`);
    });

    const snoozeSub = UltimateAlarm.addEventListener('snooze', (event) => {
      console.log('Alarm snoozed:', event);
      Alert.alert('Snoozed', `Alarm ${event.alarmId} was snoozed`);
    });

    return () => {
      dismissSub.remove();
      snoozeSub.remove();
    };
  }, []);

  async function refreshAlarms() {
    const allAlarms = await UltimateAlarm.getAllAlarms();
    setAlarms(allAlarms);
  }

  async function scheduleTestAlarm() {
    try {
      const alarmTime = new Date();
      alarmTime.setSeconds(alarmTime.getSeconds() + 10); // 10 seconds from now

      await UltimateAlarm.scheduleAlarm({
        id: `test-${Date.now()}`,
        time: alarmTime,
        title: 'Test Alarm',
        message: 'This is a test alarm (10 seconds)',
        snooze: {
          enabled: true,
          duration: 300,
        },
        data: {
          test: true,
          timestamp: Date.now(),
        },
      });

      Alert.alert('Success', 'Alarm scheduled for 10 seconds from now');
      refreshAlarms();
    } catch (error) {
      Alert.alert('Error', error.message);
    }
  }

  async function scheduleRepeatingAlarm() {
    try {
      const alarmTime = new Date();
      alarmTime.setHours(9, 0, 0, 0); // 9 AM

      await UltimateAlarm.scheduleAlarm({
        id: 'weekday-alarm',
        time: alarmTime,
        title: 'Weekday Wake-up',
        message: 'Time to start your day!',
        repeat: {
          weekdays: [1, 2, 3, 4, 5], // Monday-Friday
        },
        snooze: {
          enabled: true,
          duration: 300,
        },
      });

      Alert.alert('Success', 'Repeating alarm scheduled for weekdays at 9 AM');
      refreshAlarms();
    } catch (error) {
      Alert.alert('Error', error.message);
    }
  }

  async function cancelAllAlarms() {
    try {
      await UltimateAlarm.cancelAllAlarms();
      Alert.alert('Success', 'All alarms cancelled');
      refreshAlarms();
    } catch (error) {
      Alert.alert('Error', error.message);
    }
  }

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.title}>Ultimate Alarm Test</Text>

      {capabilities && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Capabilities</Text>
          <Text>Platform: {capabilities.platform}</Text>
          <Text>Implementation: {capabilities.implementation}</Text>
          <Text>
            True Alarm: {capabilities.features.truePersistentAlarm ? '✅' : '❌'}
          </Text>
          <Text>
            Auto Launch: {capabilities.features.autoLaunchApp ? '✅' : '❌'}
          </Text>
          <Text>
            Bypass Silent: {capabilities.features.bypassSilentMode ? '✅' : '❌'}
          </Text>
          <Text>
            Persistent Sound: {capabilities.features.persistentSound ? '✅' : '❌'}
          </Text>
          {capabilities.limitations && (
            <View style={styles.limitations}>
              <Text style={styles.warningText}>Limitations:</Text>
              {capabilities.limitations.map((limit, i) => (
                <Text key={i} style={styles.limitationText}>
                  • {limit}
                </Text>
              ))}
            </View>
          )}
        </View>
      )}

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Actions</Text>
        <Button title="Schedule Test Alarm (10s)" onPress={scheduleTestAlarm} />
        <View style={styles.spacer} />
        <Button
          title="Schedule Repeating Alarm (Weekdays 9 AM)"
          onPress={scheduleRepeatingAlarm}
        />
        <View style={styles.spacer} />
        <Button title="Cancel All Alarms" onPress={cancelAllAlarms} color="red" />
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>
          Scheduled Alarms ({alarms.length})
        </Text>
        {alarms.map((alarm, i) => (
          <View key={i} style={styles.alarmItem}>
            <Text style={styles.alarmTitle}>{alarm.title}</Text>
            <Text>ID: {alarm.id}</Text>
            <Text>Time: {new Date(alarm.time).toLocaleString()}</Text>
            {alarm.repeat && (
              <Text>
                Repeats: {alarm.repeat.weekdays.map((d) => ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][d]).join(', ')}
              </Text>
            )}
          </View>
        ))}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    backgroundColor: '#fff',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginTop: 40,
    marginBottom: 20,
    textAlign: 'center',
  },
  section: {
    marginBottom: 30,
    padding: 15,
    backgroundColor: '#f5f5f5',
    borderRadius: 8,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 10,
  },
  limitations: {
    marginTop: 10,
    padding: 10,
    backgroundColor: '#fff3cd',
    borderRadius: 4,
  },
  warningText: {
    fontWeight: 'bold',
    marginBottom: 5,
  },
  limitationText: {
    fontSize: 12,
    color: '#856404',
  },
  spacer: {
    height: 10,
  },
  alarmItem: {
    padding: 10,
    backgroundColor: '#fff',
    marginBottom: 10,
    borderRadius: 4,
    borderLeftWidth: 3,
    borderLeftColor: '#007AFF',
  },
  alarmTitle: {
    fontWeight: 'bold',
    fontSize: 16,
    marginBottom: 5,
  },
});
```

### Step 4: Test on real devices

**Android:**
```bash
cd example
npx expo run:android
```

**iOS:**
```bash
cd example
npx expo run:ios
```

**Test cases:**
- [ ] Schedule 10-second test alarm
- [ ] Verify alarm fires at correct time
- [ ] Test snooze functionality
- [ ] Test dismiss functionality
- [ ] Verify app launches on dismiss (Android, iOS 16+)
- [ ] Test cancel alarm
- [ ] Test cancel all alarms
- [ ] Schedule repeating alarm
- [ ] Verify boot persistence (reboot device)
- [ ] Test on iOS < 16 (notification fallback)

---

## Phase 5: Documentation & Polish (1 day)

### Step 1: Create API documentation

**File:** `docs/API.md`

Document every method with:
- Description
- Parameters
- Return type
- Example usage
- Platform differences

### Step 2: Create permissions guide

**File:** `docs/PERMISSIONS.md`

Document:
- Required permissions per platform
- How to request them
- What happens if denied
- Battery optimization guidance

### Step 3: Create capabilities guide

**File:** `docs/CAPABILITIES.md`

Document:
- Feature matrix (Android vs iOS 16+ vs iOS <16)
- How to detect capabilities
- How to handle degraded experiences
- User messaging recommendations

### Step 4: Create examples guide

**File:** `docs/EXAMPLES.md`

Provide code examples for:
- Basic alarm scheduling
- Repeating alarms
- Handling app launch from alarm
- Event listeners
- Error handling
- Best practices

### Step 5: Create CHANGELOG

**File:** `CHANGELOG.md`

```markdown
# Changelog

## [1.0.0] - 2026-02-13

### Added
- Initial release
- Android support via AlarmManager
- iOS 16+ support via AlarmKit
- iOS <16 fallback via notifications
- Unified API across all platforms
- Automatic platform detection
- Snooze functionality
- Repeating alarms
- App launch on dismiss
- Event listeners
- TypeScript support
```

### Step 6: Create LICENSE

**File:** `LICENSE`

```
MIT License

Copyright (c) 2026 Kyle

Permission is hereby granted, free of charge, to any person obtaining a copy...
```

### Step 7: Create CONTRIBUTING guide

**File:** `CONTRIBUTING.md`

Guidelines for:
- Setting up development environment
- Running tests
- Code style
- Submitting PRs
- Reporting bugs

---

## Phase 6: Testing & Quality Assurance (1-2 days)

### Step 1: Manual testing checklist

**Android:**
- [ ] Alarm fires at exact scheduled time
- [ ] Full-screen intent shows on lock screen
- [ ] Sound plays at max volume
- [ ] Sound bypasses silent mode
- [ ] Snooze button works
- [ ] Dismiss button works
- [ ] App launches on dismiss
- [ ] Alarm survives app force-close
- [ ] Alarm survives device reboot
- [ ] Repeating alarms work
- [ ] Cancel alarm works
- [ ] Cancel all alarms works
- [ ] Multiple alarms coexist

**iOS 16+:**
- [ ] Native iOS alarm UI appears
- [ ] Sound plays persistently
- [ ] App launches on dismiss
- [ ] Snooze works natively
- [ ] Alarm survives app force-close
- [ ] Alarm survives reboot
- [ ] Repeating alarms work
- [ ] Cancel alarm works

**iOS <16:**
- [ ] Notification appears at scheduled time
- [ ] Sound plays (30 seconds)
- [ ] Tapping notification opens app
- [ ] Snooze action works
- [ ] Notification survives force-close
- [ ] Notification survives reboot
- [ ] Cancel notification works

### Step 2: Edge cases

- [ ] Alarm scheduled in the past (should error)
- [ ] Alarm while device in airplane mode
- [ ] Alarm during active phone call
- [ ] Alarm while playing music
- [ ] Time zone changes
- [ ] Multiple alarms at same time
- [ ] Permission denied scenarios
- [ ] Storage full scenarios

### Step 3: Performance testing

- [ ] Battery impact measurement
- [ ] Memory usage
- [ ] Storage footprint
- [ ] Network usage (should be zero)

### Step 4: Code quality

- [ ] Run TypeScript type checking: `npm run typecheck`
- [ ] Run linter: `npm run lint`
- [ ] Fix all warnings
- [ ] Remove console.logs
- [ ] Add error handling

---

## Phase 7: Publishing (1 day)

### Step 1: Prepare for release

- [ ] Update version in package.json to 1.0.0
- [ ] Finalize CHANGELOG.md
- [ ] Ensure README.md is complete
- [ ] Verify all examples work
- [ ] Test on fresh install

### Step 2: Build package

```bash
npm run prepare
```

This builds the TypeScript to JavaScript using react-native-builder-bob.

### Step 3: Test local installation

```bash
# In another test project
npm install ../react-native-ultimate-alarm
```

Verify it installs and works correctly.

### Step 4: Create GitHub repository

```bash
cd C:\Users\Kyle\documents\react-native-ultimate-alarm
git init
git add .
git commit -m "Initial release v1.0.0"
git branch -M main
git remote add origin https://github.com/kylemichaelreaves/react-native-ultimate-alarm.git
git push -u origin main
```

### Step 5: Create GitHub release

- Go to GitHub > Releases > Create new release
- Tag: v1.0.0
- Title: v1.0.0 - Initial Release
- Description: Copy from CHANGELOG.md
- Publish release

### Step 6: Publish to npm

```bash
# Login to npm (first time only)
npm login

# Publish
npm publish --access public
```

### Step 7: Verify npm package

- Visit https://www.npmjs.com/package/react-native-ultimate-alarm
- Test installation: `npm install react-native-ultimate-alarm`
- Verify README displays correctly
- Check package contents

---

## Phase 8: Post-Launch (Ongoing)

### Step 1: Monitor issues

- Watch GitHub issues
- Respond to bug reports
- Collect feature requests

### Step 2: Promote package

- Share on Twitter/X
- Post on Reddit (r/reactnative)
- Share in React Native Discord
- Write blog post

### Step 3: Future enhancements

**v1.1:**
- Smart alarm (wake during light sleep)
- Custom alarm sounds library
- Alarm sound gradual volume increase

**v1.2:**
- Alarm analytics
- Social features (share routines)
- Integration with health apps

**v2.0:**
- Breaking: Improved API
- Better error messages
- More configuration options

---

## Key Files Reference

### Already Created ✅

1. `src/index.ts` - Main API with platform detection
2. `src/types.ts` - TypeScript interfaces
3. `src/NativeUltimateAlarm.ts` - Native module bindings
4. `package.json` - Package configuration
5. `tsconfig.json` - TypeScript config
6. `README.md` - Documentation

### Need to Create 🔨

#### Android (Priority 1)
7. `android/build.gradle`
8. `android/src/main/AndroidManifest.xml`
9. `android/src/main/java/com/ultimatealarm/UltimateAlarmModule.kt`
10. `android/src/main/java/com/ultimatealarm/UltimateAlarmPackage.kt`
11. `android/src/main/java/com/ultimatealarm/AlarmReceiver.kt`
12. `android/src/main/java/com/ultimatealarm/AlarmService.kt`
13. `android/src/main/java/com/ultimatealarm/BootReceiver.kt`
14. `android/src/main/java/com/ultimatealarm/AlarmStorage.kt`

#### iOS (Priority 2)
15. `ios/UltimateAlarm.podspec`
16. `ios/UltimateAlarm.swift`
17. `ios/AlarmKitManager.swift`
18. `ios/NotificationManager.swift`
19. `ios/AlarmStorage.swift`

#### Example App (Priority 3)
20. `example/` - Full Expo app
21. `example/app/index.tsx` - Test interface

#### Documentation (Priority 4)
22. `docs/API.md`
23. `docs/PERMISSIONS.md`
24. `docs/CAPABILITIES.md`
25. `docs/EXAMPLES.md`
26. `CHANGELOG.md`
27. `LICENSE`
28. `CONTRIBUTING.md`

---

## Success Criteria

**Package is complete when:**

✅ All native implementations are done and tested
✅ Example app works on real Android device
✅ Example app works on real iOS device (16+ and <16)
✅ All test cases pass
✅ Documentation is complete
✅ Package is published to npm
✅ README examples work for fresh install

**Package is successful when:**

🎯 1000+ downloads in first month
🎯 50+ GitHub stars in first 3 months
🎯 Used in 10+ production apps within 6 months
🎯 <5 open issues at any time
🎯 4.5+ star rating

---

## Timeline Estimate

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Clone source packages | 30 min | None |
| Android implementation | 2-3 days | Source packages |
| iOS implementation | 2-3 days | Source packages |
| Example app | 1 day | Native implementations |
| Documentation | 1 day | Example app |
| Testing & QA | 1-2 days | Example app |
| Publishing | 1 day | Testing complete |
| **Total** | **7-11 days** | |

**Recommended schedule:**
- Week 1: Android implementation + iOS AlarmKit
- Week 2: iOS notifications + Example app + Testing
- Week 3: Documentation + Polish + Publishing

---

## Next Steps (Start Here!)

1. **Clone source packages:**
   ```bash
   cd C:\Users\Kyle\documents
   git clone https://github.com/joaoGabriel55/react-native-alarmageddon.git
   git clone https://github.com/nickdeupree/expo-alarm-kit.git
   ```

2. **Start with Android implementation** (easier, more complete in alarmageddon)
   - Extract AlarmModule.kt first
   - Test it works before moving to other files

3. **Then iOS AlarmKit** (for iOS 16+)
   - Extract from expo-alarm-kit
   - Test on iOS 16+ device

4. **Then iOS Notifications** (for iOS <16)
   - Extract from alarmageddon
   - Test on iOS 15 or earlier

5. **Build example app and test everything**

6. **Publish!**

---

**Good luck! This will be an awesome contribution to the React Native community! 🚀**
