/**
 * Platform type
 */
export type AlarmPlatform = 'android' | 'ios';

/**
 * Implementation type based on platform and capabilities
 */
export type AlarmImplementation = 'alarmmanager' | 'alarmkit' | 'notification';

/**
 * Alarm action type
 */
export type AlarmAction = 'dismiss' | 'snooze';

/**
 * Configuration for scheduling an alarm
 */
export interface AlarmConfig {
  /** Unique identifier for the alarm */
  id: string;
  /** When the alarm should fire */
  time: Date;
  /** Alarm title shown to user */
  title: string;
  /** Optional message/body text */
  message?: string;
  /** Optional custom sound file name (without extension) */
  soundName?: string;
  /** Snooze configuration */
  snooze?: SnoozeConfig;
  /** Repeat configuration for recurring alarms */
  repeat?: RepeatConfig;
  /** Custom data to pass through alarm events */
  data?: Record<string, any>;
}

/**
 * Snooze configuration
 */
export interface SnoozeConfig {
  /** Whether snooze is enabled */
  enabled: boolean;
  /** Snooze duration in seconds (default: 300 = 5 minutes) */
  duration?: number;
}

/**
 * Repeat configuration for recurring alarms
 */
export interface RepeatConfig {
  /** Days of week (0-6, where 0 = Sunday) */
  weekdays: number[];
}

/**
 * Platform-specific alarm capabilities
 */
export interface AlarmCapabilities {
  /** Current platform */
  platform: AlarmPlatform;
  /** iOS version (iOS only) */
  iosVersion?: number;
  /** Implementation being used */
  implementation: AlarmImplementation;
  /** Feature availability */
  features: AlarmFeatures;
  /** Known limitations for current platform/implementation */
  limitations?: string[];
}

/**
 * Feature flags for alarm capabilities
 */
export interface AlarmFeatures {
  /** True if alarm persists until dismissed (not just notification sound) */
  truePersistentAlarm: boolean;
  /** True if app auto-launches on alarm dismiss */
  autoLaunchApp: boolean;
  /** True if alarm bypasses silent/DND mode */
  bypassSilentMode: boolean;
  /** True if sound plays continuously until dismissed */
  persistentSound: boolean;
  /** True if snooze is natively supported */
  nativeSnooze: boolean;
  /** True if repeating alarms are supported */
  repeatAlarms: boolean;
}

/**
 * Event emitted when alarm is dismissed or snoozed
 */
export interface AlarmEvent {
  /** ID of the alarm that fired */
  alarmId: string;
  /** User action taken */
  action: AlarmAction;
  /** Custom data passed from alarm config */
  data?: Record<string, any>;
  /** When the event occurred */
  timestamp: Date;
}

/**
 * Permission status for alarm functionality
 */
export interface PermissionStatus {
  /** Notification permission granted */
  notifications: boolean;
  /** Exact alarm permission granted (Android only) */
  exactAlarms?: boolean;
  /** Full-screen intent permission granted (Android only) */
  fullScreenIntent?: boolean;
  /** AlarmKit permission granted (iOS 16+ only) */
  alarmKit?: boolean;
}
