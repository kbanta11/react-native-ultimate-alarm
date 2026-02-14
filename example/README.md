# Ultimate Alarm Example App

This example demonstrates all features of `react-native-ultimate-alarm`.

## Features Demonstrated

✅ Platform capability detection
✅ Permission requests
✅ Scheduling one-time alarms
✅ Scheduling repeating alarms
✅ Viewing all scheduled alarms
✅ Canceling alarms
✅ Event listeners (dismiss/snooze)
✅ Alarm launch detection

## Running the Example

### Prerequisites

- Node.js 18+
- iOS: Xcode 14+, CocoaPods
- Android: Android Studio, Android SDK

### Setup

```bash
# Install dependencies
npm install

# iOS setup
cd ios && pod install && cd ..

# Run on iOS
npm run ios

# Run on Android
npm run android
```

### Testing Alarms

1. **Test Alarm (10 seconds)**
   - Tap "Schedule Test Alarm (10s)"
   - Wait 10 seconds
   - Alarm should fire
   - Test snooze and dismiss buttons

2. **Repeating Alarm**
   - Tap "Schedule Repeating Alarm (Weekdays 9 AM)"
   - Check alarm list to verify it's scheduled
   - Will fire every weekday at 9 AM

3. **App Launch Detection**
   - Schedule an alarm
   - Close the app completely
   - When alarm fires, tap "Dismiss"
   - App should auto-launch (Android & iOS 16+)
   - You'll see an alert confirming the launch

4. **Event Listeners**
   - Keep app open when alarm fires
   - Dismiss or snooze the alarm
   - You'll see alerts from the event listeners

## What to Expect Per Platform

### Android
- ✅ Full-screen alarm on lock screen
- ✅ Sound plays at max volume (bypasses silent mode)
- ✅ App auto-launches on dismiss
- ✅ Snooze and dismiss buttons work
- ✅ Survives force-close

### iOS 16+
- ✅ Native iOS alarm (like Clock app)
- ✅ Persistent sound until dismissed
- ✅ App auto-launches on dismiss
- ✅ Survives force-close
- ⚠️ Cannot customize alarm screen UI (system UI)

### iOS <16
- ⚠️ Notification-based alarm
- ⚠️ Sound limited to 30 seconds
- ⚠️ User must tap notification to open app
- ⚠️ May not survive force-close

## Customizing the Example

### Add a Custom Alarm Screen (Android)

Create a new screen that shows when the alarm fires:

```typescript
// app/alarm-ringing.tsx (if using Expo Router)
export default function AlarmRingingScreen() {
  return (
    <View style={{ flex: 1, backgroundColor: '#1a1a2e' }}>
      <Text>Your custom alarm UI!</Text>
      <Button title="Dismiss" onPress={() => {/* dismiss alarm */}} />
    </View>
  );
}
```

Then in `App.tsx`, navigate to it when alarm launches:

```typescript
const payload = await UltimateAlarm.getLaunchPayload();
if (payload) {
  navigation.navigate('AlarmRinging', { alarmId: payload.alarmId });
}
```

### Add Alarm Editing

See the full alarm manager component in:
**[docs/ALARM-MANAGER-COMPONENT.md](../docs/ALARM-MANAGER-COMPONENT.md)**

This includes:
- ✅ Edit alarm time
- ✅ Edit title/message
- ✅ Toggle snooze
- ✅ Set snooze duration
- ✅ Select weekdays for repeating

## Testing Tips

### Android

**Enable exact alarms permission:**
- Settings → Apps → Your App → Alarms & Reminders → Allow

**Disable battery optimization:**
- Settings → Battery → Battery Optimization → Your App → Don't Optimize

**Test force-close:**
1. Schedule alarm
2. Force-close app from Recent Apps
3. Alarm should still fire

### iOS

**Request notification permissions:**
- The app will prompt on first launch
- If denied, go to Settings → Your App → Notifications → Enable

**Test on real device:**
- iOS Simulator doesn't support alarms reliably
- Use a real iPhone for testing

## Troubleshooting

### Alarm doesn't fire

**Android:**
- Check battery optimization is disabled
- Check exact alarm permission is granted
- On Xiaomi/Huawei: Grant "Autostart" permission

**iOS:**
- Check notification permissions
- For iOS 16+: Check AlarmKit usage description in Info.plist
- Rebuild after changing permissions

### App doesn't launch on alarm dismiss

**Android:**
- Should work automatically

**iOS 16+:**
- Should work with AlarmKit
- Verify App Groups entitlement is configured

**iOS <16:**
- This is expected - notification-based alarms can't auto-launch
- User must tap notification

## Documentation

Full documentation:
- [API Reference](../docs/API.md)
- [Permissions Guide](../docs/PERMISSIONS.md)
- [Capabilities Guide](../docs/CAPABILITIES.md)
- [Usage Examples](../docs/EXAMPLES.md)
- [Custom Alarm UI](../docs/CUSTOM-ALARM-UI.md)

## Support

- Report issues: [GitHub Issues](https://github.com/kylemichaelreaves/react-native-ultimate-alarm/issues)
- Ask questions: [Discussions](https://github.com/kylemichaelreaves/react-native-ultimate-alarm/discussions)
