import UltimateAlarm from '../index';
import NativeUltimateAlarm from '../NativeUltimateAlarm';
import { Platform } from 'react-native';

// Mock implementations
const mockNative = NativeUltimateAlarm as jest.Mocked<typeof NativeUltimateAlarm>;

describe('UltimateAlarm', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Reset the cached implementation to ensure each test gets fresh detection
    (UltimateAlarm as any).implementation = null;
    // Default to Android for most tests
    (Platform as any).OS = 'android';
    (Platform as any).Version = 33;
  });

  describe('getCapabilities', () => {
    it('should call native getCapabilities with correct implementation', async () => {
      const mockCapabilities = {
        platform: 'android',
        implementation: 'alarmmanager',
        features: {
          truePersistentAlarm: true,
          autoLaunchApp: true,
          bypassSilentMode: true,
          persistentSound: true,
        },
        limitations: [],
      };

      mockNative.getCapabilities.mockResolvedValue(mockCapabilities);
      mockNative.hasAlarmKit.mockResolvedValue(false);

      const result = await UltimateAlarm.getCapabilities();

      expect(result).toEqual(mockCapabilities);
      expect(mockNative.getCapabilities).toHaveBeenCalled();
    });

    it('should detect AlarmKit on iOS 16+', async () => {
      (Platform as any).OS = 'ios';
      (Platform as any).Version = '16.0';

      mockNative.hasAlarmKit.mockResolvedValue(true);
      mockNative.getCapabilities.mockResolvedValue({
        platform: 'ios',
        implementation: 'alarmkit',
        features: {
          truePersistentAlarm: true,
          autoLaunchApp: true,
          bypassSilentMode: true,
          persistentSound: true,
        },
        limitations: [],
      });

      const result = await UltimateAlarm.getCapabilities();

      expect(mockNative.hasAlarmKit).toHaveBeenCalled();
      expect(result.implementation).toBe('alarmkit');
    });
  });

  describe('requestPermissions', () => {
    it('should request permissions and return result', async () => {
      mockNative.hasAlarmKit.mockResolvedValue(false);
      mockNative.requestPermissions.mockResolvedValue(true);

      const result = await UltimateAlarm.requestPermissions();

      expect(result).toBe(true);
      expect(mockNative.requestPermissions).toHaveBeenCalled();
    });

    it('should handle permission denial', async () => {
      mockNative.hasAlarmKit.mockResolvedValue(false);
      mockNative.requestPermissions.mockResolvedValue(false);

      const result = await UltimateAlarm.requestPermissions();

      expect(result).toBe(false);
    });
  });

  describe('scheduleAlarm', () => {
    it('should schedule an alarm with required parameters', async () => {
      mockNative.hasAlarmKit.mockResolvedValue(false);
      mockNative.scheduleAlarm.mockResolvedValue(undefined);

      const alarmConfig = {
        id: 'test-alarm',
        time: new Date(Date.now() + 60000), // 1 minute from now
        title: 'Test Alarm',
        message: 'This is a test',
      };

      await UltimateAlarm.scheduleAlarm(alarmConfig);

      expect(mockNative.scheduleAlarm).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          id: 'test-alarm',
          title: 'Test Alarm',
          message: 'This is a test',
        })
      );
    });

    it('should schedule alarm with snooze enabled', async () => {
      mockNative.hasAlarmKit.mockResolvedValue(false);
      mockNative.scheduleAlarm.mockResolvedValue(undefined);

      const alarmConfig = {
        id: 'snooze-alarm',
        time: new Date(Date.now() + 60000),
        title: 'Snooze Test',
        message: 'Testing snooze',
        snooze: {
          enabled: true,
          duration: 300,
        },
      };

      await UltimateAlarm.scheduleAlarm(alarmConfig);

      expect(mockNative.scheduleAlarm).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          snooze: {
            enabled: true,
            duration: 300,
          },
        })
      );
    });

    it('should schedule repeating alarm', async () => {
      mockNative.hasAlarmKit.mockResolvedValue(false);
      mockNative.scheduleAlarm.mockResolvedValue(undefined);

      const alarmConfig = {
        id: 'repeat-alarm',
        time: new Date(Date.now() + 60000),
        title: 'Repeating Alarm',
        message: 'Weekday alarm',
        repeat: {
          weekdays: [1, 2, 3, 4, 5], // Monday-Friday
        },
      };

      await UltimateAlarm.scheduleAlarm(alarmConfig);

      expect(mockNative.scheduleAlarm).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          repeat: {
            weekdays: [1, 2, 3, 4, 5],
          },
        })
      );
    });

    it('should throw error if alarm time is in the past', async () => {
      mockNative.hasAlarmKit.mockResolvedValue(false);

      const alarmConfig = {
        id: 'past-alarm',
        time: new Date(Date.now() - 60000), // 1 minute ago
        title: 'Past Alarm',
        message: 'Should fail',
      };

      await expect(UltimateAlarm.scheduleAlarm(alarmConfig)).rejects.toThrow();
    });

    it('should include custom data payload', async () => {
      mockNative.hasAlarmKit.mockResolvedValue(false);
      mockNative.scheduleAlarm.mockResolvedValue(undefined);

      const alarmConfig = {
        id: 'data-alarm',
        time: new Date(Date.now() + 60000),
        title: 'Data Test',
        message: 'Testing data',
        data: {
          userId: '123',
          type: 'reminder',
        },
      };

      await UltimateAlarm.scheduleAlarm(alarmConfig);

      expect(mockNative.scheduleAlarm).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          data: {
            userId: '123',
            type: 'reminder',
          },
        })
      );
    });
  });

  describe('cancelAlarm', () => {
    it('should cancel an alarm by ID', async () => {
      mockNative.hasAlarmKit.mockResolvedValue(false);
      mockNative.cancelAlarm.mockResolvedValue(undefined);

      await UltimateAlarm.cancelAlarm('test-alarm');

      expect(mockNative.cancelAlarm).toHaveBeenCalledWith(
        expect.any(String),
        'test-alarm'
      );
    });
  });

  describe('cancelAllAlarms', () => {
    it('should cancel all alarms', async () => {
      mockNative.hasAlarmKit.mockResolvedValue(false);
      mockNative.cancelAllAlarms.mockResolvedValue(undefined);

      await UltimateAlarm.cancelAllAlarms();

      expect(mockNative.cancelAllAlarms).toHaveBeenCalledWith(expect.any(String));
    });
  });

  describe('getAllAlarms', () => {
    it('should return all scheduled alarms', async () => {
      const mockAlarms = [
        {
          id: 'alarm-1',
          time: new Date(Date.now() + 60000),
          title: 'Alarm 1',
          message: 'First alarm',
        },
        {
          id: 'alarm-2',
          time: new Date(Date.now() + 120000),
          title: 'Alarm 2',
          message: 'Second alarm',
        },
      ];

      mockNative.hasAlarmKit.mockResolvedValue(false);
      mockNative.getAllAlarms.mockResolvedValue(mockAlarms);

      const result = await UltimateAlarm.getAllAlarms();

      expect(result).toEqual(mockAlarms);
      expect(mockNative.getAllAlarms).toHaveBeenCalled();
    });

    it('should return empty array when no alarms', async () => {
      mockNative.hasAlarmKit.mockResolvedValue(false);
      mockNative.getAllAlarms.mockResolvedValue([]);

      const result = await UltimateAlarm.getAllAlarms();

      expect(result).toEqual([]);
    });
  });

  describe('isAlarmScheduled', () => {
    it('should return true for scheduled alarm', async () => {
      mockNative.hasAlarmKit.mockResolvedValue(false);
      mockNative.isAlarmScheduled.mockResolvedValue(true);

      const result = await UltimateAlarm.isAlarmScheduled('test-alarm');

      expect(result).toBe(true);
      expect(mockNative.isAlarmScheduled).toHaveBeenCalledWith(
        expect.any(String),
        'test-alarm'
      );
    });

    it('should return false for non-existent alarm', async () => {
      mockNative.hasAlarmKit.mockResolvedValue(false);
      mockNative.isAlarmScheduled.mockResolvedValue(false);

      const result = await UltimateAlarm.isAlarmScheduled('non-existent');

      expect(result).toBe(false);
    });
  });

  describe('snoozeAlarm', () => {
    it('should snooze alarm for specified minutes', async () => {
      mockNative.hasAlarmKit.mockResolvedValue(false);
      mockNative.snoozeAlarm.mockResolvedValue(undefined);

      await UltimateAlarm.snoozeAlarm('test-alarm', 10);

      expect(mockNative.snoozeAlarm).toHaveBeenCalledWith(
        expect.any(String),
        'test-alarm',
        10
      );
    });
  });

  describe('getLaunchPayload', () => {
    it('should return launch payload if app was launched by alarm', async () => {
      const mockPayload = {
        alarmId: 'test-alarm',
        action: 'dismiss',
        timestamp: new Date(Date.now()),
      };

      mockNative.hasAlarmKit.mockResolvedValue(false);
      mockNative.getLaunchPayload.mockResolvedValue(mockPayload);

      const result = await UltimateAlarm.getLaunchPayload();

      expect(result).toEqual(mockPayload);
    });

    it('should return null if not launched by alarm', async () => {
      mockNative.hasAlarmKit.mockResolvedValue(false);
      mockNative.getLaunchPayload.mockResolvedValue(null);

      const result = await UltimateAlarm.getLaunchPayload();

      expect(result).toBeNull();
    });
  });

  describe('addEventListener', () => {
    it('should add event listener and return subscription', () => {
      const callback = jest.fn();
      const mockSubscription = { remove: jest.fn() };

      mockNative.addEventListener.mockReturnValue(mockSubscription);

      const subscription = UltimateAlarm.addEventListener('dismiss', callback);

      expect(subscription).toHaveProperty('remove');
      expect(typeof subscription.remove).toBe('function');
    });

    it('should support snooze events', () => {
      const callback = jest.fn();
      const mockSubscription = { remove: jest.fn() };

      mockNative.addEventListener.mockReturnValue(mockSubscription);

      const subscription = UltimateAlarm.addEventListener('snooze', callback);

      expect(subscription).toHaveProperty('remove');
    });
  });
});
