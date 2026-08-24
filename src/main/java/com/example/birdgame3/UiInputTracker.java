package com.example.birdgame3;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;

/** Tracks the most recently used local presentation device without touching gameplay state. */
final class UiInputTracker {
    record ActiveInput(int playerIndex, UiInputPrompts.Device device) {
        ActiveInput {
            playerIndex = Math.max(0, playerIndex);
            device = device == null ? UiInputPrompts.Device.KEYBOARD_MOUSE : device;
        }
    }

    private final ObjectProperty<UiInputPrompts.Device>[] playerDevices;
    private final ReadOnlyObjectWrapper<ActiveInput> activeInput =
            new ReadOnlyObjectWrapper<>(new ActiveInput(0, UiInputPrompts.Device.KEYBOARD_MOUSE));

    @SuppressWarnings("unchecked")
    UiInputTracker(int playerCount) {
        int count = Math.max(1, playerCount);
        playerDevices = (ObjectProperty<UiInputPrompts.Device>[]) new ObjectProperty<?>[count];
        for (int i = 0; i < count; i++) {
            playerDevices[i] = new SimpleObjectProperty<>(UiInputPrompts.Device.KEYBOARD_MOUSE);
        }
    }

    void note(int playerIndex, UiInputPrompts.Device device) {
        int player = Math.clamp(playerIndex, 0, playerDevices.length - 1);
        UiInputPrompts.Device resolved = device == null
                ? UiInputPrompts.Device.KEYBOARD_MOUSE
                : device;
        playerDevices[player].set(resolved);
        ActiveInput current = activeInput.get();
        if (current.playerIndex() != player || current.device() != resolved) {
            activeInput.set(new ActiveInput(player, resolved));
        }
    }

    UiInputPrompts.Device activeDevice() {
        return activeInput.get().device();
    }

    ReadOnlyObjectProperty<ActiveInput> activeInputProperty() {
        return activeInput.getReadOnlyProperty();
    }

    ObjectProperty<UiInputPrompts.Device> playerDeviceProperty(int playerIndex) {
        return playerDevices[Math.clamp(playerIndex, 0, playerDevices.length - 1)];
    }

    UiInputPrompts.Device playerDevice(int playerIndex) {
        return playerDeviceProperty(playerIndex).get();
    }
}
