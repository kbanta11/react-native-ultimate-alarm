const {
  withAndroidManifest,
  withInfoPlist,
  withEntitlementsPlist,
  AndroidConfig,
  IOSConfig,
} = require('expo/config-plugins');

const ALARM_USAGE_DESCRIPTION = 'This app uses alarms to wake you up at your scheduled time';

/**
 * Expo Config Plugin for react-native-ultimate-alarm
 *
 * Automatically configures:
 * - Android: Permissions, receivers, services in AndroidManifest.xml
 * - iOS: Entitlements, Info.plist entries for AlarmKit
 */
const withUltimateAlarm = (config, props = {}) => {
  const alarmUsageDescription = props.alarmUsageDescription || ALARM_USAGE_DESCRIPTION;

  // Configure Android
  config = withAndroidManifest(config, async (config) => {
    const androidManifest = config.modResults.manifest;

    // Add permissions
    if (!androidManifest['uses-permission']) {
      androidManifest['uses-permission'] = [];
    }

    const permissions = [
      'android.permission.SCHEDULE_EXACT_ALARM',
      'android.permission.USE_EXACT_ALARM',
      'android.permission.WAKE_LOCK',
      'android.permission.VIBRATE',
      'android.permission.RECEIVE_BOOT_COMPLETED',
      'android.permission.POST_NOTIFICATIONS',
      'android.permission.USE_FULL_SCREEN_INTENT',
    ];

    permissions.forEach((permission) => {
      if (!androidManifest['uses-permission'].find((p) => p.$?.['android:name'] === permission)) {
        androidManifest['uses-permission'].push({
          $: { 'android:name': permission },
        });
      }
    });

    // Add receivers and service to application
    if (!androidManifest.application) {
      androidManifest.application = [{}];
    }

    const application = androidManifest.application[0];

    if (!application.receiver) {
      application.receiver = [];
    }

    if (!application.service) {
      application.service = [];
    }

    // Add AlarmReceiver
    if (!application.receiver.find((r) => r.$?.['android:name'] === '.AlarmReceiver')) {
      application.receiver.push({
        $: {
          'android:name': 'com.ultimatealarm.AlarmReceiver',
          'android:enabled': 'true',
          'android:exported': 'false',
        },
      });
    }

    // Add BootReceiver
    if (!application.receiver.find((r) => r.$?.['android:name'] === '.BootReceiver')) {
      application.receiver.push({
        $: {
          'android:name': 'com.ultimatealarm.BootReceiver',
          'android:enabled': 'true',
          'android:exported': 'true',
        },
        'intent-filter': [
          {
            action: [
              { $: { 'android:name': 'android.intent.action.BOOT_COMPLETED' } },
              { $: { 'android:name': 'android.intent.action.QUICKBOOT_POWERON' } },
            ],
          },
        ],
      });
    }

    // Add AlarmService
    if (!application.service.find((s) => s.$?.['android:name'] === '.AlarmService')) {
      application.service.push({
        $: {
          'android:name': 'com.ultimatealarm.AlarmService',
          'android:enabled': 'true',
          'android:exported': 'false',
          'android:foregroundServiceType': 'mediaPlayback',
        },
      });
    }

    return config;
  });

  // Configure iOS
  config = withInfoPlist(config, (config) => {
    // Add AlarmKit usage description
    config.modResults.NSUserNotificationsUsageDescription =
      config.modResults.NSUserNotificationsUsageDescription || alarmUsageDescription;

    // Add background modes for processing (required for AlarmKit)
    if (!config.modResults.UIBackgroundModes) {
      config.modResults.UIBackgroundModes = [];
    }

    if (!config.modResults.UIBackgroundModes.includes('processing')) {
      config.modResults.UIBackgroundModes.push('processing');
    }

    return config;
  });

  // Add iOS Entitlements for App Groups (required for AlarmKit)
  config = withEntitlementsPlist(config, (config) => {
    const bundleIdentifier = config.ios?.bundleIdentifier || 'com.placeholder';
    const appGroupIdentifier = `group.${bundleIdentifier}.alarms`;

    if (!config.modResults['com.apple.security.application-groups']) {
      config.modResults['com.apple.security.application-groups'] = [];
    }

    if (!config.modResults['com.apple.security.application-groups'].includes(appGroupIdentifier)) {
      config.modResults['com.apple.security.application-groups'].push(appGroupIdentifier);
    }

    return config;
  });

  return config;
};

module.exports = withUltimateAlarm;
