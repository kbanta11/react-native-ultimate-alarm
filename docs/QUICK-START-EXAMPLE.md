# Quick Start Example

Copy-paste ready component for testing the alarm package locally.

## Complete Test Component

```typescript
import { useState, useEffect } from 'react';
import { View, Text, Button, Alert, StyleSheet, ScrollView } from 'react-native';
import UltimateAlarm from 'react-native-ultimate-alarm';

export default function AlarmTestScreen() {
  const [capabilities, setCapabilities] = useState(null);
  const [alarms, setAlarms] = useState([]);

  useEffect(() => {
    init();

    // Set up event listeners
    const dismissSub = UltimateAlarm.addEventListener('dismiss', (event) => {
      console.log('Alarm dismissed:', event);
      Alert.alert('Dismissed!', `Alarm ${event.alarmId} was dismissed`);
      loadAlarms(); // Refresh list
    });

    const snoozeSub = UltimateAlarm.addEventListener('snooze', (event) => {
      console.log('Alarm snoozed:', event);
      Alert.alert('Snoozed!', 'Alarm will ring again in 5 minutes');
      loadAlarms(); // Refresh list
    });

    return () => {
      dismissSub.remove();
      snoozeSub.remove();
    };
  }, []);

  async function init() {
    // Request permissions
    const hasPerms = await UltimateAlarm.hasPermissions();
    if (!hasPerms) {
      const granted = await UltimateAlarm.requestPermissions();
      if (!granted) {
        Alert.alert('Error', 'Permissions required');
        return;
      }
    }

    // Get capabilities
    const caps = await UltimateAlarm.getCapabilities();
    setCapabilities(caps);
    console.log('Capabilities:', caps);

    // Check if launched from alarm
    const launchPayload = await UltimateAlarm.getLaunchPayload();
    if (launchPayload) {
      Alert.alert(
        'Launched by Alarm!',
        `Alarm: ${launchPayload.alarmId}\nAction: ${launchPayload.action}`
      );
    }

    // Load existing alarms
    loadAlarms();
  }

  async function loadAlarms() {
    const allAlarms = await UltimateAlarm.getAllAlarms();
    setAlarms(allAlarms);
  }

  async function scheduleTestAlarm() {
    try {
      // Schedule for 10 seconds from now
      const alarmTime = new Date();
      alarmTime.setSeconds(alarmTime.getSeconds() + 10);

      await UltimateAlarm.scheduleAlarm({
        id: `test-${Date.now()}`,
        time: alarmTime,
        title: 'Test Alarm',
        message: 'This is a test! (10 seconds)',
        snooze: {
          enabled: true,
          duration: 300,
        },
        data: {
          test: true,
          timestamp: Date.now(),
        },
      });

      Alert.alert('Success', 'Alarm will ring in 10 seconds!');
      loadAlarms();
    } catch (error) {
      Alert.alert('Error', error.message);
    }
  }

  async function scheduleMorningAlarm() {
    try {
      // Schedule for 7 AM tomorrow
      const alarmTime = new Date();
      alarmTime.setDate(alarmTime.getDate() + 1);
      alarmTime.setHours(7, 0, 0, 0);

      await UltimateAlarm.scheduleAlarm({
        id: 'morning-alarm',
        time: alarmTime,
        title: 'Wake Up!',
        message: 'Time to start your day',
        snooze: {
          enabled: true,
          duration: 600, // 10 minutes
        },
        data: {
          routineId: 'morning-routine',
        },
      });

      Alert.alert('Success', `Morning alarm set for ${alarmTime.toLocaleString()}`);
      loadAlarms();
    } catch (error) {
      Alert.alert('Error', error.message);
    }
  }

  async function scheduleWeekdayAlarm() {
    try {
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
          duration: 600,
        },
      });

      Alert.alert('Success', 'Weekday alarm set for 7:00 AM (Mon-Fri)');
      loadAlarms();
    } catch (error) {
      Alert.alert('Error', error.message);
    }
  }

  async function cancelAllAlarms() {
    Alert.alert(
      'Confirm',
      'Cancel all alarms?',
      [
        { text: 'No', style: 'cancel' },
        {
          text: 'Yes',
          style: 'destructive',
          onPress: async () => {
            await UltimateAlarm.cancelAllAlarms();
            Alert.alert('Success', 'All alarms cancelled');
            loadAlarms();
          },
        },
      ]
    );
  }

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.title}>Alarm Test Screen</Text>

      {/* Capabilities */}
      {capabilities && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Capabilities</Text>
          <Text>Platform: {capabilities.platform}</Text>
          <Text>Implementation: {capabilities.implementation}</Text>
          <Text>True Alarm: {capabilities.features.truePersistentAlarm ? '✅' : '❌'}</Text>
          <Text>Auto Launch: {capabilities.features.autoLaunchApp ? '✅' : '❌'}</Text>
          {capabilities.limitations.length > 0 && (
            <View style={styles.warning}>
              <Text style={styles.warningText}>Limitations:</Text>
              {capabilities.limitations.map((limit, i) => (
                <Text key={i}>• {limit}</Text>
              ))}
            </View>
          )}
        </View>
      )}

      {/* Actions */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Schedule Alarms</Text>

        <Button title="Test Alarm (10 seconds)" onPress={scheduleTestAlarm} />
        <View style={styles.spacer} />

        <Button title="Morning Alarm (7 AM tomorrow)" onPress={scheduleMorningAlarm} />
        <View style={styles.spacer} />

        <Button title="Weekday Alarm (7 AM Mon-Fri)" onPress={scheduleWeekdayAlarm} />
        <View style={styles.spacer} />

        <Button title="Cancel All Alarms" onPress={cancelAllAlarms} color="red" />
      </View>

      {/* Alarm List */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Scheduled Alarms ({alarms.length})</Text>
        {alarms.map((alarm, i) => (
          <View key={i} style={styles.alarmItem}>
            <Text style={styles.alarmTitle}>{alarm.title}</Text>
            <Text>ID: {alarm.id}</Text>
            <Text>Time: {new Date(alarm.time).toLocaleString()}</Text>
            <Text>Message: {alarm.message}</Text>
            {alarm.repeat && (
              <Text>
                Repeats: {alarm.repeat.weekdays
                  ?.map(d => ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][d])
                  .join(', ')}
              </Text>
            )}
            <Button
              title="Cancel This Alarm"
              onPress={async () => {
                await UltimateAlarm.cancelAlarm(alarm.id);
                Alert.alert('Cancelled', `Alarm ${alarm.id} cancelled`);
                loadAlarms();
              }}
              color="red"
            />
          </View>
        ))}
        {alarms.length === 0 && (
          <Text style={styles.emptyText}>No alarms scheduled</Text>
        )}
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
  warning: {
    marginTop: 10,
    padding: 10,
    backgroundColor: '#fff3cd',
    borderRadius: 4,
  },
  warningText: {
    fontWeight: 'bold',
    marginBottom: 5,
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
  emptyText: {
    fontStyle: 'italic',
    color: '#999',
  },
});
```

## Testing Steps

1. **Install the package locally** (see main instructions above)

2. **Add this component to your app:**
   - For Expo Router: Create `app/alarm-test.tsx`
   - For React Navigation: Add to your navigator

3. **Rebuild your app** (important!):
   ```bash
   npx expo prebuild --clean
   npx expo run:android  # or run:ios
   ```

4. **Test the alarm:**
   - Tap "Test Alarm (10 seconds)"
   - Wait 10 seconds
   - Alarm should ring!
   - Test snooze and dismiss buttons

5. **Test app launch:**
   - Schedule an alarm
   - Close the app completely
   - When alarm rings, tap "Dismiss"
   - App should auto-launch (Android & iOS 16+)

## Expected Behavior

### Android
- ✅ Full-screen alarm on lock screen
- ✅ Sound plays at max volume
- ✅ Bypasses silent mode
- ✅ Auto-launches app on dismiss
- ✅ Survives force-close

### iOS 16+
- ✅ Native iOS alarm (like Clock app)
- ✅ Persistent sound
- ✅ Auto-launches app on dismiss
- ✅ Survives force-close

### iOS <16
- ⚠️ Notification-based alarm
- ⚠️ Sound limited to 30 seconds
- ⚠️ May not survive force-close
