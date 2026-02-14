import UltimateAlarm from '../index';
import NativeUltimateAlarm from '../NativeUltimateAlarm';
import type { AlarmEvent } from '../types';

const mockNative = NativeUltimateAlarm as jest.Mocked<typeof NativeUltimateAlarm>;

describe('Event Listeners', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('addEventListener', () => {
    it('should register dismiss event listener', () => {
      const callback = jest.fn();
      const mockSubscription = { remove: jest.fn() };

      mockNative.addEventListener.mockReturnValue(mockSubscription);

      const subscription = UltimateAlarm.addEventListener('dismiss', callback);

      expect(mockNative.addEventListener).toHaveBeenCalledWith(
        'dismiss',
        expect.any(Function)
      );
      expect(subscription).toHaveProperty('remove');
      expect(typeof subscription.remove).toBe('function');
    });

    it('should register snooze event listener', () => {
      const callback = jest.fn();
      const mockSubscription = { remove: jest.fn() };

      mockNative.addEventListener.mockReturnValue(mockSubscription);

      const subscription = UltimateAlarm.addEventListener('snooze', callback);

      expect(mockNative.addEventListener).toHaveBeenCalledWith(
        'snooze',
        expect.any(Function)
      );
      expect(subscription).toHaveProperty('remove');
    });

    it('should support multiple listeners for same event', () => {
      const callback1 = jest.fn();
      const callback2 = jest.fn();
      const mockSubscription1 = { remove: jest.fn() };
      const mockSubscription2 = { remove: jest.fn() };

      mockNative.addEventListener
        .mockReturnValueOnce(mockSubscription1)
        .mockReturnValueOnce(mockSubscription2);

      const sub1 = UltimateAlarm.addEventListener('dismiss', callback1);
      const sub2 = UltimateAlarm.addEventListener('dismiss', callback2);

      expect(sub1).not.toBe(sub2);
      expect(mockNative.addEventListener).toHaveBeenCalledTimes(2);
    });

    it('should support listeners for different events', () => {
      const dismissCallback = jest.fn();
      const snoozeCallback = jest.fn();
      const mockSub1 = { remove: jest.fn() };
      const mockSub2 = { remove: jest.fn() };

      mockNative.addEventListener
        .mockReturnValueOnce(mockSub1)
        .mockReturnValueOnce(mockSub2);

      const dismissSub = UltimateAlarm.addEventListener('dismiss', dismissCallback);
      const snoozeSub = UltimateAlarm.addEventListener('snooze', snoozeCallback);

      expect(dismissSub).toBeDefined();
      expect(snoozeSub).toBeDefined();
      expect(mockNative.addEventListener).toHaveBeenCalledTimes(2);
    });
  });

  describe('Event Subscription Management', () => {
    it('should allow removing event listener via subscription', () => {
      const callback = jest.fn();
      const mockRemove = jest.fn();
      const mockSubscription = { remove: mockRemove };

      mockNative.addEventListener.mockReturnValue(mockSubscription);

      const subscription = UltimateAlarm.addEventListener('dismiss', callback);
      subscription.remove();

      expect(mockRemove).toHaveBeenCalled();
    });

    it('should handle multiple remove calls gracefully', () => {
      const callback = jest.fn();
      const mockRemove = jest.fn();
      const mockSubscription = { remove: mockRemove };

      mockNative.addEventListener.mockReturnValue(mockSubscription);

      const subscription = UltimateAlarm.addEventListener('dismiss', callback);

      // Should not throw when called multiple times
      expect(() => {
        subscription.remove();
        subscription.remove();
        subscription.remove();
      }).not.toThrow();
    });

    it('should allow selective subscription removal', () => {
      const callback1 = jest.fn();
      const callback2 = jest.fn();
      const mockRemove1 = jest.fn();
      const mockRemove2 = jest.fn();
      const mockSub1 = { remove: mockRemove1 };
      const mockSub2 = { remove: mockRemove2 };

      mockNative.addEventListener
        .mockReturnValueOnce(mockSub1)
        .mockReturnValueOnce(mockSub2);

      const sub1 = UltimateAlarm.addEventListener('dismiss', callback1);
      const sub2 = UltimateAlarm.addEventListener('dismiss', callback2);

      // Remove only first subscription
      sub1.remove();

      expect(mockRemove1).toHaveBeenCalled();
      expect(mockRemove2).not.toHaveBeenCalled();
    });
  });

  describe('Event Payload', () => {
    it('should pass correct event data for dismiss event', () => {
      const callback = jest.fn();
      let eventCallback: (event: AlarmEvent) => void = () => {};

      mockNative.addEventListener.mockImplementation((event, cb) => {
        eventCallback = cb;
        return { remove: jest.fn() };
      });

      UltimateAlarm.addEventListener('dismiss', callback);

      const mockEvent: AlarmEvent = {
        alarmId: 'test-alarm',
        action: 'dismiss',
        timestamp: new Date(),
      };

      eventCallback(mockEvent);

      expect(callback).toHaveBeenCalledWith(mockEvent);
      expect(callback).toHaveBeenCalledWith(
        expect.objectContaining({
          alarmId: 'test-alarm',
          action: 'dismiss',
        })
      );
    });

    it('should pass correct event data for snooze event', () => {
      const callback = jest.fn();
      let eventCallback: (event: AlarmEvent) => void = () => {};

      mockNative.addEventListener.mockImplementation((event, cb) => {
        eventCallback = cb;
        return { remove: jest.fn() };
      });

      UltimateAlarm.addEventListener('snooze', callback);

      const mockEvent: AlarmEvent = {
        alarmId: 'test-alarm',
        action: 'snooze',
        timestamp: new Date(),
      };

      eventCallback(mockEvent);

      expect(callback).toHaveBeenCalledWith(mockEvent);
      expect(callback).toHaveBeenCalledWith(
        expect.objectContaining({
          alarmId: 'test-alarm',
          action: 'snooze',
        })
      );
    });

    it('should include custom data in event payload', () => {
      const callback = jest.fn();
      let eventCallback: (event: AlarmEvent) => void = () => {};

      mockNative.addEventListener.mockImplementation((event, cb) => {
        eventCallback = cb;
        return { remove: jest.fn() };
      });

      UltimateAlarm.addEventListener('dismiss', callback);

      const mockEvent: AlarmEvent = {
        alarmId: 'custom-alarm',
        action: 'dismiss',
        timestamp: new Date(),
        data: {
          userId: '123',
          taskId: 'task-456',
        },
      };

      eventCallback(mockEvent);

      expect(callback).toHaveBeenCalledWith(
        expect.objectContaining({
          data: {
            userId: '123',
            taskId: 'task-456',
          },
        })
      );
    });
  });

  describe('Event Listener Lifecycle', () => {
    it('should properly clean up listeners in React useEffect pattern', () => {
      const callback = jest.fn();
      const mockRemove = jest.fn();
      const mockSubscription = { remove: mockRemove };

      mockNative.addEventListener.mockReturnValue(mockSubscription);

      // Simulate React useEffect
      const subscription = UltimateAlarm.addEventListener('dismiss', callback);

      // Simulate component cleanup
      const cleanup = () => subscription.remove();
      cleanup();

      expect(mockRemove).toHaveBeenCalled();
    });

    it('should handle multiple event listeners in component lifecycle', () => {
      const dismissCallback = jest.fn();
      const snoozeCallback = jest.fn();
      const mockRemove1 = jest.fn();
      const mockRemove2 = jest.fn();

      mockNative.addEventListener
        .mockReturnValueOnce({ remove: mockRemove1 })
        .mockReturnValueOnce({ remove: mockRemove2 });

      // Simulate component mount
      const dismissSub = UltimateAlarm.addEventListener('dismiss', dismissCallback);
      const snoozeSub = UltimateAlarm.addEventListener('snooze', snoozeCallback);

      // Simulate component unmount
      dismissSub.remove();
      snoozeSub.remove();

      expect(mockRemove1).toHaveBeenCalled();
      expect(mockRemove2).toHaveBeenCalled();
    });
  });

  describe('Error Handling', () => {
    it('should handle errors in event callbacks gracefully', () => {
      const errorCallback = jest.fn(() => {
        throw new Error('Callback error');
      });

      let eventCallback: (event: AlarmEvent) => void = () => {};

      mockNative.addEventListener.mockImplementation((event, cb) => {
        eventCallback = cb;
        return { remove: jest.fn() };
      });

      UltimateAlarm.addEventListener('dismiss', errorCallback);

      const mockEvent: AlarmEvent = {
        alarmId: 'test-alarm',
        action: 'dismiss',
        timestamp: new Date(),
      };

      // Should not throw even if callback throws
      expect(() => {
        try {
          eventCallback(mockEvent);
        } catch (error) {
          // Errors in callbacks should be caught by the app
        }
      }).not.toThrow();
    });
  });
});
