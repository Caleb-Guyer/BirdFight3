package com.example.birdgame3;

import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;
import org.hid4java.HidServicesListener;
import org.hid4java.HidServicesSpecification;
import org.hid4java.ScanMode;
import org.hid4java.event.HidServicesEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WiimoteInputManager implements HidServicesListener, AutoCloseable {
    private static final int WIIMOTE_VENDOR_ID = 0x057E;
    private static final int PRODUCT_OLD = 0x0306;
    private static final int PRODUCT_PLUS = 0x0330;
    private static final int PLAYER_SLOTS = 4;

    private final Map<String, WiimoteController> controllers = new LinkedHashMap<>();
    private final WiimoteControlMode[] playerModes = new WiimoteControlMode[PLAYER_SLOTS];
    private HidServices hidServices;
    private boolean available = false;
    private String unavailableReason = "";
    private long attachSequence = 0L;

    WiimoteInputManager() {
        for (int i = 0; i < playerModes.length; i++) {
            playerModes[i] = WiimoteControlMode.OFF;
        }
        try {
            HidServicesSpecification specification = new HidServicesSpecification();
            specification.setAutoStart(false);
            specification.setAutoDataRead(false);
            specification.setScanMode(ScanMode.SCAN_AT_FIXED_INTERVAL);
            specification.setScanInterval(1500);
            hidServices = HidManager.getHidServices(specification);
            hidServices.addHidServicesListener(this);
            hidServices.start();
            available = true;
            for (HidDevice hidDevice : hidServices.getAttachedHidDevices()) {
                attachDevice(hidDevice);
            }
            refreshAssignments();
        } catch (RuntimeException ex) {
            available = false;
            unavailableReason = "Wiimote HID unavailable";
        }
    }

    synchronized void setPlayerMode(int playerIndex, WiimoteControlMode mode) {
        if (playerIndex < 0 || playerIndex >= playerModes.length) {
            return;
        }
        playerModes[playerIndex] = mode == null ? WiimoteControlMode.OFF : mode;
        refreshAssignments();
    }

    synchronized WiimoteControlMode getPlayerMode(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= playerModes.length) {
            return WiimoteControlMode.OFF;
        }
        return playerModes[playerIndex];
    }

    synchronized WiimoteMappedState stateForPlayer(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= playerModes.length) {
            return WiimoteMappedState.off("Wiimote mode off");
        }
        WiimoteControlMode mode = playerModes[playerIndex];
        if (!available) {
            return WiimoteMappedState.waiting(unavailableReason.isBlank() ? "Wiimote HID unavailable" : unavailableReason);
        }
        if (mode == WiimoteControlMode.OFF) {
            return WiimoteMappedState.off("Wiimote mode off");
        }
        for (WiimoteController controller : controllers.values()) {
            WiimoteMappedState state = controller.latestState();
            if (state.status() != null && controllerMatchesPlayer(controller, playerIndex)) {
                return state;
            }
        }
        return WiimoteMappedState.waiting("Waiting for Wiimote");
    }

    synchronized String statusForPlayer(int playerIndex) {
        return stateForPlayer(playerIndex).status();
    }

    synchronized WiimoteMappedState menuState() {
        for (int i = 0; i < playerModes.length; i++) {
            WiimoteMappedState state = stateForPlayer(i);
            if (state.connected()) {
                return state;
            }
        }
        return available ? WiimoteMappedState.waiting("Waiting for Wiimote") : WiimoteMappedState.waiting(unavailableReason);
    }

    @Override
    public synchronized void close() {
        for (WiimoteController controller : controllers.values()) {
            controller.close();
        }
        controllers.clear();
        if (hidServices != null) {
            try {
                hidServices.shutdown();
            } catch (RuntimeException ignored) {
            }
            hidServices = null;
        }
    }

    @Override
    public synchronized void hidDeviceAttached(HidServicesEvent event) {
        attachDevice(event == null ? null : event.getHidDevice());
        refreshAssignments();
    }

    @Override
    public synchronized void hidDeviceDetached(HidServicesEvent event) {
        HidDevice hidDevice = event == null ? null : event.getHidDevice();
        if (hidDevice == null) {
            return;
        }
        WiimoteController controller = controllers.remove(deviceKey(hidDevice));
        if (controller != null) {
            controller.close();
        }
        refreshAssignments();
    }

    @Override
    public void hidFailure(HidServicesEvent event) {
    }

    @Override
    public void hidDataReceived(HidServicesEvent event) {
    }

    private void attachDevice(HidDevice hidDevice) {
        if (!isSupportedWiimote(hidDevice)) {
            return;
        }
        String key = deviceKey(hidDevice);
        if (controllers.containsKey(key)) {
            return;
        }
        controllers.put(key, new WiimoteController(hidDevice, ++attachSequence));
    }

    private boolean isSupportedWiimote(HidDevice hidDevice) {
        if (hidDevice == null) {
            return false;
        }
        int vendor = hidDevice.getVendorId();
        int product = hidDevice.getProductId();
        return vendor == WIIMOTE_VENDOR_ID && (product == PRODUCT_OLD || product == PRODUCT_PLUS);
    }

    private String deviceKey(HidDevice hidDevice) {
        if (hidDevice == null) {
            return "";
        }
        String path = hidDevice.getPath();
        return path != null && !path.isBlank()
                ? path
                : vendorProductKey(hidDevice.getVendorId(), hidDevice.getProductId(), hidDevice.getSerialNumber());
    }

    private String vendorProductKey(int vendorId, int productId, String serialNumber) {
        return vendorId + ":" + productId + ":" + (serialNumber == null ? "" : serialNumber);
    }

    private boolean controllerMatchesPlayer(WiimoteController controller, int playerIndex) {
        return controller != null && statusForAssignment(controller, playerIndex);
    }

    private boolean statusForAssignment(WiimoteController controller, int playerIndex) {
        List<WiimoteController> assignedControllers = assignedControllers();
        int enabledCount = 0;
        for (int i = 0; i < playerModes.length; i++) {
            if (playerModes[i] != WiimoteControlMode.OFF) {
                if (i == playerIndex) {
                    return enabledCount < assignedControllers.size() && assignedControllers.get(enabledCount) == controller;
                }
                enabledCount++;
            }
        }
        return false;
    }

    private void refreshAssignments() {
        List<WiimoteController> orderedControllers = assignedControllers();
        int nextControllerIndex = 0;
        for (int playerIndex = 0; playerIndex < playerModes.length; playerIndex++) {
            WiimoteControlMode mode = playerModes[playerIndex];
            if (mode == WiimoteControlMode.OFF) {
                continue;
            }
            if (nextControllerIndex >= orderedControllers.size()) {
                break;
            }
            WiimoteController controller = orderedControllers.get(nextControllerIndex++);
            controller.assignToPlayer(playerIndex, mode);
        }

        for (WiimoteController controller : controllers.values()) {
            boolean assigned = false;
            for (int playerIndex = 0; playerIndex < playerModes.length; playerIndex++) {
                if (playerModes[playerIndex] == WiimoteControlMode.OFF) {
                    continue;
                }
                if (statusForAssignment(controller, playerIndex)) {
                    controller.assignToPlayer(playerIndex, playerModes[playerIndex]);
                    assigned = true;
                    break;
                }
            }
            if (!assigned) {
                controller.clearAssignment();
            }
        }
    }

    private List<WiimoteController> assignedControllers() {
        List<WiimoteController> ordered = new ArrayList<>(controllers.values());
        ordered.sort(Comparator.comparingLong(WiimoteController::attachOrder));
        return ordered;
    }
}
