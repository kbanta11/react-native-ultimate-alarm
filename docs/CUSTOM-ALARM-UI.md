# Custom Alarm UI Guide

How to customize the alarm screen appearance across different platforms.

## Platform Capabilities Summary

| Platform | Full-Screen Alarm UI | Customization Level |
|----------|---------------------|---------------------|
| **Android** | ✅ Yes | 🎨 **Full Control** - Launch custom screen in your app |
| **iOS 16+ (AlarmKit)** | ✅ Yes | ❌ **No Control** - System UI (like Clock app) |
| **iOS <16 (Notification)** | ❌ No | ⚠️ **Limited** - Notification content only |

---

## Android - Full Customization ✅

On Android, you have **complete control** over the alarm screen. When the alarm fires, you can launch a specific screen in your app.

### How It Works

1. Alarm fires → App launches with special intent data
2. You detect the alarm launch and navigate to your custom screen
3. Your custom screen can show anything you want

### Implementation

#### Step 1: Create Your Custom Alarm Screen

```typescript
// app/alarm-ringing.tsx (Expo Router)
// or AlarmRingingScreen.tsx (React Navigation)

import { useEffect, useState } from 'react';
import { View, Text, StyleSheet, Pressable, Animated } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import UltimateAlarm from 'react-native-ultimate-alarm';

export default function AlarmRingingScreen() {
  const params = useLocalSearchParams();
  const router = useRouter();
  const [pulse] = useState(new Animated.Value(1));

  // Get alarm data from params
  const alarmId = params.alarmId as string;
  const title = params.title as string;
  const message = params.message as string;

  useEffect(() => {
    // Pulse animation
    Animated.loop(
      Animated.sequence([
        Animated.timing(pulse, {
          toValue: 1.2,
          duration: 1000,
          useNativeDriver: true,
        }),
        Animated.timing(pulse, {
          toValue: 1,
          duration: 1000,
          useNativeDriver: true,
        }),
      ])
    ).start();
  }, []);

  async function handleDismiss() {
    try {
      // Stop the alarm
      await UltimateAlarm.cancelAlarm(alarmId);

      // Navigate to your main screen or journal
      router.replace('/journal/dream');
    } catch (error) {
      console.error('Failed to dismiss alarm:', error);
    }
  }

  async function handleSnooze() {
    try {
      // Snooze for 5 minutes
      await UltimateAlarm.snoozeAlarm(alarmId, 5);

      // Go back to home
      router.replace('/');
    } catch (error) {
      console.error('Failed to snooze alarm:', error);
    }
  }

  return (
    <View style={styles.container}>
      {/* Custom design - whatever you want! */}
      <Animated.View style={[styles.circle, { transform: [{ scale: pulse }] }]}>
        <Text style={styles.emoji}>☀️</Text>
      </Animated.View>

      <Text style={styles.time}>
        {new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
      </Text>

      <Text style={styles.title}>{title}</Text>
      <Text style={styles.message}>{message}</Text>

      {/* Custom buttons */}
      <View style={styles.actions}>
        <Pressable style={styles.snoozeButton} onPress={handleSnooze}>
          <Text style={styles.snoozeText}>Snooze 5 min</Text>
        </Pressable>

        <Pressable style={styles.dismissButton} onPress={handleDismiss}>
          <Text style={styles.dismissText}>Start Dream Journal</Text>
        </Pressable>
      </View>

      {/* Add any custom elements */}
      <Text style={styles.quote}>
        "The early morning has gold in its mouth." - Benjamin Franklin
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#1a1a2e',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
  },
  circle: {
    width: 150,
    height: 150,
    borderRadius: 75,
    backgroundColor: '#ff6b6b',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 40,
  },
  emoji: {
    fontSize: 64,
  },
  time: {
    fontSize: 48,
    fontWeight: 'bold',
    color: '#fff',
    marginBottom: 20,
  },
  title: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#fff',
    marginBottom: 10,
  },
  message: {
    fontSize: 18,
    color: '#aaa',
    marginBottom: 60,
    textAlign: 'center',
  },
  actions: {
    width: '100%',
    gap: 16,
  },
  snoozeButton: {
    backgroundColor: '#4a4a4a',
    padding: 20,
    borderRadius: 12,
    alignItems: 'center',
  },
  snoozeText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '600',
  },
  dismissButton: {
    backgroundColor: '#ff6b6b',
    padding: 20,
    borderRadius: 12,
    alignItems: 'center',
  },
  dismissText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '600',
  },
  quote: {
    position: 'absolute',
    bottom: 40,
    fontSize: 14,
    color: '#666',
    fontStyle: 'italic',
    textAlign: 'center',
    paddingHorizontal: 20,
  },
});
```

#### Step 2: Detect Alarm Launch and Navigate

```typescript
// App.tsx or _layout.tsx

import { useEffect } from 'react';
import { useRouter } from 'expo-router';
import UltimateAlarm from 'react-native-ultimate-alarm';

export default function RootLayout() {
  const router = useRouter();

  useEffect(() => {
    async function checkAlarmLaunch() {
      const launchPayload = await UltimateAlarm.getLaunchPayload();

      if (launchPayload) {
        console.log('App launched by alarm:', launchPayload);

        // Navigate to your custom alarm screen
        router.push({
          pathname: '/alarm-ringing',
          params: {
            alarmId: launchPayload.alarmId,
            title: launchPayload.data?.title || 'Alarm',
            message: launchPayload.data?.message || 'Wake up!',
            ...launchPayload.data, // Pass any custom data
          },
        });
      }
    }

    checkAlarmLaunch();
  }, []);

  return (
    // Your app structure
  );
}
```

#### Step 3: Pass Custom Data When Scheduling

```typescript
await UltimateAlarm.scheduleAlarm({
  id: 'morning-alarm',
  time: alarmTime,
  title: 'Wake Up!',
  message: 'Time for your dream journal',
  data: {
    // This data is available in the launch payload
    title: 'Wake Up!',
    message: 'Time for your dream journal',
    screenToShow: 'dream-journal',
    backgroundColor: '#1a1a2e',
    motivationalQuote: 'Your dreams await...',
  },
});
```

### Result on Android

✅ **Full-screen custom UI when alarm fires**
✅ **Complete design control**
✅ **Can show any React Native components**
✅ **Can navigate to any screen**
✅ **Wake lock + sound handled by native code**

---

## iOS 16+ (AlarmKit) - System UI Only ❌

On iOS 16+, the alarm uses **native iOS AlarmKit**, which means:

### What You Get

- ✅ Native iOS alarm UI (like Clock app)
- ✅ Familiar, polished experience
- ✅ System-managed wake lock and sound
- ✅ **App auto-launches on dismiss**

### What You DON'T Get

- ❌ Cannot customize the alarm screen itself
- ❌ Cannot change colors, buttons, layout
- ❌ System UI only (Apple's design)

### How to Work With It

While you **can't customize the alarm screen**, you **can show your custom screen** when the user dismisses the alarm:

```typescript
// App.tsx or _layout.tsx

useEffect(() => {
  async function checkAlarmLaunch() {
    const launchPayload = await UltimateAlarm.getLaunchPayload();

    if (launchPayload && launchPayload.action === 'dismiss') {
      // User dismissed the iOS native alarm
      // Now show YOUR custom screen
      router.push({
        pathname: '/morning-routine',
        params: {
          alarmId: launchPayload.alarmId,
          ...launchPayload.data,
        },
      });
    }
  }

  checkAlarmLaunch();
}, []);
```

**Flow:**
1. iOS native alarm rings (system UI)
2. User dismisses alarm
3. Your app auto-launches
4. You detect the launch and show YOUR custom screen

---

## iOS <16 (Notifications) - Limited Customization ⚠️

On iOS versions before 16, alarms use **local notifications**.

### What You Can Customize

✅ Notification title and body
✅ Notification sound (custom sounds)
✅ Notification actions (Snooze, Dismiss)
✅ Badge and sound settings

### What You Can't Customize

❌ Notification UI layout
❌ Cannot show full-screen custom UI
❌ Limited to notification appearance

### How to Customize

```typescript
await UltimateAlarm.scheduleAlarm({
  id: 'alarm-1',
  time: alarmTime,
  title: '🌅 Good Morning!',  // Can customize text and emoji
  message: 'Time to record your dreams', // Can customize message
  sound: 'custom-alarm-sound.mp3', // Can use custom sound (30s limit)
  snooze: {
    enabled: true,
    duration: 300,
  },
});
```

**Note:** Sound files must be:
- Added to Xcode project
- Maximum 30 seconds duration
- Supported formats: aiff, caf, wav

---

## Recommended Approach

### For Maximum Control

Use **custom data** to control what screen shows after the alarm:

```typescript
// When scheduling
await UltimateAlarm.scheduleAlarm({
  id: 'alarm-1',
  time: alarmTime,
  title: 'Morning Alarm',
  message: 'Wake up!',
  data: {
    // Control the UI from here
    screen: 'dream-journal',
    theme: 'sunrise',
    backgroundColor: '#ff6b6b',
    greeting: 'Good morning, dreamer!',
    showMotivation: true,
    motivationText: 'Your dreams are waiting to be captured',
  },
});

// When app launches
const launchPayload = await UltimateAlarm.getLaunchPayload();

if (launchPayload) {
  const { screen, theme, backgroundColor, greeting } = launchPayload.data;

  // Navigate to the appropriate screen with theme
  router.push({
    pathname: `/${screen}`,
    params: {
      theme,
      backgroundColor,
      greeting,
      ...launchPayload.data,
    },
  });
}
```

### Platform-Specific UI

Adapt your UI based on what's available:

```typescript
const caps = await UltimateAlarm.getCapabilities();

if (caps.implementation === 'alarmkit') {
  // iOS 16+ - System UI, but app auto-launches
  console.log('Will use native iOS alarm, then show custom post-alarm screen');
} else if (caps.implementation === 'alarmmanager') {
  // Android - Full custom screen possible
  console.log('Will show full custom alarm screen');
} else {
  // iOS <16 - Notification only
  console.log('Will use notification-based alarm with custom content');
}
```

---

## Summary

### What's Possible

| Platform | Alarm Screen | Post-Alarm Screen | Recommendation |
|----------|-------------|-------------------|----------------|
| **Android** | ✅ Full custom UI | ✅ Yes | Build beautiful custom alarm screen |
| **iOS 16+** | ❌ System UI only | ✅ Yes (auto-launches) | Accept system UI, customize post-alarm |
| **iOS <16** | ❌ Notification only | ⚠️ Manual tap required | Use rich notification content |

### Best Practice

1. **Create a custom post-alarm screen** that works on all platforms
2. **On Android**: Also create custom alarm ringing screen (optional)
3. **Use custom data** to control what screen shows and with what theme/content
4. **Detect platform** and adapt the experience accordingly

This gives you:
- ✅ Maximum control on Android
- ✅ Native experience on iOS 16+ with custom post-alarm flow
- ✅ Best possible experience on iOS <16
