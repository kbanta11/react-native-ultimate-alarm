# Permissions Guide

This guide covers all permission requirements for `react-native-ultimate-alarm` across different platforms.

## Table of Contents

- [Overview](#overview)
- [Android](#android)
  - [Required Permissions](#required-permissions-android)
  - [Setup](#setup-android)
  - [Runtime Permissions](#runtime-permissions-android)
  - [Battery Optimization](#battery-optimization)
- [iOS](#ios)
  - [iOS 16+](#ios-16-alarmkit)
  - [iOS <16](#ios-16-notifications)
  - [Setup](#setup-ios)
- [Requesting Permissions](#requesting-permissions)
- [Handling Permission Denial](#handling-permission-denial)

---

## Overview

The package requires different permissions depending on the platform and OS version:

| Platform | Implementation | Key Permission |
|----------|---------------|----------------|
| Android | AlarmManager | `SCHEDULE_EXACT_ALARM` (Android 12+) |
| iOS 16+ | AlarmKit | Notification authorization |
| iOS <16 | Notifications | Notification authorization |

---

## Android

### Required Permissions {#required-permissions-android}

Add these permissions to your `android/app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

  <!-- Exact alarm scheduling (Android 12+) -->
  <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
  <uses-permission android:name="android.permission.USE_EXACT_ALARM" />

  <!-- Wake device from sleep -->
  <uses-permission android:name="android.permission.WAKE_LOCK" />

  <!-- Vibration support -->
  <uses-permission android:name="android.permission.VIBRATE" />

  <!-- Re-register alarms after device reboot -->
  <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

  <!-- Show notifications -->
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

  <!-- Show alarm on lock screen -->
  <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />

</manifest>
```

### Setup {#setup-android}

**1. Add permissions to AndroidManifest.xml** (see above)

**2. Configure application in AndroidManifest.xml:**

```xml
<application>

  <!-- Add receivers and services -->
  <receiver
    android:name="com.ultimatealarm.AlarmReceiver"
    android:enabled="true"
    android:exported="false" />

  <receiver
    android:name="com.ultimatealarm.BootReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
      <action android:name="android.intent.action.BOOT_COMPLETED" />
      <action android:name="android.intent.action.QUICKBOOT_POWERON" />
    </intent-filter>
  </receiver>

  <service
    android:name="com.ultimatealarm.AlarmService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="mediaPlayback" />

</application>
```

### Runtime Permissions {#runtime-permissions-android}

#### Android 12 and higher (API 31+)

On Android 12+, you must request the exact alarm permission at runtime:

```typescript
import UltimateAlarm from 'react-native-ultimate-alarm';
import { Alert } from 'react-native';

async function setupAlarms() {
  const granted = await UltimateAlarm.requestPermissions();

  if (!granted) {
    Alert.alert(
      'Permission Required',
      'This app needs permission to schedule exact alarms. Please enable it in Settings.',
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Open Settings', onPress: openSettings },
      ]
    );
    return;
  }

  // Permission granted, schedule alarm
  await UltimateAlarm.scheduleAlarm({...});
}
```

#### Android 13+ (API 33+)

On Android 13+, you also need notification permission:

```typescript
import { PermissionsAndroid, Platform } from 'react-native';

async function requestNotificationPermission() {
  if (Platform.OS === 'android' && Platform.Version >= 33) {
    const granted = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
    );

    return granted === PermissionsAndroid.RESULTS.GRANTED;
  }

  return true;
}
```

### Battery Optimization

On many Android devices, battery optimization can prevent alarms from working reliably. Guide users to disable battery optimization for your app:

**Xiaomi/MIUI:**
```
Settings > Battery & Performance > App Battery Saver
Find your app > Set to "No restrictions"
```

**Huawei/EMUI:**
```
Settings > Battery > App Launch
Find your app > Enable "Manual" and toggle all options on
```

**Samsung/One UI:**
```
Settings > Apps > Your App > Battery > Battery Optimization
Select "Don't optimize"
```

**Stock Android:**
```
Settings > Battery > Battery Optimization
Select "All apps" > Find your app > Don't optimize
```

**Example - Detect and prompt user:**

```typescript
import { NativeModules, Linking, Alert } from 'react-native';

async function checkBatteryOptimization() {
  // This requires a custom native module check
  // or use a library like react-native-battery-optimization-check

  const isOptimized = await checkIfBatteryOptimizationEnabled();

  if (isOptimized) {
    Alert.alert(
      'Battery Optimization',
      'For reliable alarms, please disable battery optimization for this app.',
      [
        { text: 'Later', style: 'cancel' },
        {
          text: 'Settings',
          onPress: () => Linking.openSettings()
        },
      ]
    );
  }
}
```

---

## iOS

### iOS 16+ (AlarmKit) {#ios-16-alarmkit}

On iOS 16 and higher, the package uses native AlarmKit, which requires notification authorization.

**What you get:**
- ✅ True native alarms (just like Clock app)
- ✅ Persistent sound until dismissed
- ✅ Auto-launches app on dismiss
- ✅ Survives force-close and reboot
- ✅ No 30-second sound limit

**Required capabilities:**

Add to your `Info.plist`:

```xml
<key>UIBackgroundModes</key>
<array>
  <string>processing</string>
</array>

<key>NSUserNotificationsUsageDescription</key>
<string>We need notification permission to schedule alarms</string>
```

### iOS <16 (Notifications) {#ios-16-notifications}

On iOS versions before 16, the package falls back to local notifications.

**What you get:**
- ⚠️ Notification-based alarms
- ⚠️ Sound limited to 30 seconds
- ⚠️ Cannot auto-launch app on dismiss
- ⚠️ May not survive force-close

**Required capabilities:**

Add to your `Info.plist`:

```xml
<key>NSUserNotificationsUsageDescription</key>
<string>We need notification permission to schedule alarms</string>
```

### Setup {#setup-ios}

**1. Add capabilities to Info.plist** (see above sections)

**2. Request authorization in app:**

```typescript
import UltimateAlarm from 'react-native-ultimate-alarm';

async function setupAlarms() {
  const granted = await UltimateAlarm.requestPermissions();

  if (!granted) {
    Alert.alert(
      'Permission Required',
      'Please allow notifications to use alarms',
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Open Settings', onPress: () => Linking.openSettings() },
      ]
    );
    return;
  }

  // Permission granted
  await UltimateAlarm.scheduleAlarm({...});
}
```

**3. Check iOS version and capabilities:**

```typescript
import UltimateAlarm from 'react-native-ultimate-alarm';

async function checkCapabilities() {
  const caps = await UltimateAlarm.getCapabilities();

  if (caps.implementation === 'alarmkit') {
    console.log('✅ Using native iOS alarms!');
  } else if (caps.implementation === 'notification') {
    console.log('⚠️ Using notification fallback');
    console.log('Limitations:', caps.limitations);

    // Show user-friendly message
    Alert.alert(
      'Limited Alarm Features',
      'Your iOS version uses notification-based alarms with a 30-second sound limit. For full alarm features, upgrade to iOS 16 or later.'
    );
  }
}
```

---

## Requesting Permissions

### Programmatic Request

The package provides a simple API to request all necessary permissions:

```typescript
import UltimateAlarm from 'react-native-ultimate-alarm';

const granted = await UltimateAlarm.requestPermissions();

if (granted) {
  console.log('All permissions granted');
} else {
  console.log('Some permissions were denied');
}
```

### Check Current Status

Check if permissions are already granted before requesting:

```typescript
const hasPerms = await UltimateAlarm.hasPermissions();

if (!hasPerms) {
  // Request permissions
  await UltimateAlarm.requestPermissions();
}
```

### Best Practices

**1. Request permissions when needed:**

Don't request permissions on app launch. Request them when the user actually tries to schedule an alarm.

```typescript
async function handleScheduleAlarm() {
  // Check permissions first
  const hasPerms = await UltimateAlarm.hasPermissions();

  if (!hasPerms) {
    const granted = await UltimateAlarm.requestPermissions();
    if (!granted) {
      Alert.alert('Permission Required', 'Cannot schedule alarm without permissions');
      return;
    }
  }

  // Now schedule the alarm
  await UltimateAlarm.scheduleAlarm({...});
}
```

**2. Explain why you need permissions:**

Show a dialog explaining why before requesting:

```typescript
async function requestPermissionsWithExplanation() {
  Alert.alert(
    'Alarm Permissions',
    'To wake you up reliably, we need permission to schedule exact alarms on your device.',
    [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Grant Permission',
        onPress: async () => {
          await UltimateAlarm.requestPermissions();
        }
      },
    ]
  );
}
```

---

## Handling Permission Denial

### Temporary Denial

If user denies permission once, you can ask again later:

```typescript
const granted = await UltimateAlarm.requestPermissions();

if (!granted) {
  // Store that we asked
  await AsyncStorage.setItem('permission_asked', 'true');

  // Show message
  Alert.alert(
    'Permission Required',
    'Alarms need permission to work reliably. You can grant this later in Settings.'
  );
}
```

### Permanent Denial

If user denies permission permanently (especially on iOS), you need to direct them to Settings:

```typescript
import { Linking, Platform } from 'react-native';

async function openSettings() {
  if (Platform.OS === 'ios') {
    Linking.openURL('app-settings:');
  } else {
    Linking.openSettings();
  }
}

async function handlePermissionDenied() {
  Alert.alert(
    'Permission Required',
    'Alarms need exact alarm permission. Please enable it in Settings.',
    [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Open Settings', onPress: openSettings },
    ]
  );
}
```

### Graceful Degradation

Consider providing fallback functionality when permissions are denied:

```typescript
const granted = await UltimateAlarm.requestPermissions();

if (!granted) {
  // Offer alternative: reminder notifications (less reliable)
  Alert.alert(
    'Limited Functionality',
    'Without alarm permissions, we can only send reminder notifications (less reliable).',
    [
      { text: 'Use Reminders', onPress: () => setupReminders() },
      { text: 'Grant Permissions', onPress: () => UltimateAlarm.requestPermissions() },
    ]
  );
}
```

---

## Troubleshooting

### Android - Alarm not firing

**Possible causes:**
1. Battery optimization is enabled
2. Exact alarm permission denied
3. App force-closed by aggressive memory management

**Solutions:**
- Guide user to disable battery optimization
- Re-request permissions
- Test on different devices

### iOS - Notification-based alarm sound cuts off

**Cause:** iOS limits notification sounds to 30 seconds

**Solutions:**
- Upgrade to iOS 16+ for AlarmKit support
- Use shorter alarm sounds
- Set user expectations appropriately

### Both platforms - Permission request not showing

**Possible causes:**
1. Permission already denied permanently
2. Permission already granted
3. App lacks required manifest entries

**Solutions:**
- Check permission status with `hasPermissions()`
- Verify AndroidManifest.xml and Info.plist entries
- Direct user to Settings if permission denied permanently

---

## See Also

- [API.md](./API.md) - Complete API reference
- [CAPABILITIES.md](./CAPABILITIES.md) - Feature matrix across platforms
- [EXAMPLES.md](./EXAMPLES.md) - Usage examples
