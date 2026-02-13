# react-native-ultimate-alarm

🚨 **A unified alarm package for React Native with true alarm support on all platforms**

Stop juggling multiple packages and platform-specific code. `react-native-ultimate-alarm` automatically uses the best alarm implementation for each device:

- **Android**: Native `AlarmManager` with full-screen intents
- **iOS 16+**: Native `AlarmKit` framework
- **iOS < 16**: Notification-based fallback

## Features

- ✅ **True alarms** on Android and iOS 16+ (not just notifications)
- ✅ **Single API** across all platforms
- ✅ **Auto-detection** of capabilities
- ✅ **Graceful degradation** for older iOS versions
- ✅ **TypeScript** support
- ✅ **Expo** compatible
- ✅ **Snooze** functionality
- ✅ **Repeating alarms**
- ✅ **App launch** on alarm dismiss

## Installation

```bash
npm install react-native-ultimate-alarm
# or
yarn add react-native-ultimate-alarm
```

### iOS Setup

```bash
cd ios && pod install
```

Add to your `Info.plist`:
```xml
<key>NSAlarmKitUsageDescription</key>
<string>We use alarms to wake you up at your scheduled time</string>
```

For iOS 16+ AlarmKit support, add to your `app.json`:
```json
{
  "expo": {
    "ios": {
      "infoPlist": {
        "NSAlarmKitUsageDescription": "We use alarms to wake you up at your scheduled time"
      },
      "entitlements": {
        "com.apple.security.application-groups": [
          "group.$(PRODUCT_BUNDLE_IDENTIFIER).alarms"
        ]
      }
    }
  }
}
```

### Android Setup

No additional setup required! Permissions are handled automatically.

## Quick Start

```typescript
import UltimateAlarm from 'react-native-ultimate-alarm';

// 1. Request permissions
await UltimateAlarm.requestPermissions();

// 2. Schedule an alarm
await UltimateAlarm.scheduleAlarm({
  id: 'morning-alarm',
  time: new Date('2026-02-13T07:00:00'),
  title: 'Wake up!',
  message: 'Time to start your day',
  snooze: {
    enabled: true,
    duration: 300, // 5 minutes
  },
});

// 3. Check if app was launched by alarm
const launchPayload = await UltimateAlarm.getLaunchPayload();
if (launchPayload) {
  console.log('Launched by alarm:', launchPayload.alarmId);
}
```

## Usage

### Check Capabilities

```typescript
const capabilities = await UltimateAlarm.getCapabilities();

console.log('Platform:', capabilities.platform); // 'android' | 'ios'
console.log('Implementation:', capabilities.implementation); // 'alarmmanager' | 'alarmkit' | 'notification'
console.log('True alarm:', capabilities.features.truePersistentAlarm);
console.log('Auto-launch:', capabilities.features.autoLaunchApp);

if (capabilities.limitations) {
  console.log('Limitations:', capabilities.limitations);
  // Example on iOS < 16:
  // ["Sound plays for 30 seconds only", "User must tap notification to open app"]
}
```

### Schedule Alarms

```typescript
// One-time alarm
await UltimateAlarm.scheduleAlarm({
  id: 'wake-up',
  time: new Date('2026-02-13T07:00:00'),
  title: 'Good morning!',
  message: 'Time for your routine',
  soundName: 'alarm_sound', // Optional custom sound
  snooze: {
    enabled: true,
    duration: 300, // seconds
  },
  data: {
    routineId: '123',
    type: 'morning',
  },
});

// Repeating alarm (weekdays)
await UltimateAlarm.scheduleAlarm({
  id: 'weekday-alarm',
  time: new Date('2026-02-13T06:30:00'),
  title: 'Weekday Wake-up',
  repeat: {
    weekdays: [1, 2, 3, 4, 5], // Monday-Friday (0 = Sunday)
  },
});
```

### Handle Alarm Events

```typescript
// In your root component (App.tsx or _layout.tsx)
import { useEffect } from 'react';
import { useRouter } from 'expo-router';
import UltimateAlarm from 'react-native-ultimate-alarm';

export default function App() {
  const router = useRouter();

  useEffect(() => {
    // Check if launched by alarm
    async function checkLaunch() {
      const payload = await UltimateAlarm.getLaunchPayload();
      if (payload) {
        // App was launched by alarm - navigate to appropriate screen
        router.push(`/journal/dream?alarmId=${payload.alarmId}`);
      }
    }

    checkLaunch();

    // Listen for alarm events
    const dismissSubscription = UltimateAlarm.addEventListener('dismiss', (event) => {
      console.log('Alarm dismissed:', event.alarmId);
      console.log('Custom data:', event.data);
    });

    const snoozeSubscription = UltimateAlarm.addEventListener('snooze', (event) => {
      console.log('Alarm snoozed:', event.alarmId);
    });

    return () => {
      dismissSubscription.remove();
      snoozeSubscription.remove();
    };
  }, []);

  return <YourApp />;
}
```

### Cancel Alarms

```typescript
// Cancel specific alarm
await UltimateAlarm.cancelAlarm('morning-alarm');

// Cancel all alarms
await UltimateAlarm.cancelAllAlarms();
```

### List Scheduled Alarms

```typescript
const alarms = await UltimateAlarm.getAllAlarms();
console.log(`You have ${alarms.length} alarms scheduled`);

alarms.forEach((alarm) => {
  console.log(`${alarm.id}: ${alarm.title} at ${alarm.time}`);
});
```

### Snooze Alarms

```typescript
// Snooze for 5 minutes (default)
await UltimateAlarm.snoozeAlarm('morning-alarm');

// Snooze for custom duration
await UltimateAlarm.snoozeAlarm('morning-alarm', 10); // 10 minutes
```

## Platform Comparison

| Feature | Android | iOS 16+ | iOS < 16 |
|---------|---------|---------|----------|
| **Implementation** | AlarmManager | AlarmKit | Notifications |
| True persistent alarm | ✅ | ✅ | ❌ |
| Auto-launch app | ✅ | ✅ | ❌ |
| Bypass silent mode | ✅ | ✅ | ❌ |
| Persistent sound | ✅ | ✅ | ❌ (30s max) |
| Native snooze | ✅ | ✅ | ✅ |
| Repeating alarms | ✅ | ✅ | ❌ |
| Survives app kill | ✅ | ✅ | ✅ |
| Survives reboot | ✅ | ✅ | ✅ |

## API Reference

### `UltimateAlarm`

#### `getCapabilities(): Promise<AlarmCapabilities>`
Returns platform-specific alarm capabilities.

#### `requestPermissions(): Promise<boolean>`
Request necessary permissions. Returns `true` if granted.

#### `hasPermissions(): Promise<boolean>`
Check if permissions are already granted.

#### `scheduleAlarm(config: AlarmConfig): Promise<void>`
Schedule a new alarm.

#### `cancelAlarm(alarmId: string): Promise<void>`
Cancel a specific alarm.

#### `cancelAllAlarms(): Promise<void>`
Cancel all scheduled alarms.

#### `getAllAlarms(): Promise<AlarmConfig[]>`
Get all scheduled alarms.

#### `isAlarmScheduled(alarmId: string): Promise<boolean>`
Check if a specific alarm is scheduled.

#### `snoozeAlarm(alarmId: string, minutes?: number): Promise<void>`
Snooze an alarm (default: 5 minutes).

#### `getLaunchPayload(): Promise<AlarmEvent | null>`
Get payload if app was launched by alarm. Returns `null` if not launched by alarm.

⚠️ **Important**: Call this early in your app lifecycle. The payload is cleared after the first call.

#### `addEventListener(event, callback): { remove: () => void }`
Add listener for alarm events (`'dismiss'` or `'snooze'`).

#### `removeEventListener(event, callback): void`
Remove event listener.

## TypeScript Types

```typescript
interface AlarmConfig {
  id: string;
  time: Date;
  title: string;
  message?: string;
  soundName?: string;
  snooze?: {
    enabled: boolean;
    duration?: number;
  };
  repeat?: {
    weekdays: number[]; // 0-6 (Sunday-Saturday)
  };
  data?: Record<string, any>;
}

interface AlarmCapabilities {
  platform: 'android' | 'ios';
  iosVersion?: number;
  implementation: 'alarmmanager' | 'alarmkit' | 'notification';
  features: {
    truePersistentAlarm: boolean;
    autoLaunchApp: boolean;
    bypassSilentMode: boolean;
    persistentSound: boolean;
    nativeSnooze: boolean;
    repeatAlarms: boolean;
  };
  limitations?: string[];
}

interface AlarmEvent {
  alarmId: string;
  action: 'dismiss' | 'snooze';
  data?: Record<string, any>;
  timestamp: Date;
}
```

## Example App

See the [`example/`](./example) directory for a full working example.

## Troubleshooting

### Android

**Alarms not firing:**
- Check that "Battery Optimization" is disabled for your app
- Check that "Autostart" permission is granted (on Xiaomi/Huawei)
- Verify exact alarm permission is granted (Android 12+)

**Full-screen intent not showing:**
- Check that "Display over other apps" permission is granted
- On Android 14+, full-screen intents are restricted to alarm/calling apps

### iOS

**AlarmKit not available (iOS 16+):**
- Verify you added the AlarmKit usage description to Info.plist
- Verify App Groups entitlement is configured
- Rebuild the app after adding entitlements

**Notifications not showing (iOS < 16):**
- Request notification permissions before scheduling
- Check notification settings in iOS Settings app

## Contributing

Contributions are welcome! Please read our [Contributing Guide](./CONTRIBUTING.md).

## License

MIT

## Credits

Built on top of:
- [react-native-alarmageddon](https://github.com/joaoGabriel55/react-native-alarmageddon) - Android alarm implementation
- [expo-alarm-kit](https://github.com/nickdeupree/expo-alarm-kit) - iOS AlarmKit implementation

Special thanks to the authors of these packages for their excellent work.

## Support

- 🐛 [Report a bug](https://github.com/kylemichaelreaves/react-native-ultimate-alarm/issues)
- 💡 [Request a feature](https://github.com/kylemichaelreaves/react-native-ultimate-alarm/issues)
- 📖 [Read the docs](https://github.com/kylemichaelreaves/react-native-ultimate-alarm/wiki)
