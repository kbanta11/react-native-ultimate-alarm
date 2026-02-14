// Mock native modules
jest.mock('./src/NativeUltimateAlarm', () => ({
  __esModule: true,
  default: {
    hasAlarmKit: jest.fn(),
    getCapabilities: jest.fn(),
    requestPermissions: jest.fn(),
    hasPermissions: jest.fn(),
    scheduleAlarm: jest.fn(),
    cancelAlarm: jest.fn(),
    cancelAllAlarms: jest.fn(),
    getAllAlarms: jest.fn(),
    isAlarmScheduled: jest.fn(),
    snoozeAlarm: jest.fn(),
    getLaunchPayload: jest.fn(),
    addEventListener: jest.fn(() => ({
      remove: jest.fn(),
    })),
  },
}));

// Mock Platform
jest.mock('react-native', () => ({
  Platform: {
    OS: 'ios',
    select: jest.fn((obj) => obj.ios),
  },
  NativeModules: {},
  NativeEventEmitter: jest.fn().mockImplementation(() => ({
    addListener: jest.fn(() => ({ remove: jest.fn() })),
    removeAllListeners: jest.fn(),
  })),
}));
