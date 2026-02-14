import { useEffect, useState } from 'react';
import { View, Text, Button, Alert, StyleSheet, ScrollView } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import UltimateAlarm from 'react-native-ultimate-alarm';

export default function AlarmTest() {
  const [capabilities, setCapabilities] = useState<any>(null);
  const [alarms, setAlarms] = useState<any[]>([]);

  useEffect(() => {
    async function init() {
      try {
        // Get capabilities
        const caps = await UltimateAlarm.getCapabilities();
        setCapabilities(caps);
        console.log('Capabilities:', caps);

        // Request permissions
        const hasPermissions = await UltimateAlarm.requestPermissions();
        if (!hasPermissions) {
          Alert.alert('Permissions Required', 'Please grant alarm permissions in Settings');
        }

        // Check if launched from alarm
        const launchPayload = await UltimateAlarm.getLaunchPayload();
        if (launchPayload) {
          Alert.alert(
            'Alarm Triggered!',
            `Alarm: ${launchPayload.alarmId}\nAction: ${launchPayload.action}`,
            [{ text: 'OK' }]
          );
        }

        // Load existing alarms
        refreshAlarms();
      } catch (error: any) {
        console.error('Init error:', error);
        Alert.alert('Error', error.message);
      }
    }

    init();

    // Listen for alarm events
    const dismissSub = UltimateAlarm.addEventListener('dismiss', (event) => {
      console.log('Alarm dismissed:', event);
      Alert.alert('Dismissed', `Alarm ${event.alarmId} was dismissed`);
      refreshAlarms();
    });

    const snoozeSub = UltimateAlarm.addEventListener('snooze', (event) => {
      console.log('Alarm snoozed:', event);
      Alert.alert('Snoozed', `Alarm ${event.alarmId} was snoozed`);
      refreshAlarms();
    });

    return () => {
      dismissSub.remove();
      snoozeSub.remove();
    };
  }, []);

  async function refreshAlarms() {
    try {
      const allAlarms = await UltimateAlarm.getAllAlarms();
      setAlarms(allAlarms);
      console.log('All alarms:', allAlarms);
    } catch (error: any) {
      console.error('Failed to get alarms:', error);
    }
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
    } catch (error: any) {
      Alert.alert('Error', error.message);
      console.error('Schedule error:', error);
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
    } catch (error: any) {
      Alert.alert('Error', error.message);
      console.error('Schedule error:', error);
    }
  }

  async function cancelAllAlarms() {
    try {
      await UltimateAlarm.cancelAllAlarms();
      Alert.alert('Success', 'All alarms cancelled');
      refreshAlarms();
    } catch (error: any) {
      Alert.alert('Error', error.message);
      console.error('Cancel error:', error);
    }
  }

  async function cancelAlarm(alarmId: string) {
    try {
      await UltimateAlarm.cancelAlarm(alarmId);
      Alert.alert('Success', `Alarm ${alarmId} cancelled`);
      refreshAlarms();
    } catch (error: any) {
      Alert.alert('Error', error.message);
      console.error('Cancel error:', error);
    }
  }

  return (
    <View style={styles.container}>
      <StatusBar style="auto" />
      <ScrollView style={styles.scrollView}>
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
            {capabilities.limitations && capabilities.limitations.length > 0 && (
              <View style={styles.limitations}>
                <Text style={styles.warningText}>Limitations:</Text>
                {capabilities.limitations.map((limit: string, i: number) => (
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
                  Repeats: {alarm.repeat.weekdays
                    .map((d: number) => ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][d])
                    .join(', ')}
                </Text>
              )}
              <Button
                title="Cancel"
                onPress={() => cancelAlarm(alarm.id)}
                color="orange"
              />
            </View>
          ))}
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  scrollView: {
    flex: 1,
    padding: 20,
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
