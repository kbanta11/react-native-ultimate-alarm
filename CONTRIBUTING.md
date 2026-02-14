# Contributing to react-native-ultimate-alarm

Thank you for your interest in contributing to `react-native-ultimate-alarm`! This document provides guidelines and instructions for contributing.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Making Changes](#making-changes)
- [Testing](#testing)
- [Code Style](#code-style)
- [Submitting Changes](#submitting-changes)
- [Reporting Bugs](#reporting-bugs)
- [Feature Requests](#feature-requests)

---

## Code of Conduct

This project adheres to a code of conduct. By participating, you are expected to uphold this code. Please be respectful and constructive in all interactions.

---

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/react-native-ultimate-alarm.git
   cd react-native-ultimate-alarm
   ```
3. **Add upstream remote:**
   ```bash
   git remote add upstream https://github.com/kylemichaelreaves/react-native-ultimate-alarm.git
   ```

---

## Development Setup

### Prerequisites

- Node.js 18+
- npm or yarn
- For Android development:
  - Android Studio
  - Android SDK (API 21+)
  - Java 11+
- For iOS development:
  - macOS
  - Xcode 14+
  - CocoaPods

### Install Dependencies

```bash
npm install
```

### Build the Package

```bash
npm run prepare
```

This compiles TypeScript to JavaScript using `react-native-builder-bob`.

### Run Tests

```bash
npm test
```

### Type Checking

```bash
npm run typecheck
```

### Linting

```bash
npm run lint
```

---

## Project Structure

```
react-native-ultimate-alarm/
├── src/                        # TypeScript source code
│   ├── index.ts               # Main API entry point
│   ├── types.ts               # TypeScript type definitions
│   ├── NativeUltimateAlarm.ts # Native module bindings
│   └── __tests__/             # Jest tests
│       ├── index.test.ts
│       ├── platform-detection.test.ts
│       └── events.test.ts
├── android/                    # Android native code
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/ultimatealarm/
│           ├── UltimateAlarmModule.kt
│           ├── UltimateAlarmPackage.kt
│           ├── AlarmReceiver.kt
│           ├── AlarmService.kt
│           ├── BootReceiver.kt
│           └── AlarmStorage.kt
├── ios/                        # iOS native code
│   ├── UltimateAlarm.swift
│   ├── AlarmKitManager.swift
│   ├── NotificationManager.swift
│   ├── AlarmStorage.swift
│   └── UltimateAlarm.podspec
├── example/                    # Example Expo app
│   └── App.tsx
├── docs/                       # Documentation
│   ├── API.md
│   ├── PERMISSIONS.md
│   ├── CAPABILITIES.md
│   └── EXAMPLES.md
├── jest.config.js             # Jest configuration
├── jest.setup.js              # Jest setup/mocks
├── tsconfig.json              # TypeScript config
├── package.json
├── CHANGELOG.md
├── LICENSE
└── README.md
```

---

## Making Changes

### 1. Create a Branch

```bash
git checkout -b feature/my-feature
# or
git checkout -b fix/my-bugfix
```

Use descriptive branch names:
- `feature/alarm-sound-gradual-volume` - for new features
- `fix/android-boot-receiver-crash` - for bug fixes
- `docs/improve-api-examples` - for documentation
- `refactor/cleanup-alarm-storage` - for refactoring

### 2. Make Your Changes

- Write clear, concise code
- Follow existing code style
- Add/update tests for your changes
- Update documentation if needed
- Keep commits focused and atomic

### 3. Write Good Commit Messages

```
feat: add gradual volume increase for alarms

- Implement gradual volume ramp-up over 30 seconds
- Add configuration option for ramp duration
- Update tests to cover new feature
- Document new API in API.md

Closes #123
```

**Format:**
```
type: short description

- Detailed bullet points
- Explaining the changes
- And why they were made

Closes #issue-number
```

**Types:**
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `style:` - Code style changes (formatting, etc.)
- `refactor:` - Code refactoring
- `test:` - Adding or updating tests
- `chore:` - Maintenance tasks

---

## Testing

### Running Tests

```bash
# Run all tests
npm test

# Run tests in watch mode
npm test -- --watch

# Run tests with coverage
npm test -- --coverage
```

### Writing Tests

All new features and bug fixes should include tests.

**Example test:**

```typescript
import UltimateAlarm from '../index';

describe('UltimateAlarm', () => {
  it('should schedule alarm with custom data', async () => {
    const config = {
      id: 'test-alarm',
      time: new Date(Date.now() + 60000),
      title: 'Test',
      message: 'Test message',
      data: { taskId: '123' },
    };

    await UltimateAlarm.scheduleAlarm(config);

    expect(mockNative.scheduleAlarm).toHaveBeenCalledWith(
      expect.any(String),
      expect.objectContaining({ data: { taskId: '123' } })
    );
  });
});
```

### Test Coverage

We aim for:
- **Lines:** 70%+
- **Branches:** 70%+
- **Functions:** 70%+
- **Statements:** 70%+

Check coverage:
```bash
npm test -- --coverage
```

---

## Code Style

### TypeScript

- Use TypeScript for all new code
- Define types for all function parameters and return values
- Use interfaces for object shapes
- Avoid `any` - use proper types or `unknown`

```typescript
// ✅ Good
interface AlarmConfig {
  id: string;
  time: Date;
  title: string;
}

async function scheduleAlarm(config: AlarmConfig): Promise<void> {
  // ...
}

// ❌ Bad
async function scheduleAlarm(config: any) {
  // ...
}
```

### Formatting

- **Indentation:** 2 spaces
- **Semicolons:** Yes
- **Quotes:** Single quotes for strings
- **Line length:** 80-100 characters max
- **Trailing commas:** Yes

We use ESLint to enforce code style. Run:
```bash
npm run lint
```

### Naming Conventions

- **Files:** `kebab-case.ts`
- **Classes:** `PascalCase`
- **Functions:** `camelCase`
- **Constants:** `UPPER_SNAKE_CASE`
- **Interfaces:** `PascalCase`

```typescript
// ✅ Good
interface AlarmCapabilities { ... }
class UltimateAlarmClass { ... }
function scheduleAlarm() { ... }
const DEFAULT_SNOOZE_DURATION = 300;

// ❌ Bad
interface alarm_capabilities { ... }
class ultimate_alarm_class { ... }
function ScheduleAlarm() { ... }
const defaultSnoozeDuration = 300;
```

### Comments

- Write clear, self-documenting code
- Add comments for complex logic
- Use JSDoc for public APIs

```typescript
/**
 * Schedule a new alarm
 *
 * @param config - Alarm configuration
 * @throws {Error} If alarm time is in the past
 *
 * @example
 * ```typescript
 * await scheduleAlarm({
 *   id: 'morning',
 *   time: new Date('2026-02-14T07:00:00'),
 *   title: 'Wake up!',
 * });
 * ```
 */
async function scheduleAlarm(config: AlarmConfig): Promise<void> {
  // ...
}
```

---

## Submitting Changes

### 1. Update Your Branch

```bash
git fetch upstream
git rebase upstream/main
```

### 2. Run Final Checks

```bash
npm run typecheck  # TypeScript type checking
npm run lint       # Linting
npm test           # All tests
```

All checks must pass before submitting.

### 3. Push to Your Fork

```bash
git push origin feature/my-feature
```

### 4. Create a Pull Request

1. Go to the [repository](https://github.com/kylemichaelreaves/react-native-ultimate-alarm)
2. Click "New Pull Request"
3. Select your fork and branch
4. Fill in the PR template:

```markdown
## Description
Brief description of the changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Documentation update
- [ ] Refactoring

## Testing
How was this tested?

## Checklist
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] TypeScript types updated
- [ ] CHANGELOG.md updated
- [ ] All tests pass
- [ ] Lint passes
```

### 5. Code Review

- Respond to feedback promptly
- Make requested changes
- Push updates to your branch
- Be patient and respectful

---

## Reporting Bugs

### Before Reporting

1. **Search existing issues** - Your bug may already be reported
2. **Use latest version** - Update to the latest version and see if bug persists
3. **Reproduce the bug** - Ensure you can consistently reproduce it

### Creating a Bug Report

Open an issue with:

**Title:** Clear, descriptive summary

**Description:**
```markdown
## Bug Description
Clear description of the bug

## Steps to Reproduce
1. Call `UltimateAlarm.scheduleAlarm()`
2. Force-close the app
3. Alarm doesn't fire

## Expected Behavior
Alarm should fire even after force-close

## Actual Behavior
Alarm doesn't fire

## Environment
- OS: Android 13
- Device: Samsung Galaxy S21
- Package version: 0.1.0
- React Native version: 0.73.0

## Code Sample
\`\`\`typescript
await UltimateAlarm.scheduleAlarm({
  id: 'test',
  time: new Date(Date.now() + 60000),
  title: 'Test',
});
\`\`\`

## Logs
\`\`\`
Error: ...
\`\`\`
```

---

## Feature Requests

### Before Requesting

1. **Search existing issues** - Feature may already be requested
2. **Check roadmap** - Feature may be planned
3. **Consider scope** - Ensure it fits the project goals

### Creating a Feature Request

Open an issue with:

```markdown
## Feature Description
Clear description of the feature

## Use Case
Why is this feature needed? What problem does it solve?

## Proposed API
\`\`\`typescript
await UltimateAlarm.setAlarmVolume(alarmId, 0.8); // 80% volume
\`\`\`

## Alternatives Considered
What other approaches did you consider?

## Additional Context
Any other information, mockups, examples
```

---

## Development Tips

### Testing on Real Devices

**Android:**
```bash
cd example
npx expo run:android
```

**iOS:**
```bash
cd example
npx expo run:ios
```

### Debugging Native Code

**Android:**
- Use Android Studio debugger
- View logs: `adb logcat`
- Add Kotlin logging:
  ```kotlin
  Log.d("UltimateAlarm", "Alarm scheduled: $alarmId")
  ```

**iOS:**
- Use Xcode debugger
- View logs in Xcode Console
- Add Swift logging:
  ```swift
  print("Alarm scheduled: \(alarmId)")
  ```

### Local Package Testing

Test the package locally in another project:

```bash
cd my-test-app
npm install ../react-native-ultimate-alarm
```

---

## Questions?

- **Documentation:** Check [docs/](./docs/)
- **Issues:** Search [existing issues](https://github.com/kylemichaelreaves/react-native-ultimate-alarm/issues)
- **Discussions:** Start a [discussion](https://github.com/kylemichaelreaves/react-native-ultimate-alarm/discussions)

---

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

Thank you for contributing to `react-native-ultimate-alarm`! 🎉
