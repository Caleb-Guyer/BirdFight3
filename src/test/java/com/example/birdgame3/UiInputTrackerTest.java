package com.example.birdgame3;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiInputTrackerTest {
    @Test
    void tracksTheActiveDeviceAndPlayerIndependently() {
        UiInputTracker tracker = new UiInputTracker(4);

        tracker.note(2, UiInputPrompts.Device.GAMEPAD);
        assertEquals(new UiInputTracker.ActiveInput(2, UiInputPrompts.Device.GAMEPAD),
                tracker.activeInputProperty().get());
        assertEquals(UiInputPrompts.Device.GAMEPAD, tracker.playerDevice(2));
        assertEquals(UiInputPrompts.Device.KEYBOARD_MOUSE, tracker.playerDevice(0));

        tracker.note(0, UiInputPrompts.Device.WIIMOTE_NUNCHUK);
        assertEquals(new UiInputTracker.ActiveInput(0, UiInputPrompts.Device.WIIMOTE_NUNCHUK),
                tracker.activeInputProperty().get());
        assertEquals(UiInputPrompts.Device.GAMEPAD, tracker.playerDevice(2));
    }

    @Test
    void promptObserversAreNotifiedImmediatelyWhenTheDeviceChanges() {
        UiInputTracker tracker = new UiInputTracker(2);
        AtomicInteger changes = new AtomicInteger();
        tracker.activeInputProperty().addListener((obs, before, after) -> changes.incrementAndGet());

        tracker.note(0, UiInputPrompts.Device.GAMEPAD);
        tracker.note(1, UiInputPrompts.Device.WIIMOTE_SIDEWAYS);
        tracker.note(1, UiInputPrompts.Device.WIIMOTE_SIDEWAYS);

        assertEquals(2, changes.get(), "Repeating the same active source should not churn UI bindings");
    }

    @Test
    void invalidPlayerIndicesAreClampedToARealLocalSlot() {
        UiInputTracker tracker = new UiInputTracker(2);
        tracker.note(99, UiInputPrompts.Device.GAMEPAD);
        assertEquals(1, tracker.activeInputProperty().get().playerIndex());
        tracker.note(-5, UiInputPrompts.Device.WIIMOTE_SIDEWAYS);
        assertEquals(0, tracker.activeInputProperty().get().playerIndex());
    }
}
