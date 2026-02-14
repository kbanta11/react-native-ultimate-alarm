# Usage Examples

Practical examples for common alarm scenarios using `react-native-ultimate-alarm`.

## Table of Contents

- [Basic Setup](#basic-setup)
- [Simple One-Time Alarm](#simple-one-time-alarm)
- [Repeating Weekday Alarm](#repeating-weekday-alarm)
- [Alarm with Custom Data](#alarm-with-custom-data)
- [Handling App Launch from Alarm](#handling-app-launch-from-alarm)
- [Event Listeners](#event-listeners)
- [Managing Multiple Alarms](#managing-multiple-alarms)
- [Error Handling](#error-handling)
- [Custom Alarm Manager Component](#custom-alarm-manager-component)
- [Best Practices](#best-practices)

---

## Basic Setup

### Initialize in App Root

```typescript
// App.tsx or _layout.tsx
import { useEffect } from 'react';
import { Alert } from 'react-native';
import UltimateAlarm from 'react-native-ultimate-alarm';

export default function App() {
  useEffect(() => {
    async function init() {
      // Check and request permissions
      const hasPerms = await UltimateAlarm.hasPermissions();
      if (!hasPerms) {
        const granted = await UltimateAlarm.requestPermissions();
        if (!granted) {
          Alert.alert(
            'Permissions Required',
            'Alarm functionality requires permissions to work properly.'
          );
        }
      }

      // Check capabilities
      const caps = await UltimateAlarm.getCapabilities();
      console.log('Alarm implementation:', caps.implementation);

      if (!caps.features.truePersistentAlarm) {
        console.warn('Limited alarm features:', caps.limitations);
      }

      // Check if app was launched by alarm
      const launchPayload = await UltimateAlarm.getLaunchPayload();
      if (launchPayload) {
        console.log('Launched by alarm:', launchPayload.alarmId);
        // Handle alarm launch (navigate, show screen, etc.)
      }
    }

    init();
  }, []);

  return (
    // Your app components
  );
}
```

---

## Simple One-Time Alarm

Schedule an alarm to fire once at a specific time:

```typescript
import { useState } from 'react';
import { View, Button, Alert } from 'react-native';
import UltimateAlarm from 'react-native-ultimate-alarm';

export default function QuickAlarmScreen() {
  const [alarmId, setAlarmId] = useState<string | null>(null);

  async function scheduleQuickAlarm() {
    try {
      // Check permissions
      const hasPerms = await UltimateAlarm.hasPermissions();
      if (!hasPerms) {
        const granted = await UltimateAlarm.requestPermissions();
        if (!granted) {
          Alert.alert('Error', 'Permissions required');
          return;
        }
      }

      // Schedule alarm for 1 minute from now
      const alarmTime = new Date();
      alarmTime.setMinutes(alarmTime.getMinutes() + 1);

      const id = `alarm-${Date.now()}`;

      await UltimateAlarm.scheduleAlarm({
        id,
        time: alarmTime,
        title: 'Quick Alarm',
        message: 'Your alarm is ringing!',
        snooze: {
          enabled: true,
          duration: 300, // 5 minutes
        },
      });

      setAlarmId(id);
      Alert.alert('Success', 'Alarm scheduled for 1 minute from now');
    } catch (error) {
      Alert.alert('Error', error.message);
    }
  }

  async function cancelAlarm() {
    if (!alarmId) return;

    try {
      await UltimateAlarm.cancelAlarm(alarmId);
      setAlarmId(null);
      Alert.alert('Cancelled', 'Alarm has been cancelled');
    } catch (error) {
      Alert.alert('Error', error.message);
    }
  }

  return (
    <View style={{ padding: 20 }}>
      <Button
        title="Set Alarm (1 min)"
        onPress={scheduleQuickAlarm}
        disabled={alarmId !== null}
      />
      {alarmId && (
        <Button
          title="Cancel Alarm"
          onPress={cancelAlarm}
          color="red"
        />
      )}
    </View>
  );
}
```

---

## Repeating Weekday Alarm

Schedule an alarm that repeats on specific weekdays:

```typescript
import { useState } from 'react';
import { View, Text, Button, Alert } from 'react-native';
import UltimateAlarm from 'react-native-ultimate-alarm';

export default function WeekdayAlarmScreen() {
  async function scheduleWeekdayAlarm() {
    try {
      // Set alarm for 7:00 AM on weekdays
      const alarmTime = new Date();
      alarmTime.setHours(7, 0, 0, 0);

      await UltimateAlarm.scheduleAlarm({
        id: 'weekday-morning-alarm',
        time: alarmTime,
        title: 'Wake Up!',
        message: 'Time to start your day',
        repeat: {
          weekdays: [1, 2, 3, 4, 5], // Monday-Friday
        },
        snooze: {
          enabled: true,
          duration: 600, // 10 minutes
        },
        data: {
          type: 'morning-routine',
          priority: 'high',
        },
      });

      Alert.alert('Success', 'Weekday alarm set for 7:00 AM');
    } catch (error) {
      Alert.alert('Error', error.message);
    }
  }

  async function scheduleWeekendAlarm() {
    try {
      // Set alarm for 9:00 AM on weekends
      const alarmTime = new Date();
      alarmTime.setHours(9, 0, 0, 0);

      await UltimateAlarm.scheduleAlarm({
        id: 'weekend-morning-alarm',
        time: alarmTime,
        title: 'Good Morning!',
        message: 'Enjoy your weekend',
        repeat: {
          weekdays: [0, 6], // Sunday and Saturday
        },
        snooze: {
          enabled: true,
          duration: 900, // 15 minutes
        },
      });

      Alert.alert('Success', 'Weekend alarm set for 9:00 AM');
    } catch (error) {
      Alert.alert('Error', error.message);
    }
  }

  return (
    <View style={{ padding: 20 }}>
      <Text style={{ fontSize: 20, marginBottom: 20 }}>
        Repeating Alarms
      </Text>

      <Button
        title="Set Weekday Alarm (7 AM)"
        onPress={scheduleWeekdayAlarm}
      />

      <View style={{ height: 10 }} />

      <Button
        title="Set Weekend Alarm (9 AM)"
        onPress={scheduleWeekendAlarm}
      />
    </View>
  );
}
```

---

## Alarm with Custom Data

Pass custom data that you can access when the alarm fires:

```typescript
import UltimateAlarm from 'react-native-ultimate-alarm';

// Schedule alarm with custom data
async function scheduleTaskReminder(taskId: string, taskName: string, dueDate: Date) {
  await UltimateAlarm.scheduleAlarm({
    id: `task-${taskId}`,
    time: dueDate,
    title: 'Task Reminder',
    message: `Time to: ${taskName}`,
    data: {
      type: 'task-reminder',
      taskId,
      taskName,
      category: 'work',
      priority: 'high',
    },
  });
}

// Handle alarm when app launches
async function handleAlarmLaunch() {
  const launchPayload = await UltimateAlarm.getLaunchPayload();

  if (launchPayload && launchPayload.data) {
    const { type, taskId, taskName } = launchPayload.data;

    if (type === 'task-reminder') {
      // Navigate to task screen
      navigation.navigate('Task', { taskId });

      // Show notification
      Alert.alert('Task Due', taskName);
    }
  }
}

// Usage
await scheduleTaskReminder(
  '123',
  'Finish project documentation',
  new Date('2026-02-14T14:00:00')
);
```

---

## Handling App Launch from Alarm

Detect when your app is launched by an alarm and navigate to the appropriate screen:

```typescript
// App.tsx or _layout.tsx
import { useEffect } from 'react';
import { useRouter } from 'expo-router';
import UltimateAlarm from 'react-native-ultimate-alarm';

export default function App() {
  const router = useRouter();

  useEffect(() => {
    async function checkAlarmLaunch() {
      // IMPORTANT: Call this early in app lifecycle
      // The payload is cleared after first call
      const launchPayload = await UltimateAlarm.getLaunchPayload();

      if (launchPayload) {
        console.log('App launched by alarm:', launchPayload);

        // Navigate based on alarm data
        if (launchPayload.data?.routineId) {
          router.push(`/routines/${launchPayload.data.routineId}`);
        } else if (launchPayload.data?.taskId) {
          router.push(`/tasks/${launchPayload.data.taskId}`);
        } else {
          // Default alarm screen
          router.push('/alarms');
        }

        // Track analytics
        analytics.track('alarm_launched', {
          alarmId: launchPayload.alarmId,
          action: launchPayload.action,
        });
      }
    }

    checkAlarmLaunch();
  }, []);

  return (
    // Your app components
  );
}
```

---

## Event Listeners

Listen for alarm dismiss and snooze events:

```typescript
import { useEffect } from 'react';
import { Alert } from 'react-native';
import UltimateAlarm from 'react-native-ultimate-alarm';

export default function AlarmEventsScreen() {
  useEffect(() => {
    // Listen for alarm dismissals
    const dismissSubscription = UltimateAlarm.addEventListener(
      'dismiss',
      (event) => {
        console.log('Alarm dismissed:', event);

        // Track analytics
        analytics.track('alarm_dismissed', {
          alarmId: event.alarmId,
          timestamp: event.timestamp,
        });

        // Update UI
        if (event.data?.taskId) {
          markTaskComplete(event.data.taskId);
        }

        // Show confirmation
        Alert.alert(
          'Alarm Dismissed',
          `Alarm ${event.alarmId} was dismissed at ${event.timestamp.toLocaleTimeString()}`
        );
      }
    );

    // Listen for alarm snoozes
    const snoozeSubscription = UltimateAlarm.addEventListener(
      'snooze',
      (event) => {
        console.log('Alarm snoozed:', event);

        // Track analytics
        analytics.track('alarm_snoozed', {
          alarmId: event.alarmId,
          timestamp: event.timestamp,
        });

        // Update UI
        updateAlarmStatus(event.alarmId, 'snoozed');

        // Show feedback
        Alert.alert(
          'Alarm Snoozed',
          'Alarm will ring again in 5 minutes'
        );
      }
    );

    // Cleanup subscriptions on unmount
    return () => {
      dismissSubscription.remove();
      snoozeSubscription.remove();
    };
  }, []);

  return (
    // Your screen components
  );
}
```

---

## Managing Multiple Alarms

Create, list, and manage multiple alarms:

```typescript
import { useState, useEffect } from 'react';
import { View, Text, FlatList, Button, Alert } from 'react-native';
import UltimateAlarm, { AlarmConfig } from 'react-native-ultimate-alarm';

export default function AlarmListScreen() {
  const [alarms, setAlarms] = useState<AlarmConfig[]>([]);

  useEffect(() => {
    loadAlarms();
  }, []);

  async function loadAlarms() {
    try {
      const allAlarms = await UltimateAlarm.getAllAlarms();
      setAlarms(allAlarms);
    } catch (error) {
      console.error('Failed to load alarms:', error);
    }
  }

  async function addNewAlarm() {
    try {
      const alarmTime = new Date();
      alarmTime.setMinutes(alarmTime.getMinutes() + 5);

      await UltimateAlarm.scheduleAlarm({
        id: `alarm-${Date.now()}`,
        time: alarmTime,
        title: 'New Alarm',
        message: 'This is a new alarm',
        snooze: { enabled: true },
      });

      await loadAlarms(); // Refresh list
      Alert.alert('Success', 'Alarm added');
    } catch (error) {
      Alert.alert('Error', error.message);
    }
  }

  async function deleteAlarm(alarmId: string) {
    try {
      await UltimateAlarm.cancelAlarm(alarmId);
      await loadAlarms(); // Refresh list
      Alert.alert('Deleted', 'Alarm removed');
    } catch (error) {
      Alert.alert('Error', error.message);
    }
  }

  async function deleteAllAlarms() {
    Alert.alert(
      'Confirm',
      'Delete all alarms?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete All',
          style: 'destructive',
          onPress: async () => {
            try {
              await UltimateAlarm.cancelAllAlarms();
              await loadAlarms(); // Refresh list
              Alert.alert('Success', 'All alarms deleted');
            } catch (error) {
              Alert.alert('Error', error.message);
            }
          },
        },
      ]
    );
  }

  return (
    <View style={{ flex: 1, padding: 20 }}>
      <Text style={{ fontSize: 24, marginBottom: 20 }}>
        My Alarms ({alarms.length})
      </Text>

      <FlatList
        data={alarms}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <View style={{
            padding: 15,
            marginBottom: 10,
            backgroundColor: '#f5f5f5',
            borderRadius: 8,
          }}>
            <Text style={{ fontSize: 18, fontWeight: 'bold' }}>
              {item.title}
            </Text>
            <Text>{item.message}</Text>
            <Text>Time: {new Date(item.time).toLocaleString()}</Text>
            {item.repeat && (
              <Text>
                Repeats: {item.repeat.weekdays
                  ?.map(d => ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][d])
                  .join(', ')}
              </Text>
            )}
            <Button
              title="Delete"
              onPress={() => deleteAlarm(item.id)}
              color="red"
            />
          </View>
        )}
      />

      <Button title="Add New Alarm" onPress={addNewAlarm} />
      <View style={{ height: 10 }} />
      <Button
        title="Delete All"
        onPress={deleteAllAlarms}
        color="red"
        disabled={alarms.length === 0}
      />
    </View>
  );
}
```

---

## Error Handling

Proper error handling for alarm operations:

```typescript
import { Alert } from 'react-native';
import UltimateAlarm from 'react-native-ultimate-alarm';

async function scheduleAlarmWithErrorHandling(config) {
  try {
    // Check permissions first
    const hasPerms = await UltimateAlarm.hasPermissions();
    if (!hasPerms) {
      const granted = await UltimateAlarm.requestPermissions();
      if (!granted) {
        throw new Error('Permissions denied by user');
      }
    }

    // Validate alarm time
    if (config.time <= new Date()) {
      throw new Error('Alarm time must be in the future');
    }

    // Check if alarm already exists
    const isScheduled = await UltimateAlarm.isAlarmScheduled(config.id);
    if (isScheduled) {
      Alert.alert(
        'Alarm Exists',
        'An alarm with this ID already exists. Replace it?',
        [
          { text: 'Cancel', style: 'cancel' },
          {
            text: 'Replace',
            onPress: async () => {
              await UltimateAlarm.cancelAlarm(config.id);
              await UltimateAlarm.scheduleAlarm(config);
              Alert.alert('Success', 'Alarm replaced');
            },
          },
        ]
      );
      return;
    }

    // Schedule alarm
    await UltimateAlarm.scheduleAlarm(config);
    Alert.alert('Success', 'Alarm scheduled successfully');

  } catch (error) {
    console.error('Failed to schedule alarm:', error);

    // User-friendly error messages
    let message = 'Failed to schedule alarm. Please try again.';

    if (error.message.includes('permission')) {
      message = 'Alarm permissions are required. Please grant them in Settings.';
    } else if (error.message.includes('future')) {
      message = 'Alarm time must be in the future.';
    } else if (error.message.includes('time')) {
      message = 'Invalid alarm time. Please select a valid time.';
    }

    Alert.alert('Error', message);
  }
}
```

---

## Custom Alarm Manager Component

A complete alarm manager with time picker:

```typescript
import { useState } from 'react';
import { View, Text, Button, Alert, Switch, Platform } from 'react-native';
import DateTimePicker from '@react-native-community/datetimepicker';
import UltimateAlarm from 'react-native-ultimate-alarm';

export default function AlarmManager() {
  const [alarmTime, setAlarmTime] = useState(new Date());
  const [showPicker, setShowPicker] = useState(false);
  const [snoozeEnabled, setSnoozeEnabled] = useState(true);
  const [repeatWeekdays, setRepeatWeekdays] = useState(false);

  async function scheduleAlarm() {
    try {
      const config = {
        id: `alarm-${Date.now()}`,
        time: alarmTime,
        title: 'My Alarm',
        message: 'Time to wake up!',
        snooze: {
          enabled: snoozeEnabled,
          duration: 300,
        },
      };

      if (repeatWeekdays) {
        config.repeat = {
          weekdays: [1, 2, 3, 4, 5], // Monday-Friday
        };
      }

      await UltimateAlarm.scheduleAlarm(config);

      Alert.alert(
        'Alarm Set',
        `Alarm scheduled for ${alarmTime.toLocaleTimeString()}`
      );
    } catch (error) {
      Alert.alert('Error', error.message);
    }
  }

  return (
    <View style={{ padding: 20 }}>
      <Text style={{ fontSize: 24, marginBottom: 20 }}>
        Set New Alarm
      </Text>

      {/* Time Picker */}
      <View style={{ marginBottom: 20 }}>
        <Text style={{ fontSize: 16, marginBottom: 10 }}>Alarm Time:</Text>
        {Platform.OS === 'ios' ? (
          <DateTimePicker
            value={alarmTime}
            mode="time"
            display="spinner"
            onChange={(event, date) => date && setAlarmTime(date)}
          />
        ) : (
          <>
            <Button
              title={`Select Time: ${alarmTime.toLocaleTimeString()}`}
              onPress={() => setShowPicker(true)}
            />
            {showPicker && (
              <DateTimePicker
                value={alarmTime}
                mode="time"
                onChange={(event, date) => {
                  setShowPicker(false);
                  if (date) setAlarmTime(date);
                }}
              />
            )}
          </>
        )}
      </View>

      {/* Snooze Toggle */}
      <View style={{
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 20,
      }}>
        <Text style={{ flex: 1 }}>Enable Snooze (5 min)</Text>
        <Switch value={snoozeEnabled} onValueChange={setSnoozeEnabled} />
      </View>

      {/* Repeat Toggle */}
      <View style={{
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 20,
      }}>
        <Text style={{ flex: 1 }}>Repeat Weekdays</Text>
        <Switch value={repeatWeekdays} onValueChange={setRepeatWeekdays} />
      </View>

      {/* Schedule Button */}
      <Button title="Schedule Alarm" onPress={scheduleAlarm} />
    </View>
  );
}
```

---

## Best Practices

### 1. Always Check Permissions

```typescript
// ✅ Good
async function scheduleAlarm(config) {
  const hasPerms = await UltimateAlarm.hasPermissions();
  if (!hasPerms) {
    await UltimateAlarm.requestPermissions();
  }
  await UltimateAlarm.scheduleAlarm(config);
}

// ❌ Bad
async function scheduleAlarm(config) {
  await UltimateAlarm.scheduleAlarm(config); // May fail without permissions
}
```

### 2. Use Unique IDs

```typescript
// ✅ Good - unique IDs
await UltimateAlarm.scheduleAlarm({
  id: `alarm-${Date.now()}-${Math.random()}`,
  // ...
});

// ❌ Bad - hardcoded ID may conflict
await UltimateAlarm.scheduleAlarm({
  id: 'alarm',
  // ...
});
```

### 3. Validate Alarm Time

```typescript
// ✅ Good - validate time is in future
const alarmTime = selectedDate;
if (alarmTime <= new Date()) {
  Alert.alert('Error', 'Please select a future time');
  return;
}
await UltimateAlarm.scheduleAlarm({ time: alarmTime, ... });

// ❌ Bad - no validation
await UltimateAlarm.scheduleAlarm({ time: selectedDate, ... });
```

### 4. Handle Errors Gracefully

```typescript
// ✅ Good
try {
  await UltimateAlarm.scheduleAlarm(config);
  Alert.alert('Success', 'Alarm scheduled');
} catch (error) {
  console.error(error);
  Alert.alert('Error', 'Failed to schedule alarm');
}

// ❌ Bad - no error handling
await UltimateAlarm.scheduleAlarm(config);
```

### 5. Cleanup Event Listeners

```typescript
// ✅ Good - cleanup in useEffect
useEffect(() => {
  const subscription = UltimateAlarm.addEventListener('dismiss', handler);
  return () => subscription.remove(); // Cleanup
}, []);

// ❌ Bad - no cleanup (memory leak)
useEffect(() => {
  UltimateAlarm.addEventListener('dismiss', handler);
}, []);
```

---

## See Also

- [API.md](./API.md) - Complete API reference
- [PERMISSIONS.md](./PERMISSIONS.md) - Permission setup guide
- [CAPABILITIES.md](./CAPABILITIES.md) - Platform capabilities
