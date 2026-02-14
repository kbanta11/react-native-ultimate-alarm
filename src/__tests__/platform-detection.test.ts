import { Platform } from 'react-native';
import UltimateAlarm from '../index';
import NativeUltimateAlarm from '../NativeUltimateAlarm';

const mockNative = NativeUltimateAlarm as jest.Mocked<typeof NativeUltimateAlarm>;

describe('Platform Detection', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Android Platform', () => {
    beforeEach(() => {
      (Platform as any).OS = 'android';
    });

    it('should use alarmmanager implementation on Android', async () => {
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

      expect(caps.platform).toBe('android');
      expect(caps.implementation).toBe('alarmmanager');
      expect(mockNative.hasAlarmKit).toHaveBeenCalled();
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
      it('should fall back to notifications when AlarmKit unavailable', async () => {
        mockNative.hasAlarmKit.mockResolvedValue(false);
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
      });

      it('should use notification implementation when AlarmKit not available', async () => {
        mockNative.hasAlarmKit.mockResolvedValue(false);
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
