import { NativeModules, NativeEventEmitter } from 'react-native';
import type { AlarmConfig, AlarmCapabilities, AlarmEvent } from './types';

const LINKING_ERROR =
  `The package 'react-native-ultimate-alarm' doesn't seem to be linked. Make sure: \n\n` +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const UltimateAlarm = NativeModules.UltimateAlarm
  ? NativeModules.UltimateAlarm
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

const eventEmitter = new NativeEventEmitter(UltimateAlarm);

/**
 * Native module bindings for UltimateAlarm
 * @internal
 */
class NativeUltimateAlarmModule {
  /**
   * Check if AlarmKit is available (iOS 16+ only)
   */
  hasAlarmKit(): Promise<boolean> {
    return UltimateAlarm.hasAlarmKit();
  }

  /**
   * Get platform-specific capabilities
   */
  getCapabilities(implementation: string): Promise<AlarmCapabilities> {
    return UltimateAlarm.getCapabilities(implementation);
  }

  /**
   * Request necessary permissions
   */
  requestPermissions(implementation: string): Promise<boolean> {
    return UltimateAlarm.requestPermissions(implementation);
  }

  /**
   * Check if permissions are granted
   */
  hasPermissions(implementation: string): Promise<boolean> {
    return UltimateAlarm.hasPermissions(implementation);
  }

  /**
   * Schedule a new alarm
   */
  scheduleAlarm(implementation: string, config: AlarmConfig): Promise<void> {
    // Convert Date to milliseconds for native
    const nativeConfig = {
      ...config,
      time: config.time.getTime(),
    };
    return UltimateAlarm.scheduleAlarm(implementation, nativeConfig);
  }

  /**
   * Cancel an alarm by ID
   */
  cancelAlarm(implementation: string, alarmId: string): Promise<void> {
    return UltimateAlarm.cancelAlarm(implementation, alarmId);
  }

  /**
   * Cancel all alarms
   */
  cancelAllAlarms(implementation: string): Promise<void> {
    return UltimateAlarm.cancelAllAlarms(implementation);
  }

  /**
   * Get all scheduled alarms
   */
  getAllAlarms(implementation: string): Promise<AlarmConfig[]> {
    return UltimateAlarm.getAllAlarms(implementation).then((alarms: any[]) =>
      alarms.map((alarm) => ({
        ...alarm,
        time: new Date(alarm.time),
      }))
    );
  }

  /**
   * Check if specific alarm is scheduled
   */
  isAlarmScheduled(implementation: string, alarmId: string): Promise<boolean> {
    return UltimateAlarm.isAlarmScheduled(implementation, alarmId);
  }

  /**
   * Snooze an alarm
   */
  snoozeAlarm(
    implementation: string,
    alarmId: string,
    minutes: number
  ): Promise<void> {
    return UltimateAlarm.snoozeAlarm(implementation, alarmId, minutes);
  }

  /**
   * Get launch payload if app was launched by alarm
   */
  getLaunchPayload(implementation: string): Promise<AlarmEvent | null> {
    return UltimateAlarm.getLaunchPayload(implementation).then(
      (payload: any) => {
        if (!payload) return null;
        return {
          ...payload,
          timestamp: new Date(payload.timestamp),
        };
      }
    );
  }

  /**
   * Add event listener for alarm events
   */
  addEventListener(
    event: 'dismiss' | 'snooze',
    callback: (event: AlarmEvent) => void
  ): { remove: () => void } {
    const subscription = eventEmitter.addListener(
      `UltimateAlarm.${event}`,
      (nativeEvent: any) => {
        callback({
          ...nativeEvent,
          timestamp: new Date(nativeEvent.timestamp),
        });
      }
    );

    return {
      remove: () => subscription.remove(),
    };
  }

  /**
   * Remove event listener
   */
  removeEventListener(
    event: 'dismiss' | 'snooze',
    callback: (event: AlarmEvent) => void
  ): void {
    eventEmitter.removeListener(`UltimateAlarm.${event}`, callback);
  }
}

export default new NativeUltimateAlarmModule();
