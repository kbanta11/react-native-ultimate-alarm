# API Reference

Complete API documentation for `react-native-ultimate-alarm`.

## Table of Contents

- [Installation](#installation)
- [Methods](#methods)
  - [getCapabilities()](#getcapabilities)
  - [requestPermissions()](#requestpermissions)
  - [hasPermissions()](#haspermissions)
  - [scheduleAlarm()](#schedulealarm)
  - [cancelAlarm()](#cancelalarm)
  - [cancelAllAlarms()](#cancelallalarms)
  - [getAllAlarms()](#getallalarms)
  - [isAlarmScheduled()](#isalarmscheduled)
  - [snoozeAlarm()](#snoozealarm)
  - [getLaunchPayload()](#getlaunchpayload)
  - [addEventListener()](#addeventlistener)
- [Types](#types)

---

## Installation

```bash
npm install react-native-ultimate-alarm
```

Or with yarn:

```bash
yarn add react-native-ultimate-alarm
```

For Expo projects:

```bash
npx expo install react-native-ultimate-alarm
```

---

## Methods

### getCapabilities()

Get platform-specific alarm capabilities to understand what features are available on the current device.

**Signature:**
```typescript
getCapabilities(): Promise<AlarmCapabilities>
```

**Returns:**
```typescript
interface AlarmCapabilities {
  platform: 'android' | 'ios';
  implementation: 'alarmmanager' | 'alarmkit' | 'notification';
  features: {
    truePersistentAlarm: boolean;      // True alarm that survives force-close
    autoLaunchApp: boolean;             // Auto-launches app on dismiss
    bypassSilentMode: boolean;          // Plays even when phone is silenced
    persistentSound: boolean;           // Sound plays until dismissed
  };
  limitations: string[];                // Array of limitation descriptions
}
```

**Example:**
```typescript
import UltimateAlarm from 'react-native-ultimate-alarm';

const capabilities = await UltimateAlarm.getCapabilities();

console.log(`Running on ${capabilities.platform}`);
console.log(`Using ${capabilities.implementation} implementation`);

if (capabilities.features.truePersistentAlarm) {
  console.log('✅ True alarms supported!');
} else {
  console.log('⚠️ Limitations:', capabilities.limitations);
}
```

**Platform differences:**
- **Android**: Returns `alarmmanager` with all features enabled
- **iOS 16+**: Returns `alarmkit` with all features enabled (if AlarmKit is available)
- **iOS <16**: Returns `notification` with limited features

---

### requestPermissions()

Request necessary permissions for alarm scheduling.

**Signature:**
```typescript
requestPermissions(): Promise<boolean>
```

**Returns:**
- `true` if all required permissions are granted
- `false` if any required permission is denied

**Example:**
```typescript
const granted = await UltimateAlarm.requestPermissions();

if (!granted) {
  Alert.alert(
    'Permissions Required',
    'Please grant alarm permissions to use this feature'
  );
}
```

**Platform differences:**
- **Android 12+**: Requests `SCHEDULE_EXACT_ALARM` permission
- **iOS**: Requests notification permissions (or AlarmKit on iOS 16+)

**See also:** [PERMISSIONS.md](./PERMISSIONS.md) for detailed permission requirements.

---

### hasPermissions()

Check if all required permissions are currently granted.

**Signature:**
```typescript
hasPermissions(): Promise<boolean>
```

**Returns:**
- `true` if all permissions are granted
- `false` if any permission is missing

**Example:**
```typescript
const hasPerms = await UltimateAlarm.hasPermissions();

if (!hasPerms) {
  // Request permissions
  await UltimateAlarm.requestPermissions();
}
```

---

### scheduleAlarm()

Schedule a new alarm.

**Signature:**
```typescript
scheduleAlarm(config: AlarmConfig): Promise<void>
```

**Parameters:**

```typescript
interface AlarmConfig {
  id: string;                          // Unique alarm identifier
  time: Date;                          // When the alarm should fire
  title: string;                       // Alarm title
  message: string;                     // Alarm message/body
  sound?: string;                      // Custom sound (optional)
  snooze?: SnoozeConfig;              // Snooze configuration (optional)
  repeat?: RepeatConfig;              // Repeat configuration (optional)
  data?: Record<string, any>;         // Custom data payload (optional)
}

interface SnoozeConfig {
  enabled: boolean;                    // Enable snooze button
  duration?: number;                   // Snooze duration in seconds (default: 300)
}

interface RepeatConfig {
  weekdays?: number[];                 // Days to repeat (0=Sunday, 6=Saturday)
}
```

**Example - One-time alarm:**
```typescript
await UltimateAlarm.scheduleAlarm({
  id: 'morning-alarm',
  time: new Date('2026-02-14T07:00:00'),
  title: 'Wake up!',
  message: 'Time to start your day',
  snooze: {
    enabled: true,
    duration: 300, // 5 minutes
  },
});
```

**Example - Repeating alarm:**
```typescript
const alarmTime = new Date();
alarmTime.setHours(7, 0, 0, 0);

await UltimateAlarm.scheduleAlarm({
  id: 'weekday-alarm',
  time: alarmTime,
  title: 'Weekday Wake-up',
  message: 'Time for work!',
  repeat: {
    weekdays: [1, 2, 3, 4, 5], // Monday-Friday
  },
  snooze: {
    enabled: true,
    duration: 600, // 10 minutes
  },
});
```

**Example - With custom data:**
```typescript
await UltimateAlarm.scheduleAlarm({
  id: 'reminder-123',
  time: new Date(Date.now() + 3600000), // 1 hour from now
  title: 'Task Reminder',
  message: 'Complete your task',
  data: {
    taskId: '123',
    category: 'work',
    priority: 'high',
  },
});
```

**Throws:**
- `Error` if alarm time is in the past
- `Error` if permissions are not granted

**Platform differences:**
- **Android**: Full support for all features
- **iOS 16+**: Full support via AlarmKit
- **iOS <16**: Repeating alarms work, but sound limited to 30 seconds

---

### cancelAlarm()

Cancel a specific alarm by ID.

**Signature:**
```typescript
cancelAlarm(alarmId: string): Promise<void>
```

**Parameters:**
- `alarmId` - The unique ID of the alarm to cancel

**Example:**
```typescript
await UltimateAlarm.cancelAlarm('morning-alarm');
console.log('Alarm cancelled');
```

---

### cancelAllAlarms()

Cancel all scheduled alarms.

**Signature:**
```typescript
cancelAllAlarms(): Promise<void>
```

**Example:**
```typescript
await UltimateAlarm.cancelAllAlarms();
console.log('All alarms cancelled');
```

---

### getAllAlarms()

Get all currently scheduled alarms.

**Signature:**
```typescript
getAllAlarms(): Promise<AlarmConfig[]>
```

**Returns:**
Array of `AlarmConfig` objects representing all scheduled alarms.

**Example:**
```typescript
const alarms = await UltimateAlarm.getAllAlarms();

console.log(`You have ${alarms.length} alarms scheduled`);

alarms.forEach(alarm => {
  console.log(`${alarm.title} at ${alarm.time}`);
});
```

---

### isAlarmScheduled()

Check if a specific alarm is currently scheduled.

**Signature:**
```typescript
isAlarmScheduled(alarmId: string): Promise<boolean>
```

**Parameters:**
- `alarmId` - The unique ID of the alarm to check

**Returns:**
- `true` if alarm is scheduled
- `false` if alarm is not found

**Example:**
```typescript
const isScheduled = await UltimateAlarm.isAlarmScheduled('morning-alarm');

if (!isScheduled) {
  // Re-schedule the alarm
  await scheduleAlarm({...});
}
```

---

### snoozeAlarm()

Snooze an alarm (reschedule it for X minutes later).

**Signature:**
```typescript
snoozeAlarm(alarmId: string, minutes?: number): Promise<void>
```

**Parameters:**
- `alarmId` - The unique ID of the alarm to snooze
- `minutes` - How many minutes to snooze (default: 5)

**Example:**
```typescript
// Snooze for 10 minutes
await UltimateAlarm.snoozeAlarm('morning-alarm', 10);

// Snooze for default 5 minutes
await UltimateAlarm.snoozeAlarm('morning-alarm');
```

---

### getLaunchPayload()

Get the payload if the app was launched by an alarm.

**IMPORTANT:** Call this early in your app lifecycle (e.g., in `App.tsx` or `_layout.tsx`). The payload is cleared after the first call.

**Signature:**
```typescript
getLaunchPayload(): Promise<AlarmEvent | null>
```

**Returns:**

```typescript
interface AlarmEvent {
  alarmId: string;                     // ID of the alarm that triggered
  action: 'dismiss' | 'snooze';       // What action was taken
  timestamp: Date;                     // When the event occurred
  data?: Record<string, any>;         // Custom data from alarm config
}
```

**Example:**
```typescript
import { useEffect } from 'react';
import { useRouter } from 'expo-router';
import UltimateAlarm from 'react-native-ultimate-alarm';

export default function App() {
  const router = useRouter();

  useEffect(() => {
    async function checkLaunch() {
      const launchPayload = await UltimateAlarm.getLaunchPayload();

      if (launchPayload) {
        console.log('App launched by alarm:', launchPayload.alarmId);

        // Navigate based on alarm data
        if (launchPayload.data?.routineId) {
          router.push(`/routine/${launchPayload.data.routineId}`);
        }
      }
    }

    checkLaunch();
  }, []);

  // ... rest of app
}
```

**Platform availability:**
- **Android**: Always available
- **iOS 16+**: Available when using AlarmKit
- **iOS <16**: Not available (notification-based)

---

### addEventListener()

Add an event listener for alarm events.

**Signature:**
```typescript
addEventListener(
  event: 'dismiss' | 'snooze',
  callback: (event: AlarmEvent) => void
): { remove: () => void }
```

**Parameters:**
- `event` - Event type to listen for (`'dismiss'` or `'snooze'`)
- `callback` - Function to call when event occurs

**Returns:**
Subscription object with `remove()` method to unsubscribe.

**Example:**
```typescript
import { useEffect } from 'react';
import UltimateAlarm from 'react-native-ultimate-alarm';

function AlarmManager() {
  useEffect(() => {
    // Listen for alarm dismissals
    const dismissSub = UltimateAlarm.addEventListener('dismiss', (event) => {
      console.log('Alarm dismissed:', event.alarmId);
      // Track analytics, update UI, etc.
    });

    // Listen for alarm snoozes
    const snoozeSub = UltimateAlarm.addEventListener('snooze', (event) => {
      console.log('Alarm snoozed:', event.alarmId);
      // Update UI to show snoozed state
    });

    // Cleanup on unmount
    return () => {
      dismissSub.remove();
      snoozeSub.remove();
    };
  }, []);

  // ... component code
}
```

**Platform availability:**
- **Android**: Full support
- **iOS 16+**: Full support via AlarmKit
- **iOS <16**: Limited support (notification actions)

---

## Types

### AlarmCapabilities

```typescript
interface AlarmCapabilities {
  platform: 'android' | 'ios';
  implementation: 'alarmmanager' | 'alarmkit' | 'notification';
  features: AlarmFeatures;
  limitations: string[];
}
```

### AlarmFeatures

```typescript
interface AlarmFeatures {
  truePersistentAlarm: boolean;
  autoLaunchApp: boolean;
  bypassSilentMode: boolean;
  persistentSound: boolean;
}
```

### AlarmConfig

```typescript
interface AlarmConfig {
  id: string;
  time: Date;
  title: string;
  message: string;
  sound?: string;
  snooze?: SnoozeConfig;
  repeat?: RepeatConfig;
  data?: Record<string, any>;
}
```

### SnoozeConfig

```typescript
interface SnoozeConfig {
  enabled: boolean;
  duration?: number; // seconds, default: 300
}
```

### RepeatConfig

```typescript
interface RepeatConfig {
  weekdays?: number[]; // 0 = Sunday, 6 = Saturday
}
```

### AlarmEvent

```typescript
interface AlarmEvent {
  alarmId: string;
  action: 'dismiss' | 'snooze';
  timestamp: Date;
  data?: Record<string, any>;
}
```

---

## See Also

- [PERMISSIONS.md](./PERMISSIONS.md) - Permission requirements and setup
- [CAPABILITIES.md](./CAPABILITIES.md) - Feature matrix across platforms
- [EXAMPLES.md](./EXAMPLES.md) - Usage examples and best practices
