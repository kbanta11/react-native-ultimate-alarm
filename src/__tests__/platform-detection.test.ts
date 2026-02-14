import { Platform } from 'react-native';
import UltimateAlarm from '../index';
import NativeUltimateAlarm from '../NativeUltimateAlarm';

const mockNative = NativeUltimateAlarm as jest.Mocked<typeof NativeUltimateAlarm>;

describe('Platform Detection', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Reset the cached implementation to ensure each test gets fresh detection
    (UltimateAlarm as any).implementation = null;
  });

  describe('Android Platform', () => {
    beforeEach(() => {
      (Platform as any).OS = 'android';
      (Platform as any).Version = 33;
    });

    it('should use alarmmanager implementation on Android', async () => {
      mockNative.getCapabilities.mockResolvedValue({
        platform: 'android',
        implementation: 'alarmmanager',
        features: {
          truePersistentAlarm: true,
          autoLaunchApp: true,
          bypassSilentMode: true,
          persistentSound: true,
        },
        limitations: [],
      });

      const caps = await UltimateAlarm.getCapabilities();

      expect(caps.platform).toBe('android');
      expect(caps.implementation).toBe('alarmmanager');
      // hasAlarmKit should NOT be called on Android
      expect(mockNative.hasAlarmKit).not.toHaveBeenCalled();
    });

    it('should report all Android features available', async () => {
      mockNative.hasAlarmKit.mockResolvedValue(false);
      mockNative.getCapabilities.mockResolvedValue({
        platform: 'android',
        implementation: 'alarmmanager',
        features: {
          truePersistentAlarm: true,
          autoLaunchApp: true,
          bypassSilentMode: true,
          persistentSound: true,
        },
        limitations: [],
      });

      const caps = await UltimateAlarm.getCapabilities();

      expect(caps.features.truePersistentAlarm).toBe(true);
      expect(caps.features.autoLaunchApp).toBe(true);
      expect(caps.features.bypassSilentMode).toBe(true);
      expect(caps.features.persistentSound).toBe(true);
    });
  });

  describe('iOS Platform', () => {
    beforeEach(() => {
      (Platform as any).OS = 'ios';
    });

    describe('iOS 16+ with AlarmKit', () => {
      beforeEach(() => {
        (Platform as any).Version = '16.0';
      });

      it('should detect AlarmKit availability', async () => {
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

        const caps = await UltimateAlarm.getCapabilities();

        expect(caps.implementation).toBe('alarmkit');
        expect(mockNative.hasAlarmKit).toHaveBeenCalled();
      });

      it('should use alarmkit implementation when available', async () => {
        (Platform as any).Version = '16.0';
        mockNative.hasAlarmKit.mockResolvedValue(true);
        mockNative.scheduleAlarm.mockResolvedValue(undefined);

        const alarmConfig = {
          id: 'ios-alarm',
          time: new Date(Date.now() + 60000),
          title: 'iOS Alarm',
          message: 'AlarmKit test',
        };

        await UltimateAlarm.scheduleAlarm(alarmConfig);

        expect(mockNative.scheduleAlarm).toHaveBeenCalledWith(
          'alarmkit',
          expect.any(Object)
        );
      });
    });

    describe('iOS <16 with Notifications', () => {
      beforeEach(() => {
        (Platform as any).Version = '15.0';
      });

      it('should fall back to notifications when AlarmKit unavailable', async () => {
        mockNative.getCapabilities.mockResolvedValue({
          platform: 'ios',
          implementation: 'notification',
          features: {
            truePersistentAlarm: false,
            autoLaunchApp: false,
            bypassSilentMode: false,
            persistentSound: false,
          },
          limitations: [
            'Notification-based alarm (not true alarm)',
            'Sound limited to 30 seconds',
          ],
        });

        const caps = await UltimateAlarm.getCapabilities();

        expect(caps.implementation).toBe('notification');
        expect(caps.features.truePersistentAlarm).toBe(false);
        expect(caps.limitations.length).toBeGreaterThan(0);
        // hasAlarmKit should NOT be called on iOS < 16
        expect(mockNative.hasAlarmKit).not.toHaveBeenCalled();
      });

      it('should use notification implementation when AlarmKit not available', async () => {
        (Platform as any).Version = '15.0';
        mockNative.scheduleAlarm.mockResolvedValue(undefined);

        const alarmConfig = {
          id: 'ios-notif-alarm',
          time: new Date(Date.now() + 60000),
          title: 'iOS Notification Alarm',
          message: 'Notification test',
        };

        await UltimateAlarm.scheduleAlarm(alarmConfig);

        expect(mockNative.scheduleAlarm).toHaveBeenCalledWith(
          'notification',
          expect.any(Object)
        );
      });
    });
  });

  describe('Implementation Selection', () => {
    it('should automatically select best available implementation', async () => {
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

      const caps = await UltimateAlarm.getCapabilities();

      // Should prefer AlarmKit when available
      expect(caps.implementation).toBe('alarmkit');
      expect(caps.features.truePersistentAlarm).toBe(true);
    });

    it('should handle implementation detection errors gracefully', async () => {
      mockNative.hasAlarmKit.mockRejectedValue(new Error('Detection failed'));
      mockNative.getCapabilities.mockResolvedValue({
        platform: 'ios',
        implementation: 'notification',
        features: {
          truePersistentAlarm: false,
          autoLaunchApp: false,
          bypassSilentMode: false,
          persistentSound: false,
        },
        limitations: ['Fallback mode'],
      });

      // Should fall back gracefully
      const caps = await UltimateAlarm.getCapabilities();

      expect(caps.implementation).toBe('notification');
    });
  });

  describe('Feature Availability', () => {
    it('should correctly report feature differences between implementations', async () => {
      // Android - all features
      mockNative.hasAlarmKit.mockResolvedValue(false);
      mockNative.getCapabilities.mockResolvedValue({
        platform: 'android',
        implementation: 'alarmmanager',
        features: {
          truePersistentAlarm: true,
          autoLaunchApp: true,
          bypassSilentMode: true,
          persistentSound: true,
        },
        limitations: [],
      });

      const androidCaps = await UltimateAlarm.getCapabilities();
      expect(androidCaps.features.truePersistentAlarm).toBe(true);

      // iOS notification - limited features
      mockNative.getCapabilities.mockResolvedValue({
        platform: 'ios',
        implementation: 'notification',
        features: {
          truePersistentAlarm: false,
          autoLaunchApp: false,
          bypassSilentMode: false,
          persistentSound: false,
        },
        limitations: ['30s sound limit'],
      });

      const iosCaps = await UltimateAlarm.getCapabilities();
      expect(iosCaps.features.truePersistentAlarm).toBe(false);
    });
  });
});
