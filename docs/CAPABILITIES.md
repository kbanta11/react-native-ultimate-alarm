# Capabilities Guide

Understanding alarm capabilities across different platforms and how to handle feature differences.

## Table of Contents

- [Overview](#overview)
- [Feature Matrix](#feature-matrix)
- [Implementation Details](#implementation-details)
- [Detecting Capabilities](#detecting-capabilities)
- [Handling Degraded Experiences](#handling-degraded-experiences)
- [User Messaging](#user-messaging)
- [Best Practices](#best-practices)

---

## Overview

`react-native-ultimate-alarm` automatically selects the best alarm implementation for each platform:

| Platform | Implementation | Experience |
|----------|---------------|------------|
| **Android** | AlarmManager | ⭐⭐⭐⭐⭐ Best |
| **iOS 16+** | AlarmKit | ⭐⭐⭐⭐⭐ Best |
| **iOS <16** | Notifications | ⭐⭐⭐ Limited |

---

## Feature Matrix

### Core Features

| Feature | Android | iOS 16+ (AlarmKit) | iOS <16 (Notifications) |
|---------|---------|-------------------|------------------------|
| **True Persistent Alarm** | ✅ Yes | ✅ Yes | ❌ No |
| **Auto-Launch App** | ✅ Yes | ✅ Yes | ❌ No |
| **Bypass Silent Mode** | ✅ Yes | ✅ Yes | ❌ No |
| **Persistent Sound** | ✅ Yes | ✅ Yes | ⚠️ 30s limit |
| **Survives Force-Close** | ✅ Yes | ✅ Yes | ⚠️ Limited |
| **Survives Reboot** | ✅ Yes | ✅ Yes | ⚠️ Limited |
| **Custom Sounds** | ✅ Yes | ✅ Yes | ⚠️ 30s limit |
| **Snooze Support** | ✅ Yes | ✅ Yes | ✅ Yes |
| **Repeating Alarms** | ✅ Yes | ✅ Yes | ✅ Yes |
| **Lock Screen Display** | ✅ Yes | ✅ Yes | ✅ Yes |

### Platform-Specific Features

| Feature | Android | iOS 16+ | iOS <16 |
|---------|---------|---------|---------|
| **Exact Timing** | ±1s | ±1s | ±5s |
| **Volume Override** | ✅ Max volume | ✅ Alarm volume | ❌ System volume |
| **Vibration Control** | ✅ Custom patterns | ✅ Native | ⚠️ Limited |
| **Gradual Volume** | ⚠️ Manual | ✅ Native | ❌ No |
| **Smart Wake** | ❌ No | ⚠️ Planned | ❌ No |

### Limitations by Platform

| Platform | Limitations |
|----------|-------------|
| **Android** | • Requires battery optimization exemption<br>• May be killed by aggressive OEMs<br>• Users must grant SCHEDULE_EXACT_ALARM (Android 12+) |
| **iOS 16+** | • Requires notification authorization<br>• Limited to iOS 16.0+<br>• User can delete alarms from Clock app |
| **iOS <16** | • Notification-based (not true alarm)<br>• Sound limited to 30 seconds<br>• Cannot bypass silent mode<br>• Cannot auto-launch app<br>• May not survive force-close |

---

## Implementation Details

### Android (AlarmManager)

**How it works:**
- Uses native `AlarmManager.setExactAndAllowWhileIdle()`
- Launches foreground service on alarm trigger
- Plays audio with `STREAM_ALARM` (bypasses silent mode)
- Shows full-screen intent on lock screen
- Persists alarms via `SharedPreferences`
- Re-registers alarms after boot via `BootReceiver`

**Advantages:**
- ✅ True alarm system
- ✅ Plays indefinitely until dismissed
- ✅ Auto-launches app
- ✅ Maximum reliability

**Challenges:**
- ⚠️ Battery optimization on some devices
- ⚠️ Aggressive task killers on Xiaomi/Huawei
- ⚠️ User must grant exact alarm permission (Android 12+)

### iOS 16+ (AlarmKit)

**How it works:**
- Uses native `AlarmManager.shared` (iOS AlarmKit)
- Creates native alarm entries (visible in Clock app)
- Integrates with Live Activities for snooze/dismiss
- Stores launch payloads in App Group
- Handles repeating alarms natively

**Advantages:**
- ✅ Native iOS alarm experience
- ✅ Same reliability as Clock app
- ✅ Beautiful native UI
- ✅ Auto-launches app on dismiss

**Challenges:**
- ⚠️ iOS 16.0+ only
- ⚠️ User can delete alarms from Clock app
- ⚠️ Requires notification authorization

### iOS <16 (Notifications)

**How it works:**
- Uses `UNUserNotificationCenter`
- Schedules local notifications with custom sounds
- Provides snooze/dismiss notification actions
- Stores alarm data in `UserDefaults`

**Advantages:**
- ✅ Works on older iOS versions
- ✅ No special entitlements needed
- ✅ Repeating alarms supported

**Limitations:**
- ❌ Not a true alarm (just a notification)
- ❌ Sound limited to 30 seconds
- ❌ Respects device silent mode
- ❌ Cannot auto-launch app
- ❌ May not survive force-close

---

## Detecting Capabilities

### Programmatic Detection

Use `getCapabilities()` to detect what's available:

```typescript
import UltimateAlarm from 'react-native-ultimate-alarm';

const capabilities = await UltimateAlarm.getCapabilities();

console.log('Platform:', capabilities.platform);
console.log('Implementation:', capabilities.implementation);
console.log('Features:', capabilities.features);
console.log('Limitations:', capabilities.limitations);
```

### Example Response - Android

```json
{
  "platform": "android",
  "implementation": "alarmmanager",
  "features": {
    "truePersistentAlarm": true,
    "autoLaunchApp": true,
    "bypassSilentMode": true,
    "persistentSound": true
  },
  "limitations": []
}
```

### Example Response - iOS 16+

```json
{
  "platform": "ios",
  "implementation": "alarmkit",
  "features": {
    "truePersistentAlarm": true,
    "autoLaunchApp": true,
    "bypassSilentMode": true,
    "persistentSound": true
  },
  "limitations": []
}
```

### Example Response - iOS <16

```json
{
  "platform": "ios",
  "implementation": "notification",
  "features": {
    "truePersistentAlarm": false,
    "autoLaunchApp": false,
    "bypassSilentMode": false,
    "persistentSound": false
  },
  "limitations": [
    "Notification-based alarm (not true alarm)",
    "Sound limited to 30 seconds",
    "Cannot bypass silent mode",
    "Cannot auto-launch app"
  ]
}
```

---

## Handling Degraded Experiences

### Check Capabilities Before Scheduling

```typescript
async function scheduleAlarmSafely() {
  const caps = await UltimateAlarm.getCapabilities();

  if (!caps.features.truePersistentAlarm) {
    // Warn user about limitations
    Alert.alert(
      'Limited Alarm Features',
      'Your device uses notification-based alarms with a 30-second sound limit. For best experience, consider upgrading to iOS 16 or using an Android device.',
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Continue Anyway', onPress: () => scheduleAlarm() },
      ]
    );
    return;
  }

  // Full features available
  await scheduleAlarm();
}
```

### Feature-Specific Checks

```typescript
const caps = await UltimateAlarm.getCapabilities();

// Check if alarm will auto-launch app
if (caps.features.autoLaunchApp) {
  console.log('App will auto-launch on dismiss');
} else {
  console.log('User must manually open app');
}

// Check if sound will play persistently
if (caps.features.persistentSound) {
  console.log('Sound plays until dismissed');
} else {
  console.log('Sound limited to 30 seconds');
}

// Check if alarm bypasses silent mode
if (caps.features.bypassSilentMode) {
  console.log('Will ring even on silent');
} else {
  console.log('Respects device silent mode');
}
```

### Adaptive UI

Show different UI based on capabilities:

```typescript
function AlarmSettings() {
  const [capabilities, setCapabilities] = useState(null);

  useEffect(() => {
    async function loadCapabilities() {
      const caps = await UltimateAlarm.getCapabilities();
      setCapabilities(caps);
    }
    loadCapabilities();
  }, []);

  if (!capabilities) return <ActivityIndicator />;

  return (
    <View>
      <Text>Alarm Implementation: {capabilities.implementation}</Text>

      {/* Show warning for limited features */}
      {!capabilities.features.truePersistentAlarm && (
        <View style={styles.warning}>
          <Text style={styles.warningText}>
            ⚠️ Limited Features
          </Text>
          <Text>
            Your device uses notification-based alarms with these limitations:
          </Text>
          {capabilities.limitations.map((limitation, i) => (
            <Text key={i}>• {limitation}</Text>
          ))}
        </View>
      )}

      {/* Only show certain options on full-featured platforms */}
      {capabilities.features.persistentSound && (
        <View>
          <Text>Gradual Volume Increase</Text>
          <Switch value={gradualVolume} onValueChange={setGradualVolume} />
        </View>
      )}
    </View>
  );
}
```

---

## User Messaging

### Recommended Messages

**For iOS <16 users:**

```typescript
if (caps.implementation === 'notification') {
  Alert.alert(
    'Alarm Limitations',
    'Your iOS version supports alarm notifications, but with a 30-second sound limit. For the best alarm experience with persistent sounds and auto-launch, please upgrade to iOS 16 or later.',
    [{ text: 'OK' }]
  );
}
```

**For Android users with battery optimization:**

```typescript
Alert.alert(
  'Improve Alarm Reliability',
  'For best alarm reliability, please disable battery optimization for this app. This ensures alarms work even when the app is closed.',
  [
    { text: 'Later', style: 'cancel' },
    { text: 'Settings', onPress: () => Linking.openSettings() },
  ]
);
```

**First-time alarm setup:**

```typescript
const caps = await UltimateAlarm.getCapabilities();

if (caps.features.truePersistentAlarm) {
  Alert.alert(
    '✅ Full Alarm Support',
    `Your device supports true alarms that:\n\n• Play until you dismiss them\n• Work even when app is closed\n• Bypass silent mode\n• Auto-launch the app\n\nYou're all set!`,
    [{ text: 'Great!' }]
  );
}
```

---

## Best Practices

### 1. Always Check Capabilities

Don't assume features are available:

```typescript
// ❌ Bad - assumes feature exists
await UltimateAlarm.scheduleAlarm({...});

// ✅ Good - checks capabilities first
const caps = await UltimateAlarm.getCapabilities();
if (!caps.features.truePersistentAlarm) {
  warnUserAboutLimitations();
}
await UltimateAlarm.scheduleAlarm({...});
```

### 2. Set User Expectations

Be transparent about limitations:

```typescript
async function showCapabilities() {
  const caps = await UltimateAlarm.getCapabilities();

  const message = caps.features.truePersistentAlarm
    ? '✅ Your device supports full alarm features!'
    : `⚠️ Limited alarm features:\n\n${caps.limitations.join('\n')}`;

  Alert.alert('Alarm Capabilities', message);
}
```

### 3. Graceful Degradation

Provide alternative features when full features aren't available:

```typescript
const caps = await UltimateAlarm.getCapabilities();

if (!caps.features.persistentSound) {
  // iOS <16 - sound limited to 30 seconds
  // Suggest shorter alarm sounds
  recommendShortSounds();
}

if (!caps.features.autoLaunchApp) {
  // Cannot auto-launch
  // Show notification to open app instead
  scheduleReminderNotification();
}
```

### 4. Test on Multiple Platforms

Test your alarm implementation on:
- ✅ Android 12+ (exact alarm permissions)
- ✅ iOS 16+ (AlarmKit)
- ✅ iOS 15 or earlier (notification fallback)

### 5. Handle Edge Cases

```typescript
async function scheduleAlarm(config) {
  try {
    // Check permissions
    const hasPerms = await UltimateAlarm.hasPermissions();
    if (!hasPerms) {
      const granted = await UltimateAlarm.requestPermissions();
      if (!granted) {
        throw new Error('Permissions required');
      }
    }

    // Check capabilities
    const caps = await UltimateAlarm.getCapabilities();
    if (!caps.features.truePersistentAlarm) {
      // Warn user, but allow them to proceed
      await warnAboutLimitations();
    }

    // Schedule alarm
    await UltimateAlarm.scheduleAlarm(config);

  } catch (error) {
    console.error('Failed to schedule alarm:', error);
    Alert.alert('Error', 'Failed to schedule alarm. Please try again.');
  }
}
```

---

## FAQ

**Q: Why doesn't iOS <16 support true alarms?**

A: iOS didn't have a public AlarmKit API until iOS 16. Before that, only Apple's Clock app could create true alarms. We use notifications as the next best option.

**Q: Can I force use AlarmKit on iOS 15?**

A: No, AlarmKit is only available on iOS 16+. The package automatically detects iOS version and uses the best available implementation.

**Q: Why do Android alarms sometimes not fire?**

A: Android's battery optimization and aggressive task killers can prevent alarms. Guide users to disable battery optimization for your app.

**Q: Will alarms work in airplane mode?**

A: Yes! All alarm implementations work offline and in airplane mode.

**Q: Can I use longer sounds on iOS <16?**

A: No, iOS limits notification sounds to 30 seconds. This is an OS limitation. Upgrade to iOS 16+ for unlimited sound duration.

---

## See Also

- [API.md](./API.md) - Complete API reference
- [PERMISSIONS.md](./PERMISSIONS.md) - Permission requirements
- [EXAMPLES.md](./EXAMPLES.md) - Usage examples
