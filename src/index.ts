import { Platform } from 'react-native';
import NativeUltimateAlarm from './NativeUltimateAlarm';
import type {
  AlarmConfig,
  AlarmCapabilities,
  AlarmEvent,
  AlarmImplementation,
} from './types';

export type {
  AlarmConfig,
  AlarmCapabilities,
  AlarmEvent,
  AlarmFeatures,
  AlarmPlatform,
  AlarmImplementation,
  AlarmAction,
  SnoozeConfig,
  RepeatConfig,
  PermissionStatus,
} from './types';

/**
 * UltimateAlarm - Unified alarm API for React Native
 *
 * Automatically selects the best implementation for each platform:
 * - Android: Native AlarmManager
 * - iOS 16+: Native AlarmKit
 * - iOS < 16: Notification-based fallback
 */
class UltimateAlarmClass {
  private implementation: AlarmImplementation | null = null;

  /**
   * Detect and cache the best alarm implementation for this device
   * @internal
   */
  private async detectImplementation(): Promise<AlarmImplementation> {
    if (this.implementation) {
      return this.implementation;
    }

    if (Platform.OS === 'android') {
      this.implementation = 'alarmmanager';
      return 'alarmmanager';
    }

    if (Platform.OS === 'ios') {
      const iosVersion = parseInt(Platform.Version as string, 10);

      if (iosVersion >= 16) {
        // Check if AlarmKit is actually available
        const hasAlarmKit = await NativeUltimateAlarm.hasAlarmKit();
        if (hasAlarmKit) {
          this.implementation = 'alarmkit';
          return 'alarmkit';
        }
      }

      // Fallback to notifications
      this.implementation = 'notification';
      return 'notification';
    }

    throw new Error('Unsupported platform');
  }

  /**
   * Get platform-specific alarm capabilities
   *
   * Returns information about what features are available on the current device.
   *
   * @example
   * ```typescript
   * const capabilities = await UltimateAlarm.getCapabilities();
   * if (capabilities.features.truePersistentAlarm) {
   *   console.log('True alarm supported!');
   * } else {
   *   console.log('Limitations:', capabilities.limitations);
   * }
   * ```
   */
  async getCapabilities(): Promise<AlarmCapabilities> {
    const impl = await this.detectImplementation();
    return NativeUltimateAlarm.getCapabilities(impl);
  }

  /**
   * Request necessary permissions for alarms
   *
   * On Android 12+, this will prompt for exact alarm scheduling permission.
   * On iOS, this will request notification permissions (or AlarmKit on iOS 16+).
   *
   * @returns true if all required permissions are granted
   *
   * @example
   * ```typescript
   * const granted = await UltimateAlarm.requestPermissions();
   * if (!granted) {
   *   Alert.alert('Permissions Required', 'Please grant alarm permissions');
   * }
   * ```
   */
  async requestPermissions(): Promise<boolean> {
    const impl = await this.detectImplementation();
    return NativeUltimateAlarm.requestPermissions(impl);
  }

  /**
   * Check if all required permissions are granted
   *
   * @returns true if permissions are granted
   *
   * @example
   * ```typescript
   * const hasPerms = await UltimateAlarm.hasPermissions();
   * if (!hasPerms) {
   *   await UltimateAlarm.requestPermissions();
   * }
   * ```
   */
  async hasPermissions(): Promise<boolean> {
    const impl = await this.detectImplementation();
    return NativeUltimateAlarm.hasPermissions(impl);
  }

  /**
   * Schedule a new alarm
   *
   * @param config - Alarm configuration
   *
   * @example
   * ```typescript
   * await UltimateAlarm.scheduleAlarm({
   *   id: 'morning-alarm',
   *   time: new Date('2026-02-13T07:00:00'),
   *   title: 'Wake up!',
   *   message: 'Time for your morning routine',
   *   snooze: {
   *     enabled: true,
   *     duration: 300, // 5 minutes
   *   },
   *   data: {
   *     routineId: '123',
   *   },
   * });
   * ```
   */
  async scheduleAlarm(config: AlarmConfig): Promise<void> {
    const impl = await this.detectImplementation();
    return NativeUltimateAlarm.scheduleAlarm(impl, config);
  }

  /**
   * Cancel an alarm by ID
   *
   * @param alarmId - The ID of the alarm to cancel
   *
   * @example
   * ```typescript
   * await UltimateAlarm.cancelAlarm('morning-alarm');
   * ```
   */
  async cancelAlarm(alarmId: string): Promise<void> {
    const impl = await this.detectImplementation();
    return NativeUltimateAlarm.cancelAlarm(impl, alarmId);
  }

  /**
   * Cancel all scheduled alarms
   *
   * @example
   * ```typescript
   * await UltimateAlarm.cancelAllAlarms();
   * ```
   */
  async cancelAllAlarms(): Promise<void> {
    const impl = await this.detectImplementation();
    return NativeUltimateAlarm.cancelAllAlarms(impl);
  }

  /**
   * Get all scheduled alarms
   *
   * @returns Array of scheduled alarm configurations
   *
   * @example
   * ```typescript
   * const alarms = await UltimateAlarm.getAllAlarms();
   * console.log(`You have ${alarms.length} alarms scheduled`);
   * ```
   */
  async getAllAlarms(): Promise<AlarmConfig[]> {
    const impl = await this.detectImplementation();
    return NativeUltimateAlarm.getAllAlarms(impl);
  }

  /**
   * Check if a specific alarm is scheduled
   *
   * @param alarmId - The ID of the alarm to check
   * @returns true if alarm is scheduled
   *
   * @example
   * ```typescript
   * const isScheduled = await UltimateAlarm.isAlarmScheduled('morning-alarm');
   * if (!isScheduled) {
   *   // Re-schedule alarm
   * }
   * ```
   */
  async isAlarmScheduled(alarmId: string): Promise<boolean> {
    const impl = await this.detectImplementation();
    return NativeUltimateAlarm.isAlarmScheduled(impl, alarmId);
  }

  /**
   * Snooze an alarm (reschedule for X minutes later)
   *
   * @param alarmId - The ID of the alarm to snooze
   * @param minutes - How many minutes to snooze (default: 5)
   *
   * @example
   * ```typescript
   * await UltimateAlarm.snoozeAlarm('morning-alarm', 10); // Snooze for 10 minutes
   * ```
   */
  async snoozeAlarm(alarmId: string, minutes: number = 5): Promise<void> {
    const impl = await this.detectImplementation();
    return NativeUltimateAlarm.snoozeAlarm(impl, alarmId, minutes);
  }

  /**
   * Get the payload if app was launched by an alarm
   *
   * IMPORTANT: Call this early in your app lifecycle (e.g., in App.tsx or _layout.tsx).
   * The payload is cleared after the first call.
   *
   * @returns AlarmEvent if launched by alarm, null otherwise
   *
   * @example
   * ```typescript
   * // In App.tsx or _layout.tsx
   * useEffect(() => {
   *   async function checkLaunch() {
   *     const launchPayload = await UltimateAlarm.getLaunchPayload();
   *     if (launchPayload) {
   *       console.log('Launched by alarm:', launchPayload.alarmId);
   *       // Navigate to appropriate screen
   *       router.push('/journal/dream');
   *     }
   *   }
   *   checkLaunch();
   * }, []);
   * ```
   */
  async getLaunchPayload(): Promise<AlarmEvent | null> {
    const impl = await this.detectImplementation();
    return NativeUltimateAlarm.getLaunchPayload(impl);
  }

  /**
   * Add event listener for alarm events
   *
   * @param event - Event type ('dismiss' or 'snooze')
   * @param callback - Event handler
   * @returns Subscription object with remove() method
   *
   * @example
   * ```typescript
   * useEffect(() => {
   *   const subscription = UltimateAlarm.addEventListener('dismiss', (event) => {
   *     console.log('Alarm dismissed:', event.alarmId);
   *     // Handle alarm dismiss
   *   });
   *
   *   return () => subscription.remove();
   * }, []);
   * ```
   */
  addEventListener(
    event: 'dismiss' | 'snooze',
    callback: (event: AlarmEvent) => void
  ): { remove: () => void } {
    return NativeUltimateAlarm.addEventListener(event, callback);
  }
}

/**
 * Default export - Singleton instance of UltimateAlarm
 */
export default new UltimateAlarmClass();
