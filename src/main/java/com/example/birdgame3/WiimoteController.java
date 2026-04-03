package com.example.birdgame3;

import org.hid4java.HidDevice;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

final class WiimoteController implements AutoCloseable {
    private static final int REPORT_BUFFER_SIZE = 32;
    private static final int REMOTE_LED_1 = 0x10;
    private static final int LIVE_READ_TIMEOUT_MS = 2;
    private static final long IDLE_SLEEP_MS = 100L;
    private static final long RECONNECT_SLEEP_MS = 600L;

    private final HidDevice device;
    private final long attachOrder;
    private final String deviceKey;
    private final AtomicReference<WiimoteMappedState> latestState =
            new AtomicReference<>(WiimoteMappedState.waiting("Waiting for Wiimote"));
    private final WiimoteControlMapper mapper = new WiimoteControlMapper();
    private final Thread worker;

    private volatile boolean running = true;
    private volatile int assignedPlayer = -1;
    private volatile WiimoteControlMode desiredMode = WiimoteControlMode.OFF;
    private volatile boolean reconfigureRequested = true;

    private boolean deviceOpen = false;
    private int configuredPlayer = -1;
    private WiimoteControlMode configuredMode = WiimoteControlMode.OFF;
    private boolean motionPlusAvailable = false;
    private boolean motionPlusActive = false;
    private boolean nunchukConnected = false;
    private boolean statusExtensionConnected = false;
    private boolean plainNunchukMode = false;
    private double remoteAccelX = 0.0;
    private double remoteAccelY = 0.0;
    private double remoteAccelZ = 1.0;
    private double gyroYaw = 0.0;
    private double gyroRoll = 0.0;
    private double gyroPitch = 0.0;
    private boolean gyroAvailable = false;
    private double gyroZeroYaw = 0.0;
    private double gyroZeroRoll = 0.0;
    private double gyroZeroPitch = 0.0;
    private int gyroCalibrationSamples = 0;
    private double nunchukStickX = 0.0;
    private double nunchukStickY = 0.0;
    private double nunchukAccelX = 0.0;
    private double nunchukAccelY = 0.0;
    private double nunchukAccelZ = 1.0;
    private boolean dpadUp = false;
    private boolean dpadDown = false;
    private boolean dpadLeft = false;
    private boolean dpadRight = false;
    private boolean buttonA = false;
    private boolean buttonB = false;
    private boolean buttonOne = false;
    private boolean buttonTwo = false;
    private boolean buttonPlus = false;
    private boolean buttonMinus = false;
    private boolean buttonHome = false;
    private boolean buttonC = false;
    private boolean buttonZ = false;
    private long lastSampleNanos = 0L;
    private String lastStatus = "Waiting for Wiimote";
    private boolean suppressNextStatusReconfigure = false;

    WiimoteController(HidDevice device, long attachOrder) {
        this.device = device;
        this.attachOrder = attachOrder;
        String path = device == null ? null : device.getPath();
        this.deviceKey = path != null && !path.isBlank() ? path : "wiimote-" + attachOrder;
        this.worker = new Thread(this::runLoop, "wiimote-" + attachOrder);
        this.worker.setDaemon(true);
        this.worker.start();
    }

    String deviceKey() {
        return deviceKey;
    }

    long attachOrder() {
        return attachOrder;
    }

    synchronized void assignToPlayer(int playerIndex, WiimoteControlMode mode) {
        if (assignedPlayer == playerIndex && desiredMode == mode) {
            return;
        }
        assignedPlayer = playerIndex;
        desiredMode = mode == null ? WiimoteControlMode.OFF : mode;
        reconfigureRequested = true;
    }

    synchronized void clearAssignment() {
        assignToPlayer(-1, WiimoteControlMode.OFF);
    }

    WiimoteMappedState latestState() {
        return latestState.get();
    }

    @Override
    public void close() {
        running = false;
        worker.interrupt();
        closeDevice();
    }

    private void runLoop() {
        byte[] buffer = new byte[REPORT_BUFFER_SIZE];
        while (running) {
            WiimoteControlMode mode = desiredMode;
            int player = assignedPlayer;
            if (mode == WiimoteControlMode.OFF || player < 0) {
                configuredMode = WiimoteControlMode.OFF;
                configuredPlayer = -1;
                mapper.reset();
                resetLiveState();
                lastStatus = "Wiimote mode off";
                latestState.set(WiimoteMappedState.off(lastStatus));
                closeDevice();
                sleepQuietly(IDLE_SLEEP_MS);
                continue;
            }

            if (!ensureOpen()) {
                lastStatus = "Waiting for Wiimote";
                latestState.set(WiimoteMappedState.waiting(lastStatus));
                sleepQuietly(RECONNECT_SLEEP_MS);
                continue;
            }

            if (reconfigureRequested || configuredMode != mode || configuredPlayer != player) {
                if (!configureDevice(mode, player, buffer)) {
                    lastStatus = "Unable to configure Wiimote";
                    latestState.set(WiimoteMappedState.waiting(lastStatus));
                    closeDevice();
                    sleepQuietly(RECONNECT_SLEEP_MS);
                    continue;
                }
                configuredMode = mode;
                configuredPlayer = player;
                reconfigureRequested = false;
            }

            int bytesRead = device.read(buffer, LIVE_READ_TIMEOUT_MS);
            if (bytesRead > 0) {
                processReport(buffer, bytesRead, mode);
            } else if (bytesRead == 0) {
                publishMappedState(mode);
            } else if (bytesRead < 0) {
                lastStatus = "Wiimote disconnected";
                latestState.set(WiimoteMappedState.waiting(lastStatus));
                closeDevice();
                sleepQuietly(RECONNECT_SLEEP_MS);
            }
        }
        closeDevice();
    }

    private boolean ensureOpen() {
        if (deviceOpen) {
            return true;
        }
        try {
            deviceOpen = device != null && device.open();
            if (deviceOpen) {
                device.setNonBlocking(false);
            }
            return deviceOpen;
        } catch (RuntimeException ex) {
            lastStatus = "Wiimote open failed";
            return false;
        }
    }

    private void closeDevice() {
        if (!deviceOpen) {
            return;
        }
        try {
            device.close();
        } catch (RuntimeException ignored) {
        }
        deviceOpen = false;
    }

    private boolean configureDevice(WiimoteControlMode mode, int player, byte[] scratchBuffer) {
        mapper.reset();
        resetLiveState();
        if (!sendLedMask(player)) {
            return false;
        }
        sleepQuietly(20L);
        initializeStandardExtension();
        sleepQuietly(20L);

        motionPlusAvailable = detectMotionPlus(scratchBuffer);
        if (motionPlusAvailable) {
            initializeMotionPlus();
            sleepQuietly(20L);
            if (mode == WiimoteControlMode.NUNCHUK) {
                activateMotionPlus((byte) 0x05);
                sleepQuietly(20L);
                initializeStandardExtension();
                plainNunchukMode = false;
            } else {
                activateMotionPlus((byte) 0x04);
                plainNunchukMode = false;
            }
            motionPlusActive = true;
            if (!setReportingMode((byte) 0x35)) {
                return false;
            }
        } else if (mode == WiimoteControlMode.NUNCHUK) {
            motionPlusActive = false;
            plainNunchukMode = true;
            if (!setReportingMode((byte) 0x35)) {
                return false;
            }
        } else {
            motionPlusActive = false;
            plainNunchukMode = false;
            if (!setReportingMode((byte) 0x31)) {
                return false;
            }
        }
        requestStatus();
        suppressNextStatusReconfigure = true;
        lastStatus = statusMessage(mode);
        latestState.set(WiimoteMappedState.waiting(lastStatus));
        return true;
    }

    private void initializeStandardExtension() {
        writeRegister(0xA400F0, new byte[]{0x55});
        sleepQuietly(10L);
        writeRegister(0xA400FB, new byte[]{0x00});
    }

    private void initializeMotionPlus() {
        writeRegister(0xA600F0, new byte[]{0x55});
    }

    private void activateMotionPlus(byte value) {
        writeRegister(0xA600FE, new byte[]{value});
    }

    private boolean detectMotionPlus(byte[] scratchBuffer) {
        byte[] motionPlusId = readRegister(0xA600FA, 6, scratchBuffer, 260);
        return motionPlusId != null
                && motionPlusId.length >= 6
                && u8(motionPlusId[2]) == 0xA6
                && u8(motionPlusId[3]) == 0x20
                && u8(motionPlusId[5]) == 0x05;
    }

    private boolean setReportingMode(byte reportMode) {
        return writeReport((byte) 0x12, new byte[]{0x04, reportMode}) >= 0;
    }

    private void requestStatus() {
        writeReport((byte) 0x15, new byte[]{0x00});
    }

    private boolean sendLedMask(int player) {
        int safePlayer = Math.clamp(player, 0, 3);
        int mask = REMOTE_LED_1 << safePlayer;
        return writeReport((byte) 0x11, new byte[]{(byte) mask}) >= 0;
    }

    private int writeRegister(int address, byte[] data) {
        byte[] payload = new byte[21];
        payload[0] = 0x04;
        payload[1] = (byte) ((address >> 16) & 0xFF);
        payload[2] = (byte) ((address >> 8) & 0xFF);
        payload[3] = (byte) (address & 0xFF);
        payload[4] = (byte) (data == null ? 0 : data.length);
        if (data != null && data.length > 0) {
            System.arraycopy(data, 0, payload, 5, Math.min(16, data.length));
        }
        return writeReport((byte) 0x16, payload);
    }

    private byte[] readRegister(int address, int size, byte[] scratchBuffer, int timeoutMs) {
        byte[] payload = new byte[]{
                0x04,
                (byte) ((address >> 16) & 0xFF),
                (byte) ((address >> 8) & 0xFF),
                (byte) (address & 0xFF),
                (byte) ((size >> 8) & 0xFF),
                (byte) (size & 0xFF)
        };
        if (writeReport((byte) 0x17, payload) < 0) {
            return null;
        }
        long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
        while (running && System.nanoTime() < deadline) {
            int remainingMs = (int) Math.max(10L, (deadline - System.nanoTime()) / 1_000_000L);
            int bytesRead = device.read(scratchBuffer, remainingMs);
            if (bytesRead <= 0) {
                continue;
            }
            int reportId = u8(scratchBuffer[0]);
            if (reportId == 0x21 && bytesRead >= 22) {
                int sizeAndError = u8(scratchBuffer[3]);
                int error = sizeAndError & 0x0F;
                if (error != 0) {
                    return null;
                }
                int packetSize = ((sizeAndError >> 4) & 0x0F) + 1;
                int packetAddress = (u8(scratchBuffer[4]) << 8) | u8(scratchBuffer[5]);
                int requestedAddress = address & 0xFFFF;
                if (packetAddress == requestedAddress) {
                    return Arrays.copyOfRange(scratchBuffer, 6, 6 + Math.min(size, packetSize));
                }
                continue;
            }
            if (reportId == 0x20) {
                handleStatusReport(scratchBuffer, bytesRead);
            }
        }
        return null;
    }

    private int writeReport(byte reportId, byte[] payload) {
        try {
            return device.write(payload, payload.length, reportId);
        } catch (RuntimeException ex) {
            return -1;
        }
    }

    private void processReport(byte[] buffer, int bytesRead, WiimoteControlMode mode) {
        if (bytesRead <= 0) {
            return;
        }
        byte[] report = bytesRead == buffer.length ? buffer : Arrays.copyOf(buffer, bytesRead);
        int reportId = u8(report[0]);
        switch (reportId) {
            case 0x20 -> {
                boolean extensionChanged = handleStatusReport(report, bytesRead);
                if (suppressNextStatusReconfigure) {
                    suppressNextStatusReconfigure = false;
                } else if (extensionChanged) {
                    reconfigureRequested = true;
                }
            }
            case 0x31 -> {
                parseCoreButtons(report);
                parseRemoteAccelerometer(report);
                gyroAvailable = false;
                publishMappedState(mode);
            }
            case 0x35 -> {
                parseCoreButtons(report);
                parseRemoteAccelerometer(report);
                parseExtensionReport(report, mode);
                publishMappedState(mode);
            }
            default -> {
                if (reportId >= 0x30 && reportId <= 0x3F) {
                    parseCoreButtons(report);
                    publishMappedState(mode);
                }
            }
        }
    }

    private boolean handleStatusReport(byte[] buffer, int bytesRead) {
        if (bytesRead < 7) {
            return false;
        }
        parseCoreButtons(buffer);
        boolean previousExtensionConnected = statusExtensionConnected;
        int flags = u8(buffer[3]);
        statusExtensionConnected = (flags & 0x02) != 0;
        if (!statusExtensionConnected && plainNunchukMode) {
            nunchukConnected = false;
        }
        return previousExtensionConnected != statusExtensionConnected;
    }

    private void parseCoreButtons(byte[] buffer) {
        if (buffer.length < 3) {
            return;
        }
        int first = u8(buffer[1]);
        int second = u8(buffer[2]);
        dpadLeft = (first & 0x01) != 0;
        dpadRight = (first & 0x02) != 0;
        dpadDown = (first & 0x04) != 0;
        dpadUp = (first & 0x08) != 0;
        buttonPlus = (first & 0x10) != 0;
        buttonTwo = (second & 0x01) != 0;
        buttonOne = (second & 0x02) != 0;
        buttonB = (second & 0x04) != 0;
        buttonA = (second & 0x08) != 0;
        buttonMinus = (second & 0x10) != 0;
        buttonHome = (second & 0x80) != 0;
    }

    private void parseRemoteAccelerometer(byte[] buffer) {
        if (buffer.length < 6) {
            return;
        }
        int buttonsLo = u8(buffer[1]);
        int buttonsHi = u8(buffer[2]);
        int rawX = (u8(buffer[3]) << 2) | (buttonsLo & 0x03);
        int rawY = (u8(buffer[4]) << 2) | (((buttonsHi >> 5) & 0x01) << 1);
        int rawZ = (u8(buffer[5]) << 2) | (((buttonsHi >> 4) & 0x01) << 1);
        remoteAccelX = normalizeAccelerometer(rawX);
        remoteAccelY = normalizeAccelerometer(rawY);
        remoteAccelZ = normalizeAccelerometer(rawZ);
    }

    private void parseExtensionReport(byte[] buffer, WiimoteControlMode mode) {
        if (buffer.length < 12) {
            return;
        }
        byte[] ext = Arrays.copyOfRange(buffer, 6, Math.min(buffer.length, 22));
        if (motionPlusActive) {
            if (mode == WiimoteControlMode.NUNCHUK && ext.length >= 6 && (u8(ext[5]) & 0x02) == 0) {
                parseMotionPlusPassthroughNunchuk(ext);
            } else {
                parseMotionPlusData(ext);
            }
            return;
        }
        if (plainNunchukMode || statusExtensionConnected) {
            parsePlainNunchuk(ext);
        }
    }

    private void parseMotionPlusData(byte[] ext) {
        if (ext.length < 6) {
            return;
        }
        int rawYaw = u8(ext[0]) | ((u8(ext[3]) & 0xFC) << 6);
        int rawRoll = u8(ext[1]) | ((u8(ext[4]) & 0xFC) << 6);
        int rawPitch = u8(ext[2]) | ((u8(ext[5]) & 0xFC) << 6);
        boolean yawSlow = (u8(ext[3]) & 0x02) != 0;
        boolean rollSlow = (u8(ext[4]) & 0x02) != 0;
        boolean pitchSlow = (u8(ext[3]) & 0x01) != 0;
        nunchukConnected = (u8(ext[4]) & 0x01) != 0 && desiredMode == WiimoteControlMode.NUNCHUK;
        motionPlusAvailable = true;
        gyroAvailable = true;
        calibrateGyroZero(rawYaw, rawRoll, rawPitch);
        gyroYaw = normalizeGyro(rawYaw, gyroZeroYaw, yawSlow);
        gyroRoll = normalizeGyro(rawRoll, gyroZeroRoll, rollSlow);
        gyroPitch = normalizeGyro(rawPitch, gyroZeroPitch, pitchSlow);
    }

    private void parseMotionPlusPassthroughNunchuk(byte[] ext) {
        if (ext.length < 6) {
            return;
        }
        nunchukConnected = true;
        nunchukStickX = normalizeStick(u8(ext[0]));
        nunchukStickY = normalizeStick(u8(ext[1]));
        int accelX = ((u8(ext[2]) & 0xFE) << 2) | ((u8(ext[5]) >> 5) & 0x02);
        int accelY = ((u8(ext[3]) & 0xFE) << 2) | ((u8(ext[5]) >> 4) & 0x02);
        int accelZ = ((u8(ext[4]) & 0xFE) << 2) | ((u8(ext[5]) >> 6) & 0x06);
        nunchukAccelX = normalizeAccelerometer(accelX);
        nunchukAccelY = normalizeAccelerometer(accelY);
        nunchukAccelZ = normalizeAccelerometer(accelZ);
        buttonC = (u8(ext[5]) & 0x08) == 0;
        buttonZ = (u8(ext[5]) & 0x04) == 0;
    }

    private void parsePlainNunchuk(byte[] ext) {
        if (ext.length < 6) {
            return;
        }
        nunchukConnected = statusExtensionConnected;
        nunchukStickX = normalizeStick(u8(ext[0]));
        nunchukStickY = normalizeStick(u8(ext[1]));
        int accelX = (u8(ext[2]) << 2) | ((u8(ext[5]) >> 2) & 0x03);
        int accelY = (u8(ext[3]) << 2) | ((u8(ext[5]) >> 4) & 0x03);
        int accelZ = (u8(ext[4]) << 2) | ((u8(ext[5]) >> 6) & 0x03);
        nunchukAccelX = normalizeAccelerometer(accelX);
        nunchukAccelY = normalizeAccelerometer(accelY);
        nunchukAccelZ = normalizeAccelerometer(accelZ);
        buttonZ = (u8(ext[5]) & 0x01) == 0;
        buttonC = (u8(ext[5]) & 0x02) == 0;
    }

    private void calibrateGyroZero(int rawYaw, int rawRoll, int rawPitch) {
        if (gyroCalibrationSamples >= 24) {
            return;
        }
        gyroZeroYaw = (gyroZeroYaw * gyroCalibrationSamples + rawYaw) / Math.max(1, gyroCalibrationSamples + 1);
        gyroZeroRoll = (gyroZeroRoll * gyroCalibrationSamples + rawRoll) / Math.max(1, gyroCalibrationSamples + 1);
        gyroZeroPitch = (gyroZeroPitch * gyroCalibrationSamples + rawPitch) / Math.max(1, gyroCalibrationSamples + 1);
        gyroCalibrationSamples++;
    }

    private void publishMappedState(WiimoteControlMode mode) {
        lastSampleNanos = System.nanoTime();
        lastStatus = statusMessage(mode);
        WiimoteRawInput rawInput = new WiimoteRawInput(
                true,
                motionPlusAvailable,
                motionPlusActive,
                nunchukConnected,
                gyroAvailable,
                dpadUp,
                dpadDown,
                dpadLeft,
                dpadRight,
                buttonA,
                buttonB,
                buttonOne,
                buttonTwo,
                buttonPlus,
                buttonMinus,
                buttonHome,
                buttonC,
                buttonZ,
                remoteAccelX,
                remoteAccelY,
                remoteAccelZ,
                gyroYaw,
                gyroRoll,
                gyroPitch,
                nunchukStickX,
                nunchukStickY,
                nunchukAccelX,
                nunchukAccelY,
                nunchukAccelZ,
                lastSampleNanos,
                lastStatus
        );
        latestState.set(mapper.map(rawInput, mode));
    }

    private void resetLiveState() {
        motionPlusAvailable = false;
        motionPlusActive = false;
        nunchukConnected = false;
        statusExtensionConnected = false;
        plainNunchukMode = false;
        remoteAccelX = 0.0;
        remoteAccelY = 0.0;
        remoteAccelZ = 1.0;
        gyroYaw = 0.0;
        gyroRoll = 0.0;
        gyroPitch = 0.0;
        gyroAvailable = false;
        gyroZeroYaw = 0.0;
        gyroZeroRoll = 0.0;
        gyroZeroPitch = 0.0;
        gyroCalibrationSamples = 0;
        nunchukStickX = 0.0;
        nunchukStickY = 0.0;
        nunchukAccelX = 0.0;
        nunchukAccelY = 0.0;
        nunchukAccelZ = 1.0;
        dpadUp = false;
        dpadDown = false;
        dpadLeft = false;
        dpadRight = false;
        buttonA = false;
        buttonB = false;
        buttonOne = false;
        buttonTwo = false;
        buttonPlus = false;
        buttonMinus = false;
        buttonHome = false;
        buttonC = false;
        buttonZ = false;
        suppressNextStatusReconfigure = false;
    }

    private String statusMessage(WiimoteControlMode mode) {
        if (mode == WiimoteControlMode.NUNCHUK) {
            if (!nunchukConnected) {
                return motionPlusActive
                        ? "Wiimote connected | waiting for Nunchuk"
                        : "Wiimote connected | Nunchuk mode ready";
            }
            return motionPlusActive
                    ? "Wiimote + Nunchuk | gyro + accel ready"
                    : "Wiimote + Nunchuk | accel ready";
        }
        return motionPlusActive
                ? "Sideways Wiimote | gyro + accel ready"
                : "Sideways Wiimote | accel ready";
    }

    private double normalizeAccelerometer(int raw) {
        return Math.clamp((raw - 512.0) / 128.0, -4.0, 4.0);
    }

    private double normalizeStick(int raw) {
        return Math.clamp((raw - 128.0) / 96.0, -1.0, 1.0);
    }

    private double normalizeGyro(int rawValue, double zero, boolean slowMode) {
        double value = rawValue - zero;
        if (!slowMode) {
            value *= (2000.0 / 440.0);
        }
        return value / 8.0;
    }

    private int u8(byte value) {
        return value & 0xFF;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
