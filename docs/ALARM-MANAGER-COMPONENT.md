# Complete Alarm Manager Component

Full alarm management with view, edit, and delete capabilities.

## Complete Component with Editing

```typescript
import { useState, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  Button,
  Alert,
  StyleSheet,
  ScrollView,
  Switch,
  Modal,
  TouchableOpacity,
  Platform
} from 'react-native';
import DateTimePicker from '@react-native-community/datetimepicker';
import UltimateAlarm, { AlarmConfig } from 'react-native-ultimate-alarm';

export default function AlarmManager() {
  const [alarms, setAlarms] = useState<AlarmConfig[]>([]);
  const [editingAlarm, setEditingAlarm] = useState<AlarmConfig | null>(null);
  const [showEditModal, setShowEditModal] = useState(false);

  // Edit form state
  const [editTime, setEditTime] = useState(new Date());
  const [editTitle, setEditTitle] = useState('');
  const [editMessage, setEditMessage] = useState('');
  const [editSnoozeEnabled, setEditSnoozeEnabled] = useState(true);
  const [editSnoozeDuration, setEditSnoozeDuration] = useState(300);
  const [editRepeatWeekdays, setEditRepeatWeekdays] = useState<number[]>([]);
  const [showTimePicker, setShowTimePicker] = useState(false);

  useEffect(() => {
    loadAlarms();
  }, []);

  async function loadAlarms() {
    const allAlarms = await UltimateAlarm.getAllAlarms();
    setAlarms(allAlarms);
  }

  function openEditModal(alarm: AlarmConfig | null = null) {
    if (alarm) {
      // Edit existing alarm
      setEditingAlarm(alarm);
      setEditTime(new Date(alarm.time));
      setEditTitle(alarm.title);
      setEditMessage(alarm.message);
      setEditSnoozeEnabled(alarm.snooze?.enabled ?? true);
      setEditSnoozeDuration(alarm.snooze?.duration ?? 300);
      setEditRepeatWeekdays(alarm.repeat?.weekdays ?? []);
    } else {
      // Create new alarm
      setEditingAlarm(null);
      setEditTime(new Date());
      setEditTitle('');
      setEditMessage('');
      setEditSnoozeEnabled(true);
      setEditSnoozeDuration(300);
      setEditRepeatWeekdays([]);
    }
    setShowEditModal(true);
  }

  async function saveAlarm() {
    try {
      // Validate
      if (!editTitle.trim()) {
        Alert.alert('Error', 'Please enter a title');
        return;
      }
      if (!editMessage.trim()) {
        Alert.alert('Error', 'Please enter a message');
        return;
      }

      const alarmConfig: AlarmConfig = {
        id: editingAlarm?.id || `alarm-${Date.now()}`,
        time: editTime,
        title: editTitle.trim(),
        message: editMessage.trim(),
        snooze: {
          enabled: editSnoozeEnabled,
          duration: editSnoozeDuration,
        },
      };

      if (editRepeatWeekdays.length > 0) {
        alarmConfig.repeat = {
          weekdays: editRepeatWeekdays,
        };
      }

      // If editing, cancel old alarm first
      if (editingAlarm) {
        await UltimateAlarm.cancelAlarm(editingAlarm.id);
      }

      // Schedule new/updated alarm
      await UltimateAlarm.scheduleAlarm(alarmConfig);

      Alert.alert('Success', editingAlarm ? 'Alarm updated' : 'Alarm created');
      setShowEditModal(false);
      loadAlarms();
    } catch (error) {
      Alert.alert('Error', error.message);
    }
  }

  async function deleteAlarm(alarmId: string) {
    Alert.alert(
      'Delete Alarm',
      'Are you sure you want to delete this alarm?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            await UltimateAlarm.cancelAlarm(alarmId);
            Alert.alert('Deleted', 'Alarm removed');
            loadAlarms();
          },
        },
      ]
    );
  }

  function toggleWeekday(day: number) {
    setEditRepeatWeekdays(prev =>
      prev.includes(day)
        ? prev.filter(d => d !== day)
        : [...prev, day].sort()
    );
  }

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.title}>My Alarms</Text>
        <Button title="+ New Alarm" onPress={() => openEditModal()} />
      </View>

      {/* Alarm List */}
      <ScrollView style={styles.list}>
        {alarms.map((alarm) => (
          <View key={alarm.id} style={styles.alarmCard}>
            <View style={styles.alarmHeader}>
              <Text style={styles.alarmTime}>
                {new Date(alarm.time).toLocaleTimeString([], {
                  hour: '2-digit',
                  minute: '2-digit'
                })}
              </Text>
              <Text style={styles.alarmTitle}>{alarm.title}</Text>
            </View>

            <Text style={styles.alarmMessage}>{alarm.message}</Text>

            {alarm.repeat && alarm.repeat.weekdays && alarm.repeat.weekdays.length > 0 && (
              <Text style={styles.alarmRepeat}>
                Repeats: {alarm.repeat.weekdays
                  .map(d => ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][d])
                  .join(', ')}
              </Text>
            )}

            <View style={styles.alarmActions}>
              <TouchableOpacity
                style={styles.editButton}
                onPress={() => openEditModal(alarm)}
              >
                <Text style={styles.editButtonText}>Edit</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={styles.deleteButton}
                onPress={() => deleteAlarm(alarm.id)}
              >
                <Text style={styles.deleteButtonText}>Delete</Text>
              </TouchableOpacity>
            </View>
          </View>
        ))}

        {alarms.length === 0 && (
          <Text style={styles.emptyText}>
            No alarms set. Tap "+ New Alarm" to create one.
          </Text>
        )}
      </ScrollView>

      {/* Edit/Create Modal */}
      <Modal
        visible={showEditModal}
        animationType="slide"
        presentationStyle="pageSheet"
      >
        <ScrollView style={styles.modalContent}>
          <View style={styles.modalHeader}>
            <Text style={styles.modalTitle}>
              {editingAlarm ? 'Edit Alarm' : 'New Alarm'}
            </Text>
          </View>

          {/* Time Picker */}
          <View style={styles.formGroup}>
            <Text style={styles.label}>Time</Text>
            {Platform.OS === 'ios' ? (
              <DateTimePicker
                value={editTime}
                mode="time"
                display="spinner"
                onChange={(event, date) => date && setEditTime(date)}
              />
            ) : (
              <>
                <Button
                  title={editTime.toLocaleTimeString([], {
                    hour: '2-digit',
                    minute: '2-digit'
                  })}
                  onPress={() => setShowTimePicker(true)}
                />
                {showTimePicker && (
                  <DateTimePicker
                    value={editTime}
                    mode="time"
                    onChange={(event, date) => {
                      setShowTimePicker(false);
                      if (date) setEditTime(date);
                    }}
                  />
                )}
              </>
            )}
          </View>

          {/* Title */}
          <View style={styles.formGroup}>
            <Text style={styles.label}>Title</Text>
            <TextInput
              style={styles.input}
              value={editTitle}
              onChangeText={setEditTitle}
              placeholder="e.g., Wake Up"
            />
          </View>

          {/* Message */}
          <View style={styles.formGroup}>
            <Text style={styles.label}>Message</Text>
            <TextInput
              style={styles.input}
              value={editMessage}
              onChangeText={setEditMessage}
              placeholder="e.g., Time to start your day"
            />
          </View>

          {/* Snooze */}
          <View style={styles.formGroup}>
            <View style={styles.switchRow}>
              <Text style={styles.label}>Enable Snooze</Text>
              <Switch
                value={editSnoozeEnabled}
                onValueChange={setEditSnoozeEnabled}
              />
            </View>
            {editSnoozeEnabled && (
              <View>
                <Text style={styles.sublabel}>Snooze Duration (minutes)</Text>
                <View style={styles.durationButtons}>
                  {[5, 10, 15, 30].map(mins => (
                    <TouchableOpacity
                      key={mins}
                      style={[
                        styles.durationButton,
                        editSnoozeDuration === mins * 60 && styles.durationButtonActive
                      ]}
                      onPress={() => setEditSnoozeDuration(mins * 60)}
                    >
                      <Text style={[
                        styles.durationButtonText,
                        editSnoozeDuration === mins * 60 && styles.durationButtonTextActive
                      ]}>
                        {mins}m
                      </Text>
                    </TouchableOpacity>
                  ))}
                </View>
              </View>
            )}
          </View>

          {/* Repeat */}
          <View style={styles.formGroup}>
            <Text style={styles.label}>Repeat</Text>
            <View style={styles.weekdayButtons}>
              {['S', 'M', 'T', 'W', 'T', 'F', 'S'].map((day, index) => (
                <TouchableOpacity
                  key={index}
                  style={[
                    styles.weekdayButton,
                    editRepeatWeekdays.includes(index) && styles.weekdayButtonActive
                  ]}
                  onPress={() => toggleWeekday(index)}
                >
                  <Text style={[
                    styles.weekdayButtonText,
                    editRepeatWeekdays.includes(index) && styles.weekdayButtonTextActive
                  ]}>
                    {day}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>

          {/* Actions */}
          <View style={styles.modalActions}>
            <Button
              title="Cancel"
              onPress={() => setShowEditModal(false)}
              color="#999"
            />
            <Button title="Save Alarm" onPress={saveAlarm} />
          </View>
        </ScrollView>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    paddingTop: 60,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
  },
  list: {
    flex: 1,
    padding: 20,
  },
  alarmCard: {
    backgroundColor: '#f5f5f5',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    borderLeftWidth: 4,
    borderLeftColor: '#007AFF',
  },
  alarmHeader: {
    marginBottom: 8,
  },
  alarmTime: {
    fontSize: 32,
    fontWeight: 'bold',
    marginBottom: 4,
  },
  alarmTitle: {
    fontSize: 18,
    fontWeight: '600',
  },
  alarmMessage: {
    fontSize: 14,
    color: '#666',
    marginBottom: 8,
  },
  alarmRepeat: {
    fontSize: 12,
    color: '#007AFF',
    marginBottom: 12,
  },
  alarmActions: {
    flexDirection: 'row',
    gap: 8,
  },
  editButton: {
    flex: 1,
    backgroundColor: '#007AFF',
    padding: 10,
    borderRadius: 8,
    alignItems: 'center',
  },
  editButtonText: {
    color: '#fff',
    fontWeight: '600',
  },
  deleteButton: {
    flex: 1,
    backgroundColor: '#FF3B30',
    padding: 10,
    borderRadius: 8,
    alignItems: 'center',
  },
  deleteButtonText: {
    color: '#fff',
    fontWeight: '600',
  },
  emptyText: {
    textAlign: 'center',
    color: '#999',
    marginTop: 40,
    fontSize: 16,
  },
  modalContent: {
    flex: 1,
    padding: 20,
  },
  modalHeader: {
    marginTop: 20,
    marginBottom: 20,
  },
  modalTitle: {
    fontSize: 24,
    fontWeight: 'bold',
  },
  formGroup: {
    marginBottom: 24,
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 8,
  },
  sublabel: {
    fontSize: 14,
    color: '#666',
    marginTop: 8,
    marginBottom: 8,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
  },
  switchRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  durationButtons: {
    flexDirection: 'row',
    gap: 8,
  },
  durationButton: {
    flex: 1,
    padding: 12,
    borderRadius: 8,
    backgroundColor: '#f5f5f5',
    alignItems: 'center',
  },
  durationButtonActive: {
    backgroundColor: '#007AFF',
  },
  durationButtonText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
  },
  durationButtonTextActive: {
    color: '#fff',
  },
  weekdayButtons: {
    flexDirection: 'row',
    gap: 8,
  },
  weekdayButton: {
    flex: 1,
    aspectRatio: 1,
    borderRadius: 20,
    backgroundColor: '#f5f5f5',
    justifyContent: 'center',
    alignItems: 'center',
  },
  weekdayButtonActive: {
    backgroundColor: '#007AFF',
  },
  weekdayButtonText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
  },
  weekdayButtonTextActive: {
    color: '#fff',
  },
  modalActions: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 12,
    marginTop: 20,
    marginBottom: 40,
  },
});
```
