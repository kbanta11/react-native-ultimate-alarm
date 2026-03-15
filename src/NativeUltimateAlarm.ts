import { requireNativeModule, EventEmitter } from 'expo-modules-core';
import type { AlarmConfig, AlarmCapabilities, AlarmEvent } from './types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const UltimateAlarm = requireNativeModule('UltimateAlarm') as any;

const eventEmitter = new EventEmitter(UltimateAlarm);

/**
 * Native module bindings for UltimateAlarm
 * @internal
 */
class NativeUltimateAlarmModule {
  hasAlarmKit(): Promise<boolean> {
    return UltimateAlarm.hasAlarmKit();
  }

  getCapabilities(implementation: string): Promise<AlarmCapabilities> {
    return UltimateAlarm.getCapabilities(implementation);
  }

  requestPermissions(implementation: string): Promise<boolean> {
    return UltimateAlarm.requestPermissions(implementation);
  }

  hasPermissions(implementation: string): Promise<boolean> {
    return UltimateAlarm.hasPermissions(implementation);
  }

  scheduleAlarm(implementation: string, config: AlarmConfig): Promise<void> {
    const nativeConfig = {
      ...config,
      time: config.time.getTime(),
    };
    return UltimateAlarm.scheduleAlarm(implementation, nativeConfig);
  }

  dismissAlarm(implementation: string, alarmId: string): Promise<void> {
    return UltimateAlarm.dismissAlarm(implementation, alarmId);
  }

  cancelAlarm(implementation: string, alarmId: string): Promise<void> {
    return UltimateAlarm.cancelAlarm(implementation, alarmId);
  }

  cancelAllAlarms(implementation: string): Promise<void> {
    return UltimateAlarm.cancelAllAlarms(implementation);
  }

  getAllAlarms(implementation: string): Promise<AlarmConfig[]> {
    return UltimateAlarm.getAllAlarms(implementation).then((alarms: any[]) =>
      alarms.map((alarm: any) => ({
        ...alarm,
        time: new Date(alarm.time),
      }))
    );
  }

  isAlarmScheduled(implementation: string, alarmId: string): Promise<boolean> {
    return UltimateAlarm.isAlarmScheduled(implementation, alarmId);
  }

  snoozeAlarm(
    implementation: string,
    alarmId: string,
    minutes: number
  ): Promise<void> {
    return UltimateAlarm.snoozeAlarm(implementation, alarmId, minutes);
  }

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

  addEventListener(
    event: 'dismiss' | 'snooze',
    callback: (event: AlarmEvent) => void
  ): { remove: () => void } {
    const subscription = eventEmitter.addListener(
      // @ts-ignore - EventEmitter types are too strict for dynamic event names
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
}

export default new NativeUltimateAlarmModule();
